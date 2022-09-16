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
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.READS_FASTQ;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.AbstractReadsMapperModule.HADOOP_THREADS_PARAMETER_NAME;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.AbstractReadsMapperModule.MAPPER_ARGUMENTS_PARAMETER_NAME;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.AbstractReadsMapperModule.MAPPER_FLAVOR_PARAMETER_NAME;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.AbstractReadsMapperModule.MAPPER_NAME_PARAMETER_NAME;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.AbstractReadsMapperModule.MAPPER_VERSION_PARAMETER_NAME;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.kenetre.bio.alignmentfilter.MultiReadAlignmentFilterBuilder;
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.bio.readfilter.MultiReadFilterBuilder;
import fr.ens.biologie.genomique.kenetre.bio.readmapper.Mapper;
import fr.ens.biologie.genomique.kenetre.bio.readmapper.MapperBuilder;
import fr.ens.biologie.genomique.kenetre.bio.readmapper.MapperInstanceBuilder;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.kenetre.util.Version;
import fr.ens.biologie.genomique.eoulsan.data.MapperIndexDataFormat;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;

/**
 * This class define an abstract module for read filtering, mapping and
 * alignments filtering.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class AbstractFilterAndMapReadsModule extends AbstractModule {

  public static final String MODULE_NAME = "filterandmap";
  private static final String COUNTER_GROUP = "filter_map_reads";

  protected static final String READS_PORT_NAME = "reads";
  protected static final String MAPPER_INDEX_PORT_NAME = "mapperindex";
  protected static final String GENOME_DESCRIPTION_PORT_NAME =
      "genomedescription";

  protected static final int HADOOP_TIMEOUT =
      AbstractReadsMapperModule.HADOOP_TIMEOUT;

  private boolean pairedEnd;

  private Map<String, String> readsFiltersParameters;
  private Map<String, String> alignmentsFiltersParameters;
  private Mapper mapper;
  private String mapperVersion = "";
  private String mapperFlavor = "";
  private String mapperArguments;

  private int reducerTaskCount = -1;
  private int hadoopThreads = -1;

  private int hadoopMapperRequiredMemory =
      AbstractReadsMapperModule.DEFAULT_MAPPER_REQUIRED_MEMORY;

  //
  // Getters
  //

  /**
   * Get the counter group to use for this module.
   * @return the counter group of this module
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
  // Module methods
  //

  @Override
  public String getName() {

    return MODULE_NAME;
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();
    builder.addPort(READS_PORT_NAME, READS_FASTQ);
    builder.addPort(MAPPER_INDEX_PORT_NAME,
        new MapperIndexDataFormat(this.mapper));

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
    final MultiReadFilterBuilder readFilterBuilder =
        new MultiReadFilterBuilder(getGenericLogger());
    final MultiReadAlignmentFilterBuilder alignmentsFilterBuilder =
        new MultiReadAlignmentFilterBuilder(getGenericLogger());

    try {
      for (Parameter p : stepParameters) {

        // Check if the parameter is deprecated
        AbstractReadsFilterModule.checkDeprecatedParameter(context, p);
        AbstractReadsMapperModule.checkDeprecatedParameter(context, p);
        AbstractSAMFilterModule.checkDeprecatedParameter(context, p);

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

        case HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME:
          this.reducerTaskCount = p.getIntValueGreaterOrEqualsTo(1);
          break;

        default:

          // Add read filters parameters
          if (!(readFilterBuilder.addParameter(p.getName(), p.getStringValue(),
              true) ||
          // Add read alignments filters parameters
              alignmentsFilterBuilder.addParameter(p.getName(),
                  p.getStringValue(), true))) {

            Modules.unknownParameter(context, p);
          }
        }
      }

      // Force parameter checking
      readFilterBuilder.getReadFilter();
      alignmentsFilterBuilder.getAlignmentFilter();
    } catch (KenetreException e) {
      throw new EoulsanException(e);
    }

    this.readsFiltersParameters = readFilterBuilder.getParameters();
    this.alignmentsFiltersParameters = alignmentsFilterBuilder.getParameters();

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

      if (this.mapper == null) {
        Modules.invalidConfiguration(context, "Unknown mapper: " + mapperName);
      }

      if (this.mapper.isIndexGeneratorOnly()) {
        Modules.invalidConfiguration(context,
            "The selected mapper can only be used for index generation: "
                + mapperName);
      }

      // Check if the binary for the mapper is available

      // Create a new instance of the mapper for required version and flavor
      new MapperInstanceBuilder(this.mapper)
          .withMapperVersion(this.mapperVersion)
          .withMapperFlavor(this.mapperFlavor).withUseBundledBinaries(true)
          .build();

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

}
