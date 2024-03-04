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

  //private static AtomicInteger instanceCount = new AtomicInteger(0);

  //private final AbstractWorkflow workflow;

  private final KStep step;
  private final Checker checker;


  //
  // Getters
  //


  public KStep getKStep() {

    return this.step;
  }

  @Override
  public Workflow getWorkflow() {

    return this.step.getWorkflow();
  }

  /**
   * Get the abstract workflow object.
   * @return the AbstractWorkflow object of the step
   */
  AbstractWorkflow getAbstractWorkflow() {

    return this.step.getAbstractWorkflow();
  }

  private KConfiguration getConfiguration() {
    return this.step.getConfiguration();
  }

  // @Override
  // public StepContext getContext() {
  //
  // return this.stepContext;
  // }

  @Override
  public int getNumber() {

    return this.step.getNumber();
  }

  @Override
  public String getId() {

    return this.step.getId();
  }

  @Override
  public String getStepVersion() {

    return this.step.getStepVersion();
  }

  @Override
  public boolean isSkip() {

    return this.step.isSkip();
  }

  /**
   * Test if the step is a terminal step.
   * @return true if the step is a terminal step
   */
  protected boolean isTerminalStep() {

    return this.step.isTerminalStep();
  }

  @Override
  public StepType getType() {

    return this.step.getType();
  }

  /**
   * Get the underlying Module object.
   * @return the Module object
   */
  public Module getModule() {

    if (!getConfiguration().containsKey("moduleName")) {
      return null;
    }

    return StepInstances.getInstance().getModule(this);
  }

  /**
   * Get the Eoulsan mode of the step.
   * @return an EoulsanMode enum
   */
  public ExecutionMode getEoulsanMode() {

    return ExecutionMode.valueOf(getConfiguration().get("mode"));
  }

  @Override
  public String getModuleName() {

    return this.step.getModuleName();
  }

  @Override
  public StepState getState() {

    return this.step.getState();
  }

  @Override
  public Set<Parameter> getParameters() {

    return this.step.getParameters();
  }

  @Override
  public InputPorts getInputPorts() {

    return this.step.getInputPorts();
  }

  @Override
  public OutputPorts getOutputPorts() {

    return this.step.getOutputPorts();
  }

  @Override
  public int getRequiredMemory() {

    return  getConfiguration().getInt("requiredMemory");
  }

  @Override
  public int getRequiredProcessors() {

    return getConfiguration().getInt("requiredProcessors");
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

    return this.step.getWorkflowInputPorts();
  }

  /**
   * Get the output ports.
   * @return the OutputPorts object
   */
  StepOutputPorts getWorkflowOutputPorts() {

    return this.step.getWorkflowOutputPorts();
  }

  /**
   * Get step output directory (where output file of the step will be written).
   * @return the output directory
   */
  public DataFile getStepOutputDirectory() {

    return new DataFile(getConfiguration().get("outputDir"));
  }

  /**
   * Test if step log files must be created.
   * @return true if step log files must be created
   */
  boolean isCreateLogFiles() {

    return getConfiguration().getBoolean("createLogFiles");
  }

  /**
   * Get the state observer object related to this step.
   * @return a StepStateObserver
   */
  StepStateDependencies getStepStateDependencies() {

    return this.step.getStepStateDependencies();
  }

  /**
   * Get the parallelization mode of the step.
   * @return a ParallelizationMode enum
   */
  public ParallelizationMode getParallelizationMode() {

    return ParallelizationMode.valueOf(getConfiguration().get("parallelizationMode"));
  }

  /**
   * Get the data product for the step.
   * @return the data product for the step
   */
  DataProduct getDataProduct() {
    return this.step.getDataProduct();
  }

  /**
   * Get the discard output value.
   * @return the discard output value
   */
  public DiscardOutput getDiscardOutput() {
    return this.step.getDiscardOutput();
  }

  //
  // Setters
  //

  /**
   * Set the state of the step.
   * @param state the new state of the step
   */
  private void setState(final StepState state) {

    this.step.setState(state);
  }

  /**
   * Set the skip state of the step.
   * @param skipped the skipped state
   */
  void setSkipped(final boolean skipped) {

    this.step.setSkipped(skipped);
  }

  protected void registerInputAndOutputPorts(final Module module) {

    this.step.registerInputAndOutputPorts(module);
  }

  /**
   * Add a dependency for this step.
   * @param inputPort the input port provided by the dependency
   * @param dependencyOutputPort the output port of the step
   */
  protected void addDependency(final StepInputPort inputPort,
      final StepOutputPort dependencyOutputPort) {

    this.step.addDependency(inputPort, dependencyOutputPort);
  }

  /**
   * Add a dependency for this step.
   * @param step the dependency
   */
  protected void addDependency(final AbstractStep step) {

    this.step.addDependency(step);
  }

  //
  // Step lifetime methods
  //

  /**
   * Configure the step.
   * @throws EoulsanException if an error occurs while configuring a step
   */
  protected void configure() throws EoulsanException {

    this.step.configure();
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

    String id = type.getDefaultStepId();
    boolean skip = false;
    boolean terminalStep = false;
    this.checker = null;
    Set<Parameter> parameters = Collections.emptySet();
    String dataProductConfiguration = "";
    this.step = new KStep(this, workflow,  id, skip, terminalStep, parameters, dataProductConfiguration);

    KConfiguration conf = getConfiguration();
    conf.set("discardOutput", DiscardOutput.SUCCESS.toString());
    conf.set("requiredMemory", -1);
    conf.set("requiredProcessors", -1);
    conf.set("createLogFiles", false);
    conf.set("type", type.toString());
    conf.set("parallelizationMode", ParallelizationMode.NOT_NEEDED.toString());
    conf.set("requiredMemory", -1);
    conf.set("requiredProcessors", -1);
    conf.set("discardOutput", DiscardOutput.SUCCESS.toString());


    switch (type) {
    case CHECKER_STEP:

      // Create and register checker step
      final Module checkerModule = new CheckerModule();
      StepInstances.getInstance().registerStep(this, checkerModule);

      conf.set("moduleName", checkerModule.getName());
      conf.set("version", checkerModule.getVersion().toString());
      conf.set("mode", ExecutionMode.getExecutionMode(checkerModule.getClass()).toString());

      // Define output directory
      conf.set("outputDir", StepOutputDirectory.getInstance()
          .defaultDirectory(workflow, this, checkerModule, false).toString());
      break;

    case DESIGN_STEP:

      // Create and register checker step
      final Module checkerModule2 =
          StepInstances.getInstance().getModule(workflow.getCheckerStep());
      final Module designModule = new DesignModule(workflow.getDesign(),
          (CheckerModule) checkerModule2);
      StepInstances.getInstance().registerStep(this, designModule);

      conf.set("moduleName", designModule.getName());
      conf.set("version", checkerModule2.getVersion().toString());
      conf.set("mode", ExecutionMode.getExecutionMode(designModule.getClass()).toString());

      // Define output directory
      conf.set("outputDir", StepOutputDirectory.getInstance()
          .defaultDirectory(workflow, this, designModule, false).toString());
      break;

    default:

      final Module fakeModule = new FakeModule();
      StepInstances.getInstance().registerStep(this, fakeModule);

      conf.set("moduleName", type.name());
      conf.set("version", fakeModule.getVersion().toString());
      conf.set("mode", ExecutionMode.NONE.toString());

      // Define output directory
      conf.set("outputDir", StepOutputDirectory.getInstance()
          .defaultDirectory(workflow, this, fakeModule, false).toString());
      break;
    }

    // Initialize step
    this.step.init();
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

    String id = generatorModule.getName();
    boolean skip = false;
    boolean terminalStep = false;
    Set<Parameter> parameters = Collections.emptySet();
    String dataProductConfiguration = "";
    this.checker = null;
    this.step = new KStep(this, workflow,  id, skip, terminalStep, parameters, dataProductConfiguration);

    KConfiguration conf = getConfiguration();
    conf.set("createLogFiles", false);
    conf.set("type", StepType.GENERATOR_STEP.toString());
    conf.set("moduleName", generatorModule.getName());
    conf.set("version", generatorModule.getVersion().toString());
    conf.set("mode",  ExecutionMode.getExecutionMode(generatorModule.getClass()).toString());
    conf.set("parallelizationMode", getParallelizationMode(generatorModule).toString());
    conf.set("requiredMemory", -1);
    conf.set("requiredProcessors", -1);
    conf.set("discardOutput", DiscardOutput.SUCCESS.toString());

    // Define output directory
    conf.set("outputDir", StepOutputDirectory.getInstance()
        .defaultDirectory(workflow, this, generatorModule, false).toString());

    // Initialize step
    this.step.init();
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

    // Load Step instance
    final Module module =
      StepInstances.getInstance().getModule(this, moduleName, stepVersion);

    boolean terminalStep = EoulsanAnnotationUtils.isTerminal(module);
    this.checker = module.getChecker();
    this.step = new KStep(this, workflow,  id, skip, terminalStep, parameters, dataProduct);

    KConfiguration conf = getConfiguration();
    conf.set("moduleName", moduleName);
    conf.set("version", stepVersion);
    conf.set("discardOutput", discardOutput.toString());
    conf.set("requiredMemory", requiredMemory);
    conf.set("requiredProcessors", requiredProcessors);
    conf.set("type", (isGenerator(module) ? GENERATOR_STEP : STANDARD_STEP).toString());
    conf.set("mode", ExecutionMode.getExecutionMode(module.getClass()).toString());
    conf.set("createLogFiles", !isNoLog(module));
    conf.set("parallelizationMode", getParallelizationMode(module).toString());

    // Define output directory
    conf.set("outputDir", (outputDirectory != null
        ? outputDirectory :
      StepOutputDirectory.getInstance().defaultDirectory(workflow, this, module, discardOutput.isCopyResultsToOutput())).toString());

    // Initialize step
    this.step.init();
  }
}
