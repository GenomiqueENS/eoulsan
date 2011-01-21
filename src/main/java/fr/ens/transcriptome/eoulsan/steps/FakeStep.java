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

package fr.ens.transcriptome.eoulsan.steps;

import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.design.Design;

/**
 * This step is a fake step.
 * @author Laurent Jourdren
 */
@HadoopCompatible
public class FakeStep extends AbstractStep {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  @Override
  public String getName() {

    return "fakestep";
  }

  @Override
  public void configure(Set<Parameter> stepParameters,
      Set<Parameter> globalParameters) {

    for (Parameter p : stepParameters)
      logger.info("s: " + p.getName() + "\t" + p.getStringValue());

    for (Parameter p : globalParameters)
      logger.info("g: " + p.getName() + "\t" + p.getStringValue());

  }

  @Override
  public StepResult execute(final Design design, final Context context) {

    logger.info("execute design: " + design);

    return new StepResult(context, true, null);
  }

}
