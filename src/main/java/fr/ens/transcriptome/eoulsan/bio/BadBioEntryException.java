/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.bio;

/**
 * This class define an exception for biological files entries that can allow
 * retrieve the invalid entry.
 * @author Laurent Jourdren
 */
public class BadBioEntryException extends Exception {

  private String entry;

  //
  // Getter
  //

  /**
   * Return the entry at the origin of the exception.
   * @return a String with the invalid entry
   */
  public String getEntry() {

    return entry;
  }

  //
  // Constructor
  //

  public BadBioEntryException(final String message, final String entry) {

    super(message);
    this.entry = entry;
  }
}
