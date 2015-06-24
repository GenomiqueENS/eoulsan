package fr.ens.transcriptome.eoulsan.steps.galaxytool;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.python.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This class define an abstract executor interpreter that contains the default
 * implementation of the <code>execute()</code> method that use a
 * <code>ProcessBuilder</code>.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractToolExecutorInterpreter implements
    ToolExecutorInterpreter {

  static final String TMP_DIR_ENV_VARIABLE = "TMPDIR";

  @Override
  public ToolExecutorResult execute(final List<String> commandLine,
      final File executionDirectory, File temporaryDirectory,
      final File stdoutFile, final File stderrFile) {

    checkNotNull(commandLine, "commandLine argument cannot be null");
    checkNotNull(executionDirectory,
        "executionDirectory argument cannot be null");
    checkNotNull(stdoutFile, "stdoutFile argument cannot be null");
    checkNotNull(stderrFile, "stderrFile argument cannot be null");

    checkArgument(executionDirectory.isDirectory(),
        "execution directory does not exists or is not a directory: "
            + executionDirectory.getAbsolutePath());

    try {

      ProcessBuilder builder = new ProcessBuilder(commandLine);
      builder.directory(executionDirectory);
      builder.redirectOutput(stdoutFile);
      builder.redirectError(stderrFile);

      // Set the temporary directory if exists
      if (executionDirectory.isDirectory()) {
        builder.environment().put(TMP_DIR_ENV_VARIABLE,
            executionDirectory.getAbsolutePath());
      }

      // Execute command
      final Process p = builder.start();

      // Wait the end of the process
      final int exitValue = p.waitFor();

      return new ToolExecutorResult(commandLine, exitValue);

    } catch (InterruptedException | IOException e) {
      return new ToolExecutorResult(commandLine, e);
    }
  }

}
