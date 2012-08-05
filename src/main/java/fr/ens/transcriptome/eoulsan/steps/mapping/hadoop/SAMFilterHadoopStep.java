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

package fr.ens.transcriptome.eoulsan.steps.mapping.hadoop;

import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractSAMFilterStep;
import fr.ens.transcriptome.eoulsan.util.hadoop.JobsResults;
import fr.ens.transcriptome.eoulsan.util.hadoop.MapReduceUtils;

/**
 * This class define a filter alignment step in Hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopOnly
public class SAMFilterHadoopStep extends AbstractSAMFilterStep {

  @Override
  public StepResult execute(final Design design, final Context context) {

    // Create configuration object
    final Configuration conf = new Configuration(false);// this.conf;

    try {

      // Create the list of jobs to run
      final List<Job> jobs = new ArrayList<Job>(design.getSampleCount());
      for (Sample s : design.getSamples())
        jobs.add(createJob(conf, context, s));

      final long startTime = System.currentTimeMillis();

      final JobsResults jobsResults =
          MapReduceUtils.submitAndWaitForJobs(jobs,
              CommonHadoop.CHECK_COMPLETION_TIME, COUNTER_GROUP);

      return jobsResults.getStepResult(context, startTime);

    } catch (IOException e) {

      return new StepResult(context, e, "Error while running job: "
          + e.getMessage());
    } catch (InterruptedException e) {

      return new StepResult(context, e, "Error while running job: "
          + e.getMessage());
    } catch (ClassNotFoundException e) {

      return new StepResult(context, e, "Error while running job: "
          + e.getMessage());
    }

  }

  /**
   * Create the JobConf object for a sample
   * @param basePath base path of data
   * @param sample sample to process
   * @return a new JobConf object
   * @throws IOException
   */
  private Job createJob(final Configuration parentConf, final Context context,
      final Sample sample) throws IOException {

    final Configuration jobConf = new Configuration(parentConf);

    // Set input path
    final Path inputPath =
        new Path(context.getInputDataFile(MAPPER_RESULTS_SAM, sample)
            .getSource());

    // Set Genome description path
    jobConf.set(SAMFilterMapper.GENOME_DESC_PATH_KEY,
        context.getInputDataFile(DataFormats.GENOME_DESC_TXT, sample)
            .getSource());

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, COUNTER_GROUP);

    // Set SAM filter parameters
    for (Map.Entry<String, String> e : getAlignmentsFilterParameters()
        .entrySet())
      jobConf.set(
          SAMFilterReducer.MAP_FILTER_PARAMETER_KEY_PREFIX + e.getKey(),
          e.getValue());

    // timeout
    jobConf.set("mapred.task.timeout", "" + 30 * 60 * 1000);

    // Create the job and its name
    final Job job =
        new Job(jobConf, "Filter SAM files ("
            + sample.getName() + ", " + inputPath.getName() + ")");

    // Set the jar
    job.setJarByClass(ReadsMapperHadoopStep.class);

    // Set input path
    FileInputFormat.addInputPath(job, inputPath);

    // Set the Mapper class
    job.setMapperClass(SAMFilterMapper.class);

    // Set the reducer class
    job.setReducerClass(SAMFilterReducer.class);

    // job.setPartitionerClass(SAMRecordsPartitioner.class);
    // job.setSortComparatorClass(SAMRecordsKeyComparator.class);
    // job.setGroupingComparatorClass(SAMRecordsGroupComparator.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Set the number of reducers
    // job.setNumReduceTasks(9);

    // Set output path
    FileOutputFormat.setOutputPath(
        job,
        new Path(context.getOutputDataFile(
            DataFormats.FILTERED_MAPPER_RESULTS_SAM, sample).getSource()));

    return job;
  }

}
