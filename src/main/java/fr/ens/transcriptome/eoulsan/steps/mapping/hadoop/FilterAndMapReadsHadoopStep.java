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
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_TFQ;
import static fr.ens.transcriptome.eoulsan.steps.mapping.hadoop.HadoopMappingUtils.addParametersToJobConf;
import static fr.ens.transcriptome.eoulsan.steps.mapping.hadoop.ReadsFilterMapper.READ_FILTER_PARAMETER_KEY_PREFIX;
import static fr.ens.transcriptome.eoulsan.steps.mapping.hadoop.ReadsMapperHadoopStep.setZooKeeperJobConfiguration;
import static fr.ens.transcriptome.eoulsan.steps.mapping.hadoop.SAMFilterReducer.MAP_FILTER_PARAMETER_KEY_PREFIX;

import java.io.IOException;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.chain.ChainMapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.io.hadoop.FastqInputFormat;
import fr.ens.transcriptome.eoulsan.bio.io.hadoop.SAMOutputFormat;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
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
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    super.configure(context, stepParameters);

    // Check if the mapper can be used with Hadoop
    if (!getMapper().isSplitsAllowed()) {
      throw new EoulsanException(
          "The selected mapper cannot be used in hadoop mode as "
              + "computation cannot be parallelized: "
              + getMapper().getMapperName());
    }
  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    // Create configuration object
    final Configuration conf = new Configuration();// this.conf;

    try {

      // Get input and output data
      final Data readData = context.getInputData(READS_FASTQ);
      final DataFile samFile =
          context.getOutputData(MAPPER_RESULTS_SAM, readData).getDataFile();
      final DataFile mapperIndex =
          context.getInputData(MAPPER_INDEX_PORT_NAME).getDataFile();
      final DataFile genomeDescFile =
          context.getInputData(GENOME_DESCRIPTION_PORT_NAME).getDataFile();

      // Get FASTQ format
      final FastqFormat fastqFormat = readData.getMetadata().getFastqFormat();

      // The job to run
      final Job job;

      DataFile tfqFile = null;

      // Preprocess paired-end files
      if (readData.getDataFileCount() == 1) {
        job =
            createJobConf(conf, context, readData.getDataFile(0),
                readData.getName(), false, READS_FASTQ, fastqFormat,
                mapperIndex, genomeDescFile, samFile);
      } else {

        final DataFile inFile1 = readData.getDataFile(0);
        final DataFile inFile2 = readData.getDataFile(1);

        tfqFile =
            new DataFile(inFile1.getParent(), inFile1.getBasename()
                + READS_TFQ.getDefaultExtension());

        // Convert FASTQ files to TFQ
        MapReduceUtils.submitAndWaitForJob(
            PairedEndFastqToTfq.convert(conf, inFile1, inFile2, tfqFile),
            CommonHadoop.CHECK_COMPLETION_TIME);

        job =
            createJobConf(conf, context, tfqFile, readData.getName(), true,
                READS_TFQ, fastqFormat, mapperIndex, genomeDescFile, samFile);
      }

      // Submit filter and map job
      MapReduceUtils.submitAndWaitForJob(job, readData.getName(),
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

  private Job createJobConf(final Configuration parentConf,
      final StepContext context, final DataFile inFile, final String dataName,
      final boolean pairedEnd, final DataFormat inputFormat,
      final FastqFormat fastqFormat, final DataFile genomeIndexFile,
      final DataFile genomeDescFile, final DataFile outFile) throws IOException {

    final Configuration jobConf = new Configuration(parentConf);

    // Set input path
    final Path inputPath = new Path(inFile.getSource());

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, getCounterGroup());

    //
    // Reads filters parameters
    //

    // Set fastq format
    jobConf.set(ReadsFilterMapper.FASTQ_FORMAT_KEY, fastqFormat.getName());

    // Set read filter parameters
    addParametersToJobConf(getReadFilterParameters(),
        READ_FILTER_PARAMETER_KEY_PREFIX, jobConf);

    //
    // Reads mapping parameters
    //

    // Set mapper name
    jobConf.set(ReadsMapperMapper.MAPPER_NAME_KEY, getMapperName());

    // Set mapper version
    jobConf.set(ReadsMapperMapper.MAPPER_VERSION_KEY, getMapperVersion());

    // Set mapper flavor
    jobConf.set(ReadsMapperMapper.MAPPER_FLAVOR_KEY, getMapperFlavor());

    // Set pair end or single end mode
    jobConf.set(ReadsMapperMapper.PAIR_END_KEY, Boolean.toString(pairedEnd));

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
    jobConf.set(ReadsMapperMapper.FASTQ_FORMAT_KEY, "" + fastqFormat);

    // timeout
    jobConf.set("mapreduce.task.timeout", "" + HADOOP_TIMEOUT);

    // Don't reuse JVM
    jobConf.set("mapreduce.job.jvm.numtasks", "" + 1);

    // Set the memory required by the reads mapper
    jobConf
        .set("mapreduce.map.memory.mb", "" + getMapperHadoopMemoryRequired());

    // Set ZooKeeper client configuration
    setZooKeeperJobConfiguration(jobConf, context);

    //
    // Alignment filtering
    //

    // Set Genome description path
    jobConf.set(SAMFilterMapper.GENOME_DESC_PATH_KEY,
        genomeDescFile.getSource());

    // Set SAM filter parameters
    addParametersToJobConf(getAlignmentsFilterParameters(),
        MAP_FILTER_PARAMETER_KEY_PREFIX, jobConf);

    //
    // Job creation
    //

    // Create the job and its name
    final Job job =
        Job.getInstance(jobConf, "Filter and map reads ("
            + dataName + ", " + inFile.getSource() + ")");

    // Set the jar
    job.setJarByClass(ReadsFilterHadoopStep.class);

    // Set input path
    FileInputFormat.addInputPath(job, inputPath);

    // Add genome mapper index to distributed cache

    // Set genome index reference path in the distributed cache
    final Path genomeIndex = new Path(genomeIndexFile.getSource());
    job.addCacheFile(genomeIndex.toUri());

    // Set the input format
    if (inputFormat == READS_FASTQ) {
      job.setInputFormatClass(FastqInputFormat.class);
    } else {
      job.setInputFormatClass(KeyValueTextInputFormat.class);
    }

    // Set the Mappers classes using a chain mapper
    ChainMapper.addMapper(job, ReadsFilterMapper.class, Text.class, Text.class,
        Text.class, Text.class, jobConf);
    ChainMapper.addMapper(job, ReadsMapperMapper.class, Text.class, Text.class,
        Text.class, Text.class, jobConf);
    ChainMapper.addMapper(job, SAMFilterMapper.class, Text.class, Text.class,
        Text.class, Text.class, jobConf);

    // Set the reducer class
    job.setReducerClass(SAMFilterReducer.class);

    // Set the output format
    job.setOutputFormatClass(SAMOutputFormat.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Set output path
    FileOutputFormat.setOutputPath(job, new Path(outFile.getSource()));

    return job;
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

    if (inputDataFile == null) {
      throw new IOException("No input file found.");
    }

    // Set input path
    final Path inputPath = new Path(inputDataFile.getSource());

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, getCounterGroup());

    // timeout
    jobConf.set("mapreduce.task.timeout", "" + HADOOP_TIMEOUT);

    // Don't reuse JVM
    jobConf.set("mapreduce.job.jvm.numtasks", "" + 1);

    //
    // Reads filters parameters
    //

    // Set reads filter fastq format
    jobConf.set(ReadsFilterMapper.FASTQ_FORMAT_KEY, readsData.getMetadata()
        .getFastqFormat().getName());

    // Set read filters parameters
    addParametersToJobConf(getReadFilterParameters(),
        READ_FILTER_PARAMETER_KEY_PREFIX, jobConf);

    //
    // Reads mapping parameters
    //

    // Set Mapper name
    jobConf.set(ReadsMapperMapper.MAPPER_NAME_KEY, getMapperName());

    // Set mapper version
    jobConf.set(ReadsMapperMapper.MAPPER_VERSION_KEY, getMapperVersion());

    // Set mapper flavor
    jobConf.set(ReadsMapperMapper.MAPPER_FLAVOR_KEY, getMapperFlavor());

    // Set pair end or single end mode
    if (readsData.getDataFileCount() == 2) {
      jobConf.set(ReadsMapperMapper.PAIR_END_KEY, Boolean.TRUE.toString());
    } else {
      jobConf.set(ReadsMapperMapper.PAIR_END_KEY, Boolean.FALSE.toString());
    }

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
    jobConf.set(ReadsMapperMapper.FASTQ_FORMAT_KEY, readsData.getMetadata()
        .getFastqFormat().getName());

    //
    // Alignment filtering
    //

    // Set read alignments filters parameters
    addParametersToJobConf(getAlignmentsFilterParameters(),
        SAMFilterReducer.MAP_FILTER_PARAMETER_KEY_PREFIX, jobConf);

    // Set Genome description path
    jobConf.set(SAMFilterMapper.GENOME_DESC_PATH_KEY,
        context.getInputData(GENOME_DESCRIPTION_PORT_NAME).getDataFilename());

    // Set ZooKeeper client configuration
    ReadsMapperHadoopStep.setZooKeeperJobConfiguration(jobConf, context);

    // Set Job name
    // Create the job and its name
    final Job job =
        Job.getInstance(jobConf, "Filter and map reads ("
            + readsData.getName() + ", " + inputDataFile.getSource() + ")");

    // Set genome index reference path in the distributed cache
    final Path genomeIndex =
        new Path(context.getInputData(MAPPER_INDEX_PORT_NAME).getDataFilename());
    job.addCacheFile(genomeIndex.toUri());

    // Set the jar
    job.setJarByClass(ReadsFilterHadoopStep.class);

    // Set input path
    FileInputFormat.addInputPath(job, inputPath);

    // Set the input format
    if (READS_FASTQ.equals(inputDataFile.getDataFormat())) {
      job.setInputFormatClass(FastqInputFormat.class);
    }

    // Set the Mappers classes using a chain mapper
    ChainMapper.addMapper(job, ReadsFilterMapper.class, LongWritable.class,
        Text.class, Text.class, Text.class, jobConf);
    ChainMapper.addMapper(job, SAMFilterMapper.class, Text.class, Text.class,
        Text.class, Text.class, jobConf);

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
        new Path(context.getOutputData(DEFAULT_SINGLE_OUTPUT_PORT_NAME,
            readsData).getDataFilename()));

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
    final int inFileCount = readsData.getDataFileCount();

    if (inFileCount < 1) {
      throw new IOException("No input file found.");
    }

    if (inFileCount > 2) {
      throw new IOException(
          "Cannot handle more than 2 reads files at the same time.");
    }

    // Get the source
    final DataFile inputDataFile1 = readsData.getDataFile(0);
    final DataFile inputDataFile2 = readsData.getDataFile(1);

    // Set input path
    final Path inputPath1 = new Path(inputDataFile1.getSource());
    final Path inputPath2 = new Path(inputDataFile2.getSource());

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, getCounterGroup());

    // Set fastq format
    jobConf.set(PreTreatmentMapper.FASTQ_FORMAT_KEY, readsData.getMetadata()
        .getFastqFormat().getName());

    // Set Job name
    // Create the job and its name
    final Job job =
        Job.getInstance(jobConf, "Pretreatment ("
            + readsData.getName() + ", " + inputDataFile1.getSource() + ", "
            + inputDataFile2.getSource() + ")");

    // Set the jar
    job.setJarByClass(ReadsFilterHadoopStep.class);

    // Set input path : paired-end mode so two input files
    FileInputFormat.addInputPath(job, inputPath1);
    FileInputFormat.addInputPath(job, inputPath2);

    // Set the input format
    if (READS_FASTQ.equals(inputDataFile1.getDataFormat())
        && READS_FASTQ.equals(inputDataFile2.getDataFormat())) {
      job.setInputFormatClass(FastqInputFormat.class);
    }

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
