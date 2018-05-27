package fr.ens.biologie.genomique.eoulsan.bio.io;

import static fr.ens.biologie.genomique.eoulsan.bio.io.BioCharsets.GFF_CHARSET;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Objects;

import fr.ens.biologie.genomique.eoulsan.bio.ExpressionMatrix;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;

/**
 * This class define an ExpressionMatrix writer for TSV format.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ExpressionMatrixTSVWriter implements ExpressionMatrixWriter {

  private final Writer writer;

  @Override
  public void write(final ExpressionMatrix matrix) throws IOException {

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

      for (Double value : matrix.getRowValues(rowName)) {
        sb.append('\t');

        double d = value;
        if (!(Double.isNaN(d) || Double.isInfinite(d))
            && Math.floor(d) - d == 0.0) {
          sb.append((int) d);
        } else {
          sb.append(value);
        }
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
  public ExpressionMatrixTSVWriter(final Writer writer) {

    if (writer == null) {
      throw new NullPointerException("The writer is null.");
    }

    this.writer = writer;
  }

  /**
   * Public constructor.
   * @param os OutputStream to use
   */
  public ExpressionMatrixTSVWriter(final OutputStream os)
      throws FileNotFoundException {

    this.writer = FileUtils.createFastBufferedWriter(os, GFF_CHARSET);
  }

  /**
   * Public constructor.
   * @param outputFile file to use
   */
  public ExpressionMatrixTSVWriter(final File outputFile) throws IOException {

    this.writer = FileUtils.createFastBufferedWriter(outputFile, GFF_CHARSET);
  }

  /**
   * Public constructor.
   * @param outputFilename name of the file to use
   */
  public ExpressionMatrixTSVWriter(final String outputFilename)
      throws IOException {

    this.writer =
        FileUtils.createFastBufferedWriter(outputFilename, GFF_CHARSET);
  }

}
