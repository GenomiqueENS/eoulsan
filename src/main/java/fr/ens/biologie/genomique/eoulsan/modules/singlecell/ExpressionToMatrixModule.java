package fr.ens.biologie.genomique.eoulsan.modules.singlecell;

import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.EXPRESSION_MATRIX_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TSV;
import static fr.ens.biologie.genomique.eoulsan.modules.singlecell.RSingleCellExperimentCreatorModule.mergeExpressionResults;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.kenetre.bio.ExpressionMatrix;
import fr.ens.biologie.genomique.kenetre.bio.io.ExpressionMatrixWriter;
import fr.ens.biologie.genomique.kenetre.bio.io.SparseExpressionMatrixWriter;
import fr.ens.biologie.genomique.kenetre.bio.io.TSVExpressionMatrixWriter;
import fr.ens.biologie.genomique.kenetre.util.Version;
import java.io.IOException;
import java.util.Set;

/**
 * This class define a class that allow to merge expression file into a matrix file.
 *
 * @author Laurent Jourdren
 * @since 2.4
 */
@LocalOnly
public class ExpressionToMatrixModule extends AbstractModule {

  /** Module name */
  private static final String MODULE_NAME = "expression2matrix";

  private boolean denseFormat = true;

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

    return singleInputPort(EXPRESSION_RESULTS_TSV);
  }

  @Override
  public OutputPorts getOutputPorts() {
    return OutputPortsBuilder.singleOutputPort(EXPRESSION_MATRIX_TSV);
  }

  @Override
  public void configure(StepConfigurationContext context, Set<Parameter> stepParameters)
      throws EoulsanException {

    for (Parameter p : stepParameters) {

      switch (p.getName()) {
        case "dense.output.format":
          this.denseFormat = p.getBooleanValue();
          break;

        default:
          Modules.unknownParameter(context, p);
          break;
      }
    }
  }

  @Override
  public TaskResult execute(final TaskContext context, final TaskStatus status) {

    try {

      Data inputData = context.getInputData(EXPRESSION_RESULTS_TSV);
      Data outputData = context.getOutputData(EXPRESSION_MATRIX_TSV, inputData);

      // Create matrix
      final ExpressionMatrix matrix = mergeExpressionResults(inputData);

      // Write matrix
      try (ExpressionMatrixWriter writer =
          this.denseFormat
              ? new SparseExpressionMatrixWriter(outputData.getDataFile().create())
              : new TSVExpressionMatrixWriter(outputData.getDataFile().create())) {
        writer.write(matrix);
      }

    } catch (IOException e) {
      return status.createTaskResult(e);
    }

    return status.createTaskResult();
  }
}
