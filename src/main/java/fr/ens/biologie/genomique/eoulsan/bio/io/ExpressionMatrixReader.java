package fr.ens.biologie.genomique.eoulsan.bio.io;

import java.io.Closeable;
import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.bio.ExpressionMatrix;

/**
 * This interface define an ExpressionMatrix reader.
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface ExpressionMatrixReader extends Closeable {

  /**
   * Read an ExpressionMatrix object.
   * @return an ExpressionMatrix object
   * @throws IOException if an error occurs while reading the file
   */
  ExpressionMatrix read() throws IOException;

  /**
   * Read an ExpressionMatrix object.
   * @param matrix matrix to use for saving data loaded
   * @return an ExpressionMatrix object
   * @throws IOException if an error occurs while reading the file
   */
  ExpressionMatrix read(ExpressionMatrix matrix) throws IOException;

}