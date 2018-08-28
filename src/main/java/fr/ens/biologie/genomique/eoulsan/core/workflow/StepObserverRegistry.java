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

import java.util.HashSet;
import java.util.Set;

import com.google.common.eventbus.Subscribe;

import fr.ens.biologie.genomique.eoulsan.core.Step;

/**
 * This class allow to relay workflow step events to other observers. This class
 * avoid storing observers WorkflowStep objects that are serialized.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class StepObserverRegistry {

  private static StepObserverRegistry singleton;

  private final Set<StepObserver> observers = new HashSet<>();

  /**
   * Add a listener.
   * @param observer listener to add
   */
  public void addObserver(final StepObserver observer) {

    if (observer == null || this.observers.contains(observer)) {
      return;
    }

    this.observers.add(observer);
  }

  /**
   * Remove a listener.
   * @param observer listener to remove
   */
  public void removeObserver(final StepObserver observer) {

    if (observer == null) {
      return;
    }

    this.observers.remove(observer);
  }

  /**
   * Test if observers has been registered
   * @return true if observers has been registered
   */
  public boolean isNoObserverRegistered() {
    return this.observers.isEmpty();
  }

  //
  // Subscribe methods
  //

  /**
   * Handle UI task events.
   * @param event the event to handle
   */
  @Subscribe
  public void uiTaskEvent(final UITaskEvent event) {

    if (event == null) {
      return;
    }

    final Step step = event.getStep();
    final int contextId = event.getContextId();

    switch (event.getTaskStatusMessage()) {

    case SUBMITTED:
      for (StepObserver o : this.observers) {
        o.notifyTaskSubmitted(step, contextId);
      }
      break;

    case RUNNING:
      for (StepObserver o : this.observers) {
        o.notifyTaskRunning(step, contextId);
      }
      break;

    case DONE:
      for (StepObserver o : this.observers) {
        o.notifyTaskDone(step, contextId);
      }
      break;

    default:
      break;
    }
  }

  /**
   * Handle step state change events.
   * @param event the event to handle
   */
  @Subscribe
  public void stepStateEvent(final StepStateEvent event) {

    if (event == null) {
      return;
    }

    for (StepObserver o : this.observers) {
      o.notifyStepState(event.getStep(), event.getState());
    }
  }

  /**
   * Handle UI step events.
   * @param event the event to handle
   */
  @Subscribe
  public void uiStepEvent(final UIStepEvent event) {

    if (event == null) {
      return;
    }

    final Step step = event.getStep();
    final double progress = event.getProgress();

    switch (event.getStatus()) {

    case PROGRESS:
      int contextId = event.getContextId();
      String contextName = event.getContextName();
      for (StepObserver o : this.observers) {
        o.notifyStepState(step, contextId, contextName, progress);
      }
      break;

    case TASK_PROGRESS:
      int terminatedTasks = event.getTerminatedTasks();
      int submittedTasks = event.getSubmittedTasks();
      for (StepObserver o : this.observers) {
        o.notifyStepState(step, terminatedTasks, submittedTasks, progress);
      }
      break;

    case NOTE:
      String note = event.getNote();
      for (StepObserver o : this.observers) {
        o.notifyStepState(step, note);
      }
      break;

    default:
      break;
    }
  }

  /**
   * Handle UI workflow events.
   * @param event the event to handle
   */
  @Subscribe
  public void uiWorkflowEvent(final UIWorkflowEvent event) {

    if (event == null) {
      return;
    }

    for (StepObserver o : this.observers) {
      o.notifyWorkflowSuccess(event.isSuccess(), event.getMessage());
    }
  }

  //
  // Static methods
  //

  /**
   * Get the singleton instance of WorkflowStepObserverRegistry.
   * @return the singleton instance of WorkflowStepObserverRegistry
   */
  public static StepObserverRegistry getInstance() {

    if (singleton == null) {
      singleton = new StepObserverRegistry();
    }

    return singleton;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private StepObserverRegistry() {

    // Register this class to dispatch messages to UIs
    WorkflowEventBus.getInstance().register(this);
  }

}
