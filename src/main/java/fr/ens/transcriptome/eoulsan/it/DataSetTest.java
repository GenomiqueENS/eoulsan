package fr.ens.transcriptome.eoulsan.it;

import static com.google.common.io.Files.newReader;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingDirectoryFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.recursiveDelete;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.compress.utils.Charsets;
import org.testng.annotations.Test;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.actions.RegressionAction;
import fr.ens.transcriptome.eoulsan.io.comparator.DirectoriesComparator;
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

  public static final String APPLI_PATH_KEY = "{appli.path}";

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

    boolean regenerateAllExpectedData =
        Boolean.getBoolean(this.props
            .getProperty(ITActionFactory.GENERATE_ALL_EXPECTED_DATA_KEY));

    boolean generateNewExpectedData =
        Boolean.getBoolean(this.props
            .getProperty(ITActionFactory.GENERATE_NEW_EXPECTED_DATA_KEY));

    // Call for treated expected data directory
    if (regenerateAllExpectedData || generateNewExpectedData) {
      generateDataExpected(regenerateAllExpectedData);
    } else {

      // Call for launch test
      generateDataTested();
    }
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

    if (regenerateExpectedData) {
      recursiveDelete(getExpectedDirectory());
    }

    // Initialization expected directory
    this.dsaExpected =
        new DataSetAnalysis(this.props, inputDataDirectory,
            this.expectedDirectory);

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
            this.testedDirectory);

    // Analysis for tested result directory
    launchAnalysis(dsaTested, this.testedDirectory, false);

    this.dsaTested.parseDirectory();

    this.isTestedGenerated = true;

    // Initialization expected directory
    this.dsaExpected =
        new DataSetAnalysis(this.props, inputDataDirectory,
            this.expectedDirectory);
    if (!dsaExpected.isResultsAnalysisExists())
      throw new EoulsanException(
          "Launch test analysis fail: expected data doesn't exists");
    this.dsaExpected.parseDirectory();

    // TODO
    comparisonDirectory(this.testedDirectory);
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

  private void comparisonDirectory(final File outputDirectory)
      throws EoulsanException, IOException {

    final DirectoriesComparator comparator =
        new DirectoriesComparator(USE_SERIALIZATION);

    final DataSetAnalysis dsaExpected = getAnalysisExcepted();
    final DataSetAnalysis dsaTested = getAnalysisTest();

    comparator.setPatternToCompare(this.props
        .getProperty(PATTERNS_OUTPUT_FILES_KEY));

    comparator.compareDataSet(dsaExpected, dsaTested);

    String reportComparison = comparator.buildReport(testName);

    String fileName = (comparator.asRegression() ? "FAIL" : "SUCCESS");

    // Build report file
    final File reportFile = new File(outputDirectory, fileName);

    final Writer fw =
        Files.asCharSink(reportFile,
            Charsets.toCharset(Globals.DEFAULT_FILE_ENCODING)).openStream();

    fw.write(reportText.toString());
    fw.write("\n");
    fw.write(reportComparison);

    fw.flush();
    fw.close();
  }

  /**
   * @param script_key
   * @param outputDirectory
   * @throws IOException
   */
  private void executeScript(final String script_key, File outputDirectory)
      throws EoulsanException, IOException {

    if (this.props.getProperty(script_key) == null)
      return;

    String value = this.props.getProperty(script_key);
    String s = value;
    if (value.indexOf(APPLI_PATH_KEY) > -1) {
      // Replace application path in command line
      s = value.replace(APPLI_PATH_KEY, applicationPath);
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

    // If exists, launch preScript analysis
    int exitValue =
        ProcessUtils.sh(Lists.newArrayList(scriptCmdLine), outputDirectory);

    if (exitValue != 0) {
      throw new EoulsanException("Error during script execution  "
          + Joiner.on(" ").join(scriptCmdLine) + " (exitValue: " + exitValue
          + ")");
    }

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

          // Key pattern : add value for test to general value from
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
      final String testName) throws IOException, EoulsanException {

    this.inputDataDirectory = inputData;
    this.expectedDirectory = new File(inputData, "/expected");
    this.testedDirectory =
        new File(outputData, testFile.getParentFile().getName());
    this.applicationPath = applicationPath;

    this.testName = testName;

    this.props = new Properties(props);
    this.testConfigurationFile = testFile;

    initProperties();

  }

}
