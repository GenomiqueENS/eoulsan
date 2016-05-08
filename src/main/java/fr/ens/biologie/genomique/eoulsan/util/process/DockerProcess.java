package fr.ens.biologie.genomique.eoulsan.util.process;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.LogsParameter;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.LogMessage;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.Image;

import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.util.docker.DockerManager;

/**
 * This class define a process using Docker.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DockerProcess extends ProcessCommandBuilder {

  private static final int SECOND_TO_WAIT_BEFORE_KILLING_CONTAINER = 10;

  private final DockerClient dockerClient;
  private final String dockerImage;
  private final String executable;

  @Override
  protected ProcessCommand internalCreate() {

    return new ProcessCommand() {

      @Override
      public boolean isAvailable() {

        // TODO Check if the image is available in repositories
        return isInstalled();
      }

      @Override
      public boolean isInstalled() {

        try {
          final List<Image> images = dockerClient.listImages();

          for (Image image : images) {
            for (String tag : image.repoTags()) {
              if (dockerImage.equals(tag)) {
                return true;
              }
            }
          }

          return false;

        } catch (DockerException | InterruptedException e) {
          return false;
        }
      }

      @Override
      public String install() throws IOException {

        try {

          // Pull image if needed
          getLogger().fine("Pull Docker image: " + dockerImage);
          dockerClient.pull(dockerImage);

        } catch (DockerException | InterruptedException e) {
          throw new IOException(e);
        }

        return executable;
      }

      @Override
      public RunningProcess execute() throws IOException {

        // Define the command line
        final List<String> commandLine = new ArrayList<>();
        commandLine.add(executable);
        commandLine.addAll(arguments());

        // Create container configuration
        getLogger()
            .fine("Configure container, command to execute: " + commandLine);

        final List<String> env = new ArrayList<>();

        for (Map.Entry<String, String> e : environment().entrySet()) {
          env.add(e.getKey() + '=' + e.getValue());
        }

        final ContainerConfig.Builder builder =
            ContainerConfig.builder().image(dockerImage).cmd(commandLine);

        // Set the working directory
        builder.workingDir(directory().getAbsolutePath());

        // Set the UID and GID of the docker process
        if (uid() >= 0 && gid() >= 0) {
          builder.user(uid() + ":" + gid());
        }

        // Define temporary directory
        final Set<File> toBind = new HashSet<>();
        if (temporaryDirectory().isDirectory()) {
          toBind.add(temporaryDirectory());
          env.add(TMP_DIR_ENV_VARIABLE
              + "=" + temporaryDirectory().getAbsolutePath());
        }

        // Define directories to mount
        toBind.addAll(mountDirectories());

        builder.hostConfig(createBinds(directory(), toBind));

        // Set environment variables
        builder.env(env);

        // Create container
        final ContainerCreation creation;
        try {
          creation = dockerClient.createContainer(builder.build());
        } catch (DockerException | InterruptedException e1) {
          throw new IOException(e1);
        }

        // Get container id
        final String containerId = creation.id();

        return new AbstractRunningProcess() {

          @Override
          protected int internalWaitFor() throws IOException {

            final int exitValue;
            try {

              // Start container
              getLogger().fine("Start of the Docker container: " + containerId);
              dockerClient.startContainer(containerId);

              // Redirect stdout and stderr
              final LogStream logStream =
                  dockerClient.logs(containerId, LogsParameter.FOLLOW,
                      LogsParameter.STDERR, LogsParameter.STDOUT);
              redirect(logStream, stdOutFile(), stdErrFile(), redirectStderr());

              // Wait the end of the container
              getLogger()
                  .fine("Wait the end of the Docker container: " + containerId);
              dockerClient.waitContainer(containerId);

              // Get process exit code
              final ContainerInfo info =
                  dockerClient.inspectContainer(containerId);
              exitValue = info.state().exitCode();
              getLogger().fine("Exit value: " + exitValue);

              // Stop container before removing it
              dockerClient.stopContainer(containerId,
                  SECOND_TO_WAIT_BEFORE_KILLING_CONTAINER);

            } catch (DockerException | InterruptedException e) {
              throw new IOException(e);
            }

            // Remove container
            getLogger().fine("Remove Docker container: " + containerId);
            try {
              dockerClient.removeContainer(containerId);
            } catch (DockerException | InterruptedException e) {
              EoulsanLogger.getLogger()
                  .severe("Unable to remove Docker container: " + containerId);
            }

            return exitValue;

          }

          @Override
          public InputStream getInputStream() throws IOException {

            final LogStream logStream;

            try {
              logStream = dockerClient.logs(containerId, LogsParameter.FOLLOW,
                  LogsParameter.STDOUT);
            } catch (DockerException | InterruptedException e) {
              throw new IOException(e);
            }

            return new InputStream() {

              private ByteBuffer buffer;
              private boolean end;

              private void updateBuffer() {

                for (LogMessage message; logStream.hasNext();) {

                  message = logStream.next();
                  switch (message.stream()) {

                  case STDOUT:
                    this.buffer = message.content();
                    return;

                  default:
                    break;
                  }
                }

                this.end = true;
              }

              @Override
              public int read() throws IOException {

                // Test if buffer must be updated
                if (this.buffer == null || this.buffer.remaining() == 0) {

                  // Update the buffer
                  updateBuffer();

                  // Test if this is the end of the buffer
                  if (this.end) {
                    return -1;
                  }
                }

                return this.buffer.get();
              }
            };
          }

        };
      }

    };
  }

  //
  // Docker methods
  //

  /**
   * Create Docker binds.
   * @param executionDirectory execution directory
   * @param files files to binds
   * @return an HostConfig object
   */
  private static HostConfig createBinds(final File executionDirectory,
      Set<File> files) {

    final HostConfig.Builder builder = HostConfig.builder();
    final Set<String> binds = new HashSet<>();

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
  // Constructors
  //

  /**
   * Constructor.
   * @param dockerImage Docker image
   * @param temporaryDirectory temporary directory
   * @param executable executable to execute
   */
  public DockerProcess(final String dockerImage, final String executable) {

    this(DockerManager.getInstance().getClient(), dockerImage, executable);
  }

  /**
   * Constructor.
   * @param dockerClient Docker connection URI
   * @param dockerImage Docker image
   * @param executable executable to execute
   */
  public DockerProcess(final DockerClient dockerClient,
      final String dockerImage, final String executable) {

    checkNotNull(dockerClient, "dockerClient argument cannot be null");
    checkNotNull(dockerImage, "dockerImage argument cannot be null");
    checkNotNull(executable, "executable argument cannot be null");

    this.dockerClient = dockerClient;
    this.dockerImage = dockerImage;
    this.executable = executable;
  }

}
