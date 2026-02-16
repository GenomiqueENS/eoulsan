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

import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.DEFAULT_SINGLE_INPUT_PORT_NAME;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TSV;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.Common;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.checkers.Checker;
import fr.ens.biologie.genomique.eoulsan.checkers.DESeq2DesignChecker;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.DesignUtils;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.eoulsan.requirements.Requirement;
import fr.ens.biologie.genomique.eoulsan.util.r.DockerRExecutor;
import fr.ens.biologie.genomique.eoulsan.util.r.RExecutor;
import fr.ens.biologie.genomique.kenetre.bio.io.TSVCountsReader;
import fr.ens.biologie.genomique.kenetre.util.Version;

/**
 * Class to run the differential analysis with DEseq2
 * @author Xavier Bauquet
 * @since 2.0
 */

@LocalOnly
public class DESeq2Module extends AbstractModule {

  // Module name
  public static final String MODULE_NAME = "deseq2";

  private static final String[] EASY_CONTRASTS_DEFAULT_DOCKER_IMAGES =
      new String[] {"genomicpariscentre/bioconductor:release_sequencing_3.1",
          "genomicpariscentre/easycontrasts:2.0"};

  // DEseq2 options names
  private static final String NORMALIZATION_FIGURES = "norm.fig";
  private static final String DIFFANA_FIGURES = "diffana.fig";
  private static final String NORM_DIFFANA = "norm.diffana";
  private static final String DIFFANA = "diffana";
  private static final String SIZE_FACTORS_TYPE = "size.factors.type";
  private static final String FIT_TYPE = "fit.type";
  private static final String STATISTIC_TEST = "statistical.test";
  private static final String WEIGHT_CONTRAST = "weight.contrast";
  private static final String LOGO_URL = "logo.url";
  private static final String AUTHOR_NAME = "author.name";
  private static final String AUTHOR_MAIL = "author.email";
  private static final String SAVE_R_SCRIPTS = "save.r.scripts";
  private static final String VERSION = "easy.contrasts.version";

  // Default value for DEseq options
  DESeq2Parameters deseq2Parameters = new DESeq2Parameters();

  private final Set<Requirement> requirements = new HashSet<>();
  private RExecutor executor;
  private boolean saveRScripts;

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
  public InputPorts getInputPorts() {

    return new InputPortsBuilder()
        .addPort(DEFAULT_SINGLE_INPUT_PORT_NAME, true, EXPRESSION_RESULTS_TSV)
        .create();
  }

  @Override
  public Set<Requirement> getRequirements() {

    return unmodifiableSet(this.requirements);
  }

  @Override
  public Checker getChecker() {

    return new DESeq2DesignChecker();
  }

  private String defaultDockerImage(Set<Parameter> parameters)
      throws EoulsanException {

    // Get Easy contrast version
    for (Parameter p : new HashSet<>(parameters)) {

      switch (p.getName()) {
      case VERSION:
        this.deseq2Parameters
            .setEasyContrastsVersion(p.getIntValueInRange(1, 2));
        parameters.remove(p);
        break;

      default:
        break;
      }
    }

    return EASY_CONTRASTS_DEFAULT_DOCKER_IMAGES[this.deseq2Parameters
        .getEasyContrastsVersion() - 1];
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    final Set<Parameter> parameters = new HashSet<>(stepParameters);
    final int version = this.deseq2Parameters.getEasyContrastsVersion();

    context.getLogger().info("Easy contrast version: "
        + this.deseq2Parameters.getEasyContrastsVersion());

    context.getLogger().info("Docker image: " + defaultDockerImage(parameters));

    context.getLogger().info("Easy contrast version: " + version);

    // Parse R executor parameters
    this.executor = RModuleCommonConfiguration.parseRExecutorParameter(context,
        parameters, this.requirements, defaultDockerImage(parameters));

    context.getLogger().info("Docker image: " + defaultDockerImage(parameters));

    for (Parameter p : parameters) {

      switch (p.getName()) {

      case NORMALIZATION_FIGURES:
        notRelevantParameterV2(context, p);
        this.deseq2Parameters.setNormFig(parseBoolean(p));
        break;

      case DIFFANA_FIGURES:
        notRelevantParameterV2(context, p);
        this.deseq2Parameters.setDiffanaFig(parseBoolean(p));
        break;

      case NORM_DIFFANA:
        notRelevantParameterV2(context, p);
        this.deseq2Parameters.setNormDiffana(parseBoolean(p));
        break;

      case DIFFANA:
        this.deseq2Parameters.setDiffana(parseBoolean(p));
        break;

      case SIZE_FACTORS_TYPE:
        this.deseq2Parameters.setSizeFactorsType(p);
        break;

      case FIT_TYPE:
        this.deseq2Parameters.setFitType(p);
        break;

      case STATISTIC_TEST:
        this.deseq2Parameters.setStatisticTest(p);
        break;

      // Modules
      case WEIGHT_CONTRAST:
        notRelevantParameterV1(context, p);
        this.deseq2Parameters.setWeightContrast(p.getBooleanValue());
        break;

      case LOGO_URL:
        notRelevantParameterV1(context, p);
        this.deseq2Parameters.setLogoUrl(p.getStringValue());
        break;

      case AUTHOR_NAME:
        notRelevantParameterV1(context, p);
        this.deseq2Parameters.setAuthorName(p.getStringValue());
        break;

      case AUTHOR_MAIL:
        notRelevantParameterV1(context, p);
        this.deseq2Parameters.setAuthorEmail(p.getStringValue());
        break;

      case SAVE_R_SCRIPTS:
        this.saveRScripts = p.getBooleanValue();
        break;

      default:
        throw new EoulsanException(
            "Unkown parameter for step " + getName() + " : " + p.getName());
      }
    }

    // Check parameters
    this.deseq2Parameters.freeze();
    this.deseq2Parameters.check();
  }

  /**
   * Parse a boolean parameter.
   * @param p the parameter
   * @return a boolean
   * @throws EoulsanException if the parameter is not a boolean
   */
  private static boolean parseBoolean(final Parameter p)
      throws EoulsanException {

    if (p == null) {
      throw new NullPointerException("p parameter cannot be null");
    }

    String value = p.getLowerStringValue().trim();

    if (!("true".equals(value) || "false".equals(value))) {
      throw new EoulsanException("Invalid boolean value for parameter "
          + p.getName() + ": " + p.getValue());
    }

    return p.getBooleanValue();
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    if (DockerRExecutor.REXECUTOR_NAME.equals(this.executor.getName())) {
      status.setDockerImage(((DockerRExecutor) executor).getDockerImage());
    }

    // Get the design
    final Design design = context.getWorkflow().getDesign();

    // Get the experiment data
    final Data expressionData = context.getInputData(EXPRESSION_RESULTS_TSV);

    // Get the name of expression file by sample
    final Map<String, File> sampleFiles = new HashMap<>();
    for (Data d : expressionData.getListElements()) {
      sampleFiles.put(d.getName(), d.getDataFile().toFile());
    }

    String stepId = context.getCurrentStep().getId();
    boolean saveScripts =
        this.saveRScripts || context.getSettings().isSaveRscripts();

    try {

      // Check if all the counts of the expression files are not null
      checkIfAllCountAreNullInConditions(design, sampleFiles);

      for (Experiment e : design.getExperiments()) {

        // Do nothing if the experiment is skipped
        if (DesignUtils.isSkipped(e)) {
          continue;
        }

        // Select the version of Easy contrasts
        AbstractEasyContrasts ec;
        switch (this.deseq2Parameters.getEasyContrastsVersion()) {

        case 1:
          ec = new EasyContrasts1(this.executor, stepId, design, e, sampleFiles,
              this.deseq2Parameters, saveScripts);
          break;

        case 2:
          ec = new EasyContrasts2(this.executor, stepId, design, e, sampleFiles,
              this.deseq2Parameters, saveScripts);
          break;

        default:
          throw new IllegalStateException(
              "Invalide easy-contrasts-DESeq2 version: "
                  + this.deseq2Parameters.getEasyContrastsVersion());

        }

        // Run DEseq2
        ec.runDEseq2(context.getOutputDirectory());

      }
    } catch (IOException | EoulsanException e) {
      return status.createTaskResult(e,
          "Error while analysis data: " + e.getMessage());
    }

    // Write log file
    return status.createTaskResult();
  }

  /**
   * Check if all the count in an expression file are null
   * @param file the file to test
   * @return true if all the count in an expression file are null
   * @throws FileNotFoundException if the file cannot be found
   * @throws IOException if an error occurs while reading the expression file
   */
  private static boolean checkAllCountAreNullInExpressionFile(final File file)
      throws FileNotFoundException, IOException {

    try (TSVCountsReader reader = new TSVCountsReader(file)) {

      // Get the counts
      Map<String, Integer> counts = reader.read();

      for (int value : counts.values()) {

        // End of the check for the first non null value
        if (value > 0) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Check if the counts of a condition are null.
   * @param design the design
   * @param sampleFiles the expression files
   * @throws IOException if an expression file cannot be read
   * @throws FileNotFoundException if an expression file cannot be found
   * @throws EoulsanException if all the counts of a condition are null
   */
  private static void checkIfAllCountAreNullInConditions(final Design design,
      final Map<String, File> sampleFiles)
      throws FileNotFoundException, IOException, EoulsanException {

    Map<String, Boolean> emptyFiles = new HashMap<>();
    Map<String, String> technicalReplicates = new HashMap<>();
    Map<String, Boolean> result = new HashMap<>();

    // Get the technical replicates
    for (Sample s : design.getSamples()) {

      String sampleId = s.getId();

      if (s.getMetadata().containsRepTechGroup()) {
        String replicateId = s.getMetadata().getRepTechGroup();
        technicalReplicates.put(sampleId, replicateId);
      } else {
        technicalReplicates.put(sampleId, sampleId);
      }
    }

    // Check all the files
    for (Map.Entry<String, File> e : sampleFiles.entrySet()) {
      emptyFiles.put(e.getKey(),
          checkAllCountAreNullInExpressionFile(e.getValue()));
    }

    // Check if all the expression file of a technical replicate group are null
    for (Map.Entry<String, String> e : technicalReplicates.entrySet()) {

      String sampleId = e.getKey();
      String replicateId = e.getValue();

      if (!result.containsKey(replicateId)) {
        result.put(replicateId, true);
      }

      if (!result.get(replicateId)) {
        continue;
      }

      if (emptyFiles.containsKey(sampleId) && !emptyFiles.get(sampleId)) {
        result.put(replicateId, false);
      }
    }

    // Throw an exception, if the count of technical replicate group are null
    for (Map.Entry<String, Boolean> e : result.entrySet()) {

      if (e.getValue()) {

        throw new EoulsanException(
            "All the counts in the expression files are null for the the \""
                + e.getKey() + "\" condition");
      }
    }

  }

  private static void notRelevantParameterV1(StepConfigurationContext context,
      Parameter parameter) throws EoulsanException {
    notRelevantParameter(context, parameter, 1);
  }

  private static void notRelevantParameterV2(StepConfigurationContext context,
      Parameter parameter) throws EoulsanException {
    notRelevantParameter(context, parameter, 2);
  }

  /**
   * Show a message for not relevant parameters.
   * @param stepId the step identifier
   * @param parameter the deprecated parameter
   * @throws EoulsanException throw an exception if required
   */
  private static void notRelevantParameter(StepConfigurationContext context,
      Parameter parameter, int version) throws EoulsanException {

    requireNonNull(context, "context argument cannot be null");
    requireNonNull(parameter, "parameter argument cannot be null");

    String stepId = context.getCurrentStep().getId();
    String message = "The parameter \""
        + parameter.getName() + "\" in the \"" + stepId
        + "\" step is not handled by the version " + version
        + " of Easy contrats";

    Common.printWarning(message);
  }

}
