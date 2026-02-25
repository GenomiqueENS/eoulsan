package fr.ens.biologie.genomique.eoulsan.core;

/**
 * This class define a runtime exception for filename parsing errors.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class FileNamingParsingRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 7857462649825953398L;

  //
  // Constructors
  //

  /**
   * Create a new FileNamingParsingRuntimeException with a message.
   *
   * @param message the message
   */
  public FileNamingParsingRuntimeException(final String message) {

    super(message);
  }

  /** Create a new FileNamingParsingRuntimeException. */
  public FileNamingParsingRuntimeException() {
    super();
  }
}
