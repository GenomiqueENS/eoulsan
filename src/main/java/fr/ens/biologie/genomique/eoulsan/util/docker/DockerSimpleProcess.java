package fr.ens.biologie.genomique.eoulsan.util.docker;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static java.util.Collections.singletonList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Objects;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.LogsParam;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.LogMessage;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.Image;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.util.AbstractSimpleProcess;
import fr.ens.biologie.genomique.eoulsan.util.SystemUtils;

/**
 * This class define how to execute a process using Docker.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DockerSimpleProcess extends AbstractSimpleProcess {

  private static final int SECOND_TO_WAIT_BEFORE_KILLING_CONTAINER = 10;

  private final DockerClient dockerClient;
  private final String dockerImage;
  private final int userUid;
  private final int userGid;
  private final int requiredProcessors;
  private final int requiredMemory;

  @Override
  public int execute(final List<String> commandLine,
      final File executionDirectory,
      final Map<String, String> environmentVariables,
      final File temporaryDirectory, final File stdoutFile,
      final File stderrFile, final boolean redirectErrorStream)
      throws EoulsanException {

    checkNotNull(commandLine, "commandLine argument cannot be null");
    checkNotNull(executionDirectory,
        "executionDirectory argument cannot be null");
    checkNotNull(stdoutFile, "stdoutFile argument cannot be null");
    checkNotNull(stderrFile, "stderrFile argument cannot be null");

    checkArgument(executionDirectory.isDirectory(),
        "execution directory does not exists or is not a directory: "
            + executionDirectory.getAbsolutePath());

    try {

      final List<String> env = new ArrayList<>();

      if (environmentVariables != null) {
        for (Map.Entry<String, String> e : environmentVariables.entrySet()) {
          env.add(e.getKey() + '=' + e.getValue());
        }
      }

      // Pull image if needed
      pullImageIfNotExists(this.dockerClient, this.dockerImage);

      // Create container configuration
      getLogger()
          .fine("Configure container, command to execute: " + commandLine);

      final ContainerConfig.Builder builder =
          ContainerConfig.builder().image(dockerImage).cmd(commandLine);

      // Set the working directory
      builder.workingDir(executionDirectory.getAbsolutePath());

      // Set the UID and GID of the docker process
      if (this.userUid >= 0 && this.userGid >= 0) {
        builder.user(this.userUid + ":" + this.userGid);
      }

      // Define temporary directory
      final List<File> toBind;
      if (temporaryDirectory.isDirectory()) {
        toBind = singletonList(temporaryDirectory);
        env.add(
            TMP_DIR_ENV_VARIABLE + "=" + temporaryDirectory.getAbsolutePath());
      } else {
        toBind = Collections.emptyList();
      }

      // Configure host
      final HostConfig.Builder hostBuilder = HostConfig.builder();
      setHostRequirements(hostBuilder, this.requiredProcessors,
          this.requiredMemory);
      setHostBinds(hostBuilder, executionDirectory, toBind);

      // Create host configuration
      builder.hostConfig(hostBuilder.build());

      // Set environment variables
      builder.env(env);

      // Create container
      final ContainerCreation creation =
          this.dockerClient.createContainer(builder.build());

      // Get container id
      final String containerId = creation.id();

      // Start container
      getLogger().fine("Start of the Docker container: " + containerId);
      this.dockerClient.startContainer(containerId);

      // Redirect stdout and stderr
      final LogStream logStream =
          this.dockerClient.logs(containerId, LogsParam.follow(true),
              LogsParam.stderr(true), LogsParam.stdout(true));
      redirect(logStream, stdoutFile, stderrFile, redirectErrorStream);

      // Wait the end of the container
      getLogger().fine("Wait the end of the Docker container: " + containerId);
      this.dockerClient.waitContainer(containerId);

      // Get process exit code
      final ContainerInfo info =
          this.dockerClient.inspectContainer(containerId);
      final int exitValue = info.state().exitCode();
      getLogger().fine("Exit value: " + exitValue);

      // Stop container before removing it
      this.dockerClient.stopContainer(containerId,
          SECOND_TO_WAIT_BEFORE_KILLING_CONTAINER);

      // Remove container
      // getLogger().fine("Remove Docker container: " + containerId);
      // try {
      // this.dockerClient.removeContainer(containerId);
      // } catch (DockerException | InterruptedException e) {
      // EoulsanLogger.getLogger()
      // .severe("Unable to remove Docker container: " + containerId);
      // }

      return exitValue;
    } catch (DockerException | InterruptedException e) {
      throw new EoulsanException(e);
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
   * Set Host binds.
   * @param executionDirectory execution directory
   * @param files files to binds
   */
  private static void setHostBinds(final HostConfig.Builder builder,
      final File executionDirectory, final List<File> files) {

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
  }

  /**
   * Set Host requirements.
   * @param builder
   * @param requiredProcessors
   * @param requiredMemory
   * @param requiredProcessors required processors
   * @param requiredMemory required memory
   */
  private static void setHostRequirements(final HostConfig.Builder builder,
      final int requiredProcessors, final int requiredMemory) {

    if (requiredProcessors > 0 && requiredProcessors <= 1024) {

      long shares = 1024
          / Runtime.getRuntime().availableProcessors() * requiredProcessors;
      builder.cpuShares(shares);
    }

    if (requiredMemory > 0) {
      builder.memory((long) requiredMemory * 1024 * 1024);
    }
  }

  /**
   * Redirect the outputs of the container to files.
   * @param logStream the log stream
   * @param stdout stdout output file
   * @param stderr stderr output file
   * @param redirectErrorStream redirect stderr in stdout
   */
  private static void redirect(final LogStream logStream, final File stdout,
      final File stderr, final boolean redirectErrorStream) {

    final Runnable r;

    if (redirectErrorStream) {

      r = new Runnable() {

        @Override
        public void run() {

          try (WritableByteChannel stdoutChannel =
              Channels.newChannel(new FileOutputStream(stderr))) {

            for (LogMessage message; logStream.hasNext();) {

              message = logStream.next();
              switch (message.stream()) {

              case STDOUT:
              case STDERR:
                stdoutChannel.write(message.content());
                break;

              case STDIN:
              default:
                break;
              }
            }
          } catch (IOException e) {
            EoulsanLogger.getLogger().severe(e.getMessage());
          }
        }
      };

    } else {

      r = new Runnable() {

        @Override
        public void run() {

          try (
              WritableByteChannel stdoutChannel =
                  Channels.newChannel(new FileOutputStream(stdout));
              WritableByteChannel stderrChannel =
                  Channels.newChannel(new FileOutputStream(stderr))) {

            for (LogMessage message; logStream.hasNext();) {

              message = logStream.next();
              switch (message.stream()) {

              case STDOUT:
                stdoutChannel.write(message.content());
                break;

              case STDERR:
                stderrChannel.write(message.content());
                break;

              case STDIN:
              default:
                break;
              }
            }
          } catch (IOException e) {
            EoulsanLogger.getLogger().severe(e.getMessage());
          }
        }
      };
    }

    new Thread(r).start();
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("dockerImage", dockerImage)
        .toString();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param dockerImage Docker image
   * @param temporaryDirectory temporary directory
   */
  public DockerSimpleProcess(final String dockerImage) {

    this(DockerManager.getInstance().getClient(), dockerImage, -1, -1);
  }

  /**
   * Constructor.
   * @param dockerClient Docker connection URI
   * @param dockerImage Docker image
   * @param temporaryDirectory temporary directory
   */
  public DockerSimpleProcess(final DockerClient dockerClient,
      final String dockerImage) {

    this(dockerClient, dockerImage, -1, -1);
  }

  /**
   * Constructor.
   * @param dockerClient Docker connection URI
   * @param dockerImage Docker image
   * @param temporaryDirectory temporary directory
   * @param requiredProcessors required processors
   * @param requiredMemory required memory
   */
  public DockerSimpleProcess(final String dockerImage,
      final int requiredProcessors, final int requiredMemory) {

    this(DockerManager.getInstance().getClient(), dockerImage,
        requiredProcessors, requiredMemory);
  }

  /**
   * Constructor.
   * @param dockerClient Docker connection URI
   * @param dockerImage Docker image
   * @param temporaryDirectory temporary directory
   * @param requiredProcessors required processors
   * @param requiredMemory required memory
   */
  public DockerSimpleProcess(final DockerClient dockerClient,
      final String dockerImage, final int requiredProcessors,
      final int requiredMemory) {

    checkNotNull(dockerClient, "dockerClient argument cannot be null");
    checkNotNull(dockerImage, "dockerImage argument cannot be null");

    this.dockerClient = dockerClient;
    this.dockerImage = dockerImage;
    this.userUid = SystemUtils.uid();
    this.userGid = SystemUtils.gid();
    this.requiredProcessors = requiredProcessors;
    this.requiredMemory = requiredMemory;
  }

}
