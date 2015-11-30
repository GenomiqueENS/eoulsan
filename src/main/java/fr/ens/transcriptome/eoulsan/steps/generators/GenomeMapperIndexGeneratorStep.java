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

package fr.ens.transcriptome.eoulsan.steps.generators;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.GENOME_FASTA;
import static fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsMapperStep.MAPPER_FLAVOR_PARAMETER_NAME;
import static fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsMapperStep.MAPPER_VERSION_PARAMETER_NAME;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapperService;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.Steps;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractFilterAndMapReadsStep;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsMapperStep;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define a step that generate a genome mapper index.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class GenomeMapperIndexGeneratorStep extends AbstractStep {

  public static final String STEP_NAME = "genericindexgenerator";

  private SequenceReadsMapper mapper;

  @Override
  public String getName() {

    return STEP_NAME;
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
    return OutputPortsBuilder.singleOutputPort(this.mapper.getArchiveFormat());
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

        this.mapper =
            SequenceReadsMapperService.getInstance().newService(mapperName);

        if (this.mapper == null) {
          Steps.badParameterValue(context, p, "Unknown mapper");
        }

        break;

      default:
        Steps.unknownParameter(context, p);
      }
    }

  }

  /**
   * Set the version and the flavor of a mapper.
   * @param mapper mapper to configure
   * @param context the context of the task
   * @throws EoulsanException if more than one mapping step require this
   *           generator
   */
  static void searchMapperVersionAndFlavor(final SequenceReadsMapper mapper,
      final StepContext context) throws EoulsanException {

    int count = 0;
    String version = null;
    String flavor = null;

    for (WorkflowStep step : context.getWorkflow().getSteps()) {

      if (AbstractReadsMapperStep.STEP_NAME.equals(step.getStepName())
          || AbstractFilterAndMapReadsStep.STEP_NAME
              .equals(step.getStepName())) {

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

    // Set the version and the flavor to use
    mapper.setMapperVersionToUse(version);
    mapper.setMapperFlavorToUse(flavor);
  }

  /**
   * Execute the indexer.
   * @param mapper Mapper to use for the index generator
   * @param context Eoulsan context
   * @param additionalArguments additional indexer arguments
   * @param additionalDescription additional indexer arguments description
   * @param threadCount the number of thread to use
   */
  static void execute(final SequenceReadsMapper mapper,
      final StepContext context, final String additionalArguments,
      final Map<String, String> additionalDescription, final int threadCount)
          throws IOException, EoulsanException {

    checkNotNull(mapper, "mapper argument cannot be null");
    checkNotNull(context, "context argument cannot be null");

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
        context.getOutputData(mapper.getArchiveFormat(), genomeData);

    // Get the genome DataFile
    final DataFile genomeDataFile = genomeData.getDataFile();

    // Get the genome description DataFile
    final DataFile descDataFile = genomeDescData.getDataFile();
    final GenomeDescription desc = GenomeDescription.load(descDataFile.open());

    // Get the output DataFile
    final DataFile mapperIndexDataFile = outData.getDataFile();

    // Set the version and flavor
    searchMapperVersionAndFlavor(mapper, context);

    // Set mapper temporary directory
    mapper.setTempDirectory(context.getLocalTempDirectory());

    // Set the number of thread to use
    final int threads = threadCount < 1
        ? Runtime.getRuntime().availableProcessors() : threadCount;
    mapper.setThreadsNumber(threads);

    // Create indexer
    final GenomeMapperIndexer indexer =
        new GenomeMapperIndexer(mapper, args, descriptions);

    // Create index
    indexer.createIndex(genomeDataFile, desc, mapperIndexDataFile);
  }

  @Override
  public StepResult execute(final StepContext context,
      final StepStatus status) {

    try {

      status
          .setProgressMessage(this.mapper.getMapperName() + " index creation");

      // Create the index
      execute(this.mapper, context, null, null, 0);

    } catch (IOException | EoulsanException e) {

      return status.createStepResult(e);
    }

    return status.createStepResult();
  }
}
