package fr.ens.biologie.genomique.eoulsan.core.workflow;

import java.util.Objects;

/**
 * This class define a UI workflow event.
 * @author Laurent Jourdren
 * @since 2.3
 */
public class UIWorkflowEvent extends UIEvent {

  private final boolean success;
  private final String message;

  /**
   * Test if the event is a success.
   * @return true if the event is a success
   */
  public boolean isSuccess() {
    return this.success;
  }

  /**
   * Get the message of the event.
   * @return the message of the event
   */
  public String getMessage() {
    return this.message;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param success true if the workflow is a success
   * @param message the message of the event
   */
  public UIWorkflowEvent(final boolean success, final String message) {

    Objects.requireNonNull(message, "message argument cannot be null");

    this.success = success;
    this.message = message;
  }

}
