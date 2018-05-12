package fr.ens.biologie.genomique.eoulsan.actions;

import static java.util.Collections.nCopies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import fr.ens.biologie.genomique.eoulsan.Settings;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;

/**
 * This class define an action that show the list of available formats.
 * @author Laurent Jourdren
 * @since 2.3
 */
public class FormatsAction extends AbstractInfoAction {

  /** Name of this action. */
  public static final String ACTION_NAME = "formats";

  @Override
  public String getName() {

    return ACTION_NAME;
  }

  @Override
  protected void showInfo(final Settings settings) {

    DataFormatRegistry registery = DataFormatRegistry.getInstance();

    StringBuilder sb = new StringBuilder();

    List<List<String>> result = new ArrayList<>();

    // Get information about formats
    for (DataFormat format : registery.getAllFormats()) {
      result.add(infoFormat(format));
    }

    // Sort formats by names
    Collections.sort(result, new Comparator<List<String>>() {

      @Override
      public int compare(List<String> o1, List<String> o2) {

        return o1.get(0).compareTo(o2.get(0));
      }

    });

    // Define the name of the columns
    List<String> columnNames = Arrays.asList("Name", "Aliases", "Extensions",
        "Prefix", "One file per analysis", "Description");

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

  private static List<String> infoFormat(final DataFormat format) {

    List<String> result = new ArrayList<>();

    // Add format name
    result.add(null2Empty(format.getName()));

    // Get format alias
    Set<String> aliases = new HashSet<>();
    if (format.getAlias() != null) {
      aliases.add(format.getAlias());
    }
    aliases.addAll(format.getGalaxyFormatNames());
    result.add(Joiner.on(", ").join(aliases));

    // Get format extensions
    List<String> extensions = new ArrayList<>();
    extensions.add(format.getDefaultExtension());
    for (String s : format.getExtensions()) {
      if (!extensions.contains(s)) {
        extensions.add(s);
      }
    }
    result.add(Joiner.on(", ").join(extensions));

    // Get "prefix" in output filenamme
    result.add(null2Empty(format.getPrefix()));

    // One file per analysis
    result.add("" + format.isOneFilePerAnalysis());

    // Get description
    result.add(null2Empty(format.getDescription()));

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
        result = new ArrayList<>(nCopies(v.size(), Integer.valueOf(-1)));
      }

      for (int j = 0; j < v.size(); j++) {
        result.set(j, Math.max(result.get(j), v.get(j).length()));
      }
    }

    return result;
  }

}
