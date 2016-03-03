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

package fr.ens.biologie.genomique.eoulsan.util;

import java.util.Set;

/**
 * This class implements a reporter class like Counter class in Hadoop
 * framework.
 * @since 1.0
 * @author Laurent Jourdren
 */
public interface Reporter extends ReporterIncrementer {

  /**
   * Get the value of a counter.
   * @param counterGroup the group of the counter
   * @param counter the counter name
   * @return the value of the counter or -1 if the counter does not exists
   */
  long getCounterValue(String counterGroup, String counter);

  /**
   * Get a list of counter groups
   * @return a unmodifiable list of the counter groups
   */
  Set<String> getCounterGroups();

  /**
   * Get the names of the counter of a counter group.
   * @param counterGroup the group of the counter
   * @return a unmodifiable list of the name of the counter of the groups or
   *         empty list if counter doesn't exist
   */
  Set<String> getCounterNames(String counterGroup);

}
