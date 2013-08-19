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

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType.CHECKER_STEP;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.annotations.EoulsanMode;
import fr.ens.transcriptome.eoulsan.checkers.CheckerStep;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
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

  private final int number = instanceCounter++;
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

  private final Map<DataFormat, CompressionType> outputFormats = Maps
      .newHashMap();
  private final Map<DataFormat, EnumSet<CompressionType>> inputFormats = Maps
      .newHashMap();
  private final Map<DataFormat, InputDataFileLocation> inputFormatLocations =
      Maps.newHashMap();

  private final Set<DataFormat> requiredInputFormatsInWorkingDirectory = Sets
      .newHashSet();
  private final DataFile workingDir;

  private StepState stepState = StepState.CREATED;
  private StepResult result;

  //
  // Internal class
  //

  /**
   * This class allow to retrieve the DataFile that correspond to a input
   * DataFormat of the Workflow step.
   * @author Laurent Jourdren
   */
  private static final class InputDataFileLocation implements Serializable {

    /** Serialization version UID. */
    private static final long serialVersionUID = 5140719397219910909L;

    private final DataFormat format;
    private final AbstractWorkflowStep step;

    //
    // Other methods
    //

    /**
     * Count the number for DataFile available for a multifile DataFormat and a
     * Sample. This method works only for a multifile DataFormat.
     * @param sample sample
     * @return the DataFile for the sample
     */
    public int getDataFileCount(final Sample sample, final boolean existingFiles) {

      return WorkflowStepOutputDataFile.dataFileCount(this.step, this.format,
          sample, existingFiles);
    }

    /**
     * Get the DataFile.
     * @param sample sample
     * @param fileIndex file index for multifile data. (-1 = no file index)
     * @return the DataFile for the sample
     */
    public DataFile getDataFile(final Sample sample, final int fileIndex) {

      Preconditions.checkNotNull(sample, "Sample cannot be null");

      return this.step.getOutputDataFile(this.format, sample, fileIndex);
    }

    @Override
    public String toString() {

      return Objects.toStringHelper(this).add("format", format)
          .add("Step", this.step.getId()).toString();
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param step Workflow step
     * @param format format
     */
    public InputDataFileLocation(final DataFormat format,
        final AbstractWorkflowStep step) {

      Preconditions.checkNotNull(step, "Format cannot be null");
      Preconditions.checkNotNull(step, "Step cannot be null");

      this.format = format;
      this.step = step;
    }

  }

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
   * Get the input DataFormats.
   * @return a unmodifiable set with DataFormats
   */
  protected Set<DataFormat> getInputDataFormats() {

    return Collections.unmodifiableSet(this.inputFormats.keySet());
  }

  /**
   * Get the output DataFormats.
   * @return a unmodifiable set with DataFormats
   */
  protected Set<DataFormat> getOutputDataFormats() {

    return Collections.unmodifiableSet(this.outputFormats.keySet());
  }

  /**
   * Get an output format compression.
   * @param format the format
   * @return the compression type
   */
  protected CompressionType getOutputDataFormatCompression(
      final DataFormat format) {

    Preconditions.checkNotNull(format, "the format cannot be null");

    if (!this.outputFormats.containsKey(format))
      throw new IllegalArgumentException(
          "the format is not an output format of the step: " + format);

    return this.outputFormats.get(format);
  }

  /**
   * Get the compression allowed for an input format by the step.
   * @param format the format
   * @return an EnumSet with the compression types allowed
   */
  protected EnumSet<CompressionType> getInputDataFormatCompressionsAllowed(
      final DataFormat format) {

    Preconditions.checkNotNull(format, "the format cannot be null");

    if (!this.inputFormats.containsKey(format))
      throw new IllegalArgumentException(
          "the format is not an input format of the step: " + format);

    return EnumSet.copyOf(this.inputFormats.get(format));
  }

  /**
   * Get the input data format required in the working directory. This method
   * allow to declare the input files that need to be copied in the working
   * directory before starting the step. As an example, it is used to copy files
   * from a local file system to a distributed file system like HDFS. After that
   * mapreduce jobs can be efficiency launched.
   * @return a set with DataFormat or null if the step does not need any input
   *         format in the working directory.
   */
  protected Set<DataFormat> getInputFormatsRequieredInWorkingDirectory() {

    return this.requiredInputFormatsInWorkingDirectory;
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

  protected void registerInputAndOutputFormats(final Step step) {

    Preconditions.checkNotNull(step, "step cannot be null");

    // Get output formats
    final Set<DataFormat> outputFormats = step.getOutputFormats();
    if (outputFormats != null) {
      for (DataFormat format : outputFormats) {
        if (format != null) {

          CompressionType compression = step.getOutputFormatCompression(format);
          if (compression == null)
            compression = CompressionType.NONE;

          this.outputFormats.put(format, compression);
        }
      }
    }

    // Get input formats
    final Set<DataFormat> inputFormats = step.getInputFormats();
    if (inputFormats != null) {
      for (DataFormat format : inputFormats) {
        if (format != null) {

          EnumSet<CompressionType> compressionsAllowed =
              step.acceptInputFormatCompression(format);
          if (compressionsAllowed == null)
            compressionsAllowed = EnumSet.allOf(CompressionType.class);

          this.inputFormats.put(format, compressionsAllowed);
        }
      }
    }

    // Get input format required in working directory
    Set<DataFormat> inputFormatRequired =
        Sets.newHashSet(step.getInputFormatsRequieredInWorkingDirectory());
    if (inputFormatRequired != null) {
      // Put the values in a new HashSet
      inputFormatRequired = Sets.newHashSet(inputFormatRequired);
      // Keep only real input format of the step
      inputFormatRequired.retainAll(getInputDataFormats());
      this.requiredInputFormatsInWorkingDirectory.addAll(inputFormatRequired);
    }

  }

  //
  // DataFile methods
  //

  /**
   * Get an output file of the step.
   * @param format DataFormat of the output file
   * @param sample sample sample that correspond to the file
   * @return a DataFile object
   */
  DataFile getOutputDataFile(final DataFormat format, final Sample sample) {

    return new WorkflowStepOutputDataFile(this, format, sample).getDataFile();
  }

  /**
   * Get an output file of the step.
   * @param format DataFormat of the output file
   * @param sample sample sample that correspond to the file
   * @param fileIndex file index
   * @return a DataFile object
   */
  DataFile getOutputDataFile(final DataFormat format, final Sample sample,
      final int fileIndex) {

    return new WorkflowStepOutputDataFile(this, format, sample, fileIndex)
        .getDataFile();
  }

  /**
   * Get the file count for an output step of the step.
   * @param format DataFormat of the output file
   * @param sample sample sample that correspond to the file
   * @param existingFiles if true return the number of files that really exists
   *          otherwise the maximum of files.
   * @return the count of output DataFiles
   */
  int getOutputDataFileCount(final DataFormat format, final Sample sample,
      final boolean existingFiles) {

    return WorkflowStepOutputDataFile.dataFileCount(this, format, sample,
        existingFiles);
  }

  /**
   * Get a DataFile that correspond to a DataFormat and a Sample for this step.
   * @param format the input format
   * @param sample the sample
   * @return a DataFormat or null if the DataFormat is not available
   */
  DataFile getInputDataFile(final DataFormat format, final Sample sample) {

    return getInputDataFile(format, sample, -1);
  }

  /**
   * Get a DataFile that correspond to a DataFormat and a Sample for this step.
   * @param format the input format
   * @param sample the sample
   * @return a DataFormat or null if the DataFormat is not available
   */
  DataFile getInputDataFile(final DataFormat format, final Sample sample,
      final int fileIndex) {

    Preconditions.checkNotNull(format, "Format argument cannot be null");
    Preconditions.checkNotNull(sample, "Sample argument cannot be null");

    if (!this.inputFormatLocations.containsKey(format))
      throw new EoulsanRuntimeException("The "
          + format.getName() + " format is not an output format of the step "
          + getId());

    return this.inputFormatLocations.get(format).getDataFile(sample, fileIndex);
  }

  /**
   * Get the file count for an input step of the step.
   * @param format DataFormat of the input file
   * @param sample sample sample that correspond to the file
   * @param existingFiles if true return the number of files that really exists
   *          otherwise the maximum of files.
   * @return the count of intput DataFiles
   */
  int getInputDataFileCount(final DataFormat format, final Sample sample,
      final boolean existingFiles) {

    Preconditions.checkNotNull(format, "Format argument cannot be null");
    Preconditions.checkNotNull(sample, "Sample argument cannot be null");

    if (!this.inputFormatLocations.containsKey(format))
      throw new EoulsanRuntimeException("The "
          + format.getName() + " format is not an input format of the step "
          + getId());

    return this.inputFormatLocations.get(format).getDataFileCount(sample,
        existingFiles);
  }

  /**
   * Get the step that provided (as output) the input format of the step.
   * @param format the format to search
   * @return the step that provide the format as output
   */
  protected AbstractWorkflowStep getInputDataFormatStep(final DataFormat format) {

    Preconditions.checkNotNull(format, "format cannot be null");

    if (!this.inputFormatLocations.containsKey(format))
      return null;

    return this.inputFormatLocations.get(format).step;
  }

  /**
   * Test if a dependency is already set for an inputFormat.
   * @param format input format
   * @return true if the dependency is already set
   */
  protected boolean isDependencySet(DataFormat format) {

    return this.inputFormatLocations.containsKey(format);
  }

  /**
   * Add a dependency for this step.
   * @param format input format provided by the dependency
   * @param step the dependency
   */
  protected void addDependency(DataFormat format,
      final AbstractWorkflowStep step) {

    Preconditions.checkNotNull(step, "step  argument cannot be null");

    if ((step.getType() != StepType.DESIGN_STEP
        && step.getType() != StepType.GENERATOR_STEP && step.getType() != StepType.STANDARD_STEP)
        || !this.inputFormats.containsKey(format))
      throw new EoulsanRuntimeException("The dependency ("
          + step.getId() + ") do not provide data (" + format.getName() + ")");

    if (!this.inputFormatLocations.containsKey(format))
      this.inputFormatLocations.put(format, new InputDataFileLocation(format,
          step));

    addDependency(step);
  }

  /**
   * Add a dependency for this step.
   * @param step the dependency
   */
  protected void addDependency(final AbstractWorkflowStep step) {

    Preconditions.checkNotNull(step, "step  argument cannot be null");

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
      else
        return workflow.getLocalWorkingDir();
    }

    switch (EoulsanMode.getEoulsanMode(step.getClass())) {

    case HADOOP_COMPATIBLE:
      if (copyResultsToOutput)
        return workflow.getOutputDir();
      else
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
  abstract void configure() throws EoulsanException;

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
      break;

    default:
      result = null;
    }

    if (result != null)
      getLogger().info(
          "Process step "
              + getId() + " in "
              + StringUtils.toTimeHumanReadable(result.getDuration()) + " s.");

    setState(StepState.DONE);

    this.result = result;
    return result;
  }

  /**
   * Run checker (runs only for Design step).
   * @param status step status
   * @return a StepResult object
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
