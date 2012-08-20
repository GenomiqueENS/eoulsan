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

package fr.ens.transcriptome.eoulsan.steps.filtersamples;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;

/**
 * This abstract class define and parse arguments for the filter samples step.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class AbstractFilterSamplesStep extends AbstractStep {

  private static final String STEP_NAME = "filtersamples";

  private int threshold = 50;

  //
  // Getter
  //

  /**
   * Get the threshold
   * @return Returns the threshold
   */
  protected int getThreshold() {
    return this.threshold;
  }

  //
  // Step methods
  //

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public String getDescription() {

    return "This step filter samples.";
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    for (Parameter p : stepParameters) {
      if ("threshold".equals(p.getName()))
        this.threshold = p.getIntValue();
      else
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());

    }

    if (this.threshold < 0)
      throw new EoulsanException("The thresold is not set.");

  }

}
