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

package fr.ens.transcriptome.eoulsan.steps.mgmt;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.BZip2Codec;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.io.hadoop.FastQFormatNew;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.util.JobsResults;
import fr.ens.transcriptome.eoulsan.util.MapReduceUtils;

public class DistDataFileCopy {

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private final Configuration conf;
  private final Path jobPath;
  private final boolean noDir;

  private static class TextMapper extends
      Mapper<LongWritable, Text, Text, NullWritable> {

    private static final Text outKey = new Text("");
    private static final NullWritable outValue = NullWritable.get();

    @Override
    protected void map(LongWritable key, Text value, Context context)
        throws IOException, InterruptedException {

      context.write(value, outValue);
    }

  }

  private static class TextReducer extends
      Reducer<Text, NullWritable, Text, NullWritable> {

    private static final NullWritable outValue = NullWritable.get();

    @Override
    protected void reduce(final Text key, final Iterable<NullWritable> values,
        final Context context) throws IOException, InterruptedException {

      context.write(key, outValue);
    }

  }

  public void copy(final Map<DataFile, DataFile> entries) throws IOException {

    if (entries == null || entries.size() == 0)
      return;

    final Configuration conf = this.conf;

  }

  public static final Job createDecompressionJob(
      final Configuration parentConf, final DataFile inFile,
      final DataFile outFile, final boolean oneFile,
      final Class<? extends InputFormat> inputFormatClass) throws IOException {

    final Configuration jobConf = new Configuration(parentConf);

    final Path destPath = new Path(outFile.toString());
    final FileSystem fs = destPath.getFileSystem(parentConf);

    LOGGER.info("Path: " + destPath);
    LOGGER.info("exists: " + fs.exists(destPath));
    LOGGER.info("is file: " + fs.isFile(destPath));
    LOGGER.info("is dir: " + fs.isDirectory(destPath));

    if (fs.exists(destPath)) {
      fs.delete(destPath, fs.isDirectory(destPath));
    }

    final Path inputPath = new Path(inFile.toString());

    // Debug
    // jobConf.set("mapred.job.tracker", "local");

    // Create the job and its name
    final Job job =
        new Job(jobConf, "Decompress file " + inFile + " to " + outFile);

    // Set the jar
    job.setJarByClass(DistDataFileCopy.class);

    // Set input path
    FileInputFormat.addInputPath(job, inputPath);

    // job.setInputFormatClass(LongWritable.class);

    // Set the input format
    if (inputFormatClass != null)
      job.setInputFormatClass(inputFormatClass);

    // Set the Mapper class
    job.setMapperClass(TextMapper.class);

    // Set the reducer class
    job.setReducerClass(TextReducer.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(NullWritable.class);

    // Set the number of reducers
    if (oneFile)
      job.setNumReduceTasks(1);

    // Set output path
    FileOutputFormat.setOutputPath(job, new Path(outFile.toString()
        + (oneFile ? ".dir" : "")));

    return job;
  }

  public static final Job createCompressionJob(final Configuration parentConf,
      final DataFile inFile, final DataFile outFile, final boolean oneFile,
      final Class<? extends InputFormat> inputFormatClass) throws IOException {

    final Path destPath = new Path(outFile.toString());
    final FileSystem fs = destPath.getFileSystem(parentConf);

    LOGGER.info("Path: " + destPath);
    LOGGER.info("exists: " + fs.exists(destPath));
    LOGGER.info("is file: " + fs.isFile(destPath));
    LOGGER.info("is dir: " + fs.isDirectory(destPath));

    if (fs.exists(destPath)) {
      fs.delete(destPath, fs.isDirectory(destPath));
    }

    final Configuration jobConf = new Configuration(parentConf);

    final Path inputPath = new Path(inFile.toString());

    jobConf.setBoolean("mapred.output.compress", true);
    jobConf.setClass("mapred.output.compression.codec", BZip2Codec.class,
        CompressionCodec.class);

    // Debug
    // jobConf.set("mapred.job.tracker", "local");

    // Create the job and its name
    final Job job =
        new Job(jobConf, "Compress file " + inFile + " to " + outFile);

    // Set the jar
    job.setJarByClass(DistDataFileCopy.class);

    // Set input path
    FileInputFormat.addInputPath(job, inputPath);

    // Set the input format
    if (inputFormatClass != null)
      job.setInputFormatClass(inputFormatClass);

    // Set the Mapper class
    job.setMapperClass(TextMapper.class);

    // Set the reducer class
    job.setReducerClass(TextReducer.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(NullWritable.class);

    // Set the number of reducers
    if (oneFile)
      job.setNumReduceTasks(1);

    // Set output path
    FileOutputFormat.setOutputPath(job, new Path(outFile.toString()
        + (oneFile ? ".dir" : "")));

    return job;
  }

  public static void main(final Configuration conf) throws IOException,
      InterruptedException, ClassNotFoundException {

    final List<Job> jobs = Lists.newArrayList();

    final DataFile src1 =
        new DataFile("hdfs://skadi.ens.fr/user/jourdren/in.zip");
    final DataFile dest1 =
        new DataFile("hdfs://skadi.ens.fr/user/jourdren/soap_index_1.zip");

    final DataFile src2 =
        new DataFile("hdfs://skadi.ens.fr/user/jourdren/toto.bz2");
    final DataFile dest2 =
        new DataFile("hdfs://skadi.ens.fr/user/jourdren/Ant5.tfq");

    final DataFile dest3 =
        new DataFile("hdfs://skadi.ens.fr/user/jourdren/Ant5.tfq.bz2");

    jobs.add(createDecompressionJob(conf, src2, dest2, false, FastQFormatNew.class));

    final JobsResults jobsResults =
        MapReduceUtils.submitAndWaitForJobs(jobs,
            CommonHadoop.CHECK_COMPLETION_TIME, "mycounter");

    jobs.clear();
    jobs.add(createCompressionJob(conf, dest2, dest3, false, null));

    final JobsResults jobsResults2 =
        MapReduceUtils.submitAndWaitForJobs(jobs,
            CommonHadoop.CHECK_COMPLETION_TIME, "mycounter");

  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param conf Configuration object
   * @param jobPath the path where create job temporary file
   */
  public DistDataFileCopy(final Configuration conf, final Path jobPath,
      final boolean noDir) {

    if (conf == null)
      throw new NullPointerException("The configuration is null");

    if (jobPath == null)
      throw new NullPointerException("The job Path is null");

    this.conf = conf;
    this.jobPath = jobPath;
    this.noDir = noDir;

  }

}
