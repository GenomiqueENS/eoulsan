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

import java.io.File;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.steps.diffana.Normalization;

/**
 * This class define the step for normalization
 * @author deshaies
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
  public String getDescription() {

    return "This class compute normalisation of expression data for "
        + "differential analysis.";
  }

  @Override
  public DataFormat[] getInputFormats() {
    return new DataFormat[] {DataFormats.EXPRESSION_RESULTS_TSV};
  }

  @Override
  public StepResult execute(final Design design, final Context context,
      final StepStatus status) {

    try {

      final DataFormat eDF = DataFormats.EXPRESSION_RESULTS_TSV;

      String rServeName = null;
      boolean rServeEnable = context.getSettings().isRServeServerEnabled();
      if (rServeEnable)
        rServeName = context.getSettings().getRServeServername();

      final Normalization norm =
          new Normalization(design, new File("."), eDF.getPrefix(),
              eDF.getDefaultExtention(), new File("."), rServeName,
              rServeEnable);

      norm.run(context);

      // Write log file
      return status.createStepResult();

    } catch (EoulsanException e) {

      return status.createStepResult(e,
          "Error while normalizing expression data: " + e.getMessage());
    }

  }
}
