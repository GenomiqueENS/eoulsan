package fr.ens.transcriptome.eoulsan.actions;

import static com.google.common.io.Files.newReader;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.compress.utils.Charsets;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.data.DataSetAnalysis;
import fr.ens.transcriptome.eoulsan.data.DataSetTest;
import fr.ens.transcriptome.eoulsan.io.ComparatorDirectories;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class ValidationAction extends AbstractAction {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public static DateFormat DATE_FORMAT = new SimpleDateFormat(
      "yyyyMMdd-kkmmss", Globals.DEFAULT_LOCALE);

  public static final boolean USE_SERIALIZATION = true;
  public static final boolean CHECKING_SAME_NAME = true;

  private final Splitter splitter = Splitter.on(',').trimResults()
      .omitEmptyStrings();

  private final Properties props;
  private final ComparatorDirectories comparator;

  private File inputData;
  private Map<String, File> paramtersFiles;
  private File expectedAnalysisDirectory;
  private File outputAnalysisDirectory;
  private boolean isNewVersionEoulsan = false;

  private Collection<File> inputDataProjects;
  private String typeDataSetUsed = "small";

  // Optional
  private String localScriptPretreatement;
  private String localScriptPostreatement;
  private String hadoopScriptPretreatement;
  private String hadoopScriptPostreatement;

  @Override
  public String getName() {
    return "validation";
  }

  @Override
  public String getDescription() {
    return "test " + Globals.APP_NAME + " version.";
  }

  // TODO use for test into eclipse
  private void initLogger(final String logPath) {
    String s = logPath + "/eoulsan_" + DATE_FORMAT.format(new Date()) + ".log";
    System.out.println("log path " + s);

    Handler fh = null;
    try {
      fh = new FileHandler(s);

    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    fh.setFormatter(Globals.LOG_FORMATTER);

    LOGGER.setLevel(Level.ALL);
    // LOGGER.setUseParentHandlers(false);

    LOGGER.addHandler(fh);
  }

  @Override
  public void action(String[] arguments) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    String confPath = null;
    String jobDescription = null;

    int argsOptions = 0;

    try {

      // parse the command line arguments
      final CommandLine line = parser.parse(options, arguments, true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }

      // Description
      if (line.hasOption("d")) {

        jobDescription = line.getOptionValue("d");
        argsOptions += 2;
      }

      if (line.hasOption("c")) {

        // Configuration test files
        confPath = line.getOptionValue("c").trim();
        argsOptions += 2;
      }

    } catch (ParseException e) {
      Common.errorExit(e,
          "Error while parsing parameter file: " + e.getMessage());
    }

    if (arguments.length != argsOptions) {
      help(options);
    }

    // Execute program in local mode
    run(confPath, jobDescription);
  }

  /**
   * Create options for command line
   * @return an Options object
   */
  @SuppressWarnings("static-access")
  private Options makeOptions() {

    // create Options object
    final Options options = new Options();

    // Help option
    options.addOption("h", "help", false, "display this help");

    // Description option
    options.addOption(OptionBuilder.withArgName("description").hasArg()
        .withDescription("job description").withLongOpt("desc").create('d'));

    // Path to test Eoulsan version
    options.addOption(OptionBuilder.withArgName("confPath").hasArg(true)
        .withDescription("configuration test file").withLongOpt("conf")
        .create('c'));

    return options;
  }

  /**
   * Show command line help.
   * @param options Options of the software
   */
  private void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE
        + ".sh " + getName()
        + " [options] pathEoulsanNewVersion listDatasets outputDirectory",
        options);

    Common.exit(0);
  }

  //
  // Execution
  //

  /**
   * Launch Eoulsan on each datasets and if success compares result directory
   * @param pathEoulsanNewVersion
   * @param listDatasets
   * @param outputDirectory
   * @param jobDescription
   */
  public/* private */void run(final String confPath, final String jobDescription) {

    final String desc;

    if (jobDescription == null) {
      desc = "no job description";
    } else {
      desc = jobDescription.trim();
    }

    LOGGER.info(Globals.WELCOME_MSG + " Local mode.");

    try {
      // Initialization action from configuration test file
      init(new File(confPath));

      for (File project : inputDataProjects) {

        // Collect data expected for project
        final DataSetTest dstExpected =
            new DataSetTest(props, paramtersFiles, project,
                this.expectedAnalysisDirectory, isNewVersionEoulsan);

        final Map<String, DataSetAnalysis> setExpected = dstExpected.execute();

        // Collect data tested
        final DataSetTest dstTested =
            new DataSetTest(props, paramtersFiles, project,
                this.outputAnalysisDirectory);

        final Map<String, DataSetAnalysis> setTested = dstTested.execute();

        // TODO init map with the same size (from parameters list)
        // Check same count test for the data set
        if (setExpected.size() != setTested.size()) {
          throw new EoulsanException(
              "Different count test between expected directory and tested directory.");
        }

        // Comparison for each test
        for (String testName : setExpected.keySet()) {
          // Launch comparison
          comparator.compareDataSet(setExpected.get(testName),
              setTested.get(testName), project.getName(), testName);

        }
      }

      LOGGER.severe("Final report " + comparator.getReport());

      // TODO Auto-generated catch block
    } catch (IOException e) {
      e.printStackTrace();
    } catch (EoulsanException ee) {
      ee.printStackTrace();
    }

  }

  /**
   * Initialization properties with pa
   * @param conf configuration file from Action
   * @throws EoulsanException
   * @throws IOException
   */
  private void init(final File conf) throws EoulsanException, IOException {

    checkExistingFile(conf, " configuration file doesn't exist.");

    final BufferedReader br =
        new BufferedReader(newReader(conf,
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

    initLogger(this.props.getProperty("log_path"));

    // Initialization
    this.inputData =
        configureFileParameter("input_directory", " input data directory ")
            .iterator().next();

    final File[] outputDataDirectory =
        new File(this.props.getProperty("expected_analysis_directory"))
            .listFiles(new FileFilter() {

              @Override
              public boolean accept(File pathname) {
                return pathname.getName().startsWith(
                    props.getProperty("eoulsan_reference_version_git"));
              }
            });

    // Check outputData for reference Eoulsan version
    if (outputDataDirectory.length == 1) {
      // Doesn't exist, analysis must be launch
      this.expectedAnalysisDirectory = outputDataDirectory[0];

    } else if (outputDataDirectory.length == 0) {
      // New version Eoulsan to test, create data_expected analysis
      this.expectedAnalysisDirectory =
          new File(this.props.getProperty("expected_analysis_directory")
              + "/" + this.props.getProperty("eoulsan_reference_version_git")
              + "_" + DATE_FORMAT.format(new Date()));

      if (!expectedAnalysisDirectory.mkdirs())
        throw new IOException("Cannot create output analysis directory "
            + expectedAnalysisDirectory.getAbsolutePath());

      this.isNewVersionEoulsan = true;

      LOGGER.info("New version reference Eoulsan define in configuration test "
          + this.props.getProperty("eoulsan_reference_version_git"));

    } else {
      throw new EoulsanException(
          "Number data expected directory for version Eoulsan "
              + props.getProperty("eoulsan_reference_version_git")
              + " is invalid " + outputDataDirectory.length);
    }

    // Create new output test directory
    this.outputAnalysisDirectory =
        new File(this.props.getProperty("output_analysis_directory")
            + "/" + this.props.getProperty("eoulsan_test_version_git") + "_"
            + DATE_FORMAT.format(new Date()));

    if (!outputAnalysisDirectory.mkdirs())
      throw new IOException("Cannot create output analysis directory "
          + outputAnalysisDirectory.getAbsolutePath());

    Collection<File> parameterDirectories =
        configureFileParameter("parameters_directory", "parameters directory ");
    collectParametersFiles(parameterDirectories);

    collectInputDataDirectories();

    // Set not compare eoulsan.log
    comparator.setFilesToNotCompare("eoulsan.log");
  }

  private Collection<File> configureFileParameter(final String propertyName,
      final String exceptionMsg) throws EoulsanException, IOException {

    Collection<File> values = Sets.newHashSet();

    if (!props.containsKey(propertyName))
      throw new EoulsanException(exceptionMsg
          + " missing in configuration test.");

    for (String file : splitter.split(props.getProperty(propertyName))) {
      File f = new File(file);
      FileUtils.checkExistingFile(f, exceptionMsg + " doesn't exist");
      values.add(f);
    }

    return values;
  }

  private void collectInputDataDirectories() throws EoulsanException,
      IOException {

    for (String project : splitter.split(props.getProperty("input_data"))) {
      File projectDir = new File(inputData, project);

      checkExistingFile(
          projectDir,
          "Project directory doesn't exist in directory "
              + inputData.getAbsolutePath());

      this.inputDataProjects.add(projectDir);
    }

    if (this.inputDataProjects.size() == 0)
      throw new EoulsanException("None input data specified for test.");

  }

  private void collectParametersFiles(
      final Collection<File> parameterDirectories) throws EoulsanException {
    final String prefix = "param_";
    final String suffix = ".xml";

    // Collect all parameters files at the root of each directory
    for (File paramDir : parameterDirectories) {

      File[] params = paramDir.listFiles(new FileFilter() {

        @Override
        public boolean accept(File pathname) {
          return pathname.getName().startsWith(prefix)
              && pathname.getName().endsWith(suffix);
        }
      });

      String name;
      for (File param : params) {
        int beginIndex = prefix.length();
        int endIndex = param.getName().length() - suffix.length();

        name = param.getName().substring(beginIndex, endIndex);
        paramtersFiles.put(name, param);
      }
    }

    if (paramtersFiles.size() == 0)
      throw new EoulsanException("None parameters files defined here ");
  }

  //
  // Constructor
  //

  public ValidationAction() throws EoulsanException {
    props = new Properties();

    // Initialisation comparator
    this.comparator =
        new ComparatorDirectories(USE_SERIALIZATION, CHECKING_SAME_NAME);

    this.paramtersFiles = Maps.newHashMap();
    this.inputDataProjects = Sets.newHashSet();

  }

  //
  // Internal class
  //

  static class CommandLineEoulsan {

    /** Logger */
    private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

    private final String eoulsanExecutablePath;
    private final String designFile;
    private final String paramFile;

    private String logFilename = "eoulsan.log";
    private String errFilename = "eoulsan.err";
    private String outFilename = "eoulsan.out";

    // Parameters optional
    private String logLevel = "all";
    private String configurationFilePath = null;

    public List<String> buildCommandLine() {
      List<String> cmd = Lists.newArrayList();

      // cmd.add("screen");
      cmd.add(eoulsanExecutablePath + "/eoulsan.sh");
      // cmd.add("-help");

      cmd.add("-log");
      cmd.add(logFilename);
      cmd.add("-loglevel");
      cmd.add(logLevel);

      if (configurationFilePath != null) {
        cmd.add("-conf");
        cmd.add(configurationFilePath);
      }

      cmd.add("exec");
      cmd.add(paramFile);
      cmd.add(designFile);
      // cmd.add(">");
      // cmd.add(outFilename);
      // cmd.add("2>");
      // cmd.add(errFilename);

      LOGGER.info(StringUtils.join(cmd.toArray(), " "));
      // TODO
      System.out.println(StringUtils.join(cmd.toArray(), " "));

      return cmd;
    }

    //
    // Getter & setter
    //

    public String getLogFilename() {
      return logFilename;
    }

    public void setLogFilename(String logFilename) {
      this.logFilename = logFilename;
    }

    public String getLogLevel() {
      return logLevel;
    }

    public void setLogLevel(String logLevel) {
      this.logLevel = logLevel;
    }

    public String getErrFilename() {
      return errFilename;
    }

    public void setErrFilename(String errFilename) {
      this.errFilename = errFilename;
    }

    public String getOutFilename() {
      return outFilename;
    }

    public void setOutFilename(String outFilename) {
      this.outFilename = outFilename;
    }

    public String getConfigurationFilePath() {
      return configurationFilePath;
    }

    public void setConfigurationFilePath(String configurationFilePath) {
      this.configurationFilePath = configurationFilePath;
    }

    public String getEoulsanExecutablePath() {
      return eoulsanExecutablePath;
    }

    public String getDesignFile() {
      return designFile;
    }

    public String getParamFile() {
      return paramFile;
    }

    @Override
    public String toString() {
      return buildCommandLine().toString().replaceAll(",", " ");
    }

    //
    // Constructor
    //
    public CommandLineEoulsan(final String pathExecutable,
        final String designFile, final String paramFile) {

      // TODO
      // Check parameters
      this.eoulsanExecutablePath = pathExecutable;
      this.designFile = designFile;
      this.paramFile = paramFile;
    }

  }
}
