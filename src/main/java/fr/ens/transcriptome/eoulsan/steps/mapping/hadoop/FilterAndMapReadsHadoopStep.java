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

import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.allPortsRequiredInWorkingDirectory;
import static fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder.DEFAULT_SINGLE_OUTPUT_PORT_NAME;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;
import static fr.ens.transcriptome.eoulsan.steps.mapping.hadoop.HadoopMappingUtils.addParametersToJobConf;
import static fr.ens.transcriptome.eoulsan.steps.mapping.hadoop.ReadsFilterMapper.READ_FILTER_PARAMETER_KEY_PREFIX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.bio.io.hadoop.FastQFormatNew;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.Data;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractFilterAndMapReadsStep;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.hadoop.MapReduceUtils;

/**
 * This class define a Step that filter and map read in Hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopOnly
public class FilterAndMapReadsHadoopStep extends AbstractFilterAndMapReadsStep {

  @Override
  public InputPorts getInputPorts() {

    return allPortsRequiredInWorkingDirectory(super.getInputPorts());
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    super.configure(stepParameters);

    // Check if the mapper can be used with Hadoop
    if (!getMapper().isSplitsAllowed()) {
      throw new EoulsanException(
          "The selected mapper cannot be used in hadoop mode as "
              + "computation cannot be parallelized: "
              + getMapper().getMapperName());
    }
  }

  @Override
  public StepResult execute(final StepContext context,
      final StepStatus status) {

    // Create configuration object
    final Configuration conf = new Configuration();// this.conf;

    try {

      final List<Job> jobsPairedEnd = new ArrayList<Job>();
      final Data readData = context.getInputData(READS_PORT_NAME);

      if (readData.getDataFileCount() == 2) {
          jobsPairedEnd.add(createJobConfPairedEnd(conf, context));
      }

      // Submit job paired end if needed
      MapReduceUtils.submitAndWaitForJobs(jobsPairedEnd,
          CommonHadoop.CHECK_COMPLETION_TIME);

      // Create the list of jobs to run
      final Map<Job, String> jobs = Maps.newHashMap();
      jobs.put(createJobConf(conf, context), readData.getName());

      // Submit filter and map job
      // TODO Remove usage of the next method as there now only one element to process
      MapReduceUtils.submitAndWaitForJobs(jobs,
          CommonHadoop.CHECK_COMPLETION_TIME, status, getCounterGroup());

      return status.createStepResult();

    } catch (IOException e) {

      return status.createStepResult(e,
          "Error while running job: " + e.getMessage());
    } catch (InterruptedException e) {

      return status.createStepResult(e,
          "Error while running job: " + e.getMessage());
    } catch (ClassNotFoundException e) {

      return status.createStepResult(e,
          "Error while running job: " + e.getMessage());
    }
  }

  /**
   * Create a filter reads job
   * @return a JobConf object
   * @throws IOException
   */
  private Job createJobConf(final Configuration parentConf,
      final StepContext context) throws IOException {

    final Configuration jobConf = new Configuration(parentConf);

    // Get input DataFile
    final Data readsData = context.getInputData(READS_PORT_NAME);
    DataFile inputDataFile = readsData.getDataFile();

    if (inputDataFile == null)
      throw new IOException("No input file found.");

    // Set input path
    final Path inputPath = new Path(inputDataFile.getSource());

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, getCounterGroup());

    // timeout
    jobConf.set("mapred.task.timeout", "" + HADOOP_TIMEOUT);

    // Don't reuse JVM
    jobConf.set("mapred.job.reuse.jvm.num.tasks", "" + 1);

    //
    // Reads filters parameters
    //

    // Set reads filter fastq format
    // TODO implements metadata for design values
    jobConf.set(ReadsFilterMapper.FASTQ_FORMAT_KEY, "" + readsData.getMetadata().get("fastq.format"));

    // Set read filters parameters
    addParametersToJobConf(getReadFilterParameters(),
        READ_FILTER_PARAMETER_KEY_PREFIX, jobConf);

    //
    // Reads mapping parameters
    //

    // Set genome index reference path
    final Path genomeIndex =
        new Path(context.getInputData(MAPPER_INDEX_PORT_NAME).getDataFilename());

    DistributedCache.addCacheFile(genomeIndex.toUri(), jobConf);

    // Set Mapper name
    jobConf.set(ReadsMapperMapper.MAPPER_NAME_KEY, getMapperName());

    // Set pair end or single end mode
    if (readsData.getDataFileCount() == 2)
      jobConf.set(ReadsMapperMapper.PAIR_END_KEY, Boolean.TRUE.toString());
    else
      jobConf.set(ReadsMapperMapper.PAIR_END_KEY, Boolean.FALSE.toString());

    // Set the number of threads for the mapper
    if (getMapperHadoopThreads() < 0) {
      jobConf.set(ReadsMapperMapper.MAPPER_THREADS_KEY, ""
          + getMapperHadoopThreads());
    }

    // Set mapper arguments
    if (getMapperArguments() != null) {
      jobConf.set(ReadsMapperMapper.MAPPER_ARGS_KEY, getMapperArguments());
    }

    // Set Mapper fastq format
    jobConf.set(ReadsMapperMapper.FASTQ_FORMAT_KEY, ""
        + readsData.getMetadata().get("fastq.format"));

    //
    // Alignment filtering
    //

    // Set read alignments filters parameters
    addParametersToJobConf(getAlignmentsFilterParameters(),
        SAMFilterReducer.MAP_FILTER_PARAMETER_KEY_PREFIX, jobConf);

    // Set Genome description path
    jobConf.set(SAMFilterMapper.GENOME_DESC_PATH_KEY,
        context.getInputData(GENOME_DESCRIPTION_PORT_NAME).getDataFilename());

    // Set Job name
    // Create the job and its name
    final Job job =
        new Job(jobConf, "Filter and map reads ("
            + readsData.getName() + ", " + inputDataFile.getSource() + ")");

    // Debug
    // conf.set("mapred.job.tracker", "local");

    // Set the jar
    job.setJarByClass(ReadsFilterHadoopStep.class);

    // Set input path
    FileInputFormat.addInputPath(job, inputPath);

    // Set the input format
    if (READS_FASTQ.equals(inputDataFile.getDataFormat()))
      job.setInputFormatClass(FastQFormatNew.class);

    // Set the Mapper class
    job.setMapperClass(FilterAndMapMapper.class);

    // Set the reducer class
    job.setReducerClass(SAMFilterReducer.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Set the number of reducers
    job.setNumReduceTasks(1);

    // Set output path
    FileOutputFormat.setOutputPath(
        job,
        new Path(context.getOutputData(DEFAULT_SINGLE_OUTPUT_PORT_NAME, readsData).getDataFilename()));

    return job;
  }

  /**
   * Create a job for the pretreatment step in case of paired-end data.
   * @return a JobConf object
   * @throws IOException
   */
  private Job createJobConfPairedEnd(final Configuration parentConf,
      final StepContext context) throws IOException {

    final Configuration jobConf = new Configuration(parentConf);

    // get input file count for the sample
    final Data readsData = context.getInputData(DataFormats.READS_FASTQ);
    final int inFileCount =
        readsData.getDataFileCount();

    if (inFileCount < 1)
      throw new IOException("No input file found.");

    if (inFileCount > 2)
      throw new IOException(
          "Cannot handle more than 2 reads files at the same time.");

    // Get the source
    final DataFile inputDataFile1 = readsData.getDataFile(0);
    final DataFile inputDataFile2 = readsData.getDataFile(1);

    // Set input path
    final Path inputPath1 = new Path(inputDataFile1.getSource());
    final Path inputPath2 = new Path(inputDataFile2.getSource());

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, getCounterGroup());

    // Set fastq format
    // TODO Use metadata
    jobConf.set(PreTreatmentMapper.FASTQ_FORMAT_KEY, readsData.getMetadata().get("fastq.format"));

    // Set Job name
    // Create the job and its name
    final Job job =
        new Job(jobConf, "Pretreatment ("
            + readsData.getName() + ", " + inputDataFile1.getSource() + ", "
            + inputDataFile2.getSource() + ")");

    // Set the jar
    job.setJarByClass(ReadsFilterHadoopStep.class);

    // Set input path : paired-end mode so two input files
    FileInputFormat.addInputPath(job, inputPath1);
    FileInputFormat.addInputPath(job, inputPath2);

    // Set the input format
    if (READS_FASTQ.equals(inputDataFile1.getDataFormat())
        && READS_FASTQ.equals(inputDataFile2.getDataFormat()))
      job.setInputFormatClass(FastQFormatNew.class);

    // Set the Mapper class
    job.setMapperClass(PreTreatmentMapper.class);

    // Set the Reducer class
    job.setReducerClass(PreTreatmentReducer.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Output name
    String outputName =
        StringUtils.filenameWithoutExtension(inputPath2.getName());
    outputName = outputName.substring(0, outputName.length() - 1);
    outputName += ".tfq";

    // Set output path
    FileOutputFormat.setOutputPath(job, new Path(inputPath2.getParent(),
        outputName));

    return job;
  }
}
