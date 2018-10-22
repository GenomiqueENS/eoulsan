package fr.ens.biologie.genomique.eoulsan.bio.io;

import java.io.Closeable;
import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.bio.AnnotationMatrix;

/**
 * This interface define an AnnotationMatrix writer.
 * @author Laurent Jourdren
 * @since 2.4
 */
public interface AnnotationMatrixWriter extends Closeable {

  /**
   * Write an AnnotationMatrix object.
   * @param matrix matrix to write
   * @throws IOException if an error occurs while writing the file
   */
  void write(AnnotationMatrix matrix) throws IOException;

}
