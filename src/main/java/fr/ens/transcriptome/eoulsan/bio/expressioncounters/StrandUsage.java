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

package fr.ens.transcriptome.eoulsan.bio.expressioncounters;

/**
 * This enum define the strand usage for the Expression Counter.
 * @author Laurent Jourdren
 */
public enum StrandUsage {
  YES("yes", true), NO("no", false), REVERSE("reverse", true);

  private final String name;
  private final boolean saveStrandInfo;

  //
  // Getters
  //

  /**
   * Get the name of strand usage
   * @return a string with the name of the strand usage
   */
  public String getName() {

    return this.name;
  }

  /**
   * Test if strand information must be saved
   * @return true if strand information must be saved
   */
  public boolean isSaveStrandInfo() {

    return this.saveStrandInfo;
  }

  //
  // Static methods
  //

  /**
   * Get the StrandUsage from its name.
   * @param name name of the strand usage
   * @return a StrandUsage or null if no StrandUsage found for the name
   */
  public static StrandUsage getStrandUsageFromName(final String name) {

    if (name == null)
      return null;

    final String lowerName = name.trim().toLowerCase();

    for (StrandUsage su : StrandUsage.values()) {

      if (su.getName().toLowerCase().equals(lowerName))
        return su;
    }

    return null;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param name name of the strand usage
   * @param saveStrandInfo true if strand information must be saved
   */
  StrandUsage(final String name, final boolean saveStrandInfo) {

    this.name = name;
    this.saveStrandInfo = saveStrandInfo;
  }

}