package fr.ens.biologie.genomique.eoulsan.bio;

import java.util.List;

public interface ExpressionMatrix {

  /**
   * Get the raw names.
   * @return the raw names
   */
  List<String> getRawNames();

  /**
   * Get the raw count.
   * @return the raw count
   */
  int getRawCount();

  /**
   * Get the column names.
   * @return the raw names
   */
  List<String> getColumnNames();

  /**
   * Get the column count.
   * @return the raw names
   */
  int getColumnCount();

  /**
   * Get the values of a column.
   * @param columnName the column name
   * @return a list with the values of the column
   */
  List<Double> getColumnValues(String columnName);

  /**
   * Get the raw values.
   * @param rawName the raw name
   * @return a list with the raw values
   */
  List<Double> getRawValues(String rawName);

  /**
   * Get a value of the matrix
   * @param rawName the raw name
   * @param columnName the column name
   * @return the value of the cell
   */
  Double getValue(String rawName, String columnName);

  /**
   * Test if a column exists
   * @param columnName the name of the column
   * @return true if the column exists
   */
  boolean containsColumn(String columnName);

  /**
   * Test if a raw exists
   * @param rawName the name of the raw
   * @return true if the raw exists
   */
  boolean containsraw(String rawName);

  /**
   * Set a value of the matrix.
   * @param rawName raw name
   * @param columnName column name
   * @param value the value to set
   */
  void setValue(String rawName, String columnName, double value);

  /**
   * Add new raws.
   * @param rawNames the names of the raw to add
   */
  void addRaws(List<String> rawNames);

  /**
   * Add new raws.
   * @param rawNames the names of the raw to add
   */
  void addRaws(String... rawNames);

  /**
   * Add a new raw.
   * @param rawName the name of the raw to add
   */
  void addRaw(String rawName);

  /**
   * Add a new columns.
   * @param columnNames the names of the new columns
   */
  void addColumns(List<String> columnNames);

  /**
   * Add a new columns.
   * @param columnNames the name of the new columns
   */
  void addColumns(String... columnNames);

  /**
   * Add a new column.
   * @param columnName the name of the new column
   */
  void addColumn(String columnName);

  /**
   * Add a matrix value to the current matrix
   * @param matrix
   */
  void add(ExpressionMatrix matrix);

  /**
   * Rename a column.
   * @param oldColumnName the old column name
   * @param newColumnName the new column name
   */
  void renameColumn(String oldColumnName, String newColumnName);

  /**
   * Remove a column of the matrix.
   * @param columnName the name of the column to remove
   */
  void removeColumn(String columnName);

  /**
   * Remove a raw of the matrix.
   * @param rawName the name of the raw to remove
   */
  void removeRaw(String rawName);

  /**
   * Get the default value of a cell.
   * @return the default value of a cell
   */
  double getDefaultValue();

  /**
   * Set the default value of a cell.
   * @param defaultValue the default value of a cell
   */
  void setDefaultValue(double defaultValue);

}