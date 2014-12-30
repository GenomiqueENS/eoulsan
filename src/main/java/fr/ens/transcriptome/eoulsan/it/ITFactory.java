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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.compress.utils.Charsets;
import org.testng.annotations.Factory;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;

/**
 * This class launch integration test with Testng.
 * @since 2.0
 * @author Laurent Jourdren
 * @author Sandrine Perrin
 */
public class ITFactory {

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
  public static final String IT_DEBUG_ENABLE_SYSTEM_KEY = "it.debug.enable";

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
  static final String INCLUDE_CONF_KEY = "include";

  /** Patterns */
  static final String FILE_TO_COMPARE_PATTERNS_CONF_KEY =
      "file.to.compare.patterns";
  static final String EXCLUDE_TO_COMPARE_PATTERNS_CONF_KEY =
      "exclude.to.compare.patterns";
  static final String CHECK_EXISTENCE_FILE_PATTERNS_CONF_KEY =
      "file.to.check.existence.patterns";
  static final String CHECK_ABSENCE_FILE_PATTERNS_CONF_KEY =
      "file.to.check.absence.patterns";

  static final String MANUAL_GENERATION_EXPECTED_DATA_CONF_KEY =
      "manual.generation.expected.data";

  static final String PRETREATMENT_GLOBAL_SCRIPT_KEY = "pre.global.script";
  static final String POSTTREATMENT_GLOBAL_SCRIPT_KEY = "post.global.script";

  static final String APPLICATION_PATH_VARIABLE = "application.path";

  static final String TEST_CONFIGURATION_FILENAME = "test.conf";

  // private static String outputTestsDirectoryPath;

  private static final Properties CONSTANTS = initConstants();

  private final Properties globalsConf;
  private final File applicationPath;

  // File with tests name to execute
  private final File selectedTestsFile;
  private final String selectedTest;
  private final File testsDataDirectory;
  private final Map<String, File> testsDirectoryFoundToExecute;

  // private final String versionApplication;
  // private final File outputTestsDirectory;
  //
  // private final String loggerPath;
  // private final boolean generateAllExpectedDirectoryTest;
  // private final boolean generateNewExpectedDirectoryTest;

  /**
   * Create all instance for integrated tests.
   * @return array object from integrated tests
   */
  @Factory
  public final Object[] createInstances() {

    // If no test configuration path defined, do nothing
    if (this.applicationPath == null) {
      return new Object[0];
    }

    // Set the default local for all the application
    Globals.setDefaultLocale();
    try {

      final int testsCount = ITSuite.getInstance().getCountTest();

      if (testsCount == 0) {
        return new Object[0];
      }

      // Return all tests
      return ITSuite.getInstance().getTestsInstanceToArray();

    } catch (final Throwable e) {
      System.err.println(e.getMessage());

    }

    // Return none test
    return new Object[0];
  }

  //
  // Methods to collect tests
  //

  /**
   * Collect all tests to launch from parameter command : in one case all tests
   * present in output test directory, in other case from a list with all name
   * test directory. For each, it checks the file configuration 'test.txt'.
   * @return collection of test directories
   * @throws EoulsanException if an error occurs while create instance for each
   *           test.
   * @throws IOException if the source file doesn't exist
   */
  private Map<String, File> collectTestsDirectoryToExecute() throws EoulsanException, IOException {

    // final List<IT> tests = new ArrayList<>();
    final Map<String, File> result = new HashMap<>();
    final List<File> testsToExecuteDirectories = new ArrayList<>();

    // Collect tests from a file with names tests
    testsToExecuteDirectories.addAll(readTestListFile());

    // Add the selected test if set
    if (this.selectedTest != null) {

      testsToExecuteDirectories.add(new File(this.testsDataDirectory,
          this.selectedTest));
    }

    // If no test was defined by user use all the existing tests
    final File[] files = this.testsDataDirectory.listFiles();
    if (files != null && testsToExecuteDirectories.isEmpty()) {
      testsToExecuteDirectories.addAll(Arrays.asList(files));
    }

    if (testsToExecuteDirectories.size() == 0) {
      throw new EoulsanException("None test directory found in "
          + this.testsDataDirectory.getAbsolutePath());
    }

    // Build map
    for (final File testDirectory : testsToExecuteDirectories) {

      // Ignore file
      if (testDirectory.isFile()) {
        continue;
      }

      checkExistingDirectoryFile(testDirectory, "test directory");

      if (!new File(testDirectory, TEST_CONFIGURATION_FILENAME).exists()) {
        continue;
      }

      result.put(testDirectory.getName(), testDirectory);
    }

    return Collections.unmodifiableMap(result);
  }

  // // Create instance
  // final IT processIT =
  // new IT(this.globalsConf, this.applicationPath, new File(
  // testDirectory, TEST_CONFIGURATION_FILENAME),
  // this.outputTestsDirectory, testDirectory.getName());
  //
  // // Add tests
  // tests.add(processIT);
  // }
  //
  // // Check tests founded
  // if (tests.size() == 0) {
  // throw new EoulsanException(
  // "None test define (with test.conf) in directory "
  // + this.testsDataDirectory.getAbsolutePath());
  // }
  //
  // return Collections.unmodifiableList(tests);
  // }

  /**
   * Collect tests to launch from text files with name tests.
   * @return list all directories test found
   * @throws IOException if an error occurs while read file
   */
  private List<File> readTestListFile() throws IOException {

    final List<File> result = new ArrayList<>();

    if (this.selectedTestsFile == null) {
      return Collections.emptyList();
    }

    checkExistingStandardFile(this.selectedTestsFile, "selected tests file");

    final BufferedReader br =
        new BufferedReader(newReader(this.selectedTestsFile,
            Charsets.toCharset(Globals.DEFAULT_FILE_ENCODING)));

    String nameTest;
    while ((nameTest = br.readLine()) != null) {
      // Skip commentary
      if (nameTest.startsWith("#") || nameTest.isEmpty()) {
        continue;
      }

      result.add(new File(this.testsDataDirectory, nameTest.trim()));
    }

    // Close buffer
    br.close();

    return result;
  }

  //
  // Methods to load and read configuration and properties
  //

  /**
   * Initialize the constants values.
   * @return a map with the constants
   */
  private static Properties initConstants() {

    final Properties constants = new Properties();

    // Add java properties
    for (final Map.Entry<Object, Object> e : System.getProperties().entrySet()) {
      constants.put(e.getKey(), e.getValue());
    }

    // Add environment properties
    for (final Map.Entry<String, String> e : System.getenv().entrySet()) {
      constants.put(e.getKey(), e.getValue());
    }

    return constants;
  }

  /**
   * Load configuration file in properties object.
   * @param configurationFile configuration file
   * @return properties
   * @throws IOException if an error occurs when reading file.
   * @throws EoulsanException if an error occurs evaluate value property.
   */
  private static Properties loadProperties(final File configurationFile)
      throws IOException, EoulsanException {

    final Properties rawProps = new Properties();
    final Properties props;

    checkExistingStandardFile(configurationFile, "test configuration file");

    // Load configuration file
    rawProps.load(newReader(configurationFile,
        Charsets.toCharset(Globals.DEFAULT_FILE_ENCODING)));

    props = evaluateProperties(rawProps);

    // Check include
    final String includeOption = props.getProperty(INCLUDE_CONF_KEY);

    if (includeOption != null) {
      // Check configuration file
      final File otherConfigurationFile = new File(includeOption);

      checkExistingStandardFile(otherConfigurationFile,
          "configuration file doesn't exist");

      // Load configuration in global configuration
      final Properties rawNewProps = new Properties();
      rawNewProps.load(newReader(otherConfigurationFile,
          Charsets.toCharset(Globals.DEFAULT_FILE_ENCODING)));

      final Properties newProps = evaluateProperties(rawNewProps);

      for (final String propertyName : newProps.stringPropertyNames()) {
        props.put(propertyName, newProps.getProperty(propertyName));
      }
    }

    return props;
  }

  /**
   * Evaluate properties.
   * @param rawProps the raw props
   * @return the properties
   * @throws EoulsanException the Eoulsan exception
   */
  private static Properties evaluateProperties(final Properties rawProps)
      throws EoulsanException {
    final Properties props = new Properties();

    // Extract environment variable
    for (final String propertyName : rawProps.stringPropertyNames()) {
      if (propertyName.startsWith(IT.PREFIX_ENV_VAR)) {
        CONSTANTS.put(propertyName.substring(IT.PREFIX_ENV_VAR.length()),
            rawProps.getProperty(propertyName));
      }
    }

    // Evaluate property
    for (final String propertyName : rawProps.stringPropertyNames()) {
      final String propertyValue =
          evaluateExpressions(rawProps.getProperty(propertyName), true);

      props.setProperty(propertyName, propertyValue);

    }

    return props;
  }

  /**
   * Evaluate expression in a string.
   * @param s string in witch expression must be replaced
   * @param allowExec allow execution of code
   * @return a string with expression evaluated
   * @throws EoulsanException if an error occurs while parsing the string or
   *           executing an expression
   */
  static String evaluateExpressions(final String s, final boolean allowExec)
      throws EoulsanException {

    if (s == null) {
      return null;
    }

    final StringBuilder result = new StringBuilder();

    final int len = s.length();

    for (int i = 0; i < len; i++) {

      final int c0 = s.codePointAt(i);

      // Variable substitution
      if (c0 == '$' && i + 1 < len) {

        final int c1 = s.codePointAt(i + 1);
        if (c1 == '{') {

          final String expr = subStr(s, i + 2, '}');

          final String trimmedExpr = expr.trim();
          if (CONSTANTS.containsKey(trimmedExpr)) {
            result.append(CONSTANTS.get(trimmedExpr));
          }

          i += expr.length() + 2;
          continue;
        }
      }

      // Command substitution
      if (c0 == '`' && allowExec) {
        final String expr = subStr(s, i + 1, '`');
        try {
          final String r =
              ProcessUtils.execToString(evaluateExpressions(expr, false));

          // remove last '\n' in the result
          if (r.charAt(r.length() - 1) == '\n') {
            result.append(r.substring(0, r.length() - 1));
          } else {
            result.append(r);
          }

        } catch (final IOException e) {
          throw new EoulsanException("Error while evaluating expression \""
              + expr + "\"");
        }
        i += expr.length() + 1;
        continue;
      }

      result.appendCodePoint(c0);
    }

    return result.toString();
  }

  private static String subStr(final String s, final int beginIndex,
      final int charPoint) throws EoulsanException {

    final int endIndex = s.indexOf(charPoint, beginIndex);

    if (endIndex == -1) {
      throw new EoulsanException("Unexpected end of expression in \""
          + s + "\"");
    }

    return s.substring(beginIndex, endIndex);
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
   * Get a Boolean object from a Java System property.
   * @param property the key of the property to get
   * @return a Boolean object or false if the property does not exists
   */
  private static Boolean getBooleanFromSystemProperty(final String property) {

    return (property != null) && Boolean.getBoolean(property);
  }

  /**
   * Get the application path as a File object. If the "it.application.path"
   * system property is set, return a File object pointing to the file, else try
   * to find the application in <tt>./target/dist</tt> directory.
   * @return a File object or null if no application path is found
   */
  private static File getApplicationPath() {

    final File dir =
        getFileFromSystemProperty(IT_APPLICATION_PATH_KEY_SYSTEM_KEY);

    if (dir != null) {
      return dir;
    }

    // Get user dir
    final File distDir =
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

    final File[] files = distDir.listFiles();
    if (files != null) {
      for (final File f : files) {

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
   * Public constructor.
   * @throws EoulsanException if an error occurs when reading configuration
   *           file.
   * @throws IOException
   */
  public ITFactory() throws EoulsanException, IOException {

    // Get configuration file path
    final File configurationFile =
        getFileFromSystemProperty(IT_CONF_PATH_SYSTEM_KEY);

    if (configurationFile != null) {

      // Get application path
      this.applicationPath = getApplicationPath();
      CONSTANTS.setProperty(APPLICATION_PATH_VARIABLE,
          this.applicationPath.getAbsolutePath());

      checkExistingDirectoryFile(this.applicationPath,
          "The application path doest not exists.");

      // Get the file with the list of tests to run
      this.selectedTestsFile =
          getFileFromSystemProperty(IT_TEST_LIST_PATH_SYSTEM_KEY);

      // Get the test to execute
      this.selectedTest = System.getProperty(IT_TEST_SYSTEM_KEY);

      // Load configuration file
      this.globalsConf = loadProperties(configurationFile);

      // Load command line properties
      // Command generate all expected directories test
      this.globalsConf
          .setProperty(
              GENERATE_ALL_EXPECTED_DATA_CONF_KEY,
              getBooleanFromSystemProperty(
                  IT_GENERATE_ALL_EXPECTED_DATA_SYSTEM_KEY).toString());

      // Command generate new expected directories test
      this.globalsConf
          .setProperty(
              GENERATE_NEW_EXPECTED_DATA_CONF_KEY,
              getBooleanFromSystemProperty(
                  IT_GENERATE_NEW_EXPECTED_DATA_SYSTEM_KEY).toString());

      // Set test data source directory
      this.testsDataDirectory =
          new File(this.globalsConf.getProperty(TESTS_DIRECTORY_CONF_KEY));

      this.testsDirectoryFoundToExecute = collectTestsDirectoryToExecute();

      // Init it suite with all potential tests found in test data direction
      ITSuite.getInstance(this.testsDirectoryFoundToExecute, this.globalsConf,
          this.applicationPath);

      // // Retrieve application version test
      // this.versionApplication =
      // retrieveVersionApplication(
      // this.globalsConf
      // .getProperty(ITFactory.COMMAND_TO_GET_APPLICATION_VERSION_CONF_KEY),
      // this.applicationPath);
      //
      //
      // // Set test data output directory
      // this.outputTestsDirectory =
      // new File(
      // this.globalsConf.getProperty(OUTPUT_ANALYSIS_DIRECTORY_CONF_KEY),
      // this.versionApplication + "_" + DATE_FORMATTER.toString());
      //
      // // Set output tests directory path to call by Testng instance in Action
      // // class
      // // TODO: May be a Java system property will be better
      // // outputTestsDirectoryPath =
      // this.outputTestsDirectory.getAbsolutePath();

    } else {
      // Case no testng must be create when compile project with maven
      // this.versionApplication = null;
      this.applicationPath = null;
      this.testsDataDirectory = null;
      // this.outputTestsDirectory = null;
      // this.loggerPath = null;
      this.selectedTestsFile = null;
      this.selectedTest = null;
      this.globalsConf = null;
      this.testsDirectoryFoundToExecute = null;

    }
  }
}
