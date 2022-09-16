package fr.ens.biologie.genomique.eoulsan.modules.singlecell;

import static fr.ens.biologie.genomique.kenetre.bio.io.CellRangerExpressionMatrixWriter.DEFAULT_FEATURE_TYPE;
import static fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder.noOutputPort;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ADDITIONAL_ANNOTATION_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.EXPRESSION_MATRIX_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TSV;
import static fr.ens.biologie.genomique.eoulsan.modules.singlecell.RSingleCellExperimentCreatorModule.mergeExpressionResults;
import static fr.ens.biologie.genomique.eoulsan.modules.singlecell.RSingleCellExperimentCreatorModule.mergeMatrices;

import java.io.IOException;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.kenetre.util.Version;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.kenetre.bio.AnnotationMatrix;
import fr.ens.biologie.genomique.kenetre.bio.ExpressionMatrix;
import fr.ens.biologie.genomique.kenetre.bio.io.AnnotationMatrixReader;
import fr.ens.biologie.genomique.kenetre.bio.io.CellRangerExpressionMatrixWriter;
import fr.ens.biologie.genomique.kenetre.bio.io.TSVAnnotationMatrixReader;

/**
 * This class define a class that allow to create a Cell Ranger matrix from an
 * existing matrix file.
 * @author Laurent Jourdren
 * @since 2.4
 */
@LocalOnly
public class MatrixToCellRangerMatrixModule extends AbstractModule {

  /** Module name */
  private static final String MODULE_NAME = "matrix2cellrangermatrix";

  private boolean inputMatrices = true;
  private boolean useAdditionalAnnotation = true;
  private String featureAnnotationFieldName = "Gene name";
  private String featureAnnotationType = DEFAULT_FEATURE_TYPE;
  private int cellRangerMatrixFormat = 2;

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

    if (this.useAdditionalAnnotation) {
      builder.addPort("additionalannotation", ADDITIONAL_ANNOTATION_TSV);
    }

    if (this.inputMatrices) {
      builder.addPort("matrix", true, EXPRESSION_MATRIX_TSV);
    } else {
      builder.addPort("expression", true, EXPRESSION_RESULTS_TSV);
    }

    return builder.create();
  }

  @Override
  public OutputPorts getOutputPorts() {
    return noOutputPort();
  }

  @Override
  public void configure(StepConfigurationContext context,
      Set<Parameter> stepParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case "input.matrices":
        this.inputMatrices = p.getBooleanValue();
        break;

      case "use.additional.annotation":
        this.useAdditionalAnnotation = p.getBooleanValue();
        break;

      case "additional.annotation.field.name":
        this.featureAnnotationFieldName = p.getValue();
        break;

      case "additional.annotation.type":
        this.featureAnnotationType = p.getValue();
        break;

      case "cell.ranger.matrix.format":
        this.cellRangerMatrixFormat = p.getIntValueInRange(1, 2);
        break;

      default:
        Modules.unknownParameter(context, p);
        break;
      }
    }

  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    Data matrices = context.getInputData(
        this.inputMatrices ? EXPRESSION_MATRIX_TSV : EXPRESSION_RESULTS_TSV);

    AnnotationMatrix geneAnnotation = null;

    try {

      // Load additional annotation
      if (this.useAdditionalAnnotation) {
        context.getLogger().fine("Load additional annotation");

        try (AnnotationMatrixReader reader = new TSVAnnotationMatrixReader(
            context.getInputData(ADDITIONAL_ANNOTATION_TSV).getDataFile()
                .open())) {

          geneAnnotation = reader.read();
        }
      }

      // Load matrix
      final ExpressionMatrix matrix = this.inputMatrices
          ? mergeMatrices(matrices) : mergeExpressionResults(matrices);

      // Write the matrix
      try (CellRangerExpressionMatrixWriter writer =
          new CellRangerExpressionMatrixWriter(
              context.getStepOutputDirectory().toFile(), geneAnnotation,
              this.featureAnnotationFieldName, this.cellRangerMatrixFormat,
              this.featureAnnotationType)) {
        writer.write(matrix);
      }

    } catch (IOException e) {
      return status.createTaskResult(e);
    }

    return status.createTaskResult();
  }

}
