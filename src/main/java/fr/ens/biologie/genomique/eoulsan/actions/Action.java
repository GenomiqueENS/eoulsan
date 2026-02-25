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

package fr.ens.biologie.genomique.eoulsan.actions;

import java.util.List;

/**
 * This interface define an action.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public interface Action {

  /**
   * Get the name of the action.
   *
   * @return the name of the action
   */
  String getName();

  /**
   * Get action description.
   *
   * @return the description description
   */
  String getDescription();

  /**
   * Execute action.
   *
   * @param arguments arguments of the action.
   */
  void action(List<String> arguments);

  /**
   * Test if the action can be executed in hadoop Jar mode.
   *
   * @return true if the action can be executed in hadoop Jar mode
   */
  boolean isHadoopJarMode();

  /**
   * Test if the action can be executed with current platform.
   *
   * @return true if the action can be executed with current platform
   */
  boolean isCurrentArchCompatible();

  /**
   * Test if the action must be hidden from the list of available actions.
   *
   * @return true if the action must be hidden
   */
  boolean isHidden();
}
