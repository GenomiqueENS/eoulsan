package fr.ens.biologie.genomique.eoulsan.galaxytools.executorinterpreters;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.List;

import com.google.common.base.Objects;
import com.spotify.docker.client.DockerException;

import fr.ens.biologie.genomique.eoulsan.galaxytools.ToolExecutorResult;
import fr.ens.biologie.genomique.eoulsan.util.SimpleProcess;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;
import fr.ens.biologie.genomique.eoulsan.util.docker.DockerSimpleProcess;

/**
 * This class define a Docker executor interpreter.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DockerExecutorInterpreter implements ExecutorInterpreter {

  public static final String INTERPRETER_NAME = "docker";

  private final String dockerImage;

  @Override
  public String getName() {

    return INTERPRETER_NAME;
  }

  @Override
  public List<String> createCommandLine(final String arguments) {

    checkNotNull(arguments, "arguments argument cannot be null");

    return StringUtils.splitShellCommandLine(arguments);
  }

  @Override
  public ToolExecutorResult execute(final List<String> commandLine,
      final File executionDirectory, final File temporaryDirectory,
      final File stdoutFile, final File stderrFile) {

    checkNotNull(commandLine, "commandLine argument cannot be null");
    checkNotNull(executionDirectory,
        "executionDirectory argument cannot be null");
    checkNotNull(stdoutFile, "stdoutFile argument cannot be null");
    checkNotNull(stderrFile, "stderrFile argument cannot be null");

    checkArgument(executionDirectory.isDirectory(),
        "execution directory does not exists or is not a directory: "
            + executionDirectory.getAbsolutePath());

    try {

      SimpleProcess process = new DockerSimpleProcess(this.dockerImage);
      final int exitValue = process.execute(commandLine, executionDirectory,
          temporaryDirectory, stdoutFile, stderrFile);

      return new ToolExecutorResult(commandLine, exitValue);

    } catch (DockerException | InterruptedException e) {
      return new ToolExecutorResult(commandLine, e);
    }
  }

  //
  // Docker methods
  //

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
   */
  public DockerExecutorInterpreter(final String dockerImage) {

    checkNotNull(dockerImage, "dockerImage argument cannot be null");

    this.dockerImage = dockerImage;
  }

}
