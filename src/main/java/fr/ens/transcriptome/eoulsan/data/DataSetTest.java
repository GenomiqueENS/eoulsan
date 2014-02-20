package fr.ens.transcriptome.eoulsan.data;

import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingDirectoryFile;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class DataSetTest {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd",
      Globals.DEFAULT_LOCALE);

  private final Splitter splitter = Splitter.on(' ').trimResults()
      .omitEmptyStrings();

  private final File inputDataDirectory;
  private final Map<String, File> parametersFiles;
  private final boolean expected;
  private final Properties props;
  // type data input used (small or really)
  private File inputDataProject;
  private File outputDataProject;
  private File designFile;
  private Collection<File> fastqFiles;
  private boolean toGenerateAnalysisResult = true;

  private File eoulsanPath;
  private String eoulsanVersionGit;
  private List<String> eoulsanArguments;

  // Set test name with the data result
  private Map<String, DataSetAnalysis> dataSet;

  public Map<String, DataSetAnalysis> execute() throws EoulsanException,
      IOException {

    Map<String, DataSetAnalysis> tests =
        Maps.newHashMapWithExpectedSize(parametersFiles.size());

    // Check if Eoulsan must be launch on input data

    for (String testName : parametersFiles.keySet()) {

      DataSetAnalysis dsa =
          new DataSetAnalysis(inputDataProject, expected,
              parametersFiles.get(testName), this);
      File outputTest = new File(outputDataProject, "test_" + testName);

      if (toGenerateAnalysisResult) {
        // Create result directory
        dsa.buildDirectoryAnalysis(outputTest);

        // Launch eoulsan on data and create results directory
        launchAnalysis(parametersFiles.get(testName), outputTest);
      }

      // Add in map
      tests.put(testName, dsa);
    }

    return tests;
  }

  //
  // Private methods
  //

  private void launchAnalysis(final File param, final File outputTest)
      throws IOException {
    // Launch Eoulsan
    CommandEoulsan cmd = new CommandEoulsan();

    // TODO
    // Launch eoulsan
    final int exitValue =
        ProcessUtils.sh(cmd.buildCommandLine(param), outputTest);

    // Check exitvalue
    if (exitValue != 0) {
      throw new IOException("Bad error result for dataset "
          + outputTest.getAbsolutePath() + " execution: " + exitValue);
    }
  }

  private void init() throws IOException {

    final String typeDataSetUsed = this.props.getProperty("type_data_used");
    this.inputDataProject = new File(inputDataDirectory, typeDataSetUsed);

    checkExistingDirectoryFile(
        this.inputDataProject,
        "Project directory doesn't exist in directory "
            + inputDataProject.getAbsolutePath());

    // At the root of type input data directory
    this.designFile = filterFile(inputDataDirectory, ".txt")[0];
    this.fastqFiles = Arrays.asList(filterFile(inputDataDirectory, ".fastq"));

    this.eoulsanArguments =
        Lists.newArrayList(splitter.split(props
            .getProperty("command_line_argument_eoulsan")));

    if (expected) {
      this.eoulsanPath = new File(props.getProperty("eoulsan_reference_path"));

      checkExistingDirectoryFile(
          this.eoulsanPath,
          "Eoulsan executable doesn't exist for reference in "
              + eoulsanPath.getAbsolutePath());

      this.eoulsanVersionGit =
          props.getProperty("eoulsan_reference_version_git");

      this.outputDataProject =
          new File(this.props.getProperty("expected_analysis_directory"),
              buildNameDirectory());

      // Check outputData for reference Eoulsan version
      if (this.outputDataProject.exists()) {
        // Doesn't exist, analysis must be launch
        toGenerateAnalysisResult = false;
      }
    } else {

      this.eoulsanPath =
          new File(props.getProperty("eoulsan_version_to_test_path"));
      checkExistingDirectoryFile(
          this.eoulsanPath,
          "Eoulsan executable doesn't exist for test in "
              + eoulsanPath.getAbsolutePath());

      this.eoulsanVersionGit = props.getProperty("eoulsan_test_version_git");

      // TODO duplicate with code Action
      // Create new directory for test
      this.outputDataProject =
          new File(this.props.getProperty("output_analysis_directory"),
              buildNameDirectory());

    }

  }

  private String buildNameDirectory() {

    // Creation date, format (yyyyMMdd)
    return DATE_FORMAT.format(new Date()) + "_" + eoulsanVersionGit;

  }

  private File[] filterFile(final File dir, final String extension) {

    return dir.listFiles(new FileFilter() {

      @Override
      public boolean accept(File pathname) {
        return pathname.getName().contains(extension);
      }
    });
  }

  //
  // Getter
  //
  public File getDesignFile() {
    return this.designFile;
  }

  public Collection<File> getFastqFiles() {
    return this.fastqFiles;
  }

  //
  // Constructor
  //
  public DataSetTest(final Properties props,
      final Map<String, File> parametersFiles, final File inputData,
      final boolean expected) throws IOException {

    this.props = props;
    this.parametersFiles = parametersFiles;
    this.inputDataDirectory = inputData;
    this.expected = expected;

    init();

  }

  //
  // Internal class
  //
  class CommandEoulsan {

    public List<String> buildCommandLine(final File param) {

      List<String> cmd = Lists.newArrayList();

      // cmd.add("screen");
      cmd.add(eoulsanPath + "/eoulsan.sh");
      // cmd.add("-help");

      // Add arguments from configuration file
      cmd.addAll(eoulsanArguments);

      cmd.add("exec");
      cmd.add(param.getAbsolutePath());
      cmd.add(designFile.getAbsolutePath());
      // cmd.add(">");
      // cmd.add(outFilename);
      // cmd.add("2>");
      // cmd.add(errFilename);

      LOGGER.info(StringUtils.join(cmd.toArray(), " "));
      // TODO
      System.out.println(StringUtils.join(cmd.toArray(), " "));

      return cmd;
    }
  }
}
