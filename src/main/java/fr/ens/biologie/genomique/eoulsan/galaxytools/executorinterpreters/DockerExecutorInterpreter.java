package fr.ens.biologie.genomique.eoulsan.galaxytools.executorinterpreters;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.google.common.base.Objects;

import fr.ens.biologie.genomique.eoulsan.util.SimpleProcess;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;
import fr.ens.biologie.genomique.eoulsan.util.docker.DockerSimpleProcess;

/**
 * This class define a Docker executor interpreter.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DockerExecutorInterpreter extends AbstractExecutorInterpreter {

  public static final String INTERPRETER_NAME = "docker";

  private final String dockerImage;
  private final int requiredMemory;
  private final int requiredProcessors;

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
  protected SimpleProcess newSimpleProcess() {

    return new DockerSimpleProcess(this.dockerImage, this.requiredProcessors,
        this.requiredMemory);
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
   * @param requiredProcessors required processors
   * @param requiredMemory required memory
   */
  public DockerExecutorInterpreter(final String dockerImage,
      final int requiredProcessors, final int requiredMemory) {

    checkNotNull(dockerImage, "dockerImage argument cannot be null");

    this.dockerImage = dockerImage;
    this.requiredMemory = requiredMemory;
    this.requiredProcessors = requiredProcessors;
  }

}
