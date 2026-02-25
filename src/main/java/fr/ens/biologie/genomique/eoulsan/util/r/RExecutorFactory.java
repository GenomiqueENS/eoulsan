package fr.ens.biologie.genomique.eoulsan.util.r;

import fr.ens.biologie.genomique.eoulsan.Globals;
import java.io.File;
import java.io.IOException;

/**
 * This class define a factory to create RExecutor objects.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class RExecutorFactory {

  /** Define a enum for the mode of RExecutor. */
  public enum Mode {
    PROCESS,
    RSERVE,
    DOCKER;

    /**
     * Parse a mode name and returns the mode.
     *
     * @param name mode name
     * @return a mode object or null if the mode is not found
     */
    public static Mode parse(final String name) {

      if (name == null) {
        return null;
      }

      for (Mode m : values()) {

        if (m.name()
            .toLowerCase(Globals.DEFAULT_LOCALE)
            .equals(name.toLowerCase(Globals.DEFAULT_LOCALE).trim())) {
          return m;
        }
      }

      return null;
    }
  }

  /**
   * Create a new instance of RExecutor.
   *
   * @param mode executor mode
   * @param rServeServer the Rserve server
   * @param dockerImage docker image
   * @param outputDirectory output directory
   * @param temporaryDirectory temporary directory
   * @return a new instance of RExecutor
   * @throws IOException if an error occurs while creating the instance of RExecutor
   */
  public static RExecutor newRExecutor(
      final Mode mode,
      final String rServeServer,
      final String dockerImage,
      final File outputDirectory,
      final File temporaryDirectory)
      throws IOException {

    if (mode != null) {

      switch (mode) {
        case PROCESS:
          return new ProcessRExecutor(outputDirectory, temporaryDirectory);

        case RSERVE:
          return new RserveRExecutor(outputDirectory, temporaryDirectory, rServeServer);

        case DOCKER:
          return new DockerRExecutor(outputDirectory, temporaryDirectory, dockerImage);

        default:
          break;
      }
    }

    if (rServeServer != null) {
      return new RserveRExecutor(outputDirectory, temporaryDirectory, rServeServer);
    }

    if (dockerImage != null) {
      return new DockerRExecutor(outputDirectory, temporaryDirectory, dockerImage);
    }

    return new ProcessRExecutor(outputDirectory, temporaryDirectory);
  }
}
