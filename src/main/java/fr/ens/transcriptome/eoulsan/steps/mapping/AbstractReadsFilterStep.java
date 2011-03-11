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
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;

public abstract class AbstractReadsFilterStep extends AbstractStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  protected static final String STEP_NAME = "filterreads";

  protected static final String COUNTER_GROUP = "reads_filtering";

  /** filter reads length threshold. */
  public static final int LENGTH_THRESHOLD = 15;

  /** filter reads quality threshold. */
  public static final double QUALITY_THRESHOLD = 15;

  private int lengthThreshold = -1;
  private double qualityThreshold = -1;
  private boolean pairEnd;

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

  /**
   * Test if the step works in pair end mode.
   * @return true if the pair end mode is enable
   */
  protected boolean isPairend() {
    return this.pairEnd;
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
  public DataFormat[] getInputFormats() {
    return new DataFormat[] {DataFormats.READS_FASTQ};
  }

  @Override
  public DataFormat[] getOutputFormats() {
    return new DataFormat[] {DataFormats.FILTERED_READS_FASTQ};
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    for (Parameter p : stepParameters) {

      if ("lengththreshold".equals(p.getName()))
        this.lengthThreshold = p.getIntValue();
      else if ("qualitythreshold".equals(p.getName()))
        this.qualityThreshold = p.getDoubleValue();
      else if ("pairend".equals(p.getName()))
        this.pairEnd = p.getBooleanValue();
      else
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());

    }

    // Log Step parameters
    LOGGER
        .info("In " + getName() + ", lengththreshold=" + this.lengthThreshold);
    LOGGER.info("In "
        + getName() + ", qualitythreshold=" + this.qualityThreshold);
    LOGGER.info("In " + getName() + ", pairend=" + this.pairEnd);
  }

}
