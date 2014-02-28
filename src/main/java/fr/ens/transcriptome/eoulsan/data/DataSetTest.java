package fr.ens.transcriptome.eoulsan.data;

import static com.google.common.io.Files.newReader;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingDirectoryFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.compress.utils.Charsets;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class DataSetTest {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private final static Splitter splitter = Splitter.on(' ').trimResults()
      .omitEmptyStrings();

  private final Properties props;

  private final File expectedDirectory;
  private final File testedDirectory;

  private final DataSetAnalysis dsaExpected;
  private final DataSetAnalysis dsaTested;

  private boolean isExecuted = false;

  public void executeTest() throws EoulsanException, IOException {

    if (!dsaExpected.isResultsAnalysisExists()) {
      // Analysis must be launch for expected result directory
      launchAnalysis(dsaExpected,
          new File(this.props.getProperty("eoulsan_reference_path")),
          this.expectedDirectory, true);
    }

    dsaExpected.parseDirectory();

    // Analysis for tested result directory
    launchAnalysis(dsaTested,
        new File(this.props.getProperty("eoulsan_version_to_test_path")),
        this.testedDirectory, false);
    dsaTested.parseDirectory();

    isExecuted = true;

  }

  public DataSetAnalysis getAnalysisExcepted() throws EoulsanException,
      IOException {

    if (!isExecuted)
      executeTest();
    return this.dsaExpected;
  }

  public DataSetAnalysis getAnalysisTest() throws EoulsanException, IOException {
    if (!isExecuted)
      executeTest();
    return this.dsaTested;
  }

  //
  // Private methods
  //

  private void launchAnalysis(final DataSetAnalysis dsa,
      final File eoulsanPath, final File outputTest,
      final boolean isDataSetExpected) throws IOException, EoulsanException {

    String script = this.props.getProperty("script_generated_data_expected");

    // Data expected must be generated
    if (isDataSetExpected && script != null && script.trim().length() > 0) {
      // Check if a script is define in test.txt
      executeScript("script_generated_data_expected", outputTest);

    } else {
      // Call Eoulsan
      checkExistingFile(eoulsanPath, "Eoulsan executable doesn't exists.");

      // Optional scripts
      executeScript("script_pretreatment", outputTest);

      // TODO
      // Launch eoulsan
      int exitValue =
          ProcessUtils.sh(buildCommandLine(dsa, eoulsanPath), outputTest);

      // Check exitvalue
      if (exitValue != 0) {
        throw new IOException("Bad error result for dataset "
            + outputTest.getAbsolutePath() + " execution: " + exitValue);
      }

      // If exists, launch postScript analysis
      executeScript("script_posttreatment", outputTest);

    }
  }

  private List<String> buildCommandLine(final DataSetAnalysis dsa,
      final File eoulsanPath) throws EoulsanException {

    // Data for execute Eoulsan
    final List<String> eoulsanArguments =
        Lists.newLinkedList(splitter.split(props
            .getProperty("command_line_eoulsan")));

    // Check command line include param and design file
    if (eoulsanArguments.indexOf("{param}") == -1)
      throw new EoulsanException(
          "Error in command line defined in test.txt file, no parameter include.");

    if (eoulsanArguments.indexOf("{design}") == -1)
      throw new EoulsanException(
          "Error in command line defined in test.txt file, no parameter include.");

    // Replace by file paths for parameter and design in command line
    // Eoulsan
    eoulsanArguments.set(eoulsanArguments.indexOf("{param}"), dsa
        .getParamFile().getAbsolutePath());
    eoulsanArguments.set(eoulsanArguments.indexOf("{design}"), dsa
        .getDesignFile().getAbsolutePath());

    List<String> cmd = Lists.newLinkedList();

    // cmd.add("screen");
    cmd.add(eoulsanPath.getAbsolutePath() + "/eoulsan.sh");
    // cmd.add("-help");

    // TODO add param conf

    // Add arguments from configuration file
    cmd.addAll(eoulsanArguments);

    LOGGER.info(StringUtils.join(cmd.toArray(), " "));
    // TODO
    System.out.println(StringUtils.join(cmd.toArray(), " "));

    return cmd;
  }

  private void executeScript(final String script_key, File outputDirectory)
      throws IOException {

    final String scriptPath = this.props.getProperty(script_key);
    if (scriptPath == null || scriptPath.trim().length() == 0)
      return;

    // Execute script
    File scriptFile = new File(scriptPath);

    checkExistingFile(scriptFile, scriptFile.getAbsolutePath());
    checkExistingDirectoryFile(outputDirectory,
        outputDirectory.getAbsolutePath());

    // If exists, launch preScript analysis
    int exitValue =
        ProcessUtils.sh(Lists.newArrayList(scriptFile.getAbsolutePath()),
            outputDirectory);

    if (exitValue != 0) {
      throw new IOException("Error during script execution  "
          + scriptFile.getAbsolutePath() + " exitValue: " + exitValue);
    }

  }

  private void initProperties(final File testFile) throws IOException {

    checkExistingFile(testFile, " configuration file doesn't exist.");

    final BufferedReader br =
        new BufferedReader(newReader(testFile,
            Charsets.toCharset(Globals.DEFAULT_FILE_ENCODING)));
    String line = null;

    try {
      while ((line = br.readLine()) != null) {

        final int pos = line.indexOf('=');
        if (pos == -1)
          continue;

        final String key = line.substring(0, pos);
        final String value = line.substring(pos + 1);

        props.put(key, value);
      }
      br.close();

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  //
  // Constructor
  //

  public DataSetTest(final Properties props, final File testFile,
      final File inputData, final File outputData) throws IOException,
      EoulsanException {

    this.expectedDirectory = new File(inputData, "/expected");
    this.testedDirectory =
        new File(outputData, testFile.getParentFile().getName());

    this.props = new Properties(props);
    initProperties(testFile);

    this.dsaExpected = new DataSetAnalysis(inputData, this.expectedDirectory);
    this.dsaTested = new DataSetAnalysis(inputData, this.testedDirectory);

  }

}
