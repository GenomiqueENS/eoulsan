package fr.ens.biologie.genomique.eoulsan.bio.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.bio.AnnotationMatrix;
import fr.ens.biologie.genomique.eoulsan.bio.DenseAnnotationMatrix;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.GuavaCompatibility;

/**
 * This class define an AnnotationMatrix reader for TSV format.
 * @author Laurent Jourdren
 * @since 2.4
 */
public class AnnotationMatrixTSVReader implements AnnotationMatrixReader {

  private InputStream is;

  private static final String SEPARATOR = "\t";
  private boolean removeQuotes = true;

  //
  // Getters
  //

  /**
   * Test if quotes of the fields must be removed
   * @return Returns the removeQuotes
   */
  public boolean isRemoveQuotes() {
    return this.removeQuotes;
  }

  @Override
  public AnnotationMatrix read() throws IOException {

    return read(new DenseAnnotationMatrix());
  }

  @Override
  public AnnotationMatrix read(AnnotationMatrix matrix) throws IOException {

    Objects.requireNonNull(matrix, "matrix argument cannot be null");

    String line;
    boolean first = true;
    Splitter splitter = Splitter.on(SEPARATOR);
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
              matrix.setValue(rowName, it.next(), removeDoubleQuotesAndTrim(s));
            }
          }
        }
      }

    }
    return matrix;
  }

  //
  // Other mmethods
  //

  //
  // Utility methods
  //

  /**
   * Remove double quote from a string.
   * @param s The string parameter
   * @return a string without double quotes
   */
  private static String removeDoubleQuotes(final String s) {

    if (s == null) {
      return null;
    }

    String result = s;

    if (result.startsWith("\"")) {
      result = result.substring(1);
    }
    if (result.endsWith("\"")) {
      result = result.substring(0, result.length() - 1);
    }

    return result;
  }

  /**
   * Remove double quote and trim a string.
   * @param s The string parameter
   * @return a string without space and double quotes
   */
  private static String removeDoubleQuotesAndTrim(final String s) {

    if (s == null) {
      return null;
    }

    return removeDoubleQuotes(s.trim());
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param filename file to read
   * @throws IOException if an error occurs while reading the file or if the
   *           file is null.
   */
  public AnnotationMatrixTSVReader(final String filename) throws IOException {

    this(new File(filename), false);
  }

  /**
   * Public constructor.
   * @param filename file to read
   * @param noHeader true if there is no header for column names
   * @throws IOException if an error occurs while reading the file or if the
   *           file is null.
   */
  public AnnotationMatrixTSVReader(final String filename,
      final boolean noHeader) throws IOException {

    this(new File(filename), noHeader);
  }

  /**
   * Public constructor.
   * @param file file to read
   * @throws IOException if an error occurs while reading the file or if the
   *           file is null.
   */
  public AnnotationMatrixTSVReader(final File file) throws IOException {

    this(file, false);
  }

  /**
   * Public constructor.
   * @param file file to read
   * @param noHeader true if there is no header for column names
   * @throws IOException if an error occurs while reading the file or if the
   *           file is null.
   */
  public AnnotationMatrixTSVReader(final File file, final boolean noHeader)
      throws IOException {

    if (file == null) {
      throw new IOException("No file to load");
    }

    this.is = new FileInputStream(file);
  }

  /**
   * Public constructor
   * @param is Input stream to read
   * @throws IOException if the stream is null
   */
  public AnnotationMatrixTSVReader(final InputStream is) throws IOException {

    this(is, false);
  }

  /**
   * Public constructor
   * @param is Input stream to read
   * @param noHeader true if there is no header for column names
   * @throws IOException if the stream is null
   */
  public AnnotationMatrixTSVReader(final InputStream is, final boolean noHeader)
      throws IOException {

    this.is = is;
  }

}
