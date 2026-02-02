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

package fr.ens.biologie.genomique.eoulsan.modules.diffana;

import static fr.ens.biologie.genomique.eoulsan.design.DesignUtils.getAllSamplesMetadataKeys;
import static fr.ens.biologie.genomique.eoulsan.design.DesignUtils.getExperimentSampleAllMetadataKeys;
import static fr.ens.biologie.genomique.eoulsan.design.DesignUtils.referenceValueToInt;
import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.checkers.DESeq2DesignChecker;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.DesignUtils;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.design.ExperimentMetadata;
import fr.ens.biologie.genomique.eoulsan.design.ExperimentSample;
import fr.ens.biologie.genomique.eoulsan.design.ExperimentSampleMetadata;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.eoulsan.design.SampleMetadata;
import fr.ens.biologie.genomique.eoulsan.util.r.RExecutor;

/**
 * This class contains methods to run the differential analysis module DESeq2.
 * @author Xavier Bauquet
 * @since 2.0
 */
public abstract class AbstractEasyContrasts {

  // Suffix for output files from DEseq2
  private static final String DESEQ_DESIGN_FILE_SUFFIX = "-deseq2Design.txt";
  private static final String COMPARISON_FILE_SUFFIX = "-comparisonFile.txt";
  private static final String CONTRAST_FILE_SUFFIX = "-contrastFile.txt";

  // Constants
  private static final String SAMPLE_ID_FIELDNAME = "SampleId";
  private static final String SAMPLE_NAME_FIELDNAME = "Name";
  private static final String EXPRESSION_FILE_FIELDNAME = "expressionFile";
  private static final String TAB_SEPARATOR = "\t";
  private static final String NEWLINE = "\n";

  // The experiment
  private final Experiment experiment;

  // List of expression files
  final Map<String, File> sampleFiles;

  // The design
  private final Design design;

  // Workflow options for DEseq2
  private final DESeq2Parameters parameters;

  // Design options for DEseq2
  private final String model;
  private final boolean contrast;
  private final boolean buildContrast;
  private final DataFile contrastFile;

  // Files and file names
  protected final RExecutor executor;
  private final boolean saveRScripts;

  private final String stepId;

  // Temporary expression filenames
  private final Map<String, String> sampleFilenames = new HashMap<>();

  //
  // Getters
  //

  protected DESeq2Parameters getParameters() {
    return this.parameters;
  }

  protected String experimentName() {
    return this.experiment.getName();
  }

  protected String model() {
    return this.model;
  }

  protected boolean isSaveRScripts() {
    return this.saveRScripts;
  }

  protected String stepId() {
    return this.stepId;
  }

  protected boolean isBuildContrast() {
    return this.buildContrast;
  }

  protected boolean isContrast() {
    return this.contrast;
  }

  //
  // Filenames
  //

  /**
   * Define design filename.
   * @param prefix
   * @return the design filename
   */
  protected static String deseq2DesignFileName(String prefix) {

    return prefix + DESEQ_DESIGN_FILE_SUFFIX;
  }

  /**
   * Define comparison filename.
   * @param prefix
   * @return comparison filename
   */
  protected static String comparisonFileName(String prefix) {
    return prefix + COMPARISON_FILE_SUFFIX;
  }

  /**
   * Define contrast filename.
   * @param prefix
   * @return the contrast filename
   */
  protected static String contrastFilename(String prefix) {
    return prefix + CONTRAST_FILE_SUFFIX;
  }

  //
  // File writers
  //

  /**
   * Put sample files.
   * @throws IOException if an error occurs while putting sample files
   */
  protected void putSampleFiles() throws IOException {

    for (Sample sample : experiment.getSamples()) {

      // Check if the expression file related to the sample exist
      if (!this.sampleFiles.containsKey(sample.getId())) {
        continue;
      }

      final String key = sample.getId();
      final DataFile inputFile =
          new DataFile(this.sampleFiles.get(key).getAbsolutePath());
      final String outputFilename = "expression-" + key + ".tsv";

      this.executor.putInputFile(inputFile, outputFilename);
      this.sampleFilenames.put(key, outputFilename);
    }
  }

  protected void writeDESeq2Design(String prefix) throws IOException {
    this.executor.writeFile(generateDeseq2Design(),
        deseq2DesignFileName(prefix));
  }

  protected void writeContrastFile(String prefix) throws IOException {
    if (this.contrastFile != null) {
      this.executor.putInputFile(this.contrastFile, contrastFilename(prefix));
    }
  }

  protected void writeComparisonFile(String prefix)
      throws IOException, EoulsanException {
    this.executor.writeFile(generateComparisonFileContent(),
        comparisonFileName(prefix));
  }

  //
  // File content generators
  //

  /**
   * Generate DESeq2 design.
   * @return the DESeq2 in a string
   */
  private String generateDeseq2Design() {

    final StringBuilder sb = new StringBuilder();

    //
    // Print column names
    //
    sb.append(SAMPLE_ID_FIELDNAME);
    sb.append(TAB_SEPARATOR);
    sb.append(SAMPLE_NAME_FIELDNAME);
    sb.append(TAB_SEPARATOR);
    sb.append(EXPRESSION_FILE_FIELDNAME);

    // Get the common column names
    final List<String> sampleMDKeys = getAllSamplesMetadataKeys(design);

    // Get the experiment column names
    final List<String> experimentMDKeys =
        getExperimentSampleAllMetadataKeys(experiment);

    // Get Experiment reference
    final String experimentReference = experiment.getMetadata().getReference();

    final boolean referenceColumn = experimentReference != null
        || sampleMDKeys.contains(SampleMetadata.REFERENCE_KEY)
        || experimentMDKeys.contains(ExperimentSampleMetadata.REFERENCE_KEY);

    // Print common column names
    for (String key : sampleMDKeys) {
      if (!experimentMDKeys.contains(key)) {

        // The reference column will be the last column
        if (SampleMetadata.REFERENCE_KEY.equals(key)) {
          continue;
        }

        sb.append(TAB_SEPARATOR);
        sb.append(key);
      }
    }

    // Print experiment column names
    for (String key : experimentMDKeys) {

      // The reference column will be the last column
      if (ExperimentSampleMetadata.REFERENCE_KEY.equals(key)) {
        continue;
      }

      sb.append(TAB_SEPARATOR);
      sb.append(key);
    }

    // Add the column reference when the reference option is on the experiment
    // metadata
    if (referenceColumn) {

      sb.append(TAB_SEPARATOR);
      sb.append(SampleMetadata.REFERENCE_KEY);
    }

    sb.append(NEWLINE);

    // Print sample metadata
    for (Sample sample : experiment.getSamples()) {

      // Check if the expression file related to the sample exist
      if (!this.sampleFilenames.containsKey(sample.getId())) {
        continue;
      }

      sb.append(sample.getId());
      sb.append(TAB_SEPARATOR);
      sb.append(sample.getName());
      sb.append(TAB_SEPARATOR);
      sb.append(this.sampleFilenames.get(sample.getId()));

      final SampleMetadata smd = sample.getMetadata();

      for (String key : sampleMDKeys) {
        if (!experimentMDKeys.contains(key)) {

          // The reference column will be the last column
          if (SampleMetadata.REFERENCE_KEY.equals(key)) {
            continue;
          }

          sb.append(TAB_SEPARATOR);

          if (smd.contains(key)) {
            sb.append(smd.get(key));
          }
        }
      }

      // print experiment sample metadata
      final ExperimentSample es = experiment.getExperimentSample(sample);
      final ExperimentSampleMetadata expSampleMetadata = es.getMetadata();

      for (String key : experimentMDKeys) {

        // The reference column will be the last column
        if (ExperimentSampleMetadata.REFERENCE_KEY.equals(key)) {
          continue;
        }

        sb.append(TAB_SEPARATOR);

        if (expSampleMetadata.contains(key)) {
          sb.append(expSampleMetadata.get(key));
        }
      }

      // Add reference column
      if (referenceColumn) {

        sb.append(TAB_SEPARATOR);
        sb.append(referenceValueToInt(DesignUtils.getReference(es),
            experimentReference));
      }

      sb.append(NEWLINE);
    }

    return sb.toString();
  }

  /**
   * Generate comparison file content.
   * @return a String with the comparison file content
   * @throws EoulsanException if the format of one of the comparison entries of
   *           the design file is invalid
   */
  protected String generateComparisonFileContent() throws EoulsanException {

    final StringBuilder sb = new StringBuilder();

    for (String c : experiment.getMetadata().getComparisons().split(";")) {

      final String[] splitC = c.split(":");

      if (splitC.length != 2) {
        throw new EoulsanException("Invalid comparison entry format: " + c);
      }

      sb.append(splitC[0].trim());
      sb.append(TAB_SEPARATOR);
      sb.append(splitC[1].trim());
      sb.append(NEWLINE);
    }

    return sb.toString();
  }

  /**
   * Read a file from the Jar.
   * @param filePathInJar, path to the file to copy from the Jar
   * @return a String with the content of the file
   * @throws IOException if reading fails
   */
  protected static String readFromJar(final String filePathInJar)
      throws IOException {

    final InputStream is =
        AbstractEasyContrasts.class.getResourceAsStream(filePathInJar);

    final StringBuilder sb = new StringBuilder();
    String line = null;

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(is, Charset.defaultCharset()))) {

      while ((line = reader.readLine()) != null) {

        sb.append(line);
        sb.append('\n');
      }
    }

    return sb.toString();
  }

  //
  // Execution methods
  //

  private void check() throws EoulsanException {

    // Check experiment design
    DESeq2DesignChecker.checkExperimentDesign(this.experiment);

    if (this.buildContrast) {
      if (!this.experiment.getMetadata().containsComparisons()) {
        throw new EoulsanException(
            "No comparison defined to build the constrasts in experiment: "
                + experimentName());
      }
    }
  }

  /**
   * Method to run DESeq2.
   * @param workflowOutputDir workflow output directory
   * @throws IOException if writeDeseq2Design fails
   * @throws EoulsanException if the comparisons value is not correct
   */

  public void runDEseq2(final DataFile workflowOutputDir)
      throws IOException, EoulsanException {

    final String prefix = stepId() + "_" + experimentName();

    // Check
    check();

    // Open executor connection
    this.executor.openConnection();

    // Put Sample files
    putSampleFiles();

    // Write the deseq2 design
    writeDESeq2Design(prefix);

    // Copy contrast file
    writeContrastFile(prefix);

    // Execute the R scripts
    execute(prefix, workflowOutputDir);

    // Remove input files
    this.executor.removeInputFiles();

    // Retrieve output files
    this.executor.getOutputFiles();

    // Close executor connection
    this.executor.closeConnection();
  }

  protected abstract void execute(String prefix, DataFile workflowOutputDir)
      throws IOException, EoulsanException;

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param executor RServe executor
   * @param stepId the step id
   * @param design the Eoulsan design
   * @param experiment the experiment
   * @param sampleFiles the list of expression files
   * @param normFig normFig DESeq2 option
   * @param diffanaFig diffanaFig DESeq2 option
   * @param normDiffana normDiffana DESeq2 option
   * @param diffana diffana DESeq2 option
   * @param sizeFactorsType sizeFactorsType DESeq2 option
   * @param fitType fitType DESeq2 option
   * @param statisticTest statisticTest DESeq2 option
   * @param saveRScripts save R scripts
   */
  protected AbstractEasyContrasts(final RExecutor executor, final String stepId,
      final Design design, final Experiment experiment,
      final Map<String, File> sampleFiles, final DESeq2Parameters parameters,
      boolean saveRScripts) {

    requireNonNull(stepId, "stepId argument cannot be null");
    requireNonNull(executor, "executor argument cannot be null");
    requireNonNull(design, "design argument cannot be null");
    requireNonNull(experiment, "experiment argument cannot be null");
    requireNonNull(sampleFiles, "sampleFiles argument cannot be null");

    requireNonNull(parameters, "parameters argument cannot be null");

    this.stepId = stepId;

    this.executor = executor;
    this.saveRScripts = saveRScripts;
    this.design = design;
    this.experiment = experiment;
    this.sampleFiles = sampleFiles;
    this.parameters = parameters;

    ExperimentMetadata expMD = experiment.getMetadata();

    // Get model option
    if (expMD.containsModel()) {
      this.model = expMD.getModel();
    } else {
      this.model = "~Condition";
    }

    // Get contrast option
    if (expMD.containsContrast()) {
      this.contrast = expMD.isContrast();
    } else if (expMD.containsContrastFile()) {
      this.contrast = true;
    } else {
      this.contrast = false;
    }

    // Get designFile option
    if (expMD.containsContrastFile()) {
      this.contrastFile = new DataFile(expMD.getContrastFile());
      this.buildContrast = false;
    } else {
      this.contrastFile = null;

      // Get buildContrast option
      if (expMD.containsBuildContrast()) {
        this.buildContrast = expMD.isBuildContrast();
      } else {
        this.buildContrast = false;
      }
    }
  }
}
