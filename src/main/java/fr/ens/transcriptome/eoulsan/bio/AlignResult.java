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

package fr.ens.transcriptome.eoulsan.bio;

import java.util.List;

import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define an alignment result.
 * @author Laurent Jourdren
 */
public class AlignResult {

  /** List with fields values. */
  private List<String> fields;

  /**
   * Get the sequence id of the read.
   * @return the sequence id of the read
   */
  public final String getSequenceId() {

    return this.fields.get(0);
  }

  /**
   * Get the sequence of the read.
   * @return the sequence of the read
   */
  public final String getSequence() {

    return this.fields.get(1);
  }

  /**
   * Get the quality of the read.
   * @return the quality of the read
   */
  public final String getQuality() {

    return this.fields.get(2);
  }

  /**
   * Get the number of hits of the alignment.
   * @return the number of hits of the alignment
   */
  public final int getNumberOfHits() throws NumberFormatException {

    return Integer.parseInt(this.fields.get(3));
  }

  /**
   * Get the read length.
   * @return the read length
   */
  public final int getReadLength() {

    return Integer.parseInt(this.fields.get(5));
  }

  /**
   * Get the pairend flag.
   * @return the pairend flag
   */
  public final char getPairendFlag() {

    return this.fields.get(4).charAt(0);
  }

  /**
   * Test if the alignment is on the direct(+) chain of the reference.
   * @return true if the alignment is on the direct(+) chain of the reference.
   */
  public final boolean isDirectStrand() {

    return "+".equals(this.fields.get(6));
  }

  /**
   * Get the chromosome of the match.
   * @return the chromosome of the match
   */
  public final String getChromosome() {

    return this.fields.get(7);
  }

  /**
   * Get the location of the match on the chromosome. Counted from 1 in bp.
   * @return the location
   */
  public final int getLocation() {

    return Integer.parseInt(this.fields.get(8));
  }

  /**
   * Get the hit type. 0 is for exact match. If > 0 the number of mismatches.
   * @return the hit type.
   */
  public final int getHitType() {

    return Integer.parseInt(this.fields.get(9));
  }

  //
  // Other methods
  //

  /**
   * Parse line.
   * @param line Line to parse
   */
  public final void parseResultLine(final String line)
      throws BadBioEntryException {

    if (line == null) {
      throw new IllegalArgumentException("line is null");
    }

    this.fields = StringUtils.fastSplit(line, this.fields);

    if (this.fields == null) {
      throw new BadBioEntryException("The parsing of the line is null", line);
    }

    if (this.fields.size() < 11) {
      throw new BadBioEntryException("Invalid number of field "
          + this.fields.size() + " (11 expected) ", line);
    }

  }

}
