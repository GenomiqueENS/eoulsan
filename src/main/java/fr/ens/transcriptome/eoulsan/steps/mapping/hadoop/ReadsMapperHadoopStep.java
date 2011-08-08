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

import static fr.ens.transcriptome.eoulsan.data.DataFormats.FILTERED_READS_TFQ;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsMapperStep;
import fr.ens.transcriptome.eoulsan.util.JobsResults;
import fr.ens.transcriptome.eoulsan.util.MapReduceUtils;

@HadoopOnly
public class ReadsMapperHadoopStep extends AbstractReadsMapperStep {

  @Override
  public DataFormat[] getInputFormats() {
    return new DataFormat[] {FILTERED_READS_TFQ, getMapper().getArchiveFormat()};
  }

  @Override
  public StepResult execute(final Design design, final Context context) {

    // Create configuration object
    final Configuration conf = new Configuration(false);

    try {

      // Create the list of jobs to run
      final List<Job> jobs = new ArrayList<Job>(design.getSampleCount());
      for (Sample s : design.getSamples())
        jobs.add(createJobConf(conf, context, s));

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
  private Job createJobConf(final Configuration parentConf,
      final Context context, final Sample sample) throws IOException {

    final Configuration jobConf = new Configuration(parentConf);

    // final JobConf conf = new JobConf(FilterReadsHadoopStep.class);

    // final Path inputPath =
    // new Path(context.getBasePathname(), sample.getSource());
    final Path inputPath =
        new Path(
            context.getDataFilename(DataFormats.FILTERED_READS_TFQ, sample));

    // Set genome index reference path
    final Path genomeIndex =
        new Path(context.getDataFile(getMapper().getArchiveFormat(), sample)
            .getSource());

    DistributedCache.addCacheFile(genomeIndex.toUri(), jobConf);

    // Set Mapper name
    jobConf.set(ReadsMapperMapper.MAPPER_NAME_KEY, getMapperName());

    // Set pair end or single end mode
    jobConf.set(ReadsMapperMapper.PAIR_END_KEY, Boolean.FALSE.toString());

    // Set the number of threads for the mapper
    if (getMapperThreads() < 0) {
      jobConf
          .set(ReadsMapperMapper.MAPPER_THREADS_KEY, "" + getMapperThreads());
    }

    // Set mapper arguments
    if (getMapperArguments() != null) {
      jobConf.set(ReadsMapperMapper.MAPPER_ARGS_KEY, getMapperArguments());
    }

    // Set Mapper fastq format
    jobConf.set(ReadsMapperMapper.FASTQ_FORMAT_KEY, ""
        + sample.getMetadata().getFastqFormat());

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, COUNTER_GROUP);

    // Debug
    // jobConf.set("mapred.job.tracker", "local");

    // timeout
    jobConf.set("mapred.task.timeout", "" + HADOOP_TIMEOUT);

    // No JVM task resuse
    jobConf.set("mapred.job.reuse.jvm.num.tasks", "" + 1);

    // Create the job and its name
    final Job job =
        new Job(jobConf, "Map reads with "
            + getMapperName() + " (" + sample.getName() + ", "
            + inputPath.getName() + ")");

    // Set the jar
    job.setJarByClass(ReadsMapperHadoopStep.class);

    // Set input path
    FileInputFormat.addInputPath(job, inputPath);

    // Set the Mapper class
    job.setMapperClass(ReadsMapperMapper.class);

    // Set the reducer class
    // job.setReducerClass(IdentityReducer.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Set the number of reducers
    // job.setNumReduceTasks(1);

    // Set output path
    FileOutputFormat.setOutputPath(job,
        new Path(context.getDataFile(MAPPER_RESULTS_SAM, sample).getSource()));

    return job;
  }

}
