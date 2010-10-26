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

package fr.ens.transcriptome.eoulsan.core;

import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.datatypes.DataType;
import fr.ens.transcriptome.eoulsan.design.Design;

/**
 * This step is a fake step.
 * @author Laurent Jourdren
 */
public class FakeStep implements Step {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  @Override
  public String getName() {

    return "fakestep";
  }

  @Override
  public String getDescription() {

    return "A fake step";
  }

  @Override
  public DataType[] getInputTypes() {
    return null;
  }

  @Override
  public DataType[] getOutputType() {
    return null;
  }

  @Override
  public String getLogName() {

    return null;
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
  public StepResult execute(final Design design, final ExecutorInfo info) {

    logger.info("execute design: " + design);

    return new StepResult(this, true, null);
  }

}
