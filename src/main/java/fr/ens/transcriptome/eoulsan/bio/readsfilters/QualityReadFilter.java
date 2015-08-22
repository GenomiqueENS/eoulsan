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

import static fr.ens.transcriptome.eoulsan.util.StatUtils.mean;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;

/**
 * This class define a filter based on mean quality of a read.
 * @since 1.0
 * @author Maria Bernard
 * @author Laurent Jourdren
 */
public class QualityReadFilter extends AbstractReadFilter {

  public static final String FILTER_NAME = "quality";
  private double qualityThreshold = -1.0;

  @Override
  public boolean accept(final ReadSequence read) {

    if (read == null) {
      return false;
    }

    return mean(read.qualityScores()) > this.qualityThreshold;
  }

  @Override
  public String getName() {

    return "quality";
  }

  @Override
  public String getDescription() {
    return "Quality threshold ReadFilter";
  }

  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {

    if (key == null || value == null) {
      return;
    }

    if ("threshold".equals(key.trim())) {

      try {
        this.qualityThreshold = Double.parseDouble(value.trim());
      } catch (NumberFormatException e) {
        return;
      }

      if (this.qualityThreshold < 0.0) {
        throw new EoulsanException(
            "Invalid qualityThreshold: " + this.qualityThreshold);
      }
    } else {
      throw new EoulsanException(
          "Unknown parameter for " + getName() + " read filter: " + key);
    }

  }

  @Override
  public void init() {

    if (this.qualityThreshold < 0.0) {
      throw new IllegalArgumentException(
          "Quality threshold is not set for " + getName() + " read filter.");
    }
  }

  @Override
  public String toString() {

    return this.getClass().getSimpleName()
        + "{qualityThreshold=" + this.qualityThreshold + "}";
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public QualityReadFilter() {

  }

}
