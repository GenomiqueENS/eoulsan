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
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.steps.diffana.local;

import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.DEFAULT_SINGLE_INPUT_PORT_NAME;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TSV;
import static fr.ens.transcriptome.eoulsan.steps.diffana.DEseq2Executor.FitType.PARAMETRIC;
import static fr.ens.transcriptome.eoulsan.steps.diffana.DEseq2Executor.SizeFactorsType.RATIO;
import static fr.ens.transcriptome.eoulsan.steps.diffana.DEseq2Executor.StatisticTest.WALD;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Experiment;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.diffana.DEseq2Executor;
import fr.ens.transcriptome.eoulsan.steps.diffana.DEseq2Executor.FitType;
import fr.ens.transcriptome.eoulsan.steps.diffana.DEseq2Executor.SizeFactorsType;
import fr.ens.transcriptome.eoulsan.steps.diffana.DEseq2Executor.StatisticTest;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * Class to run the differential analysis with DEseq2
 * @author Xavier Bauquet
 * @since 2.0
 */

@LocalOnly
public class DESeq2Step extends AbstractStep {

  // Step name
  private static final String STEP_NAME = "deseq2";

  // DEseq2 options names
  private static final String NORMALIZATION_FIGURES = "norm.fig";
  private static final String DIFFANA_FIGURES = "diffana.fig";
  private static final String NORM_DIFFANA = "norm.diffana";
  private static final String DIFFANA = "diffana";
  private static final String SIZE_FACTORS_TYPE = "size.factors.type";
  private static final String FIT_TYPE = "fit.type";
  private static final String STATISTIC_TEST = "statistic.test";

  // Default value for DEseq options
  private boolean normFig = true;
  private boolean diffanaFig = true;
  private boolean normDiffana = true;
  private boolean diffana = true;
  private SizeFactorsType sizeFactorsType = RATIO;
  private FitType fitType = PARAMETRIC;
  private StatisticTest statisticTest = WALD;

  //
  // Step methods
  //

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    return new InputPortsBuilder().addPort(DEFAULT_SINGLE_INPUT_PORT_NAME,
        true, EXPRESSION_RESULTS_TSV).create();
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case NORMALIZATION_FIGURES:
        this.normFig = parseBoolean(p);
        break;

      case DIFFANA_FIGURES:
        this.normFig = parseBoolean(p);
        break;

      case NORM_DIFFANA:
        this.normFig = parseBoolean(p);
        break;

      case DIFFANA:
        this.normFig = parseBoolean(p);
        break;

      case SIZE_FACTORS_TYPE:
        this.sizeFactorsType = DEseq2Executor.SizeFactorsType.get(p);
        break;

      case FIT_TYPE:
        this.fitType = DEseq2Executor.FitType.get(p.getStringValue());
        break;

      case STATISTIC_TEST:
        this.statisticTest =
            DEseq2Executor.StatisticTest.get(p.getStringValue());
        break;

      default:
        throw new EoulsanException("Unkown parameter for step "
            + getName() + " : " + p.getName());
      }
    }
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
  public StepResult execute(final StepContext context, final StepStatus status) {

    // Get the design
    final Design design = context.getWorkflow().getDesign();

    // Get the output directory
    final File outputDir = context.getStepOutputDirectory().toFile();

    // Get the temporary directory
    final File tempDir = context.getLocalTempDirectory();

    // Get the experiment data
    final Data expressionData = context.getInputData(EXPRESSION_RESULTS_TSV);

    // Get the name of expression file by sample
    final Map<String, File> sampleFiles = new HashMap<>();
    for (Data d : expressionData.getListElements()) {
      sampleFiles.put(d.getName(), d.getDataFile().toFile());
    }

    try {

      // Extract R scripts
      DEseq2Executor.extractRScripts(tempDir);

      for (Experiment e : design.getExperiments()) {

        // run DEseq2
        new DEseq2Executor(design, e, sampleFiles, outputDir, tempDir, normFig,
            diffanaFig, normDiffana, diffana, sizeFactorsType, fitType,
            statisticTest).runDEseq2();

      }
    } catch (IOException | EoulsanException e) {
      return status.createStepResult(e,
          "Error while analysis data: " + e.getMessage());
    }

    // Write log file
    return status.createStepResult();
  }

}
