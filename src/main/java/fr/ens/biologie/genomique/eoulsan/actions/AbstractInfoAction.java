package fr.ens.biologie.genomique.eoulsan.actions;

import fr.ens.biologie.genomique.eoulsan.Common;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.Settings;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.workflow.CommandWorkflowParser;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import java.io.IOException;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;

/**
 * This class define an abstract action that show the some Eoulsan configuration and available
 * modules
 *
 * @author Laurent Jourdren
 * @since 2.3
 */
public abstract class AbstractInfoAction extends AbstractAction {

  @Override
  public String getDescription() {

    return "show information about " + getName() + " configuration.";
  }

  @Override
  public void action(final List<String> arguments) {

    final Options options = makeOptions();
    final CommandLineParser parser = new DefaultParser();

    try {

      // parse the command line arguments
      final CommandLine line = parser.parse(options, arguments.toArray(new String[0]), true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }
    } catch (ParseException e) {
      Common.errorExit(e, "Error while parsing command line arguments: " + e.getMessage());
    }

    // Get the settings
    Settings settings = EoulsanRuntime.getSettings();

    final DataFile workflowFile;

    switch (arguments.size()) {
      case 0:
        workflowFile = null;
        break;

      case 1:
        workflowFile = new DataFile(arguments.get(0));
        break;

      default:
        help(options);
        workflowFile = null;
        break;
    }

    if (workflowFile != null) {
      try {
        CommandWorkflowParser cwp = new CommandWorkflowParser(workflowFile);
        for (Parameter p : cwp.parse().getGlobalParameters()) {
          settings.setSetting(p.getName(), p.getStringValue());
        }

      } catch (IOException e) {
        Common.errorExit(e, "Error while reading workflow file: " + e.getMessage());
      } catch (EoulsanException e) {
        Common.errorExit(e, "Error while parsing workflow file: " + e.getMessage());
      }
    }

    // Show the informations
    showInfo(settings);
  }

  /**
   * Show the information on stdout
   *
   * @param settings the Eoulsan settings
   */
  protected abstract void showInfo(final Settings settings);

  //
  // Command line parsing
  //

  /**
   * Create options for command line
   *
   * @return an Options object
   */
  @SuppressWarnings("static-access")
  private static Options makeOptions() {

    // create Options object
    final Options options = new Options();

    // Help option
    options.addOption("h", "help", false, "Display this help");

    return options;
  }

  /**
   * Show command line help.
   *
   * @param options Options of the software
   */
  private void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = HelpFormatter.builder().setShowSince(false).get();
    try {
      formatter.printHelp(
          Globals.APP_NAME_LOWER_CASE + ".sh " + getName() + " [options] [workflowfile]",
          "",
          options,
          "",
          false);
    } catch (IOException e) {
      Common.errorExit(e, "Error while creating help message.");
    }

    Common.exit(0);
  }
}
