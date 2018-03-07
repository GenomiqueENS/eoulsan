package fr.ens.biologie.genomique.eoulsan.bio;

import static fr.ens.biologie.genomique.eoulsan.util.Utils.equal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * This class define an expression matrix.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DenseExpressionMatrix implements ExpressionMatrix {

  private final Multimap<String, Double> values = ArrayListMultimap.create();
  private final Map<String, Integer> columnIndex = new HashMap<>();
  private final Set<String> rowOrder = new LinkedHashSet<>();
  private Double defaultValue = 0.0;

  //
  // Getters
  //

  @Override
  public List<String> getRowNames() {

    return Collections.unmodifiableList(new ArrayList<>(this.rowOrder));
  }

  @Override
  public int getRowCount() {

    return this.rowOrder.size();
  }

  @Override
  public List<String> getColumnNames() {

    String[] result = new String[this.columnIndex.size()];

    for (Map.Entry<String, Integer> e : this.columnIndex.entrySet()) {
      result[e.getValue()] = e.getKey();
    }

    return Collections.unmodifiableList(Arrays.asList(result));
  }

  @Override
  public int getColumnCount() {

    return this.columnIndex.size();
  }

  @Override
  public List<Double> getColumnValues(final String columnName) {

    Objects.requireNonNull(columnName, "columnName argument cannot be null");

    List<Double> result = new ArrayList<>(this.rowOrder.size());

    for (String rowName : this.rowOrder) {
      result.add(getValue(rowName, columnName));
    }

    return Collections.unmodifiableList(result);
  }

  @Override
  public List<Double> getRowValues(final String rowName) {

    Objects.requireNonNull(rowName, "rowName argument cannot be null");

    return (List<Double>) this.values.get(rowName);
  }

  @Override
  public Double getValue(final String rowName, final String columnName) {

    Objects.requireNonNull(rowName, "rowName argument cannot be null");
    Objects.requireNonNull(columnName, "columnName argument cannot be null");

    if (!this.columnIndex.containsKey(columnName)) {
      throw new IllegalArgumentException("Unknown column name: " + columnName);
    }

    if (!this.rowOrder.contains(rowName)) {
      throw new IllegalArgumentException("Unknown row name: " + rowName);
    }

    return getRowValues(rowName).get(this.columnIndex.get(columnName));
  }

  @Override
  public boolean containsColumn(final String columnName) {

    return this.columnIndex.containsKey(columnName);
  }

  @Override
  public boolean containsRow(final String rowName) {

    if (this.columnIndex.size() == 0) {
      return this.rowOrder.contains(rowName);
    }

    return this.values.containsKey(rowName);
  }

  //
  // Setters
  //

  @Override
  public void setValue(final String rowName, final String columnName,
      final double value) {

    Objects.requireNonNull(rowName, "rowName argument cannot be null");
    Objects.requireNonNull(columnName, "columnName argument cannot be null");

    // Check if a column must be added
    if (!this.columnIndex.containsKey(columnName)) {
      addColumn(columnName);
    }

    // Check if a row must be added
    if (!this.values.containsKey(rowName)) {
      addRow(rowName);
    }

    // Set the value
    ((List<Double>) this.values.get(rowName))
        .set(this.columnIndex.get(columnName), value);
  }

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
  public void addRow(final String rowName) {

    Objects.requireNonNull(rowName, "rowName argument cannot be null");

    if (this.rowOrder.contains(rowName)) {
      return;
    }

    // Add the default values
    this.values.putAll(rowName,
        Collections.nCopies(this.columnIndex.size(), this.defaultValue));

    // Add the row name in the order of row
    this.rowOrder.add(rowName);
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
  public void addColumn(final String columnName) {

    Objects.requireNonNull(columnName, "columnName argument cannot be null");

    if (this.columnIndex.containsKey(columnName)) {
      return;
    }

    // Add the column name in the index of column
    this.columnIndex.put(columnName, this.columnIndex.size());

    if (this.columnIndex.size() == 1 && !this.rowOrder.isEmpty()) {

      // Fill existing empty rows
      for (String rowName : this.rowOrder) {
        this.values.put(rowName, this.defaultValue);
      }
    } else {

      // Fill the new column
      for (Collection<Double> list : this.values.asMap().values()) {
        list.add(this.defaultValue);
      }
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

  @Override
  public void renameColumn(final String oldColumnName,
      final String newColumnName) {

    Objects.requireNonNull(oldColumnName, "oldColumnName cannot be null");
    Objects.requireNonNull(newColumnName, "newColumnName cannot be null");

    if (!containsColumn(oldColumnName)) {
      throw new IllegalArgumentException(
          "Unknown column name: " + oldColumnName);
    }

    if (containsColumn(newColumnName)) {
      throw new IllegalArgumentException(
          "The new column name already exists: " + newColumnName);
    }

    this.columnIndex.put(newColumnName, this.columnIndex.get(oldColumnName));
    this.columnIndex.remove(oldColumnName);
  }

  @Override
  public void removeColumn(final String columnName) {

    Objects.requireNonNull(columnName, "columnName argument cannot be null");

    if (!this.columnIndex.containsKey(columnName)) {
      throw new IllegalArgumentException(
          "columnIndex does not exists: " + columnName);
    }

    // Get columnIndex
    int columnIndex = this.columnIndex.get(columnName);

    // Remove the column values
    for (Collection<Double> list : this.values.asMap().values()) {
      ((List<Double>) list).remove(columnIndex);
    }

    // Remove the column
    this.columnIndex.remove(columnName);
  }

  @Override
  public void removeRow(final String rowName) {

    Objects.requireNonNull(rowName, "rowName argument cannot be null");

    if (!this.rowOrder.contains(rowName)) {
      throw new IllegalArgumentException("rowName does not exists: " + rowName);
    }

    // Add the default values
    this.values.removeAll(rowName);

    // Add the row name in the order of row
    this.rowOrder.remove(rowName);
  }

  //
  // Default value
  //

  @Override
  public double getDefaultValue() {

    return this.defaultValue;
  }

  @Override
  public void setDefaultValue(final double defaultValue) {

    this.defaultValue = defaultValue;
  }

  //
  // Object method
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

  @Override
  public boolean equals(Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof DenseExpressionMatrix)) {
      return false;
    }

    final DenseExpressionMatrix that = (DenseExpressionMatrix) o;

    return equal(this.values, that.values)
        && equal(this.columnIndex, that.columnIndex)
        && equal(this.rowOrder, that.rowOrder);
  }

  @Override
  public int hashCode() {

    return Objects.hash(this.values, this.columnIndex, this.rowOrder);
  }

}
