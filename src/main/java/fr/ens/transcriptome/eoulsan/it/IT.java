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
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.it.ITFactory.COMMAND_TO_GENERATE_MANUALLY_CONF_KEY;
import static fr.ens.transcriptome.eoulsan.it.ITFactory.COMMAND_TO_LAUNCH_APPLICATION_CONF_KEY;
import static fr.ens.transcriptome.eoulsan.it.ITFactory.POSTTREATMENT_GLOBAL_SCRIPT_KEY;
import static fr.ens.transcriptome.eoulsan.it.ITFactory.POST_TEST_SCRIPT_CONF_KEY;
import static fr.ens.transcriptome.eoulsan.it.ITFactory.PRETREATMENT_GLOBAL_SCRIPT_KEY;
import static fr.ens.transcriptome.eoulsan.it.ITFactory.PRE_TEST_SCRIPT_CONF_KEY;
import static fr.ens.transcriptome.eoulsan.it.ITFactory.evaluateExpressions;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingDirectoryFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.createSymbolicLink;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.recursiveDelete;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.utils.Charsets;
import org.testng.annotations.Test;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;

/**
 * The class manage an integrated test for realize regression test on the
 * application used to generate data. It generate expected data directory or
 * data to test directory, in this case the comparison with expected data was
 * launch.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class IT {

  public static final Splitter CMD_LINE_SPLITTER = Splitter.on(' ')
      .trimResults().omitEmptyStrings();
  public static final String SEPARATOR = " ";

  /** Prefix for set environment variable in test configuration file. */
  static final String PREFIX_ENV_VAR = "env.var.";

  private static final String TEST_SOURCE_LINK_NAME = "test-source";
  private static final String ENV_FILENAME = "ENV";

  /** Variables. */
  private final Properties testConf;
  private final String testName;
  private final String description;
  private final File applicationPath;
  private final File inputTestDirectory;
  private final File outputTestDirectory;
  private final File expectedTestDirectory;

  /** Patterns. */
  private final String fileToComparePatterns;
  private final String excludeToComparePatterns;
  /** Patterns to check file and compare size. */
  private final String checkExistenceFilePatterns;
  /** Patterns to check file not exist in test directory. */
  private final String checkAbsenceFilePatterns;

  private final boolean generateExpectedData;
  private final boolean generateAllTests;
  private final boolean generateNewTests;
  // Case the expected data was generate manually (not with testing application)
  private final boolean manualGenerationExpectedData;

  // Instance
  private final ITResult itResult;
  private final List<String> environmentVariables;

  /**
   * Launch test execution, first generate data directory corresponding to the
   * arguments: expected data or data to test. If it is data to test then launch
   * comparison.
   * @throws Exception if an error occurs while execute script or comparison
   */
  @Test
  public final void launchTest() throws Exception {

    // Init logger
    final Stopwatch timer = Stopwatch.createStarted();

    getLogger().info("Start test " + this.testName);

    // Get ITSuite object
    final ITSuite itSuite = ITSuite.getInstance();

    // Notify the suite of the beginning of the current test
    itSuite.startTest(this.outputTestDirectory.getParentFile());

    // Compile the result comparison from all tests
    ITOutput itOutput = null;

    try {
      // Check data to generate
      if (!isDataNeededToBeGenerated()) {
        itResult.asNothingToDo();
        // Nothing to do
        return;
      }

      // Build output directory with source files
      buildOutputDirectory();

      // Launch scripts
      launchScriptsTest(itResult);

      // Treat result application directory
      itOutput =
          new ITOutput(this.outputTestDirectory, this.fileToComparePatterns,
              this.excludeToComparePatterns, this.checkExistenceFilePatterns,
              this.checkAbsenceFilePatterns);

      if (this.generateExpectedData) {
        itResult.asGeneratedData();

        // Build expected directory if necessary
        createExpectedDirectory();

        // Copy files corresponding to pattern in expected data directory
        itOutput.copyFiles(this.expectedTestDirectory);

      } else {

        // Case comparison between expected and output test directory
        final Set<ITOutputComparisonResult> results =
            itOutput.compareTo(new ITOutput(this.expectedTestDirectory
                .getParentFile(), this.fileToComparePatterns,
                this.excludeToComparePatterns, this.checkExistenceFilePatterns,
                this.checkAbsenceFilePatterns));

        itResult.addComparisonsResults(results);

        // Check if at least on comparison fail, must throw an exception
        if (!itResult.isSuccess())
          throw itResult.getException();
      }

    } catch (Throwable e) {
      itResult.setException(e);
      throw new Exception(itResult.createReportTestngMessage());

    } finally {

      timer.stop();

      if (!itResult.isNothingToDo()) {

        // Set success on generate data in expected directory
        itResult.createReportFile(timer.elapsed(TimeUnit.MILLISECONDS));
      }

      // Notify the suite of the end of the current test
      itSuite.endTest(this.outputTestDirectory.getParentFile(), this.itResult);
    }
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
            this.environmentVariables);

    final boolean isApplication = true;

    // Generated test directory
    // Optional run pre-treatment global script, before specific of the test
    executeCommand(cmdExecutor, itResult, PRETREATMENT_GLOBAL_SCRIPT_KEY,
        "PRE_SCRIPT_GLOBAL", "prescript global");

    // Optional script, pre-treatment before launch application
    executeCommand(cmdExecutor, itResult, PRE_TEST_SCRIPT_CONF_KEY,
        "PRE_SCRIPT", "prescript for test");

    // Execute application
    if (this.generateExpectedData && this.manualGenerationExpectedData) {
      // Case generate expected data manually only it doesn't exists
      executeCommand(cmdExecutor, itResult,
          COMMAND_TO_GENERATE_MANUALLY_CONF_KEY, "",
          "manual script to generate data", isApplication);
    } else {
      // Case execute testing application
      executeCommand(cmdExecutor, itResult,
          COMMAND_TO_LAUNCH_APPLICATION_CONF_KEY, "", "execution application",
          isApplication);
    }
    // Optional script, post-treatment after execution application and before
    // comparison between directories
    executeCommand(cmdExecutor, itResult, POST_TEST_SCRIPT_CONF_KEY,
        "POST_SCRIPT", "post script for test");
    // Optional run post-treatment global script, after specific of the test
    executeCommand(cmdExecutor, itResult, POSTTREATMENT_GLOBAL_SCRIPT_KEY,
        "POST_SCRIPT_GLOBAL", "post script global");
  }

  /**
   * Execute command to run command line.
   * @param cmdExecutor ITCommandExecutor object
   * @param itResult ItResult object
   * @param keyConf key configuration to retrieve command line
   * @param suffixFilename suffix filename for output standard and error file on
   *          execution processus
   * @param desc description on command line
   * @throws Throwable if an error occurs during execution processus
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
   *          execution processus
   * @param desc description on command line
   * @param isApplication true if application to run, otherwise false
   *          corresponding to annexes script
   * @throws Throwable if an error occurs during execution processus
   */
  private void executeCommand(final ITCommandExecutor cmdExecutor,
      final ITResult itResult, final String keyConf,
      final String suffixFilename, final String desc,
      final boolean isApplication) throws Throwable {

    // Execute command line and save standard and error output in file
    ITCommandResult cmdResult =
        cmdExecutor
            .executeCommand(keyConf, suffixFilename, desc, isApplication);

    if (cmdResult == null)
      return;

    itResult.addCommandResult(cmdResult);

    if (cmdResult.isCatchedException())
      throw cmdResult.getException();
  }

  /**
   * Save all environment variables in file.
   */
  private void saveEnvironmentVariable() {
    final File envFile = new File(this.outputTestDirectory, ENV_FILENAME);

    // Write in file
    if (!(this.environmentVariables == null || this.environmentVariables.size() == 0)) {
      // Convert to string
      String envToString =
          Joiner.on("\n").join(Arrays.asList(this.environmentVariables));

      try {
        com.google.common.io.Files.write(envToString, envFile, Charsets.UTF_8);
      } catch (IOException e) {
        // Nothing to do
      }
    }
  }

  /**
   * Extract all environment variables setting in test configuration file.
   * @return null if not found or an string array in the format name=value
   */
  private List<String> extractEnvironmentVariables() {

    List<String> envp = Lists.newArrayList();

    // Add environment properties
    for (Map.Entry<String, String> e : System.getenv().entrySet())
      envp.add(e.getKey() + "=" + e.getValue());

    // Add setting environment variables from configuration test
    for (Object o : this.testConf.keySet()) {
      String keyProperty = (String) o;

      // Add property if key start with prefix setenv.
      if (keyProperty.startsWith(PREFIX_ENV_VAR)) {
        String keyEnvp = keyProperty.substring(PREFIX_ENV_VAR.length());
        String valEnvp = this.testConf.getProperty(keyProperty);
        envp.add(keyEnvp + "=" + valEnvp);
      }
    }

    // No variable found, return null
    if (envp.isEmpty())
      return null;

    // Convert to array
    return Collections.unmodifiableList(envp);
  }

  /**
   * Execute command line shell to obtain the version name of application to
   * test. If fail, it return UNKOWN.
   * @param commandLine command line shell
   * @param applicationPath application path to test
   * @return version name of application to test
   */
  public static String retrieveVersionApplication(final String commandLine,
      final File applicationPath) {

    String version = "UNKNOWN";

    if (commandLine == null || commandLine.trim().length() == 0) {
      // None command line to retrieve version application set in configuration
      // file
      return version;
    }

    try {
      // Execute command
      final String output = ProcessUtils.execToString(commandLine);

      if (output != null && output.trim().length() > 0)
        // Retrieve version
        version = output.trim();

    } catch (IOException e) {
    }

    return version;
  }

  /**
   * Check the expected data or data to test must be generated.
   * @return true if data must be generated
   * @throws IOException if an error occurs while creating directory.
   */
  private boolean isDataNeededToBeGenerated() throws IOException {

    if (!this.generateExpectedData)
      // Command for generate data to test, in all case it is true
      return true;

    // Command for generate expected data test
    if (this.manualGenerationExpectedData)
      // non regenerated expected directory if already exists
      return !this.expectedTestDirectory.exists();

    // Regenerate all expected data directory, remove if always exists
    if (this.generateAllTests)
      return true;

    // Generate only missing expected data directory
    return this.generateNewTests && !this.expectedTestDirectory.exists();

  }

  /**
   * Create expected data directory if the test demand generate expected data,
   * if it doesn't exist. In case generate all expected data directory needed,
   * replace by a new.
   * @throws IOException if an error occurs when creating directory.
   */
  private void createExpectedDirectory() throws IOException {

    // Skip if data to test to generate
    if (!this.generateExpectedData)
      return;

    // Check already exists
    if ((this.manualGenerationExpectedData || this.generateNewTests)
        && this.expectedTestDirectory.exists())
      // Nothing to do
      return;

    // Regenerate existing expected data directory
    if (this.generateAllTests && this.expectedTestDirectory.exists()) {
      // Remove existing directory
      recursiveDelete(this.expectedTestDirectory);
    }

    // New check existing directory
    if (!this.expectedTestDirectory.exists()) {
      // Create new expected data directory
      if (!this.expectedTestDirectory.mkdir())
        throw new IOException(testName
            + ": error while create expected data directory: "
            + this.expectedTestDirectory.getAbsolutePath());
    }
  }

  /**
   * Build output directory for test, add symbolic link to source files useful.
   * @throws IOException if an error occurs while create the files.
   */
  private void buildOutputDirectory() throws IOException {

    if (this.outputTestDirectory.exists())
      throw new IOException("Test output directory already exists "
          + this.outputTestDirectory.getAbsolutePath());

    // Create analysis directory and temporary directory
    if (!new File(this.outputTestDirectory + "/tmp").mkdirs())
      throw new IOException("Cannot create analysis directory "
          + this.outputTestDirectory.getAbsolutePath());

    // Check input test directory
    checkExistingDirectoryFile(this.inputTestDirectory, "input test directory");

    // Create a symbolic link for each file from input data test directory
    for (File file : this.inputTestDirectory.listFiles()) {
      // TODO validate if it should limited only file, not directory
      if (file.isFile()) {
        createSymbolicLink(file, this.outputTestDirectory);
      }
    }

    // Create a symbolic link to the input directory
    createSymbolicLink(this.inputTestDirectory, new File(
        this.outputTestDirectory, TEST_SOURCE_LINK_NAME));
  }

  //
  // Privates methods
  //

  /**
   * Create the expected data test directory.
   * @param inputTestDirectory source test directory with needed files
   * @return expected data directory for the test
   * @throws EoulsanException if the existing directory is empty
   */
  private File retrieveExpectedDirectory(final File inputTestDirectory)
      throws EoulsanException {

    // Find directory start with expected
    final File[] expectedDirectories =
        inputTestDirectory.listFiles(new FileFilter() {

          @Override
          public boolean accept(File pathname) {
            return pathname.getName().startsWith("expected");
          }
        });

    // Execute test, expected must be existing
    if (expectedDirectories.length == 0 && !this.generateExpectedData)
      throw new EoulsanException(testName
          + ": no expected directory found to launch test in "
          + inputTestDirectory.getAbsolutePath());

    // No test directory found
    if (expectedDirectories.length == 0) {

      // Build expected directory name
      if (this.generateExpectedData) {

        // Retrieve command line from test configuration
        final String value =
            this.testConf
                .getProperty(ITFactory.COMMAND_TO_GET_APPLICATION_VERSION_CONF_KEY);

        final String versionExpectedApplication =
            retrieveVersionApplication(value, this.applicationPath);

        return new File(inputTestDirectory, "/expected_"
            + (this.manualGenerationExpectedData
                ? "UNKNOWN" : versionExpectedApplication));
      }
    }

    // One test directory found
    if (expectedDirectories.length > 1)
      throw new EoulsanException(testName
          + ": more one expected directory found in "
          + inputTestDirectory.getAbsolutePath());

    if (!expectedDirectories[0].isDirectory())
      throw new EoulsanException(testName
          + ": no expected directory found in "
          + inputTestDirectory.getAbsolutePath());

    // Return expected data directory
    return expectedDirectories[0];

  }

  /**
   * Group exclude file patterns with default, global configuration and
   * configuration test.
   * @param valueConfigTests
   * @return exclude files patterns for tests
   */
  private String buildExcludePatterns(final String valueConfigTests) {
    if (valueConfigTests == null || valueConfigTests.trim().length() == 0)
      // Syntax **/filename
      return "**/"
          + IT.TEST_SOURCE_LINK_NAME + SEPARATOR + "**/"
          + ITFactory.TEST_CONFIGURATION_FILENAME;

    return IT.TEST_SOURCE_LINK_NAME
        + SEPARATOR + ITFactory.TEST_CONFIGURATION_FILENAME + SEPARATOR
        + valueConfigTests;
  }

  /**
   * Retrieve properties for the test, compile specific configuration with
   * global.
   * @param globalsConf global configuration for tests
   * @param testConfFile file with the test configuration
   * @return Properties content of configuration file
   * @throws IOException if an error occurs while reading the file.
   * @throws EoulsanException if an error occurs while evaluating value property
   */
  private Properties loadConfigurationFile(final Properties globalsConf,
      final File testConfFile) throws IOException, EoulsanException {

    checkExistingFile(testConfFile, "configuration file");

    // Add global configuration
    final Properties props = new Properties();
    props.putAll(globalsConf);

    final BufferedReader br =
        newReader(testConfFile,
            Charsets.toCharset(Globals.DEFAULT_FILE_ENCODING));

    String line = null;

    while ((line = br.readLine()) != null) {
      // Skip commentary
      if (line.startsWith("#"))
        continue;

      final int pos = line.indexOf('=');
      if (pos == -1)
        continue;

      final String key = line.substring(0, pos).trim();

      // Evaluate value
      String value = evaluateExpressions(line.substring(pos + 1).trim(), true);

      // Key pattern : add value for test to values from
      // configuration general
      if (key.toLowerCase().endsWith("patterns") && props.containsKey(key)) {
        // Concatenate values
        value = props.getProperty(key) + SEPARATOR + value;
      }

      // Save parameter with value
      props.put(key, value);
    }
    br.close();

    return props;

  }

  //
  // Getter
  //

  public String getTestName() {
    return this.testName;
  }

  public File getExpectedTestDirectory() {
    return this.expectedTestDirectory;
  }

  public File getOutputTestDirectory() {
    return this.outputTestDirectory;
  }

  @Override
  public String toString() {

    return this.description
        + ", files from pattern(s) " + this.fileToComparePatterns;
  }

  public String getFileToComparePatterns() {
    return (fileToComparePatterns == null || fileToComparePatterns.isEmpty()
        ? "none" : this.fileToComparePatterns);
  }

  public String getExcludeToComparePatterns() {
    return (excludeToComparePatterns == null
        || excludeToComparePatterns.isEmpty()
        ? "none" : excludeToComparePatterns);
  }

  public String getCheckExistenceFilePatterns() {
    return (checkExistenceFilePatterns == null
        || checkExistenceFilePatterns.isEmpty()
        ? "none" : checkExistenceFilePatterns);
  }

  //
  // Constructor
  //
  /**
   * Public constructor.
   * @param globalsConf global configuration for tests
   * @param applicationPath path to the application to test
   * @param testConfFile file with the test configuration
   * @param testsDirectory output test directory with result execute application
   * @param testName test name
   * @throws IOException if an error occurs while reading the configuration
   *           file.
   * @throws EoulsanException if an error occurs while search expected directory
   *           of the test.
   */
  public IT(final Properties globalsConf, final File applicationPath,
      final File testConfFile, final File testsDirectory, final String testName)
      throws IOException, EoulsanException {

    this.testConf = loadConfigurationFile(globalsConf, testConfFile);

    this.applicationPath = applicationPath;
    this.testName = testName;

    this.itResult = new ITResult(this);
    this.environmentVariables = extractEnvironmentVariables();

    this.inputTestDirectory = testConfFile.getParentFile();

    this.generateAllTests =
        Boolean.parseBoolean(globalsConf
            .getProperty(ITFactory.GENERATE_ALL_EXPECTED_DATA_CONF_KEY));

    this.generateNewTests =
        Boolean.parseBoolean(globalsConf
            .getProperty(ITFactory.GENERATE_NEW_EXPECTED_DATA_CONF_KEY));

    this.manualGenerationExpectedData =
        Boolean.parseBoolean(this.testConf
            .getProperty(ITFactory.MANUAL_GENERATION_EXPECTED_DATA_CONF_KEY));

    this.generateExpectedData = generateAllTests || generateNewTests;
    this.expectedTestDirectory =
        retrieveExpectedDirectory(this.inputTestDirectory);

    this.outputTestDirectory = new File(testsDirectory, this.testName);

    this.fileToComparePatterns =
        this.testConf.getProperty(ITFactory.FILE_TO_COMPARE_PATTERNS_CONF_KEY);

    // Set exclude pattern for this test
    this.excludeToComparePatterns =
        buildExcludePatterns(this.testConf
            .getProperty(ITFactory.EXCLUDE_TO_COMPARE_PATTERNS_CONF_KEY));

    this.checkExistenceFilePatterns =
        this.testConf
            .getProperty(ITFactory.CHECK_EXISTENCE_FILE_PATTERNS_CONF_KEY);

    this.checkAbsenceFilePatterns =
        this.testConf
            .getProperty(ITFactory.CHECK_ABSENCE_FILE_PATTERNS_CONF_KEY);

    // Set action required
    final String actionType =
        (this.generateExpectedData
            ? (this.generateAllTests
                ? "regenerate all data expected "
                : "generate new data expected ") : "launch test");

    // Check not define, use test name
    if (this.testConf.contains(ITFactory.DESCRIPTION_CONF_KEY)) {
      this.description =
          this.testConf.getProperty(ITFactory.DESCRIPTION_CONF_KEY)
              + ", action: " + actionType.toUpperCase(Globals.DEFAULT_LOCALE);
    } else {
      this.description = this.testName + ", action type: " + actionType;
    }
  }

}
