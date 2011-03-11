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

package fr.ens.transcriptome.eoulsan.bio.readsfilters;

import fr.ens.transcriptome.eoulsan.bio.ReadSequence;

public class PairEndReadFilter implements ReadFilter {

  private final boolean pairEnd;

  @Override
  public boolean accept(final ReadSequence read) {

    return !this.pairEnd;
  }

  @Override
  public boolean accept(final ReadSequence read1, final ReadSequence read2) {

    return this.pairEnd;
  }
  
  @Override
  public String getName() {

    return "Pair end ReadFilter";
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param pairEnd true if only pair end entries must be accepted or false if
   *          only single end entries must be accepted
   */
  public PairEndReadFilter(final boolean pairEnd) {

    this.pairEnd = pairEnd;

  }

}
