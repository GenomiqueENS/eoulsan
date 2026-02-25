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

import com.google.common.base.MoreObjects;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class define a token of the workflow.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
class Token {

  private static AtomicInteger instanceCount = new AtomicInteger(0);

  private final int id;
  private final StepOutputPort fromPort;
  private final int tokensCount;
  private final Data data;

  /**
   * Get the id of the token.
   *
   * @return the id of the token
   */
  public int getId() {
    return this.id;
  }

  /**
   * Get the output port at the origin of the token.
   *
   * @return a WorkflowOutputPort object
   */
  public StepOutputPort getOrigin() {

    return this.fromPort;
  }

  /**
   * Test if the token is an end of step token.
   *
   * @return true if the token is an end of step token
   */
  public boolean isEndOfStepToken() {
    return this.tokensCount != -1;
  }

  /**
   * Get the number of tokens sent by the port at the end of the step.
   *
   * @return the number of tokens sent by the port at the end of the step
   */
  public int getTokenCount() {
    return this.tokensCount;
  }

  /**
   * Get the data in the token.
   *
   * @return the data object in the token
   */
  public Data getData() {

    if (this.data == null) {
      throw new IllegalStateException();
    }

    return this.data;
  }

  @Override
  public String toString() {

    return MoreObjects.toStringHelper(this)
        .add("id", this.id)
        .add("fromPort", this.fromPort)
        .add("tokensSent", this.tokensCount)
        .add("data", this.data)
        .toString();
  }

  //
  // Constructors
  //

  /**
   * Constructor for an end of step token.
   *
   * @param fromPort origin of the token
   * @param tokenCount number of tokens sent by the port at the end of the step
   */
  Token(final StepOutputPort fromPort, final int tokenCount) {

    Objects.requireNonNull(fromPort);

    this.id = instanceCount.incrementAndGet();

    this.fromPort = fromPort;
    this.tokensCount = tokenCount;
    this.data = null;
  }

  /**
   * Constructor for a standard token (with data).
   *
   * @param fromPort origin of the token
   * @param data data embedded in the token
   */
  Token(final StepOutputPort fromPort, final Data data) {

    Objects.requireNonNull(fromPort);
    Objects.requireNonNull(data);

    this.id = instanceCount.incrementAndGet();

    this.fromPort = fromPort;
    this.tokensCount = -1;
    this.data = data;
  }
}
