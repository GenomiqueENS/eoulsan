package fr.ens.biologie.genomique.eoulsan.bio;

import static fr.ens.biologie.genomique.eoulsan.util.Utils.equal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * This class define an abstract expression matrix.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractMatrix<E> implements Matrix<E> {

  static class BasicEntry<E> implements Entry<E> {

    final String rowName;
    final String columnName;
    final E value;

    @Override
    public String getRowName() {
      return this.rowName;
    }

    @Override
    public String getColumnName() {
      return this.columnName;
    }

    @Override
    public E getValue() {
      return this.value;
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.rowName, this.columnName, this.value);
    }

    @Override
    public boolean equals(Object o) {

      if (o == this) {
        return true;
      }

      if (!(o instanceof BasicEntry)) {
        return false;
      }

      @SuppressWarnings("unchecked")
      final BasicEntry<E> that = (BasicEntry<E>) o;

      return equal(this.rowName, that.rowName)
          && equal(this.columnName, that.columnName)
          && equal(this.value, that.value);
    }

    @Override
    public String toString() {
      return rowName + ':' + this.columnName + '=' + this.value;
    }

    BasicEntry(final String rowName, final String columnName, final E value) {
      this.rowName = rowName;
      this.columnName = columnName;
      this.value = value;
    }

  }

  @Override
  public Iterable<Entry<E>> values() {

    return new Iterable<Entry<E>>() {

      @Override
      public Iterator<Entry<E>> iterator() {

        final Iterator<String> rowNames = getRowNames().iterator();
        final List<String> columnNames = getColumnNames();

        return new Iterator<Entry<E>>() {

          Iterator<String> columnIterator = columnNames.iterator();
          String columnName;
          String rowName;
          boolean first = true;

          @Override
          public void remove() {
            throw new UnsupportedOperationException("remove");
          }

          @Override
          public boolean hasNext() {

            return rowNames.hasNext() || this.columnIterator.hasNext();
          }

          @Override
          public Entry<E> next() {

            if (first) {
              this.rowName = rowNames.next();
              first = false;
            }

            this.columnName = this.columnIterator.next();

            Entry<E> result = new BasicEntry<>(this.rowName, this.columnName,
                getValue(this.rowName, this.columnName));

            if (!this.columnIterator.hasNext() && rowNames.hasNext()) {
              this.rowName = rowNames.next();
              this.columnIterator = columnNames.iterator();
            }

            return result;
          }
        };
      }
    };
  }

  @Override
  public Iterable<Entry<E>> nonZeroValues() {

    return new Iterable<Entry<E>>() {

      private final Iterator<Entry<E>> values = values().iterator();

      @Override
      public Iterator<Entry<E>> iterator() {

        return new Iterator<Entry<E>>() {

          Entry<E> nextValue;

          @Override
          public void remove() {
            throw new UnsupportedOperationException("remove");
          }

          @Override
          public boolean hasNext() {

            while (values.hasNext()) {

              this.nextValue = values.next();
              if (this.nextValue.getValue() != getDefaultValue()) {
                return true;
              }
            }

            return false;
          }

          @Override
          public Entry<E> next() {
            return this.nextValue;
          }
        };
      }
    };
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
  public void add(final Matrix<E> matrix) {

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
  public void removeRows(final Collection<String> rowNames) {

    Objects.requireNonNull(rowNames);

    for (String rowName : rowNames) {
      removeRow(rowName);
    }
  }

  @Override
  public void removeColumns(Collection<String> columnNames) {

    Objects.requireNonNull(columnNames);

    for (String columnName : columnNames) {
      removeColumn(columnName);
    }
  }

  @Override
  public void retainRows(final Collection<String> rowNames) {

    Objects.requireNonNull(rowNames);

    List<String> rowsToRemove = new ArrayList<>(getRowNames());
    rowsToRemove.removeAll(rowNames);

    removeRows(rowsToRemove);
  }

  @Override
  public void retainColumns(final Collection<String> columnNames) {

    Objects.requireNonNull(columnNames);

    List<String> columnsToRemove = new ArrayList<>(getColumnNames());
    columnsToRemove.removeAll(columnNames);

    removeColumns(columnsToRemove);
  }

  @Override
  public int size() {
    return getRowCount() * getColumnCount();
  }

  @Override
  public boolean isEmpty() {
    return getColumnCount() == 0 && getRowCount() == 0;
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

      for (E value : getRowValues(rowName)) {
        sb.append('\t');
        sb.append(value);
      }
      sb.append('\n');
    }

    return sb.toString();
  }

}
