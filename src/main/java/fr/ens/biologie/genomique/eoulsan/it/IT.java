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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */
package fr.ens.biologie.genomique.eoulsan.it;

import static com.google.common.io.Files.newReader;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.it.ITFactory.COMMAND_TO_GENERATE_MANUALLY_CONF_KEY;
import static fr.ens.biologie.genomique.eoulsan.it.ITFactory.COMMAND_TO_LAUNCH_APPLICATION_CONF_KEY;
import static fr.ens.biologie.genomique.eoulsan.it.ITFactory.POSTTREATMENT_GLOBAL_SCRIPT_KEY;
import static fr.ens.biologie.genomique.eoulsan.it.ITFactory.POST_TEST_SCRIPT_CONF_KEY;
import static fr.ens.biologie.genomique.eoulsan.it.ITFactory.PRETREATMENT_GLOBAL_SCRIPT_KEY;
import static fr.ens.biologie.genomique.eoulsan.it.ITFactory.PRE_TEST_SCRIPT_CONF_KEY;
import static fr.ens.biologie.genomique.eoulsan.it.ITFactory.RUNTIME_IT_MAXIMUM_DEFAULT;
import static fr.ens.biologie.genomique.eoulsan.it.ITFactory.SUCCESS_IT_DELETE_FILE_CONF_KEY;
import static fr.ens.biologie.genomique.eoulsan.it.ITFactory.evaluateExpressions;
import static fr.ens.biologie.genomique.eoulsan.it.ITSuite.createRelativeOrAbsoluteSymbolicLink;
import static fr.ens.biologie.genomique.eoulsan.util.FileUtils.checkExistingDirectoryFile;
import static fr.ens.biologie.genomique.eoulsan.util.FileUtils.checkExistingFile;
import static fr.ens.biologie.genomique.eoulsan.util.FileUtils.recursiveDelete;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;

/**
 * The class manage an integrated test for realize regression test on the
 * application used to generate data. It generate expected data directory or
 * data to test directory, in this case the comparison with expected data was
 * launch.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class IT {

  public static final Splitter CMD_LINE_SPLITTER =
      Splitter.on(' ').trimResults().omitEmptyStrings();
  public static final String SEPARATOR = " ";

  /** Prefix for set environment variable in test configuration file. */
  static final String PREFIX_ENV_VAR = "env.var.";

  private static final List<String> PROPERTIES_TO_COMPILE = loadList();

  private static final String TEST_SOURCE_LINK_NAME = "test-source";
  private static final String ENV_FILENAME = "ENV";

  /** Variables. */
  private final Properties testConf;
  private final String testName;
  private final String description;
  private final File applicationPath;
  private final File testDataDirectory;
  private final File outputTestDirectory;
  private final File expectedTestDirectory;

  /** Patterns. */
  private final String fileToComparePatterns;
  private final String excludeToComparePatterns;
  private final String fileToRemovePatterns;
  /** Patterns to check file and compare size. */
  private final String checkExistenceFilePatterns;
  /** Patterns to check file not exist in test directory. */
  private final String checkAbsenceFilePatterns;

  private final boolean generateExpectedDirectoryTestData;
  private final boolean generateAllExpectedDirectoryTest;
  private final boolean generateNewExpectedDirectoryTests;
  // Case the expected data was generate manually (not with testing application)
  private final boolean manualGenerationExpectedData;

  // Instance
  private final ITResult itResult;
  private final List<String> environmentVariables;
  private final ITSuite itSuite;
  private final String checkLengthFilePatterns;

  // Compile the result comparison from all tests
  private ITOutput itOutput = null;
  private boolean isRemoveFileRequired;

  /**
   * Launch test execution, first generate data directory corresponding to the
   * arguments: expected data or data to test. If it is data to test then launch
   * comparison.
   * @throws Exception if an error occurs while execute script or comparison
   */
  @Test
  public final void launchTest() throws Exception {

    // Notify the suite of the beginning of the current test
    this.itSuite.notifyStartTest();

    // Init logger
    final Stopwatch timer = Stopwatch.createStarted();
    getLogger().info("Start test " + this.testName);
    getLogger()
        .info("Test directory " + this.testDataDirectory.getAbsolutePath());
    getLogger()
        .info("Output directory " + this.outputTestDirectory.getAbsolutePath());

    try {
      // Check data to generate
      if (!isDataNeededToBeGenerated()) {
        this.itResult.asNothingToDo();
        // Nothing to do
        return;
      }

      // Build output directory with source files
      buildOutputDirectory();

      // Launch scripts
      launchScriptsTest(this.itResult);

      // Treat result application directory
      this.itOutput = new ITOutput(this.outputTestDirectory,
          this.fileToComparePatterns, this.excludeToComparePatterns,
          this.checkLengthFilePatterns, this.checkExistenceFilePatterns,
          this.checkAbsenceFilePatterns, this.fileToRemovePatterns);

      if (this.generateExpectedDirectoryTestData) {
        this.itResult.asGeneratedData();

        // Build expected directory if necessary
        createExpectedDirectory();

        // Copy files corresponding to pattern in expected data directory
        this.itOutput.copyFiles(this.expectedTestDirectory);

      } else {

        // Case comparison between expected and output test directory
        final Set<ITOutputComparisonResult> results =
            this.itOutput.compareTo(new ITOutput(this.expectedTestDirectory,
                this.fileToComparePatterns, this.excludeToComparePatterns,
                this.checkLengthFilePatterns, this.checkExistenceFilePatterns,
                this.checkAbsenceFilePatterns, this.fileToRemovePatterns));

        this.itResult.addComparisonsResults(results);

        // Check if at least on comparison fail, must throw an exception
        if (!this.itResult.isSuccess()) {
          throw this.itResult.getException();
        }

      }

    } catch (final Throwable e) {

      this.itResult.setException(e);
      throw new Exception(this.itResult.createReportTestngMessage());

    } finally {

      if (this.itOutput != null) {
        // IT succeeded
        this.itOutput.deleteFileMatchingOnPattern(this.itResult,
            this.isRemoveFileRequired);
      }

      timer.stop();

      getLogger().info("End of test " + this.testName);

      // Set success on generate data in expected directory
      this.itResult.createReportFile(timer.elapsed(TimeUnit.MILLISECONDS));

      // Notify the suite of the end of the current test
      this.itSuite.notifyEndTest(this.itResult);

    }
  }

  /**
   * Load list with patterns which values are compiled between global
   * configuration file and test configuration file.
   * @return the list
   */
  private static List<String> loadList() {

    final List<String> l = new ArrayList<>();

    l.add(ITFactory.EXCLUDE_TO_COMPARE_PATTERNS_CONF_KEY);
    l.add(ITFactory.FILE_TO_COMPARE_PATTERNS_CONF_KEY);
    l.add(ITFactory.FILE_TO_REMOVE_CONF_KEY);
    l.add(ITFactory.CHECK_ABSENCE_FILE_PATTERNS_CONF_KEY);
    l.add(ITFactory.CHECK_EXISTENCE_FILE_PATTERNS_CONF_KEY);
    l.add(ITFactory.CHECK_LENGTH_FILE_PATTERNS_CONF_KEY);

    return Collections.unmodifiableList(l);
  }

  /**
   * Checks if the specific key of property is included in properties to compile
   * value between configuration file.
   * @param keyToFind the key to find.
   * @return true, if is key in compile properties otherwise false.
   */
  private boolean isKeyInCompileProperties(final String keyToFind) {

    for (String key : PROPERTIES_TO_COMPILE) {
      if (key.equals(keyToFind)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Launch all scripts defined for the test.
   * @throws Throwable if an error occurs while execute script or output
   *           directory is missing
   */
  private void launchScriptsTest(final ITResult itResult) throws Throwable {

    checkExistingDirectoryFile(this.outputTestDirectory,
        "output test directory");

    // Save environment variable process in file
    saveEnvironmentVariable();

    final ITCommandExecutor cmdExecutor =
        new ITCommandExecutor(this.testConf, this.outputTestDirectory,
            this.environmentVariables, getDurationMaxInMinutes());

    final boolean isApplicationCmdLine = true;

    // Generated test directory
    // Optional run pre-treatment global script, before specific of the test
    executeCommand(cmdExecutor, itResult, PRETREATMENT_GLOBAL_SCRIPT_KEY,
        "PRE_SCRIPT_GLOBAL", "global prescript");

    // Optional script, pre-treatment before launch application
    executeCommand(cmdExecutor, itResult, PRE_TEST_SCRIPT_CONF_KEY,
        "PRE_SCRIPT", "test prescript");

    // Execute application
    if (this.generateExpectedDirectoryTestData
        && this.manualGenerationExpectedData) {

      // Case generate expected data manually only it doesn't exists
      executeCommand(cmdExecutor, itResult,
          COMMAND_TO_GENERATE_MANUALLY_CONF_KEY, "",
          "manual script to generate data", isApplicationCmdLine);

    } else {

      // Case execute testing application
      executeCommand(cmdExecutor, itResult,
          COMMAND_TO_LAUNCH_APPLICATION_CONF_KEY, "", "application",
          isApplicationCmdLine);
    }
    // Optional script, post-treatment after execution application and before
    // comparison between directories
    executeCommand(cmdExecutor, itResult, POST_TEST_SCRIPT_CONF_KEY,
        "POST_SCRIPT", "test postscript");

    // Optional run post-treatment global script, after specific of the test
    executeCommand(cmdExecutor, itResult, POSTTREATMENT_GLOBAL_SCRIPT_KEY,
        "POST_SCRIPT_GLOBAL", "global postscript");
  }

  /**
   * Execute command to run command line.
   * @param cmdExecutor ITCommandExecutor object
   * @param itResult ItResult object
   * @param keyConf key configuration to retrieve command line
   * @param suffixFilename suffix filename for output standard and error file on
   *          execution process
   * @param desc description on command line
   * @throws Throwable if an error occurs during execution process
   */
  private void executeCommand(final ITCommandExecutor cmdExecutor,
      final ITResult itResult, final String keyConf,
      final String suffixFilename, final String desc) throws Throwable {

    executeCommand(cmdExecutor, itResult, keyConf, suffixFilename, desc, false);
  }

  /**
   * Execute command to run command line.
   * @param cmdExecutor ITCommandExecutor object
   * @param itResult ItResult object
   * @param keyConf key configuration to retrieve command line
   * @param suffixFilename suffix filename for output standard and error file on
   *          execution process
   * @param desc description on command line
   * @param isApplication true if application to run, otherwise false
   *          corresponding to annexes script
   * @throws Throwable if an error occurs during execution process
   */
  private void executeCommand(final ITCommandExecutor cmdExecutor,
      final ITResult itResult, final String keyConf,
      final String suffixFilename, final String desc,
      final boolean isApplication) throws Throwable {

    // Execute command line and save standard and error output in file
    final ITCommandResult cmdResult = cmdExecutor.executeCommand(keyConf,
        suffixFilename, desc, isApplication);

    if (cmdResult == null) {
      return;
    }

    // Save result of execution command line
    itResult.addCommandResult(cmdResult);

    if (cmdResult.isCaughtException()) {
      throw cmdResult.getException();
    }
  }

  /**
   * Save all environment variables in file.
   */
  private void saveEnvironmentVariable() {
    final File envFile = new File(this.outputTestDirectory, ENV_FILENAME);

    // Write in file
    if (!(this.environmentVariables == null
        || this.environmentVariables.size() == 0)) {
      // Convert to string
      final String envToString =
          Joiner.on("\n").join(this.environmentVariables);

      try {
        com.google.common.io.Files.write(envToString, envFile,
            StandardCharsets.UTF_8);

      } catch (final IOException e) {
        getLogger()
            .warning("Error while writing environment variables in file: "
                + e.getMessage());
      }
    }
  }

  /**
   * Extract all environment variables setting in test configuration file.
   * @return null if not found or an string array in the format name=value
   */
  private List<String> extractEnvironmentVariables() {

    final List<String> envp = new ArrayList<>();

    // Add environment properties
    for (final Map.Entry<String, String> e : System.getenv().entrySet()) {
      envp.add(e.getKey() + "=" + e.getValue());
    }

    // Add setting environment variables from configuration test
    for (final Object o : this.testConf.keySet()) {
      final String keyProperty = (String) o;

      // Add property if key start with prefix setenv.
      if (keyProperty.startsWith(PREFIX_ENV_VAR)) {
        final String keyEnvp = keyProperty.substring(PREFIX_ENV_VAR.length());
        final String valEnvp = this.testConf.getProperty(keyProperty);
        envp.add(keyEnvp + "=" + valEnvp);
      }
    }

    // No variable found, return null
    if (envp.isEmpty()) {
      return null;
    }

    // Convert to array
    return Collections.unmodifiableList(envp);
  }

  /**
   * Check the expected data or data to test must be generated.
   * @return true if data must be generated
   * @throws IOException if an error occurs while creating directory.
   */
  private boolean isDataNeededToBeGenerated() throws IOException {

    if (!this.generateExpectedDirectoryTestData) {
      // Command for generate data to test, in all case it is true
      return true;
    }

    // Command for generate expected data test
    if (this.manualGenerationExpectedData) {
      // non regenerated expected directory if already exists
      return !this.expectedTestDirectory.exists();
    }

    // Regenerate all expected data directory, remove if always exists
    if (this.generateAllExpectedDirectoryTest) {
      return true;
    }

    // Generate only missing expected data directory
    return this.generateNewExpectedDirectoryTests
        && !this.expectedTestDirectory.exists();

  }

  /**
   * Create expected data directory if the test demand generate expected data,
   * if it doesn't exist. In case generate all expected data directory needed,
   * replace by a new.
   * @throws IOException if an error occurs when creating directory.
   */
  private void createExpectedDirectory() throws IOException {

    // Skip if data to test to generate
    if (!this.generateExpectedDirectoryTestData) {
      return;
    }

    // Check already exists
    if ((this.manualGenerationExpectedData
        || this.generateNewExpectedDirectoryTests)
        && this.expectedTestDirectory.exists()) {
      // Nothing to do
      return;
    }

    // Regenerate existing expected data directory
    if (this.generateAllExpectedDirectoryTest
        && this.expectedTestDirectory.exists()) {
      // Remove existing directory
      recursiveDelete(this.expectedTestDirectory);
    }

    // New check existing directory
    if (!this.expectedTestDirectory.exists()) {
      // Create new expected data directory
      if (!this.expectedTestDirectory.mkdir()) {
        throw new IOException(this.testName
            + ": error while create expected data directory: "
            + this.expectedTestDirectory.getAbsolutePath());
      }
    }
  }

  /**
   * Build output directory for test, add symbolic link to source files useful.
   * @throws IOException if an error occurs while create the files.
   */
  private void buildOutputDirectory() throws IOException {

    if (this.outputTestDirectory.exists()) {
      throw new IOException("Test output directory already exists "
          + this.outputTestDirectory.getAbsolutePath());
    }

    // Create analysis directory and temporary directory
    if (!new File(this.outputTestDirectory + "/tmp").mkdirs()) {
      throw new IOException("Cannot create analysis directory "
          + this.outputTestDirectory.getAbsolutePath());
    }

    // Check input test directory
    checkExistingDirectoryFile(this.testDataDirectory, "input test directory");

    // Create a symbolic link to the input directory
    final Path testSourcePath = Files.createSymbolicLink(
        new File(this.outputTestDirectory, TEST_SOURCE_LINK_NAME).toPath(),
        this.testDataDirectory.toPath());

    // Create a symbolic link for each file from input data test directory
    for (final File file : this.testDataDirectory.listFiles()) {
      // TODO validate if it should limited only file, not directory
      if (file.isFile()) {

        final Path target =
            new File(testSourcePath.toFile(), file.getName()).toPath();
        final Path linkPath =
            new File(this.outputTestDirectory, file.getName()).toPath();

        // Create relative symbolic link
        createRelativeOrAbsoluteSymbolicLink(linkPath, target);

      }
    }

  }

  //
  // Privates methods
  //

  /**
   * Create the expected data test directory.
   * @return expected data directory for the test
   * @throws EoulsanException if the existing directory is empty
   * @throws IOException if the source test directory doesn't exist
   */
  private File retrieveExpectedDirectory()
      throws EoulsanException, IOException {

    checkExistingDirectoryFile(this.testDataDirectory,
        "output data parent directory");

    // Find directory start with expected
    final File[] expectedDirectories =
        this.testDataDirectory.listFiles(pathname -> pathname.getName().startsWith("expected"));

    // Execute test, expected must be existing
    if (expectedDirectories.length == 0
        && !this.generateExpectedDirectoryTestData) {
      throw new EoulsanException(this.testName
          + ": no expected directory found to launch test in "
          + this.testDataDirectory.getAbsolutePath());
    }

    // No test directory found
    if (expectedDirectories.length == 0) {

      // Build expected directory name
      if (this.generateExpectedDirectoryTestData) {

        // Retrieve command line from test configuration
        final String cmdToGetApplicationVersion = this.testConf
            .getProperty(ITFactory.COMMAND_TO_GET_APPLICATION_VERSION_CONF_KEY);

        final String versionExpectedApplication =
            this.itSuite.retrieveVersionApplication(cmdToGetApplicationVersion,
                this.applicationPath);

        return new File(this.testDataDirectory,
            "/expected_"
                + (this.manualGenerationExpectedData
                    ? "UNKNOWN" : versionExpectedApplication));
      }
    }

    // One test directory found
    if (expectedDirectories.length > 1) {
      throw new EoulsanException(this.testName
          + ": more one expected directory found in "
          + this.testDataDirectory.getAbsolutePath());
    }

    if (!expectedDirectories[0].isDirectory()) {
      throw new EoulsanException(this.testName
          + ": no expected directory found in "
          + this.testDataDirectory.getAbsolutePath());
    }

    // Return expected data directory
    return expectedDirectories[0];

  }

  /**
   * Group exclude file patterns with default, global configuration and
   * configuration test.
   * @param valueConfigTests patterns from configuration file
   * @return exclude files patterns for tests
   */
  private String buildExcludePatterns(final String valueConfigTests) {

    if (valueConfigTests == null || valueConfigTests.equals("none")) {
      // Syntax **/filename
      return "**/"
          + IT.TEST_SOURCE_LINK_NAME + SEPARATOR + "**/"
          + ITFactory.TEST_CONFIGURATION_FILENAME;
    }

    return IT.TEST_SOURCE_LINK_NAME
        + SEPARATOR + ITFactory.TEST_CONFIGURATION_FILENAME + SEPARATOR
        + valueConfigTests;
  }

  /**
   * Retrieve properties for the test, compile specific configuration with
   * global.
   * @param globalsConf global configuration for tests
   * @return Properties content of configuration file
   * @throws IOException if an error occurs while reading the file.
   * @throws EoulsanException if an error occurs while evaluating value property
   */
  private Properties loadConfigurationFile(final Properties globalsConf)
      throws IOException, EoulsanException {

    final File testConfFile =
        new File(this.testDataDirectory, ITFactory.TEST_CONFIGURATION_FILENAME);

    checkExistingFile(testConfFile, "test configuration file");

    // Add global configuration
    final Properties props = new Properties();
    props.putAll(globalsConf);

    final BufferedReader br = newReader(testConfFile, Globals.DEFAULT_CHARSET);

    String line = null;

    while ((line = br.readLine()) != null) {
      // Skip commentary
      if (line.startsWith("#")) {
        continue;
      }

      final int pos = line.indexOf('=');
      if (pos == -1) {
        continue;
      }

      final String key = line.substring(0, pos).trim();

      // Evaluate value
      String value = evaluateExpressions(line.substring(pos + 1).trim(), true);

      // Key pattern : add value for test to values from
      // configuration general

      if (isKeyInCompileProperties(key) && props.containsKey(key)) {
        // Concatenate values
        value = props.getProperty(key) + SEPARATOR + value;
      }

      // Save parameter with value
      props.put(key, value);
    }
    br.close();

    return props;

  }

  /**
   * Extract pattern.
   * @param propertyKey the property key
   * @return the pattern form configuration
   */
  private String extractPattern(final String propertyKey) {

    final String patterns = this.testConf.getProperty(propertyKey);

    if (patterns == null || patterns.trim().isEmpty()) {
      return "none";
    }

    return patterns.trim();
  }

  //
  // Getter
  //

  /**
   * Gets the property from configuration test.
   * @param key the key
   * @return the property value
   */
  public String getProperty(final String key) {
    return this.testConf.getProperty(key);
  }

  /**
   * Gets the test name.
   * @return the test name
   */
  public String getTestName() {
    return this.testName;
  }

  /**
   * Gets the expected test directory.
   * @return the expected test directory
   */
  public File getExpectedTestDirectory() {
    return this.expectedTestDirectory;
  }

  /**
   * Gets the output test directory.
   * @return the output test directory
   */
  public File getOutputTestDirectory() {
    return this.outputTestDirectory;
  }

  @Override
  public String toString() {
    return this.description
        + ", files from pattern(s) " + this.fileToComparePatterns;
  }

  /**
   * Gets the file to compare patterns.
   * @return the file to compare patterns
   */
  public String getFileToComparePatterns() {
    return this.fileToComparePatterns;
  }

  /**
   * Gets the file to remove patterns.
   * @return the file to remove patterns
   */
  public String getFileToRemovePatterns() {
    return this.fileToRemovePatterns;
  }

  /**
   * Gets the exclude to compare patterns.
   * @return the exclude to compare patterns
   */
  public String getExcludeToComparePatterns() {
    return this.excludeToComparePatterns;
  }

  /**
   * Gets the check existence file patterns.
   * @return the check existence file patterns
   */
  public String getCheckExistenceFilePatterns() {
    return this.checkExistenceFilePatterns;
  }

  /**
   * Gets the check length file patterns.
   * @return the check length file patterns
   */
  public String getCheckLengthFilePatterns() {
    return this.checkLengthFilePatterns;
  }

  /**
   * Gets the count files to check content.
   * @return the count files to check content
   */
  public int getCountFilesToCheckContent() {
    return (this.itOutput == null
        ? 0 : this.itOutput.getCountFilesToCheckContent());
  }

  /**
   * Gets the count files to check length.
   * @return the count files to check length
   */
  public int getCountFilesToCheckLength() {
    return (this.itOutput == null
        ? 0 : this.itOutput.getCountFilesToCheckLength());
  }

  /**
   * Gets the count files to check existence.
   * @return the count files to check existence
   */
  public int getCountFilesToCheckExistence() {
    return (this.itOutput == null
        ? 0 : this.itOutput.getCountFilesToCheckExistence());
  }

  /**
   * Gets the count files to compare.
   * @return the count files to compare
   */
  public int getCountFilesToCompare() {
    return (this.itOutput == null ? 0 : this.itOutput.getCountFilesToCompare());
  }

  /**
   * Gets the count files to remove.
   * @return the count files to remove
   */
  public int getCountFilesToRemove() {
    return (this.itOutput == null ? 0 : this.itOutput.getCountFilesToRemove());
  }

  /**
   * Gets the IT output.
   * @return the IT output
   */
  public ITOutput getITOutput() {
    return this.itOutput;
  }

  public int getDurationMaxInMinutes() {
    final String value = getProperty(ITFactory.RUNTIME_IT_MAXIMUM_KEY);

    try {

      return Integer.parseInt(value);

    } catch (Exception e) {
      getLogger().severe("Duration set in configuration invalid "
          + value + ". Use default value " + RUNTIME_IT_MAXIMUM_DEFAULT);

      return RUNTIME_IT_MAXIMUM_DEFAULT;
    }

  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param itSuite the it suite
   * @param globalsConf global configuration for tests
   * @param applicationPath path to the application to test
   * @param testsDataDirectory file with the test configuration
   * @param outputTestsDirectory output test directory with result execute
   *          application
   * @param testName test name
   * @throws IOException if an error occurs while reading the configuration
   *           file.
   * @throws EoulsanException if an error occurs while search expected directory
   *           of the test.
   */
  public IT(final ITSuite itSuite, final Properties globalsConf,
      final File applicationPath, final File testsDataDirectory,
      final File outputTestsDirectory, final String testName)
      throws IOException, EoulsanException {

    checkExistingDirectoryFile(testsDataDirectory, "tests data directory");
    checkExistingDirectoryFile(outputTestsDirectory, "output tests directory");
    checkExistingDirectoryFile(applicationPath, "application path");

    this.itSuite = itSuite;
    this.applicationPath = applicationPath;
    this.testName = testName;

    // Test data directory contains test configuration file and expected
    // directory
    this.testDataDirectory = new File(testsDataDirectory, this.testName);

    // Output directory on execution integration test
    this.outputTestDirectory = new File(outputTestsDirectory, this.testName);

    // Load properties configuration, added to globals configuration
    this.testConf = loadConfigurationFile(globalsConf);

    // Extract environment variables, after loading configuration
    this.environmentVariables = extractEnvironmentVariables();

    // Init integration tests result
    this.itResult = new ITResult(this);

    // Remove file matching on pattern if IT succeeded
    this.isRemoveFileRequired = Boolean.parseBoolean(
        this.testConf.getProperty(SUCCESS_IT_DELETE_FILE_CONF_KEY));

    // Extract properties on action: generate expected data directory
    this.generateAllExpectedDirectoryTest =
        this.itSuite.isGenerateAllExpectedDirectoryTest();

    this.generateNewExpectedDirectoryTests =
        this.itSuite.isGenerateNewExpectedDirectoryTests();

    this.generateExpectedDirectoryTestData =
        this.generateAllExpectedDirectoryTest
            || this.generateNewExpectedDirectoryTests;

    this.manualGenerationExpectedData = Boolean.parseBoolean(this.testConf
        .getProperty(ITFactory.MANUAL_GENERATION_EXPECTED_DATA_CONF_KEY));

    this.expectedTestDirectory = retrieveExpectedDirectory();

    // Extract all patterns define
    this.fileToComparePatterns =
        extractPattern(ITFactory.FILE_TO_COMPARE_PATTERNS_CONF_KEY);

    // Extract all patterns define
    this.fileToRemovePatterns =
        extractPattern(ITFactory.FILE_TO_REMOVE_CONF_KEY);

    this.excludeToComparePatterns = buildExcludePatterns(
        extractPattern(ITFactory.EXCLUDE_TO_COMPARE_PATTERNS_CONF_KEY));

    this.checkExistenceFilePatterns =
        extractPattern(ITFactory.CHECK_EXISTENCE_FILE_PATTERNS_CONF_KEY);

    this.checkLengthFilePatterns =
        extractPattern(ITFactory.CHECK_LENGTH_FILE_PATTERNS_CONF_KEY);

    this.checkAbsenceFilePatterns = this.testConf
        .getProperty(ITFactory.CHECK_ABSENCE_FILE_PATTERNS_CONF_KEY);

    // Check not define, use test name
    if (this.testConf.containsKey(ITFactory.DESCRIPTION_CONF_KEY)) {
      this.description =
          this.testConf.getProperty(ITFactory.DESCRIPTION_CONF_KEY)
              + ", action: " + this.itSuite.getActionType();
    } else {
      this.description =
          this.testName + ", action: " + this.itSuite.getActionType();
    }
  }

}
