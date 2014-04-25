package fr.ens.transcriptome.eoulsan.it;

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
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.compress.utils.Charsets;
import org.testng.annotations.Factory;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanITRuntimeException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class ITActionFactory {

  /** Key to java properties for Testng */
  public final static String CONF_PATH_KEY = "conf.path";
  public final static String TESTS_FILE_PATH_KEY = "tests.file.path";
  public final static String GENERATE_ALL_EXPECTED_DATA_KEY =
      "generate.all.expected.data";
  public final static String GENERATE_NEW_EXPECTED_DATA_KEY =
      "generate.new.expected.data";

  public final static String APPLI_PATH_KEY = "appli.path";

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final Stopwatch TIMER = Stopwatch.createUnstarted();

  public static final DateFormat DATE_FORMAT = new SimpleDateFormat(
      "yyyyMMdd-kkmmss", Globals.DEFAULT_LOCALE);

  private final Properties props;
  private final File confFile;
  private final String applicationPath;
  private final File fileListAllTests;
  private final boolean generateAllExpectedData;
  private final boolean generateNewExpectedData;

  private File outputTestsDirectory;
  private String logGlobalPath;
  private boolean exceptionThrowGlobal = false;

  private final Map<String, DataSetTest> tests;

  @Factory
  public Object[] createInstances() {

    // Set the default local for all the application
    Globals.setDefaultLocale();

    try {
      init(confFile, generateAllExpectedData, generateNewExpectedData);
      configure(applicationPath, fileListAllTests);

    } catch (Exception e) {
      e.printStackTrace();
    }

    return getAllTests().toArray(new Object[getAllTests().size()]);
  }

  /**
   * Initialization properties from tests configuration
   * @param conf configuration file from Action
   * @throws EoulsanException
   * @throws IOException
   */
  public void init(final File conf, final boolean generateAllExpectedData,
      final boolean generateNewExpectedData) {

    try {
      checkExistingFile(conf, " configuration file ");

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

      // Add command line property
      this.props.put(GENERATE_ALL_EXPECTED_DATA_KEY, generateAllExpectedData);
      this.props.put(GENERATE_NEW_EXPECTED_DATA_KEY, generateNewExpectedData);

      initLoggerGlobal(this.props.getProperty("log.path"));

    } catch (Exception e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();

      LOGGER.severe(e1.getMessage());
      closeLoggerGlobal();
    }

  }

  private void configure(final String applicationPath,
      final File fileListAllTests) throws EoulsanException, IOException {

    // Initializationthis.inputData, " input data directory ");

    final File testsDataDirectory =
        new File(this.props.getProperty("tests.directory"));
    checkExistingFile(testsDataDirectory, " tests data directory ");
    LOGGER.config("Tests data directory: "
        + testsDataDirectory.getAbsolutePath());

    final File output =
        new File(this.props.getProperty("output.analysis.directory"));
    checkExistingFile(output, " output data directory ");
    LOGGER.config("Output data directory: " + output.getAbsoluteFile());

    final String versionAppliTest =
        DataSetTest.retrieveVersionApplication(this.props,
            DataSetTest.CMD_LINE_TO_GET_VERSION_TEST_KEY, applicationPath);

    // TODO retrieve application version test
    this.outputTestsDirectory =
        new File(output, versionAppliTest
            + "_" + DATE_FORMAT.format(new Date()));
    LOGGER.config("Output tests directory: "
        + this.outputTestsDirectory.getAbsolutePath());

    if (!outputTestsDirectory.mkdir())
      throw new EoulsanException("Cannot create output tests directory "
          + outputTestsDirectory.getAbsolutePath());

    // Collect all test.txt describing test to launch
    if (fileListAllTests == null) {
      // Collect all tests
      collectTests(testsDataDirectory, applicationPath);
    } else {
      // Collect tests from a file with names tests
      collectTestsFromFile(testsDataDirectory, applicationPath,
          fileListAllTests);
    }

  }

  /**
   * Collect all tests present in test directory with a file configuration
   * 'test.txt
   * @param testsDataDirectory
   * @throws EoulsanException
   * @throws IOException
   */
  private void collectTests(final File testsDataDirectory,
      final String applicationPath) throws EoulsanException, IOException {

    final String prefix = "test";
    final String suffix = ".conf";

    // Parsing all directories test
    for (File testDirectory : testsDataDirectory.listFiles()) {

      // Collect test description file
      final File[] files = testDirectory.listFiles(new FileFilter() {

        @Override
        public boolean accept(File pathname) {
          return pathname.getName().startsWith(prefix)
              && pathname.getName().endsWith(suffix);
        }
      });

      if (files != null && files.length == 1) {
        // Test name
        String nameTest = testDirectory.getName();

        //
        final DataSetTest dst =
            new DataSetTest(this.props, applicationPath, files[0],
                testDirectory, this.outputTestsDirectory, nameTest);
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
      final String applicationPath, final File fileListAllTests)
      throws IOException, EoulsanException {

    checkExistingFile(fileListAllTests, " tests selected file doesn't exist.");

    final BufferedReader br =
        new BufferedReader(newReader(fileListAllTests,
            Charsets.toCharset(Globals.DEFAULT_FILE_ENCODING)));

    String nameTest = null;
    while ((nameTest = br.readLine()) != null) {
      // Skip commentary
      if (nameTest.startsWith("#") || nameTest.trim().length() == 0)
        continue;

      // Add test
      final File testPath = new File(testsDataDirectory, nameTest);

      checkExistingFile(new File(testPath, "test.conf"),
          "the 'test.conf' file ");

      final DataSetTest dst =
          new DataSetTest(this.props, applicationPath, new File(testPath,
              "test.conf"), testPath, this.outputTestsDirectory, nameTest);

      this.tests.put(nameTest, dst);

    }
    br.close();

  }

  //
  // Methods for logger
  //

  public void initLoggerGlobal(final String logPath) throws EoulsanException {
    logGlobalPath =
        logPath + "/eoulsan_" + DATE_FORMAT.format(new Date()) + ".log";

    Handler fh = null;
    try {
      fh = new FileHandler(logGlobalPath);

    } catch (Exception e) {
      throw new EoulsanException(e.getMessage());
    }

    fh.setFormatter(Globals.LOG_FORMATTER);

    LOGGER.setLevel(Level.ALL);
    // LOGGER.setUseParentHandlers(false);
    LOGGER.addHandler(fh);
    LOGGER.info(Globals.WELCOME_MSG);

  }

  private void closeLoggerGlobal() {

    // Set suffix logger filename
    final String suffix =
        (exceptionThrowGlobal ? "EXCEPTION" : (false ? "FAIL" : "SUCCES"));

    // Add suffix to log global filename
    LOGGER.fine(suffix
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

  //
  // Getter
  //

  public Collection<DataSetTest> getAllTests() {
    return this.tests.values();
  }

  public Properties getProperties() {
    return this.props;
  }

  //
  // Constructor
  //

  public ITActionFactory() throws EoulsanITRuntimeException {

    if (System.getProperty(CONF_PATH_KEY) == null) {
      throw new EoulsanITRuntimeException(
          "Configuration file path not define in java properties");

    }

    if (System.getProperty(APPLI_PATH_KEY) == null) {
      throw new EoulsanITRuntimeException(
          "Application path not define in java properties");
    }

    this.props = new Properties();
    this.tests = Maps.newTreeMap();

    this.confFile = new File(System.getProperty(CONF_PATH_KEY));
    this.applicationPath = System.getProperty(APPLI_PATH_KEY);

    if (System.getProperty(TESTS_FILE_PATH_KEY) != null) {
      this.fileListAllTests = new File(System.getProperty(TESTS_FILE_PATH_KEY));
    } else {
      this.fileListAllTests = null;
    }

    if (System.getProperty(GENERATE_ALL_EXPECTED_DATA_KEY) != null) {
      this.generateAllExpectedData =
          Boolean.parseBoolean(System
              .getProperty(GENERATE_ALL_EXPECTED_DATA_KEY));
    } else {
      this.generateAllExpectedData = false;
    }

    if (System.getProperty(GENERATE_NEW_EXPECTED_DATA_KEY) != null) {
      this.generateNewExpectedData =
          Boolean.parseBoolean(System
              .getProperty(GENERATE_NEW_EXPECTED_DATA_KEY));
    } else {
      this.generateNewExpectedData = false;
    }

  }
}
