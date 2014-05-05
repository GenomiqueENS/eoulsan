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
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.compress.utils.Charsets;
import org.testng.annotations.Test;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import fr.ens.transcriptome.eoulsan.EoulsanException;
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
public class RegressionProcessIT {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public final static Splitter CMD_LINE_SPLITTER = Splitter.on(' ')
      .trimResults().omitEmptyStrings();
  public static final String SEPARATOR = " ";

  /** Key for configuration test */
  public static final String PRETEST_SCRIPT_KEY = "pretest.script";
  public static final String POSTTEST_SCRIPT_KEY = "posttest.script";
  public static final String DESCRIPTION_KEY = "description";

  public static final String CMD_LINE_TO_REFERENCE_KEY =
      "cmd.line.to.reference";

  public static final String CMD_LINE_TO_TEST_KEY = "cmd.line.to.test";
  public static final String CMD_LINE_TO_GET_VERSION_REFERENCE_KEY =
      "cmd.line.to.get.version.reference";
  public static final String CMD_LINE_TO_GET_VERSION_TEST_KEY =
      "cmd.line.to.get.version.test";

  public static final String PATTERNS_INPUT_FILES_KEY = "patterns.input.files";
  public static final String PATTERNS_OUTPUT_FILES_KEY =
      "patterns.output.files";

  public static final String EXPECTED_DATA_GENERATED_MANUALLY_KEY =
      "expected.data.generated.manually";
  public final static String GENERATE_ALL_EXPECTED_DATA_KEY =
      "generate.all.expected.data";

  public static final String APPLI_PATH_VARIABLE = "{appli.path}";

  /** Variables */
  private final Properties testConf;
  private final String applicationPath;
  private final String testName;
  private final File inputTestDirectory;
  private final File outputTestDirectory;
  private final File expectedTestDirectory;

  private final boolean allTestsToGenerate;
  private final boolean newTestsToGenerate;
  private final boolean manuallyGenerate;

  private final boolean expectedDataToGenerate;

  private StringBuilder reportText = new StringBuilder();

  /**
   * Launch test execution, first generate data directory corresponding to the
   * arguments: expected data or data to test.If it is data to test then launch
   * comparison
   * @throws EoulsanException if an error occurs while execute script or
   *           comparison
   * @throws IOException if an error occurs while using the files.
   */
  @Test
  public void launchTest() throws Exception {

    // Init logger
    final Stopwatch timer = Stopwatch.createStarted();
    LOGGER.info("start test " + this.testName);

    // Compile the result comparison from all tests
    Boolean status = null;

    try {
      if (asNeedToGenerateData()) {
        // Build output directory with source files
        buildOutputDirectory();

        if (this.expectedDataToGenerate) {
          // Build expected directory if necessary
          createExpectedDirectory();
        }

        // Launch scripts
        launchScriptsTest();

        // Treat result application directory
        final RegressionResultIT regressionResultIT =
            new RegressionResultIT(this.outputTestDirectory,
                this.testConf.getProperty(PATTERNS_INPUT_FILES_KEY),
                this.testConf.getProperty(PATTERNS_OUTPUT_FILES_KEY));

        // Check comparison used
        if (this.expectedDataToGenerate) {
          // Copy files corresponding to pattern in expected data directory
          regressionResultIT.copyFiles(this.expectedTestDirectory);

        } else {
          // Case comparison between expected and output test directory
          regressionResultIT.compareTo(new RegressionResultIT(
              this.expectedTestDirectory.getParentFile(), this.testConf
                  .getProperty(PATTERNS_INPUT_FILES_KEY), this.testConf
                  .getProperty(PATTERNS_OUTPUT_FILES_KEY)));

          status = regressionResultIT.getResultComparison();
          this.reportText.append("\n" + regressionResultIT.getReport());
        }

      }
    } catch (Exception e) {
      e.printStackTrace();
      status = false;
      throw new Exception();
    } finally {

      final String suffix = status == null || status == false ? "FAIL" : "SUCCESS";

      // Create report file
      createReportFile(suffix);

      final String txt =
          (this.expectedDataToGenerate)
              ? ": generate expected data"
              : ": generate data to test and comparison";

      // End test
      timer.stop();
      LOGGER.info(suffix
          + txt + " in "
          + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));
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
      final String applicationPath) {

    String version = "UNKOWN";

    if (commandLine == null || commandLine.trim().length() == 0) {
      // None command line to retrieve version application set in configuration
      // file
      return version;
    }

    String cmd = commandLine;
    if (commandLine.indexOf(APPLI_PATH_VARIABLE) > -1) {
      // Replace application path in command line
      cmd = commandLine.replace(APPLI_PATH_VARIABLE, applicationPath);
    }

    // Execute command
    try {

      String exitValue = ProcessUtils.execToString(cmd);

      if (exitValue != null && exitValue.trim().length() > 0)
        // Retrieve version
        version = exitValue.trim();

    } catch (IOException e) {
    }

    return version;
  }

  /**
   * Check the expected data or data to test must be generated
   * @return true if data must be generated
   * @throws IOException if an error occurs while creating directory.
   */
  private boolean asNeedToGenerateData() throws IOException {

    // Command for generate data to test, in all case it is true
    if (!this.expectedDataToGenerate)
      return true;

    // Command for generate expected data test
    if (this.manuallyGenerate)
      return false;

    // Regenerate all expected data directory, remove if always exists
    if (allTestsToGenerate)
      return true;

    // Generate only missing expected data directory
    if (newTestsToGenerate && this.expectedTestDirectory.exists())
      return true;

    return false;
  }

  /**
   * Create expected data directory if the test demand generate expected data,
   * if it doesn't exist. In case generate all expected data directory needed,
   * replace by a new.
   * @throws IOException if an error occurs when creating directory.
   */
  private void createExpectedDirectory() throws IOException {

    // Skip if data to test to generate
    if (!this.expectedDataToGenerate || this.manuallyGenerate)
      return;

    // Check already exists
    if (this.newTestsToGenerate && this.expectedTestDirectory.exists())
      // Nothing to do
      return;

    // Regenerate existing expected data directory
    if (this.allTestsToGenerate && this.expectedTestDirectory.exists()) {
      // Remove existing directory
      recursiveDelete(this.expectedTestDirectory);
    }

    // New check existing directory
    if (!this.expectedTestDirectory.exists()) {
      // Create new expected data directory
      if (!this.expectedTestDirectory.mkdir())
        throw new IOException("Error while create expected data directory: "
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

    // Create a symbolic link for each file from input data test directory
    for (File file : this.inputTestDirectory.listFiles()) {
      if (file.isFile())
        createSymbolicLink(file, this.outputTestDirectory);
    }

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

    // Generated test directory
    // Optional script, pre-treatment before launch Eoulsan
    executeScript(PRETEST_SCRIPT_KEY);

    // Execute application
    if (this.expectedDataToGenerate)
      executeScript(CMD_LINE_TO_REFERENCE_KEY);
    else
      executeScript(CMD_LINE_TO_TEST_KEY);

    // Optional script, post-treatment after Eoulsan and before comparison
    // between directories
    executeScript(POSTTEST_SCRIPT_KEY);
  }

  /**
   * Execute a script from a command line retrieved from the test configuration
   * @param scriptConfKey key for configuration to get command line
   * @throws EoulsanException if an error occurs while execute script
   */
  private void executeScript(final String scriptConfKey)
      throws EoulsanException {

    if (this.testConf.getProperty(scriptConfKey) == null)
      return;

    String value = this.testConf.getProperty(scriptConfKey);
    String s = value;
    if (value.indexOf(APPLI_PATH_VARIABLE) > -1) {
      // Replace application path in command line
      s = value.replace(APPLI_PATH_VARIABLE, applicationPath);
    }

    // Build list command line
    final List<String> scriptCmdLine =
        Lists.newLinkedList(CMD_LINE_SPLITTER.split(s));

    if (scriptCmdLine.isEmpty())
      return;

    // Execute script
    reportText.append("\nexecute script with command line:"
        + Joiner.on(' ').join(scriptCmdLine));

    // Copy script in output directory
    final List<String> copyScriptCmd =
        Lists.newArrayList("cp", scriptCmdLine.get(0),
            this.outputTestDirectory.getAbsolutePath());

    int exitValue = -1;

    try {
      exitValue = ProcessUtils.sh(copyScriptCmd, this.outputTestDirectory);

      if (exitValue != 0) {
        LOGGER.warning("Fail copy script in directory "
            + this.outputTestDirectory + " for " + this.testName);
      }
      exitValue =
          ProcessUtils.sh(Lists.newArrayList(scriptCmdLine),
              this.outputTestDirectory);

      if (exitValue != 0) {
        throw new EoulsanException("Error during script execution  "
            + Joiner.on(" ").join(scriptCmdLine) + " (exitValue: " + exitValue
            + ")");
      }
    } catch (IOException e) {
      throw new EoulsanException("Script fail (cmd:"
          + Joiner.on(" ").join(scriptCmdLine) + ") with exit value "
          + exitValue + ", msg" + e.getMessage());
    }

  }

  //
  // Privates methods
  //
  /**
   * Retrieve properties for the test, compile specific configuration with
   * global
   * @param globalsConf global configuration for tests
   * @param testConfFile file with the test configuration
   * @throws IOException if an error occurs while reading the file.
   */
  private Properties readTestConfigurationFile(final Properties globalsConf,
      final File testConfFile) throws IOException {

    checkExistingFile(testConfFile, "configuration file");

    final Properties props = new Properties();

    // Add global configuration
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
        if (key.toLowerCase().startsWith("pattern") && props.containsKey(key)) {
          // Concatenate values
          value = props.getProperty(key) + SEPARATOR + value;
        }

        props.put(key, value);
      }
    }
    br.close();

    return props;

  }

  /**
   * Create the expected data test directory
   * @param inputTestDirectory source test directory with needed files
   * @return expected data directory for the test
   */
  private File retrieveExpectedDirectory(final File inputTestDirectory)
      throws EoulsanException {

    // Build expected data directory name
    if (this.expectedDataToGenerate) {

      // Retrieve command line from test configuration
      final String value =
          this.testConf.getProperty(CMD_LINE_TO_GET_VERSION_REFERENCE_KEY);

      final String versionExpectedApplication =
          retrieveVersionApplication(value, this.applicationPath);

      return new File(inputTestDirectory, "/expected_"
          + versionExpectedApplication);

    } else {
      // Find directory start with expected
      final File[] files = inputTestDirectory.listFiles(new FileFilter() {

        @Override
        public boolean accept(File pathname) {
          return pathname.getName().startsWith("expected");
        }
      });

      if (files.length != 1)
        throw new EoulsanException("More one expected file found in "
            + inputTestDirectory.getAbsolutePath());

      if (!files[0].isDirectory())
        throw new EoulsanException("No found expected directory in "
            + inputTestDirectory.getAbsolutePath());

      // Return expected data directory
      return files[0];
    }
  }

  private void createReportFile(final String status) {
    File directory = this.outputTestDirectory;

    final File reportFile = new File(directory, status);
    Writer fw;
    try {
      fw =
          newWriter(reportFile, Charset.forName(Globals.DEFAULT_FILE_ENCODING));

      fw.write(reportText.toString());
      fw.write("\n");

      fw.flush();
      fw.close();

    } catch (Exception e) {
    }

    if (expectedDataToGenerate)
      try {
        Files.copy(reportFile, new File(this.expectedTestDirectory, status));
      } catch (IOException e) {
      }
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   * @param globalsConf global configuration for tests
   * @param applicationPath path to the application to test
   * @param testConfFile file with the test configuration
   * @param outputTestsDirectory output test directory with result execute
   *          application
   * @param nameTest name test
   * @throws IOException if an error occurs while reading the configuration
   *           file.
   * @throws EoulsanException if an error occurs while search expected directory
   *           of the test.
   */
  public RegressionProcessIT(final Properties globalsConf,
      final String applicationPath, final File testConfFile,
      final File testsDirectory, String testName) throws IOException,
      EoulsanException {

    this.testConf = readTestConfigurationFile(globalsConf, testConfFile);

    this.applicationPath = applicationPath;
    this.testName = testName;

    this.inputTestDirectory = testConfFile.getParentFile();

    this.allTestsToGenerate =
        Boolean.parseBoolean(this.testConf
            .getProperty(RegressionITFactory.GENERATE_ALL_EXPECTED_DATA_KEY));

    this.newTestsToGenerate =
        Boolean.parseBoolean(this.testConf
            .getProperty(RegressionITFactory.GENERATE_NEW_EXPECTED_DATA_KEY));

    this.manuallyGenerate =
        Boolean.parseBoolean(this.testConf
            .getProperty(EXPECTED_DATA_GENERATED_MANUALLY_KEY));

    this.expectedDataToGenerate = allTestsToGenerate || newTestsToGenerate;
    this.expectedTestDirectory =
        retrieveExpectedDirectory(this.inputTestDirectory);

    this.outputTestDirectory = new File(testsDirectory, this.testName);

  }
}
