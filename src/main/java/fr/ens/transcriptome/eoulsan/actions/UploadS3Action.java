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

package fr.ens.transcriptome.eoulsan.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.Command;
import fr.ens.transcriptome.eoulsan.core.Executor;
import fr.ens.transcriptome.eoulsan.core.LocalAnalysisExecutor;
import fr.ens.transcriptome.eoulsan.core.ParamParser;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.steps.TerminalStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.local.ExecInfoLogStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.upload.LocalUploadStep;

/**
 * This class define the Local Upload S3 Action.
 * @author Laurent Jourdren
 */
public class UploadS3Action implements Action {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private static final Set<Parameter> EMPTY_PARAMEMETER_SET =
      Collections.emptySet();

  @Override
  public void action(final String[] args) {

    if (args.length != 3)
      Common.showErrorMessageAndExit("Invalid number of arguments.\n"
          + "usage: " + Globals.APP_NAME_LOWER_CASE
          + " s3upload param.xml design.txt s3://mybucket/test");

    final File paramFile = new File(args[0]);
    final File designFile = new File(args[1]);
    final DataFile dest = new DataFile(args[2]);

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
      e.execute(Lists.newArrayList((Step) new LocalUploadStep(dest),
          (Step) new TerminalStep()), null, true);

    } catch (FileNotFoundException e) {
      Common.errorExit(e, "File not found: " + e.getMessage());
    } catch (EoulsanException e) {
      Common.errorExit(e, "Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());
    }

  }

}
