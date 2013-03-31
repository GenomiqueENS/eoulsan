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
 * This class is the main class. Check the environment, if Hadoop library is in
 * the classpath launch Hadoop main class else run local main class.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class Main {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static Main main;

  private String[] args;
  private Action action;
  private String[] actionArgs;

  private String logLevel;
  private String log;
  private String conf;

  //
  // Getters
  //

  /**
   * Get the instance of the Main class.
   * @return a Main object
   */
  public static Main getInstance() {

    return main;
  }

  /**
   * Get command line arguments.
   * @return Returns the arguments
   */
  public String[] getArgs() {

    return args;
  }

  /**
   * Get the action.
   * @return Returns the action
   */
  public Action getAction() {

    return action;
  }

  /**
   * Get the action arguments.
   * @return Returns the actionArgs
   */
  public String[] getActionArgs() {

    return actionArgs;
  }

  /**
   * Get the log level arguments.
   * @return Returns the logLevel
   */
  public String getLogLevelArgument() {

    return logLevel;
  }

  /**
   * Get the log file argument.
   * @return Returns the log
   */
  public String getLogFileArgument() {

    return log;
  }

  /**
   * Get the configuration file argument.
   * @return Returns the configuration file
   */
  public String getConfigurationFileArgument() {

    return conf;
  }

  /**
   * Show command line help.
   * @param options Options of the software
   */
  protected void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(getHelpEoulsanCommand()
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
  protected Options makeOptions() {

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
   */
  private void parseCommandLine(final String args[]) {

    this.args = args;

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
        Common.showMessageAndExit(Globals.WELCOME_MSG);
      }

      // Licence option
      if (line.hasOption("license")) {
        Common.showMessageAndExit(Globals.LICENSE_TXT);
      }

      // Set Log file
      if (line.hasOption("log")) {

        argsOptions += 2;
        try {
          this.log = line.getOptionValue("log");
          final Handler fh = new FileHandler(this.log);
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
          this.logLevel = line.getOptionValue("loglevel");
          LOGGER.setLevel(Level.parse(this.logLevel.toUpperCase()));
        } catch (IllegalArgumentException e) {

          LOGGER
              .warning("Unknown log level ("
                  + this.logLevel
                  + "). Accepted values are [SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST].");

        }
      }

      // eoulsan.sh options
      if (line.hasOption('j')) {
        argsOptions += 2;
      }
      if (line.hasOption('m')) {
        argsOptions += 2;
      }
      if (line.hasOption('J')) {
        argsOptions += 2;
      }
      if (line.hasOption('p')) {
        argsOptions += 2;
      }

      // Load configuration if exists
      try {

        final Settings settings;

        if (line.hasOption("conf")) {
          this.conf = line.getOptionValue("conf");
          settings = new Settings(new File(this.conf));
          argsOptions += 2;
        } else {

          // Default configuration file
          final File defaultConfFile =
              new File(Settings.getConfigurationFilePath());

          if (defaultConfFile.exists()) {

            this.conf = defaultConfFile.getAbsolutePath();
            settings = new Settings(defaultConfFile);
          } else {
            LOGGER.config("No configuration file found.");
            settings = new Settings(false);
          }
        }

        // Initialize the runtime
        initializeRuntime(settings);

      } catch (IOException e) {
        Common.errorExit(e, "Error while reading configuration file.");
      } catch (EoulsanException e) {
        Common.errorExit(e, e.getMessage());
      }

    } catch (ParseException e) {
      Common.errorExit(e,
          "Error while parsing parameter file: " + e.getMessage());
    }

    // No arguments found
    if (args == null || args.length == argsOptions) {

      Common.showErrorMessageAndExit("This program needs one argument."
          + " Use the -h option to get more information.\n");
    }

    // Set action name and arguments
    final String actionName = args[argsOptions].trim().toLowerCase();
    this.actionArgs =
        StringUtils.arrayWithoutFirstsElement(args, argsOptions + 1);

    // Test if is in hadoop mode
    final boolean hadoopMode = EoulsanRuntime.getRuntime().isHadoopMode();

    // Search action
    this.action = ActionService.getInstance().newService(actionName);

    // Action not found ?
    if (this.action == null || hadoopMode != action.isHadoopJarMode()) {
      Common.showErrorMessageAndExit("Unknown action: "
          + actionName + ".\n" + "type: " + Globals.APP_NAME_LOWER_CASE
          + " -help for more help.\n");
    }

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
  // Abstract methods
  //

  /**
   * Initialize the Eoulsan runtime.
   */
  protected abstract void initializeRuntime(Settings settings)
      throws EoulsanException;

  /**
   * Get the command used to launch Eoulsan.
   * @return a String with the command used to launch Eoulsan
   */
  protected abstract String getHelpEoulsanCommand();

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param args command line arguments
   */
  Main(final String[] args) {

    parseCommandLine(args);
  }

  //
  // Main method
  //

  /**
   * Main method of the program.
   * @param args command line arguments
   */
  public static void main(final String[] args) {

    if (main != null)
      throw new IllegalAccessError("Main method cannot be run twice.");

    // Set the default local for all the application
    Globals.setDefaultLocale();

    // Set default log level
    LOGGER.setLevel(Globals.LOG_LEVEL);
    LOGGER.getParent().getHandlers()[0].setFormatter(Globals.LOG_FORMATTER);

    // Select the application execution mode
    final String eoulsanMode = System.getProperty(Globals.LAUNCH_MODE_PROPERTY);

    if (eoulsanMode != null && eoulsanMode.equals("local")) {
      main = new MainCLI(args);
    } else {
      main = new MainHadoop(args);
    }

    // Get the action to execute
    final Action action = main.getAction();

    // Get the Eoulsan settings
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
    action.action(main.getActionArgs());
  }

}
