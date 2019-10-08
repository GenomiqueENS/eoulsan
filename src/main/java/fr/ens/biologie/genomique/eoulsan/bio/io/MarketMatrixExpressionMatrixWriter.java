package fr.ens.biologie.genomique.eoulsan.bio.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import com.google.common.math.DoubleMath;

import fr.ens.biologie.genomique.eoulsan.bio.ExpressionMatrix;
import fr.ens.biologie.genomique.eoulsan.bio.Matrix;

/**
 * This class define a writer to save matrix saved at Market Matrix format.
 * @author Laurent Jourdren
 * @since 2.4
 */
public class MarketMatrixExpressionMatrixWriter
    implements ExpressionMatrixWriter {

  private final OutputStream os;

  @Override
  public void write(final ExpressionMatrix matrix) throws IOException {

    Objects.requireNonNull(matrix, "matrix argument cannot be null");

    write(matrix, matrix.getRowNames());
  }

  @Override
  public void write(final ExpressionMatrix matrix,
      final Collection<String> rowNamesToWrite) throws IOException {

    Objects.requireNonNull(matrix, "matrix argument cannot be null");
    Objects.requireNonNull(rowNamesToWrite,
        "rowNamesToWrite argument cannot be null");

    Set<String> rowNames = rowNamesToWrite instanceof Set
        ? (Set<String>) rowNamesToWrite : new HashSet<>(rowNamesToWrite);

    int entryCount = entryCount(matrix, rowNames);
    boolean intMatrix = entryCount < -1;
    entryCount = Math.abs(entryCount);

    try (Writer writer = new OutputStreamWriter(this.os)) {

      // Write header
      writer.write(MarketMatrixExpressionMatrixReader.MAGIC_KEY);
      writer.write("matrix coordinate "
          + (intMatrix ? "integer" : "real") + " general\n");

      // Write the size of the matrix
      writer.write(""
          + matrix.getRowCount() + ' ' + matrix.getColumnCount() + ' '
          + entryCount + '\n');

      Map<String, Integer> rowPositions = keyPositions(matrix.getRowNames());
      Map<String, Integer> columnPositions =
          keyPositions(matrix.getColumnNames());

      for (Matrix.Entry<Double> e : matrix.nonZeroValues()) {
        if (rowNames.contains(e.getRowName())) {
          String value =
              intMatrix ? "" + e.getValue().intValue() : "" + e.getValue();
          writer.write(""
              + rowPositions.get(e.getRowName()) + ' '
              + columnPositions.get(e.getColumnName()) + ' ' + value + '\n');
        }
      }
    }
  }

  /**
   * Get the indexes of the rows and columns.
   * @param entryNames entry names
   * @return a map with the indexes of the entries
   */
  private static Map<String, Integer> keyPositions(
      final List<String> entryNames) {

    final Map<String, Integer> result = new HashMap<>();
    int count = 1;

    for (String e : entryNames) {
      result.put(e, count++);
    }

    return result;
  }

  /**
   * Count the number of entries in the matrix.
   * @param matrix the matrix
   * @param rowNames the row names to write
   * @return the number of the entries in the matrix. If the result is negative
   *         the matrix is an integer matrix
   */
  private static int entryCount(ExpressionMatrix matrix, Set<String> rowNames) {

    int entryCount = 0;
    boolean intMatrix = true;

    for (Matrix.Entry<Double> e : matrix.nonZeroValues()) {

      if (rowNames.contains(e.getRowName())) {
        if (!intMatrix && DoubleMath.isMathematicalInteger(e.getValue())) {
          intMatrix = false;
        }
        entryCount++;
      }
    }

    return entryCount * (intMatrix ? -1 : 1);
  }

  @Override
  public void close() throws IOException {

    this.os.close();
  }

  /**
   * Create an OutputStream that can write GZipped files if filename ends with
   * ".gz" extension.
   * @param filename the file to read
   * @return a OutputStream object
   * @throws IOException if an error occurs when creating the file
   */
  private static OutputStream createOutputstream(final String filename)
      throws IOException {

    if (filename.endsWith(".gz")) {

      return new GZIPOutputStream(new FileOutputStream(filename));
    }

    return new FileOutputStream(filename);
  }

  /**
   * Create an OutputStream that can write GZipped files if filename ends with
   * ".gz" extension.
   * @param file the file to read
   * @return a OutputStream object
   * @throws IOException if an error occurs when creating the file
   */
  private static OutputStream createOutputstream(final File file)
      throws IOException {

    if (file.getName().endsWith(".gz")) {

      return new GZIPOutputStream(new FileOutputStream(file));
    }

    return new FileOutputStream(file);
  }

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param os InputStream to use
   */
  public MarketMatrixExpressionMatrixWriter(final OutputStream os) {

    Objects.requireNonNull(os, "os argument cannot be null");

    this.os = os;
  }

  /**
   * Public constructor
   * @param file File to use
   * @throws IOException if an error occurs when writing the file
   */
  public MarketMatrixExpressionMatrixWriter(final File file)
      throws IOException {

    Objects.requireNonNull(file, "file argument cannot be null");

    this.os = createOutputstream(file);
  }

  /**
   * Public constructor.
   * @param filename File to use
   * @throws IOException if an error occurs when writing the file
   */
  public MarketMatrixExpressionMatrixWriter(final String filename)
      throws IOException {

    Objects.requireNonNull(filename, "filename argument cannot be null");

    this.os = createOutputstream(filename);

  }

}
