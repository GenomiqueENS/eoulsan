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

import fr.ens.transcriptome.eoulsan.actions.Action;
import fr.ens.transcriptome.eoulsan.actions.ActionService;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * Main class in local mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class MainCLI {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  /**
   * Show command line help.
   * @param options Options of the software
   */
  private static void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE
        + " [options] command arguments", options);

    System.out.println("Available commands:");
    for (Action action : ActionService.getInstance().getActions()) {

      if (!action.isHadoopJarMode() && !action.isHidden()) {

        System.out.println(" - "
            + action.getName()
            + "\t"
            + action.getDescription()
            + (!action.isCurrentArchCompatible()
                ? " (not availlable for your platform)." : ""));
      }
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
   * @return the number of optional arguments
   */
  private static int parseCommandLine(final String args[]) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    int argsOptions = 0;

    try {

      // parse the command line arguments
      final CommandLine line = parser.parse(options, args, true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
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

    // Parse the command line
    final int argsOptions = parseCommandLine(args);

    // No arguments found
    if (args == null || args.length == argsOptions) {

      Common.showErrorMessageAndExit("This program needs one argument."
          + " Use the -h option to get more information.\n");
    }

    // Set action name and arguments
    final String actionName = args[argsOptions].trim().toLowerCase();
    final String[] arguments =
        StringUtils.arrayWithoutFirstsElement(args, argsOptions + 1);

    // Search action
    final Action action = ActionService.getInstance().getAction(actionName);

    // Action not found ?
    if (action == null || action.isHadoopJarMode()) {
      Common.showErrorMessageAndExit("Unknown action: "
          + actionName + ".\n" + "type: " + Globals.APP_NAME_LOWER_CASE
          + " -help for more help.\n");
    }

    final Settings settings = EoulsanRuntime.getSettings();

    // Test if action can be executed with current platform
    if (!settings.isBypassPlatformChecking()
        && !action.isCurrentArchCompatible()) {
      Common.showErrorMessageAndExit(Globals.WELCOME_MSG
          + "\nThe " + action.getName() + " of " + Globals.APP_NAME
          + " is not available for your platform. Required platforms: "
          + availableArchsToString() + ".");

    }

    // Run action
    action.action(arguments);
  }

  /**
   * Get in a string with all arch
   * @return a string with
   */
  private static String availableArchsToString() {

    final StringBuilder sb = new StringBuilder();

    boolean first = true;

    for (String osArch : Globals.AVAILABLE_BINARY_ARCH) {

      if (first) {
        first = false;
      } else {
        sb.append(", ");
      }

      sb.append(osArch.replace('\t', '/'));
    }

    return sb.toString();
  }

  //
  // Constructor
  //

  private MainCLI() {

    throw new IllegalStateException();
  }

}
