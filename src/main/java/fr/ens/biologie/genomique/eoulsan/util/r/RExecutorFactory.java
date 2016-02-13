package fr.ens.biologie.genomique.eoulsan.util.r;

import java.io.File;
import java.io.IOException;

/**
 * This class define a factory for creating a RExecutor object.
 * @author Laurent Jourdren
 * @since 2.0
 */
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;

/**
 * This class define a factory to create RExecutor objects.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class RExecutorFactory {

  /**
   * Define a enum for the mode of RExecutor.
   */
  public enum Mode {

    LOCAL, RSERVE, DOCKER;

    /**
     * Parse a mode name and returns the mode.
     * @param name mode name
     * @return a mode object or null if the mode is not found
     */
    public static Mode parse(final String name) {

      if (name == null) {
        return null;
      }

      for (Mode m : values()) {

        if (m.name().toLowerCase().equals(name.toLowerCase().trim())) {
          return m;
        }
      }

      return null;
    }

  }

  /**
   * Create a new instance of RExecutor.
   * @param mode executor mode
   * @param rServeServer the Rserve server
   * @param dockerImage docker image
   * @param outputDirectory output directory
   * @param temporaryDirectory temporary directory
   * @return a new instance of RExecutor
   * @throws IOException if an error occurs while creating the instance of
   *           RExecutor
   */
  public static RExecutor newRExecutor(final Mode mode,
      final String rServeServer, final String dockerImage,
      final File outputDirectory, final File temporaryDirectory)
          throws IOException {

    final String server = rServeServer == null
        ? EoulsanRuntime.getSettings().getRServeServerName() : rServeServer;

    if (mode != null) {

      switch (mode) {
      case LOCAL:
        return new ProcessRExecutor(outputDirectory, temporaryDirectory);

      case RSERVE:
        return new RserveRExecutor(outputDirectory, temporaryDirectory, server);

      case DOCKER:
        return new DockerRExecutor(outputDirectory, temporaryDirectory,
            dockerImage);

      default:
        break;
      }
    }

    if (server != null) {
      return new RserveRExecutor(outputDirectory, temporaryDirectory, server);
    }

    if (dockerImage != null) {
      return new DockerRExecutor(outputDirectory, temporaryDirectory,
          dockerImage);
    }

    return new ProcessRExecutor(outputDirectory, temporaryDirectory);
  }

}
