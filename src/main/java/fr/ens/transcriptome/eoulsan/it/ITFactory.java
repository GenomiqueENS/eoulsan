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
package fr.ens.transcriptome.eoulsan.it;

import static com.google.common.io.Files.newReader;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingDirectoryFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingStandardFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.createSymbolicLink;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.compress.utils.Charsets;
import org.testng.annotations.Factory;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;

/**
 * This class launch integration test with Testng.
 * @since 1.3
 * @author Laurent Jourdren
 * @author Sandrine Perrin
 */
public class ITFactory {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  // Java system properties keys used for integration tests
  public static final String IT_CONF_PATH_SYSTEM_KEY = "it.conf.path";
  public static final String IT_TEST_LIST_PATH_SYSTEM_KEY = "it.test.list.path";
  public static final String IT_TEST_SYSTEM_KEY = "it.test.name";
  public static final String IT_GENERATE_ALL_EXPECTED_DATA_SYSTEM_KEY =
      "it.generate.all.expected.data";
  public static final String IT_GENERATE_NEW_EXPECTED_DATA_SYSTEM_KEY =
      "it.generate.new.expected.data";
  public static final String IT_APPLICATION_PATH_KEY_SYSTEM_KEY =
      "it.application.path";

  // Configuration properties keys
  static final String TESTS_DIRECTORY_CONF_KEY = "tests.directory";
  static final String OUTPUT_ANALYSIS_DIRECTORY_CONF_KEY =
      "output.analysis.directory";
  static final String LOG_DIRECTORY_CONF_KEY = "log.directory";
  static final String PRE_TEST_SCRIPT_CONF_KEY = "pre.test.script";
  static final String POST_TEST_SCRIPT_CONF_KEY = "post.test.script";
  static final String GENERATE_ALL_EXPECTED_DATA_CONF_KEY =
      "generate.all.expected.data";
  static final String GENERATE_NEW_EXPECTED_DATA_CONF_KEY =
      "generate.new.expected.data";
  static final String DESCRIPTION_CONF_KEY = "description";
  static final String COMMAND_TO_LAUNCH_APPLICATION_CONF_KEY =
      "command.to.launch.application";
  static final String COMMAND_TO_GENERATE_MANUALLY_CONF_KEY =
      "command.to.generate.manually";
  static final String COMMAND_TO_GET_APPLICATION_VERSION_CONF_KEY =
      "command.to.get.application.version";
  static final String INPUT_FILES_PATTERNS_CONF_KEY = "input.files.patterns";
  static final String OUTPUT_FILES_PATTERNS_CONF_KEY = "output.files.patterns";
  static final String MANUAL_GENERATION_EXPECTED_DATA_CONF_KEY =
      "manual.generation.expected.data";

  static final String TEST_CONFIGURATION_FILENAME = "test.conf";

  private static Formatter DATE_FORMATTER = new Formatter().format(
      Globals.DEFAULT_LOCALE, "%1$tY%1$tm%1$td_%1$tH%1$tM%1$tS", new Date());

  private static String outputTestsDirectoryPath;

  // TODO: This timer is never started
  private final Stopwatch timer = Stopwatch.createUnstarted();

  private final Properties globalsConf = new Properties();
  private final File applicationPath;

  // File with tests name to execute
  private final File selectedTestsFile;
  private final String selectedTest;

  private final File testsDataDirectory;
  private final String versionApplication;
  private final File outputTestsDirectory;
  private final String loggerPath;

  /**
   * Create all instance for integrated tests
   * @return array object from integrated tests
   */
  @Factory
  public final Object[] createInstances() {

    // If no test configuration path defined, do nothing
    if (this.applicationPath == null)
      return new Object[0];

    // Set the default local for all the application
    Globals.setDefaultLocale();
    List<ProcessIT> tests = null;
    int testsCount = 0;
    try {
      init();

      tests = collectTests();
      testsCount = tests.size();
      if (testsCount == 0)
        return new Object[0];

      // Return all tests
      return tests.toArray(new Object[tests.size()]);

    } catch (Throwable e) {
      System.err.println(e.getMessage());

    } finally {
      closeLogger(testsCount);
    }

    // Return none test
    return new Object[0];
  }

  /**
   * Initialization factory with principal needed directories
   * @throws IOException if a source file doesn't exist
   */
  private void init() throws IOException {

    // Init logger
    initLogger();

    // Set source directory for tests to execute

    checkExistingDirectoryFile(this.testsDataDirectory, "tests data directory");
    LOGGER.config("Tests data directory: "
        + this.testsDataDirectory.getAbsolutePath());

    // Set output directory
    checkExistingDirectoryFile(this.outputTestsDirectory.getParentFile(),
        "output data parent directory");

    // Set directory contain all tests to execute
    LOGGER.config("Output tests directory: "
        + this.outputTestsDirectory.getAbsolutePath());

    // Create output test directory
    if (!this.outputTestsDirectory.mkdir())
      throw new IOException("Cannot create output tests directory "
          + this.outputTestsDirectory.getAbsolutePath());

    // Path to the latest link
    File latestLink = new File(this.outputTestsDirectory.getParent(), "latest");

    // Remove old link
    if (latestLink.exists()) {
      latestLink.delete();
    }

    // Recreate the link
    createSymbolicLink(this.outputTestsDirectory, latestLink);
  }

  /**
   * Collect all tests to launch from parameter command : in one case all tests
   * present in output test directory, in other case from a list with all name
   * test directory. For each, it checks the file configuration 'test.txt'.
   * @return collection of test directories
   * @throws EoulsanException if an error occurs while create instance for each
   *           test.
   * @throws IOException if the source file doesn't exist
   */
  private List<ProcessIT> collectTests() throws EoulsanException, IOException {

    final List<ProcessIT> tests = Lists.newArrayList();
    final List<File> allTestsDirectories;

    // Collect tests from a file with names tests
    allTestsDirectories = readTestListFile();

    // Add the selected test if set
    if (this.selectedTest != null) {

      allTestsDirectories.add(new File(this.testsDataDirectory,
          this.selectedTest));
    }

    if (allTestsDirectories.size() == 0)
      throw new EoulsanException("None test in file "
          + testsDataDirectory.getAbsolutePath());

    // Build map
    for (File testDirectory : allTestsDirectories) {

      // Ignore file
      if (testDirectory.isFile())
        continue;

      checkExistingDirectoryFile(testDirectory, "the test directory");

      if (!new File(testDirectory, TEST_CONFIGURATION_FILENAME).exists())
        continue;

      // Add test

      // Create instance
      final ProcessIT processIT =
          new ProcessIT(this.globalsConf, this.applicationPath, new File(
              testDirectory, TEST_CONFIGURATION_FILENAME),
              this.outputTestsDirectory, testDirectory.getName());

      // Add in map
      tests.add(processIT);

    }

    return Collections.unmodifiableList(tests);
  }

  /**
   * Collect tests to launch from text files with name tests
   * @return list all directories test found
   * @throws IOException if an error occurs while read file
   */
  private List<File> readTestListFile() throws IOException {

    final List<File> allDirectoriesFound = Lists.newArrayList();

    if (this.selectedTestsFile == null) {
      return allDirectoriesFound;
    }

    checkExistingStandardFile(this.selectedTestsFile, "selected tests file");

    final BufferedReader br =
        new BufferedReader(newReader(this.selectedTestsFile,
            Charsets.toCharset(Globals.DEFAULT_FILE_ENCODING)));

    String nameTest;
    while ((nameTest = br.readLine()) != null) {
      // Skip commentary
      if (nameTest.startsWith("#") || nameTest.trim().length() == 0)
        continue;

      allDirectoriesFound.add(new File(this.testsDataDirectory, nameTest));
    }

    // Close buffer
    br.close();

    return allDirectoriesFound;
  }

  //
  // Methods for logger
  //

  /**
   * Initialize logger.
   * @throws IOException if an error occurs while create logger
   */
  private void initLogger() throws IOException {

    Handler fh = null;
    try {
      fh = new FileHandler(loggerPath);

    } catch (Exception e) {
      throw new IOException(e.getMessage());
    }

    fh.setFormatter(Globals.LOG_FORMATTER);

    LOGGER.setLevel(Level.ALL);
    // Remove output console
    LOGGER.setUseParentHandlers(false);
    LOGGER.addHandler(fh);
    LOGGER.info(Globals.WELCOME_MSG);

  }

  /**
   * Close the logger.
   */
  private void closeLogger(final int testsCount) {

    // Add suffix to log global filename
    LOGGER.fine("End execution of "
        + testsCount + " in "
        + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

    final File loggerFile = new File(this.loggerPath);

    if (loggerFile.exists()) {
      // Create a symbolic link
      createSymbolicLink(loggerFile, this.outputTestsDirectory);
    }
  }

  //
  // Getter
  //
  /**
   * Gets the output test directory path.
   * @return the output test directory path
   */
  public static String getOutputTestDirectoryPath() {
    return outputTestsDirectoryPath;
  }

  //
  // Other methods
  //

  /**
   * Get a File object from a Java System property.
   * @param property the key of the property to get
   * @return a File object or null if the property does not exists
   */
  private static File getFileFromSystemProperty(final String property) {

    if (property == null) {
      return null;
    }

    final String value = System.getProperty(property);
    if (value == null) {
      return null;
    }

    return new File(value);
  }

  /**
   * Get the application path as a File object. If the "it.application.path"
   * system property is set, return a File object pointing to the file, else try
   * to find the application in <tt>./target/dist</tt> directory.
   * @return a File object or null if no application path is found
   */
  private static File getApplicationPath() {

    File dir = getFileFromSystemProperty(IT_APPLICATION_PATH_KEY_SYSTEM_KEY);

    if (dir != null) {
      return dir;
    }

    // Get user dir
    File distDir =
        new File(System.getProperty("user.dir")
            + File.separator + "target" + File.separator + "dist");

    // The dist directory does not exists ?
    if (!distDir.isDirectory()) {
      return null;
    }

    // Set Java property for TestNG
    System.setProperty("maven.testng.output.dir", "");

    // Search if the dist directory only contains an unique directory
    File subDir = null;
    int dirCount = 0;
    int fileCount = 0;

    for (File f : distDir.listFiles()) {

      if (f.getName().startsWith(".")) {
        continue;
      }

      if (f.isDirectory()) {
        dirCount++;
        subDir = f;
      } else if (f.isFile()) {
        fileCount++;
      }
    }

    // There only on directory in dist directory
    if (fileCount == 0 && dirCount == 1) {
      return subDir;
    }

    // Other cases
    return distDir;
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   * @throws EoulsanException if an error occurs when reading configuration
   *           file.
   */
  public ITFactory() throws EoulsanException {

    // Get configuration file path
    File configurationFile = getFileFromSystemProperty(IT_CONF_PATH_SYSTEM_KEY);

    if (configurationFile != null) {

      // Get application path
      this.applicationPath = getApplicationPath();

      // Check if application path exists
      if (this.applicationPath == null || !this.applicationPath.isDirectory()) {
        throw new EoulsanException("The application path doest not exists"
            + this.applicationPath == null
            ? "" : this.applicationPath.toString());
      }

      // Get the file with the list of tests to run
      this.selectedTestsFile =
          getFileFromSystemProperty(IT_TEST_LIST_PATH_SYSTEM_KEY);

      // Get the test to execute
      this.selectedTest = System.getProperty(IT_TEST_SYSTEM_KEY);

      // Load configuration file
      try {

        checkExistingStandardFile(configurationFile, "test configuration file");

        this.globalsConf.load(newReader(configurationFile,
            Charsets.toCharset(Globals.DEFAULT_FILE_ENCODING)));
      } catch (IOException e) {
        throw new EoulsanException("Reading test configuration file fail ("
            + configurationFile.getAbsolutePath() + "): " + e.getMessage());
      }

      // Load command line properties
      // Command generate all expected directories test
      this.globalsConf.put(GENERATE_ALL_EXPECTED_DATA_CONF_KEY,
          Boolean.getBoolean(IT_GENERATE_ALL_EXPECTED_DATA_SYSTEM_KEY));

      // Command generate new expected directories test
      this.globalsConf.put(GENERATE_NEW_EXPECTED_DATA_CONF_KEY,
          Boolean.getBoolean(IT_GENERATE_NEW_EXPECTED_DATA_SYSTEM_KEY));

      // Retrieve application version test
      this.versionApplication =
          ProcessIT
              .retrieveVersionApplication(
                  this.globalsConf
                      .getProperty(ITFactory.COMMAND_TO_GET_APPLICATION_VERSION_CONF_KEY),
                  this.applicationPath);

      // Set logger path
      this.loggerPath =
          this.globalsConf.getProperty(LOG_DIRECTORY_CONF_KEY)
              + "/" + this.versionApplication + "_" + DATE_FORMATTER.toString()
              + ".log";

      // Set test data source directory
      this.testsDataDirectory =
          new File(this.globalsConf.getProperty(TESTS_DIRECTORY_CONF_KEY));

      // Set test data output directory
      this.outputTestsDirectory =
          new File(
              this.globalsConf.getProperty(OUTPUT_ANALYSIS_DIRECTORY_CONF_KEY),
              this.versionApplication + "_" + DATE_FORMATTER.toString());

      // Set output tests directory path to call by Testng instance in Action
      // class
      // TODO: May be a Java system property will be better
      outputTestsDirectoryPath = this.outputTestsDirectory.getAbsolutePath();

    } else {
      // Case no testng must be create when compile project with maven
      this.versionApplication = null;
      this.applicationPath = null;
      this.testsDataDirectory = null;
      this.outputTestsDirectory = null;
      this.loggerPath = null;
      this.selectedTestsFile = null;
      this.selectedTest = null;
    }
  }

}
