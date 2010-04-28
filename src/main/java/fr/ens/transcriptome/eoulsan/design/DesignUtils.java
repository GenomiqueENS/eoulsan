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

package fr.ens.transcriptome.eoulsan.design;

import java.util.List;

/**
 * Utils methods for Design.
 * @author Laurent Jourdren
 */
public final class DesignUtils {

  /**
   * Show a design
   * @param design Design to show
   */
  public static void showDesign(final Design design) {

    List<String> metadataFields = design.getMetadataFieldsNames();

    StringBuffer sb = new StringBuffer();

    // Write header
    sb.append("SlideNumber");
    sb.append("\t");

    sb.append("FileName");

    for (String f : metadataFields) {

      sb.append("\t");
      sb.append(f);
    }

    System.out.println(sb.toString());

    // Write data
    List<Sample> slides = design.getSamples();

    for (Sample s : slides) {

      sb.setLength(0);

      sb.append(s.getName());
      sb.append("\t");

      String sourceInfo = s.getSourceInfo();
      if (sourceInfo != null)
        sb.append(sourceInfo);

      for (String f : metadataFields) {

        sb.append("\t");
        sb.append(s.getMetadata().get(f));
      }

      System.out.println(sb.toString());
    }

  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private DesignUtils() {
  }

}
