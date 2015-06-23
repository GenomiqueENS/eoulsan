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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.google.common.base.Preconditions;

import fr.ens.transcriptome.eoulsan.core.StepContext;

/**
 * The class define an executor on tool set in XML file.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class ToolExecutor {

  private static final String STDOUT_SUFFIX = ".STDOUT";
  private static final String STDERR_SUFFIX = ".STDERR";

  private final StepContext stepContext;
  private final List<String> commandLineTool;
  private final String toolName;
  private final String toolVersion;

  /**
   * Execute a tool.
   * @return a ToolExecutorResult object
   */
  ToolExecutorResult execute() {

    Preconditions.checkArgument(this.commandLineTool.isEmpty(),
        "Command line for Galaxy tool is empty");

    try {

      ProcessBuilder builder = new ProcessBuilder(this.commandLineTool);
      builder.redirectOutput(createStepOutput(STDOUT_SUFFIX));
      builder.redirectError(createStepOutput(STDERR_SUFFIX));

      // Execute command
      final Process p = builder.start();

      // Wait the end of the process
      final int exitValue = p.waitFor();

      return new ToolExecutorResult(commandLineTool, exitValue);

    } catch (InterruptedException | IOException e) {
      return new ToolExecutorResult(commandLineTool, e);
    }
  }

  /**
   * Get the path of the command output.
   * @param suffix the suffix of the command output
   * @return a File
   */
  private File createStepOutput(final String suffix) {

    return new File(this.stepContext.getStepOutputDirectory().toFile(),
        this.toolName + "_" + this.toolVersion + suffix);
  }

  /**
   * Split the command line in list of arguments.
   * @param commandLine the command line to parse
   * @return a list of string arguments
   */
  private static final List<String> splitCommandLine(final String commandLine) {

    final StringTokenizer st = new StringTokenizer(commandLine);
    final List<String> result = new ArrayList<>(st.countTokens());

    while (st.hasMoreTokens()) {
      result.add(st.nextToken());
    }

    return result;
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
  public ToolExecutor(final StepContext context, final String commandLine,
      final String toolName, final String toolVersion) {

    checkNotNull(commandLine, "commandLine is null.");
    checkNotNull(context, "Step context is null.");

    this.commandLineTool = splitCommandLine(commandLine);
    this.stepContext = context;
    this.toolName = toolName;
    this.toolVersion = toolVersion;

    execute();
  }

}
