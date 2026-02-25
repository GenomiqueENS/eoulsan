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

import java.nio.file.Path;

/**
 * Get information about memory on Linux systems
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public class LinuxMemInfo extends LinuxInfo {

  private static final String MEMINFO_FILE = "/proc/meminfo";

  @Override
  public Path getInfoFile() {

    return Path.of(MEMINFO_FILE);
  }

  /**
   * Get the total memory of the system.
   *
   * @return the total memory of the system
   */
  public String getMemTotal() {

    return get("MemTotal");
  }

  //
  // Constructor
  //

  /** Public constructor. */
  public LinuxMemInfo() {

    parse();
  }
}
