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

import static fr.ens.transcriptome.eoulsan.core.CommonHadoop.createConfiguration;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_TFQ;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.doubleQuotes;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileChecksum;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.io.hadoop.FastqInputFormat;
import fr.ens.transcriptome.eoulsan.bio.io.hadoop.SAMOutputFormat;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsMapperStep;
import fr.ens.transcriptome.eoulsan.util.hadoop.MapReduceUtils;

/**
 * This class define an mapper step in Hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopOnly
public class ReadsMapperHadoopStep extends AbstractReadsMapperStep {

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();
    builder.addPort(READS_PORT_NAME, READS_FASTQ, true);
    builder.addPort(MAPPER_INDEX_PORT_NAME, getMapper().getArchiveFormat(),
        true);

    return builder.create();
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
    final Configuration conf = createConfiguration();

    try {

      // Get input and output data
      final Data readsData = context.getInputData(READS_FASTQ);
      final String dataName = readsData.getName();

      final DataFile mapperIndexFile =
          context.getInputData(getMapper().getArchiveFormat()).getDataFile();
      final DataFile outFile =
          context.getOutputData(MAPPER_RESULTS_SAM, readsData).getDataFile();

      DataFile tfqFile = null;

      // Get FASTQ format
      final FastqFormat fastqFormat = readsData.getMetadata().getFastqFormat();

      // Create the job to run
      final Job job;

      // Preprocess paired-end files
      if (readsData.getDataFileCount() == 1) {
        job =
            createJobConf(conf, context, dataName, readsData.getDataFile(0),
                false, READS_FASTQ, fastqFormat, mapperIndexFile, outFile);
      } else {

        final DataFile inFile1 = readsData.getDataFile(0);
        final DataFile inFile2 = readsData.getDataFile(1);

        tfqFile =
            new DataFile(inFile1.getParent(), inFile1.getBasename()
                + READS_TFQ.getDefaultExtension());

        // Convert FASTQ files to TFQ
        MapReduceUtils.submitAndWaitForJob(PairedEndFastqToTfq.convert(conf,
            inFile1, inFile2, tfqFile, getReducerTaskCount()), readsData
            .getName(), CommonHadoop.CHECK_COMPLETION_TIME, status,
            COUNTER_GROUP);

        job =
            createJobConf(conf, context, dataName, tfqFile, true, READS_TFQ,
                fastqFormat, mapperIndexFile, outFile);
      }

      // Launch jobs
      MapReduceUtils.submitAndWaitForJob(job, readsData.getName(),
          CommonHadoop.CHECK_COMPLETION_TIME, status, COUNTER_GROUP);

      // Cleanup paired-end
      if (tfqFile != null) {

        final FileSystem fs = FileSystem.get(conf);
        fs.delete(new Path(tfqFile.getSource()), true);
      }

      return status.createStepResult();

    } catch (IOException | EoulsanException e) {

      return status.createStepResult(e,
          "Error while running job: " + e.getMessage());
    }

  }

  /**
   * Create the JobConf object for a sample
   * @param readsData reads data
   * @param inputFormat inputFormat
   * @param fastqFormat FASTQ format
   * @param mapperIndexData mapper index data
   * @param outData output data
   * @return a new JobConf object
   * @throws IOException
   */
  private Job createJobConf(final Configuration parentConf,
      final StepContext context, final String dataName,
      final DataFile readsFile, final boolean pairedEnd,
      final DataFormat inputFormat, final FastqFormat fastqFormat,
      final DataFile mapperIndexFile, final DataFile outFile)
      throws IOException {

    final Configuration jobConf = new Configuration(parentConf);

    final Path inputPath = new Path(readsFile.getSource());

    // Set mapper name
    jobConf.set(ReadsMapperMapper.MAPPER_NAME_KEY, getMapperName());

    // Set mapper version
    jobConf.set(ReadsMapperMapper.MAPPER_VERSION_KEY, getMapperVersion());

    // Set mapper flavor
    jobConf.set(ReadsMapperMapper.MAPPER_FLAVOR_KEY, getMapperFlavor());

    // Set pair end or single end mode
    jobConf.set(ReadsMapperMapper.PAIR_END_KEY, Boolean.toString(pairedEnd));

    // Set the number of threads for the mapper
    if (getMapperLocalThreads() > 0) {
      jobConf.set(ReadsMapperMapper.MAPPER_THREADS_KEY, ""
          + getMapperHadoopThreads());
    }

    // Set mapper arguments
    if (getMapperArguments() != null) {
      jobConf.set(ReadsMapperMapper.MAPPER_ARGS_KEY, doubleQuotes(getMapperArguments()));
    }

    // Set Mapper fastq format
    jobConf.set(ReadsMapperMapper.FASTQ_FORMAT_KEY, "" + fastqFormat);

    // Set mapper index checksum
    jobConf.set(ReadsMapperMapper.INDEX_CHECKSUM_KEY,
        "" + computeZipCheckSum(mapperIndexFile, parentConf));

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, COUNTER_GROUP);

    // timeout
    jobConf.set("mapreduce.task.timeout", "" + HADOOP_TIMEOUT);

    // No JVM task resuse
    jobConf.set("mapreduce.job.jvm.numtasks", "" + 1);

    // Set the memory required by the reads mapper
    jobConf
        .set("mapreduce.map.memory.mb", "" + getMapperHadoopMemoryRequired());

    // Set ZooKeeper client configuration
    setZooKeeperJobConfiguration(jobConf, context);

    // Create the job and its name
    final Job job =
        Job.getInstance(jobConf, "Mapping reads in "
            + fastqFormat + " with " + getMapperName() + " (" + dataName + ", "
            + readsFile.getName() + ")");

    // Set genome index reference path in the distributed cache
    final Path genomeIndex = new Path(mapperIndexFile.getSource());

    job.addCacheFile(genomeIndex.toUri());

    // Set the jar
    job.setJarByClass(ReadsMapperHadoopStep.class);

    // Set input path
    FileInputFormat.addInputPath(job, inputPath);

    // Set the input format
    if (inputFormat == READS_FASTQ) {
      job.setInputFormatClass(FastqInputFormat.class);
    } else {
      job.setInputFormatClass(KeyValueTextInputFormat.class);
    }

    // Set the Mapper class
    job.setMapperClass(ReadsMapperMapper.class);

    // Set the output format
    job.setOutputFormatClass(SAMOutputFormat.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Set the number of reducers
    job.setNumReduceTasks(0);

    // Set output path
    FileOutputFormat.setOutputPath(job, new Path(outFile.getSource()));

    return job;
  }

  /**
   * Configure ZooKeeper client.
   * @param jobConf job configuration
   * @param context Eoulsan context
   */
  static void setZooKeeperJobConfiguration(final Configuration jobConf,
      final StepContext context) {

    final Settings settings = context.getSettings();

    String connectString = settings.getZooKeeperConnectString();

    if (connectString == null) {

      connectString =
          jobConf.get("yarn.resourcemanager.hostname").split(":")[0]
              + ":" + settings.getZooKeeperDefaultPort();

    }

    jobConf.set(ReadsMapperMapper.ZOOKEEPER_CONNECT_STRING_KEY, connectString);
    jobConf.set(ReadsMapperMapper.ZOOKEEPER_SESSION_TIMEOUT_KEY,
        "" + settings.getZooKeeperSessionTimeout());
  }

  /**
   * Compute the checksum of a ZIP file or use the HDFS checksum if available.
   * @param is input stream
   * @return the checksum as a string
   * @throws IOException if an error occurs while creating the checksum
   */
  static String computeZipCheckSum(final DataFile file, final Configuration conf)
      throws IOException {

    final Path path = new Path(file.getSource());

    FileSystem fs = FileSystem.get(path.toUri(), conf);
    final FileChecksum checksum = fs.getFileChecksum(path);

    // If exists use checksum provided by the file system
    if (checksum != null) {
      return new BigInteger(1, checksum.getBytes()).toString(16);
    }

    // Fallback solution
    return computeZipCheckSum(file.open());
  }

  /**
   * Compute the checksum of a ZIP file.
   * @param is input stream
   * @return the checksum as a string
   * @throws IOException if an error occurs while creating the checksum
   */
  private static String computeZipCheckSum(final InputStream in)
      throws IOException {

    ZipArchiveInputStream zais = new ZipArchiveInputStream(in);

    // Create Hash function
    final Hasher hs = Hashing.md5().newHasher();

    // Store entries in a map
    final Map<String, long[]> map = new HashMap<>();

    ZipArchiveEntry e;

    while ((e = zais.getNextZipEntry()) != null) {
      map.put(e.getName(), new long[] {e.getSize(), e.getCrc()});
    }

    zais.close();

    // Add values to hash function in an ordered manner
    for (String filename : new TreeSet<String>(map.keySet())) {

      hs.putString(filename, StandardCharsets.UTF_8);
      for (long l : map.get(filename)) {
        hs.putLong(l);
      }
    }

    return hs.hash().toString();
  }

}
