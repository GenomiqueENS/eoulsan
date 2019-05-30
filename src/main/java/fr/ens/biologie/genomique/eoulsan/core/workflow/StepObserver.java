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

package fr.ens.biologie.genomique.eoulsan.core.workflow;

import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.Step.StepState;

/**
 * This interface define workflow step events
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface StepObserver {

  /**
   * The status of the step has been changed.
   * @param step step that the status has been changed
   * @param stepState state the step state
   */
  void notifyStepState(Step step, final StepState stepState);

  /**
   * The progress of the step for a sample has been changed.
   * @param step step that the progress has been changed
   * @param contextId id of the context
   * @param contextName name of the context that has been changed
   */
  void notifyStepState(Step step, int contextId, String contextName,
      double progress);

  /**
   * The progress of the step has been changed.
   * @param step step that the progress has been changed
   * @param terminatedTasks the terminated tasks count
   * @param submittedTasks the submitted tasks count
   * @param progress the progress of the step
   */
  void notifyStepState(Step step, int terminatedTasks, int submittedTasks,
      double progress);

  /**
   * The note of the step has been changed.
   * @param step step that the note has been changed
   */
  void notifyStepState(Step step, String note);

  /**
   * Notify that a task has been submitted.
   * @param step the step of the submitted task
   * @param contextId id of the context
   */
  void notifyTaskSubmitted(Step step, int contextId);

  /**
   * Notify that a task is running.
   * @param step the step of the submitted task
   * @param contextId id of the context
   */
  void notifyTaskRunning(Step step, int contextId);

  /**
   * Notify that a task has been done.
   * @param step the step of the submitted task
   * @param contextId id of the context
   */
  void notifyTaskDone(Step step, int contextId);

  /**
   * Notify the success of the workflow.
   * @param success the success of the workflow
   * @param message success message
   */
  void notifyWorkflowSuccess(boolean success, String message);

}
