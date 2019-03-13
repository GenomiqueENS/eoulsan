package fr.ens.biologie.genomique.eoulsan.modules.diffana;

import static fr.ens.biologie.genomique.eoulsan.requirements.DockerRequirement.newDockerRequirement;
import static fr.ens.biologie.genomique.eoulsan.util.r.ProcessRExecutor.RSCRIPT_EXECUTABLE;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Settings;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.requirements.PathRequirement;
import fr.ens.biologie.genomique.eoulsan.requirements.Requirement;
import fr.ens.biologie.genomique.eoulsan.requirements.RserveRequirement;
import fr.ens.biologie.genomique.eoulsan.util.r.DockerRExecutor;
import fr.ens.biologie.genomique.eoulsan.util.r.ProcessRExecutor;
import fr.ens.biologie.genomique.eoulsan.util.r.RExecutor;
import fr.ens.biologie.genomique.eoulsan.util.r.RExecutorFactory;
import fr.ens.biologie.genomique.eoulsan.util.r.RExecutorFactory.Mode;
import fr.ens.biologie.genomique.eoulsan.util.r.RserveRExecutor;

/**
 * This class define common methods used for the configuration of the step of
 * the package.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class RModuleCommonConfiguration {

  public static final String EXECUTION_MODE_PARAMETER = "r.execution.mode";
  public static final String RSERVE_SERVER_PARAMETER = "rserve.servername";
  public static final String DOCKER_IMAGE_PARAMETER = "docker.image";

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

    requireNonNull(context, "context argument cannot be null");
    requireNonNull(stepParameters, "stepParameters argument cannot be null");
    requireNonNull(requirements, "requirements argument cannot be null");
    requireNonNull(defaultDockerImage,
        "defaultDockerImage argument cannot be null");

    final Set<Parameter> toRemove = new HashSet<>();
    final Settings settings = context.getSettings();

    RExecutorFactory.Mode executionMode = null;
    String dockerImage = defaultDockerImage;
    String rserveServer = settings.isRServeServerEnabled()
        ? settings.getRServeServerName() : null;

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case EXECUTION_MODE_PARAMETER:
        executionMode = Mode.parse(p.getStringValue());

        if (executionMode == null) {
          Modules.badParameterValue(context, p, "Unknown execution mode");
        }
        toRemove.add(p);
        break;

      case RSERVE_SERVER_PARAMETER:
        rserveServer = p.getStringValue();
        toRemove.add(p);
        break;

      case DOCKER_IMAGE_PARAMETER:
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

    case ProcessRExecutor.REXECUTOR_NAME:
      requirements.add(PathRequirement.newPathRequirement(RSCRIPT_EXECUTABLE));
      break;

    case DockerRExecutor.REXECUTOR_NAME:
      requirements.add(newDockerRequirement(dockerImage));
      break;

    case RserveRExecutor.REXECUTOR_NAME:
      requirements.add(RserveRequirement.newRserveRequirement(rserveServer));
      break;

    default:
      break;
    }

    return result;
  }

}
