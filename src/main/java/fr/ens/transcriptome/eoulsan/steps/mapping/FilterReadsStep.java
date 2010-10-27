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

package fr.ens.transcriptome.eoulsan.steps.mapping;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.datatypes.DataType;
import fr.ens.transcriptome.eoulsan.datatypes.DataTypes;

/**
 * This abstract class define and parse arguments for the filter reads step.
 * @author Laurent Jourdren
 */
public abstract class FilterReadsStep extends AbstractStep {

  private static final String STEP_NAME = "filterreads";

  private int lengthThreshold = -1;
  private double qualityThreshold = -1;

  //
  // Getters
  //

  /**
   * Get the length threshold
   * @return Returns the lengthThreshold
   */
  protected int getLengthThreshold() {
    return lengthThreshold;
  }

  /**
   * Get the quality threshold
   * @return Returns the qualityThreshold
   */
  protected double getQualityThreshold() {
    return qualityThreshold;
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

    return "This step filters reads.";
  }

  @Override
  public DataType[] getInputTypes() {
    return new DataType[] {DataTypes.READS};
  }

  @Override
  public DataType[] getOutputType() {
    return new DataType[] {DataTypes.FILTERED_READS};
  }

  @Override
  public void configure(final Set<Parameter> stepParameters,
      final Set<Parameter> globalParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      if ("lengththreshold".equals(p.getName()))
        this.lengthThreshold = p.getIntValue();
      else if ("qualitythreshold".equals(p.getName()))
        this.qualityThreshold = p.getDoubleValue();
      else
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());

    }

  }

}
