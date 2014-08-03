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

import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState;

import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

/**
 * This class define an observer for step states.
 * @author Laurent Jourdren
 * @since 1.3
 */
public class WorkflowStepStateObserver {

  private final AbstractWorkflowStep step;
  private StepState stepState = StepState.CREATED;

  private Set<AbstractWorkflowStep> requiredSteps = Sets.newHashSet();
  private Set<AbstractWorkflowStep> stepsToInform = Sets.newHashSet();

  public void addDependency(final AbstractWorkflowStep step) {

    this.requiredSteps.add(step);
    step.getStepStateObserver().stepsToInform.add(this.step);
  }

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
    if (this.step.getType() == WorkflowStep.StepType.ROOT_STEP
        && state == StepState.WAITING)
      this.stepState = StepState.READY;
    else
      this.stepState = state;

    // Inform step that depend of this step
    if (this.stepState == StepState.DONE)
      for (AbstractWorkflowStep step : this.stepsToInform)
        step.getStepStateObserver().updateStatus();

    // Inform workflow object
    step.getAbstractWorkflow().updateStepState(this.step);

    // Inform listeners
    for (WorkflowStepObserver o : WorkflowStepObserverRegistry.getInstance()
        .getObservers())
      o.notifyStepState(this.step);
  }

  /**
   * Update the status of the step to READY if all the dependency of this step
   * are in DONE state.
   */
  private void updateStatus() {

    for (AbstractWorkflowStep step : this.requiredSteps)
      if (step.getState() != StepState.DONE)
        return;

    setState(StepState.READY);
  }

  //
  // Constructor
  //

  public WorkflowStepStateObserver(final AbstractWorkflowStep step) {

    Preconditions.checkNotNull(step, "step cannot be null");

    this.step = step;
  }
}
