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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.modules.mapping;

import static fr.ens.biologie.genomique.eoulsan.CommonHadoop.HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getGenericLogger;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.EoulsanRuntime.getSettings;
import static fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.io.IOException;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.Common;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.kenetre.bio.readmapper.Mapper;
import fr.ens.biologie.genomique.kenetre.bio.readmapper.MapperBuilder;
import fr.ens.biologie.genomique.kenetre.bio.readmapper.MapperInstanceBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.kenetre.util.Version;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;

/**
 * This class define an abstract module for read mapping.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class AbstractReadsMapperModule extends AbstractModule {

  /** Module name. */
  public static final String MODULE_NAME = "mapreads";

  protected static final String COUNTER_GROUP = "reads_mapping";

  protected static final String READS_PORT_NAME = "reads";
  protected static final String MAPPER_INDEX_PORT_NAME = "mapperindex";
  protected static final String GENOME_DESCRIPTION_PORT_NAME =
      "genomedescription";

  public static final String MAPPER_NAME_PARAMETER_NAME = "mapper";
  public static final String MAPPER_VERSION_PARAMETER_NAME = "mapper.version";
  public static final String MAPPER_FLAVOR_PARAMETER_NAME = "mapper.flavor";
  public static final String USE_BUNDLED_BINARIES_PARAMETER_NAME =
      "mapper.use.bundled.binares";
  public static final String MAPPER_DOCKER_IMAGE_PARAMETER_NAME =
      "mapper.docker.image";

  public static final String MAPPER_ARGUMENTS_PARAMETER_NAME =
      "mapper.arguments";
  public static final String HADOOP_THREADS_PARAMETER_NAME = "hadoop.threads";

  public static final String LOCAL_THREADS_PARAMETER_NAME = "local.threads";
  public static final String MAX_LOCAL_THREADS_PARAMETER_NAME =
      "max.local.threads";

  public static final int HADOOP_TIMEOUT = 60 * 60 * 1000;
  static final int DEFAULT_MAPPER_REQUIRED_MEMORY = 8 * 1024;

  private Mapper mapper;
  private String mapperVersion = "";
  private String mapperFlavor = "";
  private String mapperDockerImage = "";
  private boolean useBundledBinaries = true;
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
    return this.mapper.getName();
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
   * Test if the bundled binaries must be used to perform the step.
   * @return true if the bundled binaries must be used to perform the step
   */
  protected boolean isUseBundledBinaries() {
    return this.useBundledBinaries;
  }

  /**
   * Get the mapper Docker image to use.
   * @return the mapper Docker image to use
   */
  protected String getMapperDockerImage() {
    return this.mapperDockerImage;
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
  protected Mapper getMapper() {

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
  // Module methods
  //

  @Override
  public String getName() {

    return MODULE_NAME;
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

      // Check if the parameter is deprecated
      checkDeprecatedParameter(context, p);

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

      case USE_BUNDLED_BINARIES_PARAMETER_NAME:
        this.useBundledBinaries = p.getBooleanValue();
        break;

      case MAPPER_DOCKER_IMAGE_PARAMETER_NAME:
        this.mapperDockerImage = p.getStringValue();
        break;

      case MAPPER_ARGUMENTS_PARAMETER_NAME:
        this.mapperArguments = p.getStringValue();
        break;

      case HADOOP_THREADS_PARAMETER_NAME:
        this.hadoopThreads = p.getIntValueGreaterOrEqualsTo(1);
        break;

      case LOCAL_THREADS_PARAMETER_NAME:
        this.localThreads = p.getIntValueGreaterOrEqualsTo(1);
        break;

      case MAX_LOCAL_THREADS_PARAMETER_NAME:
        this.maxLocalThreads = p.getIntValueGreaterOrEqualsTo(1);
        break;

      case HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME:
        this.reducerTaskCount = p.getIntValueGreaterOrEqualsTo(1);
        break;

      default:
        Modules.unknownParameter(context, p);
      }
    }

    if (mapperName == null) {
      Modules.invalidConfiguration(context, "No mapper set");
    }

    try {
      // Create a Mapper object
      this.mapper = new MapperBuilder(mapperName).withLogger(getGenericLogger())
          .withApplicationName(Globals.APP_NAME)
          .withApplicationVersion(Globals.APP_VERSION_STRING)
          .withTempDirectory(getSettings().getTempDirectoryFile())
          .withExecutablesTempDirectory(
              EoulsanRuntime.getSettings().getExecutablesTempDirectoryFile())
          .build();

      // Check if the mapper wrapper has been found
      if (mapper == null) {
        Modules.invalidConfiguration(context, "Unknown mapper: " + mapperName);
      }

      // Check if the binary for the mapper is available

      // Create a new instance of the mapper for required version and flavor
      new MapperInstanceBuilder(this.mapper)
          .withMapperVersion(this.mapperVersion)
          .withMapperFlavor(this.mapperFlavor).withUseBundledBinaries(true)
          .withDockerImage(this.mapperDockerImage).build();

      // Check if the mapper is not only a generator
      if (mapper.isIndexGeneratorOnly()) {
        Modules.invalidConfiguration(context,
            "The selected mapper can only be used for index generation: "
                + mapperName);
      }

    } catch (IOException e) {
      throw new EoulsanException(e);
    }

    final int requiredMemory = context.getCurrentStep().getRequiredMemory();
    if (requiredMemory > 0) {
      this.hadoopMapperRequiredMemory = requiredMemory;
    }

    // Log Step parameters
    getLogger().info("In "
        + getName() + ", mapper=" + this.mapper.getName() + " (version: "
        + this.mapperVersion + ")");
    getLogger()
        .info("In " + getName() + ", mapperarguments=" + this.mapperArguments);
  }

  //
  // Other methods
  //

  /**
   * Check deprecated parameters.
   * @param context context
   * @param parameter the parameter to check
   * @throws EoulsanException if the parameter is no more supported
   */
  static void checkDeprecatedParameter(final StepConfigurationContext context,
      final Parameter parameter) throws EoulsanException {

    if (parameter == null) {
      return;
    }

    switch (parameter.getName()) {

    case "mapperarguments":
      Modules.renamedParameter(context, parameter,
          MAPPER_ARGUMENTS_PARAMETER_NAME, true);

    case MAPPER_NAME_PARAMETER_NAME:
      if ("soap".equals(parameter.getLowerStringValue())) {

        Modules.badParameterValue(context, parameter,
            "The SOAP mapper support has been removed from "
                + Globals.APP_NAME);
      }
      break;

    case "hadoop.mapper.required.memory":
      Modules.badParameterValue(context, parameter,
          "The "
              + parameter.getName()
              + " parameter no more exists. Use instead the \"requiredMemory\" "
              + "attribute of the step tag to define the amount of memory "
              + "required by the mapper in Hadoop mode.");
      break;

    default:
      break;
    }
  }
}
