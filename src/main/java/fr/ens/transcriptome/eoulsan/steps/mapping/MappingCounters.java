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

package fr.ens.transcriptome.eoulsan.steps.mapping;

/**
 * This enum define the names of the counters of the steps of this package.
 * @author Laurent Jourdren
 */
public enum MappingCounters {

  INPUT_RAW_READS_COUNTER("input raw reads"), OUTPUT_FILTERED_READS_COUNTER(
      "output accepted reads"), READS_REJECTED_BY_FILTERS_COUNTER(
      "reads rejected by filters"), INPUT_MAPPING_READS_COUNTER(
      "input mapping reads"), OUTPUT_MAPPING_ALIGNMENTS_COUNTER(
      "output mapping alignments"),
  INPUT_ALIGNMENTS_COUNTER("input alignments"), UNMAP_READS_COUNTER(
      "unmapped reads"), GOOD_QUALITY_ALIGNMENTS_COUNTER(
      "alignments mapped and with good mapping quality"),
  MAPPER_WRITING_ERRORS("errors in mapper writing"),
  ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER("alignments rejected by filters"),
  ALIGNMENTS_WITH_INVALID_SAM_FORMAT("alignments in invalid sam format"),
  OUTPUT_FILTERED_ALIGNMENTS_COUNTER("output filtered alignments"),
  ALIGNMENTS_WITH_MORE_ONE_HIT_COUNTER("alignments with more than one match");

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

  MappingCounters(final String counterName) {

    this.counterName = counterName;
  }

}
