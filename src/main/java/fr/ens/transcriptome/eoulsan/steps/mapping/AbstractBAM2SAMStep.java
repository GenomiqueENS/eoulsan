package fr.ens.transcriptome.eoulsan.steps.mapping;

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.core.CommonHadoop.HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME;
import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_BAM;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define a step for converting BAM files into SAM.
 * @since 2.0
 * @author Laurent Jourdren
 */
public abstract class AbstractBAM2SAMStep extends AbstractStep {

  private static final String STEP_NAME = "bam2sam";
  protected static final String COUNTER_GROUP = "bam2sam";

  private int reducerTaskCount = -1;

  //
  // Getters
  //

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

    return "This step sam convert BAM files to SAM files.";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {
    return singleInputPort(MAPPER_RESULTS_BAM);
  }

  @Override
  public OutputPorts getOutputPorts() {

    return new OutputPortsBuilder().addPort("sam", MAPPER_RESULTS_SAM).create();
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case "input.format":
        getLogger().warning("Deprecated parameter \""
            + p.getName() + "\" for step " + getName());
        break;

      case HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME:
        this.reducerTaskCount = p.getIntValueGreaterOrEqualsTo(1);
        break;

      default:
        throw new EoulsanException(
            "Unknown parameter for " + getName() + " step: " + p.getName());
      }
    }
  }

}
