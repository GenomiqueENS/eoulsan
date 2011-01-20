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

package fr.ens.transcriptome.eoulsan.steps.expression;

public enum ExpressionCounters {



  INVALID_SAM_ENTRIES_COUNTER("invalid SAM input entries"),
  TOTAL_READS_COUNTER("reads total"),
  UNUSED_READS_COUNTER("reads unused"),
  USED_READS_COUNTER("reads used"),
  
  
  
  PARENTS_COUNTER("parent"),
  INVALID_CHROMOSOME_COUNTER("invalid chromosome"),
  PARENT_ID_NOT_FOUND_COUNTER("Parent Id not found in exon range");
  
  
  private final String counterName;

  /**
   * Get the name of the counter.
   * @return the name of the counter
   */
  public String counterName() {

    return counterName;
  }

  @Override
  public String toString() {
    return counterName;
  }

  //
  // Constructor
  //

  ExpressionCounters(final String counterName) {

    this.counterName = counterName;
  }

}
