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
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */
package fr.ens.transcriptome.eoulsan.actions;

import java.io.File;
import java.io.IOException;

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

import com.google.common.io.Files;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.it.ITFactory;

/**
 * This class launch integration test with Testng class.
 * @since 1.3
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
  public void action(String[] arguments) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    File testngReportDirectory = null;
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
        System.setProperty(ITFactory.IT_CONF_PATH_SYSTEM_KEY, line
            .getOptionValue("c").trim());
        argsOptions += 2;
      }

      if (line.hasOption("exec")) {

        // Path to application version
        System.setProperty(ITFactory.IT_APPLICATION_PATH_KEY_SYSTEM_KEY, line
            .getOptionValue("exec").trim());
        argsOptions += 2;
      }

      // Optional argument
      if (line.hasOption("f")) {

        // List all test to launch
        System.setProperty(ITFactory.IT_TEST_LIST_PATH_SYSTEM_KEY, line
            .getOptionValue("f").trim());
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
        if (s.toLowerCase(Globals.DEFAULT_LOCALE).equals("all"))
          System.setProperty(
              ITFactory.IT_GENERATE_ALL_EXPECTED_DATA_SYSTEM_KEY, "true");

        // Value equals new, regenerate expected directories doesn't exists
        else if (s.toLowerCase(Globals.DEFAULT_LOCALE).equals("new"))
          System.setProperty(
              ITFactory.IT_GENERATE_NEW_EXPECTED_DATA_SYSTEM_KEY, "true");

        argsOptions += 2;
      }

      // Optional argument
      if (line.hasOption("o")) {

        // List all test to launch
        testngReportDirectory = new File(line.getOptionValue("o").trim());

        if (!testngReportDirectory.exists())
          throw new ParseException(
              "Output testng report directory doesn't exists: "
                  + testngReportDirectory.getAbsolutePath());

        if (!testngReportDirectory.isDirectory())
          throw new ParseException(
              "Output testng report argument is not a directory: "
                  + testngReportDirectory.getAbsolutePath());

        argsOptions += 2;
      }

    } catch (ParseException e) {
      Common
          .errorExit(e, "Error while parse parameter file: " + e.getMessage());
    }

    if (arguments.length != argsOptions) {
      help(options);
    }

    // Execute program in local mode
    runIT(testngReportDirectory);
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
        .withDescription("configuration file").withLongOpt("conf").create('c'));

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
    options
        .addOption(OptionBuilder
            .withArgName("mode")
            .hasArg()
            .withDescription(
                "optional: mode for generate data expected: all (remove existing) or mode to generate no exists directory new")
            .create("expected"));

    // Optional, path to testng report directory
    options.addOption(OptionBuilder.withArgName("file").hasArg(true)
        .withDescription("testng report directory").create('o'));

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
        + " [options] configuration_file_path application_path", options);

    Common.exit(0);
  }

  //
  // Execution
  //

  /**
   * Run all integrated test
   * @param testngReportDirectory testng report directory, if it is null use the
   *          default directory
   */
  private void runIT(final File testngReportDirectory) {

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
    testng.setTestClasses(new Class[] {ITFactory.class});
    testng.addListener(tla);

    if (testngReportDirectory != null) {
      // Replace default output directory
      testng.setOutputDirectory(testngReportDirectory.getAbsolutePath());
    }

    // Launch integration tests using TestNG
    testng.run();

    // Make a copy testngReport in output test directory
    final File reportDirectory = new File(testng.getOutputDirectory());

    final File outputTestDirectory =
        new File(ITFactory.getOutputTestDirectoryPath());

    if (reportDirectory.exists() && outputTestDirectory.exists()) {
      // Build destination directory to copy testng report
      final File destinationDirectory =
          new File(outputTestDirectory, reportDirectory.getName());

      if (destinationDirectory.mkdir()) {
        // Copy testng directory
        try {
          Files.copy(reportDirectory, destinationDirectory);
        } catch (IOException e) {
        }
      }
    }
  }

}
