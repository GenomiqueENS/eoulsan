package fr.ens.biologie.genomique.eoulsan.util.process;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Set;

/**
 * This class define a Docker client using the Singularity command line.
 * @author Laurent Jourdren
 * @since 2.3
 */
public class SingularityDockerClient implements DockerClient {

  @Override
  public void initialize(URI dockerConnectionURI) {
    // Nothing to do
  }

  @Override
  public DockerImageInstance createConnection(String dockerImage) {

    return new SingularityDockerImageInstance(dockerImage);
  }

  @Override
  public void close() {
    // Nothing to do
  }

  @Override
  public Set<String> listImageTags() throws IOException {

    // Nothing to do because the feature does not exists in Singularity
    return Collections.emptySet();
  }

}
