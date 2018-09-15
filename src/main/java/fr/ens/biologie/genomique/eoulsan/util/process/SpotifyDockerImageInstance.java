package fr.ens.biologie.genomique.eoulsan.util.process;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.util.CollectionUtils.nullToEmpty;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Objects;
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
import com.spotify.docker.client.messages.ProgressDetail;
import com.spotify.docker.client.messages.ProgressMessage;

import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.util.SystemUtils;

/**
 * This class define a Docker image instance using the Spotify Docker client
 * library.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class SpotifyDockerImageInstance extends AbstractSimpleProcess
    implements DockerImageInstance {

  private static final int SECOND_TO_WAIT_BEFORE_KILLING_CONTAINER = 10;

  private final DockerClient dockerClient;
  private final String dockerImage;
  private final int userUid;
  private final int userGid;
  private final boolean convertNFSFilesToMountRoots;

  @Override
  public AdvancedProcess start(List<String> commandLine,
      File executionDirectory, Map<String, String> environmentVariables,
      File temporaryDirectory, File stdoutFile, File stderrFile,
      boolean redirectErrorStream, File... filesUsed) throws IOException {

    requireNonNull(commandLine, "commandLine argument cannot be null");
    requireNonNull(stdoutFile, "stdoutFile argument cannot be null");
    requireNonNull(stderrFile, "stderrFile argument cannot be null");

    EoulsanLogger.getLogger().fine(getClass().getSimpleName()
        + ": commandLine=" + commandLine + ", executionDirectory="
        + executionDirectory + ", environmentVariables=" + environmentVariables
        + ", temporaryDirectory=" + temporaryDirectory + ", stdoutFile="
        + stdoutFile + ", stderrFile=" + stderrFile + ", redirectErrorStream="
        + redirectErrorStream + ", filesUsed" + Arrays.toString(filesUsed));

    if (executionDirectory != null) {
      checkArgument(executionDirectory.isDirectory(),
          "execution directory does not exists or is not a directory: "
              + executionDirectory.getAbsolutePath());
    }

    try {

      final List<String> env = new ArrayList<>();

      if (environmentVariables != null) {
        for (Map.Entry<String, String> e : environmentVariables.entrySet()) {
          env.add(e.getKey() + '=' + e.getValue());
        }
      }

      // Pull image if needed
      pullImageIfNotExists();

      // Create container configuration
      getLogger()
          .fine("Configure container, command to execute: " + commandLine);

      final ContainerConfig.Builder builder =
          ContainerConfig.builder().image(dockerImage).cmd(commandLine);

      // Set the working directory
      if (executionDirectory != null) {
        builder.workingDir(executionDirectory.getAbsolutePath());
      }

      // Set the UID and GID of the docker process
      if (this.userUid >= 0 && this.userGid >= 0) {
        builder.user(this.userUid + ":" + this.userGid);
      }

      // File/directories to mount
      final List<File> toBind = new ArrayList<>();
      if (filesUsed != null) {
        toBind.addAll(Arrays.asList(filesUsed));
      }

      // Define temporary directory
      if (temporaryDirectory != null && temporaryDirectory.isDirectory()) {
        toBind.add(temporaryDirectory);
        env.add(
            TMP_DIR_ENV_VARIABLE + "=" + temporaryDirectory.getAbsolutePath());
      }

      builder.hostConfig(createBinds(executionDirectory, toBind,
          this.convertNFSFilesToMountRoots));

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
      final LogStream logStream = this.dockerClient.logs(containerId,
          LogsParameter.FOLLOW, LogsParameter.STDERR, LogsParameter.STDOUT);
      redirect(logStream, stdoutFile, stderrFile, redirectErrorStream);

      // Get process exit code
      final ContainerInfo info = dockerClient.inspectContainer(containerId);

      if (info.state().pid() == 0) {
        throw new IOException(
            "Error while executing container, container pid is 0");
      }

      return new AdvancedProcess() {

        @Override
        public int waitFor() throws IOException {

          int exitValue;

          try {

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

            // Remove container
            getLogger().fine("Remove Docker container: " + containerId);
          } catch (DockerException | InterruptedException e) {
            throw new IOException(e);
          }
          try {
            dockerClient.removeContainer(containerId);
          } catch (DockerException | InterruptedException e) {
            EoulsanLogger.getLogger()
                .severe("Unable to remove Docker container: " + containerId);
          }

          return exitValue;
        }
      };

    } catch (DockerException | InterruptedException e) {
      throw new IOException(e);
    }

  }

  //
  // Docker methods
  //

  @Override
  public void pullImageIfNotExists() throws IOException {

    try {

      for (Image image : nullToEmpty(this.dockerClient.listImages())) {
        for (String tag : nullToEmpty(image.repoTags())) {
          if (this.dockerImage.equals(tag)) {
            return;
          }
        }
      }

      getLogger().fine("Pull Docker image: " + this.dockerImage);
      this.dockerClient.pull(this.dockerImage);
    } catch (InterruptedException | DockerException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void pullImageIfNotExists(final ProgressHandler progress)
      throws IOException {

    try {

      for (Image image : nullToEmpty(this.dockerClient.listImages())) {
        for (String tag : nullToEmpty(image.repoTags())) {
          if (this.dockerImage.equals(tag)) {
            return;
          }
        }
      }

      getLogger().fine("Pull Docker image: " + this.dockerImage);

      if (progress != null) {

        // With ProgressHandler

        final com.spotify.docker.client.ProgressHandler pg =
            new com.spotify.docker.client.ProgressHandler() {

              private final Map<String, Double> imagesProgress =
                  new HashMap<>();

              @Override
              public void progress(final ProgressMessage msg)
                  throws DockerException {

                final String id = msg.id();
                final ProgressDetail pgd = msg.progressDetail();

                // Image id must be set
                if (id == null) {
                  return;
                }

                // Register all the images to download
                if (!this.imagesProgress.containsKey(id)) {
                  this.imagesProgress.put(id, 0.0);
                }

                // Only show download progress
                if (!"Downloading".equals(msg.status())) {
                  return;
                }

                // ProgressDetail must be currently set
                if (pgd != null && pgd.total() > 0) {

                  // Compute the progress of the current image
                  final double imageProgress =
                      (double) pgd.current() / pgd.total();

                  // Update the map
                  this.imagesProgress.put(id, imageProgress);

                  // Compute downloading progress
                  double sum = 0;
                  for (double d : this.imagesProgress.values()) {
                    sum += d;
                  }
                  final double downloadProgress =
                      sum / (this.imagesProgress.size() - 1);

                  // Update the progress message
                  if (downloadProgress >= 0.0 && downloadProgress <= 1.0) {
                    progress.update(downloadProgress);
                  }
                }
              }

            };

        this.dockerClient.pull(this.dockerImage, pg);

      } else {

        // Without ProgressHandler

        this.dockerClient.pull(this.dockerImage);
      }

    } catch (InterruptedException | DockerException e) {
      throw new IOException(e);
    }

  }

  /**
   * Create Docker binds.
   * @param executionDirectory execution directory
   * @param files files to binds
   * @param convertNFSFilesToMountRoots convert NFS files to mount points
   * @return an HostConfig object
   */
  private static HostConfig createBinds(final File executionDirectory,
      final List<File> files, final boolean convertNFSFilesToMountRoots)
      throws IOException {

    HostConfig.Builder builder = HostConfig.builder();
    Set<String> binds = new HashSet<>();

    if (executionDirectory != null) {

      File f = convertNFSFilesToMountRoots
          ? convertNFSFileToMountPoint(executionDirectory) : executionDirectory;

      binds.add(f.getAbsolutePath() + ':' + f.getAbsolutePath());
    }

    if (files != null) {
      for (File f : fileIndirections(
          convertNFSFileToMountPoint(files, convertNFSFilesToMountRoots))) {
        binds.add(f.getAbsolutePath() + ':' + f.getAbsolutePath());
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

  /**
   * Convert a file path to a mount point path if the file is on a NFS server.
   * @param files the list of the files to convert
   * @param convertNFSFilesToMountRoots true if files must be converted
   * @return a set of files
   * @throws IOException if mount of a file cannot be found
   */
  static Set<File> convertNFSFileToMountPoint(final Collection<File> files,
      final boolean convertNFSFilesToMountRoots) throws IOException {

    if (files == null) {
      return null;
    }

    Set<File> result = new LinkedHashSet<>();

    for (File file : files) {

      if (file != null && file.exists()) {
        result.add(convertNFSFilesToMountRoots
            ? convertNFSFileToMountPoint(file) : file);
      }
    }

    return result;
  }

  /**
   * Convert a file path to a mount point path if the file is on a NFS server.
   * @param file the file to convert
   * @return a converted file
   * @throws IOException if mount of a file cannot be found
   */
  static File convertNFSFileToMountPoint(final File file) throws IOException {

    if (file == null) {
      return null;
    }

    FileStore fileStore = Files.getFileStore(file.toPath());
    EoulsanLogger.getLogger()
        .info("file: " + file + ", fileSystem type: " + fileStore.type());

    // If the file is on an NFS mount
    if ("nfs".equals(fileStore.type()) || "nfs4".equals(fileStore.type())) {

      // Get Mount point
      String info = fileStore.toString();
      String mountPoint =
          info.substring(0, info.length() - fileStore.name().length() - 3);

      return new File(mountPoint);
    }

    return file;
  }

  /**
   * List all the indirections of files.
   * @param files the files
   * @return a set with the file indirections
   * @throws IOException if an error occurs while searching indirections
   */
  static final Set<File> fileIndirections(final Collection<File> files)
      throws IOException {

    if (files == null) {
      return null;
    }

    Set<File> result = new LinkedHashSet<>();

    for (File f : files) {

      if (f != null) {
        result.addAll(fileIndirections(f));
      }
    }

    return result;
  }

  /**
   * List all the file indirections.
   * @param file the file
   * @return a set with the file indirections
   * @throws IOException if an error occurs while searching indirections
   */
  static final Set<File> fileIndirections(final File file) throws IOException {

    if (file == null) {
      return null;
    }

    Set<File> result = new LinkedHashSet<>();

    fileIndirections(file, result);

    return result;
  }

  /**
   * List all the file indirections.
   * @param file the file
   * @param result the result object
   * @throws IOException if an error occurs while searching indirections
   */
  private static final void fileIndirections(final File file, Set<File> result)
      throws IOException {

    if (file == null) {
      return;
    }

    // Case has been already processed
    if (result.contains(file)) {
      return;
    }

    File previousFile = new File("/");

    for (File f : parentDirectories(file)) {

      Path path = f.toPath();

      if (Files.isSymbolicLink(path)) {

        // Get the target of the link
        Path link = Files.readSymbolicLink(path);

        // If the target is not an absolute path
        if (!link.isAbsolute()) {
          link = new File(previousFile, link.toString()).toPath();
        }

        // Process the target of the link
        fileIndirections(link.toFile().getAbsoluteFile(), result);
        result.add(f);
      }

      previousFile = f;
    }

    result.add(file);
  }

  /**
   * Get all the parent directories of a file.
   * @param file the file
   * @return a list with all the parent directories of the file
   */
  private static List<File> parentDirectories(final File file) {

    List<File> result = new ArrayList<>();
    File f = file;

    do {

      result.add(0, f);
      f = f.getParentFile();

    } while (f != null);

    return result;
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
   * @param dockerClient Docker connection URI
   * @param dockerImage Docker image
   */
  SpotifyDockerImageInstance(final DockerClient dockerClient,
      final String dockerImage) {

    requireNonNull(dockerClient, "dockerClient argument cannot be null");
    requireNonNull(dockerImage, "dockerImage argument cannot be null");

    EoulsanLogger.getLogger().fine(
        getClass().getSimpleName() + " docker image used: " + dockerImage);

    this.dockerClient = dockerClient;
    this.dockerImage = dockerImage;
    this.userUid = SystemUtils.uid();
    this.userGid = SystemUtils.gid();
    this.convertNFSFilesToMountRoots = EoulsanRuntime.isRuntime()
        ? EoulsanRuntime.getSettings().isDockerMountNFSRoots() : false;
  }

}
