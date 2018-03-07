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
  private final Set<String> rawOrder = new LinkedHashSet<>();
  private Double defaultValue = 0.0;

  //
  // Getters
  //

  @Override
  public List<String> getRawNames() {

    return Collections.unmodifiableList(new ArrayList<>(this.rawOrder));
  }

  @Override
  public int getRawCount() {

    return this.rawOrder.size();
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

    List<Double> result = new ArrayList<>(this.rawOrder.size());

    for (String rawName : this.rawOrder) {
      result.add(getValue(rawName, columnName));
    }

    return Collections.unmodifiableList(result);
  }

  @Override
  public List<Double> getRawValues(final String rawName) {

    Objects.requireNonNull(rawName, "rawName argument cannot be null");

    return (List<Double>) this.values.get(rawName);
  }

  @Override
  public Double getValue(final String rawName, final String columnName) {

    Objects.requireNonNull(rawName, "rawName argument cannot be null");
    Objects.requireNonNull(columnName, "columnName argument cannot be null");

    if (!this.columnIndex.containsKey(columnName)) {
      throw new IllegalArgumentException("Unknown column name: " + columnName);
    }

    if (!this.rawOrder.contains(rawName)) {
      throw new IllegalArgumentException("Unknown raw name: " + rawName);
    }

    return getRawValues(rawName).get(this.columnIndex.get(columnName));
  }

  @Override
  public boolean containsColumn(final String columnName) {

    return this.columnIndex.containsKey(columnName);
  }

  @Override
  public boolean containsraw(final String rawName) {

    if (this.columnIndex.size() == 0) {
      return this.rawOrder.contains(rawName);
    }

    return this.values.containsKey(rawName);
  }

  //
  // Setters
  //

  @Override
  public void setValue(final String rawName, final String columnName,
      final double value) {

    Objects.requireNonNull(rawName, "rawName argument cannot be null");
    Objects.requireNonNull(columnName, "columnName argument cannot be null");

    // Check if a column must be added
    if (!this.columnIndex.containsKey(columnName)) {
      addColumn(columnName);
    }

    // Check if a raw must be added
    if (!this.values.containsKey(rawName)) {
      addRaw(rawName);
    }

    // Set the value
    ((List<Double>) this.values.get(rawName))
        .set(this.columnIndex.get(columnName), value);
  }

  @Override
  public void addRaws(final List<String> rawNames) {

    Objects.requireNonNull("rawNames argument cannot be null");

    for (String rawName : rawNames) {
      addRaw(rawName);
    }
  }

  @Override
  public void addRaws(final String... rawNames) {

    Objects.requireNonNull("rawNames argument cannot be null");

    for (String rawName : rawNames) {
      addRaw(rawName);
    }
  }

  @Override
  public void addRaw(final String rawName) {

    Objects.requireNonNull(rawName, "rawName argument cannot be null");

    if (this.rawOrder.contains(rawName)) {
      return;
    }

    // Add the default values
    this.values.putAll(rawName,
        Collections.nCopies(this.columnIndex.size(), this.defaultValue));

    // Add the raw name in the order of raw
    this.rawOrder.add(rawName);
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

    if (this.columnIndex.size() == 1 && !this.rawOrder.isEmpty()) {

      // Fill existing empty raws
      for (String rawName : this.rawOrder) {
        this.values.put(rawName, this.defaultValue);
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
    List<String> newRawNames = matrix.getRawNames();

    // Add the valueq
    for (String rawName : newRawNames) {
      for (String columnName : newColumnNames) {
        setValue(rawName, columnName, matrix.getValue(rawName, columnName));
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
  public void removeRaw(final String rawName) {

    Objects.requireNonNull(rawName, "rawName argument cannot be null");

    if (!this.rawOrder.contains(rawName)) {
      throw new IllegalArgumentException("rawName does not exists: " + rawName);
    }

    // Add the default values
    this.values.removeAll(rawName);

    // Add the raw name in the order of raw
    this.rawOrder.remove(rawName);
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

    for (String rawName : getRawNames()) {

      sb.append(rawName);

      for (Double value : getRawValues(rawName)) {
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
        && equal(this.rawOrder, that.rawOrder);
  }

  @Override
  public int hashCode() {

    return Objects.hash(this.values, this.columnIndex, this.rawOrder);
  }

}
