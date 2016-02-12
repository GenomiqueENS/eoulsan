package fr.ens.biologie.genomique.eoulsan.util.r;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * This class define a factory for creating a RExecutor object.
 * @author Laurent Jourdren
 * @since 2.0
 */
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;

public class RExecutorFactory {

  public enum Mode {

    LOCAL, RSERVE, DOCKER;

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

  public static RExecutor newRExecutor(final Mode mode,
      final String rServeServer, final String dockerImage,
      final File outputDirectory, final File temporaryDirectory)
          throws IOException {

    final String server = rServeServer == null
        ? EoulsanRuntime.getSettings().getRServeServerName() : rServeServer;
    final URI dockerConnection =
        EoulsanRuntime.getSettings().getDockerConnectionURI();

    return newRExecutor(mode, server, dockerConnection, dockerImage,
        outputDirectory, temporaryDirectory);
  }

  public static RExecutor newRExecutor(final Mode mode,
      final String rServeServer, final URI dockerConnection,
      final String dockerImage, final File outputDirectory,
      final File temporaryDirectory) throws IOException {

    if (mode != null) {

      switch (mode) {
      case LOCAL:
        return new ProcessRExecutor(outputDirectory, temporaryDirectory);

      case RSERVE:
        return new RserveRExecutor(outputDirectory, temporaryDirectory,
            rServeServer);

      case DOCKER:
        return new DockerRExecutor(outputDirectory, temporaryDirectory,
            dockerConnection, dockerImage);

      default:
        break;
      }
    }

    if (rServeServer != null) {
      return new RserveRExecutor(outputDirectory, temporaryDirectory,
          rServeServer);
    }

    if (dockerConnection != null && dockerImage != null) {
      return new DockerRExecutor(outputDirectory, temporaryDirectory,
          dockerConnection, dockerImage);
    }

    return new ProcessRExecutor(outputDirectory, temporaryDirectory);
  }

}
