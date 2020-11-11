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
import static fr.ens.biologie.genomique.eoulsan.util.StringUtils.toCompactTime;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.checkers.DESeq2DesignChecker;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
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
public class DESeq2 {

  // R scripts path in JAR file
  private static final String SCRIPTS_PATH_IN_JAR_FILE = "/DESeq2/";
  private static final String NORM_DIFFANA_SCRIPT = "normDiffana.R";
  private static final String BUILD_CONTRAST_SCRIPT = "buildContrast.R";

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
  private final boolean normFig;
  private final boolean diffanaFig;
  private final boolean normDiffana;
  private final boolean diffana;
  private final DESeq2.SizeFactorsType sizeFactorsType;
  private final DESeq2.FitType fitType;
  private final DESeq2.StatisticTest statisticTest;

  // Design options for DEseq2
  private final String model;
  private final boolean contrast;
  private final boolean buildContrast;
  private final DataFile contrastFile;

  private final boolean expHeader = true;

  // Files and file names
  private final RExecutor executor;
  private final boolean saveRScripts;

  private final String stepId;

  // Temporary expression filenames
  private final Map<String, String> sampleFilenames = new HashMap<>();

  //
  // Enums
  //

  /***
   * Enum for the sizeFactorsType option in DESeq2 related to the estimation of
   * the size factor.
   */
  public enum SizeFactorsType {

    RATIO, ITERATE;

    /**
     * Get the size factors type to be used in DESeq2.
     * @param parameter Eoulsan parameter
     * @return the size factors type value
     * @throws EoulsanException if the size factors type value is different from
     *           ratio or iterate
     */
    public static SizeFactorsType get(final Parameter parameter)
        throws EoulsanException {

      requireNonNull(parameter, "parameter argument cannot be null");

      final String lowerName = parameter.getLowerStringValue().trim();

      for (SizeFactorsType dem : SizeFactorsType.values()) {

        if (dem.name().toLowerCase().equals(lowerName)) {
          return dem;
        }
      }

      throw new EoulsanException("The value: "
          + parameter.getValue() + " is not an acceptable value for the "
          + parameter.getName() + " parameter.");
    }

    /**
     * Convert the enum name into DESeq2 value.
     * @return DESeq2 value
     */
    public String toDESeq2Value() {

      return this.name().toLowerCase();
    }
  }

  /**
   * Enum for the fitType option in DESeq2 related to the dispersion estimation.
   */
  public enum FitType {

    PARAMETRIC, LOCAL, MEAN;

    /**
     * Get the fit type to be used in DESeq2.
     * @param name name of the enum
     * @return the fit type value
     * @throws EoulsanException if the fit type value is different from
     *           parametric, local or mean
     */
    public static FitType get(final String name) throws EoulsanException {

      requireNonNull(name, "fitType argument cannot be null");

      final String lowerName = name.trim().toLowerCase();

      for (FitType dem : FitType.values()) {

        if (dem.name().toLowerCase().equals(lowerName)) {
          return dem;
        }
      }

      throw new EoulsanException("The value: "
          + name + " is not an acceptable value for the fitType option.");
    }

    /**
     * Convert the enum name into DESeq2 value.
     * @return DESeq2 value
     */
    public String toDESeq2Value() {

      return this.name().toLowerCase();
    }
  }

  /**
   * Enum for the statisticTest option in DESeq2 related to the statistic test
   * to be used during the differential expression analysis
   */
  public enum StatisticTest {

    WALD("Wald"), LRT("LRT");

    private final String name;

    public String toDESeq2Value() {

      return name;
    }

    /**
     * Get the statistic test to be used in DESeq2.
     * @param name name of the enum
     * @return the statistic test value
     * @throws EoulsanException if the statistic test value is different from
     *           Wald or LRT
     */
    public static StatisticTest get(final String name) throws EoulsanException {

      requireNonNull(name, "statisticTest cargument annot be null");

      final String lowerName = name.trim().toLowerCase();

      for (StatisticTest dem : StatisticTest.values()) {

        if (dem.toDESeq2Value().toLowerCase().equals(lowerName)) {
          return dem;
        }
      }

      throw new EoulsanException("The value: "
          + name + " is not an acceptable value for the statisticTest option.");
    }

    /**
     * Constructor.
     * @param method, dispersion estimation method
     */
    StatisticTest(final String method) {

      this.name = method;
    }

  }

  /**
   * Put sample files.
   * @throws IOException if an error occurs while putting sample files
   */
  private void putSampleFiles() throws IOException {

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
  private String generateComparisonFileContent() throws EoulsanException {

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
   * Create the command line to run normDiffana.R.
   * @return the command line to run normDiffana.R
   */
  private String[] createNormDiffanaCommandLine(
      final String deseq2DesignFileName, final String contrastFilename) {

    final List<String> command =
        new ArrayList<>(asList(booleanParameter(normFig),
            booleanParameter(diffana), booleanParameter(diffanaFig)));

    // Define contrast file
    if (contrast) {
      // add the default name of the contrast file if not an other is define
      command.add(booleanParameter(contrast));
    } else {
      // add FALSE if the contrast parameter is at false
      command.add(booleanParameter(false));
    }

    command.addAll(asList(deseq2DesignFileName, this.model,
        this.experiment.getName(), booleanParameter(this.expHeader),
        this.sizeFactorsType.toDESeq2Value(), this.fitType.toDESeq2Value(),
        this.statisticTest.toDESeq2Value(), contrastFilename,
        this.stepId + "_"));

    return command.toArray(new String[0]);
  }

  /**
   * Transform boolean for DEseq2 command line.
   * @param value boolean
   * @return boolean for DEseq2 command line
   */
  private static String booleanParameter(boolean value) {

    return Boolean.valueOf(value).toString().toUpperCase();
  }

  /**
   * Method to run DESeq2.
   * @param workflowOutputDir workflow output directory
   * @throws IOException if writeDeseq2Design fails
   * @throws EoulsanException if the comparisons value is not correct
   */
  public void runDEseq2(final DataFile workflowOutputDir)
      throws IOException, EoulsanException {

    final String prefix = this.stepId + "_" + this.experiment.getName();

    // Define design filename
    final String deseq2DesignFileName = prefix + DESEQ_DESIGN_FILE_SUFFIX;

    // Define comparison filename
    final String comparisonFileName = prefix + COMPARISON_FILE_SUFFIX;

    // Define contrast filename
    final String contrastFilename = prefix + CONTRAST_FILE_SUFFIX;

    // Check experiment design
    DESeq2DesignChecker.checkExperimentDesign(this.experiment);

    // Open executor connection
    this.executor.openConnection();

    // Put Sample files
    putSampleFiles();

    // Write the deseq2 design
    this.executor.writerFile(generateDeseq2Design(), deseq2DesignFileName);

    // Copy contrast file
    if (this.contrastFile != null) {
      this.executor.putInputFile(this.contrastFile, contrastFilename);
    }

    // Build the contrast file
    if (this.buildContrast) {

      if (!this.experiment.getMetadata().containsComparisons()) {
        throw new EoulsanException(
            "No comparison defined to build the constrasts in experiment: "
                + this.experiment.getName());
      }

      // Write the comparison file from the Eoulsan design (experiment metadata)
      this.executor.writerFile(generateComparisonFileContent(),
          comparisonFileName);

      // Read build contrast R script
      final String buildContrastScript =
          readFromJar(SCRIPTS_PATH_IN_JAR_FILE + BUILD_CONTRAST_SCRIPT);

      // Set the description of the analysis
      final String description = this.stepId
          + "_" + this.experiment.getName() + "-buildcontrasts-"
          + toCompactTime(System.currentTimeMillis());

      // Run buildContrast.R
      this.executor.executeRScript(buildContrastScript, false, null,
          this.saveRScripts, description, workflowOutputDir,
          deseq2DesignFileName, this.model, comparisonFileName,
          this.experiment.getName() + CONTRAST_FILE_SUFFIX, this.stepId + "_");
    }

    // Run normalization and differential analysis
    if (this.normDiffana) {

      // Read build contrast R script
      final String normDiffanaScript =
          readFromJar(SCRIPTS_PATH_IN_JAR_FILE + NORM_DIFFANA_SCRIPT);

      // Set the description of the analysis
      final String description = this.stepId
          + "_" + this.experiment.getName() + "-normdiffana-"
          + toCompactTime(System.currentTimeMillis());

      // TODO Do not handle custom contrast files with
      // ExperimentMetadata.containsContrastFile()

      // Run normDiffana.R
      this.executor.executeRScript(normDiffanaScript, false, null,
          this.saveRScripts, description, workflowOutputDir,
          createNormDiffanaCommandLine(deseq2DesignFileName, contrastFilename));
    }

    // Remove input files
    this.executor.removeInputFiles();

    // Retrieve output files
    this.executor.getOutputFiles();

    // Close executor connection
    this.executor.closeConnection();
  }

  //
  // Static methods
  //

  /**
   * Read a file from the Jar.
   * @param filePathInJar, path to the file to copy from the Jar
   * @return a String with the content of the file
   * @throws IOException if reading fails
   */
  private static String readFromJar(final String filePathInJar)
      throws IOException {

    final InputStream is = DESeq2.class.getResourceAsStream(filePathInJar);

    final StringBuilder sb = new StringBuilder();
    String line = null;

    try (
        BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

      while ((line = reader.readLine()) != null) {

        sb.append(line);
        sb.append('\n');
      }
    }

    return sb.toString();
  }

  //
  // Construtors
  //

  /**
   * Public constructor.
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
   */
  public DESeq2(final RExecutor executor, final String stepId,
      final Design design, final Experiment experiment,
      final Map<String, File> sampleFiles, final boolean normFig,
      final boolean diffanaFig, final boolean normDiffana,
      final boolean diffana, final SizeFactorsType sizeFactorsType,
      final FitType fitType, final StatisticTest statisticTest,
      boolean saveRScripts) {

    requireNonNull(stepId, "stepId argument cannot be null");
    requireNonNull(executor, "executor argument cannot be null");
    requireNonNull(design, "design argument cannot be null");
    requireNonNull(experiment, "experiment argument cannot be null");
    requireNonNull(sampleFiles, "sampleFiles argument cannot be null");

    requireNonNull(sizeFactorsType, "sizeFactorsType argument cannot be null");
    requireNonNull(fitType, "fitType argument cannot be null");
    requireNonNull(statisticTest, "statisticTest argument cannot be null");

    this.stepId = stepId;

    this.executor = executor;
    this.saveRScripts = saveRScripts;
    this.design = design;
    this.experiment = experiment;
    this.sampleFiles = sampleFiles;

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

    // Workflow options for DEseq2
    this.normFig = normFig;
    this.diffanaFig = diffanaFig;
    this.normDiffana = normDiffana;
    this.diffana = diffana;
    this.sizeFactorsType = sizeFactorsType;
    this.fitType = fitType;
    this.statisticTest = statisticTest;
  }
}