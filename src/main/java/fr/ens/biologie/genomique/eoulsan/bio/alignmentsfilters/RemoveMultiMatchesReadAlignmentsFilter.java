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

import java.util.List;

import htsjdk.samtools.SAMRecord;

/**
 * This alignments filter remove all the alignments if more there is more one
 * alignments for a read.
 * @since 1.1
 * @author Laurent Jourdren
 * @author Claire Wallon
 */
public class RemoveMultiMatchesReadAlignmentsFilter
    extends AbstractReadAlignmentsFilter {

  public static final String FILTER_NAME = "removemultimatches";

  @Override
  public String getName() {

    return FILTER_NAME;
  }

  @Override
  public String getDescription() {

    return "Remove all the alignments with several matches";
  }

  @Override
  public void filterReadAlignments(final List<SAMRecord> records) {

    if (records == null || records.isEmpty()) {
      return;
    }

    // single-end mode
    if (!records.get(0).getReadPairedFlag()) {
      if (records.size() > 1) {
        records.clear();
      }
    }

    // paired-end mode
    else {

      switch (records.size()) {

      case 1:
        return;

      case 2:

        int countFirstInPair = 0;
        int countSecondInPair = 0;

        for (SAMRecord record : records) {

          if (record.getFirstOfPairFlag()) {
            countFirstInPair++;
          } else if (record.getSecondOfPairFlag()) {
            countSecondInPair++;
          }
        }

        if (countFirstInPair > 1 || countSecondInPair > 1) {
          records.clear();
        }

        break;

      default:
        records.clear();
        break;
      }

    }
  }

  @Override
  public String toString() {

    return this.getClass().getSimpleName() + "{name=" + getName() + "}";
  }

}
