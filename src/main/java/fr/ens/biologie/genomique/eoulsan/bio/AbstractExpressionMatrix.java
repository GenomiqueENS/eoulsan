package fr.ens.biologie.genomique.eoulsan.bio;

import java.util.List;
import java.util.Objects;

/**
 * This class define an abstract expression matrix.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractExpressionMatrix implements ExpressionMatrix {

  @Override
  public void addRows(final List<String> rowNames) {

    Objects.requireNonNull("rowNames argument cannot be null");

    for (String rowName : rowNames) {
      addRow(rowName);
    }
  }

  @Override
  public void addRows(final String... rowNames) {

    Objects.requireNonNull("rowNames argument cannot be null");

    for (String rowName : rowNames) {
      addRow(rowName);
    }
  }

  @Override
  public void addColumns(final List<String> columnNames) {

    Objects.requireNonNull(columnNames, "columnName argument cannot be null");

    for (String columnName : columnNames) {
      addColumn(columnName);
    }
  }

  @Override
  public void addColumns(final String... columnNames) {

    Objects.requireNonNull(columnNames);

    for (String columnName : columnNames) {
      addColumn(columnName);
    }
  }

  @Override
  public void add(final ExpressionMatrix matrix) {

    Objects.requireNonNull(matrix, "matrix argument cannot be null");

    List<String> newColumnNames = matrix.getColumnNames();
    List<String> newRowNames = matrix.getRowNames();

    // Add the valueq
    for (String rowName : newRowNames) {
      for (String columnName : newColumnNames) {
        setValue(rowName, columnName, matrix.getValue(rowName, columnName));
      }
    }
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder();

    sb.append("Id");

    for (String columnName : getColumnNames()) {
      sb.append('\t');
      sb.append(columnName);
    }
    sb.append('\n');

    for (String rowName : getRowNames()) {

      sb.append(rowName);

      for (Double value : getRowValues(rowName)) {
        sb.append('\t');
        sb.append(value);
      }
      sb.append('\n');
    }

    return sb.toString();
  }

}
