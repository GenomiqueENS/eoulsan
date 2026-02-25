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

package fr.ens.biologie.genomique.eoulsan.actions;

import fr.ens.biologie.genomique.eoulsan.Globals;

/**
 * This class define an abstract Action
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class AbstractAction implements Action {

  @Override
  public boolean isHadoopJarMode() {

    return false;
  }

  @Override
  public boolean isCurrentArchCompatible() {

    return isApplicationAvailableForCurrentArch();
  }

  @Override
  public boolean isHidden() {

    return false;
  }

  //
  // Private methods
  //

  /**
   * Check if the application is available for current platform.
   *
   * @return true if the application is available for current platform
   */
  public static boolean isApplicationAvailableForCurrentArch() {

    final String os = System.getProperty("os.name").toLowerCase(Globals.DEFAULT_LOCALE);
    final String arch = System.getProperty("os.arch").toLowerCase(Globals.DEFAULT_LOCALE);

    return Globals.AVAILABLE_BINARY_ARCH.contains(os + "\t" + arch);
  }
}
