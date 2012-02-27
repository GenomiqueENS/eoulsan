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

import java.io.Serializable;

import fr.ens.transcriptome.eoulsan.steps.expression.TranscriptAndExonFinder.Exon;
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class define an exon.
 * @author Laurent Jourdren
 */
public class GenomicInterval implements Serializable,
    Comparable<GenomicInterval> {

  private String chromosome;
  private int start;
  private int end;
  private char strand;

  //
  // Getters
  //

  /**
   * Get the chromosome of the exon.
   * @return the chromosome of the exon
   */
  public String getChromosome() {

    return this.chromosome;
  }

  /**
   * Get the start position of the exon.
   * @return the start position of the exon
   */
  public int getStart() {

    return this.start;
  }

  /**
   * Get the end position of the exon.
   * @return the end position of the exon
   */
  public int getEnd() {

    return this.end;
  }

  /**
   * Get the strand of the exon.
   * @return a char with the strand of the exon
   */
  public char getStrand() {

    return this.strand;
  }

  /**
   * Get the length of the Exon.
   * @return the length of the exon
   */
  public int getLength() {

    return this.end - this.start + 1;
  }

  /**
   * Test if a sequence is in the ORF
   * @param start start position of the ORF
   * @param end end position of the ORF
   * @return true if the sequence is in the ORF
   */
  public final boolean include(final int start, final int end) {

    return start >= this.start && end <= this.end;
  }

  /**
   * Test if a sequence is in the ORF
   * @param start start position of the ORF
   * @param end end position of the ORF
   * @return true if the sequence is in the ORF
   */
  public final boolean intersect(final int start, final int end) {

    return (start >= this.start && start <= this.end)
        || (end >= this.start && end <= this.end)
        || (start < this.start && end > this.end);
  }

  @Override
  public int compareTo(final GenomicInterval e) {

    if (e == null)
      return -1;

    if (!e.getChromosome().equals(e.getChromosome()))
      return getChromosome().compareTo(e.getChromosome());

    final int startComp = ((Integer) this.start).compareTo(e.getStart());

    if (startComp != 0)
      return startComp;

    final int endComp = ((Integer) this.end).compareTo(e.getEnd());

    return endComp;
  }

  //
  // Object class overrides
  //

  @Override
  public boolean equals(final Object o) {

    if (o == this)
      return true;

    if (o == null || !(o instanceof Exon))
      return false;

    final GenomicInterval that = (GenomicInterval) o;

    return Utils.equal(this.chromosome, that.chromosome)
        && this.start == that.start && this.end == that.end
        && this.strand == that.strand;

  }

  @Override
  public int hashCode() {

    return Utils.hashCode(chromosome, start, end, strand);
  }

  /**
   * Overide toString()
   * @return a String with the start and end position of the ORF
   */
  @Override
  public String toString() {

    return this.getClass().getSimpleName()
        + "{" + chromosome + " [" + start + "-" + end + "]" + strand + "}";
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param start Start position of the ORF
   * @param end End position of the ORF
   * @param strand the strand of the ORF
   * @param parentId id of the parent
   */
  public GenomicInterval(final String chromosone, final int start,
      final int end, final char strand) {

    if (chromosone == null)
      throw new NullPointerException("The chromosome value is null");

    if (start < 1)
      throw new IllegalArgumentException("Start position is lower that 1: "
          + start);

    if (end < start)
      throw new IllegalArgumentException("End position is greater that end: "
          + end);

    if (strand != '+' && strand != '-' && strand != '.')
      throw new IllegalArgumentException("Invalid strand value: " + strand);

    this.chromosome = chromosone;
    this.start = start;
    this.end = end;
    this.strand = strand;
  }

  /**
   * Public constructor
   * @param gffEntry GFF entry
   */
  public GenomicInterval(final GFFEntry gffEntry) {

    this(gffEntry, true);
  }

  /**
   * Public constructor
   * @param gffEntry GFF entry
   * @param stranded save the strand information if true
   */
  public GenomicInterval(final GFFEntry gffEntry, final boolean stranded) {

    this(gffEntry.getSeqId(), gffEntry.getStart(), gffEntry.getEnd(), stranded
        ? gffEntry.getStrand() : '.');
  }

}
