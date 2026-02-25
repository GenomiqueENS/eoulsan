package fr.ens.biologie.genomique.eoulsan.galaxytools.executorinterpreters;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import fr.ens.biologie.genomique.eoulsan.util.EoulsanDockerManager;
import fr.ens.biologie.genomique.kenetre.util.StringUtils;
import fr.ens.biologie.genomique.kenetre.util.process.SimpleProcess;
import java.io.IOException;
import java.util.List;

/**
 * This class define a Docker executor interpreter.
 *
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

    requireNonNull(arguments, "arguments argument cannot be null");

    return StringUtils.splitShellCommandLine(arguments);
  }

  @Override
  protected SimpleProcess newSimpleProcess() throws IOException {

    return EoulsanDockerManager.getInstance().createImageInstance(this.dockerImage);
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return MoreObjects.toStringHelper(this)
        .add("name", getName())
        .add("dockerImage", dockerImage)
        .toString();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   *
   * @param dockerImage Docker image
   */
  public DockerExecutorInterpreter(final String dockerImage) {

    requireNonNull(dockerImage, "dockerImage argument cannot be null");

    this.dockerImage = dockerImage;
  }
}
