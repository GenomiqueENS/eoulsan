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

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.DEFAULT_SINGLE_INPUT_PORT_NAME;
import static fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.DIFFANA_RESULTS_TSV;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TSV;

import java.io.File;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.diffana.DiffAna;
import fr.ens.transcriptome.eoulsan.steps.diffana.DiffAna.DispersionFitType;
import fr.ens.transcriptome.eoulsan.steps.diffana.DiffAna.DispersionMethod;
import fr.ens.transcriptome.eoulsan.steps.diffana.DiffAna.DispersionSharingMode;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define the step of differential analysis in local mode.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Vivien Deshaies
 */
@LocalOnly
public class DiffAnaLocalStep extends AbstractStep {

  private static final String DISP_EST_METHOD_PARAMETER_NAME =
      "disp.est.method";
  private static final String DISP_EST_FIT_TYPE_PARAMETER_NAME =
      "disp.est.fit.type";
  private static final String DISP_EST_SHARING_MODE_PARAMETER_NAME =
      "disp.est.sharing.mode";

  private static final String STEP_NAME = "diffana";

  // parameters and there default values
  private DispersionMethod dispEstMethod = DispersionMethod.POOLED;
  private DispersionFitType dispEstFitType = DispersionFitType.LOCAL;
  private DispersionSharingMode dispEstSharingMode =
      DispersionSharingMode.MAXIMUM;

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
  public String getDescription() {

    return "This class compute the differential analysis for the experiment.";
  }

  @Override
  public InputPorts getInputPorts() {
    return new InputPortsBuilder().addPort(DEFAULT_SINGLE_INPUT_PORT_NAME,
        true, EXPRESSION_RESULTS_TSV).create();
  }

  @Override
  public OutputPorts getOutputPorts() {
    return singleOutputPort(DIFFANA_RESULTS_TSV);
  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    try {
      final DataFormat eDF = DataFormats.EXPRESSION_RESULTS_TSV;

      String rServeName = null;
      final boolean rServeEnable =
          context.getSettings().isRServeServerEnabled();
      if (rServeEnable) {
        rServeName = context.getSettings().getRServeServerName();
      }

      final Design design = context.getWorkflow().getDesign();
      final DiffAna ad =
          new DiffAna(design, new File("."), eDF.getPrefix(),
              eDF.getDefaultExtension(), new File("."), this.dispEstMethod,
              this.dispEstSharingMode, this.dispEstFitType, rServeName,
              rServeEnable);

      // Launch analysis
      ad.run(context, context.getInputData(eDF));

      // Write log file
      return status.createStepResult();

    } catch (EoulsanException e) {

      return status.createStepResult(e,
          "Error while analysis data: " + e.getMessage());
    }
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      if (DISP_EST_METHOD_PARAMETER_NAME.equals(p.getName())) {
        this.dispEstMethod =
            DispersionMethod.getDispEstMethodFromName(p.getStringValue());
        if (this.dispEstMethod == null) {
          throw new EoulsanException("Unknown dispersion estimation method in "
              + getName() + " step: " + p.getStringValue());
        }
      } else if (DISP_EST_FIT_TYPE_PARAMETER_NAME.equals(p.getName())) {
        this.dispEstFitType =
            DispersionFitType.getDispEstFitTypeFromName(p.getStringValue());
        if (this.dispEstFitType == null) {
          throw new EoulsanException(
              "Unknown dispersion estimation fitType in "
                  + getName() + " step: " + p.getStringValue());
        }
      } else if (DISP_EST_SHARING_MODE_PARAMETER_NAME.equals(p.getName())) {
        this.dispEstSharingMode =
            DispersionSharingMode.getDispEstSharingModeFromName(p
                .getStringValue());

        if (this.dispEstSharingMode == null) {
          throw new EoulsanException(
              "Unknown dispersion estimation sharing mode in "
                  + getName() + " step: " + p.getStringValue());
        }
      } else {
        throw new EoulsanException("Unkown parameter for step "
            + getName() + " : " + p.getName());
      }

    }

    // Log Step parameters
    getLogger().info(
        "In "
            + getName() + ", dispersion estimation method="
            + this.dispEstMethod.getName());
    getLogger().info(
        "In "
            + getName() + ", dispersion estimation sharing mode="
            + this.dispEstSharingMode.getName());
    getLogger().info(
        "In "
            + getName() + ", dispersion estimation fit type="
            + this.dispEstFitType.getName());
  }
}
