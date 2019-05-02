/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.bio.readsmappers;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;

import fr.ens.biologie.genomique.eoulsan.util.BinariesInstaller;

/**
 * This class define a mapper executor that executes process bundled in Eoulsan
 * jar file.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class BundledMapperExecutor implements MapperExecutor {

  private final String softwarePackage;
  private final String version;
  private final File executablesTemporaryDirectory;

  /**
   * This class define an executor result for BundledMapperExecutor and
   * PathMapperExecutor.
   * @author Laurent Jourdren
   */
  static class ProcessResult implements Result {

    private final Process process;

    @Override
    public InputStream getInputStream() {

      return process.getInputStream();
    }

    @Override
    public int waitFor() throws IOException {

      try {
        return process.waitFor();
      } catch (InterruptedException e) {
        throw new IOException(e);
      }
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param process Java Process object
     */
    ProcessResult(final Process process) {

      requireNonNull(process, "process argument cannot be null");

      this.process = process;
    }
  }

  @Override
  public String install(final String executable) throws IOException {

    requireNonNull(executable, "executable argument cannot be null");

    // Check if temporary directory for executables exists
    if (!this.executablesTemporaryDirectory.isDirectory()) {
      throw new IOException(
          "The temporary directory for executables does not exists or "
              + "is not a directory: "
              + this.executablesTemporaryDirectory.getAbsolutePath());
    }

    // Define the lock file
    File lockFile =
        new File(this.executablesTemporaryDirectory, executable + ".lock");

    String result;

    // Install binary
    try (RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
        FileChannel channel = raf.getChannel()) {

      // Lock
      FileLock lock = channel.lock();

      result = BinariesInstaller.install(this.softwarePackage, this.version,
          executable, this.executablesTemporaryDirectory.getAbsolutePath());

      // Unlock
      lock.release();
    }

    // Remove lock file if not used
    try (RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
        FileChannel channel = raf.getChannel()) {

      // Lock
      FileLock lock = channel.tryLock();

      // Remove lock file if it was unused
      if (lock != null) {
        lockFile.delete();

        // Unlock
        lock.release();
      }
    }

    return result;
  }

  @Override
  public boolean isExecutable(final String executable) {

    requireNonNull(executable, "executable argument cannot be null");

    return BinariesInstaller.check(this.softwarePackage, this.version,
        executable);
  }

  @Override
  public Result execute(final List<String> command,
      final File executionDirectory, final boolean stdout,
      final File stdErrFile, final boolean redirectStderr,
      final File... fileUsed) throws IOException {

    requireNonNull(command, "command argument cannot be null");

    final ProcessBuilder builder = new ProcessBuilder(command);
    builder.redirectErrorStream(redirectStderr);

    // Define the redirection of standard error
    if (stdErrFile != null) {
      builder.redirectError(stdErrFile);
    } else if (!redirectStderr) {
      builder.redirectError(new File("/dev/null"));
    }

    // Set execution directory if exists
    if (executionDirectory != null) {
      builder.directory(executionDirectory);
    }

    getLogger()
        .info("Process command: " + Joiner.on(' ').join(builder.command()));
    getLogger().info("Process directory: " + builder.directory());
    getLogger().fine("Process redirect output: " + builder.redirectOutput());
    getLogger().fine("Process redirect error: " + builder.redirectError());

    return new ProcessResult(builder.start());
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("softwarePackage", softwarePackage)
        .add("version", version)
        .add("executablesTemporaryDirectory", executablesTemporaryDirectory)
        .toString();
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
  BundledMapperExecutor(final String softwarePackage, final String version,
      final File executablesTemporaryDirectory) {

    requireNonNull(softwarePackage, "dockerConnection argument cannot be null");
    requireNonNull(version, "dockerConnection argument cannot be null");
    requireNonNull(executablesTemporaryDirectory,
        "dockerConnection argument cannot be null");

    this.softwarePackage = softwarePackage;
    this.version = version;
    this.executablesTemporaryDirectory = executablesTemporaryDirectory;
  }

}
