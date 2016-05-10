package fr.ens.biologie.genomique.eoulsan.galaxytools.executorinterpreters;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.galaxytools.ToolExecutorResult;
import fr.ens.biologie.genomique.eoulsan.util.ProcessUtils;
import fr.ens.biologie.genomique.eoulsan.util.process.PathProcess;
import fr.ens.biologie.genomique.eoulsan.util.process.ProcessCommand;
import fr.ens.biologie.genomique.eoulsan.util.process.ProcessCommandBuilder;

/**
 * This class define an abstract executor interpreter that contains the default
 * implementation of the <code>execute()</code> method that use a
 * <code>ProcessBuilder</code>.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractExecutorInterpreter
    implements ExecutorInterpreter {

  /**
   * Create a new ProcessCommandBuilder that will be use to launch the command.
   * @return a new ProcessCommandBuilder object
   */
  protected ProcessCommandBuilder newProcessCommandBuilder(
      final String executable) {

    return new PathProcess(executable);
  }

  @Override
  public ToolExecutorResult execute(final List<String> commandLine,
      final File executionDirectory, File temporaryDirectory,
      final File stdoutFile, final File stderrFile) {

    checkNotNull(commandLine, "commandLine argument cannot be null");
    checkNotNull(executionDirectory,
        "executionDirectory argument cannot be null");
    checkNotNull(stdoutFile, "stdoutFile argument cannot be null");
    checkNotNull(stderrFile, "stderrFile argument cannot be null");

    checkArgument(!commandLine.isEmpty(),
        "commandLine argument cannot be empty");
    checkArgument(executionDirectory.isDirectory(),
        "execution directory does not exists or is not a directory: "
            + executionDirectory.getAbsolutePath());

    try {

      final String executable = ProcessUtils.getExecutableFromCommand(commandLine);
      final List<String> arguments = ProcessUtils.getArgumentsFromCommand(commandLine);

      final ProcessCommand processCommand = newProcessCommandBuilder(executable)
          .arguments(arguments).directory(executionDirectory)
          .temporaryDirectory(temporaryDirectory).redirectOutput(stdoutFile)
          .redirectError(stderrFile).create();

      final int exitValue = processCommand.execute().exitCode();

      return new ToolExecutorResult(commandLine, exitValue);

    } catch (IOException e) {
      return new ToolExecutorResult(commandLine, e);
    }
  }

}
