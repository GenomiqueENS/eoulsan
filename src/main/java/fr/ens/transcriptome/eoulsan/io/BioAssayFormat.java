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
 * of the �cole Normale Sup�rieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.io;

import java.io.Serializable;

/**
 * Define an enum for the BioAssay files format.
 * @author Laurent Jourdren
 */
public abstract class BioAssayFormat implements Serializable {
  static final long serialVersionUID = 4344334197496520634L;

  private String type;
  private String description;
  private String extension;
  

  //
  // Getters
  //

  /**
   * Get the description.
   * @return Returns the description
   */
  public String getDescription() {
    return description;
  }

  

  /**
   * Get the short type.
   * @return Returns the type
   */
  public String getType() {
    return type;
  }

  /**
   * Get the extension of the file.
   * @return The extension of the file
   */
  public String getExtension() {
    return extension;
  }

  /**
   * Check if the first lines come from the format.
   * @param firstLines first lines of the file to test
   * @return true if the String come from the format
   */
  public abstract boolean testFormat(final String firstLines);

  /**
   * toString method.
   * @return the type of the format
   */
  @Override
  public String toString() {

    return type;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param type get the type of the format
   * @param description Description of the format
   * @param extension extension for the file format
   * @param result true if the format is a result file
   */
  public BioAssayFormat(final String type, final String description,
      final String extension) {

    this.type = type == null ? null : type.trim().toUpperCase();
    this.description = description;
    this.extension = extension == null ? null : extension.trim().toLowerCase();
  }

}
