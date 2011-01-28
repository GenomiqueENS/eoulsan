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

import static fr.ens.transcriptome.eoulsan.data.DataFormats.FILTERED_MAPPER_RESULTS_SAM;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;

public abstract class AbstractSAMFilterStep extends AbstractStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String STEP_NAME = "filtersam";

  private static final int MAX_MAPPING_QUALITY_THRESHOLD = 255;
  protected static final String COUNTER_GROUP = "sam_filtering";

  private int mappingQualityThreshold = -1;

  //
  // Getters
  //

  /**
   * Get the mapping quality threshold.
   * @return the quality mapping threshold
   */
  protected int getMappingQualityThreshold() {

    return this.mappingQualityThreshold;
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

    return "This step filters sam files.";
  }

  @Override
  public DataFormat[] getInputFormats() {
    return new DataFormat[] {MAPPER_RESULTS_SAM, GENOME_DESC_TXT};
  }

  @Override
  public DataFormat[] getOutputFormats() {
    return new DataFormat[] {FILTERED_MAPPER_RESULTS_SAM};
  }

  @Override
  public void configure(final Set<Parameter> stepParameters,
      final Set<Parameter> globalParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      if ("mappingqualitythreshold".equals(p.getName()))
        mappingQualityThreshold = p.getIntValue();

      else
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());

    }

    if (this.mappingQualityThreshold == -1) {
      throw new EoulsanException("Mapping quality theshold not set.");
    }

    if (this.mappingQualityThreshold < 0
        || this.mappingQualityThreshold > MAX_MAPPING_QUALITY_THRESHOLD) {
      throw new EoulsanException("Invalid mapping quality theshold: "
          + this.mappingQualityThreshold);
    }

    // Log Step parameters
    LOGGER.info("In "
        + getName() + ", mappingQualityThreshold="
        + this.mappingQualityThreshold);
  }

}
