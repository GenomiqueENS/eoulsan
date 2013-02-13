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
import java.util.List;
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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.actions.Action;
import fr.ens.transcriptome.eoulsan.actions.ActionService;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.SystemUtils;

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

  private final String launchModeName;

  private String[] args;
  private Action action;
  private String[] actionArgs;

  private String logLevel;
  private String logFile;
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

    return this.args;
  }

  /**
   * Get the action.
   * @return Returns the action
   */
  public Action getAction() {

    return this.action;
  }

  /**
   * Get the action arguments.
   * @return Returns the actionArgs
   */
  public String[] getActionArgs() {

    return this.actionArgs;
  }

  /**
   * Get the log level arguments.
   * @return Returns the logLevel
   */
  public String getLogLevelArgument() {

    return this.logLevel;
  }

  /**
   * Get the log file argument.
   * @return Returns the log
   */
  public String getLogFileArgument() {

    return this.logFile;
  }

  /**
   * Get the configuration file argument.
   * @return Returns the configuration file
   */
  public String getConfigurationFileArgument() {

    return this.conf;
  }

  /**
   * Get the path to the launch script.
   * @return the path to the launch script or null if no launch script has been
   *         used
   */
  public String getLaunchScriptPath() {

    return System.getProperty(Globals.LAUNCH_SCRIPT_PATH);
  }

  /**
   * Get the launch mode of the application.
   * @return the launch mode of the application
   */
  public String getLaunchMode() {

    return this.launchModeName;
  }

  //
  // Parsing methods
  //

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
   * @return the number of options argument in the command line
   */
  private int parseCommandLine() {

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
        this.logFile = line.getOptionValue("log");
      }

      // Set log level
      if (line.hasOption("loglevel")) {

        argsOptions += 2;
        this.logLevel = line.getOptionValue("loglevel");
      }

      // Set the configuration file
      if (line.hasOption("conf")) {

        argsOptions += 2;
        this.conf = line.getOptionValue("conf");
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

    } catch (ParseException e) {
      Common.errorExit(e,
          "Error while parsing parameter file: " + e.getMessage());
    }

    // No arguments found
    if (args == null || args.length == argsOptions) {

      Common.showErrorMessageAndExit("This program needs one argument."
          + " Use the -h option to get more information.\n");
    }

    return argsOptions;
  }

  //
  // Other methods
  //

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

  private void startupLog() {

    // Welcome message
    LOGGER.info("Welcome to " + Globals.WELCOME_MSG);
    LOGGER.info("Start in " + this.launchModeName + " mode");

    // Show versions
    LOGGER.info(Globals.APP_NAME + " version: " + Globals.APP_VERSION_STRING);
    LOGGER.info(Globals.APP_NAME + " revision: " + Globals.APP_BUILD_COMMIT);
    LOGGER.info(Globals.APP_NAME + " build date: " + Globals.APP_BUILD_DATE);

    // Startup script
    LOGGER.info(Globals.APP_NAME
        + " Startup script: "
        + (getLaunchScriptPath() == null
            ? "(no startup script)" : getLaunchScriptPath()));

    // Command line arguments
    final List<String> args = Lists.newArrayList();
    for (String a : getArgs())
      if (a.indexOf(' ') != -1)
        args.add("\"" + a + "\"");
      else
        args.add(a);

    LOGGER.info(Globals.APP_NAME
        + " Command line arguments: " + Joiner.on(' ').join(args));

    // Log file
    LOGGER
        .info("Log file: " + (this.logFile == null ? "(none)" : this.logFile));

    // Log level
    LOGGER.info("Log level: " + LOGGER.getLevel());

    // Log system information
    sysInfoLog();
  }

  /**
   * Log system information.
   */
  protected void sysInfoLog() {

    // Host
    LOGGER.info("Host: " + SystemUtils.getHostName());

    // Operating system
    LOGGER.info("Operating system name: " + System.getProperty("os.name"));
    LOGGER
        .info("Operating system version: " + System.getProperty("os.version"));
    LOGGER.info("Operating system arch: " + System.getProperty("os.arch"));

    // Java version
    LOGGER.info("Java vendor: " + System.getProperty("java.vendor"));
    LOGGER.info("Java vm name: " + System.getProperty("java.vm.name"));
    LOGGER.info("Java version: " + System.getProperty("java.version"));
  }

  /**
   * Load the configuration file if exists.
   * @return a new Settings object
   * @throws IOException if an error occurs while reading the configuration file
   * @throws EoulsanException if an error occurs while reading the configuration
   *           file
   */
  private Settings loadConfigurationFile() throws IOException, EoulsanException {

    // Load the setting file if has been defined in command line
    if (this.conf != null) {
      return new Settings(new File(this.conf));
    }

    // Define the default configuration file
    final File defaultConfFile = new File(Settings.getConfigurationFilePath());

    // Test if default configuration file exists
    if (defaultConfFile.exists()) {
      this.conf = defaultConfFile.getAbsolutePath();
      return new Settings(defaultConfFile);
    }

    LOGGER.config("No configuration file found.");
    return new Settings(false);
  }

  /**
   * Initialize the application logger.
   */
  private void initApplicationLogger() {

    // Set default log level
    LOGGER.setLevel(Globals.LOG_LEVEL);
    LOGGER.getParent().getHandlers()[0].setFormatter(Globals.LOG_FORMATTER);

    // Set the log file
    if (this.logFile != null) {
      try {

        final Handler fh = getLogHandler(this.logFile);
        fh.setFormatter(Globals.LOG_FORMATTER);
        LOGGER.setLevel(Globals.LOG_LEVEL);
        LOGGER.setUseParentHandlers(false);

        LOGGER.addHandler(fh);
      } catch (IOException e) {
        Common.errorExit(e, "Error while creating log file: " + e.getMessage());
      }
    }

    // Set the log level
    if (this.logLevel != null) {
      try {
        LOGGER.setLevel(Level.parse(this.logLevel.toUpperCase()));
      } catch (IllegalArgumentException e) {
        Common
            .showErrorMessageAndExit("Unknown log level ("
                + this.logLevel
                + "). Accepted values are [SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST].");
      }
    }

  }

  /**
   * Parse the action name and arguments from command line.
   * @param optionsCount number of options in the command line
   */
  private void parseAction(final int optionsCount) {

    // Set action name and arguments
    final String actionName = args[optionsCount].trim().toLowerCase();
    this.actionArgs =
        StringUtils.arrayWithoutFirstsElement(args, optionsCount + 1);

    // Test if is in hadoop mode
    final boolean hadoopMode = EoulsanRuntime.getRuntime().isHadoopMode();

    // Search action
    this.action = ActionService.getInstance().getAction(actionName);

    // Action not found ?
    if (this.action == null || hadoopMode != action.isHadoopJarMode()) {
      Common.showErrorMessageAndExit("Unknown action: "
          + actionName + ".\n" + "type: " + Globals.APP_NAME_LOWER_CASE
          + " -help for more help.\n");
    }
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

  /**
   * Get the Handler to create the log file.
   * @param logFile the path to the log file
   * @return a new Handler object
   * @throws IOException if an exception occurs while creating the handler
   */
  protected abstract Handler getLogHandler(final String logFile)
      throws IOException;

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param args command line argument.
   */
  Main(final String modeName, final String[] args) {

    this.launchModeName = modeName;
    this.args = args;

    // Parse the command line
    final int optionsCount = parseCommandLine();

    // Initialize the logger
    initApplicationLogger();

    // Log some information about the application
    startupLog();

    try {

      // Load configuration file (if needed)
      final Settings settings = loadConfigurationFile();

      // Log settings
      settings.logSettings();

      // Initialize the runtime
      initializeRuntime(settings);

    } catch (IOException e) {
      Common.errorExit(e, "Error while reading configuration file.");
    } catch (EoulsanException e) {
      Common.errorExit(e, e.getMessage());
    }

    // Parse action name and action arguments from command line
    parseAction(optionsCount);
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

    LOGGER.info("Start " + action.getName() + " action");

    // Run action
    action.action(main.getActionArgs());

    LOGGER.info("End of " + action.getName() + " action");
  }

}
