package fr.ens.biologie.genomique.eoulsan.galaxytools.executorinterpreters;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.eoulsan.galaxytools.ToolExecutorResult;
import fr.ens.biologie.genomique.kenetre.util.process.SimpleProcess;
import fr.ens.biologie.genomique.kenetre.util.process.SystemSimpleProcess;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This class define an abstract executor interpreter that contains the default implementation of
 * the <code>execute()</code> method that use a <code>ProcessBuilder</code>.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractExecutorInterpreter implements ExecutorInterpreter {

  /**
   * Create a new SimpleProcess that will be use to launch the command.
   *
   * @return a new SimpleProcess object
   * @throws IOException if an error occurs while creating the process
   */
  protected SimpleProcess newSimpleProcess() throws IOException {

    return new SystemSimpleProcess();
  }

  @Override
  public ToolExecutorResult execute(
      final List<String> commandLine,
      final File executionDirectory,
      File temporaryDirectory,
      final File stdoutFile,
      final File stderrFile,
      final File... filesUsed)
      throws IOException {

    requireNonNull(commandLine, "commandLine argument cannot be null");
    requireNonNull(executionDirectory, "executionDirectory argument cannot be null");
    requireNonNull(stdoutFile, "stdoutFile argument cannot be null");
    requireNonNull(stderrFile, "stderrFile argument cannot be null");

    checkArgument(!commandLine.isEmpty(), "commandLine argument cannot be empty");
    checkArgument(
        executionDirectory.isDirectory(),
        "execution directory does not exists or is not a directory: "
            + executionDirectory.getAbsolutePath());

    final int exitValue =
        newSimpleProcess()
            .execute(
                commandLine,
                executionDirectory,
                temporaryDirectory,
                stdoutFile,
                stderrFile,
                filesUsed);

    return new ToolExecutorResult(commandLine, exitValue);
  }
}
