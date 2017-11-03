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

package fr.ens.biologie.genomique.eoulsan.bio;

import java.io.Serializable;

import fr.ens.biologie.genomique.eoulsan.util.Utils;

/**
 * This class define a genomic interval.
 * @since 1.2
 * @author Laurent Jourdren
 * @author Claire Wallon
 */
public class GenomicInterval
    implements Serializable, Comparable<GenomicInterval> {

  private static final long serialVersionUID = 1974207984076778441L;

  private final String chromosome;
  private final int start;
  private final int end;
  private final char strand;

  //
  // Getters
  //

  /**
   * Get the chromosome of the genomic interval.
   * @return the chromosome of the genomic interval
   */
  public String getChromosome() {

    return this.chromosome;
  }

  /**
   * Get the start position of the genomic interval.
   * @return the start position of the genomic interval
   */
  public int getStart() {

    return this.start;
  }

  /**
   * Get the end position of the genomic interval.
   * @return the end position of the genomic interval
   */
  public int getEnd() {

    return this.end;
  }

  /**
   * Get the strand of the genomic interval.
   * @return a char with the strand of the genomic interval
   */
  public char getStrand() {

    return this.strand;
  }

  /**
   * Get the length of the genomic interval.
   * @return the length of the genomic interval
   */
  public int getLength() {

    return this.end - this.start + 1;
  }

  /**
   * Test if a sequence is in the genomic interval.
   * @param start start position of the sequence
   * @param end end position of the sequence
   * @return true if the sequence is in the genomic interval
   */
  public final boolean include(final int start, final int end) {

    return start >= this.start && end <= this.end;
  }

  /**
   * Test if a sequence and the genomic interval have an intersection.
   * @param start start position of the sequence
   * @param end end position of the sequence
   * @return true if the sequence and the genomic interval have an intersection
   */
  public final boolean intersect(final int start, final int end) {

    return (start >= this.start && start <= this.end)
        || (end >= this.start && end <= this.end)
        || (start < this.start && end > this.end);
  }

  @Override
  public int compareTo(final GenomicInterval e) {

    if (e == null) {
      return -1;
    }

    if (!getChromosome().equals(e.getChromosome())) {
      return getChromosome().compareTo(e.getChromosome());
    }

    final int startComp = Integer.compare(this.start, e.getStart());

    if (startComp != 0) {
      return startComp;
    }

    return Integer.compare(this.end, e.getEnd());
  }

  //
  // Object class overrides
  //

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof GenomicInterval)) {
      return false;
    }

    final GenomicInterval that = (GenomicInterval) o;

    return Utils.equal(this.chromosome, that.chromosome)
        && this.start == that.start && this.end == that.end
        && this.strand == that.strand;

  }

  @Override
  public int hashCode() {

    return Utils.hashCode(this.chromosome, this.start, this.end, this.strand);
  }

  /**
   * Override toString()
   * @return a String with the start and end position of the ORF
   */
  @Override
  public String toString() {

    return this.getClass().getSimpleName()
        + "{" + this.chromosome + " [" + this.start + "-" + this.end + "]"
        + this.strand + "}";
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param chromosome Chromosome of the genomic interval
   * @param start Start position of the genomic interval
   * @param end End position of the genomic interval
   * @param strand the strand of the genomic interval
   */
  public GenomicInterval(final String chromosome, final int start,
      final int end, final char strand) {

    if (chromosome == null) {
      throw new NullPointerException("The chromosome value is null");
    }

    if (start < 1) {
      throw new IllegalArgumentException(
          "Start position is lower than 1: " + start);
    }

    if (end < start) {
      throw new IllegalArgumentException(
          "Start position is greater than end: " + end);
    }

    if (strand != '+' && strand != '-' && strand != '.') {
      throw new IllegalArgumentException("Invalid strand value: " + strand);
    }

    this.chromosome = chromosome;
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
   * @param saveStrandInfo save the strand information
   */
  public GenomicInterval(final GFFEntry gffEntry,
      final boolean saveStrandInfo) {

    this(gffEntry.getSeqId(), gffEntry.getStart(), gffEntry.getEnd(),
        saveStrandInfo ? gffEntry.getStrand() : '.');
  }

  // /**
  // * Public constructor
  // * @param gffEntry GFF entry
  // * @param stranded save the strand information if "true" or "reverse"
  // */
  // public GenomicInterval(final GFFEntry gffEntry, final String stranded) {
  //
  // this(gffEntry.getSeqId(), gffEntry.getStart(), gffEntry.getEnd(), stranded
  // .equals("yes") || stranded.equals("reverse")
  // ? gffEntry.getStrand() : '.');
  // }

}
