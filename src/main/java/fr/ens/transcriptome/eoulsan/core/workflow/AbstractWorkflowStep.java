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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.annotations.EoulsanAnnotationUtils.isGenerator;
import static fr.ens.transcriptome.eoulsan.annotations.EoulsanAnnotationUtils.isNoLog;
import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.noInputPort;
import static fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder.noOutputPort;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType.GENERATOR_STEP;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType.STANDARD_STEP;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.annotations.EoulsanAnnotationUtils;
import fr.ens.transcriptome.eoulsan.annotations.EoulsanMode;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.ParallelizationMode;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.steps.CheckerStep;
import fr.ens.transcriptome.eoulsan.steps.DesignStep;
import fr.ens.transcriptome.eoulsan.steps.FakeStep;

/**
 * This class define a step of the workflow. This class must be extended by a
 * class to be able to work with a specific workflow file format.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractWorkflowStep implements WorkflowStep {

  /** Serialization version UID. */
  private static final long serialVersionUID = 2040628014465126384L;

  private static int instanceCounter;

  private final AbstractWorkflow workflow;

  private final int number;
  private final String id;
  private final String version;
  private final StepType type;
  private final Set<Parameter> parameters;
  private ParallelizationMode parallelizationMode =
      ParallelizationMode.STANDARD;
  private InputPorts inputPortsParameter = noInputPort();
  private OutputPorts outputPortsParameter = noOutputPort();

  private final String stepName;
  private final EoulsanMode mode;
  private boolean skip;
  private final boolean terminalStep;
  private final boolean copyResultsToOutput;
  private final boolean createLogFiles;

  private WorkflowOutputPorts outputPorts = WorkflowOutputPorts.noOutputPort();
  private WorkflowInputPorts inputPorts = WorkflowInputPorts.noInputPort();

  private final WorkflowStepStateObserver observer;

  private final DataFile outputDir;

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
  public String getStepVersion() {

    return this.version;
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

    if (this.stepName == null) {
      return null;
    }

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

    return this.observer != null ? this.observer.getState() : null;
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

  /**
   * Get step output directory (where output file of the step will be written).
   * @return the output directory
   */
  public DataFile getStepOutputDirectory() {

    return this.outputDir;
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

  /**
   * Get the parallelization mode of the step.
   * @return a ParallelizationMode enum
   */
  public ParallelizationMode getParallelizationMode() {

    return this.parallelizationMode;
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

  /**
   * Set the skip state of the step.
   * @param skipped the skipped state
   */
  void setSkipped(final boolean skipped) {

    checkArgument(this.type == StepType.GENERATOR_STEP,
        "The step is not a generator and cannot be skipped: " + getId());

    this.skip = skipped;
  }

  protected void registerInputAndOutputPorts(final Step step) {

    checkNotNull(step, "step cannot be null");

    // Get output ports
    this.outputPortsParameter = step.getOutputPorts();
    if (this.outputPorts != null) {
      this.outputPorts =
          new WorkflowOutputPorts(this, this.outputPortsParameter);
    }

    // Get input ports
    this.inputPortsParameter = step.getInputPorts();
    if (this.inputPorts != null) {
      this.inputPorts = new WorkflowInputPorts(this, this.inputPortsParameter);
    }
  }

  /**
   * Add a dependency for this step.
   * @param inputPort the input port provided by the dependency
   * @param dependencyOutputPort the output port of the step
   */
  protected void addDependency(final WorkflowInputPort inputPort,
      final WorkflowOutputPort dependencyOutputPort) {

    checkNotNull(inputPort, "inputPort argument cannot be null");
    checkNotNull(dependencyOutputPort,
        "dependencyOutputPort argument cannot be null");

    final AbstractWorkflowStep step = inputPort.getStep();
    final AbstractWorkflowStep dependencyStep = dependencyOutputPort.getStep();

    checkArgument(step == this,
        "input port ("
            + inputPort.getName() + ") is not a port of the step (" + getId()
            + ")");

    // Set the link
    inputPort.setLink(dependencyOutputPort);
    dependencyOutputPort.addLink(inputPort);

    // Add the dependency
    step.addDependency(dependencyStep);
  }

  /**
   * Add a dependency for this step.
   * @param step the dependency
   */
  protected void addDependency(final AbstractWorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    // Check if try to link a step to itself
    if (step == this) {
      throw new EoulsanRuntimeException(
          "a step cannot depends on itself: " + step.getId());
    }

    // Check if the step are in the same workflow
    if (this.getWorkflow() != step.getWorkflow()) {
      throw new EoulsanRuntimeException(
          "step dependency is not in the same workflow");
    }

    // Add step dependency
    this.observer.addDependency(step);
  }

  /**
   * Define the working directory of the step.
   * @param workflow the workflow
   * @param step step instance
   * @return the working directory of the step
   */
  private static DataFile defineOutputDirectory(final AbstractWorkflow workflow,
      final Step step, final boolean copyResultsToOutput) {

    checkNotNull(workflow, "workflow argument cannot be null");
    checkNotNull(step, "step argument cannot be null");

    final boolean hadoopMode = EoulsanRuntime.getRuntime().isHadoopMode();

    if (!hadoopMode) {

      if (copyResultsToOutput) {
        return workflow.getOutputDirectory();
      }

      return workflow.getLocalWorkingDirectory();
    }

    switch (EoulsanMode.getEoulsanMode(step.getClass())) {

    case HADOOP_COMPATIBLE:
    case HADOOP_INTERNAL:
    case HADOOP_ONLY:
      return workflow.getHadoopWorkingDirectory();

    case LOCAL_ONLY:
      if (copyResultsToOutput) {
        return workflow.getOutputDirectory();
      }

      return workflow.getLocalWorkingDirectory();

    default:
      return workflow.getLocalWorkingDirectory();
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

    if (getState() != StepState.CREATED) {
      throw new IllegalStateException(
          "Illegal step state for configuration: " + getState());
    }

    // Configure only standard steps and generator steps
    if (getType() == StepType.STANDARD_STEP
        || getType() == StepType.DESIGN_STEP
        || getType() == StepType.GENERATOR_STEP
        || getType() == StepType.CHECKER_STEP) {

      getLogger().info("Configure "
          + getId() + " step with step parameters: " + getParameters());

      final Step step = getStep();
      if (getType() == StepType.STANDARD_STEP
          || getType() == StepType.DESIGN_STEP || isGenerator(step)) {

        // Configure step
        step.configure(new WorkflowStepConfigurationContext(this),
            getParameters());

        // Update parallelization mode if step configuration requires it
        this.parallelizationMode = getParallelizationMode(step);
      }

      // Register input and output formats
      registerInputAndOutputPorts(step);
    }

    setState(StepState.CONFIGURED);
  }

  //
  // Token handling
  //

  /**
   * Send a token to the next steps.
   * @param token token to send
   */
  void sendToken(final Token token) {

    checkNotNull(token, "token cannot be null");

    final String outputPortName = token.getOrigin().getName();

    for (WorkflowInputPort inputPort : this.outputPorts.getPort(outputPortName)
        .getLinks()) {

      inputPort.getStep().postToken(inputPort, token);
    }

    // Log token sending
    TokenManagerRegistry.getInstance().getTokenManager(this)
        .logSendingToken(token.getOrigin(), token);
  }

  /**
   * Receive a token.
   * @param inputPort destination of the token
   * @param token the token
   */
  private void postToken(final WorkflowInputPort inputPort, final Token token) {

    checkNotNull(inputPort, "inputPort cannot be null");
    checkNotNull(token, "token cannot be null");

    TokenManagerRegistry.getInstance().getTokenManager(this)
        .postToken(inputPort, token);
  }

  //
  // Other methods
  //

  /**
   * Get the parallelization mode of a step.
   * @param step The step
   * @return a ParallelizationMode that cannot be null
   */
  private static ParallelizationMode getParallelizationMode(final Step step) {

    if (step != null) {
      final ParallelizationMode mode = step.getParallelizationMode();

      if (mode != null) {
        return mode;
      }
    }

    return ParallelizationMode.STANDARD;
  }

  //
  // Constructors
  //

  /**
   * Constructor that create a step with nothing to execute like ROOT_STEP,
   * DESIGN_STEP and FIRST_STEP.
   * @param workflow the workflow of the step
   * @param type the type of the step
   */
  public AbstractWorkflowStep(final AbstractWorkflow workflow,
      final StepType type) {

    checkArgument(type != StepType.STANDARD_STEP,
        "This constructor cannot be used for standard steps");
    checkArgument(type != StepType.GENERATOR_STEP,
        "This constructor cannot be used for standard steps");

    checkNotNull(workflow, "Workflow argument cannot be null");
    checkNotNull(type, "Type argument cannot be null");

    this.workflow = workflow;
    this.number = instanceCounter++;
    this.id = type.getDefaultStepId();
    this.skip = false;
    this.terminalStep = false;
    this.createLogFiles = false;
    this.type = type;
    this.parameters = Collections.emptySet();
    this.copyResultsToOutput = false;
    this.parallelizationMode = ParallelizationMode.NOT_NEEDED;

    switch (type) {
    case CHECKER_STEP:

      // Create and register checker step
      final Step checkerStep = new CheckerStep();
      StepInstances.getInstance().registerStep(this, checkerStep);

      this.stepName = checkerStep.getName();
      this.version = checkerStep.getVersion().toString();
      this.mode = EoulsanMode.getEoulsanMode(checkerStep.getClass());

      // Define output directory
      this.outputDir = defineOutputDirectory(workflow, checkerStep,
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
      this.version = checkerStep2.getVersion().toString();
      this.mode = EoulsanMode.getEoulsanMode(designStep.getClass());

      // Define output directory
      this.outputDir =
          defineOutputDirectory(workflow, designStep, this.copyResultsToOutput);

      break;

    default:

      final Step fakeStep = new FakeStep();
      StepInstances.getInstance().registerStep(this, fakeStep);

      this.stepName = type.name();
      this.version = fakeStep.getVersion().toString();
      this.mode = EoulsanMode.NONE;

      // Define output directory
      this.outputDir =
          defineOutputDirectory(workflow, fakeStep, this.copyResultsToOutput);
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

    checkNotNull(workflow, "Workflow argument cannot be null");
    checkNotNull(format, "Format argument cannot be null");

    final Step generator = format.getGenerator();
    StepInstances.getInstance().registerStep(this, generator);

    checkNotNull(generator, "The generator step is null");

    this.workflow = workflow;
    this.number = instanceCounter++;
    this.id = generator.getName();
    this.skip = false;
    this.terminalStep = false;
    this.createLogFiles = false;
    this.type = StepType.GENERATOR_STEP;
    this.stepName = generator.getName();
    this.version = generator.getVersion().toString();
    this.mode = EoulsanMode.getEoulsanMode(generator.getClass());
    this.parameters = Collections.emptySet();
    this.copyResultsToOutput = false;
    this.parallelizationMode = getParallelizationMode(generator);

    // Define output directory
    this.outputDir =
        defineOutputDirectory(workflow, generator, this.copyResultsToOutput);

    // Set state observer
    this.observer = new WorkflowStepStateObserver(this);

    // Register this step in the workflow
    this.workflow.register(this);
  }

  /**
   * Create a step for a standard step.
   * @param workflow workflow of the step
   * @param id identifier of the step
   * @param stepName step name
   * @param stepVersion step version
   * @param skip true to skip execution of the step
   * @param copyResultsToOutput copy step result to output directory
   * @param parameters parameters of the step
   * @throws EoulsanException id an error occurs while creating the step
   */
  protected AbstractWorkflowStep(final AbstractWorkflow workflow,
      final String id, final String stepName, final String stepVersion,
      final boolean skip, final boolean copyResultsToOutput,
      final Set<Parameter> parameters) throws EoulsanException {

    checkNotNull(workflow, "Workflow argument cannot be null");
    checkNotNull(id, "Step id argument cannot be null");
    checkNotNull(stepName, "Step name argument cannot be null");
    checkNotNull(parameters, "Step arguments argument cannot be null");

    this.workflow = workflow;
    this.number = instanceCounter++;
    this.id = id;
    this.skip = skip;
    this.stepName = stepName;
    this.version = stepVersion;
    this.copyResultsToOutput = copyResultsToOutput;

    // Load Step instance
    final Step step =
        StepInstances.getInstance().getStep(this, stepName, stepVersion);
    this.type = isGenerator(step) ? GENERATOR_STEP : STANDARD_STEP;
    this.mode = EoulsanMode.getEoulsanMode(step.getClass());
    this.parameters = Sets.newLinkedHashSet(parameters);
    this.terminalStep = EoulsanAnnotationUtils.isTerminal(step);
    this.createLogFiles = !isNoLog(step);
    this.parallelizationMode = getParallelizationMode(step);

    // Define output directory
    this.outputDir = defineOutputDirectory(workflow, step, copyResultsToOutput);

    // Set state observer
    this.observer = new WorkflowStepStateObserver(this);

    // Register this step in the workflow
    this.workflow.register(this);
  }

  protected AbstractWorkflowStep(final AbstractWorkflow workflow,
      final String id, final Step step, final boolean skip,
      final boolean copyResultsToOutput, final Set<Parameter> parameters)
          throws EoulsanException {

    checkNotNull(workflow, "Workflow argument cannot be null");
    checkNotNull(id, "Step id argument cannot be null");
    checkNotNull(step, "Step argument cannot be null");
    checkNotNull(parameters, "Step arguments argument cannot be null");

    this.workflow = workflow;
    this.number = instanceCounter++;
    this.id = id;
    this.skip = skip;
    this.stepName = step.getName();
    this.version =
        step.getVersion() == null ? null : step.getVersion().toString();
    this.copyResultsToOutput = copyResultsToOutput;

    this.type = isGenerator(step) ? GENERATOR_STEP : STANDARD_STEP;
    this.mode = EoulsanMode.getEoulsanMode(step.getClass());
    this.parameters = Sets.newLinkedHashSet(parameters);
    this.terminalStep = EoulsanAnnotationUtils.isTerminal(step);
    this.createLogFiles = !isNoLog(step);
    this.parallelizationMode = getParallelizationMode(step);

    // Define output directory
    this.outputDir = defineOutputDirectory(workflow, step, copyResultsToOutput);

    // Set state observer
    this.observer = new WorkflowStepStateObserver(this);

    // Register this step in the workflow
    this.workflow.register(this);
  }

}
