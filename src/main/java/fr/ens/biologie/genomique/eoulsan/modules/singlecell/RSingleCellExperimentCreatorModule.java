package fr.ens.biologie.genomique.eoulsan.modules.singlecell;

import static fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ADDITIONAL_ANNOTATION_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.EXPRESSION_MATRIX_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.SINGLE_CELL_EXPERMIMENT_RDS;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.bio.AnnotationMatrix;
import fr.ens.biologie.genomique.eoulsan.bio.DenseAnnotationMatrix;
import fr.ens.biologie.genomique.eoulsan.bio.ExpressionMatrix;
import fr.ens.biologie.genomique.eoulsan.bio.SparseExpressionMatrix;
import fr.ens.biologie.genomique.eoulsan.bio.io.AnnotationMatrixReader;
import fr.ens.biologie.genomique.eoulsan.bio.io.AnnotationMatrixWriter;
import fr.ens.biologie.genomique.eoulsan.bio.io.CountsReader;
import fr.ens.biologie.genomique.eoulsan.bio.io.ExpressionMatrixFormatFinderInputStream;
import fr.ens.biologie.genomique.eoulsan.bio.io.ExpressionMatrixReader;
import fr.ens.biologie.genomique.eoulsan.bio.io.ExpressionMatrixWriter;
import fr.ens.biologie.genomique.eoulsan.bio.io.TSVAnnotationMatrixReader;
import fr.ens.biologie.genomique.eoulsan.bio.io.TSVAnnotationMatrixWriter;
import fr.ens.biologie.genomique.eoulsan.bio.io.TSVCountsReader;
import fr.ens.biologie.genomique.eoulsan.bio.io.TSVExpressionMatrixWriter;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.core.Version;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.eoulsan.modules.diffana.RModuleCommonConfiguration;
import fr.ens.biologie.genomique.eoulsan.requirements.Requirement;
import fr.ens.biologie.genomique.eoulsan.util.r.RExecutor;

/**
 * This class define a class that allow to create a SingleCellExperiment
 * Bioconductor Object and save it as a RDS file.
 * @author Laurent Jourdren
 * @since 2.3
 */
@LocalOnly
public class RSingleCellExperimentCreatorModule extends AbstractModule {

  /** Module name */
  private static final String MODULE_NAME = "rsinglecellexperimentcreator";

  private static final String R_DOCKER_IMAGE_DEFAULT =
      "genomicpariscentre/singlecellexperiment:3.7";

  private final Set<Requirement> requirements = new HashSet<>();
  private RExecutor executor;

  private static final char CELL_SEPARATOR = '_';

  private boolean inputMatrices = true;
  private String designPrefix = "Cell.";
  private boolean useAdditionnalAnnotation = true;

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
  public OutputPorts getOutputPorts() {
    return singleOutputPort(SINGLE_CELL_EXPERMIMENT_RDS);
  }

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();

    if (this.useAdditionnalAnnotation) {
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
  public Set<Requirement> getRequirements() {

    return unmodifiableSet(this.requirements);
  }

  @Override
  public void configure(StepConfigurationContext context,
      Set<Parameter> stepParameters) throws EoulsanException {

    // Parse R executor parameters
    final Set<Parameter> parameters = new HashSet<>(stepParameters);
    this.executor = RModuleCommonConfiguration.parseRExecutorParameter(context,
        parameters, this.requirements, R_DOCKER_IMAGE_DEFAULT);

    for (Parameter p : parameters) {

      switch (p.getName()) {

      case "use.docker":
        Modules.renamedParameter(context, p,
            RModuleCommonConfiguration.EXECUTION_MODE_PARAMETER, true);
        break;

      case "input.matrices":
        this.inputMatrices = p.getBooleanValue();
        break;

      case "design.prefix":
      case "design.prefix.for.cell.annotation":
        this.designPrefix = p.getStringValue();
        break;

      case "use.gene.annotation":
        this.useAdditionnalAnnotation = p.getBooleanValue();
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

    try {

      // Define output files
      File temporaryDirectory = context.getLocalTempDirectory();
      File outputDir = context.getStepOutputDirectory().toFile();
      File matrixFile = new File(outputDir, "matrix.tsv");
      File genesFile = this.useAdditionnalAnnotation
          ? new File(outputDir, "genes.tsv") : null;
      File cellsFile =
          this.designPrefix.isEmpty() ? null : new File(outputDir, "cells.tsv");
      File rdsFile =
          context.getOutputData(SINGLE_CELL_EXPERMIMENT_RDS, "matrix")
              .getDataFile().toFile();

      // Save R script ?
      boolean saveRScript = context.getSettings().isSaveRscripts();

      // Create R Input files
      createRInputFiles(context, matrixFile, genesFile, cellsFile);

      // Launch R and create RDS file
      createRDS(matrixFile, cellsFile, genesFile, rdsFile, temporaryDirectory,
          saveRScript, context.getStepOutputDirectory());

    } catch (IOException e) {
      return status.createTaskResult(e);
    }

    return status.createTaskResult();
  }

  private void createRInputFiles(final TaskContext context,
      final File matrixFile, final File genesFile, final File cellsFile)
      throws IOException {

    Data matrices = context.getInputData(
        this.inputMatrices ? EXPRESSION_MATRIX_TSV : EXPRESSION_RESULTS_TSV);

    AnnotationMatrix geneAnnotation = null;
    AnnotationMatrix cellAnnotation = null;

    // Create final matrix
    context.getLogger().fine("Load matrix");
    final ExpressionMatrix matrix = this.inputMatrices
        ? mergeMatrices(matrices) : mergeExpressionResults(matrices);

    // Load gene annotation from additional annotation

    if (genesFile != null) {
      context.getLogger().fine("Load additional annotation");

      try (AnnotationMatrixReader reader = new TSVAnnotationMatrixReader(context
          .getInputData(ADDITIONAL_ANNOTATION_TSV).getDataFile().open())) {

        geneAnnotation = reader.read();
      }

      // Filter gene annotations
      context.getLogger().fine("Filter gene annotation");
      geneAnnotation.retainRows(matrix.getRowNames());
    }

    // Parse the design to get cell annotation
    if (cellsFile != null) {
      context.getLogger().fine("Get cell annotation");
      cellAnnotation = getCellAnnotation(matrices,
          context.getWorkflow().getDesign(), this.designPrefix);
    }

    // Duplicate cell annotation for each cell for 10X
    if (this.inputMatrices && cellAnnotation != null) {
      context.getLogger().fine("Duplicate cell annotation");
      duplicateCellAnnotation(cellAnnotation, matrix.getColumnNames());
    }

    // Save matrix data
    context.getLogger().fine("Save matrix");
    try (ExpressionMatrixWriter writer =
        new TSVExpressionMatrixWriter(matrixFile)) {
      writer.write(matrix);
    }

    // Save gene annotation
    if (geneAnnotation != null) {
      context.getLogger().fine("Save gene annotation");
      try (AnnotationMatrixWriter writer =
          new TSVAnnotationMatrixWriter(genesFile)) {
        writer.write(geneAnnotation, matrix.getRowNames());
      }
    }

    // Save cell annotation
    if (cellAnnotation != null) {
      context.getLogger().fine("Save cell annotation");
      try (AnnotationMatrixWriter writer =
          new TSVAnnotationMatrixWriter(cellsFile)) {
        writer.write(cellAnnotation, matrix.getColumnNames());
      }
    }

  }

  //
  // Matrix and annotation management
  //

  /**
   * Merge the matrices in one matrix
   * @param matrices the input data matrices
   * @return an Expression matrix object
   * @throws IOException if an error occurs while reading the input files
   */
  static ExpressionMatrix mergeMatrices(final Data matrices)
      throws IOException {

    requireNonNull(matrices, "matrices argument cannot be null");

    final ExpressionMatrix result = new SparseExpressionMatrix();

    // Do not copy matrix when there is only one matrix file
    final boolean oneMatrix = matrices.getListElements().size() == 1;

    for (Data matrixData : matrices.getListElements()) {

      // Determine the format of the input expression matrix
      try (ExpressionMatrixFormatFinderInputStream in =
          new ExpressionMatrixFormatFinderInputStream(
              matrixData.getDataFile().open())) {

        // Create reader
        try (ExpressionMatrixReader reader = in.getExpressionMatrixReader()) {

          // Read matrix
          ExpressionMatrix matrix =
              reader.read(oneMatrix ? result : new SparseExpressionMatrix());

          // Get sample name
          String sampleName = matrixData.getName();

          // Rename the column with sample name
          for (String colName : matrix.getColumnNames()) {
            matrix.renameColumn(colName, sampleName + CELL_SEPARATOR + colName);
          }

          // Add matrix to the final matrix
          if (!oneMatrix) {
            result.add(matrix);
          }
        }
      }
    }

    return result;
  }

  /**
   * Merge the matrices in one matrix
   * @param matrices the input data matrices
   * @return an Expression matrix object
   * @throws IOException if an error occurs while reading the input files
   */
  static ExpressionMatrix mergeExpressionResults(final Data matrices)
      throws IOException {

    requireNonNull(matrices, "matrices argument cannot be null");

    final ExpressionMatrix result = new SparseExpressionMatrix();

    for (Data matrixData : matrices.getListElements()) {

      // Get the sample name
      String sampleName = matrixData.getName();

      try (CountsReader reader =
          new TSVCountsReader(matrixData.getDataFile().open())) {

        // Put the expression results in the matrix
        for (Map.Entry<String, Integer> e : reader.read().entrySet()) {

          result.setValue(e.getKey(), sampleName, e.getValue());
        }
      }
    }

    return result;
  }

  /**
   * Get cell annotation from data metadata or if not found from the design.
   * @param matrices the matrices
   * @param design design to parse
   * @param prefix prefix for sample metadata to use
   * @return an Annotation object
   */
  private static AnnotationMatrix getCellAnnotation(final Data matrices,
      final Design design, final String prefix) {

    final AnnotationMatrix result = new DenseAnnotationMatrix();

    // Get Cell annotation from data metadata if exists
    for (Data d : matrices.getListElements()) {

      for (String key : d.getMetadata().keySet()) {

        if (key.startsWith(prefix)) {
          result.setValue(d.getName(), key.substring(prefix.length()),
              d.getMetadata().get(key));
        }
      }
    }

    // Get Cell annotation from design
    for (Data d : matrices.getListElements()) {

      if (!result.containsRow(d.getName())
          && design.containsSample(d.getName())) {

        Sample s = design.getSample(d.getName());

        for (String key : s.getMetadata().keySet()) {

          if (key.startsWith(prefix)) {
            result.setValue(d.getName(), key.substring(prefix.length()),
                d.getMetadata().get(key));
          }
        }
      }
    }

    return result;
  }

  /**
   * Duplicate cell annotation for 10X data.
   * @param cellAnnotation cell annotation
   * @param cellUMI cell identifiers
   */
  private static void duplicateCellAnnotation(
      final AnnotationMatrix cellAnnotation, final Collection<String> cellUMI) {

    for (String rowName : cellAnnotation.getRowNames()) {

      for (String cellId : cellUMI) {

        if (cellId.startsWith(rowName + CELL_SEPARATOR)) {

          for (String colName : cellAnnotation.getColumnNames()) {

            cellAnnotation.setValue(cellId, colName,
                cellAnnotation.getValue(rowName, colName));
          }
        }
      }
    }

    cellAnnotation.retainRows(cellUMI);
  }

  //
  // Execution methods
  //

  /**
   * Create the RDS file.
   * @param matrixFile matrix file
   * @param cellsFile cell annotations file
   * @param genesFile gene annotations file
   * @param rdsFile RDS output file
   * @param temporaryDirectory temporary directory
   * @param saveRScript if R script must be saved
   * @param RExecutionDirectory R execution directory
   * @throws IOException if an error occurs while executing the command
   */
  private void createRDS(final File matrixFile, final File cellsFile,
      final File genesFile, final File rdsFile, final File temporaryDirectory,
      final boolean saveRScript, final DataFile RExecutionDirectory)
      throws IOException {

    // Open executor connection
    this.executor.openConnection();

    // Put input files
    this.executor.putInputFile(new DataFile(matrixFile));
    if (cellsFile != null) {
      this.executor.putInputFile(new DataFile(cellsFile));
    }
    if (genesFile != null) {
      this.executor.putInputFile(new DataFile(genesFile));
    }

    // Create R script
    final String rScriptSource =
        createRScriptSource(matrixFile, cellsFile, genesFile, rdsFile);

    this.executor.executeRScript(rScriptSource, false, null, saveRScript,
        "sce-rds", RExecutionDirectory);

    // Remove input files
    this.executor.removeInputFiles();

    // Retrieve output files
    this.executor.getOutputFiles();

    // Close executor connection
    this.executor.closeConnection();
  }

  /**
   * Create R script.
   * @param matrixFile matrix file
   * @param cellsFile cell annotations file
   * @param genesFile gene annotations file
   * @param rdsFile RDS output file
   * @return the R script file
   * @throws IOException if an error occurs while creating the file
   */
  private static String createRScriptSource(final File matrixFile,
      final File cellsFile, final File genesFile, final File rdsFile)
      throws IOException {

    StringBuilder sb = new StringBuilder();

    sb.append("#!/usr/bin/env Rscript\n");

    sb.append("\n# Generated by "
        + Globals.APP_NAME + " " + Globals.APP_VERSION_STRING + "\n");
    sb.append("# Generated on " + new Date() + "\n");

    sb.append("\n# Import SingleCellExperiment package\n");
    sb.append("library(SingleCellExperiment)\n");

    sb.append("\n# Load counts\n");
    sb.append("values <- read.table(\""
        + matrixFile.getName()
        + "\", header=TRUE, row.names=1, check.names=FALSE, sep=\"\\t\")\n");

    // Load cell annotations if required
    if (cellsFile != null) {
      sb.append("\n# Load cell annotations\n");
      sb.append("cells <-read.delim(\""
          + cellsFile.getName()
          + "\", header=TRUE, row.names=1, check.names=FALSE, sep=\"\\t\")\n");
    }

    // Load gene annotations if required
    if (genesFile != null) {
      sb.append("\n# Load gene annotations\n");
      sb.append("genes <- read.delim(\""
          + genesFile.getName()
          + "\", header=TRUE, row.names=1, check.names=FALSE, sep=\"\\t\")\n");
    }

    // Create SingleCellExperiment object
    sb.append("\n# Create SingleCellExperiment object\n");
    sb.append("sce <- SingleCellExperiment("
        + "assays = list(counts = as.matrix(values))");

    if (cellsFile != null) {
      sb.append(", colData = data.frame(cells)");
    }

    sb.append(")\n");

    // Save SingleCellExperiment object in a RDS file
    sb.append("\n# Create RDS file from SingleCellExperiment object\n");
    sb.append("saveRDS(sce, \"" + rdsFile.getName() + "\")\n");

    // Print information about the R session
    sb.append("\n# Print session info\n");
    sb.append("sessionInfo()\n");

    return sb.toString();
  }

}
