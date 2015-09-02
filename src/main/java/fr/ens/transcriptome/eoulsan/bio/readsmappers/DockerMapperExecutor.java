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
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.bio.readsmappers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Objects;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.Image;

import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define a mapper executor that executes process in Docker
 * containers.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DockerMapperExecutor implements MapperExecutor {

  private final URI dockerConnection;
  private final String dockerImage;
  private final int userUid;
  private final int userGid;
  private final File temporaryDirectory;

  /**
   * This class define an executor result.
   * @author Laurent Jourdren
   */
  private class DockerResult implements Result {

    private final String containerId;
    private final File stdoutFile;
    private Integer waitForResult;

    @Override
    public InputStream getInputStream() throws IOException {

      if (this.stdoutFile == null) {
        throw new IOException(
            "The excution command has not been configured to redirect stdout");
      }

      try (DockerClient dockerClient =
          new DefaultDockerClient(dockerConnection)) {

        // Get process exit code
        final ContainerInfo info = dockerClient.inspectContainer(containerId);

        if (info.state().pid() == 0) {
          throw new IOException(
              "Error while executing container, container pid is 0");
        }

      } catch (DockerException e) {
        throw new IOException(e);
      } catch (InterruptedException e) {
        throw new IOException(e);
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

      try (DockerClient dockerClient =
          new DefaultDockerClient(dockerConnection)) {

        // Wait the end of the container
        getLogger()
            .fine("Wait the end of the Docker container: " + containerId);
        dockerClient.waitContainer(containerId);

        // Get process exit code
        final ContainerInfo info = dockerClient.inspectContainer(containerId);
        result = info.state().exitCode();

        // Remove container
        getLogger().fine("Remove Docker container: " + containerId);
        dockerClient.removeContainer(containerId);

        // Remove named pipe
        if (this.stdoutFile != null) {
          this.stdoutFile.delete();
        }

      } catch (DockerException e) {
        throw new IOException(e);
      } catch (InterruptedException e) {
        throw new IOException(e);
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
     * @param redirectStderr redirect stderr to stdout
     * @param filesUsed files used by the process
     * @throws IOException if an error occurs while creating the object
     */
    private DockerResult(final List<String> command,
        final File executionDirectory, boolean stdout,
        final boolean redirectStderr, File... filesUsed) throws IOException {

      checkNotNull(command, "command argument cannot be null");

      try (DockerClient dockerClient =
          new DefaultDockerClient(dockerConnection)) {

        // Pull image if needed
        pullImageIfNotExists(dockerClient, dockerImage);

        // Create container configuration
        getLogger().fine("Configure container, command to execute: " + command);

        List<File> newFilesUsed = new ArrayList<>();
        if (filesUsed != null) {
          for (File f : filesUsed) {
            newFilesUsed.add(f);
          }
        }

        if (stdout) {
          final String uuid = UUID.randomUUID().toString();
          this.stdoutFile = new File(temporaryDirectory, "stdout-" + uuid);
          FileUtils.createNamedPipe(this.stdoutFile);
          newFilesUsed.add(this.stdoutFile);

        } else {
          this.stdoutFile = null;
        }

        final ContainerConfig.Builder builder =
            ContainerConfig.builder().image(dockerImage)
                .cmd(convertCommand(command, this.stdoutFile, redirectStderr));

        // Set the working directory
        if (executionDirectory != null) {
          builder.workingDir(executionDirectory.getAbsolutePath());
          newFilesUsed.add(executionDirectory);
        }

        // Set the UID and GID of the docker process
        if (userUid >= 0 && userGid >= 0) {
          builder.user(userUid + ":" + userGid);
        }

        // Define binds
        final HostConfig hostConfig =
            createBinds(executionDirectory, newFilesUsed);

        // Create container
        final ContainerCreation creation =
            dockerClient.createContainer(builder.build());

        // Get container id
        this.containerId = creation.id();

        // Start container
        getLogger().fine("Start of the Docker container: " + containerId);
        dockerClient.startContainer(containerId, hostConfig);

      } catch (DockerException e) {
        throw new IOException(e);
      } catch (InterruptedException e) {
        throw new IOException(e);
      }
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

    final Result result = execute(command, null, false, false, (File[]) null);

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
      final boolean redirectStderr, final File... filesUsed)
          throws IOException {

    checkNotNull(command, "executable argument cannot be null");

    return new DockerResult(command, executionDirectory, stdout, redirectStderr,
        filesUsed);
  }

  //
  // Docker methods
  //

  /**
   * Pull a Docker image if not exists.
   * @param dockerClient the Docker client
   * @param dockerImageName the Docker image
   * @throws DockerException if an error occurs while pulling the Docker image
   * @throws InterruptedException if an error occurs while pulling the Docker
   *           image
   */
  private static void pullImageIfNotExists(DockerClient dockerClient,
      final String dockerImageName)
          throws DockerException, InterruptedException {

    checkNotNull(dockerClient, "dockerClient argument cannot be null");
    checkNotNull(dockerImageName, "dockerImageName argument cannot be null");

    List<Image> images = dockerClient.listImages();

    for (Image image : images) {
      for (String tag : image.repoTags()) {
        if (dockerImageName.equals(tag)) {
          return;
        }
      }
    }

    getLogger().fine("Pull Docker image: " + dockerImageName);
    dockerClient.pull(dockerImageName);
  }

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

  /**
   * Create Docker binds.
   * @param executionDirectory execution directory
   * @param files files to binds
   * @return an HostConfig object
   */
  private static HostConfig createBinds(final File executionDirectory,
      List<File> files) {

    HostConfig.Builder builder = HostConfig.builder();
    Set<String> binds = new HashSet<>();

    if (executionDirectory != null) {

      binds.add(executionDirectory.getAbsolutePath()
          + ':' + executionDirectory.getAbsolutePath());
    }

    if (files != null) {
      for (File f : files) {

        if (f.exists()) {
          binds.add(f.getAbsolutePath() + ':' + f.getAbsolutePath());
        }
      }
    }

    builder.binds(new ArrayList<>(binds));

    return builder.build();
  }

  //
  // Get user UID and GID
  //

  /**
   * Get user UID.
   * @return the user UID or -1 if UID cannot be found
   */
  private static final int uid() {

    try {
      return Integer.parseInt(ProcessUtils.execToString("id -u"));
    } catch (NumberFormatException | IOException e) {
      return -1;
    }
  }

  /**
   * Get user GID.
   * @return the user GID or -1 if GID cannot be found
   */
  private static final int gid() {

    try {
      return Integer.parseInt(ProcessUtils.execToString("id -u"));
    } catch (NumberFormatException | IOException e) {
      return -1;
    }
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return Objects.toStringHelper(this)
        .add("dockerConnection", dockerConnection)
        .add("dockerImage", dockerImage)
        .add("temporaryDirectory", temporaryDirectory).toString();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param dockerConnection Docker connection URI
   * @param dockerImage Docker image
   * @param temporaryDirectory temporary directory
   */
  DockerMapperExecutor(final URI dockerConnection, final String dockerImage,
      final File temporaryDirectory) {

    checkNotNull(dockerConnection, "dockerConnection argument cannot be null");
    checkNotNull(dockerImage, "dockerImage argument cannot be null");
    checkNotNull(temporaryDirectory,
        "temporaryDirectory argument cannot be null");

    this.dockerConnection = dockerConnection;
    this.dockerImage = dockerImage;
    this.temporaryDirectory = temporaryDirectory;
    this.userUid = uid();
    this.userGid = gid();
  }

}
