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

package fr.ens.biologie.genomique.eoulsan.design.io;

import fr.ens.biologie.genomique.eoulsan.design.Design;
import java.io.IOException;

/**
 * This interface define a DesignReader.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public interface DesignReader {

  /**
   * Read the design.
   *
   * @return a new Design object
   * @throws IOException if an error occurs while reading the design
   */
  Design read() throws IOException;
}
