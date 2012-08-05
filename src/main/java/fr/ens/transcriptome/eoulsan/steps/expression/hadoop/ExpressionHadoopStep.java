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
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.steps.expression.hadoop;

import static fr.ens.transcriptome.eoulsan.data.DataFormats.ANNOTATION_INDEX_SERIAL;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TXT;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.steps.expression.AbstractExpressionStep;
import fr.ens.transcriptome.eoulsan.steps.expression.FinalExpressionTranscriptsCreator;
import fr.ens.transcriptome.eoulsan.steps.expression.TranscriptAndExonFinder;
import fr.ens.transcriptome.eoulsan.steps.mapping.hadoop.ReadsMapperHadoopStep;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.hadoop.JobsResults;
import fr.ens.transcriptome.eoulsan.util.hadoop.MapReduceUtils;
import fr.ens.transcriptome.eoulsan.util.hadoop.NewAPIJobsResults;
import fr.ens.transcriptome.eoulsan.util.hadoop.PathUtils;

/**
 * This class is the main class for the expression program of the reads in
 * hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopOnly
public class ExpressionHadoopStep extends AbstractExpressionStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private Configuration conf;

  /**
   * Create JobConf object.
   * @param basePath base path
   * @param sample sample of the job
   * @param genomicType genomic type
   * @throws IOException if an error occurs while creating job
   * @throws BadBioEntryException if an entry of the annotation file is invalid
   */
  private static final Job createJob(final Configuration parentConf,
      final Context context, final Sample sample, final String genomicType)
      throws IOException, BadBioEntryException {

    final Configuration jobConf = new Configuration(parentConf);

    // Create JobConf
    // final JobConf conf = new JobConf(ExpressionHadoopStep.class);

    final Path inputPath =
        new Path(context.getInputDataFilename(
            DataFormats.FILTERED_MAPPER_RESULTS_SAM, sample));

    // Get annotation DataFile
    final DataFile annotationDataFile =
        context.getInputDataFile(DataFormats.ANNOTATION_GFF, sample);

    LOGGER.fine("sample: " + sample);
    LOGGER.fine("inputPath.getName(): " + inputPath.getName());
    LOGGER.fine("sample.getMetadata(): " + sample.getMetadata());
    LOGGER.fine("annotationDataFile: " + annotationDataFile.getSource());

    jobConf.set("mapred.child.java.opts", "-Xmx1024m");

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, COUNTER_GROUP);

    // Set Genome description path
    jobConf.set(ExpressionMapper.GENOME_DESC_PATH_KEY, context
        .getInputDataFile(DataFormats.GENOME_DESC_TXT, sample).getSource());

    final Path exonsIndexPath =
        new Path(context.getOtherDataFilename(ANNOTATION_INDEX_SERIAL, sample));
    LOGGER.info("exonsIndexPath: " + exonsIndexPath);

    if (!PathUtils.isFile(exonsIndexPath, jobConf))
      createExonsIndex(context, new Path(annotationDataFile.getSource()),
          genomicType, exonsIndexPath, jobConf);

    // Set the path to the exons index
    // conf.set(Globals.PARAMETER_PREFIX + ".expression.exonsindex.path",
    // exonsIndexPath.toString());
    DistributedCache.addCacheFile(exonsIndexPath.toUri(), jobConf);

    // Debug
    // conf.set("mapred.job.tracker", "local");

    // Create the job and its name
    final Job job =
        new Job(jobConf, "Expression computation ("
            + sample.getName() + ", " + inputPath.getName() + ", "
            + annotationDataFile.getSource() + ", " + genomicType + ")");

    // Set the jar
    job.setJarByClass(ReadsMapperHadoopStep.class);

    // Set input path
    FileInputFormat.setInputPaths(job, inputPath);

    // Set the Mapper class
    job.setMapperClass(ExpressionMapper.class);

    // Set the reducer class
    job.setReducerClass(ExpressionReducer.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Set the number of reducers
    // job.setNumReduceTasks(1);

    // Set output path
    FileOutputFormat.setOutputPath(
        job,
        new Path(context.getOutputDataFile(DataFormats.EXPRESSION_RESULTS_TXT,
            sample).getSourceWithoutExtension()
            + ".tmp"));

    return job;
  }

  private static final Job createJobPairedEnd(final Configuration parentConf,
      final Context context, final Sample sample, final String genomicType)
      throws IOException, BadBioEntryException {

    final Configuration jobConf = new Configuration(parentConf);

    // Create JobConf
    // final JobConf conf = new JobConf(ExpressionHadoopStep.class);

    final Path inputPath =
        new Path(context.getInputDataFilename(
            DataFormats.FILTERED_MAPPER_RESULTS_SAM, sample));

    // Get annotation DataFile
    final DataFile annotationDataFile =
        context.getInputDataFile(DataFormats.ANNOTATION_GFF, sample);

    LOGGER.fine("sample: " + sample);
    LOGGER.fine("inputPath.getName(): " + inputPath.getName());
    LOGGER.fine("sample.getMetadata(): " + sample.getMetadata());
    LOGGER.fine("annotationDataFile: " + annotationDataFile.getSource());

    jobConf.set("mapred.child.java.opts", "-Xmx1024m");

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, COUNTER_GROUP);

    // Set Genome description path
    jobConf.set(ExpressionMapper.GENOME_DESC_PATH_KEY, context
        .getInputDataFile(DataFormats.GENOME_DESC_TXT, sample).getSource());

    final Path exonsIndexPath =
        new Path(context.getOtherDataFilename(ANNOTATION_INDEX_SERIAL, sample));
    LOGGER.info("exonsIndexPath: " + exonsIndexPath);

    if (!PathUtils.isFile(exonsIndexPath, jobConf))
      createExonsIndex(context, new Path(annotationDataFile.getSource()),
          genomicType, exonsIndexPath, jobConf);

    // Set the path to the exons index
    // conf.set(Globals.PARAMETER_PREFIX + ".expression.exonsindex.path",
    // exonsIndexPath.toString());
    DistributedCache.addCacheFile(exonsIndexPath.toUri(), jobConf);

    // Debug
    // conf.set("mapred.job.tracker", "local");

    // Create the job and its name
    final Job job =
        new Job(jobConf, "Expression computation ("
            + sample.getName() + ", " + inputPath.getName() + ", "
            + annotationDataFile.getSource() + ", " + genomicType + ")");

    // Set the jar
    job.setJarByClass(ReadsMapperHadoopStep.class);

    // Set input path
    FileInputFormat.setInputPaths(job, inputPath);

    // Set the Mapper class
    job.setMapperClass(ExpressionMapper.class);

    // Set the reducer class
    job.setReducerClass(ExpressionReducer.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Set the number of reducers
    // job.setNumReduceTasks(1);

    // Set output path
    FileOutputFormat.setOutputPath(
        job,
        new Path(context.getOutputDataFile(DataFormats.EXPRESSION_RESULTS_TXT,
            sample).getSourceWithoutExtension()
            + ".tmp"));

    return job;
  }

  /**
   * Create exon index.
   * @param gffPath gff path
   * @param expressionType expression type
   * @param exonsIndexPath output exon index path
   * @param conf configuration object
   * @throws IOException if an error occurs while creating the index
   * @throws BadBioEntryException if an entry of the annotation file is invalid
   */
  private static final Path createExonsIndex(final Context context,
      final Path gffPath, final String expressionType,
      final Path exonsIndexPath, final Configuration conf) throws IOException,
      BadBioEntryException {

    final FileSystem fs = gffPath.getFileSystem(conf);
    final FSDataInputStream is = fs.open(gffPath);

    final TranscriptAndExonFinder ef =
        new TranscriptAndExonFinder(is, expressionType);
    final File exonIndexFile =
        context.getRuntime().createFileInTempDir(
            StringUtils.basename(gffPath.getName())
                + ANNOTATION_INDEX_SERIAL.getDefaultExtention());
    ef.save(exonIndexFile);

    PathUtils.copyLocalFileToPath(exonIndexFile, exonsIndexPath, conf);
    if (!exonIndexFile.delete())
      LOGGER.warning("Can not delete exon index file: "
          + exonIndexFile.getAbsolutePath());

    return exonsIndexPath;
  }

  private static final void createFinalExpressionTranscriptsFile(
      final Context context, final Map<Sample, Job> jobconfs,
      final Configuration conf) throws IOException {

    FinalExpressionTranscriptsCreator fetc = null;

    for (Map.Entry<Sample, Job> e : jobconfs.entrySet()) {

      final Sample sample = e.getKey();
      final Job rj = e.getValue();

      final long readsUsed =
          rj.getCounters().findCounter(COUNTER_GROUP, "reads used").getValue();

      final FileSystem fs =
          new Path(context.getBasePathname()).getFileSystem(conf);

      // Load the annotation index
      final Path exonsIndexPath =
          new Path(
              context.getOtherDataFilename(ANNOTATION_INDEX_SERIAL, sample));
      fetc = new FinalExpressionTranscriptsCreator(fs.open(exonsIndexPath));

      // Set the result path
      final Path resultPath =
          new Path(context.getOtherDataFilename(EXPRESSION_RESULTS_TXT, sample));

      fetc.initializeExpressionResults();

      // Load map-reduce results
      fetc.loadPreResults(
          new DataFile(context
              .getOutputDataFile(EXPRESSION_RESULTS_TXT, sample)
              .getSourceWithoutExtension()
              + ".tmp").open(), readsUsed);

      fetc.saveFinalResults(fs.create(resultPath));
    }

  }

  //
  // Step methods
  //

  @Override
  public void configure(Set<Parameter> stepParameters) throws EoulsanException {

    super.configure(stepParameters);
    this.conf = CommonHadoop.createConfiguration(EoulsanRuntime.getSettings());
  }

  @Override
  public StepResult execute(final Design design, final Context context) {

    // // Create configuration object
    // final Configuration conf = new Configuration(false);
    //
    // // Create the list of jobs to run
    // final Map<Sample, Job> jobsRunning = new HashMap<Sample, Job>();
    //
    // try {
    // final long startTime = System.currentTimeMillis();
    //
    // LOGGER.info("Genomic type: " + getGenomicType());

    // ///////////////////////////////////////////
    if (getCounter().getCounterName().equals("eoulsanCounter"))
      return executeJobEoulsanCounter(design, context);
    else if (getCounter().getCounterName().equals("htseq-count"))
      return executeJobHTSeqCounter(design, context);
    else
      return null;
    // ///////////////////////////////////////////

    // final List<Job> jobsPairedEnd = new ArrayList<Job>();
    // for (Sample s : design.getSamples()) {
    // if (context.getDataFileCount(READS_FASTQ, s) == 2)
    // jobsPairedEnd.add(createJobPairedEnd(conf, context, s,
    // getGenomicType()));
    // }
    //
    // MapReduceUtils.submitAndWaitForJobs(jobsPairedEnd,
    // CommonHadoop.CHECK_COMPLETION_TIME, COUNTER_GROUP);

    // for (Sample s : design.getSamples()) {
    //
    // final Job jconf = createJob(conf, context, s, getGenomicType());
    //
    // jconf.submit();
    // jobsRunning.put(s, jconf);
    // }
    //
    // // Compute map-reduce part of the expression computation
    // final JobsResults jobsResults =
    // new NewAPIJobsResults(jobsRunning.values(),
    // CommonHadoop.CHECK_COMPLETION_TIME, COUNTER_GROUP);
    //
    // final long mapReduceEndTime = System.currentTimeMillis();
    // LOGGER.info("Finish the part of the expression computation in "
    // + ((mapReduceEndTime - startTime) / 1000) + " seconds.");
    //
    // // Create the final expression files
    // createFinalExpressionTranscriptsFile(context, jobsRunning, this.conf);
    //
    // LOGGER.info("Finish the create of the final expression files in "
    // + ((System.currentTimeMillis() - mapReduceEndTime) / 1000)
    // + " seconds.");
    //
    // return jobsResults.getStepResult(context, startTime);
    //
    // } catch (IOException e) {
    //
    // return new StepResult(context, e, "Error while running job: "
    // + e.getMessage());
    // } catch (InterruptedException e) {
    //
    // return new StepResult(context, e, "Error while running job: "
    // + e.getMessage());
    // } catch (BadBioEntryException e) {
    //
    // return new StepResult(context, e, "Invalid annotation entry: "
    // + e.getEntry());
    // } catch (ClassNotFoundException e) {
    // return new StepResult(context, e, "Class not found: " + e.getMessage());
    // }

  }

  public StepResult executeJobEoulsanCounter(final Design design,
      final Context context) {

    // Create configuration object
    final Configuration conf = new Configuration(false);

    // Create the list of jobs to run
    final Map<Sample, Job> jobsRunning = new HashMap<Sample, Job>();

    try {
      final long startTime = System.currentTimeMillis();

      LOGGER.info("Genomic type: " + getGenomicType());

      // final List<Job> jobsPairedEnd = new ArrayList<Job>();
      // for (Sample s : design.getSamples()) {
      // if (context.getDataFileCount(READS_FASTQ, s) == 2)
      // jobsPairedEnd.add(createJobPairedEnd(conf, context, s,
      // getGenomicType()));
      // }
      //
      // MapReduceUtils.submitAndWaitForJobs(jobsPairedEnd,
      // CommonHadoop.CHECK_COMPLETION_TIME, COUNTER_GROUP);

      for (Sample s : design.getSamples()) {

        final Job jconf = createJob(conf, context, s, getGenomicType());

        jconf.submit();
        jobsRunning.put(s, jconf);
      }

      // Compute map-reduce part of the expression computation
      final JobsResults jobsResults =
          new NewAPIJobsResults(jobsRunning.values(),
              CommonHadoop.CHECK_COMPLETION_TIME, COUNTER_GROUP);

      final long mapReduceEndTime = System.currentTimeMillis();
      LOGGER.info("Finish the part of the expression computation in "
          + ((mapReduceEndTime - startTime) / 1000) + " seconds.");

      // Create the final expression files
      createFinalExpressionTranscriptsFile(context, jobsRunning, this.conf);

      LOGGER.info("Finish the create of the final expression files in "
          + ((System.currentTimeMillis() - mapReduceEndTime) / 1000)
          + " seconds.");

      return jobsResults.getStepResult(context, startTime);

    } catch (IOException e) {

      return new StepResult(context, e, "Error while running job: "
          + e.getMessage());
    } catch (InterruptedException e) {

      return new StepResult(context, e, "Error while running job: "
          + e.getMessage());
    } catch (BadBioEntryException e) {

      return new StepResult(context, e, "Invalid annotation entry: "
          + e.getEntry());
    } catch (ClassNotFoundException e) {
      return new StepResult(context, e, "Class not found: " + e.getMessage());
    }
  }

  public StepResult executeJobHTSeqCounter(final Design design,
      final Context context) {

    // Create configuration object
    final Configuration conf = new Configuration(false);

    // Create the list of jobs to run
    final Map<Sample, Job> jobsRunning = new HashMap<Sample, Job>();

    // try {
    // final long startTime = System.currentTimeMillis();
    //
    // LOGGER.info("Genomic type: " + getGenomicType());
    //
    // // final List<Job> jobsPairedEnd = new ArrayList<Job>();
    // // for (Sample s : design.getSamples()) {
    // // if (context.getDataFileCount(READS_FASTQ, s) == 2)
    // // jobsPairedEnd.add(createJobPairedEnd(conf, context, s,
    // // getGenomicType()));
    // // }
    // //
    // // MapReduceUtils.submitAndWaitForJobs(jobsPairedEnd,
    // // CommonHadoop.CHECK_COMPLETION_TIME, COUNTER_GROUP);
    //
    // for (Sample s : design.getSamples()) {
    //
    // final Job jconf = createJob(conf, context, s, getGenomicType());
    //
    // jconf.submit();
    // jobsRunning.put(s, jconf);
    // }
    //
    // // Compute map-reduce part of the expression computation
    // final JobsResults jobsResults =
    // new NewAPIJobsResults(jobsRunning.values(),
    // CommonHadoop.CHECK_COMPLETION_TIME, COUNTER_GROUP);
    //
    // final long mapReduceEndTime = System.currentTimeMillis();
    // LOGGER.info("Finish the part of the expression computation in "
    // + ((mapReduceEndTime - startTime) / 1000) + " seconds.");
    //
    // // Create the final expression files
    // createFinalExpressionTranscriptsFile(context, jobsRunning, this.conf);
    //
    // LOGGER.info("Finish the create of the final expression files in "
    // + ((System.currentTimeMillis() - mapReduceEndTime) / 1000)
    // + " seconds.");
    //
    // return jobsResults.getStepResult(context, startTime);
    //
    // } catch (IOException e) {
    //
    // return new StepResult(context, e, "Error while running job: "
    // + e.getMessage());
    // } catch (InterruptedException e) {
    //
    // return new StepResult(context, e, "Error while running job: "
    // + e.getMessage());
    // } catch (BadBioEntryException e) {
    //
    // return new StepResult(context, e, "Invalid annotation entry: "
    // + e.getEntry());
    // } catch (ClassNotFoundException e) {
    // return new StepResult(context, e, "Class not found: " + e.getMessage());
    // }
    return null;
  }

}
