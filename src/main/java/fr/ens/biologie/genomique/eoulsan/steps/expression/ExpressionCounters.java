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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.steps.expression;

/**
 * This enum define counters for the expression step.
 * @since 1.0
 * @author Laurent Jourdren
 */
public enum ExpressionCounters {

  INVALID_SAM_ENTRIES_COUNTER("invalid SAM input entries"),
  TOTAL_READS_COUNTER("reads total"), UNUSED_READS_COUNTER("reads unused"),
  USED_READS_COUNTER("reads used"), UNMAPPED_READS_COUNTER("unmapped reads"),
  ELIMINATED_READS_COUNTER("reads eliminated"),
  TOTAL_ALIGNMENTS_COUNTER("total number of alignments"),
  NOT_ALIGNED_ALIGNMENTS_COUNTER("number of not aligned alignments"),
  NOT_UNIQUE_ALIGNMENTS_COUNTER("number of not unique alignments"),
  LOW_QUAL_ALIGNMENTS_COUNTER("number of alignments with too low quality"),
  EMPTY_ALIGNMENTS_COUNTER("number of alignments with no feature"),
  AMBIGUOUS_ALIGNMENTS_COUNTER("number of ambiguous alignments"),

  PARENTS_COUNTER("parent"), INVALID_CHROMOSOME_COUNTER("invalid chromosome"),
  PARENT_ID_NOT_FOUND_COUNTER("Parent Id not found in exon range");

  private final String counterName;

  /**
   * Get the name of the counter.
   * @return the name of the counter
   */
  public String counterName() {

    return this.counterName;
  }

  @Override
  public String toString() {
    return this.counterName;
  }

  //
  // Constructor
  //

  ExpressionCounters(final String counterName) {

    this.counterName = counterName;
  }

}
