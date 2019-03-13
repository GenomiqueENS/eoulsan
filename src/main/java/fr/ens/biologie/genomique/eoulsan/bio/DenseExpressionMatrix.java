package fr.ens.biologie.genomique.eoulsan.bio;

/**
 * This class define a dense expression matrix.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DenseExpressionMatrix extends DenseMatrix<Double>
    implements ExpressionMatrix {

  private static final double DEFAULT_DEFAULT_VALUE = 0.0;

  @Override
  public void setValue(final String rowName, final String columnName,
      double value) {

    super.setValue(rowName, columnName, (Double) value);
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   */
  public DenseExpressionMatrix() {
    this(DEFAULT_DEFAULT_VALUE);
  }

  /**
   * Public constructor.
   * @param defaultValue the default value of the matrix
   */
  public DenseExpressionMatrix(final double defaultValue) {
    super(defaultValue);
  }

}
