package fr.ens.biologie.genomique.eoulsan.util;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Splitter;

/**
 * This class contains utility methods that are not provided by the bundled
 * version of Guava in Hadoop.
 * @author jourdren
 * @since 2.0
 */
public class GuavaCompatibility {

  /**
   * This method allow to split a charSequence into a list of String. This
   * method avoid using Splitter.splitToList() that is not available in the
   * embedded version of Guava in Hadoop. TODO Remove this method once the
   * embedded version of Guava will be changed
   * @param splitter the splitter
   * @param sequence the sequence to split
   * @return a list of String
   */
  public static List<String> splitToList(final Splitter splitter,
      final CharSequence sequence) {

    requireNonNull(splitter, "splitter argument cannot be null");
    requireNonNull(sequence, "sequence argument cannot be null");

    List<String> result = new ArrayList<>();

    for (String s : splitter.split(sequence)) {
      result.add(s);
    }

    return Collections.unmodifiableList(result);
  }

  /**
   * This method allow to split a string to a list. This method exists because
   * Guava 14 is bundled with Hadoop 2.x and the splitToList method exists only
   * since Guava 15.
   * @param splitter the splitter to use
   * @param s the string to split
   * @return an immutable list
   */
  private static List<String> splitToList(final Splitter splitter,
      final String s) {

    final Iterator<String> it = splitter.split(s).iterator();
    final List<String> result = new ArrayList<>();

    while (it.hasNext()) {
      result.add(it.next());
    }

    return Collections.unmodifiableList(result);
  }

}
