package fr.ens.biologie.genomique.eoulsan.steps.diffana.local;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.biologie.genomique.eoulsan.requirements.DockerRequirement.newDockerRequirement;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.requirements.Requirement;
import fr.ens.biologie.genomique.eoulsan.steps.Steps;
import fr.ens.biologie.genomique.eoulsan.util.r.DockerRExecutor;
import fr.ens.biologie.genomique.eoulsan.util.r.RExecutor;
import fr.ens.biologie.genomique.eoulsan.util.r.RExecutorFactory;
import fr.ens.biologie.genomique.eoulsan.util.r.RExecutorFactory.Mode;

/**
 * This class define common methods used for the configuration of the step of
 * the package.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class CommonConfiguration {

  private static final String EXECUTION_MODE = "execution.mode";
  private static final String RSERVE_SERVER = "rserve.servername";
  private static final String DOCKER_IMAGE = "docker.image";

  /**
   * Parse the step parameter and create a configured RExecutor object.
   * @param context the step context
   * @param stepParameters the step parameters. Must be a mutable object
   * @param defaultDockerImage default docker image
   * @return a configured RExecutor object
   * @throws EoulsanException if one or more parameter is invalid
   */
  public static RExecutor parseRExecutorParameter(
      final StepConfigurationContext context,
      final Set<Parameter> stepParameters, final Set<Requirement> requirements,
      final String defaultDockerImage) throws EoulsanException {

    checkNotNull(context, "context argument cannot be null");
    checkNotNull(stepParameters, "stepParameters argument cannot be null");
    checkNotNull(requirements, "requirements argument cannot be null");
    checkNotNull(defaultDockerImage,
        "defaultDockerImage argument cannot be null");

    final Set<Parameter> toRemove = new HashSet<>();

    RExecutorFactory.Mode executionMode = null;
    String rserveServer = null;
    String dockerImage = defaultDockerImage;

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case EXECUTION_MODE:
        executionMode = Mode.parse(p.getStringValue());

        if (executionMode == null) {
          Steps.badParameterValue(context, p, "Unknown execution mode");
        }
        toRemove.add(p);
        break;

      case RSERVE_SERVER:
        rserveServer = p.getStringValue();
        toRemove.add(p);
        break;

      case DOCKER_IMAGE:
        dockerImage = p.getStringValue();
        toRemove.add(p);
        break;

      default:
        break;

      }
    }

    // Remove parsed parameters
    stepParameters.removeAll(toRemove);

    // Create the executor object
    final RExecutor result;
    try {
      result = RExecutorFactory.newRExecutor(executionMode, rserveServer,
          dockerImage, context.getStepOutputDirectory().toFile(),
          context.getLocalTempDirectory());
    } catch (IOException e) {
      throw new EoulsanException(e);
    }

    // Set the requirements
    switch (result.getName()) {

    case DockerRExecutor.REXECUTOR_NAME:
      requirements.add(newDockerRequirement(dockerImage));
      break;

    default:
      break;
    }

    return result;
  }

}
