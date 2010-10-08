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

package fr.ens.transcriptome.eoulsan.steps.mgmt.hadoop;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.steps.mgmt.upload.CopyDataSource;
import fr.ens.transcriptome.eoulsan.util.PathUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class DataSourceDistCp {

  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private final Configuration conf;
  private final Path jobPath;

  public static final class DistCpMapper extends
      Mapper<LongWritable, Text, Text, Text> {

    /*
     * (non-Javadoc)
     * @see org.apache.hadoop.mapreduce.Mapper#map(java.lang.Object,
     * java.lang.Object, org.apache.hadoop.mapreduce.Mapper.Context)
     */
    @Override
    protected void map(final LongWritable key, final Text value, Context context)
        throws IOException, InterruptedException {

      final String val = value.toString();

      final int tabPos = val.indexOf('\t');

      if (tabPos == -1)
        return;

      final String srcPathname = val.substring(0, tabPos);
      final Path destPath = new Path(val.substring(tabPos + 1));
      final Configuration conf = context.getConfiguration();
      final FileSystem fs = destPath.getFileSystem(conf);

      logger.info("Start copy " + srcPathname + " to " + destPath + "\n");

      final long startTime = System.currentTimeMillis();

      // Copy the file
      new CopyDataSource(srcPathname, destPath.toString()).copy(fs
          .create(destPath));

      // Compute copy statistics
      final long duration = System.currentTimeMillis() - startTime;
      final FileStatus fStatus = fs.getFileStatus(destPath);
      final long size = fStatus == null ? 0 : fStatus.getLen();
      final double speed =
          size == 0 ? 0 : (double) size / (double) duration * 1000;

      logger.info("End copy "
          + srcPathname + " to " + destPath + " in "
          + StringUtils.toTimeHumanReadable(duration) + " (" + size
          + " bytes, " + ((int) speed) + " bytes/s)\n");
    }

  }

  public void copy(final Map<String, String> entries) throws IOException {

    if (entries == null || entries.size() == 0)
      return;

    final Configuration conf = this.conf;
    final Path tmpInputDir =
        PathUtils.createTempPath(jobPath, "distcp-in-", "", conf);
    final Path tmpOutputDir =
        PathUtils.createTempPath(jobPath, "distcp-out-", "", conf);

    //
    // Create entries for distcp
    // 

    final FileSystem fs = tmpInputDir.getFileSystem(conf);
    fs.mkdirs(tmpInputDir);

    int count = 0;
    for (Map.Entry<String, String> e : entries.entrySet()) {

      count++;
      final Path f = new Path(tmpInputDir, "distcp-" + count + ".cp");

      BufferedWriter bw =
          new BufferedWriter(new OutputStreamWriter(fs.create(f)));

      bw.write(e.getKey() + "\t" + e.getValue() + "\n");
      bw.close();
    }

    final Job job = createJobConf(conf, tmpInputDir, tmpOutputDir);

    try {
      job.waitForCompletion(false);
    } catch (InterruptedException e) {
      throw new EoulsanRuntimeException("Error while distcp: " + e.getMessage());
    } catch (ClassNotFoundException e) {
      throw new EoulsanRuntimeException("Error while distcp: " + e.getMessage());
    }

    // Remove tmp directory
    PathUtils.fullyDelete(tmpInputDir, conf);
    PathUtils.fullyDelete(tmpOutputDir, conf);

    if (!job.isSuccessful())
      throw new IOException("Unable to copy files using DataSourceDistCp.");
  }

  private static Job createJobConf(final Configuration parentConf,
      final Path cpEntriesPath, final Path outputPath) throws IOException {

    final Configuration jobConf = new Configuration(parentConf);

    // timeout
    jobConf.set("mapred.task.timeout", "" + 30 * 60 * 1000);

    // Create the job and its name
    final Job job = new Job(jobConf, "Distcp");

    // Set the jar
    job.setJarByClass(DataSourceDistCp.class);

    // Add input path
    FileInputFormat.addInputPath(job, cpEntriesPath);

    // Set the input format
    job.setInputFormatClass(TextInputFormat.class);

    // Set the Mapper class
    job.setMapperClass(DistCpMapper.class);

    // Set the reducer class
    // job.setReducerClass(IdentityReducer.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Set the number of reducers
    job.setNumReduceTasks(1);

    // Set the output Path
    FileOutputFormat.setOutputPath(job, outputPath);

    return job;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param conf Configuration object
   * @param jobPath the path where create job temporary file
   */
  public DataSourceDistCp(final Configuration conf, final Path jobPath) {

    if (conf == null)
      throw new NullPointerException("The configuration is null");

    if (jobPath == null)
      throw new NullPointerException("The job Path is null");

    this.conf = conf;
    this.jobPath = jobPath;

  }

}
