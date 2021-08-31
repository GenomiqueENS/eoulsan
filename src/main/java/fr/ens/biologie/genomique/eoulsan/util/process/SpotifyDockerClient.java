package fr.ens.biologie.genomique.eoulsan.util.process;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.Image;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.log.EoulsanRuntimeLogger;
import fr.ens.biologie.genomique.eoulsan.log.GenericLogger;
import fr.ens.biologie.genomique.eoulsan.util.CollectionUtils;

/**
 * This class define a Docker client using the Spotify Docker client library.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class SpotifyDockerClient implements DockerClient {

  private DefaultDockerClient client;
  private final GenericLogger logger;

  @Override
  public void initialize(URI dockerConnectionURI) throws IOException {

    synchronized (this) {

      if (this.client != null) {
        return;
      }

      final URI dockerConnection =
          EoulsanRuntime.getSettings().getDockerConnectionURI();

      if (dockerConnection == null) {
        throw new IOException("Docker connection URI is not set. "
            + "Please set the \"main.docker.uri\" global parameter");
      }

      this.client = new DefaultDockerClient(dockerConnection);

      if (this.client == null) {
        throw new IOException("Unable to connect to Docker deamon: "
            + EoulsanRuntime.getSettings().getDockerConnection());
      }
    }
  }

  @Override
  public DockerImageInstance createConnection(String dockerImage) {

    if (client == null) {
      throw new IllegalStateException("Docker client not initialized");
    }

    return new SpotifyDockerImageInstance(this.client, dockerImage,
        this.logger);
  }

  @Override
  public void close() {

    synchronized (this) {

      if (client != null) {
        this.client.close();
      }
    }
  }

  @Override
  public Set<String> listImageTags() throws IOException {

    final Set<String> result = new HashSet<>();

    try {

      for (Image image : CollectionUtils
          .nullToEmpty(this.client.listImages())) {
        for (String tag : CollectionUtils.nullToEmpty(image.repoTags())) {
          if (tag != null) {
            result.add(tag);
          }
        }
      }
    } catch (DockerException | InterruptedException e) {
      throw new IOException(e);
    }

    return result;
  }

  //
  // Constructors
  //

  /**
   * Constructor.
   */
  public SpotifyDockerClient() {

    this(null);
  }

  /**
   * Constructor.
   * @param logger logger to use
   */
  public SpotifyDockerClient(GenericLogger logger) {

    this.logger = logger == null ? new EoulsanRuntimeLogger() : logger;
  }

}
