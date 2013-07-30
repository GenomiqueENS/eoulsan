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
 * This enum defines the overlap mode for the Expression Counter.
 * @since 1.2
 * @author Laurent Jourdren
 */
public enum OverlapMode {

  UNION("union"), INTERSECTION_STRICT("intersection-strict"),
  INTERSECTION_NONEMPTY("intersection-nonempty");

  private final String name;

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

  //
  // Static methods
  //

  /**
   * Get the OverlapMode from its name.
   * @param name name of the overlap mode
   * @return a OverlapMode or null if no OverlapMode found for the name
   */
  public static OverlapMode getOverlapModeFromName(final String name) {

    if (name == null)
      return null;

    final String lowerName = name.trim().toLowerCase();

    for (OverlapMode om : OverlapMode.values()) {

      if (om.getName().toLowerCase().equals(lowerName))
        return om;
    }

    return null;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param name name of the overlap mode
   */
  OverlapMode(final String name) {

    this.name = name;
  }

}
