package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static fr.ens.biologie.genomique.eoulsan.Globals.STEP_OUTPUT_DIRECTORY_SUFFIX;
import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.EoulsanAnnotationUtils;
import fr.ens.biologie.genomique.eoulsan.annotations.ExecutionMode;
import fr.ens.biologie.genomique.eoulsan.core.Module;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;

/**
 * This class allow to determine the output directory of a step.
 * @author Laurent Jourdren
 * @since 2.0
 */
class StepOutputDirectory {

  private static StepOutputDirectory singleton;

  private final OutputTreeType outputTree;
  private final boolean hadoopMode;

  /**
   * This enum define the output tree type of a step.
   */
  public enum OutputTreeType {
    FLAT, STEP;

    /**
     * Get a OutputTreeType object from a type name.
     * @param type the type
     * @return a OutputTreeType object
     */
    public static OutputTreeType getOutputTreeType(final String type) {

      if (type == null) {
        return OutputTreeType.valueOf(Globals.OUTPUT_TREE_TYPE_DEFAULT);
      }

      final OutputTreeType result =
          OutputTreeType.valueOf(type.toUpperCase(Globals.DEFAULT_LOCALE).trim());

      if (result == null) {
        return OutputTreeType.valueOf(Globals.OUTPUT_TREE_TYPE_DEFAULT);
      }
      return result;
    }

    /**
     * Get a OutputTreeType object from the Eoulsan settings.
     * @return a OutputTreeType object
     */
    public static OutputTreeType getOutputTreeType() {

      return getOutputTreeType(
          EoulsanRuntime.getSettings().getOutputTreeType());
    }

  }

  /**
   * Get the output directory of the step if the outputs of the step are outputs
   * of workflow.
   * @param workflow the workflow
   * @param step the step
   * @return the output directory of the step
   */
  public DataFile defaultDirectory(final AbstractWorkflow workflow,
      final AbstractStep step, final Module module,
      final boolean copyResultsToOutput) {

    requireNonNull(workflow, "workflow argument cannot be null");
    requireNonNull(step, "step argument cannot be null");
    requireNonNull(module, "module argument cannot be null");

    if (this.hadoopMode
        && ExecutionMode.getExecutionMode(module.getClass())
            .isHadoopCompatible()) {
      return workflow.getHadoopWorkingDirectory();
    }

    if (copyResultsToOutput) {
      return workflowDirectory(workflow, step, module);
    }

    return workingDirectory(workflow, step, module);
  }

  /**
   * Get the working directory of a step.
   * @param workflow the workflow
   * @param step the step
   * @param module the module
   * @return the working directory of a step
   */
  public DataFile workingDirectory(final AbstractWorkflow workflow,
      final AbstractStep step, final Module module) {

    requireNonNull(workflow, "workflow argument cannot be null");
    requireNonNull(step, "step argument cannot be null");
    requireNonNull(module, "module argument cannot be null");

    if (this.hadoopMode
        && ExecutionMode.getExecutionMode(module.getClass())
            .isHadoopCompatible()) {
      return workflow.getHadoopWorkingDirectory();
    }

    return workflow.getLocalWorkingDirectory();
  }

  /**
   * Get the workflow output directory of a step.
   * @param workflow the workflow
   * @param step the step
   * @param module the module
   * @return the working directory of a step
   */
  public DataFile workflowDirectory(final AbstractWorkflow workflow,
      final AbstractStep step, final Module module) {

    requireNonNull(workflow, "workflow argument cannot be null");
    requireNonNull(step, "step argument cannot be null");
    requireNonNull(module, "module argument cannot be null");

    if (EoulsanAnnotationUtils.isNoOutputDirectory(module)) {
      return workflow.getOutputDirectory();
    }

    switch (this.outputTree) {
    case STEP:

      final String subDirname = step.getId() + STEP_OUTPUT_DIRECTORY_SUFFIX;
      return new DataFile(workflow.getOutputDirectory(), subDirname);

    case FLAT:
    default:
      return workflow.getOutputDirectory();
    }
  }

  //
  // Static methods
  //

  /**
   * Get the singleton of the class
   * @return the singleton of the class
   */
  public static synchronized StepOutputDirectory getInstance() {

    if (singleton == null) {
      singleton = new StepOutputDirectory();
    }

    return singleton;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private StepOutputDirectory() {

    this.hadoopMode = EoulsanRuntime.getRuntime().getMode().isHadoopMode();
    this.outputTree = OutputTreeType.getOutputTreeType();
  }

}
