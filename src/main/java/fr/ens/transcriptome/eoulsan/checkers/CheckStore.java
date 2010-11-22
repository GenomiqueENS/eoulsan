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

package fr.ens.transcriptome.eoulsan.checkers;

import java.util.HashMap;
import java.util.Map;

/**
 * This class define a storage where some results of the checker can be save for
 * later reuse by other checkers.
 * @author Laurent Jourdren
 */
public class CheckStore {

  private Map<String, Object> info = new HashMap<String, Object>();

  /**
   * Store some data.
   * @param key key of the data
   * @param value the data to store
   */
  public void add(final String key, Object value) {

    if (key == null || value == null)
      return;

    this.info.put(key, value);
  }

  /**
   * Get some data.
   * @param key key of the data to retrieve
   * @return data if stored
   */
  public Object get(final String key) {

    return this.info.get(key);
  }

}
