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

package fr.ens.biologie.genomique.eoulsan.modules.generators;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getGenericLogger;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.GENOME_FASTA;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.AbstractReadsMapperModule.MAPPER_FLAVOR_PARAMETER_NAME;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.AbstractReadsMapperModule.MAPPER_VERSION_PARAMETER_NAME;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.kenetre.util.Version;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.MapperIndexDataFormat;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.eoulsan.modules.mapping.AbstractFilterAndMapReadsModule;
import fr.ens.biologie.genomique.eoulsan.modules.mapping.AbstractReadsMapperModule;
import fr.ens.biologie.genomique.kenetre.bio.GenomeDescription;
import fr.ens.biologie.genomique.kenetre.bio.readmapper.Mapper;
import fr.ens.biologie.genomique.kenetre.bio.readmapper.MapperBuilder;
import fr.ens.biologie.genomique.kenetre.bio.readmapper.MapperInstance;
import fr.ens.biologie.genomique.kenetre.bio.readmapper.MapperInstanceBuilder;

/**
 * This class define a module that generate a genome mapper index.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class GenomeMapperIndexGeneratorModule extends AbstractModule {

  public static final String MODULE_NAME = "genericindexgenerator";

  private Mapper mapper;

  @Override
  public String getName() {

    return MODULE_NAME;
  }

  @Override
  public String getDescription() {

    return "Generate Mapper index";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {
    return new InputPortsBuilder().addPort("genome", GENOME_FASTA)
        .addPort("genomedescription", GENOME_DESC_TXT).create();
  }

  @Override
  public OutputPorts getOutputPorts() {
    return OutputPortsBuilder
        .singleOutputPort(new MapperIndexDataFormat(this.mapper));
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    if (stepParameters == null) {
      throw new EoulsanException(
          "No parameters set in " + getName() + " generator");
    }

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      // TODO replace with AbstractReadsMapperStep.MAPPER_NAME_PARAMETER_NAME ?
      case "mappername":
        final String mapperName = p.getStringValue();

        this.mapper = new MapperBuilder(mapperName)
            .withLogger(getGenericLogger()).build();

        if (this.mapper == null) {
          Modules.badParameterValue(MODULE_NAME, p, "Unknown mapper");
        }

        break;

      default:
        Modules.unknownParameter(MODULE_NAME, p);
      }
    }

  }

  /**
   * Set the version and the flavor of a mapper.
   * @param mapper mapper to configure
   * @param context the context of the task
   * @throws EoulsanException if more than one mapping step require this
   *           generator
   * @throws IOException if an error occurs while creating the mapper instance
   */
  static MapperInstance searchMapperVersionAndFlavor(final Mapper mapper,
      final TaskContext context) throws EoulsanException, IOException {

    int count = 0;
    String version = null;
    String flavor = null;

    for (Step step : context.getWorkflow().getSteps()) {

      if (AbstractReadsMapperModule.MODULE_NAME.equals(step.getModuleName())
          || AbstractFilterAndMapReadsModule.MODULE_NAME
              .equals(step.getModuleName())) {

        for (Parameter p : step.getParameters()) {

          switch (p.getName()) {

          case MAPPER_VERSION_PARAMETER_NAME:
            version = p.getStringValue();
            break;

          case MAPPER_FLAVOR_PARAMETER_NAME:
            flavor = p.getStringValue();
            break;

          default:
            break;
          }
        }
        count++;
      }
    }

    if (count > 1) {
      throw new EoulsanException(
          "Found more than one mapping step in the workflow");
    }

    // Set mapper temporary and executable temporary directories
    Mapper newMapper =
        new MapperBuilder(mapper)
            .withTempDirectory(context.getLocalTempDirectory())
            .withExecutablesTempDirectory(
                context.getSettings().getExecutablesTempDirectoryFile())
            .build();

    return new MapperInstanceBuilder(newMapper).withMapperVersion(version)
        .withMapperFlavor(flavor).withUseBundledBinaries(true).build();
  }

  /**
   * Execute the indexer.
   * @param mapper Mapper to use for the index generator
   * @param context Eoulsan context
   * @param additionalArguments additional indexer arguments
   * @param additionalDescription additional indexer arguments description
   * @param threadCount the number of thread to use
   */
  static void execute(final Mapper mapper, final TaskContext context,
      final String additionalArguments,
      final Map<String, String> additionalDescription, final int threadCount)
      throws IOException, EoulsanException {

    requireNonNull(mapper, "mapper argument cannot be null");
    requireNonNull(context, "context argument cannot be null");

    // Set default value for arguments if needed
    final String args = additionalArguments != null ? additionalArguments : "";

    // Set default value for descriptions if needed
    final Map<String, String> descriptions;
    if (additionalDescription != null) {
      descriptions = additionalDescription;
    } else {
      descriptions = Collections.emptyMap();
    }

    // Get input and output data
    final Data genomeData = context.getInputData(GENOME_FASTA);
    final Data genomeDescData = context.getInputData(GENOME_DESC_TXT);
    final Data outData =
        context.getOutputData(new MapperIndexDataFormat(mapper), genomeData);

    // Get the genome DataFile
    final DataFile genomeDataFile = genomeData.getDataFile();

    // Get the genome description DataFile
    final DataFile descDataFile = genomeDescData.getDataFile();
    final GenomeDescription desc = GenomeDescription.load(descDataFile.open());

    // Get the output DataFile
    final DataFile mapperIndexDataFile = outData.getDataFile();

    // Set the version and flavor
    final MapperInstance mapperInstance =
        searchMapperVersionAndFlavor(mapper, context);

    // Set the number of thread to use
    final int threads = threadCount < 1
        ? Runtime.getRuntime().availableProcessors() : threadCount;

    // Create indexer
    final GenomeMapperIndexer indexer =
        new GenomeMapperIndexer(mapperInstance, args, descriptions, threads);

    // Create index
    indexer.createIndex(genomeDataFile, desc, mapperIndexDataFile);
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    try {

      status.setProgressMessage(this.mapper.getName() + " index creation");

      // Create the index
      execute(this.mapper, context, null, null, 0);

    } catch (IOException | EoulsanException e) {

      return status.createTaskResult(e);
    }

    return status.createTaskResult();
  }
}
