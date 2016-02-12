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

package fr.ens.biologie.genomique.eoulsan.steps.diffana.local;

import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.DEFAULT_SINGLE_INPUT_PORT_NAME;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TSV;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.StepContext;
import fr.ens.biologie.genomique.eoulsan.core.StepResult;
import fr.ens.biologie.genomique.eoulsan.core.StepStatus;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.steps.AbstractStep;
import fr.ens.biologie.genomique.eoulsan.steps.Steps;
import fr.ens.biologie.genomique.eoulsan.steps.diffana.Normalization;
import fr.ens.biologie.genomique.eoulsan.util.Version;
import fr.ens.biologie.genomique.eoulsan.util.r.RExecutor;

/**
 * This class define the step for normalization
 * @author deshaies
 * @since 1.2
 */
@LocalOnly
public class NormalizationLocalStep extends AbstractStep {

  private static final String STEP_NAME = "normalization";

  static final String DESEQ1_DOCKER_IMAGE = "genomicpariscentre/deseq:1.8.3";

  private RExecutor executor;

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

    return "This class compute normalisation of expression data for "
        + "differential analysis.";
  }

  @Override
  public InputPorts getInputPorts() {
    return new InputPortsBuilder()
        .addPort(DEFAULT_SINGLE_INPUT_PORT_NAME, true, EXPRESSION_RESULTS_TSV)
        .create();
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    // Parse R executor parameters
    final Set<Parameter> parameters = new HashSet<>(stepParameters);
    this.executor = CommonConfiguration.parseRExecutorParameter(context,
        parameters, DESEQ1_DOCKER_IMAGE);

    if (!parameters.isEmpty()) {
      Steps.unknownParameter(context, parameters.iterator().next());
    }

  }

  @Override
  public StepResult execute(final StepContext context,
      final StepStatus status) {

    try {

      final DataFormat eDF = DataFormats.EXPRESSION_RESULTS_TSV;

      String rServeName = null;
      boolean rServeEnable = context.getSettings().isRServeServerEnabled();
      if (rServeEnable) {
        rServeName = context.getSettings().getRServeServerName();
      }

      final Design design = context.getWorkflow().getDesign();
      final Normalization norm = new Normalization(design, new File("."),
          eDF.getPrefix(), eDF.getDefaultExtension(), new File("."), rServeName,
          rServeEnable, this.executor);

      norm.run(context, context.getInputData(eDF));

      // Write log file
      return status.createStepResult();

    } catch (EoulsanException e) {

      return status.createStepResult(e,
          "Error while normalizing expression data: " + e.getMessage());
    }

  }
}
