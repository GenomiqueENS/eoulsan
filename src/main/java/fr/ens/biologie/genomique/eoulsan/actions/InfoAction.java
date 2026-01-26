package fr.ens.biologie.genomique.eoulsan.actions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;

import fr.ens.biologie.genomique.eoulsan.Infos;
import fr.ens.biologie.genomique.eoulsan.Infos.Info;
import fr.ens.biologie.genomique.eoulsan.Main;
import fr.ens.biologie.genomique.eoulsan.Settings;

/**
 * This class define an action that show the Eoulsan configuration
 * @author Laurent Jourdren
 * @since 2.3
 */
public class InfoAction extends AbstractInfoAction {

  /** Name of this action. */
  public static final String ACTION_NAME = "info";

  @Override
  public String getName() {

    return ACTION_NAME;
  }

  /**
   * Show the information on stdout
   * @param settings the Eoulsan settings
   */
  @Override
  protected void showInfo(final Settings settings) {

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
   * @param sections info sections
   * @return the maximal length of the key of the Info objects
   */
  private static int maxLengthKey(Map<String, List<Info>> sections) {

    int result = -1;

    for (List<Info> s : sections.values()) {
      result = Math.max(result, maxLengthKey(s));
    }

    return result;
  }

}
