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

package fr.ens.transcriptome.eoulsan.programs.mapping;

import java.util.regex.Pattern;

import fr.ens.transcriptome.eoulsan.bio.ReadSequence;

/**
 * Class with static methods to used to filter reads.
 * @author Laurent Jourdren
 */
public final class ReadsFilter {

  private static final Pattern PATTERN = Pattern.compile("NN+$");

  /**
   * Trim the read sequence and quality if ends with polyN.
   * @param read Read to trim
   */
  public static final void trimReadSequence(final ReadSequence read) {

    if (read == null || !read.isFastQValid())
      return;

    final String[] splitResult = PATTERN.split(read.getSequence());

    // Test if the sequence contains only N nucleotides
    if (splitResult == null || splitResult.length == 0) {
      read.setSequence("");
      read.setQuality("");

      return;
    }

    final String sequence = splitResult[0];
    final int len = sequence.length();

    // Trim read sequence and quality if needed
    if (len != read.length()) {
      read.setSequence(sequence);
      read.setQuality(read.getQuality().substring(0, len));
    }

  }

  /**
   * Test if a read is valid.
   * @param read Read to test
   * @param lengthThreshold threshold for the length of reads
   * @param qualityThreshold threshold for reads quality
   * @return true if the read is valid
   */
  public static final boolean isReadValid(final ReadSequence read,
      final int lengthThreshold, final double qualityThreshold) {

    return read.length() > lengthThreshold
        && read.meanQuality() > qualityThreshold;
  }

}
