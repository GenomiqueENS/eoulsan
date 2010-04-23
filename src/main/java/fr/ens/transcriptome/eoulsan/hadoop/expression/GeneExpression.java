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

package fr.ens.transcriptome.eoulsan.hadoop.expression;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

public class GeneExpression {

  private Map<String, GeneExpressionExon> exons =
      new HashMap<String, GeneExpressionExon>();

  private int alignmentCount;

  private final static class GeneExpressionExon {

    private int start;
    private int end;
    private byte[] coverage;

    /**
     * Add an alignment
     * @param start start of the alignment
     * @param end end of the alignment
     */
    public void addAlignement(final int start, final int end) {

      if (start < 1)
        throw new InvalidParameterException(
            "Start position can't be lower than 1.");
      if (end < start)
        throw new InvalidParameterException(
            "End position can't be lower than start position.");

      // if (start < this.start || start > this.end || end > this.end)
      // throw new InvalidParameterException("Invalid alignment.");

      final int newStart = start < this.start ? this.start : start;
      final int newEnd = end > this.end ? this.end : end;

      for (int i = newStart; i <= newEnd; i++)
        this.coverage[i - this.start] = 1;

    }

    /**
     * Get the number of base not covered.
     * @return the number of base not covered
     */
    public int getNotCovered() {

      int count = 0;
      final int len = coverage.length;
      for (int i = 0; i < len; i++)
        if (coverage[i] == 0)
          count++;

      return count;
    }

    /**
     * Get start position.
     * @return The start position
     */
    public int getStart() {

      return this.start;
    }

    /**
     * Get the stop position.
     * @return The end position
     */
    public int getEnd() {

      return this.end;
    }

    //
    // Constructor
    //

    /**
     * Public constructor.
     * @param start start of the exon
     * @param end end of the exon
     */
    public GeneExpressionExon(final int start, final int end) {

      if (start < 1)
        throw new InvalidParameterException(
            "Start position can't be lower than 1.");
      if (end < start)
        throw new InvalidParameterException(
            "End position can't be lower than start position.");

      this.start = start;
      this.end = end;
      this.coverage = new byte[end - start + 1];
    }

  }

  /**
   * Add alignment to the gene coverage
   * @param exonStart start of the exon
   * @param exonEnd end of the exon
   * @param alignmentStart start of the alignment
   * @param alignementEnd end of the alignment
   */
  public void addAlignement(final int exonStart, final int exonEnd,
      final int alignmentStart, final int alignementEnd) {

    final String key = exonStart + "_" + exonEnd;

    final GeneExpressionExon exon;

    if (this.exons.containsKey(key))
      exon = this.exons.get(key);
    else {
      exon = new GeneExpressionExon(exonStart, exonEnd);
      this.exons.put(key, exon);
    }

    exon.addAlignement(alignmentStart, alignementEnd);
    this.alignmentCount++;
  }

  public int getAlignementCount() {

    return this.alignmentCount;
  }

  public int getExonCount() {

    return this.exons.size();
  }

  public int getNotCovered() {

    int count = 0;

    for (Map.Entry<String, GeneExpressionExon> e : this.exons.entrySet())
      count += e.getValue().getNotCovered();

    return count;
  }

  public int getLength() {

    int count = 0;

    for (Map.Entry<String, GeneExpressionExon> e : this.exons.entrySet()) {

      GeneExpressionExon ge = e.getValue();
      count += ge.end - ge.start + 1;
    }

    return count;
  }

  public boolean isCompletlyCovered() {

    return getNotCovered() == 0;
  }

  /**
   * Clear data;
   */
  public void clear() {

    this.exons.clear();
    this.alignmentCount = 0;
  }

}
