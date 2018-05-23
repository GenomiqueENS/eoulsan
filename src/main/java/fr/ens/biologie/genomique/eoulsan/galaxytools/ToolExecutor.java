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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */
package fr.ens.biologie.genomique.eoulsan.galaxytools;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.workflow.TaskContextImpl;
import fr.ens.biologie.genomique.eoulsan.galaxytools.executorinterpreters.DefaultExecutorInterpreter;
import fr.ens.biologie.genomique.eoulsan.galaxytools.executorinterpreters.DockerExecutorInterpreter;
import fr.ens.biologie.genomique.eoulsan.galaxytools.executorinterpreters.ExecutorInterpreter;
import fr.ens.biologie.genomique.eoulsan.galaxytools.executorinterpreters.GenericExecutorInterpreter;

/**
 * The class define an executor on tool set in XML file.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class ToolExecutor {

  private static final String STDOUT_SUFFIX = ".galaxytool.out";
  private static final String STDERR_SUFFIX = ".galaxytool.err";

  private final TaskContext stepContext;
  private final ToolInfo toolData;
  private final String commandLine;
  private final Set<File> inputFiles;

  /**
   * Execute a tool.
   * @return a ToolExecutorResult object
   * @throws IOException if an error occurs while executing the tool
   */
  ToolExecutorResult execute() throws IOException {

    checkArgument(!this.commandLine.isEmpty(),
        "Command line for Galaxy tool is empty");

    final String interpreter = this.toolData.getInterpreter();

    // Define the interpreter to use
    final ExecutorInterpreter ti;
    switch (interpreter) {

    case "":
      ti = new DefaultExecutorInterpreter();
      break;

    case "docker":
      ti = new DockerExecutorInterpreter(this.toolData.getDockerImage());
      break;

    default:
      ti = new GenericExecutorInterpreter(interpreter);
      break;
    }

    // Create the command line
    final List<String> command = ti.createCommandLine(this.commandLine);

    final TaskContextImpl context = (TaskContextImpl) this.stepContext;

    final File executionDirectory = context.getStepOutputDirectory().toFile();
    final File workflowOutputDirectory = context.getOutputDirectory().toFile();
    final File logDirectory = context.getTaskOutputDirectory().toFile();
    final File tempDirectory = context.getLocalTempDirectory();

    final File stdoutFile =
        new File(logDirectory, context.getTaskFilePrefix() + STDOUT_SUFFIX);
    final File stderrFile =
        new File(logDirectory, context.getTaskFilePrefix() + STDERR_SUFFIX);

    getLogger().info("Interpreter: " + interpreter);
    getLogger().info("Command: " + command);
    getLogger().info("Execution directory: " + executionDirectory);
    getLogger().info("Workflow output directory: " + workflowOutputDirectory);
    getLogger().info("Temporary directory: " + tempDirectory);
    getLogger().info("Stdout: " + stdoutFile);
    getLogger().info("Stderr: " + stderrFile);

    return ti.execute(command, executionDirectory, tempDirectory, stdoutFile,
        stderrFile, toArray(inputFiles, workflowOutputDirectory));
  }

  /**
   * Convert collection and array of File objects into an Array.
   * @param collection the collection to convert
   * @param files the array to convert
   * @return an Array of File
   */
  private static File[] toArray(Collection<File> collection, File... files) {

    final List<File> list = new ArrayList<>(collection);

    if (files != null) {
      list.addAll(Arrays.asList(files));
    }

    return list.toArray(new File[0]);
  }

  //
  // Constructor
  //

  /**
   * Constructor a new galaxy tool executor.
   * @param context the context
   * @param toolData the tool data
   * @param commandLine the command line
   * @param inputFiles input files to use
   * @throws IOException if an error occurs while executing the command
   */
  public ToolExecutor(final TaskContext context, final ToolInfo toolData,
      final String commandLine, final Set<File> inputFiles) throws IOException {

    checkNotNull(commandLine, "commandLine is null.");
    checkNotNull(context, "Step context is null.");

    this.toolData = toolData;
    this.commandLine = commandLine.trim();
    this.stepContext = context;
    this.inputFiles = inputFiles;
  }

}
