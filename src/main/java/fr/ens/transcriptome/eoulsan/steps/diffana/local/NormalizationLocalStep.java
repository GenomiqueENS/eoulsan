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

import java.io.File;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.diffana.Normalization;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define the step for normalization
 * @author deshaies
 * @since 1.2
 */
@LocalOnly
public class NormalizationLocalStep extends AbstractStep {

  private static final String STEP_NAME = "normalization";

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
    return new InputPortsBuilder().addPort(DEFAULT_SINGLE_INPUT_PORT_NAME,
        true, EXPRESSION_RESULTS_TSV).create();
  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    try {

      final DataFormat eDF = DataFormats.EXPRESSION_RESULTS_TSV;

      String rServeName = null;
      boolean rServeEnable = context.getSettings().isRServeServerEnabled();
      if (rServeEnable) {
        rServeName = context.getSettings().getRServeServerName();
      }

      final Design design = context.getWorkflow().getDesign();
      final Normalization norm =
          new Normalization(design, new File("."), eDF.getPrefix(),
              eDF.getDefaultExtension(), new File("."), rServeName,
              rServeEnable);

      norm.run(context, context.getInputData(eDF));

      // Write log file
      return status.createStepResult();

    } catch (EoulsanException e) {

      return status.createStepResult(e,
          "Error while normalizing expression data: " + e.getMessage());
    }

  }
}
