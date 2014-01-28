package fr.ens.transcriptome.eoulsan.actions;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.data.DataSetAnalysis;
import fr.ens.transcriptome.eoulsan.io.ComparatorDirectories;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class ValidationAction extends AbstractAction {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final List<String> DATASETS_DEFAULT = Lists.newArrayList("",
      "");

  private static final boolean USE_SERIALIZATION = true;
  private static final boolean CHECKING_SAME_NAME = true;

  @Override
  public String getName() {
    return "validation";
  }

  @Override
  public String getDescription() {
    return "test " + Globals.APP_NAME + " version.";
  }

  private void initLogger() {
    
    Handler fh = null;
    try {
      fh = new FileHandler("eoulsan_validation.log");
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    fh.setFormatter(Globals.LOG_FORMATTER);
    
    LOGGER.setLevel(Globals.LOG_LEVEL);
    LOGGER.setUseParentHandlers(false);

    LOGGER.addHandler(fh);
  }

  @Override
  public void action(String[] arguments) {

    initLogger();

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    String pathEoulsanNewVersion = null;
    String listDatasets = null;
    String outputDirectory = null;

    String jobDescription = null;

    int argsOptions = 0;

    try {

      // parse the command line arguments
      final CommandLine line = parser.parse(options, arguments, true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }

      if (line.hasOption("d")) {

        jobDescription = line.getOptionValue("d");
        argsOptions += 2;
      }

      if (line.hasOption("p")) {

        // Retrieve path to Eoulsan
        pathEoulsanNewVersion = line.getOptionValue("p").trim();
        argsOptions += 2;
      }

      if (line.hasOption("s")) {
        // Retrieve list dataset for Eoulsan
        listDatasets = line.getOptionValue("s");
        argsOptions += 2;
      }

      if (line.hasOption("o")) {
        // Retrieve path of output directory
        outputDirectory = line.getOptionValue("o");
        argsOptions += 2;
      }

    } catch (ParseException e) {
      Common.errorExit(e,
          "Error while parsing parameter file: " + e.getMessage());
    }

    if (arguments.length != argsOptions + 2) {
      help(options);
    }

    // Execute program in local mode
    run(pathEoulsanNewVersion, listDatasets, outputDirectory, jobDescription);
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
    options.addOption(OptionBuilder.withArgName("eoulsanPath").hasArg(true)
        .withDescription("path to test Eoulsan version").withLongOpt("path")
        .create('p'));

    // Dataset(s) for Eoulsan
    options.addOption(OptionBuilder.withArgName("dataset").hasArg(true)
        .withDescription("dataset for Eoulsan").withLongOpt("dataset")
        .create('s'));

    // Output option
    options.addOption(OptionBuilder.withArgName("directory").hasArg()
        .withDescription("Output dir").withLongOpt("output").create('o'));

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
  public/* private */void run(final String pathEoulsanNewVersion,
      final String listDatasets, final String pathOutputDirectory,
      final String jobDescription) {

    checkNotNull(pathEoulsanNewVersion, "paramFile is null");
    checkNotNull(listDatasets, "designFile is null");
    checkNotNull(pathOutputDirectory, "designFile is null");

    final String desc;

    if (jobDescription == null) {
      desc = "no job description";
    } else {
      desc = jobDescription.trim();
    }

    LOGGER.info(Globals.WELCOME_MSG + " Local mode.");

    File eoulsanExecutable = new File(pathEoulsanNewVersion);
    File outputDirectory = new File(pathOutputDirectory);

    try {
      // Check path Eoulsan new version
      checkExistingFile(eoulsanExecutable, "Executable Eoulsan doesn't exist");

      // Check output directory doesn't exist
      if (!(outputDirectory.exists() && outputDirectory.isDirectory())) {
        LOGGER.severe("Analysis can't be launch, output directory "
            + pathOutputDirectory + " doesn't exists");
        throw new EoulsanException("Output directory "
            + pathOutputDirectory + " doesn't exists");
      }

      // Set dataset
      String trimmed = listDatasets.trim();

      if (trimmed == null || trimmed.length() == 0)
        throw new EoulsanException("Dataset is empty.");

      final Splitter s = Splitter.on(',').trimResults().omitEmptyStrings();
      List<String> datasets = Lists.newArrayList(s.split(trimmed));

      // Check free space on disk is enough for analysis
      // final long freeSpaceOutputDisk = outputDirectory.getFreeSpace();
      // final long neededSpaceOneAnalysis = new File(datasets.get(0)).length();
      //
      // if (neededSpaceOneAnalysis > (freeSpaceOutputDisk * 0.95)) {
      // throw new EoulsanException("Not enough free space on disk.");
      // }

      for (String dataset : datasets) {
        // Build data set expected
        final DataSetAnalysis datasetExpected =
            new DataSetAnalysis(dataset, true);

        // Init data set tested
        final DataSetAnalysis datasetTested =
            new DataSetAnalysis(pathOutputDirectory
                + "/test_" + datasetExpected.getRootPath(), false);

        datasetTested.buildDirectoryAnalysis(datasetExpected);

        // Launch Eoulsan
        CommandLineEoulsan cmd =
            new CommandLineEoulsan(pathEoulsanNewVersion, datasetTested
                .getDesignFile().toFile().getName(), datasetTested
                .getParamFile().toFile().getName());

        // TODO
        // Launch eoulsan
        final int exitValue =
            ProcessUtils.sh(cmd.buildCommandLine(),
                new File(datasetTested.getDataSetPath()));

        // Check exitvalue
        if (exitValue != 0) {
          throw new IOException("Bad error result for dataset "
              + datasetTested.getDataSetPath() + " execution: " + exitValue);
        }

        datasetTested.init();
        if (datasetTested.getDataFileSameName(".wiki", "summary.wiki") == null)
          LOGGER.severe("Fail Eoulsan analysis");
        else {
          // Compare two directory
          ComparatorDirectories comparator =
              new ComparatorDirectories(USE_SERIALIZATION, CHECKING_SAME_NAME);

          // Launch comparison
          comparator.compareDataSet(datasetExpected, datasetTested);

          // Set not compare eoulsan.log
          comparator.setFilesToNotCompare(cmd.getLogFilename());

          comparator.computeReport();
        }
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (EoulsanException ee) {
      ee.printStackTrace();
    }

  }

  //
  //
  //

  public ValidationAction() {
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
