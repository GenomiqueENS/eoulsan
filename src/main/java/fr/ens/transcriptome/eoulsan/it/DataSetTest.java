package fr.ens.transcriptome.eoulsan.it;

import static com.google.common.io.Files.newReader;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingDirectoryFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.recursiveDelete;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.compress.utils.Charsets;
import org.testng.annotations.Test;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;

public class DataSetTest {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public static final boolean USE_SERIALIZATION = true;

  public final static Splitter CMD_LINE_SPLITTER = Splitter.on(' ')
      .trimResults().omitEmptyStrings();

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

  public static final String APPLI_PATH_VARIABLE = "{appli.path}";

  public final static String GENERATE_ALL_EXPECTED_DATA_KEY =
      "generate.all.expected.data";

  /** Variables */
  private final Properties props;

  private final String applicationPath;
  private final String testName;
  private final File testConfigurationFile;
  private final File inputDataDirectory;
  private final File expectedDirectory;
  private final File testedDirectory;

  private DataSetAnalysis dsaExpected;
  private DataSetAnalysis dsaTested;

  private boolean isExpectedGenerated = false;
  private boolean isTestedGenerated = false;

  private StringBuilder reportText = new StringBuilder();

  @Test
  public void executeAnalysis() throws EoulsanException, IOException {

    final Stopwatch timer = Stopwatch.createStarted();

    LOGGER.info("start test " + this.testName);

    boolean regenerateAllExpectedData =
        Boolean.getBoolean(this.props
            .getProperty(ITActionFactory.GENERATE_ALL_EXPECTED_DATA_KEY));

    // this.props.store(new FileWriter(new File("/tmp/props2.txt")), "ropr");

    // TODO
    for (Map.Entry<Object, Object> e : this.props.entrySet()) {

      if (((String) e.getKey())
          .equals(ITActionFactory.GENERATE_ALL_EXPECTED_DATA_KEY)) {

        System.out.println(e.getKey()
            + "\t\t"
            + e.getValue()
            + "\t:\t"
            + ((String) e.getKey())
                .equals(ITActionFactory.GENERATE_ALL_EXPECTED_DATA_KEY));

        System.out.println("new "
            + this.props.getProperty("generate.all.expected.data") + " vs "
            + this.props.getProperty(GENERATE_ALL_EXPECTED_DATA_KEY) + " key "
            + GENERATE_ALL_EXPECTED_DATA_KEY);
      }
    }

    boolean generateNewExpectedData =
        Boolean.getBoolean(this.props
            .getProperty(ITActionFactory.GENERATE_NEW_EXPECTED_DATA_KEY));

    // Call for treated expected data directory
    if (regenerateAllExpectedData || generateNewExpectedData) {

      generateDataExpected(regenerateAllExpectedData);

      LOGGER.info("Generated expected data in "
          + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

    } else {

      // Call for launch test
      generateDataTested();

      // TODO
      final boolean asRegression = comparisonDirectory(this.testedDirectory);

      LOGGER.info((asRegression ? "FAIL" : "SUCCESS")
          + ": analysis test and comparison in "
          + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

    }

    timer.stop();

  }

  /**
   * @throws EoulsanException
   * @throws IOException
   */
  public void generateDataExpected(final boolean regenerateExpectedData)
      throws EoulsanException, IOException {

    if (this.isExpectedGenerated || isGeneratedManually()) {

      LOGGER.fine(testName + ": check expected data");

      this.dsaExpected.checkExpectedDirectory(true);
      return;
    }

    if (regenerateExpectedData && !isGeneratedManually()) {
      recursiveDelete(getExpectedDirectory());
    }

    // Initialization expected directory
    this.dsaExpected =
        new DataSetAnalysis(this.props, inputDataDirectory,
            this.expectedDirectory, this.testName);

    // TODO
    // argument to regenerate all expected directory if build automatically
    if (regenerateExpectedData || !dsaExpected.isResultsAnalysisExists()) {
      LOGGER.info(testName + ": (re)generated expected data");

      // Analysis must be launch for expected result directory
      launchAnalysis(dsaExpected, this.expectedDirectory, true);

    } else {
      LOGGER.fine(testName + ": check expected data");
      this.dsaExpected.checkExpectedDirectory(true);
    }

    this.dsaExpected.parseDirectory();

    this.isExpectedGenerated = true;

  }

  /**
   * @throws EoulsanException
   * @throws IOException
   */
  public void generateDataTested() throws EoulsanException, IOException {

    if (this.isTestedGenerated)
      return;

    // Initialization test directory
    this.dsaTested =
        new DataSetAnalysis(this.props, inputDataDirectory,
            this.testedDirectory, this.testName);

    this.dsaTested.buildAnalysisDirectory();

    // Analysis for tested result directory
    launchAnalysis(dsaTested, this.testedDirectory, false);

    this.dsaTested.parseDirectory();

    this.isTestedGenerated = true;

    // Initialization expected directory
    this.dsaExpected =
        new DataSetAnalysis(this.props, inputDataDirectory,
            this.expectedDirectory, this.testName);

    if (!dsaExpected.isResultsAnalysisExists())
      throw new EoulsanException(
          "Launch test analysis fail: expected data doesn't exists");

    this.dsaExpected.parseDirectory();

  }

  //
  // Private methods
  //

  /**
   * @param dsa
   * @param eoulsanPath
   * @param outputDirectory
   * @param isDataSetExpected
   * @throws IOException
   * @throws EoulsanException
   */
  private void launchAnalysis(final DataSetAnalysis dsa,
      final File outputDirectory, final boolean isDataSetExpected)
      throws IOException, EoulsanException {

    // Data expected must be generated
    if (isDataSetExpected) {
      // Execute application
      executeScript(CMD_LINE_TO_REFERENCE_KEY, outputDirectory);

    } else {
      // Generated test directory
      // Optional script, pre-treatment before launch Eoulsan
      executeScript(PRETEST_SCRIPT_KEY, outputDirectory);

      // Execute application
      executeScript(CMD_LINE_TO_TEST_KEY, outputDirectory);

      // Optional script, post-treatment after Eoulsan and before comparison
      // between directories
      executeScript(POSTTEST_SCRIPT_KEY, outputDirectory);

    }
  }

  private boolean comparisonDirectory(final File outputDirectory)
      throws EoulsanException, IOException {

    final DirectoriesComparator comparator =
        new DirectoriesComparator(outputDirectory, this.testName,
            USE_SERIALIZATION);

    final DataSetAnalysis dsaExpected = getAnalysisExcepted();
    final DataSetAnalysis dsaTested = getAnalysisTest();

    // Add pattern used for compare files after analysis
    comparator.setPatternToCompare(this.props
        .getProperty(PATTERNS_OUTPUT_FILES_KEY)
        + ","
        + this.props.getProperty(PATTERNS_INPUT_FILES_KEY));

    comparator.compareDataSet(dsaExpected, dsaTested,
        this.reportText.toString());

    return comparator.asRegression();
  }

  /**
   * @param scriptKey
   * @param outputDirectory
   * @throws IOException
   */
  private void executeScript(final String scriptKey, File outputDirectory)
      throws EoulsanException, IOException {

    if (this.props.getProperty(scriptKey) == null)
      return;

    String value = this.props.getProperty(scriptKey);
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
    checkExistingDirectoryFile(outputDirectory,
        outputDirectory.getAbsolutePath());

    reportText.append("\nexecute script with command line:"
        + Joiner.on(' ').join(scriptCmdLine));

    // Copy script in output directory
    final List<String> copyScriptCmd =
        Lists.newArrayList("cp", scriptCmdLine.get(0),
            outputDirectory.getAbsolutePath());

    int exitValue = ProcessUtils.sh(copyScriptCmd, outputDirectory);

    if (exitValue != 0) {
      LOGGER.warning("Fail copy script in directory "
          + outputDirectory + " for " + this.testName);
    }

    // If exists, launch preScript analysis
    exitValue =
        ProcessUtils.sh(Lists.newArrayList(scriptCmdLine), outputDirectory);

    if (exitValue != 0) {
      throw new EoulsanException("Error during script execution  "
          + Joiner.on(" ").join(scriptCmdLine) + " (exitValue: " + exitValue
          + ")");
    }

  }

  public static String retrieveVersionApplication(final Properties props,
      final String key, final String applicationPath) {

    String value = props.getProperty(key);
    String version = "UNKOWN";

    if (value == null || value.trim().length() == 0) {
      // None command line to retrieve version application set in configuration
      // file
      return version;
    }

    String cmd = value;
    if (value.indexOf(APPLI_PATH_VARIABLE) > -1) {
      // Replace application path in command line
      cmd = value.replace(APPLI_PATH_VARIABLE, applicationPath);
    }

    // Execute command
    try {
      // Retrieve version

      String exitValue = ProcessUtils.execToString(cmd);
      if (exitValue != null && exitValue.trim().length() > 0)
        version = exitValue.trim();

    } catch (IOException e) {
    }

    return version;
  }

  /**
   * @throws IOException
   */
  private void initProperties() throws IOException {

    checkExistingFile(testConfigurationFile,
        " configuration file doesn't exist.");

    final BufferedReader br =
        new BufferedReader(newReader(testConfigurationFile,
            Charsets.toCharset(Globals.DEFAULT_FILE_ENCODING)));
    String line = null;

    try {
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
          if (key.toLowerCase().startsWith("pattern")
              && this.props.containsKey(key)) {
            // Concatenate value
            value = this.props.getProperty(key) + ", " + value;
          }

          this.props.put(key, value);
          reportText.append("\n" + key + ": " + value);
        }
      }
      br.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  //
  // Getter and Setter
  //

  private boolean isGeneratedManually() {
    final String value =
        this.props.getProperty(EXPECTED_DATA_GENERATED_MANUALLY_KEY);
    return !value.toLowerCase(Globals.DEFAULT_LOCALE).equals("false");
  }

  public DataSetAnalysis getAnalysisExcepted() throws EoulsanException,
      IOException {

    return this.dsaExpected;
  }

  public DataSetAnalysis getAnalysisTest() throws EoulsanException, IOException {
    return this.dsaTested;
  }

  public String getPatternsToCompare() {
    return getPatternsInputRequired() + "," + getPatternsOutputRequired();
  }

  public String getPatternsInputRequired() {
    return this.props.getProperty(PATTERNS_INPUT_FILES_KEY);
  }

  public String getPatternsOutputRequired() {
    return this.props.getProperty(PATTERNS_OUTPUT_FILES_KEY);
  }

  public String getDescriptionTest() {
    return this.props.getProperty(DESCRIPTION_KEY);
  }

  public File getExpectedDirectory() {
    return this.expectedDirectory;
  }

  public File getTestedDirectory() {
    return this.testedDirectory;
  }

  //
  // Constructor
  //

  public DataSetTest(final Properties props, final String applicationPath,
      final File testFile, final File inputData, final File outputData,
      final String testName) throws IOException,
      EoulsanException {

    this.props = new Properties();

    // Load all default configuration properties
    this.props.putAll(props);

    this.inputDataDirectory = inputData;
    this.testedDirectory =
        new File(outputData, testFile.getParentFile().getName());
    this.applicationPath = applicationPath;

    this.testName = testName;
    this.testConfigurationFile = testFile;

    initProperties();

    final String versionExpectedApplication =
        retrieveVersionApplication(this.props,
            CMD_LINE_TO_GET_VERSION_REFERENCE_KEY, applicationPath);

    this.expectedDirectory =
        new File(inputData, "/expected_" + versionExpectedApplication);

  }

}
