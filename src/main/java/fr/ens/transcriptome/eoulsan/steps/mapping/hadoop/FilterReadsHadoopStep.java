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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.lib.IdentityReducer;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.io.hadoop.FastqInputFormat;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.ExecutorInfo;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.datatypes.DataFormat;
import fr.ens.transcriptome.eoulsan.datatypes.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.mapping.FilterReadsStep;
import fr.ens.transcriptome.eoulsan.util.JobsResults;
import fr.ens.transcriptome.eoulsan.util.MapReduceUtils;

/**
 * This class is the main class for the filter reads program in hadoop mode.
 * @author Laurent Jourdren
 */
@SuppressWarnings("deprecation")
public class FilterReadsHadoopStep extends FilterReadsStep {

  //
  // Step methods
  // 

  @Override
  public ExecutionMode getExecutionMode() {

    return Step.ExecutionMode.HADOOP;
  }

  @Override
  public String getLogName() {

    return "filterreads";
  }

  @Override
  public DataFormat[] getInputFormats() {
    return new DataFormat[] {DataFormats.READS_FASTQ, DataFormats.READS_TFQ};
  }

  @Override
  public DataFormat[] getOutputFormats() {
    return new DataFormat[] {DataFormats.FILTERED_READS_FASTQ};
  }

  @Override
  public StepResult execute(Design design, final ExecutorInfo info) {

    // Set the base path
    final Path basePath = new Path(info.getBasePathname());

    // Create the list of jobs to run
    final List<JobConf> jobconfs =
        new ArrayList<JobConf>(design.getSampleCount());
    for (Sample s : design.getSamples())
      jobconfs.add(createJobConf(basePath, s));

    try {
      final long startTime = System.currentTimeMillis();

      final JobsResults jobsResults =
          MapReduceUtils.submitAndWaitForRunningJobs(jobconfs,
              CommonHadoop.CHECK_COMPLETION_TIME,
              FilterReadsMapper.COUNTER_GROUP);

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
   * Create a filter reads job
   * @param basePath bas epath
   * @param sample Sample to filter
   * @return a JobConf object
   */
  private JobConf createJobConf(final Path basePath, final Sample sample) {

    final JobConf conf = new JobConf(FilterReadsHadoopStep.class);

    if (getLengthThreshold() >= 0)
      conf.set(Globals.PARAMETER_PREFIX + ".filter.reads.length.threshold", ""
          + getLengthThreshold());

    if (getQualityThreshold() >= 0)
      conf.set(Globals.PARAMETER_PREFIX + ".filter.reads.quality.threshold", ""
          + getQualityThreshold());

    // Set Job name
    conf.setJobName("Filter reads ("
        + sample.getName() + ", " + sample.getSource() + ")");

    // Debug
    // conf.set("mapred.job.tracker", "local");

    // Set the jar
    conf.setJarByClass(FilterReadsHadoopStep.class);

    // Set input path
    FileInputFormat.setInputPaths(conf, new Path(basePath, sample.getSource()));

    // Set the input format
    if (sample.getSource().endsWith(CommonHadoop.FASTQ_EXTENSION))
      conf.setInputFormat(FastqInputFormat.class);

    // Set the Mapper class
    conf.setMapperClass(FilterReadsMapper.class);

    // Set the reducer class
    conf.setReducerClass(IdentityReducer.class);

    // Set the output key class
    conf.setOutputKeyClass(Text.class);

    // Set the output value class
    conf.setOutputValueClass(Text.class);

    // Set the number of reducers
    conf.setNumReduceTasks(1);

    // Set output path
    FileOutputFormat.setOutputPath(conf, new Path(basePath, "sample_filtered_"
        + sample.getId()));

    return conf;
  }

}
