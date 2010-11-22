package fr.ens.transcriptome.eoulsan;

/**
 * This class define an exception when an argument of a method is null.
 * @author Laurent Jourdren
 */
public class NullArgumentException extends IllegalArgumentException {

  //
  // Constructors
  //

  /**
   * Public constructor.
   */
  public NullArgumentException() {

    super();
  }

  /**
   * Public constructor.
   * @param message exception message
   */
  public NullArgumentException(final String message) {

    super(message);
  }
}
