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

import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType.DESIGN_STEP;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.checkers.CheckStore;
import fr.ens.transcriptome.eoulsan.checkers.Checker;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define a step of the workflow. This class must be extended by a
 * class to be able to work with a specific worklow file format.
 * @author Laurent Jourdren
 * @since 1.3
 */
public abstract class AbstractWorkflowStep implements WorkflowStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static int instanceCounter;

  private final AbstractWorkflow workflow;
  private final int number = instanceCounter++;
  private final String id;
  private final StepType type;
  private final Set<Parameter> parameters;
  private final Step step;
  private final boolean skip;

  private Set<AbstractWorkflowStep> requieredSteps = Sets.newHashSet();
  private Set<AbstractWorkflowStep> stepsToInform = Sets.newHashSet();

  private final Set<DataFormat> outputFormats = Sets.newHashSet();
  private final Set<DataFormat> inputFormats = Sets.newHashSet();
  private final Map<DataFormat, InputDataFileLocation> inputFormatLocations =
      Maps.newHashMap();

  private StepState stepState = StepState.CREATED;
  private long duration = -1;

  //
  // Internal class
  //

  /**
   * This class allow to retrieve the DataFile that correspond to a input
   * DataFormat of the Workflow step.
   * @author Laurent Jourdren
   */
  private static final class InputDataFileLocation {

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

  @Override
  public StepType getType() {

    return this.type;
  }

  public Step getStep() {

    return this.step;
  }

  @Override
  public String getStepName() {

    return this.step == null ? null : this.step.getName();
  }

  @Override
  public StepState getState() {

    return this.stepState;
  }

  @Override
  public long getDuration() {

    return this.duration;
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

    return Collections.unmodifiableSet(this.inputFormats);
  }

  /**
   * Get the output DataFormats.
   * @return a unmodifiable set with DataFormats
   */
  protected Set<DataFormat> getOutputDataFormats() {

    return Collections.unmodifiableSet(this.outputFormats);
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
  }

  /**
   * Register input DataFormat.
   * @param format format to register
   */
  protected void registerInputFormat(final DataFormat format) {

    Preconditions.checkNotNull(format, "format cannot be null");

    this.inputFormats.add(format);
  }

  /**
   * Register input DataFormat.
   * @param format format to register
   */
  protected void registerOutputFormat(final DataFormat format) {

    Preconditions.checkNotNull(format, "format cannot be null");

    this.outputFormats.add(format);
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
          + format.getFormatName()
          + " format is not an output format of the step " + getId());

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
          + format.getFormatName()
          + " format is not an input format of the step " + getId());

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
        || !this.inputFormats.contains(format))
      throw new EoulsanRuntimeException("The dependency ("
          + step.getId() + ") do not provide data (" + format.getFormatName()
          + ")");

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

  StepResult execute() {

    if (getState() != StepState.READY)
      throw new IllegalStateException("Illegal step state for execution: "
          + getState());

    LOGGER.info("Start " + getId() + " step.");

    setState(StepState.WORKING);

    // Set the current step in the context
    this.workflow.getWorkflowContext().setStep(this);
    final StepResult result;

    Stopwatch stopwatch = new Stopwatch();
    stopwatch.start();

    switch (getType()) {

    case STANDARD_STEP:
    case GENERATOR_STEP:

      result =
          step.execute(this.workflow.getDesign(), this.workflow.getContext());
      break;

    case DESIGN_STEP:

      result = runCheckers();
      break;

    default:
      result = null;
    }

    this.duration = stopwatch.elapsedMillis();

    LOGGER.info("Process step "
        + getId() + " in "
        + StringUtils.toTimeHumanReadable(this.duration / 1000000) + " s.");

    setState(StepState.DONE);

    return result;
  }

  /**
   * Run checker (runs only for Design step).
   * @return a StepResult object
   */
  private StepResult runCheckers() {

    // This method can only works with design step
    if (getType() != DESIGN_STEP)
      return null;

    // Get the input files of the workflow
    final Set<WorkflowStepOutputDataFile> files =
        getWorkflow().getWorkflowFilesAtRootStep().getInputFiles();

    // Get the context
    final WorkflowContext context = this.workflow.getWorkflowContext();

    // Get design
    final Design design = this.getWorkflow().getDesign();

    // If no sample there is nothing to do
    if (design.getSampleCount() == 0)
      return null;

    // Get first sample
    final Sample firstSample = design.getSamples().get(0);

    // Get the checkstore
    final CheckStore checkStore = CheckStore.getCheckStore();

    try {

      // Search to format to check
      for (WorkflowStepOutputDataFile file : files)
        if (file.getFormat().isChecker()) {

          // Change current step for the context
          context.setStep(file.getStep());

          // Get the checker
          final Checker checker = file.getFormat().getChecker();

          final Sample sample =
              file.getSample() != null ? file.getSample() : firstSample;

          // Launch the check
          checker.check(context, sample, checkStore);
        }

    } catch (EoulsanException e) {

      // Set the context as before starting the checkers
      context.setStep(this);

      return new StepResult(context, e);
    }

    // Set the context as before starting the checkers
    context.setStep(this);

    return new StepResult(context, true, "");
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

    Preconditions.checkNotNull(workflow, "Workflow argument cannot be null");
    Preconditions.checkNotNull(type, "Type argument cannot be null");

    this.workflow = workflow;
    this.id = type.name();
    this.skip = false;
    this.type = type;
    this.step = null;
    this.parameters = Collections.emptySet();

    // Register this step in the workflow
    this.workflow.register(this);
  }

  /**
   * Create a Generator Workflow step.
   * @param design design object
   * @param context context object
   * @param format DataFormat
   * @throws EoulsanException if an error occurs while configuring the generator
   */
  public AbstractWorkflowStep(final AbstractWorkflow workflow,
      final DataFormat format) throws EoulsanException {

    Preconditions.checkNotNull(workflow, "Workflow argument cannot be null");
    Preconditions.checkNotNull(format, "Format argument cannot be null");

    final Step generator = format.getGenerator();

    Preconditions.checkNotNull(generator, "The generator step is null");

    this.workflow = workflow;
    this.id = generator.getName();
    this.skip = false;
    this.type = StepType.GENERATOR_STEP;
    this.step = generator;
    this.parameters = Collections.emptySet();

    // Register this step in the workflow
    this.workflow.register(this);
  }

  /**
   * Create a step for a standard step.
   * @param workflow workflow of the step
   * @param id identifier of the step
   * @param step Step object
   * @param skip true to skip execution of the step
   * @param parameters parameters of the step
   * @throws EoulsanException id an error occurs while creating the step
   */
  protected AbstractWorkflowStep(final AbstractWorkflow workflow,
      final String id, final Step step, final boolean skip,
      final Set<Parameter> parameters) throws EoulsanException {

    Preconditions.checkNotNull(workflow, "Workflow argument cannot be null");
    Preconditions.checkNotNull(id, "Step id argument cannot be null");
    Preconditions.checkNotNull(step, "Step argument cannot be null");
    Preconditions.checkNotNull(parameters,
        "Step arguments argument cannot be null");

    this.workflow = workflow;
    this.id = id;
    this.skip = skip;
    this.type = StepType.STANDARD_STEP;
    this.step = step;
    this.parameters = Sets.newHashSet(parameters);

    // Register this step in the workflow
    this.workflow.register(this);
  }

}
