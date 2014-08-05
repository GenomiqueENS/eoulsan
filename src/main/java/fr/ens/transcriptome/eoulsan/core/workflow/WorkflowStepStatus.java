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

package fr.ens.transcriptome.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * This class define a step status.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class WorkflowStepStatus {

  private final AbstractWorkflowStep step;

  private final Map<Integer, Double> contextProgress = Maps.newHashMap();
  private final Map<Integer, String> contextNames = Maps.newHashMap();
  private double progress = Double.NaN;
  private String note;

  private Set<WorkflowStepObserver> observers = WorkflowStepObserverRegistry
      .getInstance().getObservers();

  //
  // Progress Methods
  //

  /**
   * Get the note.
   * @return the note
   */
  public String getNote() {

    return this.note;
  }

  /**
   * Get the progress of the step.
   * @return a double between O and 1
   */
  public double getProgress() {

    if (Double.isNaN(this.progress)) {

      double sum = 0.0;
      for (Double p : this.contextProgress.values())
        sum += p;

      return sum
          / TokenManagerRegistry.getInstance().getTokenManager(this.step)
              .getContextCount();
    }

    return this.progress;
  }

  /**
   * Get the name of a context of the step.
   * @param contextId the id of the context
   * @return a String with the name of the context or null if the context not
   *         exists
   */
  public String getContextName(final int contextId) {

    return this.contextNames.get(contextId);
  }

  /**
   * Get the progress of a context of the step.
   * @param contextId the id of the context
   * @return a double between O and 1
   */
  public double getContextProgress(final int contextId) {

    return this.contextProgress.get(contextId);
  }

  //
  // Setters
  //

  /**
   * Set the note.
   * @param note the note to set
   */
  public void setNote(final String note) {

    this.note = note;

    // Inform listener that status has changed
    noteStatusUpdated();
  }

  /**
   * Set the progress of a step.
   * @param min minimal value
   * @param max maximal value
   * @param value value to set
   */
  public void setProgress(final int min, final int max, final int value) {

    checkProgress(min, max, value);

    if (min == max)
      setProgress(1.0);
    else
      setProgress(((double) (value - min)) / (max - min));
  }

  /**
   * Set the progress of a step.
   * @param value value to set
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
   * Set the progress of a context of the step.
   * @param contextId id of the context
   * @param contextName name of the context
   * @param min minimal value
   * @param max maximal value
   * @param value value to set
   */
  public void setContextProgress(final int contextId, final String contextName,
      final int min, final int max, final int value) {

    checkProgress(min, max, value);

    if (min == max)
      setContextProgress(contextId, contextName, 1.0);
    else
      setContextProgress(contextId, contextName, ((double) (value - min))
          / (max - min));
  }

  /**
   * Set the progress of a context of the step.
   * @param contextId id of the context
   * @param contextName name of the context
   * @param value value to set
   */
  public void setContextProgress(final int contextId, final String contextName,
      final double progress) {

    checkContext(contextId, contextName);
    checkProgress(progress);

    // Inform observers that status has changed
    progressContextStatusUpdated(contextId, contextName, progress);

    // Save progress contextName for step progress computation
    synchronized (this) {
      this.contextProgress.put(contextId, progress);

      // Update context name if needed
      if (!this.contextNames.containsKey(contextId)
          || !this.contextNames.get(contextId).equals(contextName)) {
        this.contextNames.put(contextId, contextName);
      }
    }

    // Inform observers that status has changed
    progressStatusUpdated();
  }

  //
  // Observers
  //

  /**
   * Inform observers that the status has been changed.
   * @param contextId id of the context
   * @param contextName name of the context
   * @param progress progress value
   */
  private void progressContextStatusUpdated(final int contextId,
      final String contextName, final double progress) {

    // Inform listeners
    for (WorkflowStepObserver o : this.observers)
      o.notifyStepState(this.step, contextId, contextName, progress);
  }

  /**
   * Inform observers that the status has been changed.
   */
  private void progressStatusUpdated() {

    // Inform listeners
    for (WorkflowStepObserver o : this.observers)
      o.notifyStepState(this.step, getProgress());
  }

  /**
   * Inform observers that the status has been changed.
   */
  private void noteStatusUpdated() {

    // Inform listeners
    for (WorkflowStepObserver o : this.observers)
      o.notifyStepState(this.step, this.note);
  }

  //
  // Check progress
  //

  private static void checkContext(final int contextId, final String contextName) {

    checkNotNull(contextName, "contextName cannot be null");
  }

  private static void checkProgress(final double progress) {

    checkArgument(progress >= 0.0, "Progress is lower than 0: " + progress);
    checkArgument(progress <= 1.0, "Progress is greater than 1: " + progress);
    checkArgument(!Double.isInfinite(progress), "Progress is infinite");
    checkArgument(!Double.isNaN(progress), "Progress is NaN");
  }

  private static void checkProgress(final int min, final int max,
      final int value) {

    checkArgument(min <= max, "Max is lower than min");
    checkArgument(min <= value, "Value is lower than min");
    checkArgument(value <= max, "Value is greater than max");
  }

  //
  // Constructor
  //

  public WorkflowStepStatus(final AbstractWorkflowStep step) {

    Preconditions.checkNotNull(step, "Step is null");

    this.step = step;

  }

}
