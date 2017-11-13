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

package fr.ens.biologie.genomique.eoulsan.bio.alignmentsfilters;

import java.util.ArrayList;
import java.util.List;

import htsjdk.samtools.SAMRecord;

/**
 * This alignment filter remove all the unmapped alignments.
 * @since 2.1
 * @author Laurent Jourdren
 * @author Claire Wallon
 */
public abstract class AbstractRemoveFlagReadAlignmentsFilter
    extends AbstractReadAlignmentsFilter {

  public static final String FILTER_NAME = "removetest";

  private final List<SAMRecord> recordsToRemove = new ArrayList<>();

  private final int flagValue;

  @Override
  public String getName() {

    return FILTER_NAME;
  }

  @Override
  public String getDescription() {

    return "Remove all the unmapped alignments";
  }

  private boolean isFlagEnabled(int flags) {
    return (flags & this.flagValue) != 0;
  }

  @Override
  public void filterReadAlignments(final List<SAMRecord> records) {

    if (records == null || records.isEmpty()) {
      return;
    }

    // single-end mode
    if (!records.get(0).getReadPairedFlag()) {
      for (SAMRecord r : records) {

        // storage in 'result' of records that do not pass the filter
        if (isFlagEnabled(r.getFlags())) {
          this.recordsToRemove.add(r);
        }
      }
    }

    // paired-end mode
    else {
      for (int counterRecord = 0; counterRecord < records.size()
          - 1; counterRecord += 2) {

        // storage in 'result' of records that do not pass the filter
        if (isFlagEnabled(records.get(counterRecord).getFlags())
            || isFlagEnabled(records.get(counterRecord + 1).getFlags())) {

          // records are stored 2 by 2 because of the paired-end mode
          this.recordsToRemove.add(records.get(counterRecord));
          this.recordsToRemove.add(records.get(counterRecord + 1));
        }
      }
    }

    // all records that do not pass the filter are removed
    records.removeAll(this.recordsToRemove);
    this.recordsToRemove.clear();
  }

  @Override
  public String toString() {

    return this.getClass().getSimpleName() + "{name=" + getName() + "}";
  }

  //
  // Constructor
  //

  /**
   * Protected constructor.
   * @param flagValue the value of the flag to filter
   */
  protected AbstractRemoveFlagReadAlignmentsFilter(final int flagValue) {

    this.flagValue = flagValue;
  }
}
