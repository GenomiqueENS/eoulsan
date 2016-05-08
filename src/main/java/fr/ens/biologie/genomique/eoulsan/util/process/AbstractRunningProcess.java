package fr.ens.biologie.genomique.eoulsan.util.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * This class define an abstract running process.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractRunningProcess implements RunningProcess {

  private long startTime = -1;
  private long endTime = -1;
  private long duration = -1;
  private int exitCode = -1;

  @Override
  public int waitFor() throws IOException {

    logStartTime();

    this.exitCode = internalWaitFor();

    logEndTime();

    return this.exitCode;
  }

  @Override
  public String getOutput() throws IOException {

    logStartTime();

    final StringBuilder sb = new StringBuilder();

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(getInputStream()))) {

      String line;

      while ((line = reader.readLine()) != null) {
        sb.append(line);
        sb.append('\n');
      }
    }

    logEndTime();

    return sb.toString();
  }

  /**
   * Log the start of the process.
   */
  private void logStartTime() {

    if (this.endTime == -1) {
      this.startTime = System.currentTimeMillis();
    }
  }

  /**
   * Log the end of the process.
   */
  private void logEndTime() {

    if (this.endTime == -1) {
      this.endTime = System.currentTimeMillis();
      this.duration = this.endTime - this.startTime;
    }
  }

  @Override
  public long duration() {

    return this.duration;
  }

  @Override
  public int exitCode() {

    return this.exitCode;
  }

  //
  // Asbtract method
  //

  /**
   * Wait the end of the process.
   * @return the exit code of the process
   * @throws IOException if an error occurs while waiting the process
   */
  protected abstract int internalWaitFor() throws IOException;

}
