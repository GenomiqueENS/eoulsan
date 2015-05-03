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

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.core.CommonHadoop.HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME;
import static fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;
import static fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsMapperStep.HADOOP_MAPPER_REQUIRED_MEMORY_PARAMETER_NAME;
import static fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsMapperStep.HADOOP_THREADS_PARAMETER_NAME;
import static fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsMapperStep.MAPPER_ARGUMENTS_PARAMETER_NAME;
import static fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsMapperStep.MAPPER_FLAVOR_PARAMETER_NAME;
import static fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsMapperStep.MAPPER_NAME_PARAMETER_NAME;
import static fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsMapperStep.MAPPER_VERSION_PARAMETER_NAME;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.MultiReadAlignmentsFilterBuilder;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.MultiReadFilterBuilder;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapperService;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define an abstract step for read filtering, mapping and alignments
 * filtering.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class AbstractFilterAndMapReadsStep extends AbstractStep {

  public static final String STEP_NAME = "filterandmap";
  private static final String COUNTER_GROUP = "filter_map_reads";

  protected static final String READS_PORT_NAME = "reads";
  protected static final String MAPPER_INDEX_PORT_NAME = "mapperindex";
  protected static final String GENOME_DESCRIPTION_PORT_NAME =
      "genomedescription";

  protected static final int HADOOP_TIMEOUT =
      AbstractReadsMapperStep.HADOOP_TIMEOUT;

  private boolean pairedEnd;

  private Map<String, String> readsFiltersParameters;
  private Map<String, String> alignmentsFiltersParameters;
  private SequenceReadsMapper mapper;
  private String mapperVersion = "";
  private String mapperFlavor = "";
  private String mapperArguments;

  private int reducerTaskCount = -1;
  private int hadoopThreads = -1;

  private final int mappingQualityThreshold = -1;
  private int hadoopMapperRequiredMemory =
      AbstractReadsMapperStep.DEFAULT_MAPPER_REQUIRED_MEMORY;

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
  protected boolean isPairedEnd() {
    return this.pairedEnd;
  }

  /**
   * Get the name of the mapper to use.
   * @return Returns the mapperName
   */
  protected String getMapperName() {
    return this.mapper.getMapperName();
  }

  /**
   * Get the version of the mapper to use.
   * @return the version of the mapper to use
   */
  protected String getMapperVersion() {
    return this.mapperVersion;
  }

  /**
   * Get the flavor of the mapper to use.
   * @return the flavor of the mapper to use
   */
  protected String getMapperFlavor() {
    return this.mapperVersion;
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
   * Get the amount in MB of memory required to execute the mapper. This value
   * is required by Hadoop scheduler and if the mapper require more memory than
   * declared the mapper process will be killed.
   * @return the amount of memory required by the mapper in MB
   */
  protected int getMapperHadoopMemoryRequired() {
    return this.hadoopMapperRequiredMemory;
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

  /**
   * Get the reducer task count.
   * @return the reducer task count
   */
  protected int getReducerTaskCount() {

    return this.reducerTaskCount;
  }

  /**
   * Get the parameters of the read filters.
   * @return a map with all the parameters of the filters
   */
  protected Map<String, String> getReadFilterParameters() {

    return this.readsFiltersParameters;
  }

  /**
   * Get the parameters of the read alignments filters.
   * @return a map with all the parameters of the filters
   */
  protected Map<String, String> getAlignmentsFilterParameters() {

    return this.alignmentsFiltersParameters;
  }

  //
  // Step methods
  //

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();
    builder.addPort(READS_PORT_NAME, READS_FASTQ);
    builder.addPort(MAPPER_INDEX_PORT_NAME, this.mapper.getArchiveFormat());

    return builder.create();
  }

  @Override
  public OutputPorts getOutputPorts() {
    return singleOutputPort(MAPPER_RESULTS_SAM);
  }

  @Override
  public String getDescription() {

    return "This step filters, map reads and filter alignment results.";
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    String mapperName = null;
    final MultiReadFilterBuilder mrfb = new MultiReadFilterBuilder();
    final MultiReadAlignmentsFilterBuilder mrafb =
        new MultiReadAlignmentsFilterBuilder();

    for (Parameter p : stepParameters) {

      // Check if the parameter is deprecated
      AbstractReadsFilterStep.checkDeprecatedParameter(p);
      AbstractReadsMapperStep.checkDeprecatedParameter(p);
      AbstractSAMFilterStep.checkDeprecatedParameter(p);

      switch (p.getName()) {

      case MAPPER_NAME_PARAMETER_NAME:
        mapperName = p.getStringValue();
        break;

      case MAPPER_VERSION_PARAMETER_NAME:
        mapperVersion = p.getStringValue();
        break;

      case MAPPER_FLAVOR_PARAMETER_NAME:
        mapperFlavor = p.getStringValue();
        break;

      case MAPPER_ARGUMENTS_PARAMETER_NAME:
        this.mapperArguments = p.getStringValue();
        break;

      case HADOOP_THREADS_PARAMETER_NAME:
        this.hadoopThreads = p.getIntValueGreaterOrEqualsTo(1);
        break;

      case HADOOP_MAPPER_REQUIRED_MEMORY_PARAMETER_NAME:
        this.hadoopMapperRequiredMemory =
            p.getIntValueGreaterOrEqualsTo(1) * 1024;
        break;

      case HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME:
        this.reducerTaskCount = p.getIntValueGreaterOrEqualsTo(1);
        break;

      default:

        // Add read filters parameters
        if (!(mrfb.addParameter(p.getName(), p.getStringValue(), true) ||
        // Add read alignments filters parameters
        mrafb.addParameter(p.getName(), p.getStringValue(), true))) {

          throw new EoulsanException("Unknown parameter: " + p.getName());
        }
      }
    }

    // Force parameter checking
    mrfb.getReadFilter();
    mrafb.getAlignmentsFilter();

    this.readsFiltersParameters = mrfb.getParameters();
    this.alignmentsFiltersParameters = mrafb.getParameters();

    if (mapperName == null) {
      throw new EoulsanException("No mapper set.");
    }

    this.mapper =
        SequenceReadsMapperService.getInstance().newService(mapperName);

    if (this.mapper == null) {
      throw new EoulsanException("Unknown mapper: " + mapperName);
    }

    if (this.mapper.isIndexGeneratorOnly()) {
      throw new EoulsanException(
          "The selected mapper can only be used for index generation: "
              + mapperName);
    }

    // Check if the binary for the mapper is available
    try {
      this.mapper.setMapperVersionToUse(this.mapperVersion);
      this.mapper.setMapperFlavorToUse(this.mapperFlavor);
      this.mapper.prepareBinaries();
    } catch (IOException e) {
      throw new EoulsanException(e);
    }

    // Log Step parameters
    getLogger().info(
        "In "
            + getName() + ", mapper=" + this.mapper.getMapperName()
            + " (version: " + this.mapper.getMapperVersion() + ")");
    getLogger().info(
        "In " + getName() + ", mapperarguments=" + this.mapperArguments);
  }

}
