package fr.ens.biologie.genomique.eoulsan.util.process;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class define the common code used by all the process classes that use
 * the Java Process API.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractPathProcess extends ProcessCommandBuilder {

  private String executablePath;

  //
  // Getter
  //

  /**
   * Get the path of the executable
   * @return path of the executable
   */
  protected String getExecutablePath() {

    return this.executablePath;
  }

  //
  // Setter
  //

  /**
   * Set the path of the executable
   * @param executablePath the path to the executable
   */
  protected void setExecutablePath(final String executablePath) {

    if (executablePath == null) {
      throw new NullPointerException("executable argument cannot be null");
    }

    this.executablePath = executablePath;
  }

  /**
   * Set the path of the executable
   * @param executableFile the path to the executable
   */
  protected void setExecutablePath(final File executableFile) {

    if (executableFile == null) {
      throw new NullPointerException("executable argument cannot be null");
    }

    this.executablePath = executableFile.getAbsolutePath();
  }

  //
  // Other methods
  //

  protected RunningProcess createRunningProcess() {

    // Create the process builder
    final ProcessBuilder builder = new ProcessBuilder();

    // Define the command line
    final List<String> command = new ArrayList<>();
    command.add(this.executablePath);
    command.addAll(arguments());
    builder.command(command).directory(directory());

    // Define the environment variables
    builder.environment().clear();
    builder.environment().putAll(environment());

    // Set the temporary directory
    if (temporaryDirectory() != null && temporaryDirectory().isDirectory()) {
      builder.environment().put(TMP_DIR_ENV_VARIABLE,
          temporaryDirectory().getAbsolutePath());
    }

    // Define the stdout file
    if (stdOutFile() != null) {
      builder.redirectOutput(stdOutFile());
    }

    // Define the stderr file
    if (stdErrFile() != null) {
      builder.redirectOutput(stdErrFile());
    }

    return new AbstractRunningProcess() {

      private Process process;

      @Override
      protected int internalWaitFor() throws IOException {

        synchronized (this) {
          if (this.process == null) {
            this.process = builder.start();
          }
        }

        try {
          return this.process.waitFor();
        } catch (InterruptedException e) {
          throw new IOException(e);
        }
      }

      @Override
      public InputStream getInputStream() throws IOException {

        synchronized (this) {
          if (this.process == null) {
            this.process = builder.start();
          }
        }

        return this.process.getInputStream();
      }
    };

  }

  //
  // Constructor
  //

  /**
   * Protected constructor.
   */
  protected AbstractPathProcess() {

    // Create a temporary ProcessBuilder to get the default execution directory
    // and the environment variables
    final ProcessBuilder pb = new ProcessBuilder();

    directory(pb.directory());
    environment().putAll(pb.environment());
  }

  /**
   * Protected constructor
   * @param executablePath path the executable
   */
  protected AbstractPathProcess(final String executablePath) {

    this();
    setExecutablePath(executablePath);
  }

}
