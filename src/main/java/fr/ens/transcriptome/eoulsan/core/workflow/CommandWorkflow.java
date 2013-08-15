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
 * of the Institut de Biologie de l'√âcole Normale Sup√©rieure and
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
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType.GENERATOR_STEP;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType.STANDARD_STEP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.core.ExecutorArguments;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.data.protocols.DataProtocol;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.steps.mgmt.CopyInputFormatStep;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class define a workflow based on a Command object (workflow file).
 * @author Laurent Jourdren
 * @since 1.3
 */
public class CommandWorkflow extends AbstractWorkflow {

  /** Serialization version UID. */
  private static final long serialVersionUID = 4132064673361068654L;

  private static final Set<Parameter> EMPTY_PARAMETERS = Collections.emptySet();

  private List<CommandWorkflowStep> steps;
  private Set<String> stepsIds = Sets.newHashSet();

  private final CommandWorkflowModel workflowCommand;

  private final Map<DataFormat, CommandWorkflowStep> generatorAdded = Maps
      .newHashMap();

  //
  // Add steps
  //

  /**
   * Add a step.
   * @param step step to add.
   * @throws EoulsanException if an error occurs while adding a step
   */
  private void addStep(final CommandWorkflowStep step) throws EoulsanException {

    addStep(-1, step);
  }

  /**
   * Add a step.
   * @param pos position of the step in list of steps.
   * @param step step to add.
   * @throws EoulsanException if an error occurs while adding a step
   */
  private void addStep(final int pos, final CommandWorkflowStep step)
      throws EoulsanException {

    if (step == null)
      throw new EoulsanException("Cannot add null step");

    final String stepId = step.getId();

    if (stepId == null)
      throw new EoulsanException("Cannot add a step with null id");

    if (step.getType() != GENERATOR_STEP && this.stepsIds.contains(stepId))
      throw new EoulsanException(
          "Cannot add step because it already had been added: " + stepId);

    if (step.getType() == STANDARD_STEP || step.getType() == GENERATOR_STEP) {
      for (StepType t : StepType.values()) {
        if (t.name().equals(stepId))
          throw new EoulsanException("Cannot add a step with a reserved id: "
              + stepId);
      }
    }

    if (pos == -1)
      this.steps.add(step);
    else
      this.steps.add(pos, step);

    this.stepsIds.add(stepId);
  }

  /**
   * Get the index of step in the list of step.
   * @param step the step to search
   * @return the index of the step or -1 if the step is not found
   */
  private int indexOfStep(final WorkflowStep step) {

    if (step == null)
      return -1;

    return this.steps.indexOf(step);
  }

  /**
   * Create the list of steps.
   * @throws EoulsanException if an error occurs while creating the step
   */
  private void addMainSteps() throws EoulsanException {

    this.steps = new ArrayList<CommandWorkflowStep>();
    final CommandWorkflowModel c = this.workflowCommand;

    for (String stepId : c.getStepIds()) {

      final String stepName = c.getStepName(stepId);
      final Set<Parameter> stepParameters = c.getStepParameters(stepId);
      final boolean skip = c.isStepSkipped(stepId);

      // TODO the value must be defined in CommandWorkflowModel
      final boolean copyResultsToOutput = true;

      getLogger().info(
          "Create "
              + (skip ? "skipped step" : "step ") + stepId + " (" + stepName
              + ") step.");
      addStep(new CommandWorkflowStep(this, stepId, stepName, stepParameters,
          skip, copyResultsToOutput));
    }
  }

  /**
   * Add some steps at the start of the Workflow.
   * @param firstSteps list of steps to add
   * @throws EoulsanException if an error occurs while adding a step
   */
  private void addFirstSteps(final List<Step> firstSteps)
      throws EoulsanException {

    if (firstSteps != null)
      for (Step step : Utils.listWithoutNull(firstSteps)) {

        final String stepId = step.getName();
        addStep(0, new CommandWorkflowStep(this, stepId, step.getName(),
            EMPTY_PARAMETERS, false, false));
      }

    // Add the first step. Generators cannot be added after this step
    addStep(0, new CommandWorkflowStep(this, StepType.FIRST_STEP));

    // Add the checker step
    addStep(0, new CommandWorkflowStep(this, StepType.CHECKER_STEP));

    // Add the design step
    addStep(0, new CommandWorkflowStep(this, StepType.DESIGN_STEP));

    // Add the root step
    addStep(0, new CommandWorkflowStep(this, StepType.ROOT_STEP));
  }

  /**
   * Add some steps at the end of the Workflow.
   * @param endSteps list of steps to add
   * @throws EoulsanException if an error occurs while adding a step
   */
  private void addEndSteps(final List<Step> endSteps) throws EoulsanException {

    if (endSteps == null)
      return;

    for (Step step : Utils.listWithoutNull(endSteps)) {

      final String stepId = step.getName();

      addStep(new CommandWorkflowStep(this, stepId, step.getName(),
          EMPTY_PARAMETERS, false, false));
    }
  }

  /**
   * Initialize the steps of the Workflow.
   * @throws EoulsanException if an error occurs while creating the step
   */
  private void init() throws EoulsanException {

    final CommandWorkflowModel c = this.workflowCommand;
    final Set<Parameter> globalParameters = c.getGlobalParameters();

    final Settings settings = EoulsanRuntime.getSettings();

    // Add globals parameters to Settings
    getLogger().info(
        "Init all steps with global parameters: " + globalParameters);
    for (Parameter p : globalParameters)
      settings.setSetting(p.getName(), p.getStringValue());

    // Configure all the steps
    for (CommandWorkflowStep step : this.steps) {
      step.configure();
    }
  }

  /**
   * Get the format provided by the design file.
   * @return a Set with the DataFormat
   */
  private Set<DataFormat> getDesignDataFormats() {

    final Set<DataFormat> result = Sets.newHashSet();
    final List<String> fields = getDesign().getMetadataFieldsNames();

    final DataFormatRegistry registry = DataFormatRegistry.getInstance();

    for (String fieldName : fields) {
      DataFormat df = registry.getDataFormatForDesignField(fieldName);

      if (df != null)
        result.add(df);
    }

    return result;
  }

  /**
   * Add a dependency. Add an additional step that copy/(un)compress data if
   * necessary.
   * @param step the step
   * @param format format provided by the dependency
   * @param dependency the dependency
   * @throws EoulsanException if an error occurs while adding the dependency
   */
  private void addDependency(final AbstractWorkflowStep step,
      final DataFormat format, final AbstractWorkflowStep dependency)
      throws EoulsanException {

    try {

      final DataFile stepDir = step.getStepWorkingDir();
      final DataFile depDir = dependency.getStepWorkingDir();

      final DataProtocol stepProtocol = stepDir.getProtocol();
      final DataProtocol depProtocol = depDir.getProtocol();

      final EnumSet<CompressionType> stepCompressionsAllowed =
          step.getInputDataFormatCompressionsAllowed(format);
      final CompressionType depOutputCompression =
          dependency == getDesignStep() ? CompressionType.NONE : dependency
              .getOutputDataFormatCompression(format);

      final Set<DataFormat> stepFormatRequieredWD =
          step.getInputFormatsRequieredInWorkingDirectory();

      CommandWorkflowStep newStep = null;

      // Check if copy is needed in the working directory
      if (step.getType() == StepType.STANDARD_STEP
          && stepProtocol != depProtocol
          && stepFormatRequieredWD.contains(format)) {
        newStep =
            newInputFormatCopyStep(this, step.getId(), stepDir, format,
                depOutputCompression, stepCompressionsAllowed);
      }

      // Check if (un)compression is needed
      if (newStep == null
          && step.getType() == StepType.STANDARD_STEP
          && !stepCompressionsAllowed.contains(depOutputCompression)) {
        newStep =
            newInputFormatCopyStep(this, step.getId(), stepDir, format,
                depOutputCompression, stepCompressionsAllowed);
      }

      // If the dependency if design step and step does not allow all the
      // compression types as input, (un)compress data
      if (newStep == null
          && step.getType() == StepType.STANDARD_STEP
          && dependency == this.getDesignStep()
          && !EnumSet.allOf(CompressionType.class).containsAll(
              stepCompressionsAllowed)) {
        newStep =
            newInputFormatCopyStep(this, step.getId(), stepDir, format,
                depOutputCompression, stepCompressionsAllowed);
      }

      // Set the dependencies
      if (newStep != null) {

        // Add the copy step in the list of steps just before the step given as
        // method argument
        addStep(indexOfStep(step), newStep);

        // Add the copy dependency
        newStep.addDependency(format, dependency);

        // Add the step dependency
        step.addDependency(format, newStep);
      } else {

        // Add the step dependency
        step.addDependency(format, dependency);
      }
    } catch (IOException e) {
      throw new EoulsanException(e.getMessage());
    }
  }

  /**
   * Create a new step that copy/(un)compress input data of a step
   * @param workflow workflow where adding the step
   * @param oriStepId id of the step that required copying data
   * @param format format of the data
   * @param inputCompression compression format of the data to read
   * @param outputCompressionAllowed compression formats allowed by the step
   * @return a new step
   * @throws EoulsanException if an error occurs while creating the step
   */
  private static CommandWorkflowStep newInputFormatCopyStep(
      final CommandWorkflow workflow, final String oriStepId,
      final DataFile workingDirectory, final DataFormat format,
      final CompressionType inputCompression,
      final EnumSet<CompressionType> outputCompressionAllowed)
      throws EoulsanException {

    // Set the step name
    final String stepName = CopyInputFormatStep.STEP_NAME;

    // Search a non used step id
    final Set<String> stepsIds = Sets.newHashSet();
    for (WorkflowStep s : workflow.getSteps())
      stepsIds.add(s.getId());
    int i = 1;
    String stepId;
    do {

      stepId = oriStepId + "prepare" + i;
      i++;

    } while (stepsIds.contains(stepId));

    // Find output compression
    CompressionType comp = null;
    if (outputCompressionAllowed.contains(inputCompression))
      comp = inputCompression;
    else if (outputCompressionAllowed.contains(CompressionType.NONE))
      comp = CompressionType.NONE;
    else
      comp = outputCompressionAllowed.iterator().next();

    // Set parameters
    final Set<Parameter> parameters = Sets.newHashSet();
    parameters.add(new Parameter(CopyInputFormatStep.FORMAT_PARAMETER, format
        .getName()));
    parameters.add(new Parameter(
        CopyInputFormatStep.OUTPUT_COMPRESSION_PARAMETER, comp.name()));

    // Create step
    CommandWorkflowStep step =
        new CommandWorkflowStep(workflow, stepId, stepName, parameters, false,
            false);

    // Configure step
    step.configure();

    return step;
  }

  /**
   * Search dependency between steps.
   * @throws EoulsanException if an error occurs while search dependencies
   */
  private void searchDependencies() throws EoulsanException {

    final Set<DataFormat> dataFormatsFromDesign = getDesignDataFormats();

    final List<CommandWorkflowStep> steps = this.steps;

    for (int i = steps.size() - 1; i >= 0; i--) {

      final CommandWorkflowStep step = steps.get(i);

      // If step need no data, the step depends from the previous step
      if (step.getInputDataFormats().isEmpty() && i > 0)
        step.addDependency(steps.get(i - 1));

      for (DataFormat format : step.getInputDataFormats()) {

        // Do not search dependency for the format if already has been manually
        // set
        if (step.isDependencySet(format))
          continue;

        boolean found = false;

        for (int j = i - 1; j >= 0; j--) {

          final CommandWorkflowStep stepTested = steps.get(j);

          // The tested step is a standard/generator step
          if ((stepTested.getType() == StepType.STANDARD_STEP || stepTested
              .getType() == StepType.GENERATOR_STEP)
              && stepTested.getOutputDataFormats().contains(format)) {

            addDependency(step, format, stepTested);

            found = true;
            break;
          }

          // The tested step is the design step
          if (stepTested.getType() == StepType.DESIGN_STEP
              && dataFormatsFromDesign.contains(format)) {

            addDependency(step, format, stepTested);

            found = true;
            break;
          }

        }

        if (!found) {

          // A generator is available for the DataType
          if (format.isGenerator()) {

            if (!this.generatorAdded.containsKey(format)) {

              final CommandWorkflowStep generatorStep =
                  new CommandWorkflowStep(this, format);

              generatorStep.configure();

              // Add after checker
              addStep(indexOfStep(getCheckerStep()) + 1, generatorStep);

              this.generatorAdded.put(format, generatorStep);

              searchDependencies();
              return;
            } else {

              if (step.getType() == StepType.GENERATOR_STEP) {

                // Swap generators order
                Collections.swap(this.steps, indexOfStep(step),
                    indexOfStep(this.generatorAdded.get(format)));

                searchDependencies();
                return;

              } else
                throw new EoulsanException("Cannot found \""
                    + format.getName() + "\" for step " + step.getId() + ".");
            }
          } else
            throw new EoulsanException("Cannot found \""
                + format.getName() + "\" for step " + step.getId() + ".");
        }
      }
    }

    // Clear map of generators used
    this.generatorAdded.clear();

    // TODO add steps to copy output data from steps to output directory if
    // necessary
  }

  //
  // Convert S3 URL methods
  //

  /**
   * Convert the S3 URLs to S3N URLs in source, genome and annotation fields of
   * the design.
   */
  private void convertDesignS3URLs() {

    for (Sample s : getDesign().getSamples()) {

      // Convert read file URL
      final List<String> readsSources =
          Lists.newArrayList(s.getMetadata().getReads());
      for (int i = 0; i < readsSources.size(); i++)
        readsSources.set(i, convertS3URL(readsSources.get(i)));
      s.getMetadata().setReads(readsSources);

      // Convert genome file URL
      if (s.getMetadata().isGenomeField())
        s.getMetadata().setGenome(convertS3URL(s.getMetadata().getGenome()));

      // Convert annotation file URL
      if (s.getMetadata().isAnnotationField())
        s.getMetadata().setAnnotation(
            convertS3URL(s.getMetadata().getAnnotation()));
    }
  }

  /**
   * Convert a s3:// URL to a s3n:// URL
   * @param url input URL
   * @return converted URL
   */
  private String convertS3URL(final String url) {

    return StringUtils.replacePrefix(url, "s3:/", "s3n:/");
  }

  /**
   * List the files (input files, output files and resused files) of the
   * workflow from a given step.
   * @param originStep origin step from which files must be searched
   * @return a WorkflowFile object
   */
  private WorkflowFiles listStepsFiles(final WorkflowStep originStep) {

    final Set<WorkflowStepOutputDataFile> inFiles = Sets.newHashSet();
    final Set<WorkflowStepOutputDataFile> reusedFiles = Sets.newHashSet();
    final Set<WorkflowStepOutputDataFile> outFiles = Sets.newHashSet();

    boolean firstStepFound = false;

    for (CommandWorkflowStep step : this.steps) {

      if (!firstStepFound) {

        if (step == originStep)
          firstStepFound = true;
        else
          continue;
      }

      if (step.getType() == STANDARD_STEP && !step.isSkip()) {

        // If a terminal step exist don't go further
        if (step.getStep().isTerminalStep())
          return new WorkflowFiles(inFiles, reusedFiles, outFiles);
      }

      for (Sample sample : getDesign().getSamples()) {
        for (DataFormat format : step.getInputDataFormats()) {

          final List<WorkflowStepOutputDataFile> files;

          if (format.getMaxFilesCount() == 1) {

            WorkflowStepOutputDataFile f =
                new WorkflowStepOutputDataFile(
                    step.getInputDataFormatStep(format), format, sample);

            files = Collections.singletonList(f);
          } else {
            files = Lists.newArrayList();
            final int count = step.getInputDataFileCount(format, sample, false);

            for (int i = 0; i < count; i++) {

              WorkflowStepOutputDataFile f =
                  new WorkflowStepOutputDataFile(
                      step.getInputDataFormatStep(format), format, sample, i);

              files.add(f);
            }
          }

          for (WorkflowStepOutputDataFile file : files) {

            if (reusedFiles.contains(file))
              continue;

            if (outFiles.contains(file)) {
              outFiles.remove(file);
              reusedFiles.add(file);
              continue;
            }

            inFiles.add(file);
          }
        }
      }

      // Add output files of the step
      if (!step.isSkip()) {
        for (DataFormat format : step.getOutputDataFormats()) {
          for (Sample sample : getDesign().getSamples()) {

            if (format.getMaxFilesCount() == 1) {

              WorkflowStepOutputDataFile f =
                  new WorkflowStepOutputDataFile(step, format, sample);

              outFiles.add(f);
            } else {
              final int count =
                  step.getOutputDataFileCount(format, sample, false);

              for (int i = 0; i < count; i++) {
                WorkflowStepOutputDataFile f =
                    new WorkflowStepOutputDataFile(step, format, sample, i);
                outFiles.add(f);
              }
            }
          }
        }
      }
    }

    return new WorkflowFiles(inFiles, reusedFiles, outFiles);
  }

  //
  // Workflow methods
  //

  @Override
  public WorkflowFiles getWorkflowFilesAtRootStep() {

    return listStepsFiles(getRootStep());
  }

  @Override
  public WorkflowFiles getWorkflowFilesAtFirstStep() {

    return listStepsFiles(getFirstStep());
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param context context of the workflow
   * @param workflowCommand Command object with the content of the parameter
   *          file
   * @param firstSteps optional steps to add at the beginning of the workflow
   * @param endSteps optional steps to add at the end of the workflow
   * @param design Design to use with the workflow
   * @throws EoulsanException
   */
  public CommandWorkflow(final ExecutorArguments executionArguments,
      final CommandWorkflowModel workflowCommand, final List<Step> firstSteps,
      final List<Step> endSteps, final Design design) throws EoulsanException {

    super(executionArguments, design);

    if (workflowCommand == null)
      throw new NullPointerException("The command is null.");

    this.workflowCommand = workflowCommand;

    // Set command information in context
    final WorkflowContext context = getWorkflowContext();
    context.setCommandName(workflowCommand.getName());
    context.setCommandDescription(workflowCommand.getDescription());
    context.setCommandAuthor(workflowCommand.getAuthor());

    // Convert s3:// urls to s3n:// urls
    convertDesignS3URLs();

    // Create the basic steps
    addMainSteps();

    // Add first steps
    addFirstSteps(firstSteps);

    // Add end steps
    addEndSteps(endSteps);

    // initialize steps
    init();

    // TODO Set manually defined input format source

    // Search others input format sources
    searchDependencies();
  }

}
