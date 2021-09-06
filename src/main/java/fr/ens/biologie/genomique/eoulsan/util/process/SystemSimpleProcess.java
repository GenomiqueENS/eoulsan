package fr.ens.biologie.genomique.eoulsan.util.process;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.log.EoulsanRuntimeLogger;
import fr.ens.biologie.genomique.eoulsan.log.GenericLogger;

/**
 * This class define how to easily launch a subprocess using the Java Process
 * API.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class SystemSimpleProcess extends AbstractSimpleProcess {

  private final GenericLogger logger;

  @Override
  public AdvancedProcess start(final List<String> commandLine,
      final File executionDirectory,
      final Map<String, String> environmentVariables,
      final File temporaryDirectory, final File stdoutFile,
      final File stderrFile, final boolean redirectErrorStream,
      final File... filesUsed) throws IOException {

    this.logger.debug(getClass().getName()
        + " : commandLine=" + commandLine + ", executionDirectory="
        + executionDirectory + ", environmentVariables=" + environmentVariables
        + ", temporaryDirectory=" + temporaryDirectory + ", stdoutFile="
        + stdoutFile + ", stderrFile=" + stderrFile + ", redirectErrorStream="
        + redirectErrorStream + ", filesUsed=" + Arrays.toString(filesUsed));

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

    final Process process = pb.start();

    return () -> {

      try {
        return process.waitFor();
      } catch (InterruptedException e) {
        throw new IOException(e);
      }
    };
  }

  //
  // Constructors
  //

  /**
   * Constructor.
   */
  public SystemSimpleProcess() {

    this(null);
  }

  /**
   * Constructor.
   * @param logger logger to use
   */
  public SystemSimpleProcess(GenericLogger logger) {

    this.logger = logger == null ? new EoulsanRuntimeLogger() : logger;
  }

}
