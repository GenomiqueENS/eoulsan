package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.eoulsan.core.Step.StepState;


/**
 * This class define a StepState event.
 * @since 2.3
 * @author Laurent Jourdren
 */
class StepStateEvent {

  private final AbstractStep step;
  private final StepState state;

  /**
   * Get the step.
   * @return the step
   */
  public AbstractStep getStep() {

    return this.step;
  }

  /**
   * Get the state of the step.
   * @return the state of the step
   */
  public StepState getState() {

    return this.state;
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return "StepStateEvent{StepId="
        + this.step.getId() + ", StepNumber=" + this.step.getNumber()
        + ", state=" + this.state + "}";
  }

  //
  // Constructor
  //

  StepStateEvent(final AbstractStep step, final StepState state) {

    requireNonNull(step, "step argument cannot be null");
    requireNonNull(state, "state argument cannot be null");

    this.step = step;
    this.state = state;
  }

}
