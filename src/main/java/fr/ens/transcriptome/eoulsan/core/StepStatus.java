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

import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepResult;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This interface define a step status.
 * @author Laurent Jourdren
 * @since 1.3
 */
public interface StepStatus {

  /**
   * Get step message.
   * @return the step message in a String
   */
  String getStepMessage();

  /**
   * Get the step counters.
   * @return the step counters in a map
   */
  Map<String, Long> getStepCounters();

  /**
   * Get sample message.
   * @param sample sample
   * @return the message for the sample
   */
  String getSampleMessage(Sample sample);

  /**
   * Get the sample counters.
   * @param sample sample
   * @return the sample counters as a map
   */
  Map<String, Long> getSampleCounters(Sample sample);

  /**
   * Set the step message.
   * @param message message to set
   */
  void setStepMessage(String message);

  /**
   * Set the step counters.
   * @param reporter the reporter
   * @param counterGroup counter group to use with the reporter
   * @param sampleCounterHeader header for the sample (optional)
   */
  void setStepCounters(Reporter reporter, String counterGroup,
      String sampleCounterHeader);

  /**
   * Set the sample message.
   * @param sample the sample
   * @param message the message to set
   */
  void setSampleMessage(Sample sample, String message);

  /**
   * Set the sample counters
   * @param sample the sample
   * @param reporter the reporter
   * @param counterGroup counter group to use with the reporter
   * @param sampleCounterHeader header for the sample (optional)
   */
  void setSampleCounters(Sample sample, Reporter reporter, String counterGroup,
      String sampleCounterHeader);

  /**
   * Get a note about the step progress
   * @return a String. The result can be null is not note is set
   */
  String getNote();

  /**
   * Get progress of the step.
   * @return the progress of the step as percent (between 0.0 and 1.0)
   */
  double getProgress();

  /**
   * Get the progress of a sample processing.
   * @param sample sample to process
   * @return the progress of the processing of the sample as percent (between
   *         0.0 and 1.0)
   */
  double getSampleProgress(Sample sample);

  /**
   * Set a note in step progress
   * @param note a note. Can be null to remove a previous note
   */
  void setNote(String note);

  /**
   * Set the progress of the step.
   * @param min minimal value of the progress
   * @param max maximal value of the progress
   * @param value current value of the progress
   */
  void setProgress(int min, int max, int value);

  /**
   * Set the value of the progress.
   * @param progress value of the progress. This value must be greater or equals
   *          to 0 and lower or equals to 1.0
   */
  void setProgress(double progress);

  /**
   * Set the progress of the processing of a sample by the step.
   * @param sample the sample to process
   * @param min minimal value of the progress
   * @param max maximal value of the progress
   * @param value current value of the progress
   */
  void setSampleProgress(Sample sample, int min, int max, int value);

  /**
   * Set the progress of the processing of a sample by the step.
   * @param sample the sample to process
   * @param progress value of the progress. This value must be greater or equals
   *          to 0 and lower or equals to 1.0
   */
  void setSampleProgress(Sample sample, double progress);

  /**
   * Create a StepResult object for a successful result.
   */
  WorkflowStepResult createStepResult();

  /**
   * Create a StepResult object.
   * @param success true if the step is successful
   * @param logMsg log message
   */
  WorkflowStepResult createStepResult(boolean success);

  /**
   * Create a StepResult object.
   * @param exception exception of the error
   * @param errorMsg Error message
   * @param logMsg log message
   */
  WorkflowStepResult createStepResult(Exception exception, String exceptionMessage);

  /**
   * Create a StepResult object.
   * @param exception exception of the error
   */
  WorkflowStepResult createStepResult(Exception exception);

}