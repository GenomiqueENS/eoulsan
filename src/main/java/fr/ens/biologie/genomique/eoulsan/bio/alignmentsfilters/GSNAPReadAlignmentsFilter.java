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
 * This class defines a filter to use after the mapper GSNAP (for the
 * compatibility with the expression estimation step).
 * @since 1.2
 * @author Claire Wallon
 */
public class GSNAPReadAlignmentsFilter extends AbstractReadAlignmentsFilter {

  public static final String FILTER_NAME = "gsnapfilter";

  @Override
  public String getName() {

    return FILTER_NAME;
  }

  @Override
  public String getDescription() {

    return "Remove all GSNAP alignments that are not supported by the expression estimation step";
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
      if (records.size() > 2) {
        records.clear();
      }
    }
  }

  @Override
  public String toString() {

    return this.getClass().getSimpleName() + "{name=" + getName() + "}";
  }

}
