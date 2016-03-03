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

package fr.ens.biologie.genomique.eoulsan.bio;

/**
 * This class define an exception for biological files entries that can allow
 * retrieve the invalid entry.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class BadBioEntryException extends Exception {

  // Serialization version UID
  private static final long serialVersionUID = 3332356090180839324L;

  /** Entry at the origin of the exception. */
  private final String entry;

  //
  // Getter
  //

  /**
   * Return the entry at the origin of the exception.
   * @return a String with the invalid entry
   */
  public final String getEntry() {

    return this.entry;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param message message with the origin of the exception
   * @param entry the entry at the origin of the exception
   */
  public BadBioEntryException(final String message, final String entry) {

    super(message);
    this.entry = entry;
  }
}
