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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.eoulsan.core.workflow.UITaskEvent.TaskStatusMessage;
import java.util.HashMap;
import java.util.Map;

/**
 * This class define a step status.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class StepStatus {

  private final AbstractStep step;

  private final Map<Integer, Double> taskProgress = new HashMap<>();
  private final Map<Integer, String> taskNames = new HashMap<>();
  private double progress = Double.NaN;
  private String note;

  //
  // Progress Methods
  //

  /**
   * Get the note.
   *
   * @return the note
   */
  public String getNote() {

    return this.note;
  }

  /**
   * Get the progress of the step.
   *
   * @return a double between O and 1
   */
  public double getProgress() {

    if (Double.isNaN(this.progress)) {

      double sum = 0.0;

      synchronized (this) {
        for (Double p : this.taskProgress.values()) {
          sum += p;
        }
      }

      return sum / TokenManagerRegistry.getInstance().getTokenManager(this.step).getContextCount();
    }

    return this.progress;
  }

  /**
   * Get the name of a task.
   *
   * @param contextId the id of the task
   * @return a String with the name of the context or null if the context not exists
   */
  public String getTaskName(final int contextId) {

    return this.taskNames.get(contextId);
  }

  /**
   * Get the progress of a task.
   *
   * @param contextId the id of the task
   * @return a double between O and 1
   */
  public double getTaskProgress(final int contextId) {

    return this.taskProgress.get(contextId);
  }

  /**
   * Get the number of submitted tasks.
   *
   * @return the number of submitted tasks
   */
  public int getSubmittedTasks() {

    return this.taskProgress.size();
  }

  /**
   * Get the number of terminated tasks.
   *
   * @return the number of terminated tasks
   */
  public int getTerminatedTasks() {

    int result = 0;

    synchronized (this) {
      for (double progress : this.taskProgress.values()) {
        if (progress == 1.0) {
          result++;
        }
      }
    }

    return result;
  }

  //
  // Setters
  //

  /**
   * Set the note.
   *
   * @param note the note to set
   */
  public void setNote(final String note) {

    this.note = note;

    // Inform listener that status has changed
    noteStatusUpdated();
  }

  /**
   * Set the progress of a step.
   *
   * @param min minimal value
   * @param max maximal value
   * @param value value to set
   */
  public void setProgress(final int min, final int max, final int value) {

    checkProgress(min, max, value);

    if (min == max) {
      setProgress(1.0);
    } else {
      setProgress(((double) (value - min)) / (max - min));
    }
  }

  /**
   * Set the progress of a step.
   *
   * @param progress value to set
   */
  public void setProgress(final double progress) {

    checkProgress(progress);

    synchronized (this) {
      this.progress = progress;
    }

    // Inform listener that status has changed
    progressStatusUpdated();
  }

  /**
   * Set the progress of a task.
   *
   * @param contextId id of the context
   * @param contextName name of the context
   * @param min minimal value
   * @param max maximal value
   * @param value value to set
   */
  public void setTaskProgress(
      final int contextId,
      final String contextName,
      final int min,
      final int max,
      final int value) {

    checkProgress(min, max, value);

    if (min == max) {
      setTaskProgress(contextId, contextName, 1.0);
    } else {
      setTaskProgress(contextId, contextName, ((double) (value - min)) / (max - min));
    }
  }

  /**
   * Set the progress of a task.
   *
   * @param contextId id of the context
   * @param contextName name of the context
   * @param progress progress value to set
   */
  public void setTaskProgress(
      final int contextId, final String contextName, final double progress) {

    checkContext(contextName);
    checkProgress(progress);

    // Inform observers that status has changed
    progressTaskStatusUpdated(contextId, contextName, progress);

    // Save progress contextName for step progress computation
    synchronized (this) {
      this.taskProgress.put(contextId, progress);

      // Update context name if needed
      if (!this.taskNames.containsKey(contextId)
          || !this.taskNames.get(contextId).equals(contextName)) {
        this.taskNames.put(contextId, contextName);
      }
    }

    // Inform observers that status has changed
    progressStatusUpdated();
  }

  /**
   * Set task submitted.
   *
   * @param contextId id of the context
   */
  public void setTaskSubmitted(final int contextId) {

    WorkflowEventBus.getInstance()
        .postUIEvent(new UITaskEvent(this.step, TaskStatusMessage.SUBMITTED, contextId));
  }

  /**
   * Set task running.
   *
   * @param contextId id of the context
   */
  public void setTaskRunning(final int contextId) {

    WorkflowEventBus.getInstance()
        .postUIEvent(new UITaskEvent(this.step, TaskStatusMessage.RUNNING, contextId));
  }

  /**
   * Set task done.
   *
   * @param contextId id of the context
   */
  public void setTaskDone(final int contextId) {

    WorkflowEventBus.getInstance()
        .postUIEvent(new UITaskEvent(this.step, TaskStatusMessage.DONE, contextId));
  }

  //
  // Observers
  //

  /**
   * Inform observers that the status has been changed.
   *
   * @param contextId id of the context
   * @param contextName name of the context
   * @param progress progress value
   */
  private void progressTaskStatusUpdated(
      final int contextId, final String contextName, final double progress) {

    WorkflowEventBus.getInstance()
        .postUIEvent(new UIStepEvent(this.step, contextId, contextName, progress));
  }

  /** Inform observers that the status has been changed. */
  private void progressStatusUpdated() {

    WorkflowEventBus.getInstance()
        .postUIEvent(
            new UIStepEvent(this.step, getTerminatedTasks(), getSubmittedTasks(), getProgress()));
  }

  /** Inform observers that the status has been changed. */
  private void noteStatusUpdated() {

    WorkflowEventBus.getInstance().postUIEvent(new UIStepEvent(this.step, this.note));
  }

  //
  // Check progress
  //

  private static void checkContext(final String contextName) {

    requireNonNull(contextName, "contextName cannot be null");
  }

  private static void checkProgress(final double progress) {

    checkArgument(progress >= 0.0, "Progress is lower than 0: " + progress);
    checkArgument(progress <= 1.0, "Progress is greater than 1: " + progress);
    checkArgument(!Double.isInfinite(progress), "Progress is infinite");
    checkArgument(!Double.isNaN(progress), "Progress is NaN");
  }

  private static void checkProgress(final int min, final int max, final int value) {

    checkArgument(min <= max, "Max is lower than min");
    checkArgument(min <= value, "Value is lower than min");
    checkArgument(value <= max, "Value is greater than max");
  }

  //
  // Constructor
  //

  public StepStatus(final AbstractStep step) {

    requireNonNull(step, "Step is null");

    this.step = step;
  }
}
