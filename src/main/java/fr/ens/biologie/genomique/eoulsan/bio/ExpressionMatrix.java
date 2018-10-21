package fr.ens.biologie.genomique.eoulsan.bio;

public interface ExpressionMatrix extends Matrix<Double> {

  /**
   * Set a value of the matrix.
   * @param rowName row name
   * @param columnName column name
   * @param value the value to set
   */
  void setValue(String rowName, String columnName, double value);

}