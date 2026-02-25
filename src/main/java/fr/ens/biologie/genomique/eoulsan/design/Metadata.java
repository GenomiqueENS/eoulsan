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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.design;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This interface defines common methods for metadata.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface Metadata {

  /**
   * Get the value according the key.
   *
   * @param key the key
   * @return the value
   */
  String get(String key);

  /**
   * Get the trimmed value according the key.
   *
   * @param key the key
   * @return the value
   */
  String getTrimmed(String key);

  /**
   * Set the value according the key.
   *
   * @param key the key
   * @param value the value
   */
  void set(String key, String value);

  /**
   * Set the value as a list according the key.
   *
   * @param key the key
   * @param value the value as a list
   */
  void set(String key, List<String> value);

  /**
   * Get the number of metadata.
   *
   * @return the number of metadata
   */
  int size();

  /**
   * Test if there is no metadata.
   *
   * @return true if there is no metadata
   */
  boolean isEmpty();

  /**
   * Test if the key is in md.
   *
   * @param key the key
   * @return true if the key is in md
   */
  boolean contains(String key);

  /**
   * Get the value according the key as a list.
   *
   * @param key the key
   * @return the value as a list
   */
  List<String> getAsList(String key);

  /**
   * Get the value according the key as a boolean.
   *
   * @param key the key
   * @return the value as a boolean
   */
  boolean getAsBoolean(String key);

  /**
   * Get the keys of the metadata
   *
   * @return a set with the keys of the metadata
   */
  Set<String> keySet();

  /**
   * Get an entry set of the metadata.
   *
   * @return a set of entries
   */
  Set<Map.Entry<String, String>> entrySet();

  /**
   * Remove the value according the key.
   *
   * @param key the key
   */
  void remove(String key);
}
