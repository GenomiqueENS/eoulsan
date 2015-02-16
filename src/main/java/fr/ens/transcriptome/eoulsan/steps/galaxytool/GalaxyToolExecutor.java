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
package fr.ens.transcriptome.eoulsan.steps.galaxytool;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.StepContext;

public class GalaxyToolExecutor {

  private final StepContext stepContext;
  private final ToolInterpreter toolInterpreter;

  private int exitValue = -1;
  private Throwable exception = null;
  private boolean asExecutedCommand = false;

  public Throwable getException() {
    return this.exception;
  }

  public boolean asThrowedException() {
    return this.exception != null;
  }

  public int getExitValue() {
    return exitValue;
  }

  private void execute() {

    final String commandTool = this.toolInterpreter.getCommandLine();

    checkNotNull(commandTool, "Command line galaxy tool is null.");

    if (asExecutedCommand) {
      this.exception =
          new EoulsanException(
              "GalaxyTool sample already executed this command " + commandTool);
      return;
    }

    try {
      // Define stdout and stderr file
      // Execute command
      final Process p = Runtime.getRuntime().exec(commandTool, null);

      // Save stdout
      new CopyProcessOutput(p.getInputStream(), createStepOutput("STDOUT"),
          "stdout").start();

      // Save stderr
      new CopyProcessOutput(p.getErrorStream(), createStepOutput("STDERR"),
          "stderr").start();

      // Wait the end of the process
      exitValue = p.waitFor();

      asExecutedCommand = true;

    } catch (InterruptedException | IOException e) {
      this.exception = e;
    }
  }

  private File createStepOutput(final String suffix) {

    final String toolName = this.stepContext.getCommandName();
    final String idStep = this.stepContext.getJobId();

    return new File(this.stepContext.getStepOutputDirectory().toFile(),
        toolName + "_" + idStep + "_" + suffix + ".txt");
  }

  //
  // Constructor
  //

  public GalaxyToolExecutor(final StepContext context,
      final ToolInterpreter toolInpreter) {

    checkNotNull(toolInpreter, "Tool interpreter is null.");
    checkNotNull(context, "Step context is null.");

    this.toolInterpreter = toolInpreter;
    this.stepContext = context;

    execute();
  }

  //
  // Internal class
  //

  /**
   * This internal class allow to save Process outputs.
   * @author Laurent Jourdren
   */
  private static final class CopyProcessOutput extends Thread {

    private final Path path;
    private final InputStream in;
    private final String desc;

    @Override
    public void run() {

      try {
        Files.copy(this.in, this.path, StandardCopyOption.REPLACE_EXISTING);
      } catch (final IOException e) {
        getLogger().warning(
            "Error while copying " + this.desc + ": " + e.getMessage());
      }

    }

    CopyProcessOutput(final InputStream in, final File file, final String desc) {

      checkNotNull(in, "in argument cannot be null");
      checkNotNull(file, "file argument cannot be null");
      checkNotNull(desc, "desc argument cannot be null");

      this.in = in;
      this.path = file.toPath();
      this.desc = desc;
    }
  }
}
