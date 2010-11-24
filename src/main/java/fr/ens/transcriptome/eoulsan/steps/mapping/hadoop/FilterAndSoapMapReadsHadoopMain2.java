/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.steps.mapping.hadoop;

import static fr.ens.transcriptome.eoulsan.datatypes.DataFormats.FILTERED_SOAP_RESULTS_TXT;
import static fr.ens.transcriptome.eoulsan.datatypes.DataFormats.READS_FASTQ;
import static fr.ens.transcriptome.eoulsan.datatypes.DataFormats.READS_TFQ;
import static fr.ens.transcriptome.eoulsan.datatypes.DataFormats.SOAP_INDEX_ZIP;
import static fr.ens.transcriptome.eoulsan.datatypes.DataFormats.SOAP_RESULTS_TXT;
import static fr.ens.transcriptome.eoulsan.datatypes.DataFormats.UNMAP_READS_FASTA;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.io.hadoop.FastQFormatNew;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.ExecutorInfo;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.datatypes.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.util.JobsResults;
import fr.ens.transcriptome.eoulsan.util.MapReduceUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class FilterAndSoapMapReadsHadoopMain2 extends AbstractStep {

  private static final String STEP_NAME = "filterandmapreads2";

  private static final String UNMAP_CHUNK_PREFIX = "soap-unmap-";

  private Configuration conf;
  private int lengthThreshold = -1;
  private double qualityThreshold = -1;
  private String mapperName;

  //
  // Getters
  //

  /**
   * Get the length threshold
   * @return Returns the lengthThreshold
   */
  protected int getLengthThreshold() {
    return lengthThreshold;
  }

  /**
   * Get the quality threshold
   * @return Returns the qualityThreshold
   */
  protected double getQualityThreshold() {
    return qualityThreshold;
  }

  /**
   * Get the name of the mapper to use.
   * @return Returns the mapperName
   */
  protected String getMapperName() {
    return mapperName;
  }

  //
  // Step methods
  //

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public String getDescription() {

    return "This step filters reads.";
  }

  @Override
  public ExecutionMode getExecutionMode() {

    return Step.ExecutionMode.HADOOP;
  }

  @Override
  public DataFormat[] getInputFormats() {
    return new DataFormat[] {READS_FASTQ, READS_TFQ};
  }

  @Override
  public DataFormat[] getOutputFormats() {
    return new DataFormat[] {FILTERED_SOAP_RESULTS_TXT};
  }

  @Override
  public void configure(final Set<Parameter> stepParameters,
      final Set<Parameter> globalParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      if ("lengththreshold".equals(p.getName()))
        this.lengthThreshold = p.getIntValue();
      else if ("qualitythreshold".equals(p.getName()))
        this.qualityThreshold = p.getDoubleValue();
      else if ("mapper".equals(p.getName()))
        this.mapperName = p.getStringValue();
      else
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());

    }

    if (this.mapperName == null)
      throw new EoulsanException("No mapper set.");

    if (!"soap".equals(this.mapperName))
      throw new EoulsanException("Unknown mapper: " + this.mapperName);

    this.conf = CommonHadoop.createConfiguration(globalParameters);
  }

  @Override
  public StepResult execute(final Design design, final ExecutorInfo info) {

    // final Path basePath = new Path(info.getBasePathname());

    // Create configuration object
    final Configuration conf = this.conf;

    try {

      // Create the list of jobs to run
      final List<Job> jobs = new ArrayList<Job>(design.getSampleCount());
      for (Sample s : design.getSamples())
        jobs
            .add(createJobConf(conf, info, s, lengthThreshold, qualityThreshold));

      final long startTime = System.currentTimeMillis();

      final JobsResults jobsResults =
          MapReduceUtils.submitAndWaitForJobs(jobs,
              CommonHadoop.CHECK_COMPLETION_TIME,
              FilterAndSoapMapReadsMapper.COUNTER_GROUP);

      return jobsResults.getStepResult(this, startTime);

    } catch (IOException e) {

      return new StepResult(this, e, "Error while running job: "
          + e.getMessage());
    } catch (InterruptedException e) {

      return new StepResult(this, e, "Error while running job: "
          + e.getMessage());
    } catch (ClassNotFoundException e) {

      return new StepResult(this, e, "Error while running job: "
          + e.getMessage());
    }

  }

  /**
   * Create the JobConf object for a sample
   * @param basePath base path of data
   * @param sample sample to process
   * @return a new JobConf object
   * @throws IOException if an error occurs while creating the job
   */
  private static Job createJobConf(final Configuration parentConf,
      final ExecutorInfo info, final Sample sample, final int lengthThreshold,
      final double qualityThreshold) throws IOException {

    final Configuration jobConf = new Configuration(parentConf);

    final String source = sample.getSource();

    final Path inputPath = new Path(info.getBasePathname(), source);

    if (lengthThreshold >= 0)
      jobConf.set(Globals.PARAMETER_PREFIX + ".filter.reads.length.threshold",
          "" + lengthThreshold);

    if (qualityThreshold >= 0)
      jobConf.set(Globals.PARAMETER_PREFIX + ".filter.reads.quality.threshold",
          "" + qualityThreshold);

    // Set genome reference path
    jobConf.set(Globals.PARAMETER_PREFIX + ".soap.indexzipfilepath", info
        .getDataFile(SOAP_INDEX_ZIP, sample).getSource());

    // Set unmap chuck dir path
    jobConf.set(Globals.PARAMETER_PREFIX + ".soap.unmap.chunk.prefix.dir", info
        .getDataFilename(UNMAP_READS_FASTA, sample));

    // Set unmap chuck prefix
    jobConf.set(Globals.PARAMETER_PREFIX + ".soap.unmap.chunk.prefix",
        UNMAP_CHUNK_PREFIX);

    // Set unmap output file path
    jobConf.set(Globals.PARAMETER_PREFIX + ".soap.unmap.path", info
        .getDataFilename(UNMAP_READS_FASTA, sample));

    // Set the number of threads for soap
    jobConf.set(Globals.PARAMETER_PREFIX + ".soap.nb.threads", ""
        + Runtime.getRuntime().availableProcessors());

    // Debug
    // conf.set("mapred.job.tracker", "local");

    // timeout
    jobConf.set("mapred.task.timeout", "" + 20 * 60 * 1000);

    // Create the job and its name
    final Job job =
        new Job(jobConf, "Filter and map reads with SOAP ("
            + sample.getName() + ", " + inputPath.getName() + ")");

    // Set the jar
    job.setJarByClass(FilterAndSoapMapReadsHadoopMain2.class);

    // Add input path
    FileInputFormat.addInputPath(job, inputPath);

    // Set the input format
    final String sampleExtension =
        StringUtils.extension(StringUtils
            .filenameWithoutCompressionExtension(source));
    if (READS_FASTQ.getDefaultExtention().equals(sampleExtension))
      job.setInputFormatClass(FastQFormatNew.class);
    else
      job.setInputFormatClass(TextInputFormat.class);

    // Set the Mapper class
    job.setMapperClass(FilterAndSoapMapReadsMapper2.class);

    // Set the reducer class
    // job.setReducerClass(IdentityReducer.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Set the number of reducers
    job.setNumReduceTasks(1);

    // Set the output Path
    FileOutputFormat.setOutputPath(job, new Path(info.getDataFile(
        SOAP_RESULTS_TXT, sample).getSourceWithoutExtension()));

    return job;
  }
}
