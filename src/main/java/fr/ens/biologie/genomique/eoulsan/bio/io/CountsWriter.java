package fr.ens.biologie.genomique.eoulsan.bio.io;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

/**
 * This interface define a counts writer.
 * @author Laurent Jourdren
 * @since 2.4
 */
public interface CountsWriter extends Closeable {

  /**
   * Write the counts.
   * @param counts counts to write
   * @throws IOException if an error occurs while writing the file
   */
  void write(Map<String, Integer> counts) throws IOException;

}
