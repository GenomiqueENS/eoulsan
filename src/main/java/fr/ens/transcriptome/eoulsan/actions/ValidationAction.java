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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.data.DataSetAnalysis;
import fr.ens.transcriptome.eoulsan.data.DataSetTest;
import fr.ens.transcriptome.eoulsan.io.ComparatorDirectories;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

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
  private File outputTestsDirectory;
  private File testsData;

  private final Map<String, DataSetTest> tests;

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
  private void run(final String confPath, final String jobDescription) {

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

      LOGGER.info("Number tests defined " + this.tests.size());

      for (Map.Entry<String, DataSetTest> test : this.tests.entrySet()) {
        final String testName = test.getKey();
        final DataSetTest dsa = test.getValue();

        LOGGER.info("Execute test " + testName);
        dsa.executeTest();

        // Collect data expected for project
        final DataSetAnalysis setExpected = dsa.getAnalysisExcepted();

        // Collect data tested
        final DataSetAnalysis setTested = dsa.getAnalysisTest();

        // Launch comparison
        comparator.compareDataSet(setExpected, setTested, testName);

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

    this.testsData =
        configureFileParameter("tests_directory", " tests data directory ")
            .iterator().next();

    final File output =
        configureFileParameter("output_analysis_directory",
            " output data directory ").iterator().next();

    this.outputTestsDirectory =
        new File(output, props.getProperty("eoulsan_test_version_git")
            + "_" + DATE_FORMAT.format(new Date()));

    if (!outputTestsDirectory.mkdir())
      throw new EoulsanException("Cannot create output tests directory "
          + outputTestsDirectory.getAbsolutePath());

    collectTests();

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

  private void collectTests() throws EoulsanException, IOException {
    final String prefix = "test";
    final String suffix = ".txt";

    // Parsing all directories test
    for (File dir : this.testsData.listFiles()) {

      // Collect test description file
      final File[] files = dir.listFiles(new FileFilter() {

        @Override
        public boolean accept(File pathname) {
          return pathname.getName().startsWith(prefix)
              && pathname.getName().endsWith(suffix);
        }
      });

      if (files != null && files.length == 1) {
        //
        final DataSetTest dst =
            new DataSetTest(props, files[0], dir, this.outputTestsDirectory);
        this.tests.put(dir.getName(), dst);
      }

    }

    if (this.tests.size() == 0)
      throw new EoulsanException("None test specified in "
          + this.testsData.getAbsolutePath());
  }

  //
  // Constructor
  //

  public ValidationAction() throws EoulsanException {
    props = new Properties();

    // Initialization comparator
    this.comparator =
        new ComparatorDirectories(USE_SERIALIZATION, CHECKING_SAME_NAME);

    this.tests = Maps.newHashMap();

  }
}
