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

package fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop;

import static com.google.common.collect.Lists.newArrayList;
import static fr.ens.biologie.genomique.eoulsan.CommonHadoop.createConfiguration;
import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.allPortsRequiredInWorkingDirectory;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.READS_FASTQ;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.READS_TFQ;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.HadoopMappingUtils.addParametersToJobConf;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.ReadsFilterMapper.READ_FILTER_PARAMETER_KEY_PREFIX;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.ReadsMapperHadoopModule.computeZipCheckSum;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.ReadsMapperHadoopModule.setZooKeeperJobConfiguration;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.SAMFilterReducer.MAP_FILTER_PARAMETER_KEY_PREFIX;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.chain.ChainMapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.google.common.base.Joiner;

import fr.ens.biologie.genomique.eoulsan.CommonHadoop;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.annotations.HadoopOnly;
import fr.ens.biologie.genomique.eoulsan.bio.FastqFormat;
import fr.ens.biologie.genomique.eoulsan.bio.io.hadoop.FastqInputFormat;
import fr.ens.biologie.genomique.eoulsan.bio.io.hadoop.SAMOutputFormat;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.modules.mapping.AbstractFilterAndMapReadsModule;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.MapReduceUtils;

/**
 * This class define a Step that filter and map read in Hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopOnly
public class FilterAndMapReadsHadoopModule
    extends AbstractFilterAndMapReadsModule {

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
      Modules.invalidConfiguration(context,
          "The selected mapper cannot be used in hadoop mode as "
              + "computation cannot be parallelized: " + getMapper().getName());
    }
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    // Create configuration object
    final Configuration conf = createConfiguration();

    try {

      // Get input and output data
      final Data readsData = context.getInputData(READS_FASTQ);
      final String dataName = readsData.getName();

      final DataFile samFile =
          context.getOutputData(MAPPER_RESULTS_SAM, readsData).getDataFile();
      final DataFile mapperIndex =
          context.getInputData(MAPPER_INDEX_PORT_NAME).getDataFile();

      // Get FASTQ format
      final FastqFormat fastqFormat = readsData.getMetadata().getFastqFormat();

      // The job to run
      final Job job;

      DataFile tfqFile = null;

      // Pre-process paired-end files
      if (readsData.getDataFileCount() == 1) {

        // Define input and output files
        final DataFile inFile = readsData.getDataFile(0);
        final List<String> filenames = singletonList(inFile.getName());

        job = createJobConf(conf, context, dataName, inFile, filenames, false,
            READS_FASTQ, fastqFormat, mapperIndex, samFile);

      } else {

        // Define input and output files
        final DataFile inFile1 = readsData.getDataFile(0);
        final DataFile inFile2 = readsData.getDataFile(1);
        final List<String> filenames =
            newArrayList(inFile1.getName(), inFile2.getName());

        tfqFile = new DataFile(inFile1.getParent(),
            inFile1.getBasename() + READS_TFQ.getDefaultExtension());

        // Convert FASTQ files to TFQ
        MapReduceUtils.submitAndWaitForJob(
            PairedEndFastqToTfq.convert(conf, inFile1, inFile2, tfqFile,
                getReducerTaskCount()),
            readsData.getName(), CommonHadoop.CHECK_COMPLETION_TIME, status,
            getCounterGroup());

        job = createJobConf(conf, context, dataName, tfqFile, filenames, true,
            READS_TFQ, fastqFormat, mapperIndex, samFile);
      }

      // Submit filter and map job
      MapReduceUtils.submitAndWaitForJob(job, readsData.getName(),
          CommonHadoop.CHECK_COMPLETION_TIME, status, getCounterGroup());

      return status.createTaskResult();

    } catch (IOException | EoulsanException e) {

      return status.createTaskResult(e,
          "Error while running job: " + e.getMessage());
    }
  }

  private Job createJobConf(final Configuration parentConf,
      final TaskContext context, final String dataName, final DataFile inFile,
      final List<String> filenames, final boolean pairedEnd,
      final DataFormat inputFormat, final FastqFormat fastqFormat,
      final DataFile genomeIndexFile, final DataFile outFile)
      throws IOException {

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
      jobConf.set(ReadsMapperMapper.MAPPER_THREADS_KEY,
          "" + getMapperHadoopThreads());
    }

    // Set mapper arguments
    if (getMapperArguments() != null) {
      jobConf.set(ReadsMapperMapper.MAPPER_ARGS_KEY, getMapperArguments());
    }

    // Set Mapper fastq format
    jobConf.set(ReadsMapperMapper.FASTQ_FORMAT_KEY, "" + fastqFormat);

    // Set mapper index checksum
    jobConf.set(ReadsMapperMapper.INDEX_CHECKSUM_KEY,
        "" + computeZipCheckSum(genomeIndexFile, parentConf));

    // timeout
    jobConf.set("mapreduce.task.timeout", "" + HADOOP_TIMEOUT);

    // Don't reuse JVM
    jobConf.set("mapreduce.job.jvm.numtasks", "" + 1);

    // Set the memory required by the reads mapper
    jobConf.set("mapreduce.map.memory.mb",
        "" + getMapperHadoopMemoryRequired());

    // Set the memory required by JVM (BWA need more memory than the other
    // mapper for buffering named pipes)
    jobConf.set("mapreduce.map.java.opts", "-Xmx4096M");

    // Set ZooKeeper client configuration
    setZooKeeperJobConfiguration(jobConf, context);

    //
    // Alignment filtering
    //

    // Set SAM filter parameters
    addParametersToJobConf(getAlignmentsFilterParameters(),
        MAP_FILTER_PARAMETER_KEY_PREFIX, jobConf);

    //
    // Job creation
    //

    // Create the job and its name
    final Job job = Job.getInstance(jobConf, "Filter and map reads ("
        + dataName + ", " + Joiner.on(", ").join(filenames) + ")");

    // Set the jar
    job.setJarByClass(ReadsFilterHadoopModule.class);

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

}
