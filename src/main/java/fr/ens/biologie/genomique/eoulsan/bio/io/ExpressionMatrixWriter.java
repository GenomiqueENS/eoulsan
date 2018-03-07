package fr.ens.biologie.genomique.eoulsan.bio.io;

import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.bio.ExpressionMatrix;

/**
 * This interface define an ExpressionMatrix writer.
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface ExpressionMatrixWriter {

  /**
   * Write an ExpressionMatrix object.
   * @param matrix matrix to write
   * @throws IOException if an error occurs while writing the file
   */
  void write(ExpressionMatrix matrix) throws IOException;

}