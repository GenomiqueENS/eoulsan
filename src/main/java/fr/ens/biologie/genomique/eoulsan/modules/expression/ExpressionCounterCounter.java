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

package fr.ens.biologie.genomique.eoulsan.modules.expression;

/**
 * This enum define counters for the expression step.
 * @since 1.0
 * @author Laurent Jourdren
 */
public enum ExpressionCounterCounter {

  INVALID_SAM_ENTRIES_COUNTER("invalid SAM input entries"),
  TOTAL_READS_COUNTER("reads total"), UNUSED_READS_COUNTER("reads unused"),
  USED_READS_COUNTER("reads used"), UNMAPPED_READS_COUNTER("unmapped reads"),
  ELIMINATED_READS_COUNTER("reads eliminated"),
  TOTAL_ALIGNMENTS_COUNTER("total number of alignments"),
  NOT_ALIGNED_ALIGNMENTS_COUNTER("number of not aligned alignments",
      "not_aligned"),
  NOT_UNIQUE_ALIGNMENTS_COUNTER("number of not unique alignments",
      "alignment_not_unique"),
  LOW_QUAL_ALIGNMENTS_COUNTER("number of alignments with too low quality",
      "too_low_aQual"),
  EMPTY_ALIGNMENTS_COUNTER("number of alignments with no feature",
      "no_feature"),
  AMBIGUOUS_ALIGNMENTS_COUNTER("number of ambiguous alignments", "ambiguous"),
  MISSING_MATES_COUNTER("number of missing mate alignments"),

  PARENTS_COUNTER("parent"), INVALID_CHROMOSOME_COUNTER("invalid chromosome"),
  PARENT_ID_NOT_FOUND_COUNTER("Parent Id not found in exon range");

  private final String counterName;
  private final String htseqName;

  /**
   * Get the name of the counter.
   * @return the name of the counter
   */
  public String counterName() {

    return this.counterName;
  }

  /**
   * Get the HTSeq-count name of the counter.
   * @return the HTSeq-count of the counter
   */
  public String htSeqCountCounterName() {

    return this.htseqName;
  }

  /**
   * Get an counter from its HTSeq-count name
   * @param counterName the name of the counter to search
   * @return the counter if found or null
   */
  public static ExpressionCounterCounter getCounterFromHTSeqCountName(
      final String counterName) {

    if (counterName == null) {
      throw new NullPointerException("counterName argument cannot be null");
    }

    String s =
        counterName.startsWith("__") ? counterName.substring(2) : counterName;

    for (ExpressionCounterCounter c : ExpressionCounterCounter.values()) {

      if (c.htseqName != null && c.htseqName.equals(s)) {
        return c;
      }

    }

    return null;
  }

  /**
   * Get an counter from its HTSeq-count name
   * @param counterName the name of the counter to search
   * @return the counter if found or null
   */
  public static ExpressionCounterCounter getCounterFromEoulsanName(
      final String counterName) {

    if (counterName == null) {
      throw new NullPointerException("counterName argument cannot be null");
    }

    for (ExpressionCounterCounter c : ExpressionCounterCounter.values()) {

      if (c.counterName != null && c.counterName.equals(counterName)) {
        return c;
      }

    }

    return null;
  }

  @Override
  public String toString() {
    return this.counterName;
  }

  //
  // Constructor
  //

  /**
   * Constructor name.
   * @param counterName counter name
   */
  ExpressionCounterCounter(final String counterName) {

    this(counterName, null);
  }

  /**
   * Constructor name.
   * @param counterName counter name
   * @param htseqName htSeq counter name
   */
  ExpressionCounterCounter(final String counterName, String htseqName) {

    this.counterName = counterName;
    this.htseqName = htseqName;
  }

}
