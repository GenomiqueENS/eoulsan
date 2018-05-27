/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.modules.expression.hadoop;

import static fr.ens.biologie.genomique.eoulsan.CommonHadoop.createConfiguration;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.allPortsRequiredInWorkingDirectory;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATION_GFF;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import fr.ens.biologie.genomique.eoulsan.CommonHadoop;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.Settings;
import fr.ens.biologie.genomique.eoulsan.annotations.HadoopOnly;
import fr.ens.biologie.genomique.eoulsan.bio.BadBioEntryException;
import fr.ens.biologie.genomique.eoulsan.bio.GenomeDescription;
import fr.ens.biologie.genomique.eoulsan.bio.GenomicArray;
import fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.HTSeqCounter;
import fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.HTSeqUtils;
import fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.OverlapMode;
import fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.StrandUsage;
import fr.ens.biologie.genomique.eoulsan.bio.io.hadoop.ExpressionOutputFormat;
import fr.ens.biologie.genomique.eoulsan.bio.io.hadoop.SAMInputFormat;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.modules.expression.AbstractExpressionModule;
import fr.ens.biologie.genomique.eoulsan.modules.expression.FinalExpressionFeaturesCreator;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.MapReduceUtils;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.PathUtils;
import fr.ens.biologie.genomique.eoulsan.util.locker.Locker;
import fr.ens.biologie.genomique.eoulsan.util.locker.ZooKeeperLocker;

/**
 * This class is the main class for the expression program of the reads in
 * hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopOnly
public class ExpressionHadoopModule extends AbstractExpressionModule {

  private static final String TSAM_EXTENSION = ".tsam";
  private static final String SERIALIZATION_EXTENSION = ".ser";
  static final char SAM_RECORD_PAIRED_END_SERPARATOR = '£';
  static final String GENOME_DESC_PATH_KEY =
      Globals.PARAMETER_PREFIX + ".expression.genome.desc.file";

  private Configuration conf;

  /**
   * Create JobConf object for HTSeq-count.
   * @param context the task context
   * @param alignmentsData alignment data
   * @param featureAnnotationData feature annotations data
   * @param gtfFormat true if the annotation file is in GTF format
   * @param genomeDescriptionData genome description data
   * @param genomicType genomic type
   * @param attributeId attributeId
   * @param splitAttributeValues split attribute values
   * @param stranded stranded mode
   * @param overlapMode overlap mode
   * @param removeAmbiguousCases true to remove ambiguous cases
   * @throws IOException if an error occurs while creating job
   * @throws BadBioEntryException if an entry of the annotation file is invalid
   * @throws EoulsanException if the job creating fails
   */
  private static Job createJobHTSeqCounter(final Configuration parentConf,
      final TaskContext context, final Data alignmentsData,
      final Data featureAnnotationData, final boolean gtfFormat,
      final Data genomeDescriptionData, final Data outData,
      final String genomicType, final String attributeId,
      final boolean splitAttributeValues, final StrandUsage stranded,
      final OverlapMode overlapMode, final boolean removeAmbiguousCases,
      final boolean tsamFormat)
      throws IOException, BadBioEntryException, EoulsanException {

    final Configuration jobConf = new Configuration(parentConf);

    // Get input DataFile
    DataFile inputDataFile = alignmentsData.getDataFile();

    if (inputDataFile == null) {
      throw new IOException("No input file found.");
    }

    final String dataFileSource;

    if (tsamFormat) {
      dataFileSource =
          StringUtils.filenameWithoutExtension(inputDataFile.getSource())
              + TSAM_EXTENSION;
    } else {
      dataFileSource = inputDataFile.getSource();
    }

    // Set input path
    final Path inputPath = new Path(dataFileSource);

    // Get annotation DataFile
    final DataFile annotationDataFile = featureAnnotationData.getDataFile();

    // Get output file
    final DataFile outFile = outData.getDataFile();

    // Get temporary file
    final DataFile tmpFile =
        new DataFile(outFile.getParent(), outFile.getBasename() + ".tmp");

    getLogger().fine("sample: " + alignmentsData.getName());
    getLogger().fine("inputPath.getName(): " + inputPath.getName());
    getLogger().fine("annotationDataFile: " + annotationDataFile.getSource());
    getLogger().fine("outFile: " + outFile.getSource());
    getLogger().fine("tmpFile: " + tmpFile.getSource());

    jobConf.set("mapred.child.java.opts", "-Xmx1024m");

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, COUNTER_GROUP);

    // Set Genome description path
    final DataFile genomeDescDataFile = genomeDescriptionData.getDataFile();
    jobConf.set(GENOME_DESC_PATH_KEY, genomeDescDataFile.getSource());

    // Set the "stranded" parameter
    jobConf.set(HTSeqCountMapper.STRANDED_PARAM, stranded.getName());

    // Set the "overlap mode" parameter
    jobConf.set(HTSeqCountMapper.OVERLAP_MODE_PARAM, overlapMode.getName());

    // Set the "remove ambiguous cases" parameter
    jobConf.setBoolean(HTSeqCountMapper.REMOVE_AMBIGUOUS_CASES,
        removeAmbiguousCases);

    final Path featuresIndexPath =
        getAnnotationIndexSerializedPath(featureAnnotationData.getDataFile());

    getLogger().info("featuresIndexPath: " + featuresIndexPath);

    // Create serialized feature index
    if (!PathUtils.isFile(featuresIndexPath, jobConf)) {

      final Locker lock = createZookeeperLock(parentConf, context);

      lock.lock();

      createFeaturesIndex(context, annotationDataFile, gtfFormat, genomicType,
          attributeId, splitAttributeValues, stranded, genomeDescDataFile,
          featuresIndexPath, jobConf);

      lock.unlock();
    }

    // Create the job and its name
    final Job job = Job.getInstance(jobConf,
        "Expression computation with htseq-count ("
            + alignmentsData.getName() + ", " + inputPath.getName() + ", "
            + annotationDataFile.getSource() + ", " + genomicType + ", "
            + attributeId + ", stranded: " + stranded
            + ", removeAmbiguousCases: " + removeAmbiguousCases + ")");

    // Set the path to the features index
    job.addCacheFile(featuresIndexPath.toUri());

    // Set the jar
    job.setJarByClass(ExpressionHadoopModule.class);

    // Set input path
    FileInputFormat.setInputPaths(job, inputPath);

    // Set input format
    job.setInputFormatClass(SAMInputFormat.class);

    // Set the mapper class
    job.setMapperClass(HTSeqCountMapper.class);

    // Set the combiner class
    job.setCombinerClass(HTSeqCountReducer.class);

    // Set the reducer class
    job.setReducerClass(HTSeqCountReducer.class);

    // Set the output format
    job.setOutputFormatClass(ExpressionOutputFormat.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(LongWritable.class);

    // Set output path
    FileOutputFormat.setOutputPath(job, new Path(tmpFile.getSource()));

    return job;
  }

  private static Job createJobPairedEnd(final Configuration parentConf,
      final TaskContext context, final Data alignmentsData,
      final Data genomeDescriptionData)
      throws IOException, BadBioEntryException {

    final Configuration jobConf = new Configuration(parentConf);

    // Get the source
    final DataFile inputDataFile = alignmentsData.getDataFile();

    // Set input path
    final Path inputPath = new Path(inputDataFile.getSource());

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, COUNTER_GROUP);

    // Set Genome description path
    jobConf.set(GENOME_DESC_PATH_KEY, genomeDescriptionData.getDataFilename());

    // Create the job and its name
    final Job job = Job.getInstance(jobConf,
        "Pretreatment for the expression estimation step ("
            + alignmentsData.getName() + ", " + inputDataFile.getSource()
            + ")");

    // Set the jar
    job.setJarByClass(ExpressionHadoopModule.class);

    // Set input path
    FileInputFormat.addInputPath(job, inputPath);

    // Set the Mapper class
    job.setMapperClass(PreTreatmentExpressionMapper.class);

    // Set the Reducer class
    job.setReducerClass(PreTreatmentExpressionReducer.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Output name
    String outputName =
        StringUtils.filenameWithoutExtension(inputPath.getName());
    outputName = outputName.substring(0, outputName.length());
    outputName += TSAM_EXTENSION;

    // Set output path
    FileOutputFormat.setOutputPath(job,
        new Path(inputPath.getParent(), outputName));

    return job;
  }

  /**
   * @param context Eoulsan context
   * @param annotationFile GFF annotation file path
   * @param gtfFormat true if the annotation file is in GTF format
   * @param featureType feature type to use
   * @param attributeId attribute id
   * @param splitAttributeValues split attribute values
   * @param stranded strand mode
   * @param genomeDescDataFile genome description DataFile
   * @param featuresIndexPath feature index output path
   * @param conf Hadoop configuration object
   * @throws IOException if an error occurs while creating the feature index
   *           file
   * @throws BadBioEntryException if an entry of the annotation file is invalid
   * @throws EoulsanException if an error occurs with feature types and feature
   *           identifiers
   */
  private static void createFeaturesIndex(final TaskContext context,
      final DataFile annotationFile, final boolean gtfFormat,
      final String featureType, final String attributeId,
      final boolean splitAttributeValues, final StrandUsage stranded,
      final DataFile genomeDescDataFile, final Path featuresIndexPath,
      final Configuration conf)
      throws IOException, BadBioEntryException, EoulsanException {

    // Do nothing if the file already exists
    if (PathUtils.isFile(featuresIndexPath, conf)) {
      return;
    }

    final GenomicArray<String> features = new GenomicArray<>();
    final GenomeDescription genomeDescription =
        GenomeDescription.load(genomeDescDataFile.open());
    final Map<String, Integer> counts = new HashMap<>();

    HTSeqUtils.storeAnnotation(features, annotationFile.open(), gtfFormat,
        featureType, stranded, attributeId, splitAttributeValues, counts);

    if (counts.size() == 0) {
      throw new EoulsanException(
          "Warning: No features of type '" + featureType + "' found.\n");
    }

    final File featuresIndexFile = context.getRuntime()
        .createFileInTempDir(StringUtils.basename(annotationFile.getName())
            + SERIALIZATION_EXTENSION);

    // Add all chromosomes even without annotations to the feature object
    features.addChromosomes(genomeDescription);

    // Save the annotation
    features.save(featuresIndexFile);

    PathUtils.copyLocalFileToPath(featuresIndexFile, featuresIndexPath, conf);

    if (!featuresIndexFile.delete()) {
      getLogger().warning("Can not delete features index file: "
          + featuresIndexFile.getAbsolutePath());
    }
  }

  private static void createFinalExpressionFeaturesFile(
      final TaskContext context, final Data featureAnnotationData,
      final Data outData, final Job job, final Configuration conf)
      throws IOException {

    FinalExpressionFeaturesCreator fefc = null;

    // Load the annotation index
    final Path featuresIndexPath =
        getAnnotationIndexSerializedPath(featureAnnotationData.getDataFile());

    final FileSystem fs = featuresIndexPath.getFileSystem(conf);

    fefc = new FinalExpressionFeaturesCreator(fs.open(featuresIndexPath));

    // Set the result path
    final Path resultPath = new Path(outData.getDataFile().getSource());

    fefc.initializeExpressionResults();

    // Load map-reduce results
    fefc.loadPreResults(new DataFile(job.getConfiguration()
        .get("mapreduce.output.fileoutputformat.outputdir")).open());

    fefc.saveFinalResults(fs.create(resultPath));
  }

  /**
   * Create the path to the serialized annotation index.
   * @param featureAnnotationFile feature annotation file
   * @return an Hadoop path with the path of the serialized annotation
   * @throws IOException if an error occurs while getting the path
   */
  private static Path getAnnotationIndexSerializedPath(
      final DataFile featureAnnotationFile) throws IOException {

    final DataFile file = new DataFile(featureAnnotationFile.getParent(),
        featureAnnotationFile.getBasename() + SERIALIZATION_EXTENSION);

    return new Path(file.getSource());
  }

  //
  // Module methods
  //

  @Override
  public InputPorts getInputPorts() {

    return allPortsRequiredInWorkingDirectory(super.getInputPorts());
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    super.configure(context, stepParameters);
    this.conf = CommonHadoop.createConfiguration(EoulsanRuntime.getSettings());
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    final Data alignmentsData = context.getInputData(MAPPER_RESULTS_SAM);
    final Data featureAnnotationData =
        context.getInputData(isGTFFormat() ? ANNOTATION_GFF : ANNOTATION_GFF);
    final Data genomeDescriptionData = context.getInputData(GENOME_DESC_TXT);
    final Data outData =
        context.getOutputData(EXPRESSION_RESULTS_TSV, alignmentsData);

    if (getCounter().getCounterName().equals(HTSeqCounter.COUNTER_NAME)) {
      return executeJobHTSeqCounter(context, alignmentsData,
          featureAnnotationData, genomeDescriptionData, outData, status);
    }

    return status.createTaskResult(
        new EoulsanException(
            "Unknown counter: " + getCounter().getCounterName()),
        "Unknown counter: " + getCounter().getCounterName());
  }

  /**
   * Execute HTSeq-count counter as an Hadoop job.
   * @param context Eoulsan context
   * @param status Eoulsan status
   * @return a StepResult object
   */
  private TaskResult executeJobHTSeqCounter(final TaskContext context,
      final Data alignmentsData, final Data featureAnnotationData,
      final Data genomeDescriptionData, final Data outData,
      final TaskStatus status) {

    // Create configuration object
    final Configuration conf = createConfiguration();

    try {
      final long startTime = System.currentTimeMillis();

      getLogger().info("Genomic type: " + getGenomicType());

      // Get the paired end mode
      boolean pairedEnd =
          HTSeqCounter.isPairedData(alignmentsData.getDataFile().open());

      // Paired-end pre-processing
      if (pairedEnd) {
        MapReduceUtils.submitAndWaitForJob(
            createJobPairedEnd(conf, context, alignmentsData,
                genomeDescriptionData),
            alignmentsData.getName(), CommonHadoop.CHECK_COMPLETION_TIME,
            status, COUNTER_GROUP);
      }

      // Create the list of jobs to run

      final Job job = createJobHTSeqCounter(conf, context, alignmentsData,
          featureAnnotationData, isGTFFormat(), genomeDescriptionData, outData,
          getGenomicType(), getAttributeId(), isSplitAttributeValues(),
          getStranded(), getOverlapMode(), isRemoveAmbiguousCases(), pairedEnd);

      // Compute map-reduce part of the expression computation
      MapReduceUtils.submitAndWaitForJob(job, alignmentsData.getName(),
          CommonHadoop.CHECK_COMPLETION_TIME, status, COUNTER_GROUP);

      final long mapReduceEndTime = System.currentTimeMillis();
      getLogger().info("Finish the first part of the expression computation in "
          + ((mapReduceEndTime - startTime) / 1000) + " seconds.");

      // Create the final expression files
      createFinalExpressionFeaturesFile(context, featureAnnotationData, outData,
          job, this.conf);

      getLogger().info("Finish the create of the final expression files in "
          + ((System.currentTimeMillis() - mapReduceEndTime) / 1000)
          + " seconds.");

      return status.createTaskResult();

    } catch (IOException e) {

      return status.createTaskResult(e,
          "Error while running job: " + e.getMessage());
    } catch (BadBioEntryException e) {

      return status.createTaskResult(e,
          "Invalid annotation entry: " + e.getEntry());
    } catch (EoulsanException e) {

      return status.createTaskResult(e,
          "Error while reading the annotation file: " + e.getMessage());
    }
  }

  /**
   * Create a Zookeeper lock.
   * @param conf Hadoop configuration
   * @param context Eoulsan task context
   * @return a Lock object
   * @throws IOException if an error occurs while creating the lock
   */
  private static Locker createZookeeperLock(final Configuration conf,
      final TaskContext context) throws IOException {

    final Settings settings = context.getSettings();

    String connectString = settings.getZooKeeperConnectString();

    if (connectString == null) {

      connectString = conf.get("yarn.resourcemanager.hostname").split(":")[0]
          + ":" + settings.getZooKeeperDefaultPort();

    }

    return new ZooKeeperLocker(connectString,
        settings.getZooKeeperSessionTimeout(),
        "/eoulsan-locks-" + InetAddress.getLocalHost().getHostName(),
        "expression-lock-job-"
            + context.getJobUUID() + "-step-"
            + context.getCurrentStep().getNumber());
  }

}
