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

import java.util.Objects;

import fr.ens.biologie.genomique.eoulsan.data.Data;

/**
 * This class define a token of the workflow.
 * @author Laurent Jourdren
 * @since 2.0
 */
class Token {

  private static int count;

  private final int id = ++count;
  private final StepOutputPort fromPort;
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
  public StepOutputPort getOrigin() {

    return this.fromPort;
  }

  /**
   * Test if the token is an end of step token.
   * @return true if the token is an end of step token
   */
  public boolean isEndOfStepToken() {
    return this.endOfStepToken;
  }

  /**
   * Get the data in the token.
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

    return com.google.common.base.Objects.toStringHelper(this)
        .add("id", this.id).add("fromPort", this.fromPort)
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
  Token(final StepOutputPort fromPort) {

    Objects.requireNonNull(fromPort);

    this.fromPort = fromPort;
    this.endOfStepToken = true;
    this.data = null;
  }

  /**
   * Constructor for a standard token (with data).
   * @param fromPort origin of the token
   * @param data data embedded in the token
   */
  Token(final StepOutputPort fromPort, final Data data) {

    Objects.requireNonNull(fromPort);
    Objects.requireNonNull(data);

    this.fromPort = fromPort;
    this.endOfStepToken = false;
    this.data = data;
  }

}
