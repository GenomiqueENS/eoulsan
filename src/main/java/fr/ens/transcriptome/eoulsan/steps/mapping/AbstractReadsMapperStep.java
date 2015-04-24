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

import java.io.IOException;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapperService;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define an abstract step for read mapping.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class AbstractReadsMapperStep extends AbstractStep {

  public static final String STEP_NAME = "mapreads";

  protected static final String COUNTER_GROUP = "reads_mapping";

  protected static final String READS_PORT_NAME = "reads";
  protected static final String MAPPER_INDEX_PORT_NAME = "mapperindex";
  protected static final String GENOME_DESCRIPTION_PORT_NAME =
      "genomedescription";

  public static final String MAPPER_NAME_PARAMETER_NAME = "mapper";
  public static final String MAPPER_VERSION_PARAMETER_NAME = "mapper.version";
  public static final String MAPPER_FLAVOR_PARAMETER_NAME = "mapper.flavor";

  public static final String MAPPER_ARGUMENTS_PARAMETER_NAME =
      "mapper.arguments";
  public static final String HADOOP_MAPPER_REQUIRED_MEMORY_PARAMETER_NAME =
      "hadoop.mapper.required.memory";
  public static final String HADOOP_THREADS_PARAMETER_NAME = "hadoop.threads";

  public static final String LOCAL_THREADS_PARAMETER_NAME = "local.threads";
  public static final String MAX_LOCAL_THREADS_PARAMETER_NAME =
      "max.local.threads";

  public static final int HADOOP_TIMEOUT = 60 * 60 * 1000;
  static final int DEFAULT_MAPPER_REQUIRED_MEMORY = 8 * 1024;

  private SequenceReadsMapper mapper;
  private String mapperVersion = "";
  private String mapperFlavor = "";
  private String mapperArguments;

  private int reducerTaskCount = -1;
  private int hadoopThreads;
  private int localThreads;
  private int maxLocalThreads;
  private int hadoopMapperRequiredMemory = DEFAULT_MAPPER_REQUIRED_MEMORY;

  //
  // Getters
  //

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
    return this.mapperFlavor;
  }

  /**
   * Get the arguments of the mapper to use.
   * @return Returns the mapperArguments
   */
  protected String getMapperArguments() {
    return this.mapperArguments;
  }

  /**
   * Get the number of threads to use in local mode.
   * @return Returns the mapperThreads
   */
  protected int getMapperLocalThreads() {

    return Common.getThreadsNumber(this.localThreads, this.maxLocalThreads);
  }

  /**
   * Get the number of threads to use in local mode.
   * @return Returns the mapperThreads
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
   * Get the mapper object.
   * @return the mapper object
   */
  protected SequenceReadsMapper getMapper() {

    return this.mapper;
  }

  /**
   * Get the reducer task count.
   * @return the reducer task count
   */
  protected int getReducerTaskCount() {

    return this.reducerTaskCount;
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
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public OutputPorts getOutputPorts() {
    return singleOutputPort(MAPPER_RESULTS_SAM);
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    String mapperName = null;

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case MAPPER_NAME_PARAMETER_NAME:
        mapperName = p.getStringValue();
        break;

      case MAPPER_VERSION_PARAMETER_NAME:
        this.mapperVersion = p.getStringValue();
        break;

      case MAPPER_FLAVOR_PARAMETER_NAME:
        this.mapperFlavor = p.getStringValue();
        break;

      case MAPPER_ARGUMENTS_PARAMETER_NAME:
        this.mapperArguments = p.getStringValue();
        break;

      case HADOOP_THREADS_PARAMETER_NAME:
        this.hadoopThreads = p.getIntValue();
        break;

      case LOCAL_THREADS_PARAMETER_NAME:
        this.localThreads = p.getIntValue();
        break;

      case MAX_LOCAL_THREADS_PARAMETER_NAME:
        this.maxLocalThreads = p.getIntValue();
        break;

      case HADOOP_MAPPER_REQUIRED_MEMORY_PARAMETER_NAME:
        this.hadoopMapperRequiredMemory = p.getIntValue() * 1024;
        break;

      case HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME:

        final int reducerTaskCount = p.getIntValue();

        if (reducerTaskCount < 1) {
          throw new EoulsanException("Invalid "
              + HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME + " parameter value: "
              + p.getValue());
        }

        this.reducerTaskCount = reducerTaskCount;
        break;

      default:

        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());
      }
    }

    if (mapperName == null) {
      throw new EoulsanException("No mapper set.");
    }

    this.mapper =
        SequenceReadsMapperService.getInstance().newService(mapperName);

    // Check if the mapper wrapper has been found
    if (this.mapper == null) {
      throw new EoulsanException("Unknown mapper: " + mapperName);
    }

    // Check if the mapper is not only a generator
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
