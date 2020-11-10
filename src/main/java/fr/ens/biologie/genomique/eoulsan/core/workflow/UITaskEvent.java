package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.eoulsan.core.Step;

/**
 * This class define a UI task event.
 * @author Laurent Jourdren
 * @since 2.3
 */
public class UITaskEvent extends UIEvent {

  enum TaskStatusMessage {
    SUBMITTED, RUNNING, DONE,
  }

  private final Step step;
  private final TaskStatusMessage status;
  private final int contextId;

  /**
   * Get the step.
   * @return the step
   */
  Step getStep() {
    return this.step;
  }

  /**
   * Get the event type
   * @return the event type
   */
  TaskStatusMessage getTaskStatusMessage() {
    return this.status;
  }

  /**
   * Get the context Id related to the event
   * @return the context id
   */
  int getContextId() {
    return this.contextId;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param step the step
   * @param status the event type
   * @param contextId the context id related to the event
   */
  public UITaskEvent(final Step step, final TaskStatusMessage status,
      final int contextId) {

    requireNonNull(step, "step argument cannot be null");
    requireNonNull(status, "step argument cannot be null");

    this.step = step;
    this.status = status;
    this.contextId = contextId;
  }
}
