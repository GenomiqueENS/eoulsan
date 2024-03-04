package fr.ens.biologie.genomique.eoulsan.core.workflow;

import com.google.common.collect.Sets;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntimeException;
import fr.ens.biologie.genomique.eoulsan.core.Module;
import fr.ens.biologie.genomique.eoulsan.core.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.annotations.EoulsanAnnotationUtils.isGenerator;
import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.noInputPort;
import static fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder.noOutputPort;
import static java.util.Objects.requireNonNull;

public class KStep implements Serializable {

  private static AtomicInteger instanceCount = new AtomicInteger(0);

  private final AbstractStep step;

  private final AbstractWorkflow workflow;

  private final int number;
  private final String id;

  private final KConfiguration conf = new KConfiguration();

  private final Set<Parameter> parameters;

  private InputPorts inputPortsParameter = noInputPort();
  private OutputPorts outputPortsParameter = noOutputPort();

  private boolean skip;

  private final boolean terminalStep;

  private StepOutputPorts outputPorts = StepOutputPorts.noOutputPort();
  private StepInputPorts inputPorts = StepInputPorts.noInputPort();

  private final DataProduct dataProduct = new DefaultDataProduct();
  private final String dataProductConfiguration;

  private final StepStateDependencies stepStateDependencies;

  //
  // Getters
  //

  public KConfiguration getConfiguration() {
    return this.conf;
  }

  public AbstractStep getStep() {

    return this.step;
  }

  public Workflow getWorkflow() {

    return this.workflow;
  }

  /**
   * Get the abstract workflow object.
   *
   * @return the AbstractWorkflow object of the step
   */
  public AbstractWorkflow getAbstractWorkflow() {

    return this.workflow;
  }

  public int getNumber() {

    return this.number;
  }

  public String getId() {

    return this.id;
  }

  public String getStepVersion() {

    return this.conf.get("version");
  }

  public boolean isSkip() {

    return this.skip;
  }

  /**
   * Test if the step is a terminal step.
   *
   * @return true if the step is a terminal step
   */
  protected boolean isTerminalStep() {

    return this.terminalStep;
  }

  public Step.StepType getType() {

    return Step.StepType.valueOf(conf.get("type"));
  }

  public String getModuleName() {

    return this.conf.get("moduleName");
  }

  public Step.StepState getState() {

    return this.stepStateDependencies != null ?
      this.stepStateDependencies.getState() : null;
  }

  public Set<Parameter> getParameters() {

    return Collections.unmodifiableSet(this.parameters);
  }

  public InputPorts getInputPorts() {

    return this.inputPortsParameter;
  }

  public OutputPorts getOutputPorts() {

    return this.outputPortsParameter;
  }

  /**
   * Get the input ports.
   *
   * @return the InputPorts object
   */
  public StepInputPorts getWorkflowInputPorts() {

    return this.inputPorts;
  }

  /**
   * Get the output ports.
   *
   * @return the OutputPorts object
   */
  public StepOutputPorts getWorkflowOutputPorts() {

    return this.outputPorts;
  }

  /**
   * Get the state observer object related to this step.
   *
   * @return a StepStateObserver
   */
  public StepStateDependencies getStepStateDependencies() {

    return this.stepStateDependencies;
  }

  /**
   * Get the data product for the step.
   *
   * @return the data product for the step
   */
  DataProduct getDataProduct() {
    return this.dataProduct;
  }

  /**
   * Get the discard output value.
   *
   * @return the discard output value
   */
  public Step.DiscardOutput getDiscardOutput() {
    return Step.DiscardOutput.valueOf(conf.get("discardOutput"));
  }

  //
  // Setters
  //

  /**
   * Set the state of the step.
   *
   * @param state the new state of the step
   */
  public void setState(final Step.StepState state) {

    requireNonNull(state, "state argument cannot be null");

    // Send the message
    WorkflowEventBus.getInstance().postStepStateChange(this.step, state);
  }

  /**
   * Set the skip state of the step.
   *
   * @param skipped the skipped state
   */
  void setSkipped(final boolean skipped) {

    checkArgument(getType() == Step.StepType.GENERATOR_STEP,
      "The step is not a generator and cannot be skipped: " + getId());

    this.skip = skipped;
  }

  protected void registerInputAndOutputPorts(
    final fr.ens.biologie.genomique.eoulsan.core.Module module) {

    requireNonNull(module, "module cannot be null");

    // Get output ports
    this.outputPortsParameter = module.getOutputPorts();
    if (this.outputPorts != null) {
      this.outputPorts =
        new StepOutputPorts(this.step, this.outputPortsParameter);
    }

    // Get input ports
    this.inputPortsParameter = module.getInputPorts();
    if (this.inputPorts != null) {
      this.inputPorts = new StepInputPorts(this.step, this.inputPortsParameter);
    }
  }

  /**
   * Add a dependency for this step.
   *
   * @param inputPort            the input port provided by the dependency
   * @param dependencyOutputPort the output port of the step
   */
  protected void addDependency(final StepInputPort inputPort,
    final StepOutputPort dependencyOutputPort) {

    requireNonNull(inputPort, "inputPort argument cannot be null");
    requireNonNull(dependencyOutputPort,
      "dependencyOutputPort argument cannot be null");

    final AbstractStep step = inputPort.getStep();
    final AbstractStep dependencyStep = dependencyOutputPort.getStep();

    checkArgument(step == this.step,
      "input port (" + inputPort.getName() + ") is not a port of the step ("
        + getId() + ")");

    // Set the link
    inputPort.setLink(dependencyOutputPort);
    dependencyOutputPort.addLink(inputPort);

    // Add the dependency
    step.addDependency(dependencyStep);
  }

  /**
   * Add a dependency for this step.
   *
   * @param step the dependency
   */
  protected void addDependency(final AbstractStep step) {

    requireNonNull(step, "step argument cannot be null");

    // Check if try to link a step to itself
    if (step == this.step) {
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
   *
   * @throws EoulsanException if an error occurs while configuring a step
   */
  public void configure() throws EoulsanException {

    if (getState() != Step.StepState.CREATED) {
      throw new IllegalStateException(
        "Illegal step state for configuration: " + getState());
    }

    // Configure only standard steps and generator steps
    if (getType() == Step.StepType.STANDARD_STEP
      || getType() == Step.StepType.DESIGN_STEP
      || getType() == Step.StepType.GENERATOR_STEP
      || getType() == Step.StepType.CHECKER_STEP) {

      getLogger().info("Configure " + getId() + " step with step parameters: "
        + getParameters());

      final fr.ens.biologie.genomique.eoulsan.core.Module module =
        this.step.getModule();
      if (getType() == Step.StepType.STANDARD_STEP
        || getType() == Step.StepType.DESIGN_STEP || isGenerator(module)) {

        // Configure step
        module.configure(new StepConfigurationContextImpl(this.step),
          getParameters());

        // Update parallelization mode if step configuration requires it
        this.conf.set("parallelizationMode",
          getParallelizationMode(module).toString());
      }

      // Register input and output formats
      registerInputAndOutputPorts(module);
    }

    // Configure data product
    this.dataProduct.configure(this.dataProductConfiguration);
    getLogger().info(
      "Use " + this.dataProduct.getName() + " data product for " + getId()
        + " step");

    // Seal the configuration
    this.conf.seal();

    setState(Step.StepState.CONFIGURED);
  }

  public void init() {


    // Set state observer
    this.stepStateDependencies.init();

    // Register this step in the workflow
    this.workflow.register(this.step);
  }

  //
  // Other methods
  //

  /**
   * Get the parallelization mode of a module.
   *
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
  // Constructor
  //

  public KStep(final AbstractStep step, final AbstractWorkflow workflow,
    final String id, final boolean skip,
    final boolean terminalStep, final Set<Parameter> parameters,
    final String dataProduct) {

    requireNonNull(step, "Step argument cannot be null");
    requireNonNull(workflow, "Workflow argument cannot be null");
    requireNonNull(id, "Step id argument cannot be null");
    requireNonNull(parameters, "parameters argument cannot be null");
    requireNonNull(dataProduct, "dataProduct argument cannot be null");

    this.step = step;
    this.workflow = workflow;
    this.number = instanceCount.incrementAndGet();
    this.id = id;
    this.skip = skip;
    this.terminalStep = terminalStep;

    this.dataProductConfiguration = dataProduct;

    // Set parameters
    this.parameters = Sets.newLinkedHashSet(parameters);

    // Set state observer
    // TODO Enable once StepStateDependencies can handle KStep
    this.stepStateDependencies = new StepStateDependencies(this);

    // Register this step in the workflow
    // TODO Enable once AbstractWorkflow can handle KStep
    //this.workflow.register(this.step);
  }

}
