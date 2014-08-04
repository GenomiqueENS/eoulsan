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
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.core;

import java.util.Map;

import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This interface define a step status.
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface StepStatus {

  /**
   * Get sample message.
   * @return the message for the context
   */
  String getMessage();

  /**
   * Get the sample counters.
   * @return the sample counters as a map
   */
  Map<String, Long> getCounters();

  /**
   * Get the context description.
   * @return a String with the context description
   */
  String getDescription();

  /**
   * Get the progress of the context processing.
   * @return the progress of the processing of the sample as percent (between
   *         0.0 and 1.0)
   */
  double getProgress();

  /**
   * Set the context message.
   * @param message the message to set
   */
  void setMessage(String message);

  /**
   * Set the context description.
   * @param description the description to set
   */
  void setDescription(String description);

  /**
   * Set the context counters.
   * @param reporter the reporter
   * @param counterGroup counter group to use with the reporter
   */
  void setCounters(Reporter reporter, String counterGroup);

  /**
   * Set the progress of the processing of the context.
   * @param min minimal value of the progress
   * @param max maximal value of the progress
   * @param value current value of the progress
   */
  void setProgress(int min, int max, int value);

  /**
   * Set the progress of the processing of the context.
   * @param progress value of the progress. This value must be greater or equals
   *          to 0 and lower or equals to 1.0
   */
  void setProgress(double progress);

  /**
   * Create a StepResult object for a successful result.
   */
  StepResult createStepResult();

  /**
   * Create a StepResult object.
   * @param success true if the step is successful
   */
  StepResult createStepResult(boolean success);

  /**
   * Create a StepResult object.
   * @param exception exception of the error
   * @param exceptionMessage Error message
   */
  StepResult createStepResult(Throwable exception, String exceptionMessage);

  /**
   * Create a StepResult object.
   * @param exception exception of the error
   */
  StepResult createStepResult(Throwable exception);

}
