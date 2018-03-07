package fr.ens.biologie.genomique.eoulsan.bio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class define a sparse expression matrix.
 * @author Laurent Jourdren
 * @since 2.2
 */
public class SparseExpressionMatrix extends AbstractExpressionMatrix {

  private static final double DEFAULT_DEFAULT_VALUE = 0.0;

  private final Map<Long, Double> values = new HashMap<>();
  private final Map<String, Integer> rowNames = new LinkedHashMap<>();
  private final Map<String, Integer> columnNames = new LinkedHashMap<>();
  private int rowCount;
  private int columnCount;
  private final Double defaultValue;

  @Override
  public List<String> getRowNames() {

    return Collections
        .unmodifiableList(new ArrayList<>(this.rowNames.keySet()));
  }

  @Override
  public int getRowCount() {

    return this.rowNames.size();
  }

  @Override
  public List<String> getColumnNames() {

    return Collections
        .unmodifiableList(new ArrayList<>(this.columnNames.keySet()));
  }

  @Override
  public int getColumnCount() {

    return this.columnNames.size();
  }

  @Override
  public List<Double> getColumnValues(final String columnName) {

    final List<Double> result = new ArrayList<>(this.rowNames.size());

    final Integer columnId = getColumnId(columnName);

    for (Map.Entry<String, Integer> e : this.rowNames.entrySet()) {

      Long id = getCellId(e.getValue(), columnId);
      result.add(getValue(id));
    }

    return result;
  }

  @Override
  public List<Double> getRowValues(final String rowName) {

    final List<Double> result = new ArrayList<>(this.columnNames.size());

    final Integer rowId = getRowId(rowName);

    for (Map.Entry<String, Integer> e : this.columnNames.entrySet()) {

      Long id = getCellId(rowId, e.getValue());
      result.add(getValue(id));
    }

    return result;
  }

  @Override
  public Double getValue(final String rowName, final String columnName) {

    return getValue(getCellId(getRowId(rowName), getColumnId(columnName)));
  }

  @Override
  public boolean containsColumn(final String columnName) {

    Objects.requireNonNull(columnName, "columnName argument cannot be null");

    return this.columnNames.containsKey(columnName);
  }

  @Override
  public boolean containsRow(final String rowName) {

    Objects.requireNonNull(rowName, "rowName argument cannot be null");

    return this.rowNames.containsKey(rowName);
  }

  @Override
  public void setValue(final String rowName, final String columnName,
      double value) {

    Objects.requireNonNull(rowName, "rowName argument cannot be null");
    Objects.requireNonNull(columnName, "columnName argument cannot be null");

    Integer rowId = this.rowNames.get(rowName);
    if (rowId == null) {
      addRow(rowName);
      rowId = Integer.valueOf(this.rowCount - 1);
    }

    Integer columnId = this.columnNames.get(columnName);
    if (columnId == null) {
      addColumn(columnName);
      columnId = Integer.valueOf(this.columnCount - 1);
    }

    this.values.put(getCellId(rowId, columnId), value);
  }

  @Override
  public void addRow(final String rowName) {

    Objects.requireNonNull(rowName, "rowName argument cannot be null");

    if (this.rowNames.containsKey(rowName)) {
      return;
    }

    this.rowNames.put(rowName, this.rowCount++);
  }

  @Override
  public void addColumn(final String columnName) {

    Objects.requireNonNull(columnName, "columnName argument cannot be null");

    if (this.columnNames.containsKey(columnName)) {
      return;
    }

    this.columnNames.put(columnName, this.columnCount++);

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

    Integer columnId = getColumnId(oldColumnName);
    this.columnNames.remove(oldColumnName);
    this.columnNames.put(newColumnName, columnId);
  }

  @Override
  public void removeColumn(final String columnName) {

    Objects.requireNonNull(columnName, "columnName argument cannot be null");

    if (!this.columnNames.containsKey(columnName)) {
      throw new IllegalArgumentException(
          "columnName does not exists: " + columnName);
    }

    // Remove the values
    Integer columnId = getColumnId(columnName);
    for (Map.Entry<String, Integer> e : this.rowNames.entrySet()) {

      Long id = getCellId(e.getValue(), columnId);

      if (this.values.containsKey(id)) {
        this.values.remove(id);
      }
    }

    // Remove the column
    this.columnNames.remove(columnName);
  }

  @Override
  public void removeRow(String rowName) {

    Objects.requireNonNull(rowName, "rowName argument cannot be null");

    if (!this.rowNames.containsKey(rowName)) {
      throw new IllegalArgumentException("rowName does not exists: " + rowName);
    }

    // Remove the values
    Integer rowId = getRowId(rowName);
    for (Map.Entry<String, Integer> e : this.columnNames.entrySet()) {

      Long id = getCellId(rowId, e.getValue());

      if (this.values.containsKey(id)) {
        this.values.remove(id);
      }
    }

    // Remove the column
    this.rowNames.remove(rowName);

  }

  @Override
  public double getDefaultValue() {

    return this.defaultValue;
  }

  //
  // Internal methods
  //

  private Integer getColumnId(final String columnName) {

    Objects.requireNonNull(columnName, "columnName argument cannot be null");

    Integer result = this.columnNames.get(columnName);

    if (result == null) {
      throw new IllegalArgumentException("Unknown column name: " + columnName);
    }

    return result;
  }

  private Integer getRowId(final String rowName) {

    Objects.requireNonNull(rowName, "rowName argument cannot be null");

    Integer result = this.rowNames.get(rowName);

    if (result == null) {
      throw new IllegalArgumentException("Unknown row name: " + rowName);
    }

    return result;
  }

  private static Long getCellId(final Integer rowId, final Integer columnId) {

    return Long.valueOf(rowId.longValue() << 32) + (columnId.longValue());
  }

  private Double getValue(final Long id) {

    Double result = this.values.get(id);

    return result != null ? result : this.defaultValue;
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param defaultValue the default value of the matrix
   */
  public SparseExpressionMatrix() {
    this(DEFAULT_DEFAULT_VALUE);
  }

  /**
   * Public constructor.
   * @param defaultValue the default value of the matrix
   */
  public SparseExpressionMatrix(final double defaultValue) {
    this.defaultValue = defaultValue;
  }

}
