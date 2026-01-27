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

package fr.ens.biologie.genomique.eoulsan.util;

import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.kenetre.util.process.DockerManager;
import fr.ens.biologie.genomique.kenetre.util.process.DockerManager.ClientType;

/**
 * This class define a Docker manager configurated with Eoulsan settings.
 * @since 2.6
 * @author Laurent Jourdren
 */
public class EoulsanDockerManager {

  /**
   * Get the instance of the DockerManager.
   * @return the instance of the DockerManager
   * @throws IOException if an error occurs while creating the DockerManager
   *           instance
   */
  public static DockerManager getInstance() throws IOException {

    return DockerManager.getInstance(findClientForEoulsan(),
        EoulsanRuntime.getSettings().getDockerConnectionURI());
  }

  //
  // Other methods
  //

  private static ClientType findClientForEoulsan() {

    if (EoulsanRuntime.isRuntime()
        && EoulsanRuntime.getSettings().isDockerBySingularityEnabled()) {

      return ClientType.SINGULARITY;
    }

    if (EoulsanRuntime.isRuntime()
        && EoulsanRuntime.getRuntime().getMode().isHadoopMode()) {

      return ClientType.FALLBACK;
    }

    if (EoulsanRuntime.isRuntime()) {
      return ClientType
          .parseClientName(EoulsanRuntime.getSettings().getDockerBackend());
    }

    return ClientType.FALLBACK;
  }

  //
  // Constructor
  //

  private EoulsanDockerManager() {

    throw new IllegalStateException();
  }

}
