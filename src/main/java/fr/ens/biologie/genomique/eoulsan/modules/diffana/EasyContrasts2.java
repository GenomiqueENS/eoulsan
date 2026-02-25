package fr.ens.biologie.genomique.eoulsan.modules.diffana;

import static fr.ens.biologie.genomique.kenetre.util.StringUtils.toCompactTime;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.util.r.RExecutor;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class allow to execute easy contrasts 2.
 *
 * @author Laurent Jourdren
 * @since 2.7
 */
public class EasyContrasts2 extends AbstractEasyContrasts {

  private static final String SCRIPTS_PATH_IN_JAR_FILE = "/easy-contrasts-DESeq2-v2/";
  private static final String[] R_SCRIPTS =
      new String[] {
        "01_normDiffana.Rmd",
        "02_child_collapse_replicates.Rmd",
        "03_child_differential_expression.Rmd",
        "04_child_pairwise_comparison.Rmd"
      };

  /**
   * Install R scripts to execute.
   *
   * @throws IOException if an error occurs while installing scripts
   */
  private void installScripts() throws IOException {

    for (String scriptName : R_SCRIPTS) {

      final String scriptContent = readFromJar(SCRIPTS_PATH_IN_JAR_FILE + scriptName);

      this.executor.writeFile(scriptContent, scriptName);
    }
  }

  /**
   * Create a map with the script parameters.
   *
   * @param prefix the prefix of the experiment
   * @return a map with the script parameters
   */
  private Map<String, String> scriptParameters(String prefix) {

    DESeq2Parameters parameters = getParameters();

    Map<String, String> result = new LinkedHashMap<>();

    addParameter(result, "projectName", experimentName());
    addParameter(result, "designPath", deseq2DesignFileName(prefix));
    addParameter(result, "diffanaTest", parameters.isDiffana());
    addParameter(result, "expHeader", parameters.isExpHeader());
    addParameter(result, "deseqModel", model());
    addParameter(result, "sizeFactorType", parameters.getSizeFactorsType().toDESeq2Value());
    addParameter(result, "fitType", parameters.getFitType().toDESeq2Value());
    addParameter(result, "statisticTest", parameters.getStatisticTest().toDESeq2Value());
    addParameter(result, "prefix", prefix);
    addParameter(result, "plotInteractive", false);

    if (isComplexMode()) {
      addParameter(result, "comparisonPath", comparisonFileName(prefix));
      addParameter(result, "weightContrast", parameters.isWeightContrast());
    }

    if (parameters.getLogoUrl() != null) {
      addParameter(result, "logoUrl", parameters.getLogoUrl());
    }

    if (parameters.getAuthorName() != null) {
      addParameter(result, "authorName", parameters.getAuthorName());
    }

    if (parameters.getAuthorEmail() != null) {
      addParameter(result, "authorEmail", parameters.getAuthorEmail());
    }

    //
    addParameter(result, "leaveOnError", true);

    return result;
  }

  /**
   * Add a parameter to the script parameter map.
   *
   * @param map the script parameter map
   * @param key name of the parameter to add
   * @param value value of the parameter to add
   */
  private void addParameter(Map<String, String> map, String key, String value) {

    map.put(key, '"' + value + '"');
  }

  /**
   * Add a parameter to the script parameter map.
   *
   * @param map the script parameter map
   * @param key name of the parameter to add
   * @param value value of the parameter to add
   */
  private void addParameter(Map<String, String> map, String key, boolean value) {

    map.put(key, value ? "TRUE" : "FALSE");
  }

  /**
   * Create the command line for executing the R script.
   *
   * @param prefix the prefix of the experiment
   * @return a String with command line for executing the R script
   */
  private String commandLine(String prefix) {

    StringBuilder sb = new StringBuilder();

    sb.append("rmarkdown::render(");
    sb.append("input = '01_normDiffana.Rmd', ");
    sb.append("output_file = '");
    sb.append(experimentName());
    sb.append(".html', ");
    sb.append("params = list(");

    boolean first = true;
    for (Map.Entry<String, String> e : scriptParameters(prefix).entrySet()) {

      if (!first) {
        sb.append(", ");
      }
      sb.append(e.getKey());
      sb.append(" = ");
      sb.append(e.getValue());

      first = false;
    }
    sb.append("))");

    return sb.toString();
  }

  @Override
  protected void execute(String prefix, DataFile workflowOutputDir)
      throws IOException, EoulsanException {

    installScripts();

    // Set the description of the analysis
    final String description =
        stepId()
            + "_"
            + experimentName()
            + "-normDiffana-"
            + toCompactTime(System.currentTimeMillis());

    if (isComplexMode()) {
      writeContrastFile(prefix);
      writeComparisonFile(prefix);
    }

    this.executor.executeRScript(commandLine(prefix), description, workflowOutputDir);
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
  EasyContrasts2(
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
