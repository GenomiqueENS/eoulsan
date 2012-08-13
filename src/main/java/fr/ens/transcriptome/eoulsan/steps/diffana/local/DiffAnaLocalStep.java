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
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.steps.diffana.DiffAna;

/**
 * This class define the step of differential analysis in local mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
@LocalOnly
public class DiffAnaLocalStep extends AbstractStep {

  private static final String STEP_NAME = "diffana";

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  //
  // Step methods
  //

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public String getDescription() {

    return "This class compute the differential analysis for the experiment.";
  }

  @Override
  public DataFormat[] getInputFormats() {
    return new DataFormat[] {DataFormats.EXPRESSION_RESULTS_TXT};
  }

  @Override
  public DataFormat[] getOutputFormats() {
    return new DataFormat[] {DataFormats.DIFFANA_RESULTS_TSV};
  }

  @Override
  public StepResult execute(final Design design, final Context context) {

    try {
      final long startTime = System.currentTimeMillis();
      final StringBuilder log = new StringBuilder();

      final DataFormat eDF = DataFormats.EXPRESSION_RESULTS_TXT;

      LOGGER.info("Rserve enable : "
          + EoulsanRuntime.getSettings().isRServeServerEnabled());
      
      String rServeName = null;
      if (EoulsanRuntime.getRuntime().getSettings().isRServeServerEnabled())
        rServeName =
            EoulsanRuntime.getRuntime().getSettings().getRServeServername();

      final DiffAna ad =
          new DiffAna(design, new File("."), eDF.getType().getPrefix(),
              eDF.getDefaultExtention(), new File("."), rServeName);
      ad.run();

      // Write log file
      return new StepResult(context, startTime, log.toString());

    } catch (EoulsanException e) {

      return new StepResult(context, e, "Error while analysis data: "
          + e.getMessage());
    }

  }

}