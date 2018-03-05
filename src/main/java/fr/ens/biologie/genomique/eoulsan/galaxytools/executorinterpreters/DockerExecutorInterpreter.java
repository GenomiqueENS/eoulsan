package fr.ens.biologie.genomique.eoulsan.galaxytools.executorinterpreters;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.List;

import com.google.common.base.Objects;

import fr.ens.biologie.genomique.eoulsan.util.StringUtils;
import fr.ens.biologie.genomique.eoulsan.util.process.DockerManager;
import fr.ens.biologie.genomique.eoulsan.util.process.SimpleProcess;

/**
 * This class define a Docker executor interpreter.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DockerExecutorInterpreter extends AbstractExecutorInterpreter {

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
  protected SimpleProcess newSimpleProcess() throws IOException {

    return DockerManager.getInstance().createImageInstance(this.dockerImage);
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("name", getName())
        .add("dockerImage", dockerImage).toString();
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
