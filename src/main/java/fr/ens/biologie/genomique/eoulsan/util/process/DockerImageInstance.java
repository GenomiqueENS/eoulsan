package fr.ens.biologie.genomique.eoulsan.util.process;

import java.io.IOException;

/**
 * This interface define a Docker image instance.
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface DockerImageInstance extends SimpleProcess {

  /**
   * Progression handler interface.
   */
  interface ProgressHandler {

    /**
     * Update the progression.
     * @param progress value of the progression
     */
    void update(double progress);
  }

  /**
   * Pull an image if not exist.
   * @throws IOException if an error occurs while pulling the image
   */
  void pullImageIfNotExists() throws IOException;

  /**
   * Pull an image if not exist.
   * @param progress progress handler
   * @throws IOException if an error occurs while pulling the image
   */
  void pullImageIfNotExists(ProgressHandler progress) throws IOException;

}
