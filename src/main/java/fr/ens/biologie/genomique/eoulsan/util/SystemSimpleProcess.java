package fr.ens.biologie.genomique.eoulsan.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;

/**
 * This class define how to easily launch a subprocess using the Java Process
 * API.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class SystemSimpleProcess extends AbstractSimpleProcess {

  @Override
  public int execute(final List<String> commandLine,
      final File executionDirectory,
      final Map<String, String> environmentVariables,
      final File temporaryDirectory, final File stdoutFile,
      final File stderrFile, final boolean redirectErrorStream)
      throws EoulsanException {

    final ProcessBuilder pb = new ProcessBuilder(commandLine);

    // Set execution directory
    if (executionDirectory != null) {
      pb.directory(executionDirectory);
    }

    // Set environment variables
    if (environmentVariables != null) {
      pb.environment().putAll(environmentVariables);
    }

    // Set temporary directory
    if (temporaryDirectory != null) {
      pb.environment().put(TMP_DIR_ENV_VARIABLE,
          temporaryDirectory.getAbsolutePath());
    }

    // Redirect stdout
    if (stdoutFile != null) {
      pb.redirectOutput(stdoutFile);
    }

    // Redirect stderr
    if (redirectErrorStream) {
      pb.redirectErrorStream(true);
    } else if (stderrFile != null) {
      pb.redirectError(stderrFile);
    }

    // Start the process
    try {
      return pb.start().waitFor();
    } catch (IOException | InterruptedException e) {
      throw new EoulsanException(e);
    }
  }

}
