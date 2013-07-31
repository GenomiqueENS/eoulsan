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
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.io.DesignReader;
import fr.ens.transcriptome.eoulsan.design.io.SimpleDesignReader;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define an executor for local mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class LocalExecutor extends Executor {

  private final ExecutionArguments arguments;

  private final File designFile;

  @Override
  protected ExecutionArguments getExecutionArguments() {

    return this.arguments;
  }

  @Override
  protected Design loadDesign() throws EoulsanException {

    if (this.designFile == null)
      throw new EoulsanException("The design file is null.");

    if (!this.designFile.exists())
      throw new EoulsanException("The design file does not exist: "
          + this.designFile);

    DesignReader dr = new SimpleDesignReader(designFile);
    return dr.read();
  }

  @Override
  protected void writeStepLogs(final StepResult result) {

    if (result == null || result.getStep() == null)
      return;

    try {

      final File logDir =
          new File(this.designFile.getParent(), getExecutionArguments()
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
   * @param command command to execute
   * @param designFile the path to the design file
   * @param paramFile the path to the parameter file
   * @param context execution context
   */
  public LocalExecutor(final Command command, final File designFile,
      final File paramFile, final ExecutionArguments arguments) {

    if (command == null)
      throw new NullPointerException("The command is null");

    setCommand(command);

    if (designFile == null)
      throw new NullPointerException("The design file is null");

    this.designFile = designFile;
    this.arguments = arguments;
  }
}
