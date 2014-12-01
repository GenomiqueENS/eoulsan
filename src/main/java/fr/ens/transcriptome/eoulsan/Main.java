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

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static java.util.Collections.unmodifiableList;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.base.Joiner;

import fr.ens.transcriptome.eoulsan.actions.Action;
import fr.ens.transcriptome.eoulsan.actions.ActionService;
import fr.ens.transcriptome.eoulsan.util.SystemUtils;

/**
 * This class is the main class. Check the environment, if Hadoop library is in
 * the classpath launch Hadoop main class else run local main class.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class Main {

  private static final String EOULSAN_CLASSPATH_JVM_ARG = "eoulsan.classpath";
  private static final String EOULSAN_SCRIPT_PATH_JVM_ARG =
      "eoulsan.script.path";

  private static Main main;

  private final String launchModeName;

  private List<String> args;
  private Action action;
  private List<String> actionArgs;

  private String logLevel;
  private String logFile;
  private String conf;
  private List<String> commandLineSettings;

  private final BufferedHandler handler = new BufferedHandler();

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
   * Get java executable path.
   * @return the path to the java executable
   */
  public String getJavaExecutablePath() {

    return System.getProperty("java.home") + "/bin/java";
  }

  /**
   * Get JVM arguments.
   * @return the JVM arguments as an array
   */
  public List<String> getJVMArgs() {

    return ManagementFactory.getRuntimeMXBean().getInputArguments();
  }

  /**
   * Get Eoulsan classpath. The result of the method is based on the content of
   * the -Deoulsan.hadoop.libs JVM argument.
   * @return the JVM class as a String
   */
  public String getClassPath() {

    return System.getProperty(EOULSAN_CLASSPATH_JVM_ARG);
  }

  /**
   * Get Eoulsan script path.
   * @return the Eoulsan script path
   */
  public String getEoulsanScriptPath() {

    return System.getProperty(EOULSAN_SCRIPT_PATH_JVM_ARG);
  }

  /**
   * Get command line arguments.
   * @return Returns the arguments
   */
  public List<String> getArgs() {

    return unmodifiableList(this.args);
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
  public List<String> getActionArgs() {

    return unmodifiableList(this.actionArgs);
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
   * Get the command line settings arguments.
   * @return a list with the settings defined in the command line
   */
  public List<String> getCommandLineSettings() {

    if (this.commandLineSettings == null) {
      return Collections.emptyList();
    }

    return unmodifiableList(this.commandLineSettings);
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
    formatter.printHelp(
        getHelpEoulsanCommand() + " [options] action arguments", options);

    System.out.println("Available actions:");
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

    options.addOption(OptionBuilder
        .withArgName("property=value")
        .hasArg()
        .withDescription(
            "set a configuration setting. This "
                + "option can be used several times").create('s'));

    options.addOption(OptionBuilder.withArgName("file").hasArg()
        .withDescription("external log file").create("log"));

    options.addOption(OptionBuilder.withArgName("level").hasArg()
        .withDescription("log level").create("loglevel"));

    return options;
  }

  /**
   * Parse the options of the command line
   * @return the number of options argument in the command line
   */
  private int parseCommandLine() {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();
    final String[] argsArray = this.args.toArray(new String[args.size()]);

    int argsOptions = 0;

    try {

      // parse the command line arguments
      final CommandLine line = parser.parse(options, argsArray, true);

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

      // Set the configuration settings
      if (line.hasOption('s')) {

        this.commandLineSettings = Arrays.asList(line.getOptionValues('s'));
        argsOptions += 2 * this.commandLineSettings.size();
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
          "Error while parsing command line arguments: " + e.getMessage());
    }

    // No arguments found
    if (args == null || args.size() == argsOptions) {

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
    getLogger().info("Welcome to " + Globals.WELCOME_MSG);
    getLogger().info("Start in " + this.launchModeName + " mode");

    // Show versions
    getLogger().info(
        Globals.APP_NAME + " version: " + Globals.APP_VERSION_STRING);
    getLogger().info(
        Globals.APP_NAME + " revision: " + Globals.APP_BUILD_COMMIT);
    getLogger().info(
        Globals.APP_NAME + " build date: " + Globals.APP_BUILD_DATE);

    // Startup script
    getLogger().info(
        Globals.APP_NAME
            + " Startup script: "
            + (getLaunchScriptPath() == null
                ? "(no startup script)" : getLaunchScriptPath()));

    // Command line arguments
    final List<String> args = new ArrayList<>();
    for (String a : getArgs())
      if (a.indexOf(' ') != -1)
        args.add("\"" + a + "\"");
      else
        args.add(a);

    getLogger().info(
        Globals.APP_NAME
            + " Command line arguments: " + Joiner.on(' ').join(args));

    // Log file
    getLogger().info(
        "Log file: " + (this.logFile == null ? "(none)" : this.logFile));

    // Log level
    getLogger().info("Log level: " + getLogger().getLevel());
  }

  /**
   * Log system information.
   */
  protected void sysInfoLog() {

    // Host
    getLogger().info("Host: " + SystemUtils.getHostName());

    // Operating system
    getLogger().info("Operating system name: " + System.getProperty("os.name"));
    getLogger().info(
        "Operating system version: " + System.getProperty("os.version"));
    getLogger().info("Operating system arch: " + System.getProperty("os.arch"));

    // Java version
    getLogger().info("Java vendor: " + System.getProperty("java.vendor"));
    getLogger().info("Java vm name: " + System.getProperty("java.vm.name"));
    getLogger().info("Java version: " + System.getProperty("java.version"));
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

    getLogger().config("No configuration file found.");
    return new Settings(false);
  }

  /**
   * Set the command line settings entry.
   * @param settings the settings object
   */
  private void setManualSettings(final Settings settings) {

    for (String s : getCommandLineSettings()) {

      final int index = s.indexOf('=');

      if (index == -1) {
        settings.setSetting(s, "");
      } else {
        settings.setSetting(s.substring(0, index), s.substring(index + 1));
      }
    }
  }

  /**
   * Initialize the application logger.
   */
  private void initApplicationLogger() {

    // Disable parent Handler
    getLogger().setUseParentHandlers(false);

    // Set log level to all before setting the real log level with
    // BufferedHandler
    getLogger().setLevel(Level.ALL);

    // Add Buffered handler as unique Handler
    getLogger().addHandler(this.handler);

    // Set the formatter
    this.handler.setFormatter(Globals.LOG_FORMATTER);

    // Set the log level
    if (this.logLevel != null) {
      try {
        this.handler.setLevel(Level.parse(this.logLevel.toUpperCase()));
      } catch (IllegalArgumentException e) {
        Common
            .showErrorMessageAndExit("Unknown log level ("
                + this.logLevel
                + "). Accepted values are [SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST].");
      }
    } else
      this.handler.setLevel(Globals.LOG_LEVEL);

    // Set the log file in arguments
    if (this.logFile != null) {
      try {
        this.handler.addHandler(getLogHandler(this.logFile));
      } catch (IOException e) {
        Common.errorExit(e, "Error while creating log file: " + e.getMessage());
      }
    } else {

      final ConsoleHandler ch = new ConsoleHandler();
      this.handler.addHandler(ch);
    }

  }

  /**
   * Create a new log file and flush log.
   * @param logFilename log file name
   * @throws EoulsanException if an error occurs while creating log file
   */
  public void createLogFileAndFlushLog(final String logFilename)
      throws EoulsanException {

    try {
      Handler h = getLogHandler(logFilename);
      this.handler.addHandler(h);
    } catch (IOException e) {

      throw new EoulsanException(e.getMessage());
    }

    flushLog();
  }

  /**
   * Flush log.
   */
  public void flushLog() {

    this.handler.flush();
  }

  /**
   * Parse the action name and arguments from command line.
   * @param optionsCount number of options in the command line
   */
  private void parseAction(final int optionsCount) {

    // Set action name and arguments
    final String actionName = args.get(optionsCount).trim().toLowerCase();
    this.actionArgs = args.subList(optionsCount + 1, args.size());

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
    this.args = Arrays.asList(args);

    // Parse the command line
    final int optionsCount = parseCommandLine();

    // Initialize the logger
    initApplicationLogger();

    // Log some information about the application
    startupLog();

    try {

      // Load configuration file (if needed)
      final Settings settings = loadConfigurationFile();

      // Set manual settings
      setManualSettings(settings);

      // Log settings
      settings.logSettings();

      // Initialize the runtime
      initializeRuntime(settings);

      // Log system information
      sysInfoLog();

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

    getLogger().info("Start " + action.getName() + " action");

    // Run action
    action.action(main.getActionArgs());

    getLogger().info("End of " + action.getName() + " action");

    // Flush logs
    main.flushLog();
  }

}
