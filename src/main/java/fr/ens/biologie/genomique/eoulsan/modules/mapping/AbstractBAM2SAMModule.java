package fr.ens.biologie.genomique.eoulsan.modules.mapping;

import static fr.ens.biologie.genomique.eoulsan.CommonHadoop.HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME;
import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.biologie.genomique.eoulsan.core.Modules.removedParameter;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_BAM;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.Version;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import htsjdk.samtools.SAMFileHeader.SortOrder;

/**
 * This class define a module for converting BAM files into SAM.
 * @since 2.0
 * @author Laurent Jourdren
 */
public abstract class AbstractBAM2SAMModule extends AbstractModule {

  private static final String MODULE_NAME = "bam2sam";
  protected static final String COUNTER_GROUP = "bam2sam";

  private int reducerTaskCount = -1;
  private SortOrder sortOrder = SortOrder.coordinate;

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

  /**
   * Get the sort order.
   * @return the sort order
   */
  protected SortOrder getSortOrder() {

    return this.sortOrder;
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

    return "This module sam convert BAM files to SAM files.";
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
        removedParameter(context, p);
        break;

      case HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME:
        this.reducerTaskCount = p.getIntValueGreaterOrEqualsTo(1);
        break;

      case "sort.order":
        try {
          this.sortOrder = SortOrder.valueOf(p.getLowerStringValue());
        } catch (IllegalArgumentException e) {
          Modules.badParameterValue(context, p, "Unknown sort order");
        }
        break;

      default:
        throw new EoulsanException(
            "Unknown parameter for " + getName() + " step: " + p.getName());
      }
    }
  }

}
