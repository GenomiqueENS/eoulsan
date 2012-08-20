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

package fr.ens.transcriptome.eoulsan;

/**
 * A nestable Eoulsan exception. This class came from from Java Code. In
 * Eoulsan, checked exceptions are generally preferred to RuntimeExceptions, but
 * RuntimeExceptions can be used as a fall-back if you are implementing an
 * interface which doesn't support checked exceptions. If you do this, please
 * document this clearly in the implementing class.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Matthew Pocock
 * @author Thomas Down
 */
public class EoulsanRuntimeException extends RuntimeException {

  // Serialization version UID
  private static final long serialVersionUID = 4568739390687951448L;

  /** Unknow runtime exception type. */
  public static final int UNKNOW = 0;
  /** Null pointer exception type . */
  public static final int NULL_POINTER = 1;
  /** Invalid argument exception type. */
  public static final int INVALID_ARGUMENT = 2;
  /** Invalid index exception type. */
  public static final int INVALID_INDEX = 3;

  /*
   * private static final String UNKNOW_MESSAGE = "Unknown exception : ";
   * private static final String NULL_POINTER_MESSAGE =
   * "Null pointer exception : "; private static final String
   * INVALID_ARGUMENT_MESSAGE = "Invalid argument exception : "; private static
   * final String INVALID_INDEX_MESSAGE = "Invalid index exception : "; private
   * int type; private String causeMessage; private String message;
   */

  //
  // Getters
  //

  /**
   * Get the type of the RuntimeException
   * @return Returns the type
   */
  // public int getType() {
  // return type;
  // }

  /**
   * Get the cause of the exception
   * @return Returns the cause of the exception
   */
  // public String getCauseMessage() {
  // return causeMessage;
  // }

  /**
   * Get the message of the exception.
   * @return Returns the message
   */
  // public String getMessage() {
  // return message;
  // }

  //
  // Setters
  //

  /**
   * Set the type of the exception.
   * @param type The type to set
   */
  // public void setType(final int type) {
  // this.type = type;
  // }

  /**
   * Set the cause message.
   * @param causeMessage The variable to set
   */
  // public void setCauseMessage(final String causeMessage) {
  // this.causeMessage = causeMessage;
  // }

  /**
   * Set the message of the exception.
   * @param message The message to set
   */
  // public void setMessage(final String message) {
  // this.message = message;
  // }

  //
  // Other methods
  //

  // private String getExceptionMessage() {
  //
  // String result;
  //
  // switch (getType()) {
  // case NULL_POINTER:
  // result = NULL_POINTER_MESSAGE;
  // break;
  //
  // case INVALID_ARGUMENT:
  // result = INVALID_ARGUMENT_MESSAGE;
  // break;
  //
  // case INVALID_INDEX:
  // result = INVALID_INDEX_MESSAGE;
  // break;
  //
  // default:
  // result = UNKNOW_MESSAGE;
  // break;
  // }
  //
  // return result + " " + getCauseMessage();
  // }

  //
  // Constructors
  //

  /**
   * Create a new EoulsanRuntimeException with a message.
   * @param message the message
   */
  public EoulsanRuntimeException(final String message) {
    // setMessage(message);
    super(message);
  }

  /**
   * Create a new EoulsanRuntimeException with a cause and a message.
   * @param type Type of exception message
   * @param causeMessage the cause that caused this EoulsanRuntimeException
   */
  // public EoulsanRuntimeException(final int type, final String causeMessage) {
  // setType(type);
  // setCauseMessage(causeMessage);
  // setMessage(getExceptionMessage());
  //
  // }

  /**
   * Create a new EoulsanRuntimeException.
   */
  public EoulsanRuntimeException() {
    super();
  }

}