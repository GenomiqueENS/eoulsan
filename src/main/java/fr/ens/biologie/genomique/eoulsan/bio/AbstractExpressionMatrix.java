package fr.ens.biologie.genomique.eoulsan.bio;

import static fr.ens.biologie.genomique.eoulsan.util.Utils.equal;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * This class define an abstract expression matrix.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractExpressionMatrix implements ExpressionMatrix {

  static class BasicEntry implements Entry {

    final String rowName;
    final String columnName;
    final Double value;

    @Override
    public String getRowName() {
      return this.rowName;
    }

    @Override
    public String getColumnName() {
      return this.columnName;
    }

    @Override
    public Double getValue() {
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

      final BasicEntry that = (BasicEntry) o;

      return equal(this.rowName, that.rowName)
          && equal(this.columnName, that.columnName)
          && equal(this.value, that.value);
    }

    @Override
    public String toString() {
      return rowName + ':' + this.columnName + '=' + this.value;
    }

    BasicEntry(final String rowName, final String columnName,
        final Double value) {
      this.rowName = rowName;
      this.columnName = columnName;
      this.value = value;
    }

  }

  @Override
  public Iterable<Entry> values() {

    return new Iterable<Entry>() {

      @Override
      public Iterator<Entry> iterator() {

        final Iterator<String> rowNames = getRowNames().iterator();
        final List<String> columnNames = getColumnNames();

        return new Iterator<Entry>() {

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
          public Entry next() {

            if (first) {
              this.rowName = rowNames.next();
              first = false;
            }

            this.columnName = this.columnIterator.next();

            Entry result = new BasicEntry(this.rowName, this.columnName,
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
  public Iterable<Entry> nonZeroValues() {

    return new Iterable<Entry>() {

      private Iterator<Entry> values = values().iterator();

      @Override
      public Iterator<Entry> iterator() {

        return new Iterator<Entry>() {

          Entry nextValue;

          @Override
          public void remove() {
            throw new UnsupportedOperationException("remove");
          }

          @Override
          public boolean hasNext() {

            while (values.hasNext()) {

              this.nextValue = values.next();
              if (this.nextValue.getValue().doubleValue() != 0.0) {
                return true;
              }
            }

            return false;
          }

          @Override
          public Entry next() {
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
  public int size() {
    return getRowCount() * getColumnCount();
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
