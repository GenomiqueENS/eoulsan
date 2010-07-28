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
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsUrlStreamHandlerFactory;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.io.DesignReader;
import fr.ens.transcriptome.eoulsan.io.DesignWriter;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.io.LogReader;
import fr.ens.transcriptome.eoulsan.io.SimpleDesignReader;
import fr.ens.transcriptome.eoulsan.io.SimpleDesignWriter;
import fr.ens.transcriptome.eoulsan.util.PathUtils;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class is the main class for filtering samples after mapping in hadoop
 * mode.
 * @author Laurent Jourdren
 */
public class FilterSamplesHadoopMain {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  // Configure URL handler for hdfs protocol
  static {
    URL.setURLStreamHandlerFactory(new FsUrlStreamHandlerFactory());
  }

  private static int threshold = 50;

  private static void filterSamples(final String srcDesignFilename,
      final String destDesignFilename, final double threshold) {

    try {
      filterSamples(new Path(srcDesignFilename), new Path(destDesignFilename),
          threshold);
    } catch (EoulsanIOException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    } catch (IOException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    }
  }

  private static void filterSamples(final Path srcDesignFile,
      final Path destDesignFile, final double threshold) throws IOException,
      EoulsanIOException {

    if (srcDesignFile == null)
      throw new NullPointerException("Source design file is null");

    if (destDesignFile == null)
      throw new NullPointerException("Destination design file is null");

    final Configuration conf = new Configuration();
    final FileSystem fs = srcDesignFile.getFileSystem(conf);

    if (PathUtils.isFile(destDesignFile, conf))
      throw new EoulsanIOException("The output design already exists: "
          + destDesignFile);

    // Read the design file
    final DesignReader dr = new SimpleDesignReader(fs.open(srcDesignFile));
    final Design design = dr.read();

    // Read soapmapreads.log
    LogReader logReader =
        new LogReader(fs.open(new Path(srcDesignFile.getParent(),
            "soapmapreads.log")));
    final Reporter reporter = logReader.read();

    // Compute ration and filter samples
    for (String group : reporter.getCounterGroups()) {

      final int pos1 = group.indexOf('(');
      final int pos2 = group.indexOf(',');

      if (pos1 == -1 || pos2 == -1)
        continue;

      final String sample = group.substring(pos1 + 1, pos2).trim();

      final long inputReads =
          reporter.getCounterValue(group, Common.SOAP_INPUT_READS_COUNTER);
      final long oneLocus =
          reporter.getCounterValue(group,
              Common.SOAP_ALIGNEMENT_WITH_ONLY_ONE_HIT_COUNTER);

      final double ratio = (double) oneLocus / (double) inputReads;
      logger.info("Check Reads with only one match: "
          + sample + " " + oneLocus + "/" + inputReads + "=" + ratio
          + " threshold=" + threshold);

      if (ratio < threshold) {
        design.removeSample(sample);
        logger.info("Remove sample: " + sample);
      }
    }

    // Write output design
    DesignWriter writer = new SimpleDesignWriter(fs.create(destDesignFile));
    writer.write(design);
  }

  //
  // Main method
  //

  /**
   * Main method
   * @param args command line arguments
   */
  public static void main(String[] args) {

    logger.info("Start SOAP map reads.");

    if (args == null)
      throw new NullPointerException("The arguments of import data is null");

    if (args.length != 2)
      throw new IllegalArgumentException("Soap map need two arguments");

    // Set the design path
    final String srcDesignPathname = args[0];
    final String destDesignPathname = args[0];

    filterSamples(srcDesignPathname, destDesignPathname, threshold);
  }

}
