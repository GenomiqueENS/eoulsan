package fr.ens.transcriptome.eoulsan.steps.mapping;

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_BAM;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_INDEX_BAI;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define a step for converting SAM files into BAM.
 * @since 2.0
 * @author Laurent Jourdren
 */
public abstract class AbstractSAM2BAMStep extends AbstractStep {

  private static final String STEP_NAME = "sam2bam";

  protected static final String COUNTER_GROUP = "sam2bam";

  private int compressionLevel;

  //
  // Getters
  //

  /**
   * Get the compression level to use.
   * @return the compression level to use
   */
  protected int getCompressionLevel() {
    return this.compressionLevel;
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

    return "This step sam convert SAM files to BAM files.";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {
    return singleInputPort(MAPPER_RESULTS_SAM);
  }

  @Override
  public OutputPorts getOutputPorts() {

    return new OutputPortsBuilder().addPort("bam", MAPPER_RESULTS_BAM)
        .addPort("bai", MAPPER_RESULTS_INDEX_BAI).create();
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case "compression.level":

        final int level = p.getIntValue();

        if (level < 0 || level > 9) {
          throw new EoulsanException("Invalid compression level [0-9]: "
              + level + " step: " + p.getName());
        }

        this.compressionLevel = level;
        break;

      case "input.format":

        getLogger().warning(
            "Deprecated parameter \""
                + p.getName() + "\" for step " + getName());
        break;

      default:
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());
      }
    }
  }

}
