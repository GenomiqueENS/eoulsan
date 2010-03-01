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

package fr.ens.transcriptome.eoulsan.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.hadoop.filterreads.ValidReadMapper;
import fr.ens.transcriptome.eoulsan.util.MapReduceUtils;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

public class FilterReads {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private static final String FILTER_READS_OUTPUT_DIR_EXTENSION =
      ".filter.temp.dir";

  public static void main(final String[] args) throws Exception {

    System.out.println("FilterReads arguments:\t" + Arrays.toString(args));

    if (args == null)
      throw new NullPointerException("The arguments of import data is null");

    if (args.length != 1)
      throw new IllegalArgumentException("Import data needs only one argument");

    final String uri = args[0];

    // Create Configuration Object
    final Configuration conf = new Configuration();

    final Path basePath = new Path(uri);
    final Path inputDirPath = new Path(basePath, Common.READS_SUBDIR);

    logger.info("Map reads input path: " + inputDirPath);

    final List<Path> paths =
        PathUtils.listPathsBySuffix(inputDirPath, Common.FASTQ_EXTENSION, true,
            conf);

    final List<Job> jobs = new ArrayList<Job>();

    // Run the filter
    for (Path inputPath : paths) {

      final Path outputPath =
          PathUtils.newPathWithOtherExtension(inputPath,
              FILTER_READS_OUTPUT_DIR_EXTENSION);

      jobs.add(createFilterReadJob(conf, inputPath, outputPath));
    }

    // Runs the jobs
    MapReduceUtils.submitandWaitForJobs(jobs, Common.CHECK_COMPLETION_TIME);

    // Rename the outputs of map reduce
    for (Path inputPath : paths) {

      final Path outputPath =
          PathUtils.newPathWithOtherExtension(inputPath,
              FILTER_READS_OUTPUT_DIR_EXTENSION);
      final Path resultPath =
          PathUtils.newPathWithOtherExtension(inputPath,
              Common.READS_FILTERED_EXTENSION);

      MapReduceUtils.moveMapredOutput(outputPath, resultPath, true, conf);
    }

  }

  /**
   * Create a map reduce job
   * @param conf Configuration
   * @param inputPath input path for reads
   * @param outputPath output directory
   * @return a new job
   * @throws IOException if an error occurs while creating job
   * @throws InterruptedException if an error occurs while creating job
   * @throws ClassNotFoundException if an error occurs while creating job
   */
  private static Job createFilterReadJob(final Configuration conf,
      final Path inputPath, final Path outputPath) throws IOException,
      InterruptedException, ClassNotFoundException {

    final Configuration jobConf = new Configuration(conf);

    // Create the job and its name
    final Job job = new Job(jobConf, "Filter reads in " + inputPath.getName());

    // Set the jar of the map reduce classes
    job.setJarByClass(ReadsFilterMapReduceOld.class);

    // Set the input format
    job.setInputFormatClass(FastQFormat.class);
    // job.setInputFormatClass(TextInputFormat.class);

    // Set the mapper class
    job.setMapperClass(ValidReadMapper.class);

    // Set the map key/values types
    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(Text.class);

    FileInputFormat.addInputPath(job, inputPath);

    // Set the output Path
    FileOutputFormat.setOutputPath(job, outputPath);

    return job;
  }

}
