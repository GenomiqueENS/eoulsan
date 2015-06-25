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
package fr.ens.transcriptome.eoulsan.galaxytools;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskContext;

/**
 * The class define an executor on tool set in XML file.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class ToolExecutor {

  private static final String STDOUT_SUFFIX = ".galaxytool.out";
  private static final String STDERR_SUFFIX = ".galaxytool.err";

  private final StepContext stepContext;
  private final ToolData toolData;
  private final String commandLineTool;

  /**
   * Execute a tool.
   * @return a ToolExecutorResult object
   */
  ToolExecutorResult execute() {

    checkArgument(!this.commandLineTool.isEmpty(),
        "Command line for Galaxy tool is empty");

    final String interpreter = this.toolData.getInterpreter();

    // Define the interpreter to use
    final ToolExecutorInterpreter ti;
    switch (interpreter) {

    case "":
      ti = new DefaultToolExecutorInterpreter();
      break;

    case "docker":
      ti =
          new DockerToolExecutorInterpreter(EoulsanRuntime.getSettings()
              .getDockerConnectionURI(), this.toolData.getDockerImage(),
              EoulsanRuntime.getSettings().getTempDirectoryFile());
      break;

    default:
      ti = new GenericToolExecutorInterpreter(interpreter);
      break;
    }

    // Create the command line
    final List<String> command =
        ti.createCommandLine(splitCommandLine(this.commandLineTool));

    final TaskContext context = (TaskContext) this.stepContext;

    final File executionDirectory = context.getStepOutputDirectory().toFile();
    final File logDirectory = context.getTaskOutputDirectory().toFile();
    final File tempDirectory = context.getLocalTempDirectory();

    final File stdoutFile =
        new File(logDirectory, context.getTaskFilePrefix() + STDOUT_SUFFIX);
    final File stderrFile =
        new File(logDirectory, context.getTaskFilePrefix() + STDERR_SUFFIX);

    getLogger().info("Interpreter: " + interpreter);
    getLogger().info("Command: " + command);
    getLogger().info("Execution directory: " + executionDirectory);
    getLogger().info("Stdout: " + stdoutFile);
    getLogger().info("Stderr: " + stderrFile);

    return ti.execute(command, executionDirectory, tempDirectory, stdoutFile,
        stderrFile);
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
   * @param interpreter the interpreter to use
   * @param commandLine the command line
   * @param toolName the tool name
   * @param toolVersion the tool version
   */
  public ToolExecutor(final StepContext context, final ToolData toolData,
      final String commandLine) {

    checkNotNull(commandLine, "commandLine is null.");
    checkNotNull(context, "Step context is null.");

    this.toolData = toolData;
    this.commandLineTool = commandLine.trim();
    this.stepContext = context;

    execute();
  }

}
