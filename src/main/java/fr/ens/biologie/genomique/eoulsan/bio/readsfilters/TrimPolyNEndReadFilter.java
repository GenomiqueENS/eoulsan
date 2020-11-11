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

package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import java.util.regex.Pattern;

import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

/**
 * Define a filter that remove terminal polyN sequences.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class TrimPolyNEndReadFilter extends AbstractReadFilter {

  public static final String FILTER_NAME = "trimpolynend";
  private static final Pattern PATTERN = Pattern.compile("NN+$");

  @Override
  public boolean accept(final ReadSequence read) {

    if (read == null) {
      return false;
    }

    trim(read);

    // Do no accept 0 length reads
    if (read.length() == 0) {
      return false;
    }

    return true;
  }

  /**
   * Trim the read sequence and quality if ends with polyN.
   * @param read Read to trim
   */
  public static void trim(final ReadSequence read) {

    if (read == null
        || read.getSequence() == null || read.getQuality() == null
        || read.getSequence().length() != read.getQuality().length()
        || read.getSequence().length() == 0) {
      return;
    }

    final String[] splitResult = PATTERN.split(read.getSequence());

    // Test if the sequence contains only N nucleotides
    if (splitResult == null || splitResult.length == 0) {
      read.setSequence("");
      read.setQuality("");

      return;
    }

    final ReadSequence tmp = read.subSequence(0, splitResult[0].length());

    read.setSequence(tmp.getSequence());
    read.setQuality(tmp.getQuality());
  }

  @Override
  public String getName() {

    return FILTER_NAME;
  }

  @Override
  public String getDescription() {

    return "Trim polyN ends";
  }

  @Override
  public String toString() {

    return this.getClass().getSimpleName() + "{}";
  }

}
