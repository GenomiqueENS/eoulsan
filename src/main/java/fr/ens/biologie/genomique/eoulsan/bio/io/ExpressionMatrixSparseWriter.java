package fr.ens.biologie.genomique.eoulsan.bio.io;

import static fr.ens.biologie.genomique.eoulsan.bio.io.BioCharsets.GFF_CHARSET;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Objects;

import fr.ens.biologie.genomique.eoulsan.bio.ExpressionMatrix;
import fr.ens.biologie.genomique.eoulsan.bio.ExpressionMatrix.Entry;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;

/**
 * This class define an ExpressionMatrix writer for sparse format.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ExpressionMatrixSparseWriter implements ExpressionMatrixWriter {

  private final Writer writer;

  @Override
  public void write(final ExpressionMatrix matrix) throws IOException {

    Objects.requireNonNull(matrix, "matrix argument cannot be null");

    // Write header
    this.writer.write("gene\tcell\tcount\n");

    for (Entry e : matrix.nonZeroValues()) {

      double d = e.getValue();

      if (!(Double.isNaN(d) || Double.isInfinite(d))
          && Math.floor(d) - d == 0.0) {

        int intValue = (int) d;

        this.writer.write(
            e.getRowName() + '\t' + e.getColumnName() + '\t' + intValue + '\n');

      } else {
        this.writer
            .write(e.getRowName() + '\t' + e.getColumnName() + '\t' + d + '\n');
      }

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
  public ExpressionMatrixSparseWriter(final Writer writer) {

    if (writer == null) {
      throw new NullPointerException("The writer is null.");
    }

    this.writer = writer;
  }

  /**
   * Public constructor.
   * @param os OutputStream to use
   */
  public ExpressionMatrixSparseWriter(final OutputStream os)
      throws FileNotFoundException {

    this.writer = FileUtils.createFastBufferedWriter(os, GFF_CHARSET);
  }

  /**
   * Public constructor.
   * @param outputFile file to use
   */
  public ExpressionMatrixSparseWriter(final File outputFile)
      throws IOException {

    this.writer = FileUtils.createFastBufferedWriter(outputFile, GFF_CHARSET);
  }

  /**
   * Public constructor.
   * @param outputFilename name of the file to use
   */
  public ExpressionMatrixSparseWriter(final String outputFilename)
      throws IOException {

    this.writer =
        FileUtils.createFastBufferedWriter(outputFilename, GFF_CHARSET);
  }

}
