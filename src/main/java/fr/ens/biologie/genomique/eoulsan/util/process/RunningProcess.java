package fr.ens.biologie.genomique.eoulsan.util.process;

import java.io.IOException;
import java.io.InputStream;

/**
 * This interface define a running process.
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface RunningProcess {

  /**
   * Wait the end of the process.
   * @return the exit code of the process
   * @throws IOException if an error occurs while waiting the process
   */
  int waitFor() throws IOException;

  /**
   * Get the duration of the command in milliseconds.
   * @return the duration of the command in milliseconds
   */
  long duration();

  /**
   * Get the exit code the of command.
   * @return the exit code of the command
   */
  int exitCode();

  /**
   * Execute the process and get its input stream.
   * @return The InputStream of the output
   * @throws IOException if an error occurs while executing the process
   */
  InputStream getInputStream() throws IOException;

  /**
   * Execute the process and get its output.
   * @return a string with the output
   * @throws IOException if an error occurs while executing the process
   */
  String getOutput() throws IOException;

}
