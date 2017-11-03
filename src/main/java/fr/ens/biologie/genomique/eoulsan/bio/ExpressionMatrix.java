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
public class ExpressionMatrix {

  private final Multimap<String, Double> values = ArrayListMultimap.create();
  private final Map<String, Integer> columnIndex = new HashMap<>();
  private final Set<String> rawOrder = new LinkedHashSet<>();
  private Double defaultValue = 0.0;

  //
  // Getters
  //

  /**
   * Get the raw names.
   * @return the raw names
   */
  public List<String> getRawNames() {

    return Collections.unmodifiableList(new ArrayList<>(this.rawOrder));
  }

  /**
   * Get the raw count.
   * @return the raw count
   */
  public int getRawCount() {

    return this.rawOrder.size();
  }

  /**
   * Get the column names.
   * @return the raw names
   */
  public List<String> getColumnNames() {

    String[] result = new String[this.columnIndex.size()];

    for (Map.Entry<String, Integer> e : this.columnIndex.entrySet()) {
      result[e.getValue()] = e.getKey();
    }

    return Collections.unmodifiableList(Arrays.asList(result));
  }

  /**
   * Get the column count.
   * @return the raw names
   */
  public int getColumnCount() {

    return this.columnIndex.size();
  }

  /**
   * Get the values of a column.
   * @param columnName the column name
   * @return a list with the values of the column
   */
  public List<Double> getColumnValues(final String columnName) {

    Objects.requireNonNull(columnName, "columnName argument cannot be null");

    List<Double> result = new ArrayList<>(this.rawOrder.size());

    for (String rawName : this.rawOrder) {
      result.add(getValue(rawName, columnName));
    }

    return Collections.unmodifiableList(result);
  }

  /**
   * Get the raw values.
   * @param rawName the raw name
   * @return a list with the raw values
   */
  public List<Double> getRawValues(final String rawName) {

    Objects.requireNonNull(rawName, "rawName argument cannot be null");

    return (List<Double>) this.values.get(rawName);
  }

  /**
   * Get a value of the matrix
   * @param rawName the raw name
   * @param columnName the column name
   * @return the value of the cell
   */
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

  /**
   * Test if a column exists
   * @param columnName the name of the column
   * @return true if the column exists
   */
  public boolean containsColumn(final String columnName) {

    return this.columnIndex.containsKey(columnName);
  }

  /**
   * Test if a raw exists
   * @param rawName the name of the raw
   * @return true if the raw exists
   */
  public boolean containsraw(final String rawName) {

    if (this.columnIndex.size() == 0) {
      return this.rawOrder.contains(rawName);
    }

    return this.values.containsKey(rawName);
  }

  //
  // Setters
  //

  /**
   * Set a value of the matrix.
   * @param rawName raw name
   * @param columnName column name
   * @param value the value to set
   */
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

  /**
   * Add new raws.
   * @param rawNames the names of the raw to add
   */
  public void addRaws(final List<String> rawNames) {

    Objects.requireNonNull("rawNames argument cannot be null");

    for (String rawName : rawNames) {
      addRaw(rawName);
    }
  }

  /**
   * Add new raws.
   * @param rawNames the names of the raw to add
   */
  public void addRaws(final String... rawNames) {

    Objects.requireNonNull("rawNames argument cannot be null");

    for (String rawName : rawNames) {
      addRaw(rawName);
    }
  }

  /**
   * Add a new raw.
   * @param rawName the name of the raw to add
   */
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

  /**
   * Add a new columns.
   * @param columnNames the names of the new columns
   */
  public void addColumns(final List<String> columnNames) {

    Objects.requireNonNull(columnNames, "columnName argument cannot be null");

    for (String columnName : columnNames) {
      addColumn(columnName);
    }
  }

  /**
   * Add a new columns.
   * @param columnNames the name of the new columns
   */
  public void addColumns(final String... columnNames) {

    Objects.requireNonNull(columnNames);

    for (String columnName : columnNames) {
      addColumn(columnName);
    }
  }

  /**
   * Add a new column.
   * @param columnName the name of the new column
   */
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

  /**
   * Add a matrix value to the current matrix
   * @param matrix
   */
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

  /**
   * Rename a column.
   * @param oldColumnName the old column name
   * @param newColumnName the new column name
   */
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

  /**
   * Remove a column of the matrix.
   * @param columnName the name of the column to remove
   */
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

  /**
   * Remove a raw of the matrix.
   * @param rawName the name of the raw to remove
   */
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

  /**
   * Get the default value of a cell.
   * @return the default value of a cell
   */
  public double getDefaultValue() {

    return this.defaultValue;
  }

  /**
   * Set the default value of a cell.
   * @param defaultValue the default value of a cell
   */
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

    if (!(o instanceof ExpressionMatrix)) {
      return false;
    }

    final ExpressionMatrix that = (ExpressionMatrix) o;

    return equal(this.values, that.values)
        && equal(this.columnIndex, that.columnIndex)
        && equal(this.rawOrder, that.rawOrder);
  }

  @Override
  public int hashCode() {

    return Objects.hash(this.values, this.columnIndex, this.rawOrder);
  }

}
