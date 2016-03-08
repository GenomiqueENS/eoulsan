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

package fr.ens.biologie.genomique.eoulsan.util.docker;

import java.net.URI;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;

/**
 * This class define a class that manage Eoulsan Docker connections.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DockerManager {

  private static DockerManager singleton;

  private DockerClient client;

  /**
   * Get Docker client.
   * @return a Docker client object
   */
  public DockerClient getClient() {

    if (this.client != null) {
      return this.client;
    }

    final URI dockerConnection =
        EoulsanRuntime.getSettings().getDockerConnectionURI();

    if (dockerConnection == null) {
      return null;
    }

    this.client = new DefaultDockerClient(dockerConnection);

    return this.client;
  }

  /**
   * Close Docker connections.
   */
  public void closeConnections() {

    final DockerClient client = getClient();

    if (client != null) {
      client.close();
    }
  }

  //
  // Singleton method
  //

  public static DockerManager getInstance() {

    if (singleton == null) {
      singleton = new DockerManager();
    }

    return singleton;
  }

  //
  // Constructor
  //

  private DockerManager() {
  }

}
