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

/**
 * This interface allow to set the progress of a task.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface Progress {

  //
  // Getters
  //

  /**
   * Get the progress message.
   *
   * @return the message for the context
   */
  String getProgressMessage();

  /**
   * Get the progress of the context processing.
   *
   * @return the progress of the processing of the sample as percent (between 0.0 and 1.0)
   */
  double getProgress();

  //
  // Setters
  //

  /**
   * Set the progress message.
   *
   * @param message the message to set
   */
  void setProgressMessage(String message);

  /**
   * Set the progress of the processing.
   *
   * @param min minimal value of the progress
   * @param max maximal value of the progress
   * @param value current value of the progress
   */
  void setProgress(int min, int max, int value);

  /**
   * Set the progress of the processing.
   *
   * @param progress value of the progress. This value must be greater or equals to 0 and lower or
   *     equals to 1.0
   */
  void setProgress(double progress);
}
