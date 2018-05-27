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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Objects;

import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;
import fr.ens.biologie.genomique.eoulsan.util.process.DockerImageInstance;
import fr.ens.biologie.genomique.eoulsan.util.process.DockerManager;
import fr.ens.biologie.genomique.eoulsan.util.process.SimpleProcess.AdvancedProcess;

/**
 * This class define a mapper executor that executes process in Docker
 * containers.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DockerMapperExecutor implements MapperExecutor {

  private final DockerImageInstance dockerConnection;
  private final File temporaryDirectory;

  /**
   * This class define an executor result.
   * @author Laurent Jourdren
   */
  private class DockerResult implements Result {

    // private final String containerId;
    private final File stdoutFile;
    private Integer waitForResult;
    private AdvancedProcess process;

    @Override
    public InputStream getInputStream() throws IOException {

      if (this.stdoutFile == null) {
        throw new IOException(
            "The excution command has not been configured to redirect stdout");
      }

      try {

        @SuppressWarnings("resource")
        final FileInputStream fis = new FileInputStream(stdoutFile);

        return Channels.newInputStream(fis.getChannel());
      } catch (FileNotFoundException e) {
        getLogger().severe("Cannot find stdout named pipe of the container: "
            + this.stdoutFile);
        return null;
      }
    }

    @Override
    public int waitFor() throws IOException {

      // Reuse previous result if waitFor() has been already called
      if (this.waitForResult != null) {
        return this.waitForResult;
      }

      int result;

      // Wait the end of the container
      getLogger().fine("Wait the end of the Docker container");

      result = process.waitFor();

      // Remove named pipe
      if (this.stdoutFile != null) {
        if (!this.stdoutFile.delete()) {
          getLogger()
              .warning("Unable to delete stdout file: " + this.stdoutFile);
        }
      }

      this.waitForResult = result;

      return result;
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param command command to execute
     * @param executionDirectory execution directory
     * @param stdout true if stdout will be read
     * @param stdErrFile standard error file
     * @param redirectStderr redirect stderr to stdout
     * @param filesUsed files used by the process
     * @throws IOException if an error occurs while creating the object
     */
    private DockerResult(final List<String> command,
        final File executionDirectory, boolean stdout, final File stdErrFile,
        final boolean redirectStderr, File... filesUsed) throws IOException {

      checkNotNull(command, "command argument cannot be null");

      // Pull image if needed
      dockerConnection.pullImageIfNotExists();

      // Create container configuration
      getLogger().fine("Configure container, command to execute: " + command);

      List<File> newFilesUsed = new ArrayList<>();
      if (filesUsed != null) {
        Collections.addAll(newFilesUsed, filesUsed);
      }

      if (stdout) {
        final String uuid = UUID.randomUUID().toString();
        this.stdoutFile = new File(temporaryDirectory, "stdout-" + uuid);
        FileUtils.createNamedPipe(this.stdoutFile);
        newFilesUsed.add(this.stdoutFile);

      } else {
        this.stdoutFile = null;
      }

      if (stdErrFile != null) {
        newFilesUsed.add(stdErrFile);
      }

      // Start container
      getLogger().fine("Start of the Docker container");
      // dockerClient.startContainer(containerId);

      final File nullFile = new File("/dev/null");
      final File finalErrFile = stdErrFile == null ? nullFile : stdErrFile;
      final File[] files = newFilesUsed.toArray(new File[newFilesUsed.size()]);

      this.process = dockerConnection.start(
          convertCommand(command, this.stdoutFile, redirectStderr),
          executionDirectory, null, null, nullFile, finalErrFile, false, files);
    }
  }

  //
  // MapperExecutor methods
  //

  @Override
  public boolean isExecutable(final String executable) throws IOException {

    checkNotNull(executable, "binaryFilename argument cannot be null");
    checkArgument(!executable.isEmpty(),
        "binaryFilename argument cannot be empty");

    final List<String> command = newArrayList("which", executable);

    final Result result =
        execute(command, null, false, null, false, (File[]) null);

    return result.waitFor() == 0;
  }

  @Override
  public String install(final String executable) throws IOException {

    checkNotNull(executable, "executable argument cannot be null");

    return executable;
  }

  @Override
  public Result execute(final List<String> command,
      final File executionDirectory, final boolean stdout,
      final File stdErrFile, final boolean redirectStderr,
      final File... filesUsed) throws IOException {

    checkNotNull(command, "executable argument cannot be null");

    return new DockerResult(command, executionDirectory, stdout, stdErrFile,
        redirectStderr, filesUsed);
  }

  //
  // Docker methods
  //

  /**
   * Convert command to sh command if needed.
   * @param command the command to convert
   * @param stdout the stdout file to use
   * @param redirectStderr redirect stderr to stdout
   * @return a converted command
   */
  private List<String> convertCommand(final List<String> command,
      final File stdout, final boolean redirectStderr) {

    checkNotNull(command, "command argument cannot be null");

    if (stdout == null) {
      return command;
    }

    List<String> result = new ArrayList<>();
    result.add("sh");
    result.add("-c");

    final StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (String c : command) {

      if (first) {
        sb.append(' ');
      }
      sb.append(StringUtils.bashEscaping(c));
    }

    sb.append(redirectStderr ? " &> " : " > ");
    sb.append(stdout.getAbsolutePath());

    result.add(sb.toString());

    return result;
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return Objects.toStringHelper(this)
        .add("dockerConnection", dockerConnection)
        .add("temporaryDirectory", temporaryDirectory).toString();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param dockerImage Docker image
   * @param temporaryDirectory temporary directory
   * @throws IOException if an error occurs while creating the connection
   */
  DockerMapperExecutor(final String dockerImage, final File temporaryDirectory)
      throws IOException {

    checkNotNull(dockerImage, "dockerImage argument cannot be null");
    checkNotNull(temporaryDirectory,
        "temporaryDirectory argument cannot be null");

    this.temporaryDirectory = temporaryDirectory;
    this.dockerConnection =
        DockerManager.getInstance().createImageInstance(dockerImage);
  }

}
