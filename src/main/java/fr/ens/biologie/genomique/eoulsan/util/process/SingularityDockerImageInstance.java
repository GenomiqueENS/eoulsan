package fr.ens.biologie.genomique.eoulsan.util.process;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.util.process.SpotifyDockerImageInstance.convertNFSFileToMountPoint;
import static fr.ens.biologie.genomique.eoulsan.util.process.SpotifyDockerImageInstance.fileIndirections;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;

public class SingularityDockerImageInstance extends AbstractSimpleProcess
    implements DockerImageInstance {

  private final String dockerImage;
  private final boolean convertNFSFilesToMountRoots;

  @Override
  public AdvancedProcess start(final List<String> commandLine,
      final File executionDirectory,
      final Map<String, String> environmentVariables,
      final File temporaryDirectory, final File stdoutFile,
      final File stderrFile, final boolean redirectErrorStream,
      final File... filesUsed) throws IOException {

    checkNotNull(commandLine, "commandLine argument cannot be null");
    checkNotNull(stdoutFile, "stdoutFile argument cannot be null");
    checkNotNull(stderrFile, "stderrFile argument cannot be null");

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

    // Pull image if needed
    pullImageIfNotExists();

    final List<String> command = new ArrayList<>();
    command.add("singularity");
    command.add("run");
    command.add("--contain");

    // File/directories to mount
    List<File> directoriesToBind = new ArrayList<>();
    if (filesUsed != null) {
      directoriesToBind.addAll(Arrays.asList(filesUsed));
    }

    // Execution directory
    if (executionDirectory != null) {
      directoriesToBind.add(executionDirectory);
      command.add("--workdir");
      command.add(executionDirectory.getAbsolutePath());
    }


    // Temporary directory
    if (temporaryDirectory != null && temporaryDirectory.isDirectory()) {
      directoriesToBind.add(temporaryDirectory);
    }

    // Bind directories
    toBind(command, directoriesToBind, this.convertNFSFilesToMountRoots);

    // Remove container at the end of the execution
    command.add("--rm");

    // TODO The container must be writable as Docker images
    command.add("--writable");

    // Docker image to use
    command.add("docker://" + this.dockerImage);

    command.addAll(commandLine);

    // Redirect outputs
    final ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectOutput(stdoutFile);
    pb.redirectErrorStream(redirectErrorStream);
    if (!redirectErrorStream) {
      pb.redirectError(stderrFile);
    }

    // Environment variables
    pb.environment().clear();
    if (environmentVariables != null) {
      for (Map.Entry<String, String> e : environmentVariables.entrySet()) {
        pb.environment().put(e.getKey(), e.getValue());
      }
    }

    final Process process = pb.start();

    return new AdvancedProcess() {

      @Override
      public int waitFor() throws IOException {

        try {
          return process.waitFor();
        } catch (InterruptedException e) {
          throw new IOException(e);
        }
      }
    };

  }

  /**
   * Add the volume arguments to the Docker command line.
   * @param command the command line
   * @param files the share files to add
   * @throws IOException if an error occurs when converting the file path
   */
  private static void toBind(final List<String> command, final List<File> files,
      final boolean convertNFSFilesToMountRoots) throws IOException {

    for (File file : fileIndirections(
        convertNFSFileToMountPoint(files, convertNFSFilesToMountRoots))) {

      command.add("--bind");
      command.add(file.getAbsolutePath() + ':' + file.getAbsolutePath());
    }
  }

  @Override
  public void pullImageIfNotExists() throws IOException {

    getLogger().fine("Pull Docker image: " + this.dockerImage);
    Process p = new ProcessBuilder("singularity", "pull",
        "docker://" + this.dockerImage).start();
    int exitCode;
    try {
      exitCode = p.waitFor();
    } catch (InterruptedException e) {
      throw new IOException(
          "Error while pulling Docker image: " + this.dockerImage);
    }

    if (exitCode != 0) {
      throw new IOException(
          "Error while pulling Docker image: " + this.dockerImage);
    }
  }

  @Override
  public void pullImageIfNotExists(final ProgressHandler progress)
      throws IOException {

    if (progress != null) {
      progress.update(0);
    }

    pullImageIfNotExists();

    if (progress != null) {
      progress.update(1);
    }
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param dockerImage Docker image
   */
  SingularityDockerImageInstance(final String dockerImage) {

    checkNotNull(dockerImage, "dockerImage argument cannot be null");

    EoulsanLogger.getLogger().fine(
        getClass().getSimpleName() + " docker image used: " + dockerImage);

    this.dockerImage = dockerImage;

    this.convertNFSFilesToMountRoots = EoulsanRuntime.isRuntime()
        ? EoulsanRuntime.getSettings().isDockerMountNFSRoots() : false;
  }

}
