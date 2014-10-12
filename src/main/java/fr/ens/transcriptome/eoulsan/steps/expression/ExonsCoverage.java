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

package fr.ens.transcriptome.eoulsan.steps.expression;

import java.util.HashMap;
import java.util.Map;

/**
 * This class handle exon coverage.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class ExonsCoverage {

  private Map<String, Exoncoverage> exons = new HashMap<String, Exoncoverage>();

  private int alignmentCount;

  private final static class Exoncoverage {

    private int exonStart;
    private int exonEnd;
    private byte[] coverage;

    /**
     * Add an alignment
     * @param alignmentStart start of the alignment
     * @param alignmentEnd end of the alignment
     */
    public void addAlignement(final int alignmentStart, final int alignmentEnd) {

      if (alignmentStart < 1)
        throw new IllegalArgumentException(
            "Start position can't be lower than 1.");
      if (alignmentEnd < alignmentStart)
        throw new IllegalArgumentException(
            "End position can't be lower than start position.");

      // Test if alignment is outside the exon
      if (alignmentStart > exonEnd | alignmentEnd < exonStart)
        return;

      final int start =
          alignmentStart < this.exonStart ? this.exonStart : alignmentStart;
      final int end = alignmentEnd > this.exonEnd ? this.exonEnd : alignmentEnd;

      for (int i = start; i <= end; i++)
        this.coverage[i - this.exonStart] = 1;

    }

    /**
     * Get the number of base covered.
     * @return the number of base not covered
     */
    public int getCovered() {

      int count = 0;
      final int len = coverage.length;
      for (int i = 0; i < len; i++)
        if (coverage[i] > 0)
          count++;

      return count;
    }

    /**
     * Get start position.
     * @return The start position
     */
    public int getStart() {

      return this.exonStart;
    }

    /**
     * Get the stop position.
     * @return The end position
     */
    public int getEnd() {

      return this.exonEnd;
    }

    //
    // Constructor
    //

    /**
     * Public constructor.
     * @param start start of the exon
     * @param end end of the exon
     */
    public Exoncoverage(final int start, final int end) {

      if (start < 1)
        throw new IllegalArgumentException(
            "Start position can't be lower than 1.");
      if (end < start)
        throw new IllegalArgumentException(
            "End position can't be lower than start position.");

      this.exonStart = start;
      this.exonEnd = end;
      this.coverage = new byte[end - start + 1];
    }

  }

  /**
   * Add alignment to the gene coverage
   * @param exonStart start of the exon
   * @param exonEnd end of the exon
   * @param alignmentStart start of the alignment
   * @param alignementEnd end of the alignment
   * @param incrementalignmentCount true if the alignment count must be
   *          incremented
   */
  public void addAlignement(final int exonStart, final int exonEnd,
      final int alignmentStart, final int alignementEnd,
      final boolean incrementalignmentCount) {

    final String key = exonStart + "_" + exonEnd;

    final Exoncoverage exon;

    if (this.exons.containsKey(key))
      exon = this.exons.get(key);
    else {
      exon = new Exoncoverage(exonStart, exonEnd);
      this.exons.put(key, exon);
    }

    exon.addAlignement(alignmentStart, alignementEnd);
    if (incrementalignmentCount)
      this.alignmentCount++;
  }

  public int getAlignementCount() {

    return this.alignmentCount;
  }

  public int getExonCount() {

    return this.exons.size();
  }

  public int getCovered() {

    int count = 0;

    for (Map.Entry<String, Exoncoverage> e : this.exons.entrySet())
      count += e.getValue().getCovered();

    return count;
  }

  public int getNotCovered(final int geneLength) {

    return geneLength - getCovered();
  }

  public int getLength() {

    int count = 0;

    for (Map.Entry<String, Exoncoverage> e : this.exons.entrySet()) {

      Exoncoverage ge = e.getValue();
      count += ge.exonEnd - ge.exonStart + 1;
    }

    return count;
  }

  /**
   * Clear data;
   */
  public void clear() {

    this.exons.clear();
    this.alignmentCount = 0;
  }

}
