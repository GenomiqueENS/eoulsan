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

/**
 * This class define a filter based on mean quality of a read.
 * @author Maria Bernard
 * @author Laurent Jourdren
 */
public class QualityReadFilter extends AbstractReadFilter {

  private double qualityThreshold;

  @Override
  public boolean accept(final ReadSequence read) {

    if (read == null)
      throw new NullPointerException("The read is null");

    return read.meanQuality() > this.qualityThreshold;
  }

  @Override
  public String getName() {

    return "Quality ReadFilter";
  }
  
  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param qualityThreshold The minimal threshold for mean quality of reads
   */
  public QualityReadFilter(final double qualityThreshold) {

    if (qualityThreshold < 0.0)
      throw new IllegalArgumentException("Invalid qualityThreshold: "
          + qualityThreshold);

    this.qualityThreshold = qualityThreshold;

  }

}
