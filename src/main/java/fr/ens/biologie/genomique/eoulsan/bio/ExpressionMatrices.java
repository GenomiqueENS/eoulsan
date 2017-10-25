package fr.ens.biologie.genomique.eoulsan.bio;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * This class contains useful methods for transforming matrices.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ExpressionMatrices {

  /**
   * Merge some columns of a matrix in another matrice by summing the content of
   * the cell to merge.
   * @param srcMatrix source matrix
   * @param srcColumNames column of the source matrix to merge
   * @param destMatrix destination matrix
   * @param destColumName destination column name
   */
  public static void merge(final ExpressionMatrix srcMatrix,
      final Collection<String> srcColumNames, final ExpressionMatrix destMatrix,
      final String destColumName) {

    Objects.requireNonNull(srcMatrix, "srcMatrix argument cannot be null");
    Objects.requireNonNull(srcColumNames,
        "srcColumNames argument cannot be null");
    Objects.requireNonNull(destMatrix, "destMatrix argument cannot be null");
    Objects.requireNonNull(destColumName,
        "destColumName argument cannot be null");

    for (String rawName : srcMatrix.getRawNames()) {

      double sum = 0.0;

      for (String columnName : srcColumNames) {
        sum += srcMatrix.getValue(rawName, columnName);
      }

      destMatrix.setValue(rawName, destColumName, sum);
    }
  }

  /**
   * Remove all the raws of the matrix that contains only zero values.
   * @param matrix the matrix to process
   */
  public static void removeEmptyRows(final ExpressionMatrix matrix) {

    Objects.requireNonNull(matrix, "matrix argument cannot be null");

    for (String rawName : matrix.getRawNames()) {

      List<Double> raw = matrix.getRawValues(rawName);

      boolean findNonZero = false;
      for (Double v : raw) {
        if (v != 0.0) {
          findNonZero = true;
          break;
        }
      }

      if (!findNonZero) {
        matrix.removeRaw(rawName);
      }

    }

  }

}
