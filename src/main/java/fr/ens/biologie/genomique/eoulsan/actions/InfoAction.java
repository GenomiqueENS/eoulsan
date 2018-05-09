package fr.ens.biologie.genomique.eoulsan.actions;

import static fr.ens.biologie.genomique.eoulsan.Globals.APP_NAME;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.python.google.common.base.Strings;

import fr.ens.biologie.genomique.eoulsan.Common;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.Infos;
import fr.ens.biologie.genomique.eoulsan.Infos.Info;
import fr.ens.biologie.genomique.eoulsan.Main;
import fr.ens.biologie.genomique.eoulsan.Settings;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.workflow.CommandWorkflowParser;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;

/**
 * This class define an action that show the Eoulsan configuration
 * @author Laurent Jourdren
 * @since 2.3
 */
public class InfoAction extends AbstractAction {

  /** Name of this action. */
  public static final String ACTION_NAME = "info";

  @Override
  public String getName() {

    return ACTION_NAME;
  }

  @Override
  public String getDescription() {

    return "Get information about " + APP_NAME + " configuration.";
  }

  @Override
  public void action(final List<String> arguments) {

    // TODO must handle the workflow file as an argument

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    try {

      // parse the command line arguments
      final CommandLine line = parser.parse(options,
          arguments.toArray(new String[arguments.size()]), true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }
    } catch (ParseException e) {
      Common.errorExit(e,
          "Error while parsing command line arguments: " + e.getMessage());
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
        Common.errorExit(e,
            "Error while reading workflow file: " + e.getMessage());
      } catch (EoulsanException e) {
        Common.errorExit(e,
            "Error while parsing workflow file: " + e.getMessage());
      }
    }

    // Show the informations
    showInfo(settings);
  }

  /**
   * Show the information on stdout
   * @param settings the Eoulsan settings
   */
  private void showInfo(final Settings settings) {

    // Map for all the sections
    Map<String, List<Info>> sections = new LinkedHashMap<>();

    // Software section
    sections.put("Software", Infos.softwareInfos(Main.getInstance()));

    // Command line
    sections.put("Command line", Infos.commandLineInfo(Main.getInstance()));

    // General configuration
    sections.put("General configuration", Infos.generalConf(settings));

    // Modules and formats
    sections.put("Modules and formats", Infos.modulesAndFormatsInfo(settings));

    // Modules and formats
    sections.put("Storages", Infos.storageInfo(settings));

    // Cluster configuration
    sections.put("Cluster configuration", Infos.clusterInfo(settings));

    // Cluster configuration
    sections.put("Cloud configuration", Infos.cloudInfo(settings));

    // Docker configuration
    sections.put("Docker", Infos.dockerInfo(settings));

    // R and RServe configuration
    sections.put("R and RServe", Infos.rAndRserveInfo(settings));

    // Email configuration
    sections.put("Email", Infos.mailInfo(settings));

    // System configuration
    sections.put("System configuration", Infos.systemInfos());

    // CPU information
    sections.put("CPU", Infos.cpuInfo());

    // Memory information
    sections.put("Memory", Infos.memInfo());

    // Partition information
    sections.put("Partitions", Infos.partitionInfo(settings));

    System.out.println();
    System.out.print(infoToString(sections));
  }

  /**
   * Convert a map of list of Info into a String.
   * @param sections the section to convert
   * @return a String with a the description of the configuration
   */
  private static String infoToString(final Map<String, List<Info>> sections) {

    final StringBuilder sb = new StringBuilder();

    int maxLengthKey = maxLengthKey(sections) + 3;

    for (Map.Entry<String, List<Info>> e : sections.entrySet()) {

      sb.append(e.getKey());
      sb.append(':');
      sb.append('\n');

      for (Info i : e.getValue()) {

        sb.append("    ");
        sb.append(Strings.padEnd(i.getName() + ':', maxLengthKey, ' '));

        boolean first = true;

        for (String v : i.getValues()) {

          if (first) {
            first = false;
          } else {
            sb.append("    ");
            sb.append(Strings.padEnd("", maxLengthKey, ' '));
          }
          sb.append(v);
          sb.append('\n');
        }
        if (i.getValues().isEmpty()) {
          sb.append('\n');
        }
      }
      sb.append('\n');
    }

    return sb.toString();
  }

  /**
   * Get the maximal length of the key of the Info objects.
   * @param infos the info object
   * @return the maximal length of the key of the Info objects
   */
  private static int maxLengthKey(List<Info> infos) {

    int result = -1;

    for (Info i : infos) {
      result = Math.max(result, i.getName().length());
    }

    return result;
  }

  /**
   * Get the maximal length of the key of the Info objects in a map.
   * @param infos the info object
   * @return the maximal length of the key of the Info objects
   */
  private static int maxLengthKey(Map<String, List<Info>> sections) {

    int result = -1;

    for (List<Info> s : sections.values()) {
      result = Math.max(result, maxLengthKey(s));
    }

    return result;
  }

  //
  // Command line parsing
  //

  /**
   * Create options for command line
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
   * @param options Options of the software
   */
  private static void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE
        + ".sh " + ACTION_NAME + " [options] [workflowfile]", options);

    Common.exit(0);
  }

}
