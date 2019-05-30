package fr.ens.biologie.genomique.eoulsan.util.process;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;

/**
 * This interface define how to easily launch a subprocess.
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface SimpleProcess {

  interface AdvancedProcess {

    int waitFor() throws IOException;
  }

  /**
   * Execute a process.
   * @param commandLine command line
   * @param executionDirectory execution directory
   * @param temporaryDirectory temporary directory
   * @param stdoutFile stdout file
   * @param stderrFile stderr file
   * @param filesUsed files used by the process
   * @return the exit code of the process
   * @throws IOException if an error occurs while running the process
   */
  int execute(List<String> commandLine, File executionDirectory,
      File temporaryDirectory, File stdoutFile, File stderrFile,
      File... filesUsed) throws IOException;

  /**
   * Execute a process.
   * @param commandLine command line
   * @param executionDirectory execution directory
   * @param environmentVariables environment variables
   * @param temporaryDirectory temporary directory
   * @param stdoutFile stdout file
   * @param stderrFile stderr file
   * @param redirectErrorStream true if stderr must be redirected in stdout
   * @param filesUsed files used by the process
   * @return the exit code of the process
   * @throws IOException if an error occurs while running the process
   */
  int execute(List<String> commandLine, File executionDirectory,
      Map<String, String> environmentVariables, File temporaryDirectory,
      File stdoutFile, File stderrFile, boolean redirectErrorStream,
      File... filesUsed) throws IOException;

  /**
   * Start a process.
   * @param commandLine command line
   * @param executionDirectory execution directory
   * @param environmentVariables environment variables
   * @param temporaryDirectory temporary directory
   * @param stdoutFile stdout file
   * @param stderrFile stderr file
   * @param redirectErrorStream true if stderr must be redirected in stdout
   * @param filesUsed files used by the process
   * @return an AdvancedProcess object
   * @throws IOException if an error occurs while starting the process
   */
  AdvancedProcess start(List<String> commandLine, File executionDirectory,
      Map<String, String> environmentVariables, File temporaryDirectory,
      File stdoutFile, File stderrFile, boolean redirectErrorStream,
      File... filesUsed) throws IOException;

}