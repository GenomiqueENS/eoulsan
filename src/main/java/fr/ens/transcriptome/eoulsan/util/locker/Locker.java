package fr.ens.transcriptome.eoulsan.util.locker;

import java.io.IOException;

/**
 * This interface define an interface for the global execution locker.
 * @author Laurent Jourdren
 */
public interface Locker {

  /**
   * Wait the token and then lock the resource.
   * @throws IOException if an error occurs while locking the resource
   */
  void lock() throws IOException;

  /**
   * Unlock the resource.
   * @throws IOException if an error occurs while unlocking the resource
   */
  void unlock() throws IOException;

}
