package fr.ens.biologie.genomique.eoulsan.util.process;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * This class an abstract SimpleProcess class.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractSimpleProcess implements SimpleProcess {

  protected static final String TMP_DIR_ENV_VARIABLE = "TMPDIR";

  @Override
  public int execute(final List<String> commandLine,
      final File executionDirectory, final File temporaryDirectory,
      final File stdoutFile, final File stderrFile, final File... filesUsed)
      throws IOException {

    return execute(commandLine, executionDirectory, null, temporaryDirectory,
        stdoutFile, stderrFile, false, filesUsed);
  }

  @Override
  public int execute(final List<String> commandLine,
      final File executionDirectory,
      final Map<String, String> environmentVariables,
      final File temporaryDirectory, final File stdoutFile,
      final File stderrFile, final boolean redirectErrorStream,
      final File... filesUsed) throws IOException {

    AdvancedProcess process = start(commandLine, executionDirectory,
        environmentVariables, temporaryDirectory, stdoutFile, stderrFile,
        redirectErrorStream, filesUsed);

    return process.waitFor();
  }

}
