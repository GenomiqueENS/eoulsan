package fr.ens.transcriptome.eoulsan.core.workflow;

import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState;

import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

/**
 * Created by jourdren on 16/02/14.
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
