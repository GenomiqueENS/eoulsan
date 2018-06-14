package fr.ens.biologie.genomique.eoulsan.modules.singlecell;

import static fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ADDITIONAL_ANNOTATION_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.EXPRESSION_MATRIX_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.SINGLE_CELL_EXPERMIMENT_RDS;
import static fr.ens.biologie.genomique.eoulsan.requirements.DockerRequirement.newDockerRequirement;
import static fr.ens.biologie.genomique.eoulsan.requirements.PathRequirement.newPathRequirement;
import static java.util.Collections.unmodifiableSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.bio.ExpressionMatrix;
import fr.ens.biologie.genomique.eoulsan.bio.SparseExpressionMatrix;
import fr.ens.biologie.genomique.eoulsan.bio.io.ExpressionMatrixReader;
import fr.ens.biologie.genomique.eoulsan.bio.io.ExpressionMatrixSparseReader;
import fr.ens.biologie.genomique.eoulsan.bio.io.ExpressionMatrixTSVWriter;
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
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.eoulsan.design.SampleMetadata;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.eoulsan.requirements.Requirement;
import fr.ens.biologie.genomique.eoulsan.util.GuavaCompatibility;
import fr.ens.biologie.genomique.eoulsan.util.process.DockerManager;
import fr.ens.biologie.genomique.eoulsan.util.process.SimpleProcess;
import fr.ens.biologie.genomique.eoulsan.util.process.SystemSimpleProcess;

/**
 * This class define a class that allow to create a SingleCellExperiment
 * Bioconductor Object and save it as a RDS file.
 * @author Laurent Jourdren
 * @since 2.3
 */
@LocalOnly
public class RSingleCellExperimentCreator extends AbstractModule {

  /** Module name */
  private static final String MODULE_NAME = "rsinglecellexperimentcreator";

  private static final String R_DOCKER_IMAGE =
      "genomicpariscentre/singlecellexperiment:3.7";
  private static final String R_EXECUTABLE = "R";

  private boolean dockerMode;
  private String dockerImage = R_DOCKER_IMAGE;
  private final Set<Requirement> requirements = new HashSet<>();

  private static final char CELL_SEPARATOR = '_';

  private boolean inputMatrices = true;
  private String designPrefix = "Cell.";

  /**
   * This class store annotation.
   */
  private static class Annotation {

    private final List<String> fieldNames = new ArrayList<>();
    private final Map<String, List<String>> values = new LinkedHashMap<>();

    private void retainGenes(final Collection<String> geneNames) {

      Set<String> keys = new HashSet<>(this.values.keySet());
      keys.removeAll(geneNames);

      for (String geneName : keys) {
        this.values.remove(geneName);
      }
    }

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
  public OutputPorts getOutputPorts() {
    return singleOutputPort(SINGLE_CELL_EXPERMIMENT_RDS);
  }

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();

    builder.addPort("additionalannotation", ADDITIONAL_ANNOTATION_TSV);

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

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case "use.docker":
        this.dockerMode = p.getBooleanValue();
        break;

      case "docker.image":
        this.dockerImage = p.getStringValue().trim();
        if (this.dockerImage.isEmpty()) {
          Modules.badParameterValue(context, p,
              "The docker image name is empty");
        }
        break;

      case "input.matrices":
        this.inputMatrices = p.getBooleanValue();
        break;

      case "design.prefix":
        this.designPrefix = p.getStringValue();
        break;

      default:
        Modules.unknownParameter(context, p);
        break;
      }
    }

    // Define requirements
    if (this.dockerMode) {
      this.requirements.add(newDockerRequirement(this.dockerImage));
    } else {
      this.requirements.add(newPathRequirement(R_DOCKER_IMAGE));
    }
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    Data matrices = context.getInputData(
        this.inputMatrices ? EXPRESSION_MATRIX_TSV : EXPRESSION_RESULTS_TSV);

    try {

      // Create final matrix
      final ExpressionMatrix matrix = this.inputMatrices
          ? mergeMatrices(matrices) : mergeExpressionResults(matrices);

      // Load gene annotation from additional annotation
      final Annotation geneAnnotation = readTSVAnnotation(
          context.getInputData(ADDITIONAL_ANNOTATION_TSV).getDataFile().open());

      // Filter gene annotations
      geneAnnotation.retainGenes(matrix.getRowNames());

      // Parse the design to get cell annotation
      Annotation cellAnnotation =
          parseDesign(context.getWorkflow().getDesign(), this.designPrefix);

      // Duplicate cell annotation for each cell for 10X
      if (this.inputMatrices) {
        duplicateCellAnnotation(cellAnnotation, matrix.getColumnNames());
      }

      // Define output files
      File temporaryDirectory = context.getLocalTempDirectory();
      File outputDir = context.getStepOutputDirectory().toFile();
      File matrixFile = new File(outputDir, "matrix.tsv");
      File genesFile = new File(outputDir, "genes.tsv");
      File cellsFile = new File(outputDir, "cells.tsv");
      File rdsFile =
          context.getOutputData(SINGLE_CELL_EXPERMIMENT_RDS, "matrix")
              .getDataFile().toFile();

      // Save data
      saveMatrix(matrix, matrixFile);
      saveAnnotation(geneAnnotation, matrix.getRowNames(), genesFile);
      saveAnnotation(cellAnnotation, matrix.getColumnNames(), cellsFile);

      // Launch R and create RDS file

      // Launch R
      if (this.dockerMode) {
        createRDSWithDocker(dockerImage, matrixFile, cellsFile, genesFile,
            rdsFile, temporaryDirectory);
      } else {
        createRDS(matrixFile, cellsFile, genesFile, rdsFile,
            temporaryDirectory);
      }

    } catch (IOException e) {
      return status.createTaskResult(e);
    }

    return status.createTaskResult();
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
  private static ExpressionMatrix mergeMatrices(final Data matrices)
      throws IOException {

    final ExpressionMatrix result = new SparseExpressionMatrix();

    for (Data matrixData : matrices.getListElements()) {

      // TODO Create a Reader that can determine the format of the input
      // expression matrix

      // Create reader
      ExpressionMatrixReader reader =
          new ExpressionMatrixSparseReader(matrixData.getDataFile().open());

      // Read matrix
      ExpressionMatrix matrix = reader.read(new SparseExpressionMatrix());

      // Get sample name
      String sampleName = matrixData.getName();

      // Rename the column with sample name
      for (String colName : matrix.getColumnNames()) {
        matrix.renameColumn(colName, sampleName + CELL_SEPARATOR + colName);
      }

      // Add matrix to the final matrix
      result.add(matrix);
    }

    return result;
  }

  /**
   * Merge the matrices in one matrix
   * @param matrices the input data matrices
   * @return an Expression matrix object
   * @throws IOException if an error occurs while reading the input files
   */
  private static ExpressionMatrix mergeExpressionResults(final Data matrices)
      throws IOException {

    final ExpressionMatrix result = new SparseExpressionMatrix();

    for (Data matrixData : matrices.getListElements()) {

      // Get the sample name
      String sampleName = matrixData.getName();

      // Put the expression results in the matrix
      for (Map.Entry<String, Integer> e : readExpressionResults(
          matrixData.getDataFile().open()).entrySet()) {

        result.setValue(e.getKey(), sampleName, e.getValue());
      }
    }

    return result;
  }

  /**
   * Parse Design to generate cell annotation.
   * @param design design to parse
   * @param prefix prefix for sample metadata to use
   * @return an Annotation object
   */
  private static Annotation parseDesign(final Design design,
      final String prefix) {

    final Annotation result = new Annotation();

    // Search for metadata starting with the prefix
    for (Sample s : design.getSamples()) {
      for (String k : s.getMetadata().keySet()) {

        if (k.startsWith(prefix) && !result.fieldNames.contains(k)) {
          result.fieldNames.add(k);
        }
      }
    }

    // Fill the value
    for (Sample s : design.getSamples()) {

      String id = s.getId();
      List<String> values = new ArrayList<>();

      for (String field : result.fieldNames) {

        SampleMetadata sm = s.getMetadata();

        if (sm.contains(field)) {
          values.add(sm.get(field));
        } else {
          values.add("");
        }

      }
      result.values.put(id, values);
    }

    // Remove the prefix to the fieldnames
    for (int i = 0; i < result.fieldNames.size(); i++) {
      result.fieldNames.set(i,
          result.fieldNames.get(i).substring(prefix.length()));
    }

    return result;
  }

  /**
   * Duplicate cell annotation for 10X data.
   * @param cellAnnotation cell annotation
   * @param cellIds cell identifiers
   */
  private static void duplicateCellAnnotation(final Annotation cellAnnotation,
      final Collection<String> cellIds) {

    Set<String> oriKeys = new HashSet<>(cellAnnotation.values.keySet());

    for (String key : oriKeys) {

      for (String cellId : cellIds) {

        if (cellId.startsWith(key + CELL_SEPARATOR)) {
          cellAnnotation.values.put(cellId, cellAnnotation.values.get(key));
        }
      }
    }

    cellAnnotation.retainGenes(cellIds);
  }

  //
  // Files reading and saving
  //

  /**
   * Read annotation in TSV format.
   * @param in input stream
   * @return an Annotation object
   * @throws IOException if an error occurs while reading the file
   */
  private static final Annotation readTSVAnnotation(final InputStream in)
      throws IOException {

    final Annotation result = new Annotation();

    String line = null;
    int lineCount = 0;

    try (
        BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

      Splitter splitter = Splitter.on('\t').trimResults();
      boolean first = true;
      int expectedFieldCount = 0;
      while ((line = reader.readLine()) != null) {

        line = line.trim();
        lineCount++;

        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }

        List<String> fields = GuavaCompatibility.splitToList(splitter, line);

        if (first) {
          first = false;
          result.fieldNames.addAll(fields.subList(1, fields.size() - 1));
          expectedFieldCount = fields.size();
          continue;
        }

        // Add missing fields
        if (fields.size() < expectedFieldCount) {
          fields = new ArrayList<>(fields);
          fields.addAll(
              Collections.nCopies(expectedFieldCount - fields.size(), ""));
        }

        if (fields.size() != expectedFieldCount) {
          throw new IOException("Invalid number of fields ("
              + fields.size() + ") found line " + lineCount + ", "
              + expectedFieldCount + " fields are expected: " + line);
        }

        result.values.put(fields.get(0),
            fields.subList(1, expectedFieldCount - 1));
      }
    }

    return result;
  }

  /**
   * Load the content of an expression file.
   * @param in input stream of the expression file
   * @return a map with the counts
   * @throws IOException if an error occurs while reading the counts
   */
  private static Map<String, Integer> readExpressionResults(
      final InputStream in) throws IOException {

    // TODO Create an Expression(Result)TSVReader Expression(Result)TSVWriter
    // that handle Map<String,Integer>

    final Map<String, Integer> result = new LinkedHashMap<>();

    String line = null;
    int lineCount = 0;

    try (
        BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

      Splitter splitter = Splitter.on('\t').trimResults();

      boolean first = true;
      while ((line = reader.readLine()) != null) {

        line = line.trim();
        lineCount++;

        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }

        if (first) {
          first = false;
          continue;
        }

        List<String> fields = GuavaCompatibility.splitToList(splitter, line);
        if (fields.size() != 2) {
          throw new IOException("Invalid number of fields found line "
              + lineCount + ", 2 fields are expected: " + line);
        }

        try {
          result.put(fields.get(0), Integer.parseInt(fields.get(1)));
        } catch (NumberFormatException e) {
          throw new IOException(
              "Invalid count found line " + lineCount + ": " + line);
        }
      }
    }

    return result;
  }

  /**
   * Save matrix.
   * @param matrix matrix to save
   * @param outputFile output file
   * @throws IOException if an error occurs while saving the file
   */
  private static void saveMatrix(final ExpressionMatrix matrix,
      final File outputFile) throws IOException {

    // TODO Writer and Readers for expression matrix must be autoclosable
    new ExpressionMatrixTSVWriter(outputFile).write(matrix);
  }

  /**
   * Save annotation.
   * @param annotation annotation to save
   * @param entryOrder order of the entries to save
   * @param outputFile output file
   * @throws IOException if an error occurs while saving file
   */
  private static void saveAnnotation(Annotation annotation,
      List<String> entryOrder, File outputFile) throws IOException {

    try (FileWriter writer = new FileWriter(outputFile)) {

      Joiner joiner = Joiner.on('\t');

      writer.write('\t' + joiner.join(annotation.fieldNames) + '\n');

      for (String id : entryOrder) {

        if (annotation.values.containsKey(id)) {

          writer
              .write(id + '\t' + joiner.join(annotation.values.get(id)) + '\n');
        }
      }
    }
  }

  //
  // Execution methods
  //

  /**
   * Create the RDS file with R installed in the user path.
   * @param dockerImage docker image to use
   * @param matrixFile matrix file
   * @param cellsFile cell annotations file
   * @param genesFile gene annotations file
   * @param rdsFile RDS output file
   * @param temporaryDirectory temporary directory
   * @throws IOException if an error occurs while executing the command
   */
  private static void createRDSWithDocker(final String dockerImage,
      final File matrixFile, final File cellsFile, final File genesFile,
      final File rdsFile, final File temporaryDirectory) throws IOException {

    // Create R script
    final File rScriptFile =
        createRScript(matrixFile, cellsFile, genesFile, rdsFile);

    SimpleProcess process = dockerImage == null
        ? new SystemSimpleProcess()
        : DockerManager.getInstance().createImageInstance(dockerImage);

    File executionDirectory = rdsFile.getParentFile();
    File stdoutFile = new File(executionDirectory, "r.stdout");
    File stderrFile = new File(executionDirectory, "r.stderr");

    // Define the list of the files/directory to mount in the Docker instance
    List<File> filesUsed = new ArrayList<>();
    filesUsed.add(executionDirectory);
    filesUsed.add(temporaryDirectory);
    filesUsed.add(rScriptFile);
    filesUsed.add(matrixFile);
    filesUsed.add(cellsFile);
    filesUsed.add(genesFile);
    filesUsed.add(rdsFile);

    // Launch Docker container
    final int exitValue = process.execute(createRCommand(rScriptFile),
        executionDirectory, temporaryDirectory, stdoutFile, stderrFile,
        rScriptFile, matrixFile, cellsFile, genesFile);

    if (exitValue > 0) {
      throw new IOException("Invalid exit code of R: " + exitValue);
    }
  }

  /**
   * Create the RDS file with R installed in the user path.
   * @param matrixFile matrix file
   * @param cellsFile cell annotations file
   * @param genesFile gene annotations file
   * @param rdsFile RDS output file
   * @param temporaryDirectory temporary directory
   * @throws IOException if an error occurs while executing the command
   */
  private void createRDS(final File matrixFile, final File cellsFile,
      final File genesFile, final File rdsFile, final File temporaryDirectory)
      throws IOException {

    createRDSWithDocker(null, matrixFile, cellsFile, genesFile, rdsFile,
        temporaryDirectory);
  }

  /**
   * Create execution command.
   * @param rScriptFile the path to the R script
   * @return a list with the arguments of the command to launch
   */
  private static List<String> createRCommand(final File rScriptFile) {

    List<String> result = new ArrayList<>();

    // The executable name
    result.add(R_EXECUTABLE);

    // The command arguments
    result.add("--no-save");
    result.add("-e");
    result.add("source(\"" + rScriptFile.getAbsolutePath() + "\")");

    return result;
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
  private static File createRScript(final File matrixFile, final File cellsFile,
      final File genesFile, final File rdsFile) throws IOException {

    File rScriptFile = new File(rdsFile.getParentFile(), "sce-rds.R");

    try (FileWriter writer = new FileWriter(rScriptFile)) {

      writer.write("# Import SingleCellExperiment package\n");
      writer.write("library(SingleCellExperiment)\n");

      writer.write("\n# Load counts\n");
      writer.write("values <- read.table(\""
          + matrixFile.getAbsolutePath()
          + "\", header=TRUE, row.names=1, sep=\"\\t\")\n");

      writer.write("\n# Load cell annotations\n");
      writer.write("cells <-read.delim(\""
          + cellsFile.getAbsolutePath()
          + "\", header=TRUE, row.names=1, sep=\"\\t\")");

      writer.write("\n# Load gene annotations\n");
      writer.write("genes <- read.delim(\""
          + genesFile.getAbsolutePath()
          + "\", header=TRUE, row.names=1, sep=\"\\t\")\n");

      writer.write("\n# Create SingleCellExperiment object\n");
      writer.write("sce <- SingleCellExperiment("
          + "assays = list(counts = as.matrix(values)), "
          + "colData = data.frame(cells), " + "rowData = data.frame(genes))\n");

      writer.write("\n# Create RDS file\n");
      writer.write("saveRDS(sce, \"" + rdsFile.getAbsolutePath() + "\")\n");
    }

    return rScriptFile;
  }

}
