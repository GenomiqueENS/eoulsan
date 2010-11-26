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

import static fr.ens.transcriptome.eoulsan.data.DataFormats.FILTERED_SOAP_RESULTS_TXT;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_TFQ;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.SOAP_INDEX_ZIP;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.SOAP_RESULTS_TXT;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.UNMAP_READS_FASTA;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.lib.IdentityReducer;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.bio.io.hadoop.FastqInputFormat;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.util.JobsResults;
import fr.ens.transcriptome.eoulsan.util.MapReduceUtils;

/**
 * This class is the main class for the filter and mapping program of the reads
 * in hadoop mode.
 * @author Laurent Jourdren
 */
@SuppressWarnings("deprecation")
@HadoopOnly
public class FilterAndSoapMapReadsHadoopStep extends AbstractStep {

  private static final String STEP_NAME = "filterandmapreads";

  private static final String UNMAP_CHUNK_PREFIX = "soap-unmap-";

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
    return new DataFormat[] {READS_FASTQ, READS_TFQ, SOAP_INDEX_ZIP};
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

  }

  @Override
  public StepResult execute(final Design design, final Context context) {

    // Create the list of jobs to run
    final List<JobConf> jobconfs =
        new ArrayList<JobConf>(design.getSampleCount());
    for (Sample s : design.getSamples())
      jobconfs.add(createJobConf(context, s, getLengthThreshold(),
          getQualityThreshold()));

    try {
      final long startTime = System.currentTimeMillis();

      final JobsResults jobsResults =
          MapReduceUtils.submitAndWaitForRunningJobs(jobconfs,
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
   */
  private static JobConf createJobConf(final Context context,
      final Sample sample, final int lengthThreshold,
      final double qualityThreshold) {

    final JobConf conf = new JobConf(FilterReadsHadoopStep.class);

    final Path inputPath = new Path(context.getBasePathname(), sample.getSource());

    // Set Job name
    conf.setJobName("Filter and map reads with SOAP ("
        + sample.getName() + ", " + inputPath.getName() + ")");

    if (lengthThreshold >= 0)
      conf.set(Globals.PARAMETER_PREFIX + ".filter.reads.length.threshold", ""
          + lengthThreshold);

    if (qualityThreshold >= 0)
      conf.set(Globals.PARAMETER_PREFIX + ".filter.reads.quality.threshold", ""
          + qualityThreshold);

    // Set genome index reference path
    final Path genomeIndex =
        new Path(context.getDataFile(SOAP_INDEX_ZIP, sample).getSource());

    conf.set(Globals.PARAMETER_PREFIX + ".soap.indexzipfilepath", genomeIndex
        .toString());

    DistributedCache.addCacheFile(genomeIndex.toUri(), conf);

    // Set unmap chuck dir path
    conf.set(Globals.PARAMETER_PREFIX + ".soap.unmap.chunk.prefix.dir", context
        .getDataFile(UNMAP_READS_FASTA, sample).getSourceWithoutExtension());

    // Set unmap chuck prefix
    conf.set(Globals.PARAMETER_PREFIX + ".soap.unmap.chunk.prefix",
        UNMAP_CHUNK_PREFIX);

    // Set unmap output file path
    conf.set(Globals.PARAMETER_PREFIX + ".soap.unmap.path", context
        .getDataFilename(UNMAP_READS_FASTA, sample));

    // Set the number of threads for soap
    // conf.set(Globals.PARAMETER_PREFIX + ".soap.nb.threads", ""
    // + Runtime.getRuntime().availableProcessors());

    // Debug
    // conf.set("mapred.job.tracker", "local");

    // timeout
    conf.set("mapred.task.timeout", "" + 30 * 60 * 1000);

    // No JVM task resuse
    conf.setNumTasksToExecutePerJvm(1);

    // Set the jar
    conf.setJarByClass(FilterAndSoapMapReadsHadoopStep.class);

    // Set input path
    FileInputFormat.setInputPaths(conf, inputPath);

    // Set the input format
    if (sample.getSource().endsWith(READS_FASTQ.getDefaultExtention()))
      conf.setInputFormat(FastqInputFormat.class);

    // Set the Mapper class
    conf.setMapperClass(FilterAndSoapMapReadsMapper.class);

    // Set the reducer class
    conf.setReducerClass(IdentityReducer.class);

    // Set the output key class
    conf.setOutputKeyClass(Text.class);

    // Set the output value class
    conf.setOutputValueClass(Text.class);

    // Set the number of reducers
    conf.setNumReduceTasks(1);

    // Set output path
    FileOutputFormat.setOutputPath(conf, new Path(context.getDataFile(
        SOAP_RESULTS_TXT, sample).getSourceWithoutExtension()));

    return conf;
  }
}
