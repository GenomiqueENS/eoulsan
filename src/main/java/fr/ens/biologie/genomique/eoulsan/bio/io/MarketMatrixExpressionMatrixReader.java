package fr.ens.biologie.genomique.eoulsan.bio.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.bio.DenseExpressionMatrix;
import fr.ens.biologie.genomique.eoulsan.bio.ExpressionMatrix;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.GuavaCompatibility;

/**
 * This class define a reader for matrix saved at Market Matrix format.
 * @author Laurent Jourdren
 * @since 2.2
 */
public class MarketMatrixExpressionMatrixReader
    implements ExpressionMatrixReader {

  static String MAGIC_KEY = "%%MatrixMarket ";

  private final InputStream is;

  /**
   * Get the row name of a row number
   * @param rowNumber row number
   * @return the row name
   */
  protected String getRowName(final int rowNumber) {
    return "row" + rowNumber;
  }

  /**
   * Get the column name of a column number
   * @param columnNumber column number
   * @return the column name
   */
  protected String getColumnName(final int columnNumber) {
    return "column" + columnNumber;
  }

  @Override
  public ExpressionMatrix read() throws IOException {

    return read(new DenseExpressionMatrix());
  }

  @Override
  public ExpressionMatrix read(final ExpressionMatrix matrix)
      throws IOException {

    Objects.requireNonNull(matrix, "matrix argument cannot be null");

    boolean first = true;

    int rowCount = -1;
    int columnCount = -1;
    int nonzero = -1;

    String line;
    int lineCount = 0;

    try (BufferedReader reader = FileUtils.createBufferedReader(is)) {

      while ((line = reader.readLine()) != null) {

        lineCount++;
        if (first) {

          if (!line.startsWith(MAGIC_KEY)) {
            throw new IOException("Invalid Market Matrice header: " + line);
          }

          List<String> fields = GuavaCompatibility.splitToList(
              Splitter.on(' ').trimResults().omitEmptyStrings(), line);

          if (fields.size() < 2) {
            throw new IOException("Invalid Market Matrice header: " + line);
          }

          if (!"matrix".equals(fields.get(1))) {
            throw new IOException("The reader only handle matrix files");
          }

          for (String s : fields.subList(2, fields.size() - 1)) {

            switch (s.toLowerCase()) {
            case "coordinate":
            case "real":
            case "integer":
            case "general":
              break;

            default:
              throw new IOException(
                  "The reader does not support qualifier: " + s);
            }
          }

          first = false;
          continue;
        }

        // Skip comments
        if (line.startsWith("%")) {
          continue;
        }

        // Throw an error if the line is too long
        if (line.length() > 1024) {
          throw new IOException(
              "Invalide line length (>1024), line#" + lineCount + ": " + line);
        }

        line = line.trim();

        // Skip empty lines
        if (line.isEmpty()) {
          continue;
        }

        List<String> fields =
            GuavaCompatibility.splitToList(Splitter.on(' '), line);
        if (fields.size() != 3) {
          throw new IOException(
              "3 values are expected line #" + lineCount + ": " + line);
        }

        int i;
        int j;
        double value;

        try {
          i = Integer.parseInt(fields.get(0));
          j = Integer.parseInt(fields.get(1));
          value = Double.parseDouble(fields.get(2));
        } catch (NumberFormatException e) {
          throw new IOException(
              "Invalid number format line #" + lineCount + ": " + line);
        }

        if (nonzero == -1) {
          rowCount = i;
          columnCount = j;
          nonzero = (int) value;

          // Fill row names
          for (int k = 1; k <= rowCount; k++) {
            matrix.addRow(getRowName(k));
          }

          // Fill column names
          for (int k = 1; k <= columnCount; k++) {
            matrix.addColumn(getColumnName(k));
          }

        } else {
          matrix.setValue(getRowName(i), getColumnName(j), value);
        }
      }
    }

    return matrix;
  }

  @Override
  public void close() throws IOException {

    this.is.close();
  }

  /**
   * Create an InputStream that can read GZipped files if filename ends with
   * ".gz" extension.
   * @param filename the name of file to read
   * @return a InputStream object
   * @throws IOException if an error occurs when opening the file
   */
  private static InputStream createInputstream(final String filename)
      throws IOException {

    if (filename.endsWith(".gz")) {

      return new GZIPInputStream(new FileInputStream(filename));
    }

    return new FileInputStream(filename);
  }

  /**
   * Create an InputStream that can read GZipped files if filename ends with
   * ".gz" extension.
   * @param file the file to read
   * @return a InputStream object
   * @throws IOException if an error occurs when opening the file
   */
  private static InputStream createInputstream(final File file)
      throws IOException {

    if (file.getName().endsWith(".gz")) {

      return new GZIPInputStream(new FileInputStream(file));
    }

    return new FileInputStream(file);
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param is InputStream to use
   */
  public MarketMatrixExpressionMatrixReader(final InputStream is) {

    Objects.requireNonNull(is, "is argument cannot be null");

    this.is = is;
  }

  /**
   * Public constructor. If the filename ends with ".gz" the file will be
   * uncompressed while reading.
   * @param file File to use
   * @throws IOException if an error occurs while opening the file
   */
  public MarketMatrixExpressionMatrixReader(final File file)
      throws IOException {

    Objects.requireNonNull(file, "file argument cannot be null");

    this.is = createInputstream(file);
  }

  /**
   * Public constructor. If the filename ends with ".gz" the file will be
   * uncompressed while reading.
   * @param filename File to use
   * @throws IOException if an error occurs while opening the file
   */
  public MarketMatrixExpressionMatrixReader(final String filename)
      throws IOException {

    Objects.requireNonNull(filename, "filename argument cannot be null");

    this.is = createInputstream(filename);

  }

}
