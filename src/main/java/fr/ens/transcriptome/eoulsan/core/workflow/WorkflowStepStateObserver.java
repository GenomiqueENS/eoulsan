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

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.CREATED;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.DONE;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.READY;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.WAITING;

import java.io.Serializable;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState;

/**
 * This class define an observer for step states.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class WorkflowStepStateObserver implements Serializable {

  private static final long serialVersionUID = -5734184849291521186L;

  private final AbstractWorkflowStep step;
  private StepState stepState;

  private Set<AbstractWorkflowStep> requiredSteps = Sets.newHashSet();
  private Set<AbstractWorkflowStep> stepsToInform = Sets.newHashSet();

  /**
   * Add a dependency.
   * @param step the dependency
   */
  public void addDependency(final AbstractWorkflowStep step) {

    this.requiredSteps.add(step);
    step.getStepStateObserver().stepsToInform.add(this.step);
  }

  /**
   * Get the state of the step.
   * @return the state of the step
   */
  public StepState getState() {

    return this.stepState;
  }

  /**
   * Set the state of the step.
   * @param state the new state of the step
   */
  public void setState(final StepState state) {

    if (state == null)
      return;

    // If is the root step, there is nothing to wait
    synchronized (this) {
      if (this.step.getType() == WorkflowStep.StepType.ROOT_STEP
          && state == WAITING) {
        this.stepState = READY;
      } else {
        this.stepState = state;
      }
    }

    // Log the new state of the step
    getLogger().fine(
        "Step #"
            + this.step.getNumber() + " " + this.step.getId()
            + " is now in state " + this.stepState);

    // If step has just been created there is nothing to do
    if (this.stepState == CREATED) {
      return;
    }

    // Inform step that depend of this step
    if (this.stepState == DONE) {
      for (AbstractWorkflowStep step : this.stepsToInform) {
        step.getStepStateObserver().updateStatus();
      }
    }

    // Start Token manager thread for the step if state is READY
    if (this.stepState == READY) {
      TokenManagerRegistry.getInstance().getTokenManager(this.step).start();
    }

    // Inform workflow object
    step.getAbstractWorkflow().updateStepState(this.step);

    // Inform listeners
    for (WorkflowStepObserver o : WorkflowStepObserverRegistry.getInstance()
        .getObservers()) {
      o.notifyStepState(this.step);
    }
  }

  /**
   * Update the status of the step to READY if all the dependency of this step
   * are in DONE state.
   */
  private void updateStatus() {

    // Do nothing if the step is already in READY state
    if (getState() == READY) {
      return;
    }

    for (AbstractWorkflowStep step : this.requiredSteps) {
      if (step.getState() != DONE) {
        return;
      }
    }

    // Set the step to the READY state
    setState(READY);
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param step the step related to the instance
   */
  public WorkflowStepStateObserver(final AbstractWorkflowStep step) {

    checkNotNull(step, "step cannot be null");

    this.step = step;
    setState(CREATED);
  }
}
