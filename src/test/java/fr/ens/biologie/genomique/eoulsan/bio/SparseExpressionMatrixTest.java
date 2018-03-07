package fr.ens.biologie.genomique.eoulsan.bio;

public class SparseExpressionMatrixTest extends AbstractExpressionMatrixTest {

  @Override
  protected ExpressionMatrix createMatrix() {

    return new SparseExpressionMatrix();
  }

  @Override
  protected ExpressionMatrix createMatrix(double defaultValue) {

    return new SparseExpressionMatrix(defaultValue);
  }

}
