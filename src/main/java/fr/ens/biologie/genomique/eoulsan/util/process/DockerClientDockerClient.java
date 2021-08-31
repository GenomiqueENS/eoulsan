package fr.ens.biologie.genomique.eoulsan.util.process;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;

/**
 * This class define a Docker client using the DockerClient client library.
 * @author Laurent Jourdren
 * @since 2.6
 */
public class DockerClientDockerClient implements DockerClient {

  private com.github.dockerjava.api.DockerClient client;

  @Override
  public void initialize(URI dockerConnectionURI) throws IOException {

    synchronized (this) {

      DockerClientConfig standard =
          DefaultDockerClientConfig.createDefaultConfigBuilder().build();

      DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
          .dockerHost(dockerConnectionURI).build();

      this.client = DockerClientImpl.getInstance(standard, httpClient);

      if (this.client == null) {
        throw new IOException("Unable to connect to Docker deamon: "
            + EoulsanRuntime.getSettings().getDockerConnection());
      }
    }
  }

  @Override
  public DockerImageInstance createConnection(String dockerImage)
      throws IOException {

    return new DockerClientDockerImageInstance(this.client, dockerImage);
  }

  @Override
  public Set<String> listImageTags() throws IOException {

    final Set<String> result = new HashSet<>();

    List<Image> list = this.client.listImagesCmd().exec();

    for (Image image : list) {

      if (image.getRepoTags() != null && image.getRepoTags().length > 0) {
        result.add(image.getRepoTags()[0]);
      }
    }

    return result;
  }

  @Override
  public void close() throws IOException {

    this.client.close();
  }

}
