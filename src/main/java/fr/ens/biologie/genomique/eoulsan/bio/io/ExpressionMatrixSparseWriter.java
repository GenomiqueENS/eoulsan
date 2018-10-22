package fr.ens.biologie.genomique.eoulsan.bio.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Objects;

import fr.ens.biologie.genomique.eoulsan.bio.ExpressionMatrix;
import fr.ens.biologie.genomique.eoulsan.bio.Matrix;

/**
 * This class define an ExpressionMatrix writer for sparse format.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ExpressionMatrixSparseWriter implements ExpressionMatrixWriter {

  private final OutputStream os;

  @Override
  public void write(final ExpressionMatrix matrix) throws IOException {

    Objects.requireNonNull(matrix, "matrix argument cannot be null");

    try (Writer writer = new OutputStreamWriter(this.os)) {

      // Write header
      writer.write("gene\tcell\tcount\n");

      for (Matrix.Entry<Double> e : matrix.nonZeroValues()) {

        double d = e.getValue();

        if (!(Double.isNaN(d) || Double.isInfinite(d))
            && Math.floor(d) - d == 0.0) {

          int intValue = (int) d;

          writer.write(e.getRowName()
              + '\t' + e.getColumnName() + '\t' + intValue + '\n');

        } else {
          writer.write(
              e.getRowName() + '\t' + e.getColumnName() + '\t' + d + '\n');
        }

      }
    }
  }

  @Override
  public void close() throws IOException {

    this.os.close();
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param os OutputStream to use
   */
  public ExpressionMatrixSparseWriter(final OutputStream os)
      throws FileNotFoundException {

    Objects.requireNonNull(os, "os argument cannot be null");

    this.os = os;
  }

  /**
   * Public constructor.
   * @param outputFile file to use
   */
  public ExpressionMatrixSparseWriter(final File outputFile)
      throws IOException {

    Objects.requireNonNull(outputFile, "os argument cannot be null");

    this.os = new FileOutputStream(outputFile);
  }

  /**
   * Public constructor.
   * @param outputFilename name of the file to use
   */
  public ExpressionMatrixSparseWriter(final String outputFilename)
      throws IOException {

    Objects.requireNonNull(outputFilename, "os argument cannot be null");

    this.os = new FileOutputStream(outputFilename);
  }

}
