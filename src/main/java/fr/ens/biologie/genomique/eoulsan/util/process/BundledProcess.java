package fr.ens.biologie.genomique.eoulsan.util.process;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.util.BinariesInstaller;

/**
 * This class define a process that will execute an executable bundled in
 * Eouslan jar.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class BundledProcess extends AbstractPathProcess {

  private static final String SYNC = AbstractPathProcess.class.getName();

  private final String softwarePackage;
  private final String executable;
  private final String version;
  private final File executablesTemporaryDirectory;

  private String executablePath;

  @Override
  protected ProcessCommand internalCreate() {

    return new ProcessCommand() {

      @Override
      public boolean isAvailable() {

        return BinariesInstaller.check(softwarePackage, version, executable);
      }

      @Override
      public boolean isInstalled() {

        return executablePath != null;
      }

      @Override
      public String install() throws IOException {

        if (executablePath != null) {
          return executablePath;
        }

        synchronized (SYNC) {

          executablePath = BinariesInstaller.install(softwarePackage, version,
              executable, executablesTemporaryDirectory.getAbsolutePath());
        }

        if (executablePath == null) {
          throw new IOException(
              "The bundled executable has not been installed: " + executable);
        }

        setExecutablePath(executablePath);

        return executablePath;
      }

      @Override
      public RunningProcess execute() throws IOException {

        if (getExecutablePath() == null) {
          install();
        }

        return createRunningProcess();
      }
    };
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param softwarePackage software package of the mapper
   * @param version version of the mapper
   * @param executablesTemporaryDirectory temporary directory for executables
   */
  public BundledProcess(final String softwarePackage, final String executable,
      final String version, final File executablesTemporaryDirectory) {

    super();

    checkNotNull(softwarePackage, "softwarePackage argument cannot be null");
    checkNotNull(executable, "executable argument cannot be null");
    checkNotNull(version, "version argument cannot be null");
    checkNotNull(executablesTemporaryDirectory,
        "executablesTemporaryDirectory argument cannot be null");

    this.softwarePackage = softwarePackage;
    this.executable = executable;
    this.version = version;
    this.executablesTemporaryDirectory = executablesTemporaryDirectory;
  }

}
