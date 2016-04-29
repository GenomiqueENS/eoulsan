package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkNotNull;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.annotations.ExecutionMode;
import fr.ens.biologie.genomique.eoulsan.core.Module;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;

/**
 * This class allow to determine the output directory of a step.
 * @author Laurent Jourdren
 * @since 2.0
 */
class StepOutputDirectoryDispatcher {

  private static final String STEP_OUTPUT_DIR_SUFFIX = "_output";

  private static StepOutputDirectoryDispatcher singleton;

  private boolean stepOutputOwnDirectory = true;

  /**
   * Get the output directory of the step if the outputs of the step are outputs
   * of workflow.
   * @param workflow the workflow
   * @param step the step
   * @return the output directory of the step
   */
  public DataFile dispatch(final AbstractWorkflow workflow,
      final AbstractStep step, final Module module,
      final boolean copyResultsToOutput) {

    checkNotNull(workflow, "workflow argument cannot be null");
    checkNotNull(step, "step argument cannot be null");
    checkNotNull(module, "module argument cannot be null");

    final boolean hadoopMode =
        EoulsanRuntime.getRuntime().getMode().isHadoopMode();

    if (!hadoopMode) {

      if (copyResultsToOutput) {
        return getOutputDirectory(workflow, step);
      }

      return workflow.getLocalWorkingDirectory();
    }

    switch (ExecutionMode.getExecutionMode(module.getClass())) {

    case HADOOP_COMPATIBLE:
    case HADOOP_INTERNAL:
    case HADOOP_ONLY:
      return workflow.getHadoopWorkingDirectory();

    case LOCAL_ONLY:
      if (copyResultsToOutput) {
        return getOutputDirectory(workflow, step);
      }

      return workflow.getLocalWorkingDirectory();

    default:
      return workflow.getLocalWorkingDirectory();
    }

  }

  /**
   * Get the output directory of the step if the outputs of the step are outputs
   * of workflow.
   * @param workflow the workflow
   * @param step the step
   * @return the output directory of the step
   */
  private DataFile getOutputDirectory(final AbstractWorkflow workflow,
      final AbstractStep step) {

    if (stepOutputOwnDirectory) {

      final String subDirname = step.getId() + STEP_OUTPUT_DIR_SUFFIX;

      return new DataFile(workflow.getOutputDirectory(), subDirname);
    }

    return workflow.getLocalWorkingDirectory();
  }

  //
  // Static methods
  //

  /**
   * Get the singleton of the class
   * @return the singleton of the class
   */
  private static synchronized StepOutputDirectoryDispatcher getInstance() {

    if (singleton == null) {
      singleton = new StepOutputDirectoryDispatcher();
    }

    return singleton;
  }

  /**
   * Define the working directory of the step.
   * @param workflow the workflow
   * @param step the step
   * @param module module instance
   * @return the working directory of the step
   */
  public static DataFile defineOutputDirectory(final AbstractWorkflow workflow,
      final AbstractStep step, final Module module,
      final boolean copyResultsToOutput) {

    return getInstance().dispatch(workflow, step, module, copyResultsToOutput);
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private StepOutputDirectoryDispatcher() {
  }

}
