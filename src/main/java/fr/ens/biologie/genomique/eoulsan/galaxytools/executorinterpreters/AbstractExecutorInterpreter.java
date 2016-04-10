package fr.ens.biologie.genomique.eoulsan.galaxytools.executorinterpreters;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.galaxytools.ToolExecutorResult;
import fr.ens.biologie.genomique.eoulsan.util.SimpleProcess;
import fr.ens.biologie.genomique.eoulsan.util.SystemSimpleProcess;

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
   * Create a new SimpleProcess that will be use to launch the command.
   * @return a new SimpleProcess object
   */
  protected SimpleProcess newSimpleProcess() {

    return new SystemSimpleProcess();
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

      final int exitValue = newSimpleProcess().execute(commandLine,
          executionDirectory, temporaryDirectory, stdoutFile, stderrFile);

      return new ToolExecutorResult(commandLine, exitValue);

    } catch (EoulsanException e) {
      return new ToolExecutorResult(commandLine, e);
    }
  }

}
