package fr.ens.biologie.genomique.eoulsan.bio;

/**
 * This class define a dense annotation matrix.
 * @author Laurent Jourdren
 * @since 2.4
 */
public class DenseAnnotationMatrix extends DenseMatrix<String>
    implements AnnotationMatrix {

  private static final String DEFAULT_DEFAULT_VALUE = "";

  //
  // Constructors
  //

  /**
   * Public constructor.
   */
  public DenseAnnotationMatrix() {
    this(DEFAULT_DEFAULT_VALUE);
  }

  /**
   * Public constructor.
   * @param defaultValue the default value of the matrix
   */
  public DenseAnnotationMatrix(final String defaultValue) {
    super(defaultValue);
  }

}
