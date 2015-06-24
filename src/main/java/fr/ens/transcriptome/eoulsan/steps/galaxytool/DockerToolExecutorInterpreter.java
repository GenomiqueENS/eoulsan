package fr.ens.transcriptome.eoulsan.steps.galaxytool;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static java.util.Collections.singletonList;
import static org.python.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Objects;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.Image;

import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define a Docker executor interpreter.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DockerToolExecutorInterpreter implements ToolExecutorInterpreter {

  private final URI dockerConnection;
  private final String dockerImage;
  private final int userUid;
  private final int userGid;
  private final File temporaryDirectory;

  @Override
  public String getName() {

    return "docker";
  }

  @Override
  public List<String> createCommandLine(final List<String> arguments) {

    checkNotNull(arguments, "arguments argument cannot be null");

    return arguments;
  }

  @Override
  public ToolExecutorResult execute(final List<String> commandLine,
      final File executionDirectory, final File stdoutFile, File stderrFile) {

    checkNotNull(commandLine, "commandLine argument cannot be null");
    checkNotNull(executionDirectory,
        "executionDirectory argument cannot be null");
    checkNotNull(stdoutFile, "stdoutFile argument cannot be null");
    checkNotNull(stderrFile, "stderrFile argument cannot be null");

    checkArgument(executionDirectory.isDirectory(),
        "execution directory does not exists or is not a directory: "
            + executionDirectory.getAbsolutePath());

    try (DockerClient dockerClient =
        new DefaultDockerClient(this.dockerConnection)) {

      // Pull image if needed
      pullImageIfNotExists(dockerClient, this.dockerImage);

      // Create container configuration
      getLogger().fine(
          "Configure container, command to execute: " + commandLine);

      final ContainerConfig.Builder builder =
          ContainerConfig.builder().image(dockerImage)
              .cmd(convertCommand(commandLine, stdoutFile, stderrFile));

      // Set the working directory
      builder.workingDir(executionDirectory.getAbsolutePath());

      // Set the UID and GID of the docker process
      if (userUid >= 0 && userGid >= 0) {
        builder.user(userUid + ":" + userGid);
      }

      // Define binds
      final HostConfig hostConfig =
          createBinds(executionDirectory, singletonList(executionDirectory));

      // Create container
      final ContainerCreation creation =
          dockerClient.createContainer(builder.build());

      // Get container id
      String containerId = creation.id();

      // Start container
      getLogger().fine("Start of the Docker container: " + containerId);
      dockerClient.startContainer(containerId, hostConfig);

      // Wait the end of the container
      getLogger().fine("Wait the end of the Docker container: " + containerId);
      dockerClient.waitContainer(containerId);

      // Get process exit code
      final ContainerInfo info = dockerClient.inspectContainer(containerId);
      final int exitValue = info.state().exitCode();

      // Remove container
      getLogger().fine("Remove Docker container: " + containerId);
      dockerClient.removeContainer(containerId);

      return new ToolExecutorResult(commandLine, exitValue);

    } catch (DockerException | InterruptedException e) {
      return new ToolExecutorResult(commandLine, e);
    }
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
      final String dockerImageName) throws DockerException,
      InterruptedException {

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
   * @return a converted command
   */
  private List<String> convertCommand(final List<String> command,
      final File stdout, final File stderr) {

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

    sb.append(" > ");
    sb.append(stdout.getAbsolutePath());

    sb.append(" 2> ");
    sb.append(stderr.getAbsolutePath());

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
  public DockerToolExecutorInterpreter(final URI dockerConnection,
      final String dockerImage, final File temporaryDirectory) {

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
