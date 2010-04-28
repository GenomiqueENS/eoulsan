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
import java.util.HashMap;
import java.util.Map;

/**
 * This class define a registery for BioAssayFormat objects.
 * @author Laurent Jourdren
 */
public final class BioAssayFormatRegistery implements Serializable {

  private static Map<String, BioAssayFormat> formats =
      new HashMap<String, BioAssayFormat>();

  /** Undefined BioAssayFormat. */
  public static final BioAssayFormat UNDEFINED_TXT_BIOASSAY_FORMAT =
      new BioAssayFormat("UNDEFINED", "Undefined .txt result file", ".txt") {

        @Override
        public boolean testFormat(String firstLines) {

          return false;
        }
      };

  static {

    addBioAssayFormat(UNDEFINED_TXT_BIOASSAY_FORMAT);
  }

  //
  // Static methods
  //

  /**
   * Add a BioAssay format.
   * @param format BioAssayFormat to add to th registery
   */
  public static void addBioAssayFormat(final BioAssayFormat format) {

    if (format == null)
      return;

    formats.put(format.getType(), format);
  }

  /**
   * Get a BioAssayFormat from its type.
   * @param type Type of the BioAssayFormat
   * @return a BioAssayFormat enum
   */
  public static BioAssayFormat getBioAssayFormat(final String type) {

    if (type == null)
      return null;

    final String s = type.trim();

    BioAssayFormat result = formats.get(s);

    if (result == null)
      return getBioAssayFormatByExtension(s.substring(s.lastIndexOf(".")));

    return result;
  }

  /**
   * Get a BioAssayFormat from its extension.
   * @param type Type of the BioAssayFormat
   * @return a BioAssayFormat enum
   */
  public static BioAssayFormat getBioAssayFormatByExtension(
      final String extension) {

    if (extension == null)
      return null;

    final String ext = extension.trim().toLowerCase();

    if (ext.equals(".txt"))
      return UNDEFINED_TXT_BIOASSAY_FORMAT;

    for (BioAssayFormat baf : formats.values())
      if (baf.getExtension().equals(ext))
        return baf;

    return null;
  }

  /**
   * Get the BioAssayFormat from the first line of a stream.
   * @param firstLines first lines to test
   * @return the BioAssayFormat of the stream if it has been discovered
   */
  public static BioAssayFormat getBioAssayFormatFromFirstLines(
      final String firstLines) {

    if (firstLines == null)
      return null;

    for (BioAssayFormat format : formats.values()) {

      if (format.testFormat(firstLines))
        return format;

    }

    return null;
  }

  //
  // Constructor
  //

  private BioAssayFormatRegistery() {
  }

}
