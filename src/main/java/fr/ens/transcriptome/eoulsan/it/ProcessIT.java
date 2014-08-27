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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.Files.newReader;
import static com.google.common.io.Files.newWriter;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingDirectoryFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.createSymbolicLink;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.recursiveDelete;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.compress.utils.Charsets;
import org.testng.annotations.Test;

import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanITRuntimeException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;

/**
 * The class manage an integrated test for realize regression test on the
 * application used to generate data. It generate expected data directory or
 * data to test directory, in this case the comparison with expected data was
 * launch.
 * @author Sandrine Perrin
 * @since 1.3
 */
public class ProcessIT {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public static final Splitter CMD_LINE_SPLITTER = Splitter.on(' ')
      .trimResults().omitEmptyStrings();
  public static final String SEPARATOR = " ";
  private static final String TEST_SOURCE_LINK_NAME = "test-source";
  private static final String STDERR_FILENAME = "STDERR";
  private static final String STDOUT_FILENAME = "STDOUT";

  private static final String APPLICATION_PATH_VARIABLE = "${application.path}";

  /** Variables */
  private final Properties testConf;
  private final String testName;
  private final String description;
  private final File applicationPath;
  private final File inputTestDirectory;
  private final File outputTestDirectory;
  private final File expectedTestDirectory;

  private final String inputFilesPattern;
  private final String outputFilesPattern;

  private final boolean generateExpectedData;
  private final boolean generateAllTests;
  private final boolean generateNewTests;
  // Case the expected data was generate manually (not with testing application)
  private final boolean manualGenerationExpectedData;

  private final StringBuilder reportText = new StringBuilder();

  /**
   * This internal class allow to save Process outputs
   * @author Laurent Jourdren
   */
  private static final class CopyProcessOutput extends Thread {

    private final Path path;
    private final InputStream in;
    private final String desc;

    @Override
    public void run() {
      // TODO Auto-generated method stub

      try {
        Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        LOGGER.warning("Error while copying " + desc + ": " + e.getMessage());
      }

    }

    CopyProcessOutput(final InputStream in, final File file, final String desc) {

      checkNotNull(in, "in argument cannot be null");
      checkNotNull(file, "file argument cannot be null");
      checkNotNull(desc, "desc argument cannot be null");

      this.in = in;
      this.path = file.toPath();
      this.desc = desc;
    }

  }

  /**
   * Launch test execution, first generate data directory corresponding to the
   * arguments: expected data or data to test.If it is data to test then launch
   * comparison
   * @throws Exception if an error occurs while execute script or comparison
   */
  @Test
  public final void launchTest() throws Exception {

    // Init logger
    final Stopwatch timer = Stopwatch.createStarted();
    LOGGER.info("start test " + this.testName);

    // Compile the result comparison from all tests
    boolean status = true;

    ResultIT regressionResultIT = null;
    ResultIT.OutputExecution outputComparison = null;
    String msgException = null;

    try {
      // Check data to generate
      if (!isDataNeededToBeGenerated())
        // Nothing to do
        return;

      // Build output directory with source files
      buildOutputDirectory();

      // Launch scripts
      launchScriptsTest();

      // Treat result application directory
      regressionResultIT =
          new ResultIT(this.outputTestDirectory, this.inputFilesPattern,
              this.outputFilesPattern, TEST_SOURCE_LINK_NAME
                  + " " + ITFactory.TEST_CONFIGURATION_FILENAME);

      if (this.generateExpectedData) {
        // Build expected directory if necessary
        createExpectedDirectory();

        // Copy files corresponding to pattern in expected data directory
        regressionResultIT.copyFiles(this.expectedTestDirectory);

        this.reportText.append("\nSUCCESS: copy files to "
            + expectedTestDirectory.getAbsolutePath());

      } else {

        // Case comparison between expected and output test directory
        outputComparison =
            regressionResultIT.compareTo(new ResultIT(
                this.expectedTestDirectory.getParentFile(),
                this.inputFilesPattern, this.outputFilesPattern,
                TEST_SOURCE_LINK_NAME
                    + " " + ITFactory.TEST_CONFIGURATION_FILENAME));

        // Comparison assessment
        status = outputComparison.isResult();
      }

    } catch (EoulsanITRuntimeException e) {
      msgException =
          "Fail comparison test "
              + testName + ", cause: " + e.getMessage() + "\n";
      LOGGER.warning(msgException);
      throw new Exception(msgException);

    } catch (Exception e) {
      msgException =
          "Fail test "
              + testName + ", cause: " + e.getMessage() + "\n\t"
              + e.getClass().getName() + "\n";
      LOGGER.warning(msgException);
      throw new Exception(msgException);

    } finally {

      if (outputComparison != null) {
        // Append in report
        this.reportText.append("\n" + outputComparison.getReport());
      }

      // If throws an exception
      if (msgException != null) {
        status = false;
        this.reportText.append("\n" + msgException);
      }

      final String reportFilename = (status ? "SUCCESS" : "FAIL");

      // Create report file
      createReportFile(reportFilename);

      // End test
      timer.stop();
      LOGGER.info(reportFilename
          + " for "
          + testName
          + ((this.generateExpectedData)
              ? ": generate expected data" : ": launch test and comparison")
          + " in " + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));
    }
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

    // Execute command
    try {

      // Replace application path variable in command line
      final String cmd =
          commandLine.replace(APPLICATION_PATH_VARIABLE,
              applicationPath.getAbsolutePath());

      // Execute command
      final String output = ProcessUtils.execToString(cmd);

      if (output != null && output.trim().length() > 0)
        // Retrieve version
        version = output.trim();

    } catch (IOException e) {
    }

    return version;
  }

  /**
   * Check the expected data or data to test must be generated
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
      if (file.isFile()) {
        createSymbolicLink(file, this.outputTestDirectory);
      }
    }

    // Create a symbolic link to the input directory
    createSymbolicLink(this.inputTestDirectory, new File(
        this.outputTestDirectory, TEST_SOURCE_LINK_NAME));
  }

  //
  // Scripting methods
  //
  /**
   * Launch all scripts defined for the test.
   * @throws EoulsanException if an error occurs while execute script
   * @throws IOException if the output directory is missing
   */
  private void launchScriptsTest() throws EoulsanException, IOException {

    checkExistingDirectoryFile(this.outputTestDirectory,
        "output test directory");

    // Define stdout and stderr file
    final File stdoutFile = new File(this.outputTestDirectory, STDOUT_FILENAME);
    final File stderrFile = new File(this.outputTestDirectory, STDERR_FILENAME);

    // Generated test directory
    // Optional script, pre-treatment before launch application
    executeScript(ITFactory.PRE_TEST_SCRIPT_CONF_KEY);

    // Execute application
    if (this.generateExpectedData && this.manualGenerationExpectedData)
      // Case generate expected data manually only it doesn't exists
      executeScript(ITFactory.COMMAND_TO_GENERATE_MANUALLY_CONF_KEY,
          stdoutFile, stderrFile);
    else
      // Case execute testing application
      executeScript(ITFactory.COMMAND_TO_LAUNCH_APPLICATION_CONF_KEY,
          stdoutFile, stderrFile);

    // Optional script, post-treatment after execution application and before
    // comparison between directories
    executeScript(ITFactory.POST_TEST_SCRIPT_CONF_KEY);
  }

  /**
   * Execute a script from a command line retrieved from the test configuration
   * @param scriptConfKey key for configuration to get command line
   * @throws EoulsanException if an error occurs while execute script
   */
  private void executeScript(final String scriptConfKey)
      throws EoulsanException {

    executeScript(scriptConfKey, null, null);
  }

  /**
   * Execute a script from a command line retrieved from the test configuration.
   * @param scriptConfKey key for configuration to get command line
   * @param stdoutFile file where copy the standard output of the script
   * @param stderrFile file where copy the standard output of the script
   * @throws EoulsanException if an error occurs while execute script
   */
  private void executeScript(final String scriptConfKey, final File stdoutFile,
      final File stderrFile) throws EoulsanException {

    if (this.testConf.getProperty(scriptConfKey) == null)
      return;

    // Get command line from the configuration
    final String cmdLine = this.testConf.getProperty(scriptConfKey);

    // Replace application path variable in command line
    final String cmd =
        cmdLine.replace(APPLICATION_PATH_VARIABLE,
            this.applicationPath.getAbsolutePath()).trim();

    if (cmd.isEmpty())
      return;

    // Execute script
    this.reportText.append("\nexecute script with command line: " + cmd);

    executeCommandLine(cmd, this.outputTestDirectory, "execution application",
        stdoutFile, stderrFile);
  }

  /**
   * Execute command line to run process integrated test
   * @param cmdLine command line shell
   * @param directory source for command line
   * @param msg message if an error occurs during execution or if the process
   *          fail
   * @param stdoutFile file where copy the standard output of the process
   * @param stderrFile file where copy the standard output of the process
   * @throws EoulsanException if an error occurs during execution or if the
   *           process fail
   */
  private void executeCommandLine(final String cmdLine, final File directory,
      final String msg, final File stdoutFile, final File stderrFile)
      throws EoulsanException {

    try {

      final Process p = Runtime.getRuntime().exec(cmdLine, null, directory);

      // Save stdout
      if (stdoutFile != null) {
        new CopyProcessOutput(p.getInputStream(), stdoutFile, "stdout").start();
      }

      // Save stderr
      if (stdoutFile != null) {
        new CopyProcessOutput(p.getErrorStream(), stderrFile, "stderr").start();
      }

      // Wait the end of the process
      final int exitValue = p.waitFor();

      if (exitValue != 0) {
        throw new EoulsanException("Bad exit value for script "
            + msg + " (command line: " + cmdLine + "): " + exitValue);
      }

    } catch (IOException | InterruptedException e) {
      throw new EoulsanException("Error during execution script for script "
          + msg + " (command line: " + cmdLine + "): " + e.getMessage());
    }
  }

  //
  // Privates methods
  //

  /**
   * Create the expected data test directory
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
          + ": no expected directoryfound in "
          + inputTestDirectory.getAbsolutePath());

    // Return expected data directory
    return expectedDirectories[0];

  }

  /**
   * Create report of the test execution
   * @param status result of test execution, use like filename
   */
  private void createReportFile(final String status) {
    File directory = this.outputTestDirectory;

    final File reportFile = new File(directory, status);
    Writer fw;
    try {
      fw =
          newWriter(reportFile, Charset.forName(Globals.DEFAULT_FILE_ENCODING));

      fw.write(this.reportText.toString());
      fw.write("\n");

      fw.flush();
      fw.close();

    } catch (Exception e) {
    }

    if (this.generateExpectedData)
      try {
        Files.copy(reportFile.toPath(), new File(this.expectedTestDirectory,
            status).toPath(), StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
      }
  }

  /**
   * Retrieve properties for the test, compile specific configuration with
   * global
   * @param globalsConf global configuration for tests
   * @param testConfFile file with the test configuration
   * @return Properties content of configuration file
   * @throws IOException if an error occurs while reading the file.
   */
  private Properties loadConfigurationFile(final Properties globalsConf,
      final File testConfFile) throws IOException {

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
      String value = line.substring(pos + 1).trim();

      // Save parameter with value
      if (value.length() > 0) {

        // Key pattern : add value for test to values from
        // configuration general
        if (key.toLowerCase().endsWith("patterns") && props.containsKey(key)) {
          // Concatenate values
          value = props.getProperty(key) + SEPARATOR + value;
        }

        props.put(key, value);
      }
    }
    br.close();

    return props;

  }

  @Override
  public String toString() {

    return this.description
        + "\n(output files pattern defined " + this.outputFilesPattern + ")";
  }

  //
  // Constructor
  //

  /**
   * Public constructor
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
  public ProcessIT(final Properties globalsConf, final File applicationPath,
      final File testConfFile, final File testsDirectory, final String testName)
      throws IOException, EoulsanException {

    this.testConf = loadConfigurationFile(globalsConf, testConfFile);

    this.applicationPath = applicationPath;
    this.testName = testName;

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

    this.inputFilesPattern =
        this.testConf.getProperty(ITFactory.INPUT_FILES_PATTERNS_CONF_KEY);
    this.outputFilesPattern =
        this.testConf.getProperty(ITFactory.OUTPUT_FILES_PATTERNS_CONF_KEY);

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
              + "\n action type: "
              + actionType.toUpperCase(Globals.DEFAULT_LOCALE);
    } else {
      this.description = this.testName + "\naction type: " + actionType;
    }
  }
}
