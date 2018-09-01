/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.util.process;

import java.io.IOException;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;

/**
 * This class define a class that manage Eoulsan Docker connections.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DockerManager {

  private static DockerManager singleton;
  private final DockerClient client;

  /**
   * Create a Docker image instance.
   * @param dockerImage docker image
   * @return a Docker connection object
   * @throws IOException if an error occurs while creating the image instance
   */
  public DockerImageInstance createImageInstance(final String dockerImage)
      throws IOException {

    return this.client.createConnection(dockerImage);
  }

  /**
   * List the tags of installed images.
   * @return a set with the tags of installed images
   * @throws IOException if an error occurs while listing the tag
   */
  public Set<String> listImageTags() throws IOException {

    return client.listImageTags();
  }

  /**
   * Close Docker connections.
   * @throws IOException if an error occurs while closing the connections
   */
  public static void closeConnections() throws IOException {

    if (singleton != null) {
      singleton.client.close();
    }
  }

  //
  // Singleton method
  //

  /**
   * Get the instance of the DockerManager.
   * @return the instance of the DockerManager
   * @throws IOException if an error occurs while creating the DockerManager
   *           instance
   */
  public static synchronized DockerManager getInstance() throws IOException {

    if (singleton == null) {
      singleton = new DockerManager();
    }

    return singleton;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   * @throws IOException if an error occurs while creating the instance
   */
  private DockerManager() throws IOException {

    if (EoulsanRuntime.isRuntime()
        && EoulsanRuntime.getSettings().isDockerBySingularityEnabled()) {

      this.client = new SingularityDockerClient();

    } else {

      if (EoulsanRuntime.isRuntime()
          && EoulsanRuntime.getRuntime().getMode().isHadoopMode()) {
        this.client = new FallBackDockerClient();
      } else {
        this.client = new SpotifyDockerClient();
      }
    }

    this.client
        .initialize(EoulsanRuntime.getSettings().getDockerConnectionURI());
  }

}
