/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Utils {

  /**
   * Reverse a map
   * @param map Map to reverse
   * @return The reverse map
   */
  public static <K, V> Map<V, Set<K>> reverseMap(final Map<K, V> map) {

    if (map == null)
      return null;

    final Map<V, Set<K>> result = new HashMap<V, Set<K>>();

    for (Map.Entry<K, V> e : map.entrySet()) {

      final Set<K> set;

      final V value = e.getValue();

      if (!result.containsKey(value)) {
        set = new HashSet<K>();
        result.put(value, set);
      } else
        set = result.get(value);

      set.add(e.getKey());
    }

    return result;
  }

  /**
   * Create an unmodifiableSet from an array
   * @param array array with the values of the output Set
   * @return an unmodifiableSet with the values of the array or null if the
   *         array is null
   */
  public static <E> Set<E> unmodifiableSet(final E[] array) {

    if (array == null)
      return null;

    final List<E> list = Arrays.asList(array);

    return Collections.unmodifiableSet(new HashSet<E>(list));
  }

  /**
   * Return a list without null elements.
   * @param list input list
   * @return a list without null elements
   */
  public static <E> List<E> listWithoutNull(final List<E> list) {

    if (list == null)
      return null;

    final List<E> result = new ArrayList<E>();

    for (E e : list)
      if (e != null)
        result.add(e);

    return result;
  }

}
