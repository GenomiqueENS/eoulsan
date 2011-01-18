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
