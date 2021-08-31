package fr.ens.biologie.genomique.eoulsan.util.process;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.log.GenericLogger;
import fr.ens.biologie.genomique.eoulsan.util.SystemUtils;
import fr.ens.biologie.genomique.eoulsan.util.Utils;

/**
 * This class define a Docker image instance using the DockerClient Docker
 * client library.
 * @author Laurent Jourdren
 * @since 2.6
 */
public class DockerClientDockerImageInstance extends AbstractSimpleProcess
    implements DockerImageInstance {

  private final com.github.dockerjava.api.DockerClient dockerClient;
  private final String dockerImage;
  private final int userUid;
  private final int userGid;
  private final boolean convertNFSFilesToMountRoots;
  private final GenericLogger logger;

  class LogWriteAll extends ResultCallback.Adapter<Frame> {

    final OutputStream stdoutChannel;
    final OutputStream stderrChannel;

    @Override
    public void onComplete() {
      super.onComplete();
      try {
        this.stdoutChannel.close();
        this.stderrChannel.close();
      } catch (IOException e) {
        logger.error(e.getMessage());
      }
    }

    @Override
    public void onError(Throwable arg0) {
      super.onComplete();
      try {
        this.stdoutChannel.close();
        this.stderrChannel.close();
      } catch (IOException e) {
        logger.error(e.getMessage());
      }
    }

    @Override
    public void onNext(Frame item) {

      try {

        switch (item.getStreamType()) {
        case STDOUT:
          this.stdoutChannel.write(item.getPayload());
          break;
        case STDERR:
          this.stderrChannel.write(item.getPayload());
          break;

        default:
          break;
        }

      } catch (IOException e) {
        logger.error(e.getMessage());
      }
    }

    LogWriteAll(File stdoutFile, File stderrFile) throws FileNotFoundException {

      this.stdoutChannel = new FileOutputStream(stdoutFile);
      this.stderrChannel = new FileOutputStream(stderrFile);
    }
  }

  class LogWriteRedirect extends ResultCallback.Adapter<Frame> {

    final OutputStream stdoutChannel;
    final OutputStream stderrChannel;

    @Override
    public void onComplete() {
      super.onComplete();
      try {
        this.stdoutChannel.close();
        this.stderrChannel.close();
      } catch (IOException e) {
        logger.error(e.getMessage());
      }
    }

    @Override
    public void onError(Throwable arg0) {
      super.onComplete();
      try {
        this.stdoutChannel.close();
        this.stderrChannel.close();
      } catch (IOException e) {
        logger.error(e.getMessage());
      }
    }

    @Override
    public void onNext(Frame item) {

      try {

        switch (item.getStreamType()) {
        case STDOUT:
          this.stdoutChannel.write(item.getPayload());
          break;
        case STDERR:
          this.stderrChannel.write(item.getPayload());
          break;

        default:
          break;
        }

      } catch (IOException e) {
        logger.error(e.getMessage());
      }

    }

    LogWriteRedirect(File stdoutFile, File stderrFile)
        throws FileNotFoundException {

      this.stdoutChannel = new FileOutputStream(stdoutFile);
      this.stderrChannel = new FileOutputStream(stderrFile);
    }
  }

  @Override
  public AdvancedProcess start(List<String> commandLine,
      File executionDirectory, Map<String, String> environmentVariables,
      File temporaryDirectory, File stdoutFile, File stderrFile,
      boolean redirectErrorStream, File... filesUsed) throws IOException {

    requireNonNull(commandLine, "commandLine argument cannot be null");
    requireNonNull(stdoutFile, "stdoutFile argument cannot be null");
    requireNonNull(stderrFile, "stderrFile argument cannot be null");

    this.logger.debug(getClass().getSimpleName()
        + ": commandLine=" + commandLine + ", executionDirectory="
        + executionDirectory + ", environmentVariables=" + environmentVariables
        + ", temporaryDirectory=" + temporaryDirectory + ", stdoutFile="
        + stdoutFile + ", stderrFile=" + stderrFile + ", redirectErrorStream="
        + redirectErrorStream + ", filesUsed=" + Arrays.toString(filesUsed));

    if (executionDirectory != null) {
      checkArgument(executionDirectory.isDirectory(),
          "execution directory does not exists or is not a directory: "
              + executionDirectory.getAbsolutePath());
    }

    final List<String> env = new ArrayList<>();

    if (environmentVariables != null) {
      for (Map.Entry<String, String> e : environmentVariables.entrySet()) {
        env.add(e.getKey() + '=' + e.getValue());
      }
    }

    // Pull image if needed
    pullImageIfNotExists();

    // Create container configuration
    this.logger
        .debug("Configure container, command to execute: " + commandLine);

    final CreateContainerCmd cmd = this.dockerClient
        .createContainerCmd(this.dockerImage).withCmd(commandLine);

    // Set the working directory
    if (executionDirectory != null) {
      cmd.withWorkingDir(executionDirectory.getAbsolutePath());
    }

    // Set the UID and GID of the docker process
    if (this.userUid >= 0 && this.userGid >= 0) {
      cmd.withUser(this.userUid + ":" + this.userGid);
    }

    // File/directories to mount
    final List<File> toBind = new ArrayList<>();
    if (filesUsed != null) {
      toBind.addAll(Utils.filterNull(Arrays.asList(filesUsed)));
    }

    // Define temporary directory
    if (temporaryDirectory != null && temporaryDirectory.isDirectory()) {
      toBind.add(temporaryDirectory);
      env.add(
          TMP_DIR_ENV_VARIABLE + "=" + temporaryDirectory.getAbsolutePath());
    }

    // Set binds
    cmd.getHostConfig()
        .setBinds(createBinds(executionDirectory, toBind,
            this.convertNFSFilesToMountRoots, this.logger)
                .toArray(new Bind[0]));

    // Set environment variables
    cmd.withEnv(env);

    // Create container
    CreateContainerResponse container = cmd.exec();

    // Get container id
    final String containerId = container.getId();

    // Start container
    this.logger.debug("Start of the Docker container: " + containerId);
    this.dockerClient.startContainerCmd(container.getId()).exec();

    redirect(containerId, stdoutFile, stderrFile, redirectErrorStream);

    // Get process exit code
    ContainerState state =
        this.dockerClient.inspectContainerCmd(containerId).exec().getState();

    if ("running".equals(state.getStatus()) && state.getPidLong() == 0L) {
      throw new IOException(
          "Error while executing container, container pid is 0");
    }

    return () -> {

      long exitValue;

      // Wait the end of the container
      this.logger.debug("Wait the end of the Docker container: " + containerId);
      this.dockerClient.waitContainerCmd(containerId)
          .exec(new WaitContainerResultCallback()).awaitStatusCode();

      // Get process exit code
      ContainerState exitState =
          this.dockerClient.inspectContainerCmd(containerId).exec().getState();

      exitValue = exitState.getExitCodeLong();
      this.logger.debug("Exit value: " + exitValue);

      // Remove container
      this.logger.debug("Remove Docker container: " + containerId);

      dockerClient.removeContainerCmd(containerId).exec();

      return (int) exitValue;
    };
  }

  private void redirect(final String containerId, final File stdout,
      final File stderr, final boolean redirectErrorStream)
      throws FileNotFoundException {

    LogContainerCmd logContainerCmd =
        this.dockerClient.logContainerCmd(containerId);
    logContainerCmd.withStdOut(true).withStdErr(true);

    ResultCallback.Adapter<Frame> callback = redirectErrorStream
        ? new LogWriteRedirect(stdout, stderr)
        : new LogWriteAll(stdout, stderr);

    try {
      logContainerCmd.exec(callback).awaitCompletion();
    } catch (InterruptedException e) {
      this.logger.error(e.getMessage());
    }

  };

  @Override
  public void pullImageIfNotExists() throws IOException {

    String imageName = this.dockerImage;
    String tag = "";

    int pos = this.dockerImage.lastIndexOf(':');
    if (pos != 1) {

      imageName = this.dockerImage.substring(0, pos);
      tag = this.dockerImage.substring(pos + 1);
    }

    try {
      this.dockerClient.pullImageCmd(imageName).withTag(tag)
          .exec(new PullImageResultCallback())
          .awaitCompletion(30, TimeUnit.SECONDS);

    } catch (InterruptedException e) {
      throw new IOException(e);
    }

  }

  @Override
  public void pullImageIfNotExists(ProgressHandler progress)
      throws IOException {

    pullImageIfNotExists();
  }

  //
  // Other methods
  //

  /**
   * Create Docker binds.
   * @param executionDirectory execution directory
   * @param files files to binds
   * @param convertNFSFilesToMountRoots convert NFS files to mount points
   * @return a list with binds to create
   */
  private static List<Bind> createBinds(final File executionDirectory,
      final List<File> files, final boolean convertNFSFilesToMountRoots,
      final GenericLogger logger) throws IOException {

    List<Bind> binds = new ArrayList<>();
    Set<File> mounted = new HashSet<>();

    if (executionDirectory != null) {

      File f = convertNFSFilesToMountRoots
          ? DockerUtils.convertNFSFileToMountPoint(executionDirectory, logger)
          : executionDirectory;

      binds.add(Bind.parse(f.getAbsolutePath() + ':' + f.getAbsolutePath()));
      mounted.add(f.getAbsoluteFile());
    }

    if (files != null) {
      for (File f : DockerUtils
          .fileIndirections(DockerUtils.convertNFSFileToMountPoint(files,
              convertNFSFilesToMountRoots, logger))) {

        if (!mounted.contains(f.getAbsoluteFile())) {
          binds
              .add(Bind.parse(f.getAbsolutePath() + ':' + f.getAbsolutePath()));
          mounted.add(f.getAbsoluteFile());
        }
      }
    }

    return binds;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param dockerClient Docker connection URI
   * @param dockerImage Docker image
   * @param logger logger to use
   */
  DockerClientDockerImageInstance(
      final com.github.dockerjava.api.DockerClient dockerClient,
      final String dockerImage, final GenericLogger logger) {

    requireNonNull(dockerClient, "dockerClient argument cannot be null");
    requireNonNull(dockerImage, "dockerImage argument cannot be null");
    requireNonNull(logger, "logger argument cannot be null");

    logger.debug(
        getClass().getSimpleName() + " docker image used: " + dockerImage);

    this.dockerClient = dockerClient;
    this.dockerImage = dockerImage;
    this.userUid = SystemUtils.uid();
    this.userGid = SystemUtils.gid();
    this.convertNFSFilesToMountRoots = EoulsanRuntime.isRuntime()
        ? EoulsanRuntime.getSettings().isDockerMountNFSRoots() : false;
    this.logger = logger;
  }

}
