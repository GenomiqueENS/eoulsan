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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Utils {

  /**
   * Reverse a map
   * @param map Map to reverse
   * @return The reverse map
   */
  public static Map<String, Set<String>> reverseMap(Map<String, String> map) {

    if (map == null)
      return null;

    Map<String, Set<String>> result = new HashMap<String, Set<String>>();

    for (Map.Entry<String, String> e : map.entrySet()) {

      final Set<String> set;

      final String value = e.getValue();

      if (!result.containsValue(value)) {
        set = new HashSet<String>();
        result.put(value, set);
      } else
        set = result.get(value);

      set.add(e.getKey());
    }

    return result;
  }

}
