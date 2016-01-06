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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.steps.diffana;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.design.DesignUtils.getAllSamplesMetadataKeys;
import static fr.ens.biologie.genomique.eoulsan.design.DesignUtils.getExperimentSampleAllMetadataKeys;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.design.ExperimentMetadata;
import fr.ens.biologie.genomique.eoulsan.design.ExperimentSampleMetadata;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.eoulsan.design.SampleMetadata;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;

/**
 * This class contains methods to run the differential analysis module DESeq2.
 * @author Xavier Bauquet
 * @since 2.0
 */
public class DEseq2Executor {

  private static final String DEFAULT_R_LANG = "C";
  private static final String LANG_ENVIRONMENT_VARIABLE = "LANG";

  private static final String CONDITION_COLUMN = "Condition";
  private static final String REFERENCE_COLUMN_NAME = "Reference";
  private static final String REFERENCE_EXP_MD_KEY = "reference";

  // R scripts path in JAR file
  private static final String SCRIPTS_PATH_IN_JAR_FILE = "/DESeq2/";
  private static final String NORM_DIFFANA_SCRIPT = "normDiffana.R";
  private static final String BUILD_CONTRAST_SCRIPT = "buildContrast.R";

  // Suffix for output files from DEseq2
  private static final String DESEQ_DESIGN_FILE_SUFFIX = "-deseq2Design.txt";
  private static final String COMPARISON_FILE_SUFFIX = "-comparisonFile.txt";
  private static final String CONTRAST_FILE_SUFFIX = "-contrastFile.txt";
  private static final String LOG_SUFFIX = "-deseq2.log";

  // Constants
  private static final String SAMPLE_ID_FIELDNAME = "SampleId";
  private static final String SAMPLE_NAME_FIELDNAME = "Name";
  private static final String EXPRESSION_FILE_FIELDNAME = "expressionFile";
  private static final String TAB_SEPARATOR = "\t";
  private static final String NEWLINE = "\n";

  // The experiment
  private final Experiment experiment;
  private final String expName;

  // List of expression files
  final Map<String, File> sampleFiles;

  // The design
  private final Design design;

  // Workflow options for DEseq2
  private final boolean normFig;
  private final boolean diffanaFig;
  private final boolean normDiffana;
  private final boolean diffana;
  private final DEseq2Executor.SizeFactorsType sizeFactorsType;
  private final DEseq2Executor.FitType fitType;
  private final DEseq2Executor.StatisticTest statisticTest;

  // Design options for DEseq2
  private final String model;
  private final boolean contrast;
  private final boolean buildContrast;

  private final boolean expHeader = true;

  // Files and file names
  private final String deseq2DesignName;
  private final String comparisonName;
  private final String contrastName;
  private final File deseq2DesignFile;
  private final File comparisonFile;
  private final File contrastFile;
  private final File outputDir;
  private final File tempDir;

  private final String stepId;

  //
  // Enums
  //

  /***
   * Enum for the sizeFactorsType option in DESeq2 related to the estimation of
   * the size factor.
   */
  public static enum SizeFactorsType {

    RATIO, ITERATE;

    /**
     * Get the size factors type to be used in DESeq2.
     * @param name, name of the enum
     * @return the size factors type value
     * @throws EoulsanException if the size factors type value is different from
     *           ratio or iterate
     */
    public static SizeFactorsType get(final Parameter parameter)
        throws EoulsanException {

      checkNotNull(parameter, "parameter argument cannot be null");

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
  public static enum FitType {

    PARAMETRIC, LOCAL, MEAN;

    /**
     * Get the fit type to be used in DESeq2.
     * @param name, name of the enum
     * @return the fit type value
     * @throws EoulsanException if the fit type value is different from
     *           parametric, local or mean
     */
    public static FitType get(final String name) throws EoulsanException {

      checkNotNull(name, "fitType argument cannot be null");

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
  public static enum StatisticTest {

    WALD("Wald"), LRT("LRT");

    private String name;

    public String toDESeq2Value() {

      return name;
    }

    /**
     * Get the statistic test to be used in DESeq2.
     * @param name, name of the enum
     * @return the statistic test value
     * @throws EoulsanException if the statistic test value is different from
     *           Wald or LRT
     */
    public static StatisticTest get(final String name) throws EoulsanException {

      checkNotNull(name, "statisticTest cargument annot be null");

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
   * Write the DEseq2 design specific to the experiment.
   * @param deseq2DesignFile name of the file to output
   * @param design the Eoulsan design
   * @throws IOException if an error occurs while writing the DESeq2 design
   */
  public void writeDeseq2Design() throws IOException {

    try (Writer bw = new FileWriter(deseq2DesignFile)) {

      final ExperimentMetadata expMetadata = experiment.getMetadata();

      //
      // Print column names
      //
      bw.append(SAMPLE_ID_FIELDNAME);
      bw.append(TAB_SEPARATOR);
      bw.append(SAMPLE_NAME_FIELDNAME);
      bw.append(TAB_SEPARATOR);
      bw.append(EXPRESSION_FILE_FIELDNAME);

      // Get the common column names
      final List<String> sampleMDKeys = getAllSamplesMetadataKeys(design);

      // Get the experiment column names
      final List<String> experimentMDKeys =
          getExperimentSampleAllMetadataKeys(experiment);

      // Print common column names
      for (String key : sampleMDKeys) {
        if (!experimentMDKeys.contains(key)) {

          // if the reference option on the experiment metadata exist, the
          // column Reference from the common column is not added
          if (expMetadata.contains(REFERENCE_EXP_MD_KEY)
              && key == REFERENCE_COLUMN_NAME) {

            continue;
          }

          bw.append(TAB_SEPARATOR);
          bw.append(key);
        }
      }

      // Print experiment column names
      for (String key : experimentMDKeys) {

        bw.append(TAB_SEPARATOR);
        bw.append(key);
      }

      // Add the column reference when the reference option is on the experiment
      // metadata
      if (expMetadata.contains(REFERENCE_EXP_MD_KEY)) {

        bw.append(TAB_SEPARATOR);
        bw.append(REFERENCE_COLUMN_NAME);
      }

      bw.append(NEWLINE);

      // Print sample metadata
      for (Sample sample : experiment.getSamples()) {

        // Check if the expression file related to the sample exist
        if (!this.sampleFiles.containsKey(sample.getId())) {
          continue;
        }

        bw.append(sample.getId());
        bw.append(TAB_SEPARATOR);
        bw.append(sample.getName());
        bw.append(TAB_SEPARATOR);
        bw.append(this.sampleFiles.get(sample.getId()).getAbsolutePath());

        final SampleMetadata smd = sample.getMetadata();

        for (String key : sampleMDKeys) {
          if (!experimentMDKeys.contains(key)) {

            // if the reference option on the experiment metadata exist, the
            // column Reference from the common column is not added
            if (expMetadata.contains(REFERENCE_EXP_MD_KEY)
                && key == REFERENCE_COLUMN_NAME) {

              continue;
            }

            bw.append(TAB_SEPARATOR);

            if (smd.contains(key)) {
              bw.append(smd.get(key));
            }
          }
        }

        // print experiment sample metadata
        final ExperimentSampleMetadata expSampleMetadata =
            experiment.getExperimentSample(sample).getMetadata();

        for (String key : experimentMDKeys) {

          bw.append(TAB_SEPARATOR);

          if (expSampleMetadata.contains(key)) {
            bw.append(expSampleMetadata.get(key));
          }
        }

        if (expMetadata.contains(REFERENCE_EXP_MD_KEY)) {
          bw.append(TAB_SEPARATOR);
          if (sample.getName() == expMetadata.getReference()) {
            bw.append("1");
          } else {
            bw.append("0");
          }

        }

        bw.append(NEWLINE);
      }

    }
  }

  /**
   * Write the comparison file for DEseq2.
   * @param design the Eoulsan design
   * @throws IOException if the FileWriter fails
   */
  private void writeComparison() throws IOException {

    try (Writer bw = new FileWriter(comparisonFile)) {

      for (String c : experiment.getMetadata().getComparison().split(";")) {
        String[] splitC = c.split(":");
        bw.append(splitC[0]);
        bw.append(TAB_SEPARATOR);
        bw.append(splitC[1]);
        bw.append(NEWLINE);
      }
    }

  }

  /**
   * Create the command line to run buildContrast.R.
   * @return the command line to run buildContrast.R
   */
  private List<String> createBuidContrastCommandLine() {

    if (this.buildContrast) {

      return asList(this.tempDir + File.separator + BUILD_CONTRAST_SCRIPT,
          deseq2DesignName, model, comparisonName, contrastName, stepId + "_");
    }

    return Collections.emptyList();
  }

  /**
   * Create the command line to run normDiffana.R.
   * @return the command line to run normDiffana.R
   */
  private List<String> createNormDiffanaCommandLine() {

    final List<String> command = new ArrayList<>();
    command.addAll(asList(this.tempDir + File.separator + NORM_DIFFANA_SCRIPT,
        booleanParameter(normFig), booleanParameter(diffana),
        booleanParameter(diffanaFig)));

    // Define contrast file
    if (contrast) {
      // add the default name of the contrast file if not an other is define
      command.add(booleanParameter(contrast));
    } else {
      // add FALSE if the contrast parameter is at false
      command.add(booleanParameter(false));
    }

    command.addAll(asList(deseq2DesignName, model, expName,
        booleanParameter(expHeader), sizeFactorsType.toDESeq2Value(),
        fitType.toDESeq2Value(), statisticTest.toDESeq2Value(),
        contrastFile.toString(), stepId + "_"));

    return command;
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
   * @throws IOException if writeDeseq2Design fails
   * @throws EoulsanException if the comparisons value is not correct
   */
  public void runDEseq2() throws IOException, EoulsanException {

    final ExperimentMetadata emd = experiment.getMetadata();

    if (emd.containsComparison()) {

      // Check if the comparison value is correct
      for (String c : emd.getComparison().split(";")) {
        String[] splitC = c.split(":");
        if (splitC.length != 2) {
          throw new EoulsanException("Error in "
              + experiment.getName()
              + " experiment, comparison cannot have more than 1 value.");
        }
      }
    }

    // Check if the column Condition is missing for the experiment
    if (!getExperimentSampleAllMetadataKeys(experiment).contains(
        CONDITION_COLUMN)
        && !getAllSamplesMetadataKeys(design).contains(CONDITION_COLUMN)) {
      throw new EoulsanException("Condition column missing for experiment: "
          + expName);
    }

    // Write the deseq2 design
    writeDeseq2Design();

    // Create the deseq2 log file, delete the deseq2 log file if already exist
    final File logFile =
        new File(this.outputDir, this.stepId + "_" + this.expName + LOG_SUFFIX);
    if (logFile.exists()) {
      if (!logFile.delete()) {
        getLogger().warning(
            "Cannot remove previous experiment log file: " + logFile);
      }
    }

    // Build the contrast file
    if (buildContrast) {

      if (!experiment.getMetadata().containsComparison()) {
        throw new EoulsanException(
            "No comparison defined to build the constrasts in experiment: "
                + expName);
      }

      // Write the comparison file from the Eoulsan design (experiment metadata)
      writeComparison();

      // Run buildContrast.R
      executeAndWaitRScript(createBuidContrastCommandLine(), this.outputDir,
          logFile, false, "build constrast",
          "Error while executing build constrast");
    }

    // Run normalization and differential analysis
    if (normDiffana) {

      // Run normDiffana.R
      executeAndWaitRScript(createNormDiffanaCommandLine(), this.outputDir,
          logFile, true, "DEseq2 normalization and differential analysis",
          "Error while executing normalization and differential analysis.");
    }

  }

  //
  // Static method
  //

  /**
   * Execute the command line and wait until the job is finish + error message.
   * @param processBuilder, the command line to execute
   * @param description, the description of the process
   * @param errorMessage, the error message
   * @throws EoulsanException if the wait fails
   */
  private void executeAndWaitRScript(final List<String> command,
      final File outputDir, final File logFile, final boolean appendLogFile,
      final String description, final String errorMessage)
      throws EoulsanException {

    final ProcessBuilder builder =
        new ProcessBuilder(command).directory(this.outputDir)
            .redirectErrorStream(true);

    // Configure the log file
    if (appendLogFile) {
      builder.redirectOutput(Redirect.appendTo(logFile));
    } else {
      builder.redirectOutput(Redirect.to(logFile));
    }

    // Set the LANG to C
    builder.environment().put(LANG_ENVIRONMENT_VARIABLE, DEFAULT_R_LANG);

    // message for eoulsan.log
    getLogger().info(
        "Step diffana: run " + description + ": " + builder.command());

    try {

      final Process process = builder.start();

      if (process.waitFor() != 0) {
        throw new EoulsanException(errorMessage);
      }

    } catch (InterruptedException | IOException e) {
      throw new EoulsanException(errorMessage, e);
    }

  }

  /**
   * Copy the R scripts to the tmp file.
   * @param tempDir, path to the tmp file
   * @throws IOException if the copy fails
   */
  public static void extractRScripts(final File tempDir) throws IOException {

    checkNotNull(tempDir, "tempDir argument cannot be null");
    checkArgument(tempDir.isDirectory(),
        "tempDir argument is not a directory: " + tempDir);

    // copy buildContrast.R
    copyFromJar(SCRIPTS_PATH_IN_JAR_FILE + BUILD_CONTRAST_SCRIPT, new File(
        tempDir, BUILD_CONTRAST_SCRIPT), true);
    // copy normDiffana.R
    copyFromJar(SCRIPTS_PATH_IN_JAR_FILE + NORM_DIFFANA_SCRIPT, new File(
        tempDir, NORM_DIFFANA_SCRIPT), true);

  }

  /**
   * Copy file from the Jar to a specific destination.
   * @param filePathInJar, path to the file to copy from the Jar
   * @param outputFile, path to the place to copy the file
   * @param setExecutableRight set executable rights to the copied file
   * @throws IOException if the copy fails
   */
  @SuppressWarnings("resource")
  private static void copyFromJar(final String filePathInJar,
      final File outputFile, final boolean setExecutableRight)
      throws IOException {

    // Do not copy the file if already exists
    if (outputFile.exists()) {
      return;
    }

    final InputStream is =
        DEseq2Executor.class.getResourceAsStream(filePathInJar);
    FileUtils.copy(is, new FileOutputStream(outputFile));

    if (setExecutableRight) {
      if (!outputFile.setExecutable(true)) {
        throw new IOException("Cannot set executable flag to : " + outputFile);
      }
    }
  }

  //
  // Construtors
  //

  /**
   * Public constructor.
   * @param stepId the step id
   * @param design, the Eoulsan design
   * @param experiment the experiment
   * @param sampleFiles, the list of expression files
   * @param outputDir, the output directory
   * @param tempDir, the tmp directory
   * @param normFig, normFig DESeq2 option
   * @param diffanaFig, diffanaFig DESeq2 option
   * @param normDiffana, normDiffana DESeq2 option
   * @param diffana, diffana DESeq2 option
   * @param sizeFactorsType, sizeFactorsType DESeq2 option
   * @param fitType, fitType DESeq2 option
   * @param statisticTest, statisticTest DESeq2 option
   */
  public DEseq2Executor(final String stepId, final Design design,
      final Experiment experiment, final Map<String, File> sampleFiles,
      final File outputDir, final File tempDir, final boolean normFig,
      final boolean diffanaFig, final boolean normDiffana,
      final boolean diffana,
      final DEseq2Executor.SizeFactorsType sizeFactorsType,
      final DEseq2Executor.FitType fitType,
      final DEseq2Executor.StatisticTest statisticTest) {

    checkNotNull(design, "design argument cannot be null");
    checkNotNull(experiment, "experiment argument cannot be null");
    checkNotNull(sampleFiles, "sampleFiles argument cannot be null");
    checkNotNull(outputDir, "outputDir argument cannot be null");
    checkNotNull(tempDir, "tempDir argument cannot be null");

    checkNotNull(sizeFactorsType, "sizeFactorsType argument cannot be null");
    checkNotNull(fitType, "fitType argument cannot be null");
    checkNotNull(statisticTest, "statisticTest argument cannot be null");

    this.design = design;
    this.experiment = experiment;
    this.expName = experiment.getName();
    this.sampleFiles = sampleFiles;

    this.outputDir = outputDir;
    this.tempDir = tempDir;

    this.stepId = stepId;

    this.deseq2DesignName =
        this.stepId + "_" + expName + DESEQ_DESIGN_FILE_SUFFIX;
    this.comparisonName = this.stepId + "_" + expName + COMPARISON_FILE_SUFFIX;
    this.contrastName = expName + CONTRAST_FILE_SUFFIX;
    this.deseq2DesignFile = new File(outputDir, deseq2DesignName);
    this.comparisonFile = new File(outputDir, comparisonName);

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

    // Get buildContrast option
    if (expMD.containsBuildContrast()) {
      this.buildContrast = expMD.isBuildContrast();
    } else {
      this.buildContrast = false;
    }

    // Get designFile option
    if (expMD.containsContrastFile()) {
      this.contrastFile =
          new File(outputDir, stepId + "_" + expMD.getContrastFile());
    } else {
      this.contrastFile = new File(outputDir, stepId + "_" + contrastName);
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
