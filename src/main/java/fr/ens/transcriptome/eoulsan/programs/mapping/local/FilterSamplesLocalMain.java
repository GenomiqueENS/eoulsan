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

package fr.ens.transcriptome.eoulsan.programs.mapping.local;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.MainCLI;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.io.DesignReader;
import fr.ens.transcriptome.eoulsan.io.DesignWriter;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.io.LogReader;
import fr.ens.transcriptome.eoulsan.io.SimpleDesignReader;
import fr.ens.transcriptome.eoulsan.io.SimpleDesignWriter;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class is the main class for filtering samples after mapping in local
 * mode.
 * @author Laurent Jourdren
 */
public class FilterSamplesLocalMain {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  public static String PROGRAM_NAME = "filtersamples";

  private static int threshold = 50;

  private static void filterSamples(final String srcDesignFilename,
      final String destDesignFilename, final double threshold) {

    try {
      filterSamples(new File(srcDesignFilename), new File(destDesignFilename),
          threshold);
    } catch (EoulsanIOException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    } catch (IOException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    }
  }

  private static void filterSamples(final File srcDesignFile,
      final File destDesignFile, final double threshold) throws IOException,
      EoulsanIOException {

    if (srcDesignFile == null)
      throw new NullPointerException("Source design file is null");

    if (destDesignFile == null)
      throw new NullPointerException("Destination design file is null");

    if (destDesignFile.exists())
      throw new EoulsanIOException("The output design already exists: "
          + destDesignFile);

    // Read the design file
    final DesignReader dr = new SimpleDesignReader(srcDesignFile);
    final Design design = dr.read();

    // Read filterreads.log
    LogReader logReader = new LogReader(new File("filterreads.log"));
    final Reporter filterReadsReporter = logReader.read();

    // Read soapmapreads.log
    logReader = new LogReader(new File("soapmapreads.log"));
    final Reporter soapMapReadsReporter = logReader.read();

    // Get the input reads for each sample
    final Map<String, Long> sampleInputMapReads =
        parseReporter(filterReadsReporter, "reads after filtering");

    // Get the number of match with onlt one locus for each sample
    final Map<String, Long> soapAlignementWithOneLocus =
        parseReporter(soapMapReadsReporter,
            "soap alignment with only one locus");

    // Compute ration and filter samples
    for (String sample : sampleInputMapReads.keySet()) {

      if (!soapAlignementWithOneLocus.containsKey(sample))
        continue;

      final long inputReads = sampleInputMapReads.get(sample);
      final long oneLocus = soapAlignementWithOneLocus.get(sample);

      final double ratio = (double) oneLocus / (double) inputReads;
      logger.info("Check Reads with only one match: "
          + sample + " " + oneLocus + "/" + inputReads + "=" + ratio
          + " threshold=" + threshold);

      if (ratio < threshold)
        design.removeSample(sample);
    }

    // Write output design
    DesignWriter writer = new SimpleDesignWriter(destDesignFile);
    writer.write(design);
  }

  private static final Map<String, Long> parseReporter(final Reporter reporter,
      final String counter) {

    final Map<String, Long> result = new HashMap<String, Long>();

    final Set<String> groups = reporter.getCounterGroups();

    for (String group : groups) {

      final int pos1 = group.indexOf('(');
      final int pos2 = group.indexOf(',');

      if (pos1 == -1 || pos2 == -1)
        continue;

      final String sample = group.substring(pos1 + 1, pos2).trim();

      result.put(sample, reporter.getCounterValue(group, counter));
    }

    return result;
  }

  /**
   * Show command line help.
   * @param options Options of the software
   */
  private static void help(final Options options) {

    // Show help message
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE
        + " [options] " + PROGRAM_NAME + " src_design design_design", options);

    System.exit(0);
  }

  /**
   * Create options for command line
   * @return an Options object
   */
  private static Options makeOptions() {

    // create Options object
    final Options options = new Options();

    options.addOption("version", false, "show version of the software");
    options
        .addOption("about", false, "display information about this software");
    options.addOption("h", "help", false, "display this help");
    options.addOption("license", false,
        "display information about the license of this software");

    options.addOption(OptionBuilder.withArgName("value").hasArg()
        .withDescription("threshold of the filter").create("threshold"));

    return options;
  }

  /**
   * Parse the options of the command line
   * @param args command line arguments
   * @return the number of optional arguments
   */
  private static int parseCommandLine(final String args[]) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    int argsOptions = 0;

    try {

      // parse the command line arguments
      CommandLine line = parser.parse(options, args);

      if (line.hasOption("help"))
        help(options);

      if (line.hasOption("about"))
        MainCLI.about();

      if (line.hasOption("version"))
        MainCLI.version();

      if (line.hasOption("license"))
        MainCLI.license();

      if (line.hasOption("threshold")) {
        threshold = Integer.parseInt(line.getOptionValue("threshold"));
        argsOptions += 2;
      }

    } catch (ParseException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }

    return argsOptions;
  }

  /**
   * Main method
   * @param args command line arguments
   */
  public static void main(final String[] args) {

    // Parse the command line
    final int argsOptions = parseCommandLine(args);

    if (args.length < argsOptions + 2) {
      System.err.println("Error: " + PROGRAM_NAME + " need two parameters");
      help(makeOptions());
      System.exit(1);
    }

    final String srcDesignFilename = args[argsOptions];
    final String destDesignFilename = args[argsOptions + 1];

    filterSamples(srcDesignFilename, destDesignFilename, threshold / 100.0);
  }

}
