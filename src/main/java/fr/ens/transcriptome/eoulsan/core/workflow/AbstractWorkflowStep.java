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
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

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
  private final WorkflowStepContext stepContext;

  private final int number;
  private final String id;
  private final StepType type;
  private final Set<Parameter> parameters;
  private final String stepName;
  private final EoulsanMode mode;
  private final boolean skip;
  private final boolean terminalStep;
  private final boolean copyResultsToOutput;
  private final boolean createLogFiles;

  private Set<AbstractWorkflowStep> requieredSteps = Sets.newHashSet();
  private Set<AbstractWorkflowStep> stepsToInform = Sets.newHashSet();

  private WorkflowOutputPorts outputPorts = WorkflowOutputPorts.noOutputPort();
  private WorkflowInputPorts inputPorts = WorkflowInputPorts.noInputPort();

  private final DataFile workingDir;

  private StepState stepState = StepState.CREATED;
  private StepResult result;

  //
  // Getters
  //

  @Override
  public Workflow getWorkflow() {

    return this.workflow;
  }

  @Override
  public StepContext getContext() {

    return this.stepContext;
  }

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

    return this.stepState;
  }

  @Override
  public Set<Parameter> getParameters() {

    return Collections.unmodifiableSet(this.parameters);
  }

  /**
   * Get the input ports.
   * @return the InputPorts object
   */
  protected WorkflowInputPorts getInputPorts() {

    return this.inputPorts;
  }

  /**
   * Get the output ports.
   * @return the OutputPorts object
   */
  protected WorkflowOutputPorts getOutputPorts() {

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

  //
  // Setters
  //

  /**
   * Set the state of the step.
   * @param state the new state of the step
   */
  public void setState(final StepState state) {

    if (state == null)
      return;

    // If is the root step, there is nothing to wait
    if (this.type == StepType.ROOT_STEP && state == StepState.WAITING)
      this.stepState = StepState.READY;
    else
      this.stepState = state;

    // Inform step that depend of this step
    if (this.stepState == StepState.DONE)
      for (AbstractWorkflowStep step : this.stepsToInform)
        step.updateStatus();

    // Inform workflow object
    this.workflow.updateStepState(this);

    // Inform listeners
    WorkflowStepEventRelay.getInstance().updateStepState(this);
  }

  protected void registerInputAndOutputPorts(final Step step) {

    Preconditions.checkNotNull(step, "step cannot be null");

    // Get output ports
    final OutputPorts outputPorts = step.getOutputFormats();
    if (outputPorts != null)
      this.outputPorts = new WorkflowOutputPorts(this, outputPorts);

    // Get input ports
    final InputPorts inputPorts = step.getInputFormats();
    if (inputPorts != null)
      if (inputPorts != null)
        this.inputPorts = new WorkflowInputPorts(this, inputPorts);
  }

  protected void registerDesignOutputPorts() {

    final DataFormatRegistry registry = DataFormatRegistry.getInstance();
    final Design design = getWorkflow().getDesign();

    if (design.getSampleCount() == 0)
      return;

    final Sample firstSample = design.getSamples().get(0);
    final Set<WorkflowOutputPort> ports = Sets.newHashSet();

    for (String fieldName : firstSample.getMetadata().getFields()) {

      final DataFormat format = registry.getDataFormatForDesignField(fieldName);

      if (format != null)
        ports.add(new WorkflowOutputPort(this, format.getName(), format,
            CompressionType.NONE));
    }

    this.outputPorts = new WorkflowOutputPorts(ports);
  }

  //
  // DataFile methods
  //

  /**
   * Get a WorkflowOutputPort from its name
   * @param outputPortName the output port name
   * @return a WorkflowOutputPort object
   */
  private WorkflowOutputPort getOutputPort(final String outputPortName) {

    Preconditions.checkNotNull(outputPortName, "outputPortName cannot be null");
    Preconditions.checkArgument(this.outputPorts.contains(outputPortName),
        "Unknown output port for step \"" + getId() + ": " + outputPortName);

    return this.outputPorts.getPort(outputPortName);
  }

  /**
   * Get an output file of the step.
   * @param portName name of the output port that generate file
   * @param sample sample sample that correspond to the file
   * @return a DataFile object
   */
  DataFile getOutputDataFile(final String portName, final Sample sample) {

    return new WorkflowStepOutputDataFile(getOutputPort(portName), sample)
        .getDataFile();
  }

  /**
   * Get an output file of the step.
   * @param portName name of the output port that generate file
   * @param sample sample sample that correspond to the file
   * @param fileIndex file index
   * @return a DataFile object
   */
  DataFile getOutputDataFile(final String portName, final Sample sample,
      final int fileIndex) {

    return new WorkflowStepOutputDataFile(getOutputPort(portName), sample,
        fileIndex).getDataFile();
  }

  /**
   * Get the file count for an output step of the step.
   * @param portName name of the output port that generate file
   * @param sample sample sample that correspond to the file
   * @param existingFiles if true return the number of files that really exists
   *          otherwise the maximum of files.
   * @return the count of output DataFiles
   */
  int getOutputDataFileCount(final String portName, final Sample sample,
      final boolean existingFiles) {

    return WorkflowStepOutputDataFile.dataFileCount(getOutputPort(portName),
        sample, existingFiles);
  }

  /**
   * Get a DataFile that correspond to a port and a Sample for this step.
   * @param portName name of the output port that generate file
   * @param sample the sample
   * @return a DataFile or null if the port is not available
   */
  DataFile getInputDataFile(final String portName, final Sample sample) {

    return getInputDataFile(portName, sample, -1);
  }

  /**
   * Get a DataFile that correspond to a port and a Sample for this step.
   * @param portName name of the output port that generate file
   * @param sample the sample
   * @return a DataFile or null if the port is not available
   */
  DataFile getInputDataFile(final String portName, final Sample sample,
      final int fileIndex) {

    Preconditions.checkNotNull(portName, "PortName argument cannot be null");
    Preconditions.checkNotNull(sample, "Sample argument cannot be null");

    // Check if the port exists
    if (!getInputPorts().contains(portName))
      throw new EoulsanRuntimeException(
          "the step does not contains input port named: " + portName);

    final WorkflowInputPort port = getInputPorts().getPort(portName);

    // Check if the port is linked
    if (!port.isLinked())
      throw new EoulsanRuntimeException("the port \""
          + portName + "\" of the step \"" + getId() + "\" is not linked.");

    return port.getLink().getDataFile(sample, fileIndex);
  }

  /**
   * Get the file count for an input step of the step.
   * @param portName name of the output port that generate file
   * @param sample sample sample that correspond to the file
   * @param existingFiles if true return the number of files that really exists
   *          otherwise the maximum of files.
   * @return the count of intput DataFiles
   */
  int getInputDataFileCount(final String portName, final Sample sample,
      final boolean existingFiles) {

    checkNotNull(portName, "PortName argument cannot be null");
    checkNotNull(sample, "Sample argument cannot be null");

    // Check if the port exists
    if (!getInputPorts().contains(portName))
      throw new EoulsanRuntimeException(
          "the step does not contains input port named: " + portName);

    final WorkflowInputPort port = getInputPorts().getPort(portName);

    // Check if the port is linked
    if (!port.isLinked())
      throw new EoulsanRuntimeException("the port \""
          + portName + "\" of the step \"" + getId() + "\" is not linked.");

    return port.getLink().getDataFileCount(sample, existingFiles);
  }

  /**
   * Add a dependency for this step.
   * @param outputPortName name of the output port provided by the dependency
   * @param step the dependency
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

    // Set the set link
    inputPort.setLink(outputPort);

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
    this.requieredSteps.add(step);
    step.stepsToInform.add(this);
  }

  /**
   * Define the working directory of the step.
   * @param workflow the workflow
   * @param step step instance
   * @return the working directory of the step
   */
  private static final DataFile defineWorkingDirectory(
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
   * Update the status of the step to READY if all the dependency of this step
   * are in DONE state.
   */
  private void updateStatus() {

    for (AbstractWorkflowStep step : this.requieredSteps)
      if (step.getState() != StepState.DONE)
        return;

    setState(StepState.READY);
  }

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
        || getType() == StepType.GENERATOR_STEP) {

      getLogger().info(
          "Configure "
              + getId() + " step with step parameters: " + getParameters());

      final Step step = getStep();
      if (getType() == StepType.STANDARD_STEP)
        step.configure(getParameters());

      // Register input and output formats
      registerInputAndOutputPorts(step);
    }

    // Create output port of design step
    if (getType() == StepType.DESIGN_STEP) {

      // Register output port
      registerDesignOutputPorts();
    }

    setState(StepState.CONFIGURED);
  }

  /**
   * Execute the step.
   * @return a StepResult object
   */
  StepResult execute() {

    if (getState() != StepState.READY)
      throw new IllegalStateException("Illegal step state for execution: "
          + getState());

    getLogger().info("Start " + getId() + " step.");

    setState(StepState.WORKING);

    final StepStatus status = new WorkflowStepStatus(this);
    final StepResult result;

    switch (getType()) {

    case CHECKER_STEP:

      // Checker can only be configured after execution of configure() on other
      // step.
      configureCheckerStep(status);

    case STANDARD_STEP:
    case GENERATOR_STEP:

      final Step step = StepInstances.getInstance().getStep(this);

      result = step.execute(this.workflow.getDesign(), getContext(), status);

      getLogger().info(
          "Process step "
              + getId() + " in "
              + StringUtils.toTimeHumanReadable(result.getDuration()) + " s.");

      setState(result.isSuccess() ? StepState.DONE : StepState.FAIL);

      break;

    default:
      result = null;
      setState(StepState.DONE);
    }

    getLogger().info("End " + getId() + " step.");
    this.result = result;
    return result;
  }

  /**
   * Run checker (runs only for Design step).
   * @param status step status
   */
  private void configureCheckerStep(final StepStatus status) {

    // This method can only works with design step
    if (getType() != CHECKER_STEP)
      return;

    // Get Checker step
    final CheckerStep checkerStep =
        (CheckerStep) StepInstances.getInstance().getStep(this);

    // Get the input files of the workflow
    final Set<WorkflowStepOutputDataFile> files =
        getWorkflow().getWorkflowFilesAtRootStep().getInputFiles();

    // Get design
    final Design design = this.getWorkflow().getDesign();

    // If no sample there is nothing to do
    if (design.getSampleCount() == 0)
      return;

    // Search to format to check
    for (WorkflowStepOutputDataFile file : files)
      if (file.getFormat().isChecker()) {

        // Add the checker to the list of checkers to launch
        checkerStep.addChecker(file.getFormat().getChecker());
      }

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
    this.stepContext =
        new WorkflowStepContext(workflow.getWorkflowContext(), this);
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

    default:
      this.stepName = null;
      this.mode = EoulsanMode.NONE;

      // Define working directory
      this.workingDir =
          defineWorkingDirectory(workflow, null, this.copyResultsToOutput);
      break;
    }

    // Register this step in the workflow
    this.workflow.register(this);
  }

  /**
   * Create a Generator Workflow step.
   * @param design design object
   * @param stepContext context object
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
    this.stepContext =
        new WorkflowStepContext(workflow.getWorkflowContext(), this);
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

    // Register this step in the workflow
    this.workflow.register(this);
  }

  /**
   * Create a step for a standard step.
   * @param workflow workflow of the step
   * @param id identifier of the step
   * @param step Step object
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
    this.stepContext =
        new WorkflowStepContext(workflow.getWorkflowContext(), this);
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

    // Register this step in the workflow
    this.workflow.register(this);
  }

  /**
   * Create a step for a standard step.
   * @param workflow workflow of the step
   * @param id identifier of the step
   * @param step Step object
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
    this.stepContext =
        new WorkflowStepContext(workflow.getWorkflowContext(), this);
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

    // Register this step in the workflow
    this.workflow.register(this);
  }
}
