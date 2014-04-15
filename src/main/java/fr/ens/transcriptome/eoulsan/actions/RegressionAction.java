package fr.ens.transcriptome.eoulsan.actions;

import static com.google.common.io.Files.newReader;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.createSymbolicLink;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
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
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.it.DataSetTest;
import fr.ens.transcriptome.eoulsan.it.EoulsanITFactory;
import fr.ens.transcriptome.eoulsan.it.ITActionFactory;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class RegressionAction extends AbstractAction {

  @Override
  public String getName() {
    return "regression";
  }

  @Override
  public String getDescription() {
    return "test " + Globals.APP_NAME + " version.";
  }

  @Override
  public void action(String[] arguments) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    // File confPath = null;
    // String applicationPath = null;

    // Optional, file
    // File fileListAllTests = null;
    // boolean regenerateAllExpectedData = false;
    // boolean isCheckingExpectedDirAction = false;

    int argsOptions = 0;

    try {

      // parse the command line arguments
      final CommandLine line = parser.parse(options, arguments, true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }

      if (line.hasOption("c")) {

        // Configuration test files
        System.setProperty(ITActionFactory.CONF_PATH_KEY,
            line.getOptionValue("c").trim());
        argsOptions += 2;
      }

      if (line.hasOption("exec")) {

        // Path to application version
        System.setProperty(ITActionFactory.APPLI_PATH_KEY,
            line.getOptionValue("exec").trim());
        argsOptions += 2;
      }

      // Optional argument
      if (line.hasOption("f")) {

        // List all test to launch
        System.setProperty(ITActionFactory.TESTS_FILE_PATH_KEY, line
            .getOptionValue("f").trim());
        argsOptions += 2;
      }

      // Optional argument
      // TODO option name
      if (line.hasOption("generate")) {
        final String s = line.getOptionValue("generate").trim();

        // Value equals all, regenerate all expected directories generated
        // automatically
        if (s.toLowerCase(Globals.DEFAULT_LOCALE).equals("all"))
          System.setProperty(ITActionFactory.GENERATE_ALL_EXPECTED_DATA_KEY,
              "true");

        // Value equals new, regenerate expected directories doesn't exists
        else if (s.toLowerCase(Globals.DEFAULT_LOCALE).equals("new"))
          System.setProperty(ITActionFactory.GENERATE_NEW_EXPECTED_DATA_KEY,
              "true");

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
    run();
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

    // Path to test configuration
    options.addOption(OptionBuilder.withArgName("confPath").hasArg(true)
        .withDescription("configuration test file").withLongOpt("conf")
        .create('c'));

    // Path to application version to execute
    options.addOption(OptionBuilder.withArgName("exec").hasArg()
        .withDescription("path application version").create("exec"));

    // Optional, path to file with list name tests to treat
    options.addOption(OptionBuilder.withArgName("fileTest").hasArg(true)
        .withDescription("path to file with list name tests")
        .withLongOpt("file").create('f'));

    // Optional, force generated expected data
    options.addOption(OptionBuilder.withArgName("type").hasArg()
        .withDescription("mode generate data expected").create("generate"));

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
        + ".sh " + getName() + " [options] configuration_tests_path", options);

    Common.exit(0);
  }

  //
  // Execution
  //

  /**
   */
  private void run() {

    // Define a listener that print information about the results of the
    // integration tests
    TestListenerAdapter tla = new TestListenerAdapter() {

      @Override
      public void onTestSuccess(final ITestResult tr) {

        super.onTestSuccess(tr);
        System.out.println(tr);
      }

      @Override
      public void onTestFailure(final ITestResult tr) {

        super.onTestFailure(tr);
        System.err.println(tr);
        System.err.println(tr.getThrowable().getMessage());
      }

    };

    // Create and configure TestNG
    TestNG testng = new TestNG();
    testng.setTestClasses(new Class[] {ITActionFactory.class});
    testng.addListener(tla);

    // Launch integration tests using TestNG
    testng.run();
  }

  //
  // Constructor
  //

  public RegressionAction() {
  }
}
