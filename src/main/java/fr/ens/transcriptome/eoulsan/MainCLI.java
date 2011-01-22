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
import java.util.List;
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

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.actions.AWSExecAction;
import fr.ens.transcriptome.eoulsan.actions.Action;
import fr.ens.transcriptome.eoulsan.actions.CreateDesignAction;
import fr.ens.transcriptome.eoulsan.actions.CreateHadoopJarAction;
import fr.ens.transcriptome.eoulsan.actions.ExecAction;
import fr.ens.transcriptome.eoulsan.actions.HadoopExecAction;
import fr.ens.transcriptome.eoulsan.actions.UploadS3Action;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * Main class in local mode.
 * @author Laurent Jourdren
 */
public final class MainCLI {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  /**
   * Show command line help.
   * @param options Options of the software
   */
  private static void help(final Options options, final List<Action> actions) {

    // Show help message
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE
        + " [options] command arguments", options);

    System.out.println("Available commands:");
    for (Action action : actions) {
      System.out.println("\t- "
          + action.getName() + "\t" + action.getDescription());
    }

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
   * @param action available actions
   * @return the number of optional arguments
   */
  private static int parseCommandLine(final String args[],
      final List<Action> actions) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    int argsOptions = 0;

    try {

      // parse the command line arguments
      final CommandLine line = parser.parse(options, args);

      // Help option
      if (line.hasOption("help")) {
        help(options, actions);
      }

      // About option
      if (line.hasOption("about")) {
        Common.showMessageAndExit(Globals.ABOUT_TXT);
      }

      // Version option
      if (line.hasOption("version")) {
        Common.showMessageAndExit(Globals.APP_NAME
            + " version " + Globals.APP_VERSION_STRING + " ("
            + Globals.APP_BUILD_NUMBER + " on " + Globals.APP_BUILD_DATE + ")");
      }

      // Licence option
      if (line.hasOption("license")) {
        Common.showMessageAndExit(Globals.LICENSE_TXT);
      }

      // Load configuration if exists
      try {

        final Settings settings;

        if (line.hasOption("conf")) {
          settings = new Settings(new File(line.getOptionValue("conf")));
          argsOptions += 2;
        } else {
          settings = new Settings();
        }

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
          final Handler fh = new FileHandler(line.getOptionValue("log"));
          fh.setFormatter(Globals.LOG_FORMATTER);
          LOGGER.setLevel(Globals.LOG_LEVEL);
          LOGGER.setUseParentHandlers(false);

          LOGGER.addHandler(fh);
        } catch (IOException e) {
          Common.errorExit(e,
              "Error while creating log file: " + e.getMessage());
        }
      }

      // Set log level
      if (line.hasOption("loglevel")) {

        argsOptions += 2;
        try {
          LOGGER.setLevel(Level.parse(line.getOptionValue("loglevel")
              .toUpperCase()));
        } catch (IllegalArgumentException e) {

          LOGGER
              .warning("Unknown log level ("
                  + line.getOptionValue("loglevel")
                  + "). Accepted values are [SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST].");

        }
      }

    } catch (ParseException e) {
      Common.errorExit(e,
          "Error while parsing parameter file: " + e.getMessage());
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
    LOGGER.setLevel(Globals.LOG_LEVEL);
    LOGGER.getParent().getHandlers()[0].setFormatter(Globals.LOG_FORMATTER);

    // Define available actions
    final List<Action> actions =
        Lists.newArrayList(new CreateDesignAction(), new ExecAction(),
            new HadoopExecAction(), new CreateHadoopJarAction(),
            new UploadS3Action(), new AWSExecAction());

    // Parse the command line
    final int argsOptions = parseCommandLine(args, actions);

    // No arguments found
    if (args == null || args.length == argsOptions) {

      Common.showErrorMessageAndExit("This program needs one argument."
          + " Use the -h option to get more information.\n");
    }

    final String actionName = args[argsOptions].trim().toLowerCase();
    final String[] arguments =
        StringUtils.arrayWithoutFirstsElement(args, argsOptions + 1);

    // Search action
    boolean actionFound = false;
    for (Action action : actions) {

      if (action.getName().equals(actionName)) {

        action.action(arguments);
        actionFound = true;
        break;
      }
    }

    // Action not found
    if (!actionFound) {
      Common.showErrorMessageAndExit("Unknown action: "
          + actionName + ".\n" + "type: " + Globals.APP_NAME_LOWER_CASE
          + " -help for more help.\n");
    }

  }

  //
  // Constructor
  //

  private MainCLI() {

    throw new IllegalStateException();
  }

}
