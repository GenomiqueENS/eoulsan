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

package fr.ens.transcriptome.eoulsan;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ens.transcriptome.eoulsan.core.action.LocalCreateDesignAction;
import fr.ens.transcriptome.eoulsan.core.action.LocalExecAction;
import fr.ens.transcriptome.eoulsan.core.action.LocalUploadS3Action;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * Main class in local mode.
 * @author Laurent Jourdren
 */
public class MainCLI {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  /**
   * Show command line help.
   * @param options Options of the software
   */
  private static void help(final Options options) {

    // Show help message
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE + " [options] design",
        options);

    Common.exit(0);
  }

  /**
   * Create options for command line
   * @return an Options object
   */
  @SuppressWarnings("static-access")
  private static Options makeOptions() {

    // create Options object
    final Options options = new Options();

    options.addOption("version", false, "show version of the software");
    options
        .addOption("about", false, "display information about this software");
    options.addOption("h", "help", false, "display this help");
    options.addOption("license", false,
        "display information about the license of this software");

    options.addOption(OptionBuilder.withArgName("file").hasArg()
        .withDescription("configuration file to use").create("conf"));

    options.addOption(OptionBuilder.withArgName("file").hasArg()
        .withDescription("external log file").create("log"));

    options.addOption(OptionBuilder.withArgName("level").hasArg()
        .withDescription("log level").create("loglevel"));

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

      // Help option
      if (line.hasOption("help"))
        help(options);

      // About option
      if (line.hasOption("about"))
        Common.showMessageAndExit(Globals.ABOUT_TXT);

      // Version option
      if (line.hasOption("version"))
        Common.showMessageAndExit(Globals.APP_NAME
            + " version " + Globals.APP_VERSION_STRING + " ("
            + Globals.APP_BUILD_NUMBER + " on " + Globals.APP_BUILD_DATE + ")");

      // Licence option
      if (line.hasOption("license"))
        Common.showMessageAndExit(Globals.LICENSE_TXT);

      // Load configuration if exists
      try {

        final Settings settings;

        if (line.hasOption("conf")) {
          settings = new Settings(new File(line.getOptionValue("conf")));
          argsOptions += 2;
        } else
          settings = new Settings();

        // Initialize the runtime
        LocalEoulsanRuntime.newEoulsanRuntime(settings);

      } catch (IOException e) {
        Common.errorExit(e, "Error while reading configuration file.");
      } catch (EoulsanException e) {
        Common.errorExit(e, e.getMessage());
      }

      // Set Log file
      if (line.hasOption("log")) {

        argsOptions += 2;
        try {
          Handler fh = new FileHandler(line.getOptionValue("log"));
          fh.setFormatter(Globals.LOG_FORMATTER);
          logger.setLevel(Globals.LOG_LEVEL);
          logger.setUseParentHandlers(false);

          logger.addHandler(fh);
        } catch (IOException e) {
          Common.errorExit(e, "Error while creating log file: "
              + e.getMessage());
        }
      }

      // Set log level
      if (line.hasOption("loglevel")) {

        argsOptions += 2;
        try {
          logger.setLevel(Level.parse(line.getOptionValue("loglevel")
              .toUpperCase()));
        } catch (IllegalArgumentException e) {

          logger
              .warning("Unknown log level ("
                  + line.getOptionValue("loglevel")
                  + "). Accepted values are [SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST].");

        }
      }

    } catch (ParseException e) {
      Common.errorExit(e, "Error while parsing parameter file: "
          + e.getMessage());
    }

    return argsOptions;
  }

  //
  // Action methods
  //

  //
  // Main method
  //

  /**
   * Main method for the CLI mode.
   * @param args command line arguments
   */
  public static void main(final String[] args) {

    // Set log level
    logger.setLevel(Globals.LOG_LEVEL);
    logger.getParent().getHandlers()[0].setFormatter(Globals.LOG_FORMATTER);

    // Parse the command line
    final int argsOptions = parseCommandLine(args);

    if (args == null || args.length == argsOptions) {

      Common.showErrorMessageAndExit("This program needs one argument."
          + " Use the -h option to get more information.\n" + "usage:\n\t"
          + Globals.APP_NAME_LOWER_CASE
          + " createdesign fastq_files fasta_file gff_gfile\n" + "usage:\n\t"
          + Globals.APP_NAME_LOWER_CASE
          + " createdesign exec param.xml design.txt" + "usage:\n\t"
          + Globals.APP_NAME_LOWER_CASE + " uploads3 param.xml design.txt");

    }

    final String action = args[argsOptions].trim().toLowerCase();
    final String[] arguments =
        StringUtils.arrayWithoutFirstsElement(args, argsOptions + 1);

    if ("exec".equals(action))
      new LocalExecAction().action(arguments);
    else if ("createdesign".equals(action))
      new LocalCreateDesignAction().action(arguments);
    else if ("uploads3".equals(action))
      new LocalUploadS3Action().action(arguments);

  }
}
