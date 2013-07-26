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
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.core.Command;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.data.DataType;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.Utils;

public class CommandWorkflow implements Workflow {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final Set<Parameter> EMPTY_PARAMETERS = Collections.emptySet();

  private List<CommandWorkflowStep> steps;
  private Set<String> stepsIds = Sets.newHashSet();

  private final Command command;
  private final Design design;
  private final Context context;

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
      addStep(new CommandWorkflowStep(design, context, stepId, stepName,
          stepParameters, skip));
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
        addStep(0, new CommandWorkflowStep(this.design, this.context, stepId,
            step.getName(), EMPTY_PARAMETERS, false));
      }

    // Add the first step. Generators cannot be added after this step
    addStep(0, new CommandWorkflowStep(design, context, StepType.FIRST_STEP));

    // Add the design step
    addStep(0, new CommandWorkflowStep(design, context, StepType.DESIGN_STEP));
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

      addStep(new CommandWorkflowStep(this.design, this.context, stepId,
          step.getName(), EMPTY_PARAMETERS, false));
    }
  }

  private int findFirstStepPos() {

    for (int i = 0; i < this.steps.size(); i++)
      if (this.steps.get(i).getType() == StepType.FIRST_STEP)
        return i;

    return -1;
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
    CommandWorkflowStep previousStep = null;
    for (CommandWorkflowStep step : this.steps) {

      step.addPreviousStep(previousStep);
      if (previousStep != null)
        previousStep.addNextStep(step);
      step.configure();
      previousStep = step;
    }
  }

  private Set<DataType> getDesignDataTypes() {

    final Set<DataType> result = Sets.newHashSet();
    final List<String> fields = this.design.getMetadataFieldsNames();

    final DataFormatRegistry registry = DataFormatRegistry.getInstance();

    for (String fieldName : fields) {
      DataType dt = registry.getDataFormatForDesignField(fieldName);

      if (dt != null)
        result.add(dt);
    }

    return result;
  }

  private void searchInputDataFormat() throws EoulsanException {

    final Set<DataType> dataTypesFromDesign = getDesignDataTypes();

    for (int i = this.steps.size() - 1; i >= 0; i--) {

      final CommandWorkflowStep step = this.steps.get(i);
      System.out.println("In step " + step.getId());

      ListMultimap<DataType, DataFormat> lmm =
          step.getInputDataFormatByDataTypes();

      for (DataType dt : lmm.keySet()) {
        System.out.println("\tsearch for " + dt.getName());
        boolean found = false;
        List<DataFormat> generatorAvaillables = Lists.newArrayList();

        for (DataFormat df : lmm.get(dt)) {
          System.out.println("\t\tas dataformat " + df.getFormatName());
          if (!found) {
            for (int j = i - 1; j >= 0; j--) {

              final CommandWorkflowStep stepTested = this.steps.get(j);

              // The tested step is a standard/generator step
              if ((stepTested.getType() == StepType.STANDARD_STEP || stepTested
                  .getType() == StepType.GENERATOR_STEP)
                  && stepTested.getOutputDataFormats().contains(df)) {

                step.addInputFormatLocation(df, stepTested);
                found = true;
                break;
              }

              // The tested step is the design step
              if (stepTested.getType() == StepType.DESIGN_STEP
                  && dataTypesFromDesign.contains(dt)) {

                for (DataFormat df2 : lmm.get(dt))
                  step.addInputFormatLocation(df2, stepTested);
                found = true;
                break;
              }

            }
          }

          // A generator is available for the DataType
          if (df.isGenerator() && !this.generatorAdded.contains(df))
            generatorAvaillables.add(df);

        }

        if (!found) {

          // Add generator if needed
          if (!generatorAvaillables.isEmpty()) {

            final CommandWorkflowStep generatorStep =
                new CommandWorkflowStep(this.design, this.context,
                    generatorAvaillables.get(0));

            generatorStep.configure();
            System.out.println("add + " + dt.getName());
            addStep(1, generatorStep);

            searchInputDataFormat();
            return;
          }

          else
            throw new EoulsanException("Cannot found \""
                + dt.getName() + "\" for step " + step.getId() + ".");
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

    for (Sample s : this.design.getSamples()) {

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

  //
  // Check existing files
  //

  private void checkExistingOutputFiles() throws EoulsanException {

    final Set<DataFile> testedFiles = Sets.newHashSet();

    for (CommandWorkflowStep step : this.steps) {

      if (step.getType() == STANDARD_STEP && !step.isSkip()) {

        // If a terminal step exist don't go further
        if (step.getStep().isTerminalStep())
          return;

        for (DataFormat format : step.getOutputDataFormats())
          for (Sample sample : this.design.getSamples()) {

            DataFile file;

            if (format.getMaxFilesCount() > 1)
              file = step.getOutputDataFile(format, sample, 0);
            else
              file = step.getOutputDataFile(format, sample);

            if (!testedFiles.contains(file) && file.exists())
              throw new EoulsanException("For sample "
                  + sample.getId() + ", generated \"" + format.getFormatName()
                  + "\" already exists (" + file + ").");

            testedFiles.add(file);
          }
      }
    }

  }

  private void checkExistingInputFiles() throws EoulsanException {

    final Map<DataFile, Boolean> testedFiles = Maps.newHashMap();
    final Set<DataFile> generatedFiles = Sets.newHashSet();

    for (CommandWorkflowStep step : this.steps) {

      if ((step.getType() == STANDARD_STEP || step.getType() == GENERATOR_STEP)
          && !step.isSkip()) {

        // If a terminal step exist don't go further
        if (step.getStep().isTerminalStep())
          return;

        ListMultimap<DataType, DataFormat> lmm =
            step.getInputDataFormatByDataTypes();

        for (Sample sample : this.design.getSamples()) {
          for (DataType type : lmm.keys()) {

            boolean found = false;
            for (DataFormat format : lmm.get(type)) {

              DataFile inFile = step.getInputDataFile(format, sample);

              if (generatedFiles.contains(inFile))
                found = true;
              else if (testedFiles.containsKey(inFile))
                found = testedFiles.get(inFile);
              else {
                found = inFile.exists();
                testedFiles.put(inFile, found);
              }

              if (found)
                break;
            }

            if (!found)
              throw new EoulsanException("For sample "
                  + sample.getId() + " in step " + step.getId()
                  + ", input file for " + type.getName() + " not exists.");

          }
        }

        // Add generated file of the step
        for (DataFormat format : step.getOutputDataFormats())
          for (Sample sample : this.design.getSamples())
            generatedFiles.add(step.getOutputDataFile(format, sample));

      }
    }

  }

  private void scanStepsFiles() {

    final Set<DataFile> inFiles = Sets.newHashSet();
    final Set<DataFile> interFiles = Sets.newHashSet();
    final Set<DataFile> outFiles = Sets.newHashSet();

    for (CommandWorkflowStep step : this.steps) {

      if (step.getType() == STANDARD_STEP && !step.isSkip()) {

        // If a terminal step exist don't go further
        if (step.getStep().isTerminalStep())
          return;
      }

      ListMultimap<DataType, DataFormat> lmm =
          step.getInputDataFormatByDataTypes();

      for (Sample sample : this.design.getSamples()) {
        for (DataType type : lmm.keys()) {

          for (DataFormat format : lmm.get(type)) {

            DataFile file = step.getInputDataFile(format, sample);

            if (interFiles.contains(file))
              continue;

            if (outFiles.contains(file)) {
              outFiles.remove(file);
              interFiles.add(file);
              continue;
            }

            inFiles.add(file);
          }
        }
      }

      // Add output files of the step
      for (DataFormat format : step.getOutputDataFormats())
        for (Sample sample : this.design.getSamples()) {
          
          DataFile file = step.getOutputDataFile(format, sample);

          outFiles.add(file);
        }
    }

  }

  //
  // Constructor
  //

  public CommandWorkflow(final Command command, final List<Step> firstSteps,
      final List<Step> endSteps, final Design design, final Context context)
      throws EoulsanException {

    if (command == null)
      throw new NullPointerException("The command is null.");

    if (design == null)
      throw new NullPointerException("The design is null.");

    if (context == null)
      throw new NullPointerException("The execution context is null.");

    this.command = command;
    this.design = design;
    this.context = context;

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
    searchInputDataFormat();

    // check if output files does not exists
    checkExistingOutputFiles();

    // check if input files exists
    checkExistingInputFiles();
  }

  public void show() {

    for (CommandWorkflowStep ws : this.steps)
      ws.show();
  }

  //
  // Workflow methods
  //

  /**
   * Get the first steps of the workflow.
   * @return a set with the first steps the workflow
   */
  public Set<WorkflowStep> getFirstSteps() {

    return Collections.singleton((WorkflowStep) this.steps.get(0));
  }

}
