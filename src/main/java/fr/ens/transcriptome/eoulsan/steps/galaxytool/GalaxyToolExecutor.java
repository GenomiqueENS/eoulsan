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

import fr.ens.transcriptome.eoulsan.core.StepContext;

public class GalaxyToolExecutor {

  private final StepContext stepContext;
  private final String commandLineTool;
  private final String toolName;
  private final String toolVersion;

  GalaxyToolResult execute() {

    checkNotNull(commandLineTool, "Command line galaxy tool is null.");

    final GalaxyToolResult result = new GalaxyToolResult(commandLineTool);

    try {
      // Execute command
      final Process p = Runtime.getRuntime().exec(commandLineTool, null);

      // Save stdout
      new CopyProcessOutput(p.getInputStream(), createStepOutput("STDOUT"),
          "stdout").start();

      // Save stderr
      new CopyProcessOutput(p.getErrorStream(), createStepOutput("STDERR"),
          "stderr").start();

      // Wait the end of the process
      final int exitValue = p.waitFor();

      result.setExitValue(exitValue);

    } catch (InterruptedException | IOException e) {
      result.setException(e);
    }

    return result;
  }

  private File createStepOutput(final String suffix) {

    return new File(this.stepContext.getStepOutputDirectory().toFile(),
        this.toolName + "_" + this.toolVersion + "." + suffix);
  }

  //
  // Constructor
  //

  /**
   * Constructor a new galaxy tool executor.
   * @param context the context
   * @param commandLine the command line
   * @param toolName the tool name
   * @param toolVersion the tool version
   */
  public GalaxyToolExecutor(final StepContext context,
      final String commandLine, final String toolName, final String toolVersion) {

    checkNotNull(commandLine, "commandLine is null.");
    checkNotNull(context, "Step context is null.");

    this.commandLineTool = commandLine;
    this.stepContext = context;
    this.toolName = toolName;
    this.toolVersion = toolVersion;

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
