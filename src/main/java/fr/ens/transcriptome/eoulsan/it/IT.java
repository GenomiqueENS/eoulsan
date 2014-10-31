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
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.it.ITFactory.getItSuite;
import static fr.ens.transcriptome.eoulsan.it.ITSuite.isDebugEnable;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.utils.Charsets;
import org.testng.annotations.Test;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.it.ITResult.OutputExecution;
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
  private static final String TEST_SOURCE_LINK_NAME = "test-source";
  private static final String STDERR_FILENAME = "STDERR";
  private static final String STDOUT_FILENAME = "STDOUT";
  private static final String CMDLINE_FILENAME = "CMDLINE";
  private static final String ENV_FILENAME = "ENV";

  private static final String APPLICATION_PATH_VARIABLE = "${application.path}";

  /** Prefix for set environment variable in test configuration file */
  private static final String PREFIX_ENV_VAR = "env.var.";

  /** Variables */
  private final Properties testConf;
  private final String testName;
  private final String description;
  private final File applicationPath;
  private final File inputTestDirectory;
  private final File outputTestDirectory;
  private final File expectedTestDirectory;

  /** Patterns */
  private final String fileToComparePatterns;
  private final String excludeToComparePatterns;
  /** Patterns to check file and compare size */
  private final String checkExistenceFilePatterns;
  /** Patterns to check file not exist in test directory */
  private final String checkAbsenceFilePatterns;

  private final boolean generateExpectedData;
  private final boolean generateAllTests;
  private final boolean generateNewTests;
  // Case the expected data was generate manually (not with testing application)
  private final boolean manualGenerationExpectedData;

  // Compile current environment variable and set in configuration file with
  // prefix PREFIX_ENV_VAR
  private final String[] environmentVariables;
  // private final StringBuilder reportText = new StringBuilder();

  // Instance
  private final ReportTest reportTest;

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

      try {
        Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        getLogger().warning(
            "Error while copying " + desc + ": " + e.getMessage());
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
   * This internal class instance exception from process execution.
   * @author Sandrine Perrin
   */
  private static final class ProcessScriptException extends Exception {
    private static final long serialVersionUID = -749903788412172296L;

    private Exception exception;

    ProcessScriptException(final Exception e) {
      this(e.getMessage());
      this.exception = e;
    }

    ProcessScriptException() {
      super();
    }

    public ProcessScriptException(final String message) {
      super(message);
    }
  }

  /**
   * This internal class allow to save Process outputs
   * @author Laurent Jourdren
   */
  private static final class ResultScriptProcess {

    final String cmdLine;
    final File directory;
    final int exitValue;
    final ProcessScriptException exception;
    final File stdoutFile;
    final File stderrFile;
    final String message;
    final long duration;

    public boolean isCatchedException() {
      return exception != null;
    }

    public String getMessage() {
      final StringBuilder msg = new StringBuilder();

      msg.append("\nExecute script for " + message);
      msg.append("\n\tcommand line: " + cmdLine);
      msg.append("\n\tin directory: " + directory.getAbsolutePath());
      msg.append("\n\texit value: " + exitValue);
      msg.append("\n\tduration: " + toTimeHumanReadable(duration));
      msg.append("\n");

      if (isCatchedException())
        msg.append(createExceptionText(exception, true));

      return msg.toString();
    }

    public ProcessScriptException getException() {
      return this.exception;
    }

    //
    // Constructor
    //
    ResultScriptProcess(final String cmdLine, final File directory,
        final int exitValue, final ProcessScriptException exception,
        final File stdoutFile, final File stderrFile, final String message,
        final long duration) {

      this.cmdLine = cmdLine;
      this.directory = directory;
      this.exitValue = exitValue;
      this.exception = exception;
      this.stdoutFile = stdoutFile;
      this.stderrFile = stderrFile;
      this.message = message;
      this.duration = duration;
    }

  }

  /**
   * This internal class allow to build report execution test
   * @author Sandrine Perrin
   */
  private final class ReportTest {

    private OutputExecution ouputComparison;
    private Throwable exception;
    private List<ResultScriptProcess> resultScriptProcess = Lists
        .newArrayList();

    private boolean generatedData = false;
    // Set result on execution test
    private boolean success = false;

    // True if expected directory already exist
    private boolean nothingToDo = false;

    /**
     * Create report of the test execution
     * @param status result of test execution, use like filename
     */
    private void createReportFile() {

      final String filename = isSuccess() ? "SUCCESS" : "FAIL";

      final File reportFile = new File(outputTestDirectory, filename);
      Writer fw;
      try {
        fw =
            newWriter(reportFile,
                Charset.forName(Globals.DEFAULT_FILE_ENCODING));

        fw.write(createReportText(false));
        fw.write("\n");

        fw.flush();
        fw.close();

      } catch (Exception e) {
      }

      if (isGeneratedData())
        try {
          Files.copy(reportFile.toPath(), new File(expectedTestDirectory,
              filename).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
        }
    }

    /**
     * Create report retrieve by testng instance and display in report.
     * @return report text
     */
    private String createReportTestngMessage() {
      if (isSuccess())
        return "";

      // Text without stack message when an exception occurs
      String txt = "Fail test: " + testName;
      txt += createExceptionText(this.exception, false);
      return txt;
    }

    /**
     * Create report retrieve by global tests logger.
     * @param duration duration of execution
     * @return report text
     */
    private String getLoggerTest(final String duration) {
      if (nothingToDo)
        return "Nothing_to_do: for " + testName;

      String txt =
          (isSuccess() ? "SUCCESS" : "FAIL")
              + ": for "
              + testName
              + ((isGeneratedData())
                  ? ": generate expected data" : ": launch test and comparison")
              + " in " + duration;

      txt += createExceptionText(this.exception, false);

      return txt;
    }

    /**
     * Create report text.
     * @param withStackTrace if true contains the stack trace if exist
     * @return report text
     */
    private String createReportText(final boolean withStackTrace) {

      final StringBuilder txt = new StringBuilder();
      txt.append((isSuccess() ? "SUCCESS" : "FAIL")
          + ": for "
          + testName
          + ((isGeneratedData())
              ? ": generate expected data" : ": launch test and comparison"));

      // Add synthesis on execution script
      if (!this.resultScriptProcess.isEmpty()) {
        for (ResultScriptProcess rsp : this.resultScriptProcess) {
          txt.append(rsp.getMessage());
        }
      }

      if (isGeneratedData()) {
        txt.append("\nSUCCESS: copy files to "
            + expectedTestDirectory.getAbsolutePath());
      }

      // Add report text on comparison execution
      if (this.ouputComparison != null) {
        // Execution integration test
        txt.append("\n" + this.ouputComparison.getReport());
      }

      // Add message on exception
      if (this.exception != null) {
        if (!(this.exception instanceof ProcessScriptException))
          txt.append("\n" + createExceptionText(this.exception, withStackTrace));
      }

      // Return text
      return txt.toString();
    }

    //
    // Getter and Setter
    //

    public void asGeneratedData() {
      this.generatedData = true;
    }

    public boolean isGeneratedData() {
      return this.generatedData;
    }

    public void setSuccess(boolean result) {
      this.success = result;
    }

    public boolean isSuccess() {
      return this.success;
    }

    public void setException(final Throwable e) {
      this.exception = e;
    }

    public void setOutputComparison(OutputExecution outputComparison) {
      this.ouputComparison = outputComparison;
    }

    public void asNothingToDo() {
      this.nothingToDo = true;
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

    getLogger().info("Start test " + this.testName);

    getItSuite().startTest(this.outputTestDirectory);

    // Compile the result comparison from all tests
    ITResult resultIT = null;
    ITResult.OutputExecution outputComparison = null;
    boolean isSuccess = false;

    try {
      // Check data to generate
      if (!isDataNeededToBeGenerated()) {
        reportTest.asNothingToDo();
        isSuccess = true;
        // Nothing to do
        return;
      }

      // Build output directory with source files
      buildOutputDirectory();

      // Launch scripts
      launchScriptsTest();

      // Treat result application directory
      resultIT =
          new ITResult(this.outputTestDirectory, this.fileToComparePatterns,
              this.excludeToComparePatterns, this.checkExistenceFilePatterns,
              this.checkAbsenceFilePatterns);

      if (this.generateExpectedData) {
        this.reportTest.asGeneratedData();

        // Build expected directory if necessary
        createExpectedDirectory();

        // Copy files corresponding to pattern in expected data directory
        resultIT.copyFiles(this.expectedTestDirectory);

      } else {

        // Case comparison between expected and output test directory
        outputComparison =
            resultIT.compareTo(new ITResult(this.expectedTestDirectory
                .getParentFile(), this.fileToComparePatterns,
                this.excludeToComparePatterns, this.checkExistenceFilePatterns,
                this.checkAbsenceFilePatterns));
      }

      isSuccess = true;

    } catch (Throwable e) {
      reportTest.setException(e);
      throw new Exception(reportTest.createReportTestngMessage());

    } finally {
      // Update global counter
      getItSuite().updateCounter(reportTest.isSuccess());

      // Set success on generate data in expected directory
      reportTest.setSuccess(isSuccess);
      reportTest.setOutputComparison(outputComparison);
      reportTest.createReportFile();
      
      // End test
      timer.stop();
      getLogger().info(
          reportTest.getLoggerTest(toTimeHumanReadable(timer
              .elapsed(TimeUnit.MILLISECONDS))));
      
      getItSuite().endTest(this.outputTestDirectory);
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
   * Create message exception with stack trace if required.
   * @param withStackTrace if true contains the stack trace if exist
   * @return message
   */
  private static String createExceptionText(final Throwable exception,
      final boolean withStackTrace) {

    if (exception == null)
      return "";

    final StringBuilder msgException = new StringBuilder();

    msgException.append("\n=== Execution Test Error ===");
    msgException.append("\nFrom class: \n\t"
        + exception.getClass().getName() + "");
    msgException.append("\nException message: \n\t"
        + exception.getMessage() + "\n");

    if (isDebugEnable() && withStackTrace) {
      // Add the stack trace
      msgException.append("\n=== Execution Test Debug Stack Trace ===\n");
      msgException.append("\n"
          + Joiner.on("\n\t").join(exception.getStackTrace()));
    }

    // Return text
    return msgException.toString();
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
  // Scripting methods
  //
  /**
   * Launch all scripts defined for the test.
   * @throws ProcessScriptException if an error occurs while execute script
   * @throws IOException if the output directory is missing
   */
  private void launchScriptsTest() throws IOException, ProcessScriptException {

    checkExistingDirectoryFile(this.outputTestDirectory,
        "output test directory");

    // Define stdout and stderr file
    final File stdoutFile = new File(this.outputTestDirectory, STDOUT_FILENAME);
    final File stderrFile = new File(this.outputTestDirectory, STDERR_FILENAME);
    final File cmdLineFile =
        new File(this.outputTestDirectory, CMDLINE_FILENAME);

    // Save environment variable process in file
    saveEnvironmentVariable();

    // Generated test directory
    // Optional run pre-treatment global script, before specific of the test
    executeScript(ITFactory.PRETREATMENT_GLOBAL_SCRIPT_KEY, new File(stdoutFile
        + "_PRE_SCRIPT_GLOBAL"), new File(stderrFile + "_PRE_SCRIPT_GLOBAL"),
        false, "pre script global", null);

    // Optional script, pre-treatment before launch application
    executeScript(ITFactory.PRE_TEST_SCRIPT_CONF_KEY, new File(stdoutFile
        + "_PRE_SCRIPT"), new File(stderrFile + "_PRE_SCRIPT"), false,
        "pre script for test", null);

    // Execute application
    if (this.generateExpectedData && this.manualGenerationExpectedData)
      // Case generate expected data manually only it doesn't exists
      executeScript(ITFactory.COMMAND_TO_GENERATE_MANUALLY_CONF_KEY,
          stdoutFile, stderrFile, true, "manual script to generate data",
          cmdLineFile);
    else
      // Case execute testing application
      executeScript(ITFactory.COMMAND_TO_LAUNCH_APPLICATION_CONF_KEY,
          stdoutFile, stderrFile, true, "execution application", cmdLineFile);

    // Optional script, post-treatment after execution application and before
    // comparison between directories
    executeScript(ITFactory.POST_TEST_SCRIPT_CONF_KEY, new File(stdoutFile
        + "_POST_SCRIPT"), new File(stderrFile + "_POST_SCRIPT"), false,
        "post script for test", null);

    // Optional run post-treatment global script, after specific of the test
    executeScript(ITFactory.POSTTREATMENT_GLOBAL_SCRIPT_KEY, new File(
        stdoutFile + "_POST_SCRIPT_GLOBAL"), new File(stderrFile
        + "_POST_SCRIPT_GLOBAL"), false, "post script global", null);

  }

  /**
   * Execute a script from a command line retrieved from the test configuration.
   * @param scriptConfKey key for configuration to get command line
   * @param stdoutFile file where copy the standard output of the script
   * @param stderrFile file where copy the standard output of the script
   * @param saveStandardOutputInSuccess if true generate always standard output
   *          and error file otherwise only if script failed
   * @param message message to describe script
   * @param cmdLineFile file where copy the command line of the script
   * @throws EoulsanException if an error occurs while execute script
   */
  private ResultScriptProcess executeScript(final String scriptConfKey,
      final File stdoutFile, final File stderrFile,
      final boolean saveStandardOutputInSuccess, final String desc,
      final File cmdLineFile) throws ProcessScriptException {

    ProcessScriptException exception = null;
    int exitValue = -1;
    final Stopwatch timer = Stopwatch.createStarted();
    final long duration;

    if (this.testConf.getProperty(scriptConfKey) == null)
      return null;

    // Get command line from the configuration
    final String cmdLine = this.testConf.getProperty(scriptConfKey);

    // Replace application path variable in command line
    final String cmd =
        cmdLine.replace(APPLICATION_PATH_VARIABLE,
            this.applicationPath.getAbsolutePath()).trim();

    if (cmd.isEmpty())
      return null;

    // Save command line in file
    if (cmdLineFile != null)
      try {
        com.google.common.io.Files.write(cmd + "\n", cmdLineFile,
            Charsets.UTF_8);
      } catch (IOException e) {
        // Nothing to do
      }

    try {

      final Process p =
          Runtime.getRuntime().exec(cmdLine, this.environmentVariables,
              this.outputTestDirectory);

      // Save stdout
      if (stdoutFile != null) {
        new CopyProcessOutput(p.getInputStream(), stdoutFile, "stdout").start();
      }

      // Save stderr
      if (stderrFile != null) {
        new CopyProcessOutput(p.getErrorStream(), stderrFile, "stderr").start();
      }

      // Wait the end of the process
      exitValue = p.waitFor();

      // Execution script fail, create an exception
      if (exitValue != 0) {
        exception =
            new ProcessScriptException(
                "Error during execution script, bad exit value: " + exitValue);
      }

      if (exitValue == 0 && !saveStandardOutputInSuccess) {
        // Success execution, remove standard and error output file
        stdoutFile.delete();
        stderrFile.delete();
      }

    } catch (IOException | InterruptedException e) {
      exception = new ProcessScriptException(e);
    } finally {
      duration = timer.elapsed(TimeUnit.MILLISECONDS);
      timer.stop();
    }

    return new ResultScriptProcess(cmdLine, this.outputTestDirectory,
        exitValue, exception, stdoutFile, stderrFile, desc, duration);
  }

  /**
   * Extract all environment variables setting in test configuration file. Key
   * must be start with keyword {@link IT#PREFIX_ENV_VAR}
   * @return null if not found or an string array in the format name=value
   */
  private String[] extractEnvironmentVariables() {

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
    return envp.toArray(new String[envp.size()]);
  }

  /**
   * Save all environment variables in file.
   */
  private void saveEnvironmentVariable() {
    final File envFile = new File(this.outputTestDirectory, ENV_FILENAME);

    // Write in file
    if (this.environmentVariables == null
        || this.environmentVariables.length == 0) {
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
        + ", files from pattern(s) " + this.fileToComparePatterns + ""
        + this.checkExistenceFilePatterns;
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
  public IT(final Properties globalsConf, final File applicationPath,
      final File testConfFile, final File testsDirectory, final String testName)
      throws IOException, EoulsanException {

    this.testConf = loadConfigurationFile(globalsConf, testConfFile);

    // Extract environment variable from current context and configuration test
    this.environmentVariables = extractEnvironmentVariables();

    this.applicationPath = applicationPath;
    this.testName = testName;
    this.reportTest = new ReportTest();

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
