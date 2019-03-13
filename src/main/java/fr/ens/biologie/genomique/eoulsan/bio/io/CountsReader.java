package fr.ens.biologie.genomique.eoulsan.bio.io;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

/**
 * This interface define a counts reader.
 * @author Laurent Jourdren
 * @since 2.4
 */
public interface CountsReader extends Closeable {

  /**
   * Read the counts.
   * @return a map with the counts
   * @throws IOException if an error occurs while reading the file
   */
  Map<String, Integer> read() throws IOException;

}
