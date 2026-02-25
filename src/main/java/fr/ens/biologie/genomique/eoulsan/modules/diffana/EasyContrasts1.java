package fr.ens.biologie.genomique.eoulsan.modules.diffana;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.kenetre.util.StringUtils.toCompactTime;
import static java.util.Arrays.asList;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.util.r.RExecutor;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This class allow to execute easy contrasts 1.
 *
 * @author Laurent Jourdren
 * @since 2.7
 */
public class EasyContrasts1 extends AbstractEasyContrasts {

  // R scripts path in JAR file
  private static final String SCRIPTS_PATH_IN_JAR_FILE = "/easy-contrasts-DESeq2-v1/";
  private static final String NORM_DIFFANA_SCRIPT = "normDiffana.R";
  private static final String BUILD_CONTRAST_SCRIPT = "buildContrast.R";

  /**
   * Create the command line to run normDiffana.R.
   *
   * @return the command line to run normDiffana.R
   */
  private String[] createNormDiffanaCommandLine(
      final String deseq2DesignFileName, final String contrastFilename) {

    final DESeq2Parameters parameters = getParameters();

    final List<String> command =
        new ArrayList<>(
            asList(
                booleanParameter(parameters.isNormFig()),
                booleanParameter(parameters.isDiffana()),
                booleanParameter(parameters.isDiffanaFig())));

    // Define contrast file
    if (isContrast()) {
      // add the default name of the contrast file if not an other is define
      command.add(booleanParameter(isContrast()));
    } else {
      // add FALSE if the contrast parameter is at false
      command.add(booleanParameter(false));
    }

    command.addAll(
        asList(
            deseq2DesignFileName,
            model(),
            experimentName(),
            booleanParameter(parameters.isExpHeader()),
            parameters.getSizeFactorsType().toDESeq2Value(),
            parameters.getFitType().toDESeq2Value(),
            parameters.getStatisticTest().toDESeq2Value(),
            contrastFilename,
            stepId() + "_"));

    return command.toArray(new String[0]);
  }

  /**
   * Execute the build contrast script.
   *
   * @param prefix the prefix to use
   * @param workflowOutputDir output directory
   * @throws IOException if an error occurs while reading or writing input files for the script
   * @throws EoulsanException if an error occurs while executing the script
   */
  private void executeBuildContrasts(String prefix, final DataFile workflowOutputDir)
      throws IOException, EoulsanException {

    // Write the comparison file from the Eoulsan design (experiment metadata)
    writeComparisonFile(prefix);

    // Read build contrast R script
    final String buildContrastScript =
        readFromJar(SCRIPTS_PATH_IN_JAR_FILE + BUILD_CONTRAST_SCRIPT);

    // Set the description of the analysis
    final String description =
        stepId()
            + "_"
            + experimentName()
            + "-buildcontrasts-"
            + toCompactTime(System.currentTimeMillis());

    // Create R script arguments
    String[] buildContrastScriptArgs =
        new String[] {
          deseq2DesignFileName(prefix),
          model(),
          comparisonFileName(prefix),
          contrastFilename(prefix),
          stepId() + "_"
        };

    // Log R Command line
    getLogger().info("R script to execute:\n" + buildContrastScript);
    getLogger().info("R script arguments: " + Arrays.toString(buildContrastScriptArgs));

    // Run buildContrast.R
    this.executor.executeRScript(
        buildContrastScript,
        false,
        null,
        isSaveRScripts(),
        description,
        workflowOutputDir,
        buildContrastScriptArgs);
  }

  /**
   * Execute the norm diffana script.
   *
   * @param prefix the prefix to use
   * @param workflowOutputDir output directory
   * @throws IOException if an error occurs while reading or writing input files for the script
   */
  private void executeNormDiffana(String prefix, final DataFile workflowOutputDir)
      throws IOException {

    // Read build contrast R script
    final String normDiffanaScript = readFromJar(SCRIPTS_PATH_IN_JAR_FILE + NORM_DIFFANA_SCRIPT);

    // Set the description of the analysis
    final String description =
        stepId()
            + "_"
            + experimentName()
            + "-normdiffana-"
            + toCompactTime(System.currentTimeMillis());

    // TODO Do not handle custom contrast files with
    // ExperimentMetadata.containsContrastFile()

    // Create R script arguments
    String[] normDiffanaScriptArgs =
        createNormDiffanaCommandLine(deseq2DesignFileName(prefix), contrastFilename(prefix));

    // Log R Command line
    getLogger().info("R script to execute:\n" + normDiffanaScript);
    getLogger().info("R script arguments: " + Arrays.toString(normDiffanaScriptArgs));

    // Run normDiffana.R
    this.executor.executeRScript(
        normDiffanaScript,
        false,
        null,
        isSaveRScripts(),
        description,
        workflowOutputDir,
        normDiffanaScriptArgs);
  }

  //
  // Other methods
  //

  /**
   * Transform boolean for DEseq2 command line.
   *
   * @param value boolean
   * @return boolean for DEseq2 command line
   */
  private static String booleanParameter(boolean value) {

    return Boolean.valueOf(value).toString().toUpperCase(Globals.DEFAULT_LOCALE);
  }

  //
  // Execute method
  //

  @Override
  protected void execute(String prefix, DataFile workflowOutputDir)
      throws IOException, EoulsanException {

    // Build the contrast file
    if (isBuildContrast()) {
      executeBuildContrasts(prefix, workflowOutputDir);
    }

    // Run normalization and differential analysis
    if (getParameters().isNormDiffana()) {
      executeNormDiffana(prefix, workflowOutputDir);
    }
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   *
   * @param executor RServe executor
   * @param stepId the step id
   * @param design the Eoulsan design
   * @param experiment the experiment
   * @param sampleFiles the list of expression files
   * @param parameters DESeq2 parameters
   * @param saveRScripts save R scripts
   */
  EasyContrasts1(
      final RExecutor executor,
      final String stepId,
      final Design design,
      final Experiment experiment,
      final Map<String, File> sampleFiles,
      final DESeq2Parameters parameters,
      boolean saveRScripts) {

    super(executor, stepId, design, experiment, sampleFiles, parameters, saveRScripts);
  }
}
