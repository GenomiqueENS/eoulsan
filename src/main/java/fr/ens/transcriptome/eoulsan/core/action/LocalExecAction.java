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

package fr.ens.transcriptome.eoulsan.core.action;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.Command;
import fr.ens.transcriptome.eoulsan.core.Executor;
import fr.ens.transcriptome.eoulsan.core.LocalAnalysisExecutor;
import fr.ens.transcriptome.eoulsan.core.ParamParser;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.steps.mgmt.local.ExecInfoLogStep;

/**
 * This class define the Local exec Action.
 * @author Laurent Jourdren
 */
public class LocalExecAction implements Action {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private static final Set<Parameter> EMPTY_PARAMEMETER_SET =
      Collections.emptySet();

  @Override
  public void action(final String[] args) {

    if (args.length != 2)
      Common.showErrorMessageAndExit("Invalid number of arguments.\n"
          + "usage: " + Globals.APP_NAME_LOWER_CASE
          + " exec param.xml design.txt");

    final File paramFile = new File(args[0]);
    final File designFile = new File(args[1]);

    logger.info(Globals.WELCOME_MSG + " Local mode.");
    logger.info("Parameter file: " + paramFile);
    logger.info("Design file: " + designFile);

    try {

      // Test if param file exists
      if (!paramFile.exists())
        throw new FileNotFoundException(paramFile.toString());

      // Test if design file exists
      if (!designFile.exists())
        throw new FileNotFoundException(designFile.toString());

      // Parse param file
      final ParamParser pp = new ParamParser(paramFile);
      final Command c = new Command();

      // Add execution info to log Step
      c.addStep(ExecInfoLogStep.STEP_NAME, EMPTY_PARAMEMETER_SET);

      pp.parse(c);

      // Execute
      final Executor e = new LocalAnalysisExecutor(c, designFile, paramFile);
      e.execute();

    } catch (FileNotFoundException e) {
      Common.errorExit(e, "File not found: " + e.getMessage());
    } catch (EoulsanException e) {
      Common.errorExit(e, "Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());
    }

  }

}
