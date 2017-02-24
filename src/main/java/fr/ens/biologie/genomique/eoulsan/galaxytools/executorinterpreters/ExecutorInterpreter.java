package fr.ens.biologie.genomique.eoulsan.galaxytools.executorinterpreters;

import java.io.File;
import java.io.IOException;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.galaxytools.ToolExecutorResult;

/**
 * This interface define a executor interpreter for Galaxy tools.
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface ExecutorInterpreter {

  /**
   * Get the name of the interpreter.
   * @return the name of the interpreter
   */
  String getName();

  /**
   * Create the command line for the the argument of the interpreter.
   * @param arguments the interpreter arguments
   * @return the command line
   */
  List<String> createCommandLine(String arguments);

  /**
   * Execute a command line.
   * @param commandLine the command line to execute
   * @param executionDirectory execution directory
   * @param temporaryDirectory temporary directory
   * @param stdoutFile stdout file
   * @param stderrFile stderr file
   * @return a ToolExecutor object
   * @throws IOException if an error occurs while executing the command
   */
  ToolExecutorResult execute(final List<String> commandLine,
      File executionDirectory, File temporaryDirectory, File stdoutFile,
      File stderrFile) throws IOException;

}
