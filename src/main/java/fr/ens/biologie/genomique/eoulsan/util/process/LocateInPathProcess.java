package fr.ens.biologie.genomique.eoulsan.util.process;

import java.io.File;
import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.util.SystemUtils;

/**
 * This class allow to execute a command in the PATH.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class LocateInPathProcess extends PathProcess {

  /**
   * Search the executable path in the PATH.
   * @param executable the executable name
   * @return the file of the executable if exists
   * @throws IOException if the executable cannot be found in PATH
   */
  private static File locateExecutable(final String executable)
      throws IOException {

    if (executable == null) {
      throw new NullPointerException("executable argument cannot be null");
    }

    File file = SystemUtils.searchExecutableInPATH(executable);

    if (file == null) {
      throw new IOException(
          "Cannot find the executable in PATH: " + executable);
    }

    return file;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param executable the executable to find in PATH
   * @throws IOException if the executable cannot be found in PATH
   */
  public LocateInPathProcess(final String executable) throws IOException {

    super(locateExecutable(executable));
  }

}
