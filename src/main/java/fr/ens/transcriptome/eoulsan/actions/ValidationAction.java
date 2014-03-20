package fr.ens.transcriptome.eoulsan.actions;

import static com.google.common.io.Files.newReader;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.createSymbolicLink;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
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

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.data.DataSetAnalysis;
import fr.ens.transcriptome.eoulsan.data.DataSetTest;
import fr.ens.transcriptome.eoulsan.io.ComparatorDirectories;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class ValidationAction extends AbstractAction {

  public static final String LOGGER_TESTS_GLOBAL = "tests_global";
  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);
  private static final Logger LOGGER_GLOBAL = Logger
      .getLogger(LOGGER_TESTS_GLOBAL);

  public static DateFormat DATE_FORMAT = new SimpleDateFormat(
      "yyyyMMdd-kkmmss", Globals.DEFAULT_LOCALE);

  public static final boolean USE_SERIALIZATION = true;
  public static final boolean CHECKING_SAME_NAME = true;

  private final Properties props;
  private final ComparatorDirectories comparator;

  private File inputData;
  private File outputTestsDirectory;
  private String logGlobalPath;
  private String logTestPath;
  private boolean noRegressionGlobal = true;
  private boolean exceptionThrowGlobal = false;

  private final Map<String, DataSetTest> tests;

  @Override
  public String getName() {
    return "validation";
  }

  @Override
  public String getDescription() {
    return "test " + Globals.APP_NAME + " version.";
  }

  private void initLogger(final String dirTestPath, final String nameTest) {

    // Close previous logger test file
    if (this.logTestPath != null) {
      for (Handler handler : LOGGER.getHandlers()) {
        LOGGER.removeHandler(handler);
      }

      // Remove lock logger file
      if (!new File(logTestPath + ".lck").delete())
        // TODO
        System.out.println("Fail remove lock logger file: "
            + logTestPath + ".lck");
    }

    // Init new logger for a current test
    logTestPath = dirTestPath + "/test_" + nameTest + ".log";

    Handler fh = null;
    try {
      fh = new FileHandler(logTestPath);

    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    fh.setFormatter(Globals.LOG_FORMATTER);

    LOGGER.setLevel(Level.ALL);
    // TODO to remove after test
    LOGGER.setUseParentHandlers(false);

    LOGGER.addHandler(fh);
  }

  // TODO use for test into eclipse
  public void initLoggerGlobal(final String logPath) {
    logGlobalPath =
        logPath + "/eoulsan_" + DATE_FORMAT.format(new Date()) + ".log";

    Handler fh = null;
    try {
      fh = new FileHandler(logGlobalPath);

    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    fh.setFormatter(Globals.LOG_FORMATTER);

    LOGGER_GLOBAL.setLevel(Level.ALL);
    // LOGGER.setUseParentHandlers(false);
    LOGGER_GLOBAL.addHandler(fh);

    LOGGER_GLOBAL.info(Globals.WELCOME_MSG);

  }

  private void closeLoggerGlobal() {

    final String suffix =
        exceptionThrowGlobal ? "EXCEPTION" : (noRegressionGlobal
            ? "SUCCES" : "FAIL");

    // Add suffix to log global filename
    final File destFile =
        new File(StringUtils.filenameWithoutExtension(this.logGlobalPath)
            + "_" + suffix + ".log");

    final File logFile = new File(logGlobalPath);

    // Create a symbolic link
    if (logFile.exists())
      if (logFile.renameTo(destFile))
        createSymbolicLink(destFile, this.outputTestsDirectory);

  }

  private void closeLoggerTest(final boolean noRegressionTest,
      final boolean exceptionTest) {
    final String suffix =
        exceptionTest ? "EXCEPTION" : (noRegressionTest ? "SUCCES" : "FAIL");

    // Add suffix to log global filename
    final File sourceFile =
        new File(StringUtils.filenameWithoutExtension(this.logTestPath)
            + "_" + suffix + ".log");

    final File logFile = new File(logTestPath);

    // Create a symbolic link
    if (logFile.exists())
      if (logFile.renameTo(sourceFile))
        return;
  }

  @Override
  public void action(String[] arguments) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    String confPath = null;
    String jobDescription = null;

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

    } catch (ParseException e) {
      Common.errorExit(e,
          "Error while parsing parameter file: " + e.getMessage());
    }

    if (arguments.length != argsOptions) {
      help(options);
    }

    // Execute program in local mode
    run(confPath, jobDescription);
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
  private void run(final String confPath, final String jobDescription) {

    final String desc;

    if (jobDescription == null) {
      desc = "no job description";
    } else {
      desc = jobDescription.trim();
    }

    LOGGER_GLOBAL.info(desc);

    // Initialization action from configuration test file
    try {
      init(new File(confPath));

    } catch (EoulsanException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    LOGGER_GLOBAL.info("Number tests defined " + this.tests.size());

    for (Map.Entry<String, DataSetTest> test : this.tests.entrySet()) {
      final String testName = test.getKey();
      final DataSetTest dst = test.getValue();
      Exception exception = null;
      String reportTest = "";

      try {
        initLogger(dst.getTestedDirectory().getAbsolutePath(), testName);
        String loggerFilename = new File(logTestPath).getName();
        exception = null;
        LOGGER.info("Execute test " + testName);

        dst.executeTest();

        // Collect data expected for project
        final DataSetAnalysis setExpected = dst.getAnalysisExcepted();

        // Collect data tested
        final DataSetAnalysis setTested = dst.getAnalysisTest();

        final String filesToIgnoreCurrentTest =
            dst.getFilesToIngore()
                + "," + loggerFilename + "," + loggerFilename + ".lck";
        comparator.setFilesToNotCompare(filesToIgnoreCurrentTest);

        // Launch comparison
        comparator.compareDataSet(setExpected, setTested, testName);

        reportTest =
            comparator.buildReport(dst.isCheckingExistingFiles(), testName);
        noRegressionGlobal = noRegressionGlobal && comparator.asNoRegression();

        // Remove filename to ignore specific for this test
        this.comparator.removeFilesToNoCompare(filesToIgnoreCurrentTest);

        // TODO Auto-generated catch block
      } catch (IOException e) {
        exception = e;
        e.printStackTrace();

      } catch (EoulsanException ee) {
        exception = ee;
        ee.printStackTrace();

      } finally {

        exceptionThrowGlobal = exceptionThrowGlobal || (exception != null);

        // Add entry in logger for assessment test
        if (exception == null) {
          if (comparator.asNoRegression()) {
            LOGGER_GLOBAL.info(reportTest);
          } else {
            LOGGER_GLOBAL.severe(reportTest);
          }
        } else {

          // Exception throws
          final String msg =
              exception.getClass().getName()
                  + ": " + exception.getMessage() + "\n"
                  + StringUtils.join(exception.getStackTrace(), "\n\t");

          LOGGER.severe(msg);

          LOGGER_GLOBAL.severe("Exception throw with test:" + testName);
          LOGGER_GLOBAL.severe(msg);
        }
        // End run current test
        closeLoggerTest(comparator.asNoRegression(), (exception != null));
      }
    }
    // End run all tests
    closeLoggerGlobal();

  }

  /**
   * Initialization properties with pa
   * @param conf configuration file from Action
   * @throws EoulsanException
   * @throws IOException
   */
  private void init(final File conf) throws EoulsanException, IOException {

    checkExistingFile(conf, " configuration file doesn't exist.");

    final BufferedReader br =
        new BufferedReader(newReader(conf,
            Charsets.toCharset(Globals.DEFAULT_FILE_ENCODING)));
    String line = null;

    try {
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

    } catch (IOException e) {
      e.printStackTrace();
    }

    initLoggerGlobal(this.props.getProperty("log_path"));

    // Initialization
    this.inputData = new File(this.props.getProperty("input_directory"));
    FileUtils.checkExistingFile(this.inputData, " input data directory ");
    LOGGER_GLOBAL.config("Input data directory: "
        + this.inputData.getAbsolutePath());

    final File testsData = new File(this.props.getProperty("tests_directory"));
    FileUtils.checkExistingFile(testsData, " tests data directory ");
    LOGGER_GLOBAL
        .config("Tests data directory: " + testsData.getAbsolutePath());

    final File output =
        new File(this.props.getProperty("output_analysis_directory"));
    FileUtils.checkExistingFile(output, " output data directory ");
    LOGGER_GLOBAL.config("Output data directory: " + output.getAbsoluteFile());

    this.outputTestsDirectory =
        new File(output, props.getProperty("eoulsan_test_version_git")
            + "_" + DATE_FORMAT.format(new Date()));
    LOGGER_GLOBAL.config("Output tests directory: "
        + this.outputTestsDirectory.getAbsolutePath());

    if (!outputTestsDirectory.mkdir())
      throw new EoulsanException("Cannot create output tests directory "
          + outputTestsDirectory.getAbsolutePath());

    LOGGER_GLOBAL.config("Comparator param: use serialization file "
        + USE_SERIALIZATION);
    LOGGER_GLOBAL.config("Comparison files with extensions: "
        + Joiner.on(", ").join(comparator.getAllExtensionsTreated()));

    // Collect all test.txt describing test to launch
    collectTests(testsData);

    // TODO is useful ???
    // Set not compare eoulsan.log
    comparator.setFilesToNotCompare(this.props
        .getProperty("files_ignored_for_comparison"));

  }

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
            new DataSetTest(props, files[0], dir, this.outputTestsDirectory);
        this.tests.put(nameTest, dst);
      }

    }

    if (this.tests.size() == 0)
      throw new EoulsanException("None test in "
          + testsDataDirectory.getAbsolutePath());
  }

  //
  // Constructor
  //

  public ValidationAction() throws EoulsanException {
    props = new Properties();

    // Initialization comparator
    this.comparator =
        new ComparatorDirectories(USE_SERIALIZATION, CHECKING_SAME_NAME);

    this.tests = Maps.newHashMap();

  }
}
