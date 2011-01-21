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
import java.io.IOException;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.HadoopJarRepackager;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;

/**
 * This class launch Eoulsan in hadoop mode.
 * @author Laurent Jourdren
 */
public class HadoopExecAction implements Action {

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String HADOOP_CMD = "hadoop jar ";

  @Override
  public void action(final String[] arguments) {

    if (arguments.length != 3) {
      Common.showErrorMessageAndExit("Invalid number of arguments.\n"
          + "usage: " + Globals.APP_NAME_LOWER_CASE
          + " hadoopexec param.xml design.txt hdfs://example.org/test");
    }

    final String paramFile = arguments[0];
    final String designFile = arguments[1];
    final String hdfsPath = arguments[2];

    run(paramFile, designFile, hdfsPath);
  }

  private void run(final String paramFile, final String designFile,
      final String hdfsPath) {

    try {

      File repackagedJarFile = HadoopJarRepackager.repack();

      LOGGER.info("Launch Eoulsan in Hadoop mode.");

      ProcessUtils.execThreadOutput(HADOOP_CMD
          + repackagedJarFile.getCanonicalPath() + " exec " + paramFile + " "
          + designFile + " " + hdfsPath);

    } catch (IOException e) {
      Common.errorExit(e, "Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());
    }

  }

}
