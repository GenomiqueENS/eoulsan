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

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import htsjdk.samtools.SAMRecord;

/**
 * This alignments filter keep a given number of the first alignments for a
 * read.
 * @since 1.2
 * @author Claire Wallon
 */
public class KeepNumberMatchReadAlignmentsFilter
    extends AbstractReadAlignmentsFilter {

  public static final String FILTER_NAME = "keepnumbermatch";
  private int numberMatch = -1;

  @Override
  public String getName() {
    return FILTER_NAME;
  }

  @Override
  public String getDescription() {
    return "This filter allows to keep a given number of alignments of a read.";
  }

  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {

    if (key == null || value == null) {
      return;
    }

    if ("threshold".equals(key.trim())) {

      try {
        this.numberMatch = Integer.parseInt(value.trim());
      } catch (NumberFormatException e) {
        return;
      }

      if (this.numberMatch < 0) {
        throw new EoulsanException(
            "Invalid number of match to keep: " + this.numberMatch);
      }
    } else {
      throw new EoulsanException(
          "Unknown parameter for " + getName() + " alignments filter: " + key);
    }
  }

  @Override
  public void init() {

    if (this.numberMatch < 0) {
      throw new IllegalArgumentException(
          "The number of match to keep is not set for "
              + getName() + " alignments filter.");
    }
  }

  @Override
  public void filterReadAlignments(final List<SAMRecord> records) {

    if (records == null || records.isEmpty()) {
      return;
    }

    List<SAMRecord> recordsToKeep = new ArrayList<>();
    int cptRecords = 0;
    int nbRecordsToKeep;

    // single-end mode
    if (!records.get(0).getReadPairedFlag()) {
      nbRecordsToKeep = this.numberMatch;
    }

    // paired-end mode
    else {
      nbRecordsToKeep = this.numberMatch * 2;
    }

    if (records.size() > nbRecordsToKeep) {

      while (cptRecords < nbRecordsToKeep) {
        recordsToKeep.add(records.get(cptRecords));
        cptRecords++;
      }
      records.clear();
      records.addAll(recordsToKeep);
    }
  }

  @Override
  public String toString() {

    return this.getClass().getSimpleName()
        + "{name=" + getName() + ", numberMatch=" + this.numberMatch + "}";
  }

}
