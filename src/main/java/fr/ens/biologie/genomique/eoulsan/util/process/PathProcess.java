package fr.ens.biologie.genomique.eoulsan.util.process;

import java.io.File;
import java.io.IOException;

/**
 * This class allow to execute a command from its full path.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class PathProcess extends AbstractPathProcess {

  @Override
  protected ProcessCommand internalCreate() {

    return new ProcessCommand() {

      @Override
      public boolean isAvailable() {

        return isExecutable(getExecutablePath());
      }

      @Override
      public boolean isInstalled() {

        return isAvailable();
      }

      @Override
      public String install() throws IOException {

        return getExecutablePath();
      }

      @Override
      public RunningProcess execute() {

        return createRunningProcess();
      }
    };

  }

  //
  // Other methods
  //

  private static boolean isExecutable(final String path) {

    if (path == null) {
      return false;
    }

    return isExecutable(new File(path));
  }

  private static boolean isExecutable(final File file) {

    return file.isFile() && file.canExecute();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param executablePath the executable path
   */
  public PathProcess(final String executablePath) {
    super();

    if (executablePath == null) {
      throw new NullPointerException(
          "The executablePath argument cannot be null");
    }

    if (!isExecutable(executablePath)) {
      throw new NullPointerException(
          "The executablePath does not exists or is not an executable: "
              + executablePath);
    }

    super.setExecutablePath(executablePath);
  }

  /**
   * Constructor.
   * @param executableFile the executable file
   */
  public PathProcess(final File executableFile) {

    super();

    if (executableFile == null) {
      throw new NullPointerException(
          "The executablePath argument cannot be null");
    }

    if (!isExecutable(executableFile)) {
      throw new NullPointerException(
          "The executablePath does not exists or is not an executable: "
              + executableFile);
    }

    super.setExecutablePath(executableFile);
  }

}
