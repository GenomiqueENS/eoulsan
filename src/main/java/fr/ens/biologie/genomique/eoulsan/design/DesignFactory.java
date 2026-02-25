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

package fr.ens.biologie.genomique.eoulsan.design;

import java.util.Objects;

/**
 * This class is a factory for experimental design.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class DesignFactory {

  /**
   * Create a design without targets.
   *
   * @return a new DesignImpl
   */
  public static Design createEmptyDesign() {

    return new DesignImpl();
  }

  /**
   * Create an unmodifiable wrapper around an existing design.
   *
   * @param design the design
   * @return an unmodifiable wrapper
   */
  public static Design unmodifiableDesign(final Design design) {

    Objects.requireNonNull(design, "design argument cannot be null");

    if (design instanceof UnmodifiableDesign) {
      return design;
    }

    return new UnmodifiableDesign(design);
  }

  //
  // Constructor
  //

  private DesignFactory() {}
}
