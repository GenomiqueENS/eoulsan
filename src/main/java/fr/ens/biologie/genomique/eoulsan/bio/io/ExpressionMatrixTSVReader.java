package fr.ens.biologie.genomique.eoulsan.bio.io;

import static fr.ens.biologie.genomique.eoulsan.bio.io.BioCharsets.GFF_CHARSET;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.python.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.bio.DenseExpressionMatrix;
import fr.ens.biologie.genomique.eoulsan.bio.ExpressionMatrix;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;

/**
 * This class define an ExpressionMatrix reader for TSV format.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ExpressionMatrixTSVReader implements ExpressionMatrixReader {

  private final BufferedReader reader;

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

    while ((line = reader.readLine()) != null) {

      line = line.trim();

      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }

      List<String> fields = splitter.splitToList(line);

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

    // Close the reader
    this.reader.close();

    return matrix;
  }

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param is InputStream to use
   */
  public ExpressionMatrixTSVReader(final InputStream is) {

    if (is == null) {
      throw new NullPointerException("InputStream is null");
    }

    this.reader = new BufferedReader(new InputStreamReader(is, GFF_CHARSET));
  }

  /**
   * Public constructor
   * @param file File to use
   */
  public ExpressionMatrixTSVReader(final File file)
      throws FileNotFoundException {

    if (file == null) {
      throw new NullPointerException("File is null");
    }

    this.reader = FileUtils.createBufferedReader(file, GFF_CHARSET);
  }

  /**
   * Public constructor.
   * @param filename File to use
   */
  public ExpressionMatrixTSVReader(final String filename)
      throws FileNotFoundException {

    this.reader = FileUtils.createBufferedReader(filename, GFF_CHARSET);
  }

}
