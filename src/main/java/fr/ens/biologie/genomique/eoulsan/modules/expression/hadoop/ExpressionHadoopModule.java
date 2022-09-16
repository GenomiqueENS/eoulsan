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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
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
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.bio.expressioncounter.ExpressionCounter;
import fr.ens.biologie.genomique.eoulsan.bio.io.hadoop.ExpressionOutputFormat;
import fr.ens.biologie.genomique.eoulsan.bio.io.hadoop.SAMInputFormat;
import fr.ens.biologie.genomique.eoulsan.bio.io.hadoop.SAMOutputFormat;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.modules.expression.AbstractExpressionModule;
import fr.ens.biologie.genomique.eoulsan.modules.expression.ExpressionCounterUtils;
import fr.ens.biologie.genomique.eoulsan.modules.expression.FinalExpressionFeaturesCreator;
import fr.ens.biologie.genomique.kenetre.util.StringUtils;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.MapReduceUtils;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.PathUtils;
import fr.ens.biologie.genomique.eoulsan.util.locker.Locker;
import fr.ens.biologie.genomique.eoulsan.util.locker.ZooKeeperLocker;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

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
    final Data featureAnnotationData = context
        .getInputData(isGTFInputFormat() ? ANNOTATION_GFF : ANNOTATION_GFF);
    final Data genomeDescriptionData = context.getInputData(GENOME_DESC_TXT);
    final Data outData = context.getOutputData(
        isSAMOutputFormat() ? MAPPER_RESULTS_SAM : EXPRESSION_RESULTS_TSV,
        alignmentsData);

    // Create configuration object
    final Configuration conf = createConfiguration();

    try {
      final long startTime = System.currentTimeMillis();

      getLogger().info("Counter: " + getExpressionCounter());

      // Initialize the counter
      initializeCounter(getExpressionCounter(), genomeDescriptionData,
          featureAnnotationData);

      // Get the paired end mode
      boolean pairedEnd = isPairedData(alignmentsData.getDataFile().open());

      // Paired-end pre-processing
      if (pairedEnd) {
        MapReduceUtils.submitAndWaitForJob(
            createPairedEndJob(conf, context, alignmentsData,
                genomeDescriptionData),
            alignmentsData.getName(), CommonHadoop.CHECK_COMPLETION_TIME,
            status, COUNTER_GROUP);
      }

      // Create the expression job
      final Job job = createExpressionJob(conf, context, alignmentsData,
          genomeDescriptionData, featureAnnotationData, outData,
          getExpressionCounter(), pairedEnd);

      // Compute map-reduce part of the expression computation
      MapReduceUtils.submitAndWaitForJob(job, alignmentsData.getName(),
          CommonHadoop.CHECK_COMPLETION_TIME, status, COUNTER_GROUP);

      final long mapReduceEndTime = System.currentTimeMillis();
      getLogger().info("Finish the first part of the expression computation in "
          + ((mapReduceEndTime - startTime) / 1000) + " seconds.");

      // Only for TSV output
      if (!isSAMOutputFormat()) {

        // Create the final expression files
        createFinalExpressionFeaturesFile(context, getExpressionCounter(),
            outData, job, this.conf);

        getLogger().info("Finish the create of the final expression files in "
            + ((System.currentTimeMillis() - mapReduceEndTime) / 1000)
            + " seconds.");
      }

      return status.createTaskResult();

    } catch (IOException e) {

      return status.createTaskResult(e,
          "Error while running job: " + e.getMessage());
    } catch (KenetreException | EoulsanException e) {

      return status.createTaskResult(e,
          "Error while reading the annotation file: " + e.getMessage());
    }
  }

  //
  // Hadoop Jobs creation
  //

  /**
   * Create JobConf object for an expression job.
   * @param parentConf parent configuration
   * @param context the Eoulsan task context
   * @param alignmentsData alignment data
   * @param genomeDescriptionData genome description data
   * @param featureAnnotationData feature annotations data
   * @throws IOException if an error occurs while creating job
   * @throws EoulsanException if an error occurs while initialize the counter
   */
  private static Job createExpressionJob(final Configuration parentConf,
      final TaskContext context, final Data alignmentsData,
      final Data genomeDescriptionData, final Data featureAnnotationData,
      final Data outData, final ExpressionCounter counter,
      final boolean tsamFormat) throws IOException, EoulsanException {

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

    getLogger().fine("sample: " + alignmentsData.getName());
    getLogger().fine("inputPath.getName(): " + inputPath.getName());
    getLogger().fine("annotationDataFile: " + annotationDataFile.getSource());
    getLogger().fine("outFile: " + outFile.getSource());

    jobConf.set("mapred.child.java.opts", "-Xmx1024m");

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, COUNTER_GROUP);

    // Set Genome description path
    final DataFile genomeDescDataFile = genomeDescriptionData.getDataFile();
    jobConf.set(GENOME_DESC_PATH_KEY, genomeDescDataFile.getSource());

    // Define counter serialization file
    final DataFile featureAnnotationFile = featureAnnotationData.getDataFile();
    final Path counterSerializationFilePath =
        new Path(new DataFile(featureAnnotationFile.getParent(),
            featureAnnotationFile.getBasename() + SERIALIZATION_EXTENSION)
                .getSource());

    getLogger()
        .info("counterSerializationFilePath: " + counterSerializationFilePath);

    // Create serialized feature index
    if (!PathUtils.isFile(counterSerializationFilePath, jobConf)) {

      final Locker lock = createZookeeperLock(parentConf, context);

      lock.lock();

      // Serialize the counter
      serializeCounter(context, counter, counterSerializationFilePath, jobConf);

      lock.unlock();
    }

    // Create the job and its name
    final Job job = Job.getInstance(jobConf,
        "Expression computation with htseq-count ("
            + alignmentsData.getName() + ", " + inputPath.getName() + ", "
            + annotationDataFile.getSource() + ")");

    // Set the path to the features index
    job.addCacheFile(counterSerializationFilePath.toUri());

    // Set the jar
    job.setJarByClass(ExpressionHadoopModule.class);

    // Set input path
    FileInputFormat.setInputPaths(job, inputPath);

    // Set input format
    job.setInputFormatClass(SAMInputFormat.class);

    if (MAPPER_RESULTS_SAM.equals(outData.getFormat())) {

      // Set the mapper class for SAM output
      job.setMapperClass(ExpressionSAMOutputMapper.class);

      // Set the number of reducers
      job.setNumReduceTasks(0);

      // Set the output format
      job.setOutputFormatClass(SAMOutputFormat.class);

      // Set the output key class
      job.setOutputKeyClass(Text.class);

      // Set the output value class
      job.setOutputValueClass(Text.class);

      // Set output path
      FileOutputFormat.setOutputPath(job, new Path(outFile.getSource()));

    } else {

      // Set the mapper class for TSV output
      job.setMapperClass(ExpressionMapper.class);

      // Set the combiner class
      job.setCombinerClass(ExpressionReducer.class);

      // Set the reducer class
      job.setReducerClass(ExpressionReducer.class);

      // Set the output format
      job.setOutputFormatClass(ExpressionOutputFormat.class);

      // Set the output key class
      job.setOutputKeyClass(Text.class);

      // Set the output value class
      job.setOutputValueClass(LongWritable.class);

      // Get temporary file
      final DataFile tmpFile =
          new DataFile(outFile.getParent(), outFile.getBasename() + ".tmp");
      getLogger().fine("tmpFile: " + tmpFile.getSource());

      // Set output path
      FileOutputFormat.setOutputPath(job, new Path(tmpFile.getSource()));
    }

    return job;
  }

  /**
   * Create JobConf object for a paired-end job.
   * @param parentConf parent configuration
   * @param context the Eoulsan task context
   * @param alignmentsData alignment data
   * @param genomeDescriptionData genome description data
   * @throws IOException if an error occurs while creating job
   */
  private static Job createPairedEndJob(final Configuration parentConf,
      final TaskContext context, final Data alignmentsData,
      final Data genomeDescriptionData) throws IOException {

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
        StringUtils.filenameWithoutExtension(inputPath.getName())
            + TSAM_EXTENSION;

    // Set output path
    FileOutputFormat.setOutputPath(job,
        new Path(inputPath.getParent(), outputName));

    return job;
  }

  //
  // Counter initialization and serialization methods
  //

  /**
   * Initialize the counter.
   * @param counter the counter to initialize
   * @param genomeDescData the genome description data
   * @param annotationData the annotation data
   * @throws EoulsanException if an error occurs while initialize the counter
   * @throws IOException if an error occurs while reading the input data
   */
  private static void initializeCounter(final ExpressionCounter counter,
      final Data genomeDescData, final Data annotationData)
      throws KenetreException, IOException {

    // Initialize the counter
    ExpressionCounterUtils.init(counter, genomeDescData.getDataFile(),
        annotationData.getDataFile(),
        annotationData.getFormat() == DataFormats.ANNOTATION_GTF);
  }

  /**
   * Serialize a counter object.
   * @param context Eoulsan context
   * @param counter to serialize
   * @param counterSerializationFilePath feature index output path
   * @param conf Hadoop configuration object
   * @throws IOException if an error occurs while creating the counter
   *           serialization file
   * @throws EoulsanException if an error occurs while initialize the counter
   */
  private static void serializeCounter(final TaskContext context,
      final ExpressionCounter counter, final Path counterSerializationFilePath,
      final Configuration conf) throws IOException, EoulsanException {

    // Do nothing if the file already exists
    if (PathUtils.isFile(counterSerializationFilePath, conf)) {
      return;
    }

    // Define the filename of the counter serialization file
    final File counterSerializationFile =
        context.getRuntime().createFileInTempDir(
            counterSerializationFilePath.getName() + SERIALIZATION_EXTENSION);

    // Serialize the counter
    serializeCounter(counter, counterSerializationFile);

    PathUtils.copyLocalFileToPath(counterSerializationFile,
        counterSerializationFilePath, conf);

    if (!counterSerializationFile.delete()) {
      getLogger().warning("Can not delete the counter serialization file: "
          + counterSerializationFile.getAbsolutePath());
    }
  }

  /**
   * Serialize a counter object.
   * @param counter to serialize
   * @param counterSerializationFile feature index output file
   * @throws IOException if an error occurs while creating the the counter
   *           serialization file
   */
  private static void serializeCounter(final ExpressionCounter counter,
      final File counterSerializationFile) throws IOException {

    if (counter == null) {
      throw new NullPointerException("counter argument cannot be null");
    }

    if (counterSerializationFile == null) {
      throw new NullPointerException(
          "featuresIndexFile argument cannot be null");
    }

    try (ObjectOutputStream oos = new ObjectOutputStream(
        new FileOutputStream(counterSerializationFile))) {

      oos.writeObject(counter);
    }
  }

  //
  // Other methods
  //

  /**
   * Create the final expression file.
   * @param context the Eoulsan context
   * @param counter the counter to use
   * @param outData output data
   * @param job Hadoop expression job
   * @param conf Hadoop configuration
   * @throws IOException if an error occurs while creating the final expression
   *           file
   */
  private static void createFinalExpressionFeaturesFile(
      final TaskContext context, final ExpressionCounter counter,
      final Data outData, final Job job, final Configuration conf)
      throws IOException {

    FinalExpressionFeaturesCreator fefc = null;

    fefc = new FinalExpressionFeaturesCreator(counter);

    // Set the result path
    final Path resultPath = new Path(outData.getDataFile().getSource());
    final FileSystem fs = resultPath.getFileSystem(conf);

    fefc.initializeExpressionResults();

    // Load map-reduce results
    fefc.loadPreResults(new DataFile(job.getConfiguration()
        .get("mapreduce.output.fileoutputformat.outputdir")).open());

    fefc.saveFinalResults(fs.create(resultPath));
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

  /**
   * Check if a SAM file contains paired-end data.
   * @param samIs the SAM file input stream
   * @return true if the SAM file contains paired-end data
   */
  public static boolean isPairedData(final InputStream samIs) {

    if (samIs == null) {
      throw new NullPointerException("is argument cannot be null");
    }

    try {
      final SamReader input =
          SamReaderFactory.makeDefault().open(SamInputResource.of(samIs));

      SAMRecordIterator samIterator = input.iterator();

      boolean result = false;

      // Test if input is paired-end data
      if (samIterator.hasNext()) {
        if (samIterator.next().getReadPairedFlag()) {
          result = true;
        }
      }

      // Close input file
      input.close();

      return result;

    } catch (IOException e) {
      return false;
    }
  }

}