package fr.ens.transcriptome.eoulsan.core.workflow;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import fr.ens.transcriptome.eoulsan.core.Data;

/**
 * This class define a token of the workflow.
 * @author Laurent Jourdren
 * @since 1.3
 */
class Token {

  private static int count;

  private final int id = ++count;
  private final WorkflowOutputPort fromPort;
  private final boolean endOfStepToken;
  private final Data data;

  /**
   * Get the id of the token.
   * @return the id of the token
   */
  public int getId() {
    return this.id;
  }

  /**
   * Get the output port at the origin of the token.
   * @return a WorkflowOutputPort object
   */
  public WorkflowOutputPort getOrigin() {

    return fromPort;
  }

  /**
   * Test if the token is an end of step token.
   * @return true if the token is an end of step token
   */
  public boolean isEndOfStepToken() {
    return endOfStepToken;
  }

  /**
   * Get the data in the token.
   * @return the data object in the token
   */
  public Data getData() {

    if (this.data == null)
      throw new IllegalStateException();

    return data;
  }

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("id", this.id)
        .add("fromPort", this.fromPort)
        .add("endOfStepToken", this.endOfStepToken).add("data", this.data)
        .toString();
  }

  //
  // Constructors
  //

  /**
   * Constructor for an end of step token.
   * @param fromPort origin of the token
   */
  Token(final WorkflowOutputPort fromPort) {

    Preconditions.checkNotNull(fromPort);

    this.fromPort = fromPort;
    this.endOfStepToken = true;
    this.data = null;
  }

  /**
   * Constructor for a standard token (with data).
   * @param fromPort origin of the token
   * @param data data embedded in the token
   */
  Token(final WorkflowOutputPort fromPort, final Data data) {

    Preconditions.checkNotNull(fromPort);
    Preconditions.checkNotNull(data);

    this.fromPort = fromPort;
    this.endOfStepToken = false;
    this.data = data;
  }

}
