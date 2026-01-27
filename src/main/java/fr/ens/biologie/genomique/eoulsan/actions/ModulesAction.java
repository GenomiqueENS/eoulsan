package fr.ens.biologie.genomique.eoulsan.actions;

import static java.util.Collections.nCopies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Strings;

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.Settings;
import fr.ens.biologie.genomique.eoulsan.annotations.EoulsanAnnotationUtils;
import fr.ens.biologie.genomique.eoulsan.annotations.ExecutionMode;
import fr.ens.biologie.genomique.eoulsan.core.Module;
import fr.ens.biologie.genomique.eoulsan.core.workflow.ModuleRegistry;
import fr.ens.biologie.genomique.eoulsan.modules.GalaxyToolModule;
import fr.ens.biologie.genomique.kenetre.util.Version;

/**
 * This class define an action that show the list of available formats.
 * @author Laurent Jourdren
 * @since 2.3
 */
public class ModulesAction extends AbstractInfoAction {

  /** Name of this action. */
  public static final String ACTION_NAME = "modules";

  @Override
  public String getName() {

    return ACTION_NAME;
  }

  @Override
  protected void showInfo(Settings settings) {
    // TODO Auto-generated method stub

    ModuleRegistry registery = ModuleRegistry.getInstance();

    StringBuilder sb = new StringBuilder();

    List<List<String>> result = new ArrayList<>();

    // Get information about formats
    for (Module module : registery.getAllModules()) {

      if (!module.getName().startsWith("_")) {
        result.add(infoModule(module));
      }
    }

    // Sort formats by names
    result.sort(Comparator.comparing(o -> o.get(0)));

    // Define the name of the columns
    List<String> columnNames = Arrays.asList("Name", "Version", "Type",
        "Generator", "Execution mode", "Description");

    // Get the maximal length of each column
    List<Integer> maxLengths = maxLengthKey(columnNames, result);

    // Print header
    for (int i = 0; i < columnNames.size(); i++) {

      sb.append(Strings.padEnd(columnNames.get(i), maxLengths.get(i) + 2, ' '));
    }
    sb.append('\n');

    // Print values
    for (List<String> info : result) {

      for (int i = 0; i < info.size(); i++) {

        sb.append(Strings.padEnd(info.get(i), maxLengths.get(i) + 2, ' '));
      }
      sb.append('\n');

    }

    System.out.println();
    System.out.print(sb.toString());

  }

  private static List<String> infoModule(final Module module) {

    List<String> result = new ArrayList<>();

    // Add Module name
    result.add(null2Empty(module.getName()));

    // Add version
    Version v = module.getVersion();
    result.add(v == null ? "" : v.toString());

    // Add Type
    if (module instanceof GalaxyToolModule) {
      result.add("galaxy tool");
    } else {
      result.add("java");
    }

    // Generator
    result.add("" + EoulsanAnnotationUtils.isGenerator(module));

    // Add Execution mode
    ExecutionMode mode = ExecutionMode.getExecutionMode(module.getClass());
    result.add(mode.toString().replace('_', ' ').toLowerCase(Globals.DEFAULT_LOCALE));

    // Get description
    result.add(null2Empty(module.getDescription()));

    return result;
  }

  /**
   * Convert null values to empty values.
   * @param s String to test
   * @return a non null string
   */
  private static String null2Empty(final String s) {

    if (s == null) {
      return "";
    }

    return s;
  }

  /**
   * Get the maximal length of the key of the Info objects.
   * @param values the info object
   * @return the maximal length of the key of the Info objects
   */
  private static List<Integer> maxLengthKey(List<String> columnNames,
      List<List<String>> values) {

    List<Integer> result = null;

    List<List<String>> vals = new ArrayList<>(values);
    vals.add(columnNames);

    for (List<String> v : vals) {

      if (result == null) {
        result = new ArrayList<>(nCopies(v.size(), -1));
      }

      for (int j = 0; j < v.size(); j++) {
        result.set(j, Math.max(result.get(j), v.get(j).length()));
      }
    }

    return result;
  }

}
