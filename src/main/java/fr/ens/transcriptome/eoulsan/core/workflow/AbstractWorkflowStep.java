/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.noInputPort;
import static fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder.noOutputPort;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType.CHECKER_STEP;

import java.util.Collections;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.annotations.EoulsanMode;
import fr.ens.transcriptome.eoulsan.checkers.CheckerStep;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.steps.DesignStep;
import fr.ens.transcriptome.eoulsan.steps.FakeStep;

/**
 * This class define a step of the workflow. This class must be extended by a
 * class to be able to work with a specific worklow file format.
 * @author Laurent Jourdren
 * @since 1.3
 */
public abstract class AbstractWorkflowStep implements WorkflowStep {

  /** Serialization version UID. */
  private static final long serialVersionUID = 2040628014465126384L;

  private static int instanceCounter;

  private final AbstractWorkflow workflow;
  // private final WorkflowStepContext stepContext;

  private final int number;
  private final String id;
  private final StepType type;
  private final Set<Parameter> parameters;
  private InputPorts inputPortsParameter = noInputPort();
  private OutputPorts outputPortsParameter = noOutputPort();

  private final String stepName;
  private final EoulsanMode mode;
  private final boolean skip;
  private final boolean terminalStep;
  private final boolean copyResultsToOutput;
  private final boolean createLogFiles;

  private WorkflowOutputPorts outputPorts = WorkflowOutputPorts.noOutputPort();
  private WorkflowInputPorts inputPorts = WorkflowInputPorts.noInputPort();

  private final WorkflowStepStateObserver observer;

  private final DataFile workingDir;

  private StepResult result;

  //
  // Getters
  //

  @Override
  public Workflow getWorkflow() {

    return this.workflow;
  }

  /**
   * Get the abstract workflow object.
   * @return the AbstractWorkflow object of the step
   */
  AbstractWorkflow getAbstractWorkflow() {

    return this.workflow;
  }

  // @Override
  // public StepContext getContext() {
  //
  // return this.stepContext;
  // }

  @Override
  public int getNumber() {

    return this.number;
  }

  @Override
  public String getId() {

    return this.id;
  }

  @Override
  public boolean isSkip() {

    return this.skip;
  }

  /**
   * Test if the step is a terminal step.
   * @return true if the step is a terminal step
   */
  protected boolean isTerminalStep() {

    return this.terminalStep;
  }

  @Override
  public StepType getType() {

    return this.type;
  }

  /**
   * Get the underlying Step object.
   * @return the Step object
   */
  public Step getStep() {

    if (this.stepName == null)
      return null;

    return StepInstances.getInstance().getStep(this);
  }

  /**
   * Get the Eoulsan mode of the step.
   * @return an EoulsanMode enum
   */
  public EoulsanMode getEoulsanMode() {

    return this.mode;
  }

  @Override
  public String getStepName() {

    return this.stepName;
  }

  @Override
  public StepState getState() {

    return this.observer.getState();
  }

  @Override
  public Set<Parameter> getParameters() {

    return Collections.unmodifiableSet(this.parameters);
  }

  @Override
  public InputPorts getInputPorts() {

    return this.inputPortsParameter;
  }

  @Override
  public OutputPorts getOutputPorts() {

    return this.outputPortsParameter;
  }

  /**
   * Get the input ports.
   * @return the InputPorts object
   */
  WorkflowInputPorts getWorkflowInputPorts() {

    return this.inputPorts;
  }

  /**
   * Get the output ports.
   * @return the OutputPorts object
   */
  WorkflowOutputPorts getWorkflowOutputPorts() {

    return this.outputPorts;
  }

  @Override
  public StepResult getResult() {

    return this.result;
  }

  /**
   * Get step working directory (where output file of the step will be written).
   * @return the working directory
   */
  public DataFile getStepWorkingDir() {

    return this.workingDir;
  }

  /**
   * Test if output files of the steps must be copied to output directory.
   * @return true if output files of the steps must be copied to output
   *         directory
   */
  protected boolean isCopyResultsToOutput() {

    return this.copyResultsToOutput;
  }

  /**
   * Test if step log files must be created.
   * @return true if step log files must be created
   */
  boolean isCreateLogFiles() {

    return this.createLogFiles;
  }

  /**
   * Get the state observer object related to this step.
   * @return a WorkflowStepStateObserver
   */
  WorkflowStepStateObserver getStepStateObserver() {

    return this.observer;
  }

  //
  // Setters
  //

  /**
   * Set the state of the step.
   * @param state the new state of the step
   */
  public void setState(final StepState state) {

    this.observer.setState(state);
  }

  protected void registerInputAndOutputPorts(final Step step) {

    Preconditions.checkNotNull(step, "step cannot be null");

    // Get output ports
    this.outputPortsParameter = step.getOutputPorts();
    if (outputPorts != null)
      this.outputPorts = new WorkflowOutputPorts(this, outputPortsParameter);

    // Get input ports
    this.inputPortsParameter = step.getInputPorts();
    if (inputPorts != null)
      this.inputPorts = new WorkflowInputPorts(this, inputPortsParameter);
  }

  /**
   * Add a dependency for this step.
   * @param inputPort the input port provided by the dependency
   * @param outputPort the output port of the step
   */
  protected void addDependency(final WorkflowInputPort inputPort,
      final WorkflowOutputPort outputPort) {

    Preconditions.checkNotNull(inputPort, "inputPort argument cannot be null");
    Preconditions
        .checkNotNull(outputPort, "outputPort argument cannot be null");
    Preconditions
        .checkArgument(outputPort.getStep() == this,
            "input port ("
                + inputPort.getName() + ") is not a port of the step ("
                + getId() + ")");

    // Set the link
    inputPort.setLink(outputPort);
    outputPort.addLink(inputPort);

    // Add the dependency
    inputPort.getStep().addDependency(outputPort.getStep());
  }

  /**
   * Add a dependency for this step.
   * @param step the dependency
   */
  protected void addDependency(final AbstractWorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    // Check if try to link a step to itself
    if (step == this)
      throw new EoulsanRuntimeException("a step cannot depends on itself: "
          + step.getId());

    // Check if the step are in the same workflow
    if (this.getWorkflow() != step.getWorkflow())
      throw new EoulsanRuntimeException(
          "step dependency is not in the same workflow");

    // Add step dependency
    this.observer.addDependency(step);
  }

  /**
   * Define the working directory of the step.
   * @param workflow the workflow
   * @param step step instance
   * @return the working directory of the step
   */
  private static DataFile defineWorkingDirectory(
      final AbstractWorkflow workflow, final Step step,
      final boolean copyResultsToOutput) {

    final boolean hadoopMode = EoulsanRuntime.getRuntime().isHadoopMode();

    if (!hadoopMode) {

      if (copyResultsToOutput)
        return workflow.getOutputDir();

      return workflow.getLocalWorkingDir();
    }

    switch (EoulsanMode.getEoulsanMode(step.getClass())) {

    case HADOOP_COMPATIBLE:
      if (copyResultsToOutput)
        return workflow.getOutputDir();

      return workflow.getHadoopWorkingDir();

    case HADOOP_ONLY:
      return workflow.getHadoopWorkingDir();

    default:
      return workflow.getLocalWorkingDir();
    }

  }

  //
  // Step lifetime methods
  //

  /**
   * Configure the step.
   * @throws EoulsanException if an error occurs while configuring a step
   */
  protected void configure() throws EoulsanException {

    if (getState() != StepState.CREATED)
      throw new IllegalStateException("Illegal step state for configuration: "
          + getState());

    // Configure only standard steps and generator steps
    if (getType() == StepType.STANDARD_STEP
        || getType() == StepType.DESIGN_STEP
        || getType() == StepType.GENERATOR_STEP
        || getType() == StepType.DESIGN_STEP) {

      getLogger().info(
          "Configure "
              + getId() + " step with step parameters: " + getParameters());

      final Step step = getStep();
      if (getType() == StepType.STANDARD_STEP
          || getType() == StepType.DESIGN_STEP) {
        step.configure(getParameters());
      }

      // Register input and output formats
      registerInputAndOutputPorts(step);
    }

    setState(StepState.CONFIGURED);
  }

  /**
   * Run checker (runs only for Design step).
   */
  private void configureCheckerStep() {

    // TODO modify design step to configure also the checker step

    /* This method can only works with design step */
    if (getType() != CHECKER_STEP)
      return;

    // Get Checker step
    final CheckerStep checkerStep =
        (CheckerStep) StepInstances.getInstance().getStep(this);

    // Get the input files of the workflow
    // final Set<WorkflowStepOutputDataFile> files =
    // getWorkflow().getWorkflowFilesAtRootStep().getInputFiles();

    // Get design
    final Design design = this.getWorkflow().getDesign();

    // If no sample there is nothing to do
    if (design.getSampleCount() == 0)
      return;

    // Search to format to check
    // for (WorkflowStepOutputDataFile file : files)
    // if (file.getFormat().isChecker()) {
    //
    // // Add the checker to the list of checkers to launch
    // checkerStep.addChecker(file.getFormat().getChecker());
    // }

  }

  //
  // Token handling
  //

  /**
   * Send a token to the next steps.
   * @param token token to send
   */
  void sendToken(final Token token) {

    Preconditions.checkNotNull(token, "token cannot be null");

    final String outputPortName = token.getOrigin().getName();

    for (WorkflowInputPort inputPort : this.outputPorts.getPort(outputPortName)
        .getLinks()) {

      inputPort.getStep().postToken(inputPort, token);
    }
  }

  /**
   * Receive a token.
   * @param inputPort destination of the token
   * @param token the token
   */
  private void postToken(final WorkflowInputPort inputPort, final Token token) {

    Preconditions.checkNotNull(inputPort, "inputPort cannot be null");
    Preconditions.checkNotNull(token, "token cannot be null");

    TokenManagerRegistry.getInstance().getTokenManager(this)
        .postToken(inputPort, token);
  }

  //
  // Constructor
  //

  /**
   * Constructor that create a step with nothing to execute like ROOT_STEP,
   * DESIGN_STEP and FIRST_STEP.
   * @param workflow the workflow of the step
   * @param type the type of the step
   */
  public AbstractWorkflowStep(final AbstractWorkflow workflow,
      final StepType type) {

    Preconditions.checkArgument(type != StepType.STANDARD_STEP,
        "This constructor cannot be used for standard steps");
    Preconditions.checkArgument(type != StepType.GENERATOR_STEP,
        "This constructor cannot be used for standard steps");

    Preconditions.checkNotNull(workflow, "Workflow argument cannot be null");
    Preconditions.checkNotNull(type, "Type argument cannot be null");

    this.workflow = workflow;
    this.number = instanceCounter++;
    this.id = type.getDefaultStepId();
    this.skip = false;
    this.terminalStep = false;
    this.createLogFiles = false;
    this.type = type;
    this.parameters = Collections.emptySet();
    this.copyResultsToOutput = false;

    switch (type) {
    case CHECKER_STEP:

      // Create and register checker step
      final Step checkerStep = new CheckerStep();
      StepInstances.getInstance().registerStep(this, checkerStep);

      this.stepName = checkerStep.getName();
      this.mode = EoulsanMode.getEoulsanMode(checkerStep.getClass());

      // Define working directory
      this.workingDir =
          defineWorkingDirectory(workflow, checkerStep,
              this.copyResultsToOutput);
      break;

    case DESIGN_STEP:

      // Create and register checker step
      final Step checkerStep2 =
          StepInstances.getInstance().getStep(this.workflow.getCheckerStep());
      final Step designStep =
          new DesignStep(this.workflow.getDesign(), (CheckerStep) checkerStep2);
      StepInstances.getInstance().registerStep(this, designStep);

      this.stepName = designStep.getName();
      this.mode = EoulsanMode.getEoulsanMode(designStep.getClass());

      // Define working directory
      this.workingDir =
          defineWorkingDirectory(workflow, designStep, this.copyResultsToOutput);

      break;

    default:

      StepInstances.getInstance().registerStep(this, new FakeStep());

      this.stepName = type.name();
      this.mode = EoulsanMode.NONE;

      // Define working directory
      this.workingDir =
          defineWorkingDirectory(workflow, null, this.copyResultsToOutput);
      break;
    }

    // Set state observer
    this.observer = new WorkflowStepStateObserver(this);

    // Register this step in the workflow
    this.workflow.register(this);
  }

  /**
   * Create a Generator Workflow step.
   * @param workflow the workflow
   * @param format DataFormat
   * @throws EoulsanException if an error occurs while configuring the generator
   */
  public AbstractWorkflowStep(final AbstractWorkflow workflow,
      final DataFormat format) throws EoulsanException {

    Preconditions.checkNotNull(workflow, "Workflow argument cannot be null");
    Preconditions.checkNotNull(format, "Format argument cannot be null");

    final Step generator = format.getGenerator();
    StepInstances.getInstance().registerStep(this, generator);

    Preconditions.checkNotNull(generator, "The generator step is null");

    this.workflow = workflow;
    this.number = instanceCounter++;
    this.id = generator.getName();
    this.skip = false;
    this.terminalStep = false;
    this.createLogFiles = false;
    this.type = StepType.GENERATOR_STEP;
    this.stepName = generator.getName();
    this.mode = EoulsanMode.getEoulsanMode(generator.getClass());
    this.parameters = Collections.emptySet();
    this.copyResultsToOutput = false;

    // Define working directory
    this.workingDir =
        defineWorkingDirectory(workflow, generator, copyResultsToOutput);

    // Set state observer
    this.observer = new WorkflowStepStateObserver(this);

    // Register this step in the workflow
    this.workflow.register(this);
  }

  /**
   * Create a step for a standard step.
   * @param workflow workflow of the step
   * @param id identifier of the step
   * @param skip true to skip execution of the step
   * @param copyResultsToOutput copy step result to output directory
   * @param parameters parameters of the step
   * @throws EoulsanException id an error occurs while creating the step
   */
  protected AbstractWorkflowStep(final AbstractWorkflow workflow,
      final String id, final String stepName, final boolean skip,
      final boolean copyResultsToOutput, final Set<Parameter> parameters)
      throws EoulsanException {

    Preconditions.checkNotNull(workflow, "Workflow argument cannot be null");
    Preconditions.checkNotNull(id, "Step id argument cannot be null");
    Preconditions.checkNotNull(stepName, "Step name argument cannot be null");
    Preconditions.checkNotNull(parameters,
        "Step arguments argument cannot be null");

    this.workflow = workflow;
    this.number = instanceCounter++;
    this.id = id;
    this.skip = skip;
    this.type = StepType.STANDARD_STEP;
    this.stepName = stepName;
    this.copyResultsToOutput = copyResultsToOutput;

    // Load Step instance
    final Step step = StepInstances.getInstance().getStep(this, stepName);
    this.mode = EoulsanMode.getEoulsanMode(step.getClass());
    this.parameters = Sets.newLinkedHashSet(parameters);
    this.terminalStep = step.isTerminalStep();
    this.createLogFiles = step.isCreateLogFiles();

    // Define working directory
    this.workingDir =
        defineWorkingDirectory(workflow, step, copyResultsToOutput);

    // Set state observer
    this.observer = new WorkflowStepStateObserver(this);

    // Register this step in the workflow
    this.workflow.register(this);
  }

  /**
   * Create a step for a standard step.
   * @param workflow workflow of the step
   * @param id identifier of the step
   * @param skip true to skip execution of the step
   * @param copyResultsToOutput copy step result to output directory
   * @param parameters parameters of the step
   * @throws EoulsanException id an error occurs while creating the step
   */
  protected AbstractWorkflowStep(final AbstractWorkflow workflow,
      final String id, final String stepName, final boolean skip,
      final boolean copyResultsToOutput, final DataFile workingDir,
      final Set<Parameter> parameters) throws EoulsanException {

    Preconditions.checkNotNull(workflow, "Workflow argument cannot be null");
    Preconditions.checkNotNull(id, "Step id argument cannot be null");
    Preconditions.checkNotNull(stepName, "Step name argument cannot be null");
    Preconditions.checkNotNull(workingDir,
        "working directory argument cannot be null");
    Preconditions.checkNotNull(parameters,
        "Step arguments argument cannot be null");

    if (!(workingDir.equals(workflow.getOutputDir())
        || workingDir.equals(workflow.getLocalWorkingDir()) || workingDir
          .equals(workflow.getHadoopWorkingDir()))) {
      throw new IllegalArgumentException(
          "working dir is not the output/local/hadoop directory of the workflow: "
              + workingDir);
    }

    this.workflow = workflow;
    this.number = instanceCounter++;
    this.id = id;
    this.skip = skip;
    this.type = StepType.STANDARD_STEP;
    this.stepName = stepName;
    this.copyResultsToOutput = copyResultsToOutput;

    // Load Step instance
    final Step step = StepInstances.getInstance().getStep(this, stepName);
    this.mode = EoulsanMode.getEoulsanMode(step.getClass());
    this.parameters = Sets.newLinkedHashSet(parameters);
    this.terminalStep = step.isTerminalStep();
    this.createLogFiles = step.isCreateLogFiles();

    // Define working directory
    this.workingDir = workingDir;

    // Set state observer
    this.observer = new WorkflowStepStateObserver(this);

    // Register this step in the workflow
    this.workflow.register(this);
  }
}
