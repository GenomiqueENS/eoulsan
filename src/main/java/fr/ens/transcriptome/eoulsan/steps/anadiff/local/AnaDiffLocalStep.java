/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.steps.anadiff.local;

import java.io.File;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.ExecutorInfo;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.datatypes.DataFormat;
import fr.ens.transcriptome.eoulsan.datatypes.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.steps.anadiff.AnaDiff;

/**
 * This class define the step of differential analysis in local mode.
 * @author Laurent Jourdren
 */
@LocalOnly
public class AnaDiffLocalStep extends AbstractStep {

  private static final String STEP_NAME = "anadiff";

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
  public ExecutionMode getExecutionMode() {

    return Step.ExecutionMode.LOCAL;
  }

  @Override
  public DataFormat[] getInputFormats() {
    return new DataFormat[] {DataFormats.EXPRESSION_RESULTS_TXT};
  }

  @Override
  public DataFormat[] getOutputFormats() {
    return new DataFormat[] {DataFormats.ANADIF_RESULTS_TXT};
  }

  @Override
  public StepResult execute(final Design design, final ExecutorInfo info) {

    try {
      final long startTime = System.currentTimeMillis();
      final StringBuilder log = new StringBuilder();

      final AnaDiff ad = new AnaDiff(design, new File("."));

      ad.run();

      // Write log file
      return new StepResult(this, startTime, log.toString());

    } catch (EoulsanException e) {

      return new StepResult(this, e, "Error while analysis data: "
          + e.getMessage());
    }

  }

}
