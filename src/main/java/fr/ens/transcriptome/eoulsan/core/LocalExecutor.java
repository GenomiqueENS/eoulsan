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

package fr.ens.transcriptome.eoulsan.core;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define an executor for local mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class LocalExecutor extends Executor {

  @Override
  protected void writeStepLogs(final StepResult result) {

    if (result == null || result.getStep() == null)
      return;

    try {

      final File logDir =
          new File(new File(getArguments().getLogPathname()), getArguments()
              .getJobId());

      if (!logDir.exists())
        if (!logDir.mkdirs()) {
          throw new IOException("Can not create log directory: "
              + logDir.getAbsolutePath());
        }

      final String logFilename = result.getStep().getId();

      final Writer writer =
          FileUtils.createFastBufferedWriter(new File(logDir, logFilename
              + ".log"));

      if (result.getLogMessage() != null)
        writer.write(result.getLogMessage());
      else
        writer.write("Nothing to log.");
      writer.close();

    } catch (IOException e) {

      Common.showAndLogErrorMessage("Unable to create log file for "
          + result.getStep() + " step.");
    }

  }

  @Override
  protected void checkTemporaryDirectory() {

    final File tempDir = EoulsanRuntime.getSettings().getTempDirectoryFile();

    if (tempDir == null)
      throw new EoulsanRuntimeException("Temporary directory is null");

    if ("".equals(tempDir.getAbsolutePath()))
      throw new EoulsanRuntimeException("Temporary directory is null");

    if (!tempDir.exists())
      throw new EoulsanRuntimeException("Temporary directory does not exists: "
          + tempDir);

    if (!tempDir.isDirectory())
      throw new EoulsanRuntimeException(
          "Temporary directory is not a directory: " + tempDir);

    if (!tempDir.canRead())
      throw new EoulsanRuntimeException("Temporary directory cannot be read: "
          + tempDir);

    if (!tempDir.canWrite())
      throw new EoulsanRuntimeException(
          "Temporary directory cannot be written: " + tempDir);

    if (!tempDir.canExecute())
      throw new EoulsanRuntimeException(
          "Temporary directory is not executable: " + tempDir);
  }

  //
  // Constructor
  //

  /**
   * Constructor
   * @param arguments executor arguments
   * @throws EoulsanException if an error occurs while loading the design file
   *           or the workflow file
   */
  public LocalExecutor(final ExecutorArguments arguments)
      throws EoulsanException {

    super(arguments);
  }
}
