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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */
package fr.ens.biologie.genomique.eoulsan.actions;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;

import fr.ens.biologie.genomique.eoulsan.Common;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.it.ITFactory;
import fr.ens.biologie.genomique.eoulsan.it.ITSuite;
import fr.ens.biologie.genomique.kenetre.io.FileUtils;

/**
 * This class launch integration test with Testng class.
 * @since 2.0
 * @author Laurent Jourdren
 * @author Sandrine Perrin
 */
public class IntegrationTestAction extends AbstractAction {

  @Override
  public String getName() {
    return "it";
  }

  @Override
  public String getDescription() {
    return "integration test " + Globals.APP_NAME + " version.";
  }

  @Override
  public void action(final List<String> arguments) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    File testNGReportDirectory = null;
    File testOutputDirectory = null;
    int argsOptions = 0;

    try {

      // parse the command line arguments
      final CommandLine line =
          parser.parse(options, arguments.toArray(new String[0]), true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }

      if (line.hasOption("testconf")) {
        final String val = line.getOptionValue("testconf").trim();

        if (!(new File(val).exists() && new File(val).canRead())) {
          Common.errorExit(null,
              "Integration test configuration file doesn't exists");
        }

        // Configuration test files
        System.setProperty(ITFactory.IT_CONF_PATH_SYSTEM_KEY, val);
        argsOptions += 2;

      }

      if (line.hasOption("exec")) {

        // Path to application version
        System.setProperty(ITFactory.IT_APPLICATION_PATH_KEY_SYSTEM_KEY,
            line.getOptionValue("exec").trim());
        argsOptions += 2;
      }

      // Optional argument
      if (line.hasOption("f")) {

        // List all test to launch
        System.setProperty(ITFactory.IT_TEST_LIST_PATH_SYSTEM_KEY,
            line.getOptionValue("f").trim());
        argsOptions += 2;
      }

      // Optional argument
      if (line.hasOption("t")) {

        // Test to launch
        System.setProperty(ITFactory.IT_TEST_SYSTEM_KEY,
            line.getOptionValue("t").trim());
        argsOptions += 2;
      }

      // Optional argument
      if (line.hasOption("expected")) {
        final String s = line.getOptionValue("expected").trim();

        // Value equals all, regenerate all expected directories generated
        // automatically
        if (s.toLowerCase(Globals.DEFAULT_LOCALE).equals("all")) {
          System.setProperty(ITFactory.IT_GENERATE_ALL_EXPECTED_DATA_SYSTEM_KEY,
              "true");
        }
        // Value equals new, regenerate expected directories doesn't exists
        else if (s.toLowerCase(Globals.DEFAULT_LOCALE).equals("new")) {
          System.setProperty(ITFactory.IT_GENERATE_NEW_EXPECTED_DATA_SYSTEM_KEY,
              "true");
        }

        argsOptions += 2;
      }

      // Optional argument
      if (line.hasOption("d")) {
        // List all test to launch
        testOutputDirectory = new File(line.getOptionValue("d").trim());

        // Add property for test output directory
        System.setProperty(ITFactory.IT_OUTPUT_DIR_SYSTEM_KEY,
            testOutputDirectory.getAbsolutePath());

        argsOptions += 2;
      }

      if (line.hasOption("o")) {

        // List all test to launch
        testNGReportDirectory = new File(line.getOptionValue("o").trim());

        try {
          FileUtils.checkExistingDirectoryFile(testNGReportDirectory,
              "Output TestNG report");
        } catch (IOException e) {

          throw new ParseException(e.getMessage());
        }

        argsOptions += 2;
      }

    } catch (final ParseException e) {
      Common.errorExit(e,
          "Error while parse parameter file: " + e.getMessage());
    }

    if (argsOptions == 0 || arguments.size() != argsOptions) {
      help(options);
    }

    // Execute program in local mode
    runIT(testNGReportDirectory);
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
    options.addOption(OptionBuilder.withArgName("file").hasArg(true)
        .withDescription("configuration file").create("testconf"));

    // Path to application version to execute
    options.addOption(OptionBuilder.withArgName("appliPath").hasArg()
        .withDescription("application path to launch").create("exec"));

    // Optional, path to file with list name tests to treat
    options.addOption(OptionBuilder.withArgName("file").hasArg(true)
        .withDescription("optional: files with tests name to launch")
        .withLongOpt("file").create('f'));

    // Optional, the name of the test to execute
    options.addOption(OptionBuilder.withArgName("test").hasArg(true)
        .withDescription("optional: test name to launch").withLongOpt("test")
        .create('t'));

    // Optional, force generated expected data
    options.addOption(OptionBuilder.withArgName("mode").hasArg()
        .withDescription(
            "optional: mode for generate data expected: all (remove existing) or mode to generate no exists directory new")
        .create("expected"));

    // Optional, the test output directory
    options.addOption(OptionBuilder.withArgName("outputdir").hasArg(true)
        .withDescription("optional: test output directory").withLongOpt("dir")
        .create('d'));

    // Optional, path to TestNG report directory
    options.addOption(OptionBuilder.withArgName("file").hasArg(true)
        .withDescription("TestNG report directory").create('o'));

    return options;
  }

  /**
   * Show command line help.
   * @param options Options of the software
   */
  private void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(
        Globals.APP_NAME_LOWER_CASE + ".sh " + getName() + " [options]",
        options);

    Common.exit(0);
  }

  //
  // Execution
  //

  /**
   * Run all integrated test.
   * @param testNGReportDirectory TestNG report directory, if it is null use the
   *          default directory
   */
  private void runIT(final File testNGReportDirectory) {

    // Define a listener that print information about the results of the
    // integration tests
    final TestListenerAdapter tla = new TestListenerAdapter() {

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

    final TestNG testng = new TestNG();
    try {
      // Create and configure TestNG
      testng.setTestClasses(new Class[] {ITFactory.class});
      testng.addListener(tla);

      if (testNGReportDirectory != null) {
        // Replace default output directory
        testng.setOutputDirectory(testNGReportDirectory.getAbsolutePath());
      }

    } catch (final Throwable e) {
      Common.errorExit(e,
          "Integration test can not be initialized the test factory.");
    }

    // Launch integration tests using TestNG
    testng.run();

    // Make a copy testNGReport in output test directory
    final File reportDirectory = new File(testng.getOutputDirectory());

    final File outputTestDirectory =
        new File(ITSuite.getInstance().getOutputTestDirectoryPath());

    if (reportDirectory.exists() && outputTestDirectory.exists()) {
      // Build destination directory to copy TestNG report
      final File destinationDirectory =
          new File(outputTestDirectory, reportDirectory.getName());

      // Copy TestNG directory
      // try {
      // FileUtils.copyDirectory(reportDirectory, destinationDirectory);
      //
      // } catch (final IOException e) {
      // // Nothing to do
      // }
    }
  }

}
