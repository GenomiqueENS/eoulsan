package fr.ens.biologie.genomique.eoulsan.util.process;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.util.process.SpotifyDockerImageInstance.convertNFSFileToMountPoint;
import static fr.ens.biologie.genomique.eoulsan.util.process.SpotifyDockerImageInstance.fileIndirections;
import static java.util.Objects.requireNonNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;

public class SingularityDockerImageInstance extends AbstractSimpleProcess
    implements DockerImageInstance {

  private final String dockerImage;
  private final File imageDirectory;
  private final boolean convertNFSFilesToMountRoots;

  @Override
  public AdvancedProcess start(final List<String> commandLine,
      final File executionDirectory,
      final Map<String, String> environmentVariables,
      final File temporaryDirectory, final File stdoutFile,
      final File stderrFile, final boolean redirectErrorStream,
      final File... filesUsed) throws IOException {

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

    // Pull image if needed
    pullImageIfNotExists();

    final List<String> command = new ArrayList<>();
    command.add("singularity");
    command.add("exec");

    // The "--pwd" and "--contain" options are incompatible in 2.4.2. Need
    // Singularity 2.4.3+
    if (!EoulsanRuntime.getSettings()
        .getBooleanSetting("debug.docker.singularity.disable.contain")) {
      command.add("--contain");
    }

    // File/directories to mount
    List<File> directoriesToBind = new ArrayList<>();
    if (filesUsed != null) {
      directoriesToBind.addAll(Arrays.asList(filesUsed));
    }

    // Execution directory
    if (executionDirectory != null) {
      directoriesToBind.add(executionDirectory);
      command.add("--pwd");
      command.add(executionDirectory.getAbsolutePath());
    }

    // Temporary directory
    if (temporaryDirectory != null && temporaryDirectory.isDirectory()) {
      directoriesToBind.add(temporaryDirectory);
      command.add("--workdir");
      command.add(temporaryDirectory.getAbsolutePath());
    }

    // Bind directories
    toBind(command, directoriesToBind, this.convertNFSFilesToMountRoots);

    // TODO The container must be writable as Docker images but it cannot work
    // with Singularity Docker image compatibility mode
    // command.add("--writable");

    // Docker image to use
    // command.add("docker://" + this.dockerImage);
    command.add(new File(this.imageDirectory,
        dockerImageNameToSingularityImageName(this.dockerImage))
            .getAbsolutePath());

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

    EoulsanLogger.getLogger().fine(getClass().getSimpleName()
        + ": singularity command line: " + pb.command());

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

    // Do nothing if the image has been already downloaded
    if (new File(this.imageDirectory,
        dockerImageNameToSingularityImageName(this.dockerImage)).exists()) {
      return;
    }

    getLogger().fine("Pull Docker image: " + this.dockerImage);
    Process p = new ProcessBuilder("singularity", "pull",
        "docker://" + this.dockerImage).directory(this.imageDirectory).start();
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

    // Add the image to the image list
    addImageToImageListFile(this.dockerImage);
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

  /**
   * Add an image name the image list file.
   * @param dockerImage the name of the docker image
   * @throws IOException if an error occurs while adding the image name to the
   *           image list file
   */
  private void addImageToImageListFile(final String dockerImage)
      throws IOException {

    File f = new File(this.imageDirectory, "image.list");

    try (FileWriter fw = new FileWriter(f, true);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bw)) {

      out.println(dockerImage
          + '\t' + dockerImageNameToSingularityImageName(dockerImage));
    }
  }

  private static String dockerImageNameToSingularityImageName(
      final String dockerImage) {

    if (dockerImage == null) {
      return null;
    }

    String result;
    int pos = dockerImage.indexOf('/');

    if (pos != -1) {
      result = dockerImage.substring(pos + 1);
    } else {
      result = dockerImage;
    }

    return result.replace(':', '-') + ".simg";
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param dockerImage Docker image
   */
  SingularityDockerImageInstance(final String dockerImage,
      final File imageDirectory) {

    requireNonNull(dockerImage, "dockerImage argument cannot be null");
    requireNonNull(imageDirectory, "imageDirectory argument cannot be null");

    EoulsanLogger.getLogger().fine(
        getClass().getSimpleName() + " docker image used: " + dockerImage);

    this.dockerImage = dockerImage;
    this.imageDirectory = imageDirectory;

    this.convertNFSFilesToMountRoots = EoulsanRuntime.isRuntime()
        ? EoulsanRuntime.getSettings().isDockerMountNFSRoots() : false;
  }

}
