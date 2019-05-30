package fr.ens.biologie.genomique.eoulsan.core.workflow;

import fr.ens.biologie.genomique.eoulsan.core.Step;

/**
 * This class define a UI step event.
 * @author Laurent Jourdren
 * @since 2.3
 */
public class UIStepEvent extends UIEvent {

  enum StepStatusMessage {
    PROGRESS, TASK_PROGRESS, NOTE,
  }

  private final Step step;
  private final StepStatusMessage status;
  private final int contextId;
  private final String contextName;
  private final double progress;
  private final int terminatedTasks;
  private final int submittedTasks;
  private final String note;

  /**
   * Get the step.
   * @return the step
   */
  public Step getStep() {
    return step;
  }

  /**
   * Get the message type
   * @return the message type
   */
  public StepStatusMessage getStatus() {
    return status;
  }

  /**
   * Get the context id.
   * @return the context id
   */
  public int getContextId() {
    return contextId;
  }

  /**
   * Get the context name.
   * @return the context name
   */
  public String getContextName() {
    return contextName;
  }

  /**
   * Get the progress.
   * @return the progress
   */
  public double getProgress() {
    return progress;
  }

  /**
   * Get the number of terminated tasks.
   * @return the number of terminated tasks
   */
  public int getTerminatedTasks() {
    return terminatedTasks;
  }

  /**
   * Get the number of submitted tasks.
   * @return the number of submitted tasks
   */
  public int getSubmittedTasks() {
    return submittedTasks;
  }

  /**
   * Get the note.
   * @return the note
   */
  public String getNote() {
    return note;
  }

  //
  // Constructors
  //

  /**
   * Constructor.
   * @param step the step
   * @param contextId the context id
   * @param contextName the context name
   * @param progress the progress
   */
  UIStepEvent(final Step step, final int contextId, final String contextName,
      final double progress) {

    this.step = step;
    this.status = StepStatusMessage.PROGRESS;
    this.contextId = contextId;
    this.contextName = contextName;
    this.progress = progress;

    this.terminatedTasks = -1;
    this.submittedTasks = -1;
    this.note = null;
  }

  /**
   * Constructor.
   * @param step the step.
   * @param terminatedTasks the number of terminated tasks
   * @param submittedTasks the number of submitted tasks
   * @param progress the progress
   */
  UIStepEvent(final Step step, final int terminatedTasks, int submittedTasks,
      final double progress) {

    this.step = step;
    this.status = StepStatusMessage.TASK_PROGRESS;
    this.progress = progress;
    this.terminatedTasks = terminatedTasks;
    this.submittedTasks = submittedTasks;

    this.contextId = -1;
    this.contextName = null;
    this.note = null;
  }

  /**
   * Constructor.
   * @param step the step
   * @param note the note
   */
  UIStepEvent(final Step step, final String note) {

    this.step = step;
    this.status = StepStatusMessage.NOTE;
    this.note = note;

    this.progress = -1;
    this.terminatedTasks = -1;
    this.submittedTasks = -1;
    this.contextId = -1;
    this.contextName = null;
  }

}
