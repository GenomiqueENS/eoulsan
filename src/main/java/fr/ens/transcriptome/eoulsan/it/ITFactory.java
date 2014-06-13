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

  /** Key to java properties for Testng */
  public final static String CONF_PATH_KEY = "conf.path";
  public final static String TESTS_FILE_PATH_KEY = "tests.file.path";
  public final static String GENERATE_ALL_EXPECTED_DATA_KEY =
      "generate.all.expected.data";
  public final static String GENERATE_NEW_EXPECTED_DATA_KEY =
      "generate.new.expected.data";

  public final static String APPLICATION_PATH_KEY = "application.path";

  private final static Stopwatch TIMER = Stopwatch.createUnstarted();

  private static Formatter FORMATTED_DATE = new Formatter().format(
      Globals.DEFAULT_LOCALE, "%1$tY%1$tm%1$td_%1$tH%1$tM%1$tS", new Date());

  private final Properties globalsConf;
  private final File applicationPath;
  private final File configurationFile;

  // File with tests name to execute
  private final File selectedTestsFile;

  private final File testsDataDirectory;
  private final String versionApplication;
  private final File outputTestsDirectory;
  private final String loggerPath;

  /**
   * Create all instance for integrated tests
   * @return array object from integrated tests
   */
  @Factory
  public Object[] createInstances() {

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
  private List<ProcessIT> collectTests() throws EoulsanException,
      IOException {

    final List<ProcessIT> tests = Lists.newArrayList();
    final List<File> allTestsDirectories;

    // Collect all test.txt describing test to launch
    if (this.selectedTestsFile == null) {
      // Collect all tests
      allTestsDirectories =
          Lists.newArrayList(this.testsDataDirectory.listFiles());
    } else {
      // Collect tests from a file with names tests
      allTestsDirectories = readFileAsList();
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

      if (!new File(testDirectory, "test.conf").exists())
        continue;

      // Add test

      // Create instance
      final ProcessIT processIT =
          new ProcessIT(this.globalsConf, this.applicationPath,
              new File(testDirectory, "test.conf"), this.outputTestsDirectory,
              testDirectory.getName());

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
  private List<File> readFileAsList() throws IOException {

    final List<File> allDirectoriesFound = Lists.newArrayList();

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
        + toTimeHumanReadable(TIMER.elapsed(TimeUnit.MILLISECONDS)));

    final File loggerFile = new File(this.loggerPath);

    if (loggerFile.exists()) {
      // Create a symbolic link
      createSymbolicLink(loggerFile, this.outputTestsDirectory);
    }
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   * @throws EoulsanException if an error occurs when reading configuration file.
   */
  public ITFactory() throws EoulsanException {

    if (System.getProperty(CONF_PATH_KEY) != null) {
      this.configurationFile = new File(System.getProperty(CONF_PATH_KEY));
    } else {
      this.configurationFile = null;
    }

    if (System.getProperty(APPLICATION_PATH_KEY) != null) {
      this.applicationPath = new File(System.getProperty(APPLICATION_PATH_KEY));
    } else {
      this.applicationPath = null;
    }

    if (this.configurationFile != null && this.applicationPath != null) {

      if (System.getProperty(TESTS_FILE_PATH_KEY) != null) {
        this.selectedTestsFile =
            new File(System.getProperty(TESTS_FILE_PATH_KEY));
      } else {
        this.selectedTestsFile = null;
      }

      // Load configuration file
      this.globalsConf = new Properties();
      try {
        this.globalsConf.load(newReader(this.configurationFile,
            Charsets.toCharset(Globals.DEFAULT_FILE_ENCODING)));
      } catch (IOException e) {
        throw new EoulsanException("Configuration is missing ("
            + this.configurationFile.getAbsolutePath() + ")");
      }

      // Load command line properties
      // Command generate all expected directories test
      String val = System.getProperty(GENERATE_ALL_EXPECTED_DATA_KEY);
      val = (val == null ? "false" : (val.trim().toLowerCase()));
      this.globalsConf.put(GENERATE_ALL_EXPECTED_DATA_KEY, val);

      // Command generate new expected directories test
      val = System.getProperty(GENERATE_NEW_EXPECTED_DATA_KEY);
      val = (val == null ? "false" : (val.trim().toLowerCase()));
      this.globalsConf.put(GENERATE_NEW_EXPECTED_DATA_KEY, val);

      // Retrieve application version test
      this.versionApplication =
          ProcessIT
              .retrieveVersionApplication(
                  this.globalsConf
                      .getProperty(ProcessIT.COMMAND_TO_GET_VERSION_APPLICATION_KEY),
                  this.applicationPath);

      // Init logger path
      this.loggerPath =
          this.globalsConf.getProperty("log.directory")
              + "/" + this.versionApplication + "_" + FORMATTED_DATE.toString()
              + ".log";

      // Init test data source directory
      this.testsDataDirectory =
          new File(this.globalsConf.getProperty("tests.directory"));

      // Init test data output directory
      this.outputTestsDirectory =
          new File(this.globalsConf.getProperty("output.analysis.directory"),
              this.versionApplication + "_" + FORMATTED_DATE.toString());

    } else {
      // Case no testng must be create when compile project with maven
      this.globalsConf = null;
      this.versionApplication = null;
      this.testsDataDirectory = null;
      this.outputTestsDirectory = null;
      this.loggerPath = null;
      this.selectedTestsFile = null;

    }
  }
}
