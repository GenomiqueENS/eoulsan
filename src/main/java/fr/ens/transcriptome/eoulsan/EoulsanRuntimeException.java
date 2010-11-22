/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the �cole Normale Sup�rieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan;

/*
 * Title: NividicRuntimeException.java
 * Description:
 * Copyright: Copyright (c) 2003-2004 CEA - ENS
 * Created on 3 juin 2004
 */

/**
 * A nestable nividic exception. This class came from from Java Code. In Nividic,
 * checked exceptions are generally preferred to RuntimeExceptions, but
 * RuntimeExceptions can be used as a fall-back if you are implementing an
 * interface which doesn't support checked exceptions. If you do this, please
 * document this clearly in the implementing class.
 * @author Laurent Jourdren
 * @author Matthew Pocock
 * @author Thomas Down
 */
public class EoulsanRuntimeException extends RuntimeException {

  /** Unknow runtime exception type. */
  public static final int UNKNOW = 0;
  /** Null pointer exception type . */
  public static final int NULL_POINTER = 1;
  /** Invalid argument exception type. */
  public static final int INVALID_ARGUMENT = 2;
  /** Invalid index exception type. */
  public static final int INVALID_INDEX = 3;

  /*private static final String UNKNOW_MESSAGE = "Unknown exception : ";
  private static final String NULL_POINTER_MESSAGE = "Null pointer exception : ";
  private static final String INVALID_ARGUMENT_MESSAGE = "Invalid argument exception : ";
  private static final String INVALID_INDEX_MESSAGE = "Invalid index exception : ";

  private int type;
  private String causeMessage;
  private String message;*/

  //
  // Getters
  //

  /**
   * Get the type of the RuntimeException
   * @return Returns the type
   */
//  public int getType() {
//    return type;
//  }

  /**
   * Get the cause of the exception
   * @return Returns the cause of the exception
   */
//  public String getCauseMessage() {
//    return causeMessage;
//  }

  /**
   * Get the message of the exception.
   * @return Returns the message
   */
//  public String getMessage() {
//    return message;
//  }

  //
  // Setters
  //

  /**
   * Set the type of the exception.
   * @param type The type to set
   */
//  public void setType(final int type) {
//    this.type = type;
//  }

  /**
   * Set the cause message.
   * @param causeMessage The variable to set
   */
//  public void setCauseMessage(final String causeMessage) {
//    this.causeMessage = causeMessage;
//  }

  /**
   * Set the message of the exception.
   * @param message The message to set
   */
//  public void setMessage(final String message) {
//    this.message = message;
//  }

  //
  // Other methods
  //

//  private String getExceptionMessage() {
//
//    String result;
//
//    switch (getType()) {
//    case NULL_POINTER:
//      result = NULL_POINTER_MESSAGE;
//      break;
//
//    case INVALID_ARGUMENT:
//      result = INVALID_ARGUMENT_MESSAGE;
//      break;
//
//    case INVALID_INDEX:
//      result = INVALID_INDEX_MESSAGE;
//      break;
//
//    default:
//      result = UNKNOW_MESSAGE;
//      break;
//    }
//
//    return result + " " + getCauseMessage();
//  }

  //
  // Constructors
  //

  /**
   * Create a new NividicRuntimeException with a message.
   * @param message the message
   */
  public EoulsanRuntimeException(final String message) {
    //setMessage(message);
    super(message);
  }

  /**
   * Create a new NividicRuntimeException with a cause and a message.
   * @param type Type of exception message
   * @param causeMessage the cause that caused this NividicRuntimeException
   */
//  public EoulsanRuntimeException(final int type, final String causeMessage) {
//    setType(type);
//    setCauseMessage(causeMessage);
//    setMessage(getExceptionMessage());
//
//  }

  /**
   * Create a new NividicRuntimeException.
   */
  public EoulsanRuntimeException() {
    super();
  }

}