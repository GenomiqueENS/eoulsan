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

import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType.GENERATOR_STEP;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType.STANDARD_STEP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.core.Command;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.Utils;

public class CommandWorkflow extends AbstractWorkflow {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final Set<Parameter> EMPTY_PARAMETERS = Collections.emptySet();

  private List<CommandWorkflowStep> steps;
  private Set<String> stepsIds = Sets.newHashSet();

  private final Command command;

  private final CommandWorkflowStep rootStep;
  private final CommandWorkflowStep designStep;
  private final CommandWorkflowStep firstStep;

  private final Set<DataFormat> generatorAdded = Sets.newHashSet();

  //
  // Add steps
  //

  private void addStep(final CommandWorkflowStep ws) throws EoulsanException {

    addStep(-1, ws);
  }

  private void addStep(final int pos, final CommandWorkflowStep ws)
      throws EoulsanException {

    if (ws == null)
      throw new EoulsanException("Cannot add null step");

    final String stepId = ws.getId();

    if (stepId == null)
      throw new EoulsanException("Cannot add a step with null id");

    if (ws.getType() != GENERATOR_STEP && this.stepsIds.contains(stepId))
      throw new EoulsanException(
          "Cannot add step because it already had been added: " + stepId);

    if (ws.getType() == STANDARD_STEP || ws.getType() == GENERATOR_STEP) {
      for (StepType t : StepType.values()) {
        if (t.name().equals(stepId))
          throw new EoulsanException("Cannot add a step with a reserved id: "
              + stepId);
      }
    }

    if (pos == -1)
      this.steps.add(ws);
    else
      this.steps.add(pos, ws);

    this.stepsIds.add(stepId);
  }

  /**
   * Create the list of steps
   * @throws EoulsanException if an error occurs while creating the step
   */
  private void addMainSteps() throws EoulsanException {

    this.steps = new ArrayList<CommandWorkflowStep>();
    final Command c = this.command;

    for (String stepId : c.getStepIds()) {

      final String stepName = c.getStepName(stepId);
      final Set<Parameter> stepParameters = c.getStepParameters(stepId);
      final boolean skip = c.isStepSkipped(stepId);

      LOGGER.info("Create "
          + (skip ? "skipped step" : "step ") + stepId + " (" + stepName
          + ") step.");
      addStep(new CommandWorkflowStep(this, stepId, stepName, stepParameters,
          skip));
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
            EMPTY_PARAMETERS, false));
      }

    // Add the first step. Generators cannot be added after this step
    addStep(0, this.firstStep);

    // Add the design step
    addStep(0, this.designStep);

    // Add the design step
    addStep(0, this.rootStep);
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
          EMPTY_PARAMETERS, false));
    }
  }

  /**
   * Initialize the steps of the Workflow
   * @throws EoulsanException if an error occurs while creating the step
   */
  private void init() throws EoulsanException {

    final Command c = this.command;
    final Set<Parameter> globalParameters = c.getGlobalParameters();

    final Settings settings = EoulsanRuntime.getSettings();

    // Add globals parameters to Settings
    LOGGER.info("Init all steps with global parameters: " + globalParameters);
    for (Parameter p : globalParameters)
      settings.setSetting(p.getName(), p.getStringValue());

    // Configure all the steps
    for (CommandWorkflowStep step : this.steps) {
      step.configure();
    }
  }

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

  private void searchDependencies() throws EoulsanException {

    final Set<DataFormat> dataFormatsFromDesign = getDesignDataFormats();

    final List<CommandWorkflowStep> steps = this.steps;

    for (int i = steps.size() - 1; i >= 0; i--) {

      final CommandWorkflowStep step = steps.get(i);

      // If step need no data, the step depends from the previous step
      if (step.getInputDataFormats().isEmpty() && i > 0)
        step.addDependency(steps.get(i - 1));

      for (DataFormat df : step.getInputDataFormats()) {

        // Do not search dependency for the format if already has been manually
        // set
        if (step.isDependencySet(df))
          continue;

        boolean found = false;
        List<DataFormat> generatorAvaillables = Lists.newArrayList();

        for (int j = i - 1; j >= 0; j--) {

          final CommandWorkflowStep stepTested = steps.get(j);

          // The tested step is a standard/generator step
          if ((stepTested.getType() == StepType.STANDARD_STEP || stepTested
              .getType() == StepType.GENERATOR_STEP)
              && stepTested.getOutputDataFormats().contains(df)) {

            step.addDependency(df, stepTested);

            found = true;
            break;
          }

          // The tested step is the design step
          if (stepTested.getType() == StepType.DESIGN_STEP
              && dataFormatsFromDesign.contains(df)) {

            step.addDependency(df, stepTested);

            found = true;
            break;
          }

        }

        // A generator is available for the DataType
        if (df.isGenerator() && !this.generatorAdded.contains(df))
          generatorAvaillables.add(df);

        if (!found) {

          // Add generator if needed
          if (!generatorAvaillables.isEmpty()) {

            final CommandWorkflowStep generatorStep =
                new CommandWorkflowStep(this, generatorAvaillables.get(0));

            generatorStep.configure();

            // Add after design step (in pos 2)
            addStep(2, generatorStep);

            searchDependencies();
            return;
          }

          else
            throw new EoulsanException("Cannot found \""
                + df.getFormatName() + "\" for step " + step.getId() + ".");
        }
      }
    }

    // Remove duplicate generators
    removeDuplicateGenerators();
  }

  private void removeDuplicateGenerators() {

    Set<String> generatorNames = Sets.newHashSet();
    for (WorkflowStep ws : Lists.newArrayList(this.steps)) {

      if (ws.getType() == StepType.GENERATOR_STEP) {

        final String stepId = ws.getId();

        if (generatorNames.contains(stepId))
          this.steps.remove(ws);
        else
          generatorNames.add(stepId);
      }
    }
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

  private WorkflowFiles listStepsFiles(final WorkflowStep firstStep) {

    final Set<WorkflowStepOutputDataFile> inFiles = Sets.newHashSet();
    final Set<WorkflowStepOutputDataFile> reusedFiles = Sets.newHashSet();
    final Set<WorkflowStepOutputDataFile> outFiles = Sets.newHashSet();

    boolean firstStepFound = false;

    for (CommandWorkflowStep step : this.steps) {

      if (!firstStepFound) {

        if (step == firstStep)
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

  public WorkflowStep getFirstStep() {

    return this.firstStep;
  }

  /**
   * Get the first steps of the workflow.
   * @return a set with the first steps the workflow
   */
  public WorkflowStep getRootStep() {

    return this.rootStep;
  }

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

  public CommandWorkflow(final Command command, final List<Step> firstSteps,
      final List<Step> endSteps, final Design design) throws EoulsanException {

    super(design);

    if (command == null)
      throw new NullPointerException("The command is null.");

    // Define the root step
    this.rootStep = new CommandWorkflowStep(this, StepType.ROOT_STEP);

    // Define the design step
    this.designStep = new CommandWorkflowStep(this, StepType.DESIGN_STEP);

    // Define the first step
    this.firstStep = new CommandWorkflowStep(this, StepType.FIRST_STEP);

    this.command = command;

    // Set command information in context
    final WorkflowContext context = getWorkflowContext();
    context.setCommandName(command.getName());
    context.setCommandDescription(command.getDescription());
    context.setCommandAuthor(command.getAuthor());

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
