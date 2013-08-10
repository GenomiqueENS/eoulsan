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

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState;

/**
 * This class allow to relay workflow step events to other listener. This class
 * avoid storing listeners WorkflowStep objects that are serialized.
 * @author Laurent Jourdren
 * @since 1.3
 */
public class WorkflowStepEventRelay implements WorkflowStepEvent {

  private static WorkflowStepEventRelay singleton;

  private List<WorkflowStepEvent> listeners = Lists.newArrayList();

  private Map<WorkflowStep, StepState> lastStates = Maps.newHashMap();
  private Map<WorkflowStep, Double> lastProgress = Maps.newHashMap();
  private Map<WorkflowStep, String> lastNotes = Maps.newHashMap();

  /**
   * Add a listener.
   * @param listener listener to add
   */
  public void addListener(final WorkflowStepEvent listener) {

    if (listener == null || this.listeners.contains(listener))
      return;

    this.listeners.add(listener);
  }

  /**
   * Remove a listener.
   * @param listener listener to remove
   */
  public void removeListener(final WorkflowStepEvent listener) {

    if (listener == null)
      return;

    this.listeners.remove(listener);
  }

  @Override
  public void updateStepState(final WorkflowStep step) {

    if (step == null)
      return;

    // Avoid duplicates messages
    if (this.lastStates.containsKey(step)
        && step.getState() == this.lastStates.get(step))
      return;

    for (WorkflowStepEvent listener : this.listeners)
      listener.updateStepState(step);

    // Save the last update
    this.lastStates.put(step, step.getState());
  }

  @Override
  public void updateStepState(final WorkflowStep step, final double progress) {

    if (step == null)
      return;

    // Avoid duplicates messages
    if (this.lastStates.containsKey(step)
        && this.lastProgress.containsKey(step)
        && step.getState() == this.lastStates.get(step)
        && progress == this.lastProgress.get(step))
      return;

    for (WorkflowStepEvent listener : this.listeners)
      listener.updateStepState(step, progress);

    // Save the last update
    this.lastStates.put(step, step.getState());
    this.lastProgress.put(step, progress);
  }

  @Override
  public void updateStepState(final WorkflowStep step, final String note) {

    if (step == null)
      return;

    // Avoid duplicates messages
    if (this.lastStates.containsKey(step)
        && this.lastNotes.containsKey(step)
        && step.getState() == this.lastStates.get(step)
        && note == this.lastNotes.get(step))
      return;

    for (WorkflowStepEvent listener : this.listeners)
      listener.updateStepState(step, note);

    // Save the last update
    this.lastStates.put(step, step.getState());
    this.lastNotes.put(step, note);
  }

  //
  // Static methods
  //

  /**
   * Get the singleton instance of WorkflowStepEventRelay.
   * @return the singleton instance of WorkflowStepEventRelay
   */
  public static WorkflowStepEventRelay getInstance() {

    if (singleton == null)
      singleton = new WorkflowStepEventRelay();

    return singleton;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private WorkflowStepEventRelay() {
  }

}
