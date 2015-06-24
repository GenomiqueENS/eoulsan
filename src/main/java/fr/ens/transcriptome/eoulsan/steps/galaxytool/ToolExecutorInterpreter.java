package fr.ens.transcriptome.eoulsan.steps.galaxytool;

import java.io.File;
import java.util.List;

/**
 * This interface define a executor interpreter for Galaxy tools.
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface ToolExecutorInterpreter {

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
  List<String> createCommandLine(List<String> arguments);

  /**
   * Execute a command line.
   * @param commandLine the command line to execute
   * @param executionDirectory execution directory
   * @param stdoutFile stdout file
   * @param stderrFile stderr file
   * @return a ToolExecutor object
   */
  ToolExecutorResult execute(final List<String> commandLine,
      File executionDirectory, File stdoutFile, File stderrFile);

}