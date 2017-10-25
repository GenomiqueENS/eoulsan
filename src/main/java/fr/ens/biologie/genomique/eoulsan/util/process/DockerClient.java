package fr.ens.biologie.genomique.eoulsan.util.process;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

/**
 * This interface define a Docker client.
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface DockerClient {

  /**
   * Initialize the Docker client.
   * @param dockerConnectionURI the URI of the Docker connection.
   * @throws IOException if an error occurs while initialize the Docker
   *           connection
   */
  void initialize(URI dockerConnectionURI) throws IOException;

  /**
   * Create a Docker connection.
   * @param dockerImage the Docker image to use with the connection
   * @return a new Docker connection
   * @throws IOException if an error occurs while creating the connection
   */
  DockerImageInstance createConnection(String dockerImage) throws IOException;

  /**
   * List the Docker images tags.
   * @return a set with the Docker image tags
   * @throws IOException if an error occurs while listing the Docker image tags
   */
  Set<String> listImageTags() throws IOException;

  /**
   * Close the Docker client.
   * @throws IOException if an error occurs while closing the client
   */
  void close() throws IOException;

}
