package fr.ens.transcriptome.eoulsan.data;

import static com.google.common.io.Files.newReader;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingDirectoryFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.recursiveDelete;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.compress.utils.Charsets;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class DataSetTest {

  /** Logger */
  private static final Logger LOGGER_TEST = Logger.getLogger(Globals.APP_NAME);

  /** Key for configuration test */
  private static final String COMMAND_LINE_EOULSAN_KEY = "command_line_eoulsan";
  private static final String SCRIPT_PRETREATMENT_KEY = "script_pretreatment";
  private static final String SCRIPT_POSTTREATMENT_KEY = "script_posttreatment";
  private static final String DESCRIPTION_KEY = "description";
  private static final String SCRIPT_GENERATED_DATA_EXPECTED_KEY =
      "script_generated_data_expected";

  private static final String PATTERNS_INPUT_FILES_KEY = "patterns_input_files";
  private static final String PATTERNS_OUTPUT_FILES_KEY =
      "patterns_output_files";

  private static final String CHECKING_EXISTING_FILES_KEY =
      "check_existing_files_between_directories";
  private static final String EOULSAN_CONF_FILE_KEY = "eoulsan_conf_file";
  private static final String EXPECTED_DATA_GENERATED_MANUALLY_KEY =
      "expected_data_generated_manually";

  private final static Splitter splitter = Splitter.on(' ').trimResults()
      .omitEmptyStrings();

  private final Properties props;

  private final File testConfigurationFile;
  private final File inputDataDirectory;
  private final File expectedDirectory;
  private final File testedDirectory;

  private DataSetAnalysis dsaExpected;
  private DataSetAnalysis dsaTested;

  private boolean isExpectedGenerated = false;
  private boolean isTestedGenerated = false;

  /**
   * @throws EoulsanException
   * @throws IOException
   */
  public void generateDataExpected(final boolean regenerateExpectedData)
      throws EoulsanException, IOException {

    if (this.isExpectedGenerated || isGeneratedManually())
      return;

    // Initialization expected directory
    this.dsaExpected =
        new DataSetAnalysis(this.props, inputDataDirectory,
            this.expectedDirectory);

    // TODO
    // argument to regenerate all expected directory if build automatically
    if (regenerateExpectedData || !dsaExpected.isResultsAnalysisExists()) {

      if (regenerateExpectedData) {
        recursiveDelete(getExpectedDirectory());
        // Create empty directory
        this.dsaExpected =
            new DataSetAnalysis(this.props, inputDataDirectory,
                this.expectedDirectory);
      }

      LOGGER_TEST.info("generate expected data");

      // Analysis must be launch for expected result directory
      launchAnalysis(dsaExpected,
          new File(this.props.getProperty("eoulsan_reference_path")),
          this.expectedDirectory, true);
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
    launchAnalysis(dsaTested,
        new File(this.props.getProperty("eoulsan_version_to_test_path")),
        this.testedDirectory, false);

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
      final File eoulsanPath, final File outputDirectory,
      final boolean isDataSetExpected) throws IOException, EoulsanException {

    String script = this.props.getProperty("script_generated_data_expected");

    // Data expected must be generated
    if (isDataSetExpected) {
      if (script == null || script.trim().length() == 0) {
        // Execute Eoulsan
        executeEoulsan(dsa, eoulsanPath, outputDirectory);

      } else {
        // Check if a script is define in test.txt
        executeScript(SCRIPT_GENERATED_DATA_EXPECTED_KEY, outputDirectory);
      }

    } else {
      // Generated test directory
      // Optional script, pre-treatment before launch Eoulsan
      executeScript(SCRIPT_PRETREATMENT_KEY, outputDirectory);

      // Execute Eoulsan
      executeEoulsan(dsa, eoulsanPath, outputDirectory);

      // Optional script, post-treatment after Eoulsan and before comparison
      // between directories
      executeScript(SCRIPT_POSTTREATMENT_KEY, outputDirectory);

    }
  }

  /**
   * @param dsa
   * @param eoulsanPath
   * @return
   * @throws EoulsanException
   */
  private List<String> buildCommandLine(final DataSetAnalysis dsa,
      final File eoulsanPath) throws EoulsanException {

    // Data for execute Eoulsan
    final List<String> eoulsanArguments =
        Lists.newLinkedList(splitter.split(props
            .getProperty(COMMAND_LINE_EOULSAN_KEY)));

    if (eoulsanArguments == null || eoulsanArguments.isEmpty())
      throw new EoulsanException("Eoulsan arguments for command line is empty.");

    // Check command line include param and design file
    if (eoulsanArguments.indexOf("{param}") == -1)
      throw new EoulsanException(
          "Error in Eoulsan command line: no parameter included.");

    if (eoulsanArguments.indexOf("{design}") == -1)
      throw new EoulsanException(
          "Error in Eoulsan command line: no design included.");

    // Replace by parameter in command line Eoulsan
    eoulsanArguments.set(eoulsanArguments.indexOf("{param}"), dsa
        .getParamFile().getAbsolutePath());

    // Replace by design in command line Eoulsan
    eoulsanArguments.set(eoulsanArguments.indexOf("{design}"), dsa
        .getDesignFile().getAbsolutePath());

    List<String> cmd = Lists.newLinkedList();

    cmd.add(eoulsanPath.getAbsolutePath() + "/eoulsan.sh");

    // Set eoulsan configuration file path
    if (this.props.getProperty(EOULSAN_CONF_FILE_KEY) != null) {
      cmd.add("-conf");
      cmd.add(this.props.getProperty(EOULSAN_CONF_FILE_KEY));
    }

    // Add arguments from configuration file
    cmd.addAll(eoulsanArguments);

    LOGGER_TEST.info(StringUtils.join(cmd.toArray(), " "));

    return cmd;
  }

  /**
   * @param dsa
   * @param eoulsanPath
   * @param outputDirectory
   * @throws IOException
   * @throws EoulsanException
   */
  private void executeEoulsan(final DataSetAnalysis dsa,
      final File eoulsanPath, final File outputDirectory) throws IOException,
      EoulsanException {

    // Check file
    checkExistingFile(eoulsanPath, "Eoulsan executable doesn't exists.");
    checkExistingDirectoryFile(outputDirectory,
        outputDirectory.getAbsolutePath());

    // Launch Eoulsan with command line
    int exitValue =
        ProcessUtils.sh(buildCommandLine(dsa, eoulsanPath), outputDirectory);

    // Check exitvalue
    if (exitValue != 0) {
      throw new IOException("Bad error result for dataset expected "
          + outputDirectory.getAbsolutePath() + " execution: " + exitValue);
    }
  }

  /**
   * @param script_key
   * @param outputDirectory
   * @throws IOException
   */
  private void executeScript(final String script_key, File outputDirectory)
      throws IOException {

    if (this.props.getProperty(script_key) == null)
      return;

    final List<String> scriptCmdLine =
        Lists.newLinkedList(splitter.split(this.props.getProperty(script_key)));

    if (scriptCmdLine.isEmpty())
      return;

    // Execute script
    checkExistingDirectoryFile(outputDirectory,
        outputDirectory.getAbsolutePath());

    // If exists, launch preScript analysis
    int exitValue =
        ProcessUtils.sh(Lists.newArrayList(scriptCmdLine), outputDirectory);

    if (exitValue != 0) {
      throw new IOException("Error during script execution  "
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
        final String value = line.substring(pos + 1).trim();

        // Save parameter with value
        if (value.length() > 0) {
          this.props.put(key, value);
          LOGGER_TEST.config(key + ": " + value);
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

  public boolean isCheckingExistingFiles() {

    String s = this.props.getProperty(CHECKING_EXISTING_FILES_KEY);
    if (s == null || s.length() == 0)
      return false;

    return s.toLowerCase().equals("true");
  }

  //
  // Constructor
  //

  public DataSetTest(final Properties props, final File testFile,
      final File inputData, final File outputData) throws IOException,
      EoulsanException {

    this.inputDataDirectory = inputData;
    this.expectedDirectory = new File(inputData, "/expected");
    this.testedDirectory =
        new File(outputData, testFile.getParentFile().getName());

    this.props = new Properties(props);
    this.testConfigurationFile = testFile;

    initProperties();

  }

}
