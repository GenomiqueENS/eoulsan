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

import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_TFQ;

import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapperService;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;

public abstract class AbstractFilterAndMapReadsStep extends AbstractStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final int MAX_MAPPING_QUALITY_THRESHOLD = 255;

  private static final String STEP_NAME = "filterandmap";
  private static final String COUNTER_GROUP = "filter_map_reads";

  private int lengthThreshold = -1;
  private double qualityThreshold = -1;
  private boolean pairEnd;

  private SequenceReadsMapper mapper;
  private String mapperArguments;
  private int mapperThreads = -1;

  private int mappingQualityThreshold = -1;

  //
  // Getters
  //

  /**
   * Get the length threshold
   * @return Returns the lengthThreshold
   */
  protected int getLengthThreshold() {
    return this.lengthThreshold;
  }

  /**
   * Get the quality threshold
   * @return Returns the qualityThreshold
   */
  protected double getQualityThreshold() {
    return this.qualityThreshold;
  }

  /**
   * Get the counter group to use for this step.
   * @return the counter group of this step
   */
  protected String getCounterGroup() {
    return COUNTER_GROUP;
  }

  /**
   * Test if the step works in pair end mode.
   * @return true if the pair end mode is enable
   */
  protected boolean isPairend() {
    return this.pairEnd;
  }

  /**
   * Get the name of the mapper to use.
   * @return Returns the mapperName
   */
  protected String getMapperName() {
    return this.mapper.getMapperName();
  }

  /**
   * Get the name of the mapper to use.
   * @return Returns the mapperName
   */
  protected String getMapperArguments() {
    return this.mapperArguments;
  }

  /**
   * Get the name of the mapper to use.
   * @return Returns the mapperName
   */
  protected int getMapperThreads() {
    return this.mapperThreads;
  }

  /**
   * Get the mapper.
   * @return the mapper object
   */
  protected SequenceReadsMapper getMapper() {

    return this.mapper;
  }

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
  public DataFormat[] getInputFormats() {
    return new DataFormat[] {READS_FASTQ, READS_TFQ,
        this.mapper.getArchiveFormat(), DataFormats.GENOME_DESC_TXT};
  }

  @Override
  public DataFormat[] getOutputFormats() {
    return new DataFormat[] {DataFormats.FILTERED_MAPPER_RESULTS_SAM};
  }

  @Override
  public String getDescription() {

    return "This step filters, map reads and filter alignment results.";
  }

  @Override
  public void configure(final Set<Parameter> stepParameters,
      final Set<Parameter> globalParameters) throws EoulsanException {

    String mapperName = null;

    for (Parameter p : stepParameters) {

      if ("lengththreshold".equals(p.getName()))
        this.lengthThreshold = p.getIntValue();
      else if ("qualitythreshold".equals(p.getName()))
        this.qualityThreshold = p.getDoubleValue();
      else if ("pairend".equals(p.getName()))
        this.pairEnd = p.getBooleanValue();
      else if ("mapper".equals(p.getName()))
        mapperName = p.getStringValue();
      else if ("mapperArguments".equals(p.getName()))
        this.mapperArguments = p.getStringValue();
      else if ("mappingqualitythreshold".equals(p.getName()))
        mappingQualityThreshold = p.getIntValue();
      else
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());

    }

    if (mapperName == null) {
      throw new EoulsanException("No mapper set.");
    }

    this.mapper =
        SequenceReadsMapperService.getInstance().getMapper(mapperName);

    if (this.mapper == null) {
      throw new EoulsanException("Unknown mapper: " + mapperName);
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
    LOGGER
        .info("In " + getName() + ", lengththreshold=" + this.lengthThreshold);
    LOGGER.info("In "
        + getName() + ", qualitythreshold=" + this.qualityThreshold);
    LOGGER.info("In " + getName() + ", pairend=" + this.pairEnd);
    LOGGER.info("In " + getName() + ", mapper=" + this.mapper.getMapperName());
    LOGGER
        .info("In " + getName() + ", mapperarguments=" + this.mapperArguments);
    LOGGER.info("In "
        + getName() + ", mappingQualityThreshold="
        + this.mappingQualityThreshold);
  }

}
