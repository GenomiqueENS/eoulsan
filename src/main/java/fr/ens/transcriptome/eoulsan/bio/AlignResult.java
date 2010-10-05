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

import java.util.ArrayList;
import java.util.List;

import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define an alignment result.
 * @author Laurent Jourdren
 */
public class AlignResult {

  private List<String> fields = new ArrayList<String>();

  /**
   * Get the sequence id of the read
   * @return the sequence id of the read
   */
  public String getSequenceId() {

    return this.fields.get(0);
  }

  /**
   * Get the sequence of the read.
   * @return the sequence of the read
   */
  public String getSequence() {

    return this.fields.get(1);
  }

  /**
   * Get the quality of the read.
   * @return the quality of the read
   */
  public String getQuality() {

    return this.fields.get(2);
  }

  /**
   * Get the number of hits of the alignment.
   * @return the number of hits of the alignment
   */
  public int getNumberOfHits() {

    try {
      return Integer.parseInt(this.fields.get(3));
    } catch (NumberFormatException e) {
      System.err.println(fields);
      throw e;
    }
  }

  /**
   * Get the read length.
   * @return the read length
   */
  public int getReadLength() {

    return Integer.parseInt(this.fields.get(5));
  }

  /**
   * Get the pairend flag
   * @return the pairend flag
   */
  public char getPairendFlag() {

    return this.fields.get(4).charAt(0);
  }

  /**
   * Test if the alignment is on the direct(+) chain of the reference.
   * @return true if the alignment is on the direct(+) chain of the reference.
   */
  public boolean isDirectStrand() {

    return "+".equals(this.fields.get(6));
  }

  /**
   * Get the chromosome of the match.
   * @return the chromosome of the match
   */
  public String getChromosome() {

    return this.fields.get(7);
  }

  /**
   * Get the location of the match on the chromosome. Counted from 1 in bp.
   * @return the location
   */
  public int getLocation() {

    return Integer.parseInt(this.fields.get(8));
  }

  /**
   * Get the hit type. 0 is for exact match. If > 0 the number of mismatches.
   * @return the hit type.
   */
  public int getHitType() {

    return Integer.parseInt(this.fields.get(9));
  }

  //
  // Other methods
  //

  /**
   * Parse line.
   * @param line Line to parse
   */
  public void parseResultLine(final String line) {

    if (line == null)
      throw new NullPointerException("line is null");

    this.fields = StringUtils.fastSplit(line, this.fields);
  }

}
