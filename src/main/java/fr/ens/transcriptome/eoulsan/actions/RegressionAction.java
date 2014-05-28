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

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.it.RegressionITFactory;

/**
 * This class launch integration test with Testng class.
 * @since 1.3
 * @author Laurent Jourdren
 * @author Sandrine Perrin
 */
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
        System.setProperty(RegressionITFactory.CONF_PATH_KEY, line
            .getOptionValue("c").trim());
        argsOptions += 2;
      }

      if (line.hasOption("exec")) {

        // Path to application version
        System.setProperty(RegressionITFactory.APPLICATION_PATH_KEY, line
            .getOptionValue("exec").trim());
        argsOptions += 2;
      }

      // Optional argument
      if (line.hasOption("f")) {

        // List all test to launch
        System.setProperty(RegressionITFactory.TESTS_FILE_PATH_KEY, line
            .getOptionValue("f").trim());
        argsOptions += 2;
      }

      // Optional argument
      if (line.hasOption("expected")) {
        final String s = line.getOptionValue("expected").trim();

        // Value equals all, regenerate all expected directories generated
        // automatically
        if (s.toLowerCase(Globals.DEFAULT_LOCALE).equals("all"))
          System.setProperty(
              RegressionITFactory.GENERATE_ALL_EXPECTED_DATA_KEY, "true");

        // Value equals new, regenerate expected directories doesn't exists
        else if (s.toLowerCase(Globals.DEFAULT_LOCALE).equals("new"))
          System.setProperty(
              RegressionITFactory.GENERATE_NEW_EXPECTED_DATA_KEY, "true");

        argsOptions += 2;
      }

    } catch (ParseException e) {
      Common.errorExit(e,
          "Error while parse parameter file: " + e.getMessage());
    }

    if (arguments.length != argsOptions) {
      help(options);
    }

    // Execute program in local mode
    runIT();
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

    // Optional, force generated expected data
    options
        .addOption(OptionBuilder
            .withArgName("mode")
            .hasArg()
            .withDescription(
                "optional: mode for generate data expected: all (remove existing) or mode to generate no exists directory new")
            .create("expected"));

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
   */
  private void runIT() {

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
    testng.setTestClasses(new Class[] {RegressionITFactory.class});
    testng.addListener(tla);

    // Launch integration tests using TestNG
    testng.run();
  }

}
