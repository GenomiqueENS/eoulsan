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

package fr.ens.biologie.genomique.eoulsan.core;

import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import java.util.Set;

/**
 * This interface define a port of a step.
 *
 * @since 2.0
 * @author Laurent Jourdren
 */
public interface Port {

  /**
   * Get the name of the port.
   *
   * @return the name of the port
   */
  String getName();

  /**
   * Get the data format of the port.
   *
   * @return a DataFormat object
   */
  DataFormat getFormat();

  /**
   * Test if the port requires a list as value.
   *
   * @return true if the the port requires a list as value
   */
  boolean isList();

  /**
   * Get the steps linked to this port.
   *
   * @return a step with the list of linked steps
   */
  Set<Step> getLinkedSteps();
}
