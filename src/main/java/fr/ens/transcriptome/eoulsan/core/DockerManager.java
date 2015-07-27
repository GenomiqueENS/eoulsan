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
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;

public class DockerManager {

  private static DockerManager singleton;

  private Set<String> images = new HashSet<>();
  private DockerClient client;

  //
  // Docker images to use methods
  //

  /**
   * Add an Docker image to fetch
   * @param image the name of the Docker image to fetch
   */
  public void addImageToFetch(final String image) {

    checkNotNull(image, "image argument cannot be null");
    checkArgument(!image.trim().isEmpty(), "image name cannot be empty");

    this.images.add(image.trim());
  }

  /**
   * Get the images to fetch.
   * @return a set with the images to fetch
   */
  public Set<String> getImagesToFetch() {

    return Collections.unmodifiableSet(this.images);
  }

  //
  // Docker client methods
  //

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
