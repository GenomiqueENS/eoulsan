package fr.ens.transcriptome.eoulsan.actions;

import static com.google.common.io.Files.newReader;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.createSymbolicLink;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
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
import org.apache.commons.compress.utils.Charsets;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.data.DataSetAnalysis;
import fr.ens.transcriptome.eoulsan.data.DataSetTest;
import fr.ens.transcriptome.eoulsan.io.ComparatorDirectories;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class RegressionAction extends AbstractAction {

  public static final String LOGGER_TESTS_GLOBAL = "tests_global";
  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);
  private static final Logger LOGGER_GLOBAL = Logger
      .getLogger(LOGGER_TESTS_GLOBAL);

  private final Stopwatch TIMER = Stopwatch.createUnstarted();

  public static DateFormat DATE_FORMAT = new SimpleDateFormat(
      "yyyyMMdd-kkmmss", Globals.DEFAULT_LOCALE);

  public static final boolean USE_SERIALIZATION = true;
  public static final boolean CHECKING_SAME_NAME = true;

  private final Properties props;
  // private final ComparatorDirectories comparator;

  private File inputData;
  private File outputTestsDirectory;
  private File tmpDir;
  private String logGlobalPath;
  private String logTestPath;
  private boolean asRegression = false;
  private boolean exceptionThrowGlobal = false;

  private final Map<String, DataSetTest> tests;

  @Override
  public String getName() {
    return "regression";
  }

  @Override
  public String getDescription() {
    return "test " + Globals.APP_NAME + " version.";
  }

  private void initLoggerTest(final String nameTest) throws EoulsanException {

    // Close previous logger test file
    if (this.logTestPath != null) {
      for (Handler handler : LOGGER.getHandlers()) {
        LOGGER.removeHandler(handler);
      }

      // Remove lock logger file
      if (!new File(this.logTestPath + ".lck").delete())
        // TODO
        System.out.println("Fail remove lock logger file: "
            + logTestPath + ".lck");
    }

    // Init new logger for a current test
    this.logTestPath = this.tmpDir + "/test_" + nameTest + ".log";

    initLogger(LOGGER, logTestPath);

  }

  public void initLoggerGlobal(final String logPath) throws EoulsanException {
    logGlobalPath =
        logPath + "/eoulsan_" + DATE_FORMAT.format(new Date()) + ".log";

    initLogger(LOGGER_GLOBAL, logGlobalPath);

  }

  private void initLogger(final Logger logger, final String logPath)
      throws EoulsanException {
    Handler fh = null;
    try {
      fh = new FileHandler(logPath);

    } catch (Exception e) {
      throw new EoulsanException(e.getMessage());
    }

    fh.setFormatter(Globals.LOG_FORMATTER);

    logger.setLevel(Level.ALL);
    // LOGGER.setUseParentHandlers(false);
    logger.addHandler(fh);
    logger.info(Globals.WELCOME_MSG);

  }

  private void addExceptionMessageInLogger(final Logger logger,
      final Exception exception) {

    if (exception == null)
      return;

    final String msg =
        exception.getClass().getName()
            + ": " + exception.getMessage() + "\n"
            + StringUtils.join(exception.getStackTrace(), "\n\t");
    logger.severe(msg);
  }

  private void closeLoggerGlobal(final String suf) {

    // Set suffix logger filename
    final String suffix =
        (suf != null ? suf : (exceptionThrowGlobal
            ? "EXCEPTION" : (asRegression ? "FAIL" : "SUCCES")));

    // Add suffix to log global filename
    LOGGER_GLOBAL.fine(suffix
        + " end execution in "
        + toTimeHumanReadable(TIMER.elapsed(TimeUnit.MILLISECONDS)));

    final File logFile = new File(logGlobalPath);

    if (logFile.exists()) {

      final File destFile =
          new File(StringUtils.filenameWithoutExtension(this.logGlobalPath)
              + "_" + suffix + ".log");

      // Rename log file, add suffix
      if (logFile.renameTo(destFile))
        // Create a symbolic link
        createSymbolicLink(destFile, this.outputTestsDirectory);
    }
  }

  private void closeLoggerTest(final Exception exception, final File outputDir,
      final boolean asRegressionTest) {

    final File srcFile = new File(this.logTestPath);

    // Throw exception occurs during initialization test, no logger test exists
    if (!srcFile.exists())
      return;

    final String suffix;

    if (exception == null) {
      suffix = asRegressionTest ? "FAIL" : "SUCCES";

    } else {
      suffix = "EXCEPTION";

      addExceptionMessageInLogger(LOGGER, exception);
      addExceptionMessageInLogger(LOGGER_GLOBAL, exception);
    }

    // srcFile.renameTo(destFile);

    // Add suffix to log global filename
    final File destFile =
        new File(outputDir, StringUtils.filenameWithoutExtension(srcFile
            .getName()) + "_" + suffix + ".log");

    // Copy and rename
    try {
      FileUtils.moveFile(srcFile, destFile);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void action(String[] arguments) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    String confPath = null;
    String jobDescription = null;

    // Optional, file
    String testsFilePath = null;
    boolean regenerateExpectedData = false;
    boolean isCheckingAction = false;

    int argsOptions = 0;

    try {

      // parse the command line arguments
      final CommandLine line = parser.parse(options, arguments, true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }

      // Description
      if (line.hasOption("d")) {

        jobDescription = line.getOptionValue("d");
        argsOptions += 2;
      }

      if (line.hasOption("c")) {

        // Configuration test files
        confPath = line.getOptionValue("c").trim();
        argsOptions += 2;
      }

      // Optional argument
      if (line.hasOption("f")) {

        // List all test to launch
        testsFilePath = line.getOptionValue("f").trim();
        argsOptions += 2;
      }

      // Optional argument
      // TODO option name
      if (line.hasOption("generate")) {
        isCheckingAction = true;
        final String s = line.getOptionValue("generate").trim();

        // Value equals all, regenerate all expected directories generated
        // automatically
        if (s.toLowerCase(Globals.DEFAULT_LOCALE).equals("all"))
          regenerateExpectedData = true;
        else {
          // Value equals new, regenerate expected directories doesn't exists
          if (s.toLowerCase(Globals.DEFAULT_LOCALE).equals("new"))
            regenerateExpectedData = false;
        }

        argsOptions += 2;
      }

    } catch (ParseException e) {
      Common.errorExit(e,
          "Error while parsing parameter file: " + e.getMessage());
    }

    if (arguments.length != argsOptions) {
      help(options);
    }

    // Execute program in local mode
    run(confPath, testsFilePath, regenerateExpectedData, isCheckingAction);
  }

  /**
   * Create options for command line
   * @return an Options object
   */
  @SuppressWarnings("static-access")
  private Options makeOptions() {

    // create Options object
    final Options options = new Options();

    // Help option
    options.addOption("h", "help", false, "display this help");

    // Description option
    options.addOption(OptionBuilder.withArgName("description").hasArg()
        .withDescription("job description").withLongOpt("desc").create('d'));

    // Path to test Eoulsan version
    options.addOption(OptionBuilder.withArgName("confPath").hasArg(true)
        .withDescription("configuration test file").withLongOpt("conf")
        .create('c'));

    // Optional, path to file with list name tests to treat
    options.addOption(OptionBuilder.withArgName("fileTest").hasArg(true)
        .withDescription("path to file with list name tests")
        .withLongOpt("file").create('f'));

    // Optional, force generated expected data
    options.addOption(OptionBuilder.withArgName("type").hasArg()
        .withDescription("mode generate data expected").create("generate"));

    return options;
  }

  /**
   * Show command line help.
   * @param options Options of the software
   */
  private void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE
        + ".sh " + getName() + " [options] configuration_tests_path", options);

    Common.exit(0);
  }

  //
  // Execution
  //

  /**
   * Launch Eoulsan on each dataset and if success compares result directory
   * @param pathEoulsanNewVersion
   * @param listDatasets
   * @param outputDirectory
   * @param jobDescription
   */
  private void run(final String confPath, final String testsFilesPath,
      final boolean regenerateExpectedData, final boolean isCheckingAction) {

    // Initialization action from configuration test file
    init(new File(confPath), testsFilesPath);

    LOGGER_GLOBAL.info("Tests found count: " + this.tests.size());
    TIMER.start();

    if (isCheckingAction) {
      runGenerateExpectedData(regenerateExpectedData);
    } else {
      runTest();
    }

  }

  /**
   * TODO Value equals all, regenerate all expected directories generated
   * automatically otherwise generated only expected directory doesn't exist
   * @param regenerateExpectedData if true generate all expected directories
   *          otherwise this missing
   */
  private void runGenerateExpectedData(final boolean regenerateExpectedData) {
    // Generate all expected data directory
    try {
      for (Map.Entry<String, DataSetTest> test : this.tests.entrySet()) {
        final String testName = test.getKey();
        final DataSetTest dst = test.getValue();

        LOGGER_GLOBAL.info(testName + ": check expected data");
        dst.generateDataExpected(regenerateExpectedData);
      }

    } catch (Exception e) {
      e.printStackTrace();

    } finally {
      closeLoggerGlobal("CHECK");
    }
  }

  private void runTest() {
    Exception exception = null;

    // Generate all
    for (Map.Entry<String, DataSetTest> test : this.tests.entrySet()) {
      final String testName = test.getKey();
      final DataSetTest dst = test.getValue();
      final ComparatorDirectories comparator =
          new ComparatorDirectories(USE_SERIALIZATION);

      String reportTest = "";

      try {
        initLoggerTest(testName);
        LOGGER.info(testName + ": execute test");

        // Add description current test
        LOGGER_GLOBAL.info(testName + ": " + dst.getDescriptionTest());

        dst.generateDataTested();

        // Collect data expected for project
        final DataSetAnalysis setExpected = dst.getAnalysisExcepted();

        // Collect data tested
        final DataSetAnalysis setTested = dst.getAnalysisTest();

        comparator.setPatternToCompare(dst.getPatternsToCompare());

        // Launch comparison
        comparator.compareDataSet(setExpected, setTested, testName);

        // Compile assessments for all tests
        reportTest =
            comparator.buildReport(dst.isCheckingExistingFiles(), testName);

        if (comparator.asRegression()) {
          LOGGER_GLOBAL.severe(reportTest);
          this.asRegression = true;

        } else {
          LOGGER_GLOBAL.info(reportTest);
        }

      } catch (Exception e) {
        exception = e;
        e.printStackTrace();

      } finally {

        // Add assessment current test
        if (exception != null) {
          exceptionThrowGlobal = true;
          LOGGER_GLOBAL.severe("Exception throw with test:" + testName);
        }

        // End run current test
        closeLoggerTest(exception, dst.getTestedDirectory(),
            comparator.asRegression());
      }
    }
    TIMER.stop();
    // End run all tests
    closeLoggerGlobal(null);
  }

  /**
   * Initialization properties from tests configuration
   * @param conf configuration file from Action
   * @throws EoulsanException
   * @throws IOException
   */
  private void init(final File conf, final String testsSelectedPath) {

    try {
      checkExistingFile(conf, " configuration file doesn't exist.");

      final BufferedReader br =
          new BufferedReader(newReader(conf,
              Charsets.toCharset(Globals.DEFAULT_FILE_ENCODING)));
      String line = null;

      while ((line = br.readLine()) != null) {
        // Skip commentary
        if (line.startsWith("#"))
          continue;

        final int pos = line.indexOf('=');
        if (pos == -1)
          continue;

        final String key = line.substring(0, pos).trim();
        final String value = line.substring(pos + 1).trim();

        props.put(key, value);

      }
      br.close();

      initLoggerGlobal(this.props.getProperty("log_path"));
      configure(testsSelectedPath);

    } catch (Exception e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();

      addExceptionMessageInLogger(LOGGER_GLOBAL, e1);
      closeLoggerGlobal(null);
    }

  }

  private void configure(final String testsSelectedPath)
      throws EoulsanException, IOException {
    // Initialization
    this.inputData = new File(this.props.getProperty("input_directory"));
    checkExistingFile(this.inputData, " input data directory ");
    LOGGER_GLOBAL.config("Input data directory: "
        + this.inputData.getAbsolutePath());

    final File testsData = new File(this.props.getProperty("tests_directory"));
    checkExistingFile(testsData, " tests data directory ");
    LOGGER_GLOBAL
        .config("Tests data directory: " + testsData.getAbsolutePath());

    final File output =
        new File(this.props.getProperty("output_analysis_directory"));
    checkExistingFile(output, " output data directory ");
    LOGGER_GLOBAL.config("Output data directory: " + output.getAbsoluteFile());

    this.outputTestsDirectory =
        new File(output, props.getProperty("eoulsan_test_version_git")
            + "_" + DATE_FORMAT.format(new Date()));
    LOGGER_GLOBAL.config("Output tests directory: "
        + this.outputTestsDirectory.getAbsolutePath());

    this.tmpDir = new File(this.props.getProperty("tmp_path"));
    checkExistingFile(this.tmpDir, " tmp directory ");
    LOGGER_GLOBAL.config("Tmp directory: " + tmpDir.getAbsoluteFile());

    if (!outputTestsDirectory.mkdir())
      throw new EoulsanException("Cannot create output tests directory "
          + outputTestsDirectory.getAbsolutePath());

    // Collect all test.txt describing test to launch
    if (testsSelectedPath == null) {
      // Collect all tests
      collectTests(testsData);
    } else {
      // Collect tests from a file with names tests
      collectTestsFromFile(testsData, testsSelectedPath);
    }

  }

  /**
   * Collect all tests present in test directory with a file configuration
   * 'test.txt
   * @param testsDataDirectory
   * @throws EoulsanException
   * @throws IOException
   */
  private void collectTests(final File testsDataDirectory)
      throws EoulsanException, IOException {

    final String prefix = "test";
    final String suffix = ".txt";

    // Parsing all directories test
    for (File dir : testsDataDirectory.listFiles()) {

      // Collect test description file
      final File[] files = dir.listFiles(new FileFilter() {

        @Override
        public boolean accept(File pathname) {
          return pathname.getName().startsWith(prefix)
              && pathname.getName().endsWith(suffix);
        }
      });

      if (files != null && files.length == 1) {
        // Test name
        String nameTest = dir.getName();

        //
        final DataSetTest dst =
            new DataSetTest(this.props, files[0], dir,
                this.outputTestsDirectory);
        this.tests.put(nameTest, dst);
      }

    }

    if (this.tests.size() == 0)
      throw new EoulsanException("None test in "
          + testsDataDirectory.getAbsolutePath());
  }

  /**
   * Collect tests to launch from text files with name tests
   * @param testsDataDirectory
   * @param testsSelectedPath
   * @throws IOException
   * @throws EoulsanException
   */
  private void collectTestsFromFile(final File testsDataDirectory,
      final String testsSelectedPath) throws IOException, EoulsanException {

    final File testsSelectedFile = new File(testsSelectedPath);
    checkExistingFile(testsSelectedFile, " tests selected file doesn't exist.");

    final BufferedReader br =
        new BufferedReader(newReader(testsSelectedFile,
            Charsets.toCharset(Globals.DEFAULT_FILE_ENCODING)));

    String line = null;
    while ((line = br.readLine()) != null) {
      // Skip commentary
      if (line.startsWith("#") || line.trim().length() == 0)
        continue;

      // Add test
      final File testPath = new File(testsDataDirectory, line);

      checkExistingFile(new File(testPath, "test.txt"), "the 'test.txt' file ");

      final DataSetTest dst =
          new DataSetTest(this.props, new File(testPath, "test.txt"), testPath,
              this.outputTestsDirectory);

      this.tests.put(line, dst);

    }
    br.close();

  }

  //
  // Constructor
  //

  public RegressionAction() throws EoulsanException {
    props = new Properties();

    this.tests = Maps.newTreeMap();

  }
}
