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

package fr.ens.transcriptome.eoulsan.steps.mapping;

import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_TFQ;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.MultiReadAlignmentsFilterBuilder;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.MultiReadFilterBuilder;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapperService;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;

/**
 * This class define an abstract step for read filtering, mapping and alignments
 * filtering.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class AbstractFilterAndMapReadsStep extends AbstractStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String STEP_NAME = "filterandmap";
  private static final String COUNTER_GROUP = "filter_map_reads";

  protected static final int HADOOP_TIMEOUT =
      AbstractReadsMapperStep.HADOOP_TIMEOUT;

  private boolean pairEnd;

  private MultiReadFilterBuilder readFilterBuilder;
  private MultiReadAlignmentsFilterBuilder readAlignmentsFilterBuilder;
  private SequenceReadsMapper mapper;
  private String mapperArguments;
  private int hadoopThreads = -1;

  private int mappingQualityThreshold = -1;

  //
  // Getters
  //

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
  protected int getMapperHadoopThreads() {
    return this.hadoopThreads;
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
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    // NEW VERSION...

    String mapperName = null;
    final MultiReadFilterBuilder mrfb = new MultiReadFilterBuilder();
    final MultiReadAlignmentsFilterBuilder mrafb =
        new MultiReadAlignmentsFilterBuilder();

    for (Parameter p : stepParameters) {

      if ("mapper".equals(p.getName()))
        mapperName = p.getStringValue();

      else if ("mapperarguments".equals(p.getName())
          || "mapper.arguments".equals(p.getName()))
        this.mapperArguments = p.getStringValue();
      else if ("hadoop.threads".equals(p.getName()))
        this.hadoopThreads = p.getIntValue();

      // TODO mrfb/mrafb.addParameter must return false if the filter is not found
      // TODO Remove the list of allowed filter reads parameters
      // read filters parameters
      else if ("paircheck".equals(p.getName())
          || "pairend.accept.pairend".equals(p.getName())
          || "pairend.accept.accept.singlend".equals(p.getName())
          || "illuminaid".equals(p.getName())
          || "quality.threshold".equals(p.getName())
          || "qualityThreshold".equals(p.getName())
          || "trim.length.threshold".equals(p.getName())
          || "lengthThreshold".equals(p.getName())) {
        mrfb.addParameter(
            AbstractReadsFilterStep.convertCompatibilityFilterKey(p.getName()),
            p.getStringValue());
      }

      // read alignments filters parameters
      else {
        mrafb.addParameter(
            AbstractSAMFilterStep.convertCompatibilityFilterKey(p.getName()),
            p.getStringValue());
      }

    }

    // Force parameter checking
    mrfb.getReadFilter();
    mrafb.getAlignmentsFilter();

    this.readFilterBuilder = mrfb;
    this.readAlignmentsFilterBuilder = mrafb;

    if (mapperName == null)
      throw new EoulsanException("No mapper set.");

    this.mapper =
        SequenceReadsMapperService.getInstance().getMapper(mapperName);

    if (this.mapper == null)
      throw new EoulsanException("Unknown mapper: " + mapperName);

    if (this.mapper.isIndexGeneratorOnly())
      throw new EoulsanException(
          "The selected mapper can only be used for index generation: "
              + mapperName);

    // Log Step parameters
    LOGGER.info("In "
        + getName() + ", mapper=" + this.mapper.getMapperName() + " (version: "
        + mapper.getMapperVersion() + ")");
    LOGGER
        .info("In " + getName() + ", mapperarguments=" + this.mapperArguments);
  }

  /**
   * Get the parameters of the read filters.
   * @return a map with all the parameters of the filters
   */
  protected Map<String, String> getReadFilterParameters() {

    return this.readFilterBuilder.getParameters();
  }

  /**
   * Get the parameters of the read alignments filters.
   * @return a map with all the parameters of the filters
   */
  protected Map<String, String> getAlignmentsFilterParameters() {

    return this.readAlignmentsFilterBuilder.getParameters();
  }

}
