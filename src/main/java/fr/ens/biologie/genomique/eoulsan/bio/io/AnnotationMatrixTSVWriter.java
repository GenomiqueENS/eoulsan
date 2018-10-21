package fr.ens.biologie.genomique.eoulsan.bio.io;

import static fr.ens.biologie.genomique.eoulsan.bio.io.BioCharsets.GFF_CHARSET;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Objects;

import fr.ens.biologie.genomique.eoulsan.bio.AnnotationMatrix;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;

/**
 * This class define an AnnotationMatrix writer for TSV format.
 * @author Laurent Jourdren
 * @since 2.4
 */
public class AnnotationMatrixTSVWriter implements AnnotationMatrixWriter {

  private final Writer writer;

  @Override
  public void write(final AnnotationMatrix matrix) throws IOException {

    Objects.requireNonNull(matrix, "matrix argument cannot be null");

    StringBuilder sb = new StringBuilder();

    for (String columnName : matrix.getColumnNames()) {
      sb.append('\t');
      sb.append(columnName);
    }
    sb.append('\n');
    this.writer.write(sb.toString());

    for (String rowName : matrix.getRowNames()) {
      sb.setLength(0);

      sb.append(rowName);

      for (String value : matrix.getRowValues(rowName)) {
        sb.append('\t');

        sb.append(
            value.indexOf(' ') != -1 ? StringUtils.doubleQuotes(value) : value);

      }
      sb.append('\n');
      this.writer.write((sb.toString()));
    }

    this.writer.close();
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param writer Writer to use
   */
  public AnnotationMatrixTSVWriter(final Writer writer) {

    if (writer == null) {
      throw new NullPointerException("The writer is null.");
    }

    this.writer = writer;
  }

  /**
   * Public constructor.
   * @param os OutputStream to use
   */
  public AnnotationMatrixTSVWriter(final OutputStream os)
      throws FileNotFoundException {

    this.writer = FileUtils.createFastBufferedWriter(os, GFF_CHARSET);
  }

  /**
   * Public constructor.
   * @param outputFile file to use
   */
  public AnnotationMatrixTSVWriter(final File outputFile) throws IOException {

    this.writer = FileUtils.createFastBufferedWriter(outputFile, GFF_CHARSET);
  }

  /**
   * Public constructor.
   * @param outputFilename name of the file to use
   */
  public AnnotationMatrixTSVWriter(final String outputFilename)
      throws IOException {

    this.writer =
        FileUtils.createFastBufferedWriter(outputFilename, GFF_CHARSET);
  }

}
