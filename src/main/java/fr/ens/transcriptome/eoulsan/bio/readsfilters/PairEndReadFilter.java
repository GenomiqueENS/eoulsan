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

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;

/**
 * This class define a read filter that allow to filter paired-end reads of
 * single end reads.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class PairEndReadFilter implements ReadFilter {

  private boolean acceptPairEnd = true;
  private final boolean acceptSingleEnd = true;

  @Override
  public boolean accept(final ReadSequence read) {

    return this.acceptSingleEnd;
  }

  @Override
  public boolean accept(final ReadSequence read1, final ReadSequence read2) {

    if (read1 == null || read2 == null) {
      return false;
    }

    return this.acceptPairEnd;
  }

  @Override
  public String getName() {

    return "pairend";
  }

  @Override
  public String getDescription() {

    return "Pair end ReadFilter";
  }

  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {

    if (key == null || value == null) {
      return;
    }

    if ("accept.pairend".equals(key.trim())) {
      this.acceptPairEnd = Boolean.parseBoolean(value.trim());
    } else if ("accept.singlend".equals(key.trim())) {
      this.acceptPairEnd = Boolean.parseBoolean(value.trim());
    } else {
      throw new EoulsanException("Unknown parameter for "
          + getName() + " read filter: " + key);
    }

  }

  @Override
  public void init() {
  }

  @Override
  public String toString() {

    return this.getClass().getSimpleName()
        + "{acceptSingleEnd=" + this.acceptSingleEnd + ", acceptPairEnd="
        + this.acceptPairEnd + "}";
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public PairEndReadFilter() {
  }

}
