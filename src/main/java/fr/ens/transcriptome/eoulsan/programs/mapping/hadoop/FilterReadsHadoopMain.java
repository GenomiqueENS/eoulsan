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

package fr.ens.transcriptome.eoulsan.programs.mapping.hadoop;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.hadoop.fs.FsUrlStreamHandlerFactory;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.lib.IdentityReducer;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.io.DesignReader;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.io.SimpleDesignReader;
import fr.ens.transcriptome.eoulsan.io.hadoop.FastqInputFormat;
import fr.ens.transcriptome.eoulsan.util.MapReduceUtils;

/**
 * This class is the main class for the filter reads program in hadoop mode.
 * @author Laurent Jourdren
 */
@SuppressWarnings("deprecation")
public class FilterReadsHadoopMain {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  // Configure URL handler for hdfs protocol
  static {
    URL.setURLStreamHandlerFactory(new FsUrlStreamHandlerFactory());
  }

  private static JobConf createJobConf(final Path basePath,
      final Sample sample, final int lengthThreshold,
      final double qualityThreshold) {

    final JobConf conf = new JobConf(FilterReadsHadoopMain.class);

    if (lengthThreshold >= 0)
      conf.set(Globals.PARAMETER_PREFIX + ".filter.reads.length.threshold", ""
          + lengthThreshold);

    if (qualityThreshold >= 0)
      conf.set(Globals.PARAMETER_PREFIX + ".filter.reads.quality.threshold", ""
          + qualityThreshold);

    // Set Job name
    conf.setJobName("Filter reads ("
        + sample.getName() + ", " + sample.getSource() + ")");

    // Debug
    // conf.set("mapred.job.tracker", "local");

    // Set the jar
    conf.setJarByClass(FilterReadsHadoopMain.class);

    // Set input path
    FileInputFormat.setInputPaths(conf, new Path(basePath, sample.getSource()));

    // Set the input format
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
        + Common.getSampleId(sample)));

    return conf;
  }

  //
  // Main method
  //

  /**
   * Main method
   * @param args command line arguments
   */
  public static void main(final String[] args) {

    logger.info("Start filter reads.");

    if (args == null)
      throw new NullPointerException("The arguments of import data is null");

    if (args.length < 1)
      throw new IllegalArgumentException(
          "Filter reads need one or two arguments");

    // Set the design path
    final String designPathname = args[0];

    // Set the thresholds
    int lengthThreshold = -1;
    double qualityThreshold = -1;

    if (args.length > 1)
      try {
        lengthThreshold = Integer.parseInt(args[1]);
      } catch (NumberFormatException e) {
        CommonHadoop.error("Invalid length threshold: " + args[1]);
      }

    if (args.length > 2)
      try {
        qualityThreshold = Double.parseDouble(args[2]);
      } catch (NumberFormatException e) {
        CommonHadoop.error("Invalid quality threshold: " + args[2]);
      }

    final Path designPath = new Path(designPathname);
    final Path basePath = designPath.getParent();
    Design design = null;

    try {

      final DesignReader dr =
          new SimpleDesignReader(designPath.toUri().toURL().openStream());

      design = dr.read();

    } catch (IOException e) {
      CommonHadoop.error("Error while reading design file: ", e);
    } catch (EoulsanIOException e) {
      CommonHadoop.error("Error while reading design file: ", e);
    }

    // Create the list of jobs to run
    final List<JobConf> jobconfs =
        new ArrayList<JobConf>(design.getSampleCount());
    for (Sample s : design.getSamples())
      jobconfs
          .add(createJobConf(basePath, s, lengthThreshold, qualityThreshold));

    try {
      final long startTime = System.currentTimeMillis();
      CommonHadoop.writeLog(new Path(basePath, "filterreads.log"), startTime,
          MapReduceUtils.submitandWaitForJobs(jobconfs,
              CommonHadoop.CHECK_COMPLETION_TIME,
              FilterReadsMapper.COUNTER_GROUP));

    } catch (IOException e) {
      CommonHadoop.error("Error while running job: ", e);
    } catch (InterruptedException e) {
      CommonHadoop.error("Error while running job: ", e);
    } catch (ClassNotFoundException e) {
      CommonHadoop.error("Error while running job: ", e);
    }

  }
}
