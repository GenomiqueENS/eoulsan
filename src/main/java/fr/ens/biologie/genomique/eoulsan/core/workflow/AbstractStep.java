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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.annotations.EoulsanAnnotationUtils.isGenerator;
import static fr.ens.biologie.genomique.eoulsan.annotations.EoulsanAnnotationUtils.isNoLog;
import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.noInputPort;
import static fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder.noOutputPort;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepType.GENERATOR_STEP;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepType.STANDARD_STEP;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Sets;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntimeException;
import fr.ens.biologie.genomique.eoulsan.annotations.EoulsanAnnotationUtils;
import fr.ens.biologie.genomique.eoulsan.annotations.ExecutionMode;
import fr.ens.biologie.genomique.eoulsan.checkers.Checker;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Module;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.ParallelizationMode;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.Workflow;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.modules.CheckerModule;
import fr.ens.biologie.genomique.eoulsan.modules.DesignModule;
import fr.ens.biologie.genomique.eoulsan.modules.FakeModule;

/**
 * This class define a step of the workflow. This class must be extended by a
 * class to be able to work with a specific workflow file format.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractStep implements Step {

  /** Serialization version UID. */
  private static final long serialVersionUID = 2040628014465126384L;

  private static AtomicInteger instanceCount = new AtomicInteger(0);

  private final AbstractWorkflow workflow;

  private final int number;
  private final String id;
  private final String version;
  private final StepType type;
  private final Set<Parameter> parameters;
  private final Checker checker;
  private ParallelizationMode parallelizationMode =
      ParallelizationMode.STANDARD;
  private InputPorts inputPortsParameter = noInputPort();
  private OutputPorts outputPortsParameter = noOutputPort();

  private final String moduleName;
  private final ExecutionMode mode;
  private boolean skip;
  private final DiscardOutput discardOutput;
  private final boolean terminalStep;
  private final boolean createLogFiles;

  private final int requiredMemory;
  private final int requiredProcessors;

  private StepOutputPorts outputPorts = StepOutputPorts.noOutputPort();
  private StepInputPorts inputPorts = StepInputPorts.noInputPort();

  private final DataProduct dataProduct = new DefaultDataProduct();
  private final String dataProductConfiguration;

  private final StepStateDependencies stepStateDependencies;

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
   * Get the underlying Module object.
   * @return the Module object
   */
  public Module getModule() {

    if (this.moduleName == null) {
      return null;
    }

    return StepInstances.getInstance().getModule(this);
  }

  /**
   * Get the Eoulsan mode of the step.
   * @return an EoulsanMode enum
   */
  public ExecutionMode getEoulsanMode() {

    return this.mode;
  }

  @Override
  public String getModuleName() {

    return this.moduleName;
  }

  @Override
  public StepState getState() {

    return this.stepStateDependencies != null
        ? this.stepStateDependencies.getState() : null;
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

  @Override
  public int getRequiredMemory() {

    return this.requiredMemory;
  }

  @Override
  public int getRequiredProcessors() {

    return this.requiredProcessors;
  }

  @Override
  public Checker getChecker() {
    return this.checker;
  }

  /**
   * Get the input ports.
   * @return the InputPorts object
   */
  StepInputPorts getWorkflowInputPorts() {

    return this.inputPorts;
  }

  /**
   * Get the output ports.
   * @return the OutputPorts object
   */
  StepOutputPorts getWorkflowOutputPorts() {

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
   * Test if step log files must be created.
   * @return true if step log files must be created
   */
  boolean isCreateLogFiles() {

    return this.createLogFiles;
  }

  /**
   * Get the state observer object related to this step.
   * @return a StepStateObserver
   */
  StepStateDependencies getStepStateDependencies() {

    return this.stepStateDependencies;
  }

  /**
   * Get the parallelization mode of the step.
   * @return a ParallelizationMode enum
   */
  public ParallelizationMode getParallelizationMode() {

    return this.parallelizationMode;
  }

  /**
   * Get the data product for the step.
   * @return the data product for the step
   */
  DataProduct getDataProduct() {
    return this.dataProduct;
  }

  /**
   * Get the discard output value.
   * @return the discard output value
   */
  public DiscardOutput getDiscardOutput() {
    return this.discardOutput;
  }

  //
  // Setters
  //

  /**
   * Set the state of the step.
   * @param state the new state of the step
   */
  private void setState(final StepState state) {

    requireNonNull(state, "state argument cannot be null");

    // Send the message
    WorkflowEventBus.getInstance().postStepStateChange(this, state);
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

  protected void registerInputAndOutputPorts(final Module module) {

    requireNonNull(module, "module cannot be null");

    // Get output ports
    this.outputPortsParameter = module.getOutputPorts();
    if (this.outputPorts != null) {
      this.outputPorts = new StepOutputPorts(this, this.outputPortsParameter);
    }

    // Get input ports
    this.inputPortsParameter = module.getInputPorts();
    if (this.inputPorts != null) {
      this.inputPorts = new StepInputPorts(this, this.inputPortsParameter);
    }
  }

  /**
   * Add a dependency for this step.
   * @param inputPort the input port provided by the dependency
   * @param dependencyOutputPort the output port of the step
   */
  protected void addDependency(final StepInputPort inputPort,
      final StepOutputPort dependencyOutputPort) {

    requireNonNull(inputPort, "inputPort argument cannot be null");
    requireNonNull(dependencyOutputPort,
        "dependencyOutputPort argument cannot be null");

    final AbstractStep step = inputPort.getStep();
    final AbstractStep dependencyStep = dependencyOutputPort.getStep();

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
  protected void addDependency(final AbstractStep step) {

    requireNonNull(step, "step argument cannot be null");

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
    this.stepStateDependencies.addDependency(step);
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

      final Module module = getModule();
      if (getType() == StepType.STANDARD_STEP
          || getType() == StepType.DESIGN_STEP || isGenerator(module)) {

        // Configure step
        module.configure(new StepConfigurationContextImpl(this),
            getParameters());

        // Update parallelization mode if step configuration requires it
        this.parallelizationMode = getParallelizationMode(module);
      }

      // Register input and output formats
      registerInputAndOutputPorts(module);
    }

    // Configure data product
    this.dataProduct.configure(this.dataProductConfiguration);
    getLogger().info("Use "
        + this.dataProduct.getName() + " data product for " + getId()
        + " step");

    setState(StepState.CONFIGURED);
  }

  //
  // Other methods
  //

  /**
   * Get the parallelization mode of a module.
   * @param module The module
   * @return a ParallelizationMode that cannot be null
   */
  private static ParallelizationMode getParallelizationMode(
      final Module module) {

    if (module != null) {
      final ParallelizationMode mode = module.getParallelizationMode();

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
  AbstractStep(final AbstractWorkflow workflow, final StepType type) {

    checkArgument(type != StepType.STANDARD_STEP,
        "This constructor cannot be used for standard steps");
    checkArgument(type != StepType.GENERATOR_STEP,
        "This constructor cannot be used for standard steps");

    requireNonNull(workflow, "Workflow argument cannot be null");
    requireNonNull(type, "Type argument cannot be null");

    this.workflow = workflow;
    this.number = instanceCount.incrementAndGet();
    this.id = type.getDefaultStepId();
    this.skip = false;
    this.terminalStep = false;
    this.createLogFiles = false;
    this.type = type;
    this.parameters = Collections.emptySet();
    this.parallelizationMode = ParallelizationMode.NOT_NEEDED;
    this.requiredMemory = -1;
    this.requiredProcessors = -1;
    this.dataProductConfiguration = "";
    this.discardOutput = DiscardOutput.SUCCESS;
    this.checker = null;

    switch (type) {
    case CHECKER_STEP:

      // Create and register checker step
      final Module checkerModule = new CheckerModule();
      StepInstances.getInstance().registerStep(this, checkerModule);

      this.moduleName = checkerModule.getName();
      this.version = checkerModule.getVersion().toString();
      this.mode = ExecutionMode.getExecutionMode(checkerModule.getClass());

      // Define output directory
      this.outputDir = StepOutputDirectory.getInstance()
          .defaultDirectory(workflow, this, checkerModule, false);
      break;

    case DESIGN_STEP:

      // Create and register checker step
      final Module checkerModule2 =
          StepInstances.getInstance().getModule(this.workflow.getCheckerStep());
      final Module designModule = new DesignModule(this.workflow.getDesign(),
          (CheckerModule) checkerModule2);
      StepInstances.getInstance().registerStep(this, designModule);

      this.moduleName = designModule.getName();
      this.version = checkerModule2.getVersion().toString();
      this.mode = ExecutionMode.getExecutionMode(designModule.getClass());

      // Define output directory
      this.outputDir = StepOutputDirectory.getInstance()
          .defaultDirectory(workflow, this, designModule, false);

      break;

    default:

      final Module fakeModule = new FakeModule();
      StepInstances.getInstance().registerStep(this, fakeModule);

      this.moduleName = type.name();
      this.version = fakeModule.getVersion().toString();
      this.mode = ExecutionMode.NONE;

      // Define output directory
      this.outputDir = StepOutputDirectory.getInstance()
          .defaultDirectory(workflow, this, fakeModule, false);
      break;
    }

    // Set state observer
    this.stepStateDependencies = new StepStateDependencies(this);

    // Register this step in the workflow
    this.workflow.register(this);
  }

  /**
   * Create a Generator Workflow step.
   * @param workflow the workflow
   * @param format DataFormat
   * @throws EoulsanException if an error occurs while configuring the generator
   */
  AbstractStep(final AbstractWorkflow workflow, final DataFormat format)
      throws EoulsanException {

    requireNonNull(workflow, "Workflow argument cannot be null");
    requireNonNull(format, "Format argument cannot be null");

    final Module generatorModule = format.getGenerator();
    StepInstances.getInstance().registerStep(this, generatorModule);

    requireNonNull(generatorModule, "The generator module is null");

    this.workflow = workflow;
    this.number = instanceCount.incrementAndGet();
    this.id = generatorModule.getName();
    this.skip = false;
    this.terminalStep = false;
    this.createLogFiles = false;
    this.type = StepType.GENERATOR_STEP;
    this.moduleName = generatorModule.getName();
    this.version = generatorModule.getVersion().toString();
    this.mode = ExecutionMode.getExecutionMode(generatorModule.getClass());
    this.parameters = Collections.emptySet();
    this.parallelizationMode = getParallelizationMode(generatorModule);
    this.requiredMemory = -1;
    this.requiredProcessors = -1;
    this.dataProductConfiguration = "";
    this.discardOutput = DiscardOutput.SUCCESS;
    this.checker = null;

    // Define output directory
    this.outputDir = StepOutputDirectory.getInstance()
        .defaultDirectory(workflow, this, generatorModule, false);

    // Set state observer
    this.stepStateDependencies = new StepStateDependencies(this);

    // Register this step in the workflow
    this.workflow.register(this);
  }

  /**
   * Create a step for a standard step.
   * @param workflow workflow of the step
   * @param id identifier of the step
   * @param moduleName module name
   * @param stepVersion step version
   * @param skip true to skip execution of the step
   * @param discardOutput discard value
   * @param parameters parameters of the step
   * @param requiredMemory required memory
   * @param requiredProcessors required processors
   * @param dataProduct data product
   * @throws EoulsanException id an error occurs while creating the step
   */
  AbstractStep(final AbstractWorkflow workflow, final String id,
      final String moduleName, final String stepVersion, final boolean skip,
      final DiscardOutput discardOutput, final Set<Parameter> parameters,
      final int requiredMemory, final int requiredProcessors,
      final String dataProduct) throws EoulsanException {

    this(workflow, id, moduleName, stepVersion, skip, discardOutput, parameters,
        requiredMemory, requiredProcessors, dataProduct, null);
  }

  /**
   * Create a step for a standard step.
   * @param workflow workflow of the step
   * @param id identifier of the step
   * @param moduleName module name
   * @param stepVersion step version
   * @param skip true to skip execution of the step
   * @param discardOutput discard output value
   * @param parameters parameters of the step
   * @param requiredMemory required memory
   * @param requiredProcessors required processors
   * @param dataProduct data product
   * @param outputDirectory output directory
   * @throws EoulsanException id an error occurs while creating the step
   */
  AbstractStep(final AbstractWorkflow workflow, final String id,
      final String moduleName, final String stepVersion, final boolean skip,
      final DiscardOutput discardOutput, final Set<Parameter> parameters,
      final int requiredMemory, final int requiredProcessors,
      final String dataProduct, final DataFile outputDirectory)
      throws EoulsanException {

    requireNonNull(workflow, "Workflow argument cannot be null");
    requireNonNull(id, "Step id argument cannot be null");
    requireNonNull(moduleName, "Step module argument cannot be null");
    requireNonNull(parameters, "parameters argument cannot be null");
    requireNonNull(dataProduct, "dataProduct argument cannot be null");

    this.workflow = workflow;
    this.number = instanceCount.incrementAndGet();
    this.id = id;
    this.skip = skip;
    this.moduleName = moduleName;
    this.version = stepVersion;
    this.discardOutput = discardOutput;
    this.requiredMemory = requiredMemory;
    this.requiredProcessors = requiredProcessors;
    this.dataProductConfiguration = dataProduct;

    // Load Step instance
    final Module module =
        StepInstances.getInstance().getModule(this, moduleName, stepVersion);
    this.type = isGenerator(module) ? GENERATOR_STEP : STANDARD_STEP;
    this.mode = ExecutionMode.getExecutionMode(module.getClass());
    this.parameters = Sets.newLinkedHashSet(parameters);
    this.terminalStep = EoulsanAnnotationUtils.isTerminal(module);
    this.createLogFiles = !isNoLog(module);
    this.parallelizationMode = getParallelizationMode(module);
    this.checker = module.getChecker();

    // Define output directory
    this.outputDir = outputDirectory != null
        ? outputDirectory : StepOutputDirectory.getInstance().defaultDirectory(
            workflow, this, module, discardOutput.isCopyResultsToOutput());

    // Set state observer
    this.stepStateDependencies = new StepStateDependencies(this);

    // Register this step in the workflow
    this.workflow.register(this);
  }

}
