package fr.ens.biologie.genomique.eoulsan.bio.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.bio.DenseExpressionMatrix;
import fr.ens.biologie.genomique.eoulsan.bio.ExpressionMatrix;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.GuavaCompatibility;

/**
 * This class define an ExpressionMatrix reader for TSV format.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TSVExpressionMatrixReader implements ExpressionMatrixReader {

  private final InputStream is;

  @Override
  public ExpressionMatrix read() throws IOException {

    return read(new DenseExpressionMatrix());
  }

  @Override
  public ExpressionMatrix read(final ExpressionMatrix matrix)
      throws IOException {

    Objects.requireNonNull(matrix, "matrix argument cannot be null");

    String line;
    boolean first = true;
    Splitter splitter = Splitter.on('\t');
    List<String> columnNames = null;

    try (BufferedReader reader = FileUtils.createBufferedReader(this.is)) {

      while ((line = reader.readLine()) != null) {

        line = line.trim();

        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }

        List<String> fields = GuavaCompatibility.splitToList(splitter, line);

        if (first) {
          if (fields.size() == 1) {
            continue;
          }

          // Remove first (unused element)
          columnNames = new ArrayList<>(fields);
          columnNames.remove(0);

          first = false;
        } else {

          if (fields.size() > columnNames.size() + 1) {
            throw new IOException(
                "Found a line with invalid number of column: " + line);
          }

          Iterator<String> it = columnNames.iterator();
          String rowName = null;
          for (String s : fields) {
            if (rowName == null) {
              rowName = s;
            } else {
              matrix.setValue(rowName, it.next(), Double.parseDouble(s));
            }
          }
        }
      }

    }

    return matrix;
  }

  @Override
  public void close() throws IOException {

    this.is.close();
  }

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param is InputStream to use
   */
  public TSVExpressionMatrixReader(final InputStream is) {

    Objects.requireNonNull(is, "is argument cannot be null");

    this.is = is;
  }

  /**
   * Public constructor
   * @param file File to use
   */
  public TSVExpressionMatrixReader(final File file)
      throws FileNotFoundException {

    Objects.requireNonNull(file, "file argument cannot be null");

    this.is = new FileInputStream(file);
  }

  /**
   * Public constructor.
   * @param filename File to use
   */
  public TSVExpressionMatrixReader(final String filename)
      throws FileNotFoundException {

    Objects.requireNonNull(filename, "filename argument cannot be null");

    this.is = new FileInputStream(filename);

  }

}
