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
import java.io.FileFilter;
import java.io.IOException;
import java.util.Date;
import java.util.Formatter;
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
import fr.ens.transcriptome.eoulsan.Globals;

/**
 * This class launch integration test with Testng.
 * @since 1.3
 * @author Laurent Jourdren
 * @author Sandrine Perrin
 */
public class RegressionITFactory {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  /** Key to java properties for Testng */
  public final static String CONF_PATH_KEY = "conf.path";
  public final static String TESTS_FILE_PATH_KEY = "tests.file.path";
  public final static String GENERATE_ALL_EXPECTED_DATA_KEY =
      "generate.all.expected.data";
  public final static String GENERATE_NEW_EXPECTED_DATA_KEY =
      "generate.new.expected.data";

  public final static String APPLI_PATH_KEY = "appli.path";

  private static final Stopwatch TIMER = Stopwatch.createUnstarted();

  private static final Formatter FORMATTED_DATE = new Formatter().format(
      Globals.DEFAULT_LOCALE, "%1$tY%1$tm%1$te_%1$tH%1$tM%1$tS", new Date());

  private final Properties globalsConf;
  private final String applicationPath;
  private final File confFile;

  // File with tests name to execute
  private final File selectedTestsFile;

  private File outputTestsDirectory;
  private File testsDataDirectory;
  private String loggerPath;

  /**
   * Create all instance for integrated tests
   * @return array object from integrated tests
   */
  @Factory
  public Object[] createInstances() {

    // Set the default local for all the application
    Globals.setDefaultLocale();
    Map<String, RegressionProcessIT> tests = null;

    try {
      readConfigurationFile();
      init();

      tests = collectTests();

      // Return all tests
      return tests.values().toArray(new Object[tests.size()]);

    } catch (Throwable e) {
      e.printStackTrace();

    } finally {
      closeLogger(tests.size());
    }

    // Return none test
    return new Object[0];
  }

  /**
   * Retrieve properties from tests configuration
   * @param confFile configuration file from Action
   * @throws EoulsanException
   * @throws IOException
   */
  private void readConfigurationFile() throws IOException {

    checkExistingStandardFile(this.confFile, "configuration file");

    final BufferedReader br =
        new BufferedReader(newReader(this.confFile,
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

      this.globalsConf.put(key, value);

    }
    br.close();

    // Add command line property, use by each execution test
    boolean generateAll =
        System.getProperty(GENERATE_ALL_EXPECTED_DATA_KEY) != null;
    this.globalsConf.put(GENERATE_ALL_EXPECTED_DATA_KEY,
        Boolean.toString(generateAll));

    boolean generateNew =
        System.getProperty(GENERATE_NEW_EXPECTED_DATA_KEY) != null;
    this.globalsConf.put(GENERATE_NEW_EXPECTED_DATA_KEY,
        Boolean.toString(generateNew));
  }

  /**
   * Initialization factory with principal needed directories
   * @throws EoulsanException
   * @throws IOException
   */
  private void init() throws IOException, EoulsanException {

    // Retrieve application version test
    final String versionApplication =
        RegressionProcessIT.retrieveVersionApplication(this.globalsConf
            .getProperty(RegressionProcessIT.CMD_LINE_TO_GET_VERSION_TEST_KEY),
            this.applicationPath);

    // Init logger
    initLogger(this.globalsConf.getProperty("log.path"), versionApplication);

    // Set source directory for tests to execute
    this.testsDataDirectory =
        new File(this.globalsConf.getProperty("tests.directory"));
    checkExistingDirectoryFile(this.testsDataDirectory, "tests data directory");
    LOGGER.config("Tests data directory: "
        + this.testsDataDirectory.getAbsolutePath());

    // Set output directory
    final File output =
        new File(this.globalsConf.getProperty("output.analysis.directory"));
    checkExistingDirectoryFile(output, "output data directory");
    LOGGER.config("Output data directory: " + output.getAbsoluteFile());

    // Set directory contain all tests to execute
    this.outputTestsDirectory =
        new File(output, versionApplication
            + "_" + FORMATTED_DATE.toString());
    LOGGER.config("Output tests directory: "
        + this.outputTestsDirectory.getAbsolutePath());

    // Create output test directory
    if (!this.outputTestsDirectory.mkdir())
      throw new IOException("Cannot create output tests directory "
          + this.outputTestsDirectory.getAbsolutePath());

  }

  private Map<String, RegressionProcessIT> collectTests()
      throws EoulsanException, IOException {

    // Collect all test.txt describing test to launch
    if (this.selectedTestsFile == null) {
      // Collect all tests
      return collectTestsFromDirectory();
    }
    // Collect tests from a file with names tests
    return collectTestsFromFile();
  }

  /**
   * Collect all tests present in test directory with a file configuration
   * 'test.txt
   * @param testsDataDirectory
   * @throws EoulsanException
   * @throws IOException
   */
  private Map<String, RegressionProcessIT> collectTestsFromDirectory()
      throws EoulsanException, IOException {

    final Map<String, RegressionProcessIT> tests = Maps.newTreeMap();

    final String filename = "test.conf";

    // Parsing all directories test
    for (File testDirectory : this.testsDataDirectory.listFiles()) {

      // Collect test description file
      final File[] files = testDirectory.listFiles(new FileFilter() {

        @Override
        public boolean accept(File pathname) {
          return pathname.getName().equals(filename);
        }
      });

      if (files != null && files.length == 1) {
        // Test name
        String nameTest = testDirectory.getName();

        //
        final RegressionProcessIT dst =
            new RegressionProcessIT(this.globalsConf, this.applicationPath,
                files[0], this.outputTestsDirectory, nameTest);
        tests.put(nameTest, dst);
      }

    }

    if (tests.size() == 0)
      throw new EoulsanException("None test in "
          + testsDataDirectory.getAbsolutePath());

    return tests;
  }

  /**
   * Collect tests to launch from text files with name tests
   * @param testsDataDirectory
   * @param testsSelectedPath
   * @throws IOException
   * @throws EoulsanException
   */
  private Map<String, RegressionProcessIT> collectTestsFromFile()
      throws IOException, EoulsanException {

    final Map<String, RegressionProcessIT> tests = Maps.newTreeMap();

    checkExistingStandardFile(this.selectedTestsFile, "selected tests file");

    final BufferedReader br =
        new BufferedReader(newReader(this.selectedTestsFile,
            Charsets.toCharset(Globals.DEFAULT_FILE_ENCODING)));

    String nameTest = null;
    while ((nameTest = br.readLine()) != null) {
      // Skip commentary
      if (nameTest.startsWith("#") || nameTest.trim().length() == 0)
        continue;

      // Add test
      final File testPath = new File(testsDataDirectory, nameTest);

      checkExistingStandardFile(new File(testPath, "test.conf"),
          "the 'test.conf' file ");

      final RegressionProcessIT processIT =
          new RegressionProcessIT(this.globalsConf, this.applicationPath,
              new File(testPath, "test.conf"), this.outputTestsDirectory,
              nameTest);

      tests.put(nameTest, processIT);

    }
    br.close();

    if (tests.size() == 0)
      throw new EoulsanException("None test in file "
          + testsDataDirectory.getAbsolutePath());

    return tests;
  }

  //
  // Methods for logger
  //

  /**
   * Initialize logger.
   * @param logPath file name logger
   * @throws IOException if an error occurs while create logger
   */
  private void initLogger(final String logPath, final String applicationName)
      throws IOException {

    this.loggerPath =
        logPath
            + "/" + applicationName + "_" + FORMATTED_DATE.toString() + ".log";

    Handler fh = null;
    try {
      fh = new FileHandler(loggerPath);

    } catch (Exception e) {
      throw new IOException(e.getMessage());
    }

    fh.setFormatter(Globals.LOG_FORMATTER);

    LOGGER.setLevel(Level.ALL);
    // LOGGER.setUseParentHandlers(false);
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
   * @throws EoulsanException
   */
  public RegressionITFactory() throws EoulsanException {

    if (System.getProperty(CONF_PATH_KEY) == null) {
      throw new EoulsanException(
          "Configuration file path not define in java properties");
    }

    if (System.getProperty(APPLI_PATH_KEY) == null) {
      throw new EoulsanException(
          "Application path not define in java properties");
    }

    this.confFile = new File(System.getProperty(CONF_PATH_KEY));
    this.applicationPath = System.getProperty(APPLI_PATH_KEY);

    if (System.getProperty(TESTS_FILE_PATH_KEY) != null) {
      this.selectedTestsFile =
          new File(System.getProperty(TESTS_FILE_PATH_KEY));
    } else {
      this.selectedTestsFile = null;
    }

    this.globalsConf = new Properties();

  }

}
