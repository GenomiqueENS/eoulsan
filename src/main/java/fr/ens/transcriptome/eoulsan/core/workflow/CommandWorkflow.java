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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.core.ExecutorArguments;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowModel.StepPort;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.protocols.DataProtocol;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.steps.mgmt.CopyInputDataStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.CopyOutputDataStep;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class define a workflow based on a Command object (workflow file).
 * @author Laurent Jourdren
 * @since 2.0
 */
public class CommandWorkflow extends AbstractWorkflow {

  /** Serialization version UID. */
  private static final long serialVersionUID = 4132064673361068654L;

  private static final Set<Parameter> EMPTY_PARAMETERS = Collections.emptySet();

  private List<CommandWorkflowStep> steps;
  private Set<String> stepsIds = Sets.newHashSet();

  private final CommandWorkflowModel workflowCommand;

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
      final boolean copyResultsToOutput = !c.isStepDiscardOutput(stepId);

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
   * Add a dependency. Add an additional step that copy/(un)compress data if
   * necessary.
   * @param port input port of the step
   * @param depencencyPort output port the dependency
   * @throws EoulsanException if an error occurs while adding the dependency
   */
  private void addDependency(final WorkflowInputPort port,
      final WorkflowOutputPort depencencyPort) throws EoulsanException {

    try {

      final AbstractWorkflowStep fromStep = port.getStep();
      final AbstractWorkflowStep toStep = depencencyPort.getStep();

      final DataFile stepDir = port.getStep().getStepWorkingDir();
      final DataFile depDir = depencencyPort.getStep().getStepWorkingDir();

      final DataProtocol stepProtocol = stepDir.getProtocol();
      final DataProtocol depProtocol = depDir.getProtocol();

      final EnumSet<CompressionType> stepCompressionsAllowed =
          port.getCompressionsAccepted();

      final CompressionType depOutputCompression =
          depencencyPort.getCompression();

      CommandWorkflowStep newStep = null;

      // Check if copy is needed in the working directory
      if (fromStep.getType() == StepType.STANDARD_STEP
          && stepProtocol != depProtocol && port.isRequiredInWorkingDirectory()) {
        newStep =
            newInputFormatCopyStep(this, port, depOutputCompression,
                stepCompressionsAllowed);
      }

      // Check if (un)compression is needed
      if (newStep == null
          && fromStep.getType() == StepType.STANDARD_STEP
          && !port.getCompressionsAccepted().contains(depOutputCompression)) {
        newStep =
            newInputFormatCopyStep(this, port, depOutputCompression,
                stepCompressionsAllowed);
      }

      // If the dependency if design step and step does not allow all the
      // compression types as input, (un)compress data
      if (newStep == null
          && fromStep.getType() == StepType.STANDARD_STEP
          && toStep == this.getDesignStep()
          && !EnumSet.allOf(CompressionType.class).containsAll(
              stepCompressionsAllowed)) {
        newStep =
            newInputFormatCopyStep(this, port, depOutputCompression,
                stepCompressionsAllowed);
      }

      // Set the dependencies
      if (newStep != null) {

        // Add the copy step in the list of steps just before the step given as
        // method argument
        addStep(indexOfStep(toStep), newStep);

        // Add the copy dependency
        newStep.addDependency(newStep.getWorkflowInputPorts().getFirstPort(),
            depencencyPort);

        // Add the step dependency
        toStep.addDependency(port, newStep.getWorkflowOutputPorts()
            .getFirstPort());
      } else {

        // Add the step dependency
        toStep.addDependency(port, depencencyPort);
      }
    } catch (IOException e) {
      throw new EoulsanException(e.getMessage());
    }
  }

  /**
   * Create a new step that copy/(un)compress input data of a step.
   * @param workflow workflow where adding the step
   * @param port input port
   * @param inputCompression compression format of the data to read
   * @param outputCompressionAllowed compression formats allowed by the step
   * @return a new step
   * @throws EoulsanException if an error occurs while creating the step
   */
  private static CommandWorkflowStep newInputFormatCopyStep(
      final CommandWorkflow workflow, final WorkflowInputPort port,
      final CompressionType inputCompression,
      final EnumSet<CompressionType> outputCompressionAllowed)
      throws EoulsanException {

    // Set the step name
    final String stepName = CopyInputDataStep.STEP_NAME;

    // Search a non used step id
    final Set<String> stepsIds = Sets.newHashSet();
    for (WorkflowStep s : workflow.getSteps())
      stepsIds.add(s.getId());
    int i = 1;
    String stepId;
    do {

      stepId = port.getStep().getId() + "prepare" + i;
      i++;

    } while (stepsIds.contains(stepId));

    // Find output compression
    final CompressionType comp;
    if (outputCompressionAllowed.contains(inputCompression))
      comp = inputCompression;
    else if (outputCompressionAllowed.contains(CompressionType.NONE))
      comp = CompressionType.NONE;
    else
      comp = outputCompressionAllowed.iterator().next();

    // Set parameters
    final Set<Parameter> parameters = Sets.newHashSet();
    parameters.add(new Parameter(CopyInputDataStep.FORMAT_PARAMETER, port
        .getFormat().getName()));
    parameters.add(new Parameter(
        CopyInputDataStep.OUTPUT_COMPRESSION_PARAMETER, comp.name()));

    // Create step
    CommandWorkflowStep step =
        new CommandWorkflowStep(workflow, stepId, stepName, parameters, false,
            false);

    // Configure step
    step.configure();

    return step;
  }

  /**
   * Create a new step that copy output data of a step.
   * @param workflow workflow where adding the step
   * @param outputPorts output ports where data must be copied
   * @return a new step
   * @throws EoulsanException if an error occurs while creating the step
   */
  private static CommandWorkflowStep newOutputFormatCopyStep(
      final CommandWorkflow workflow, final WorkflowOutputPorts outputPorts)
      throws EoulsanException {

    // Set the step name
    final String stepName = CopyOutputDataStep.STEP_NAME;

    // Search a non used step id
    final Set<String> stepsIds = Sets.newHashSet();
    for (WorkflowStep s : workflow.getSteps())
      stepsIds.add(s.getId());
    int i = 1;
    String stepId;
    do {

      stepId = outputPorts.getFirstPort().getStep().getId() + "finalize" + i;
      i++;

    } while (stepsIds.contains(stepId));

    List<String> portsList = Lists.newArrayList();
    List<String> formatsList = Lists.newArrayList();
    for (WorkflowOutputPort port : outputPorts) {
      portsList.add(port.getName());
      formatsList.add(port.getFormat().getName());
    }

    // Set parameters
    final Set<Parameter> parameters = Sets.newHashSet();
    parameters.add(new Parameter(CopyOutputDataStep.PORTS_PARAMETER, Joiner.on(
        ',').join(portsList)));
    parameters.add(new Parameter(CopyOutputDataStep.FORMATS_PARAMETER, Joiner
        .on(',').join(formatsList)));

    // Create step
    CommandWorkflowStep step =
        new CommandWorkflowStep(workflow, stepId, stepName, parameters, false,
            false);

    // Configure step
    step.configure();

    return step;
  }

  /**
   * Add user defined dependencies.
   * @throws EoulsanException if an error occurs while setting dependencies
   */
  private void addManualDependencies() throws EoulsanException {

    // Create a map with the name of the steps
    final Map<String, CommandWorkflowStep> stepsMap = Maps.newHashMap();
    for (CommandWorkflowStep step : this.steps)
      stepsMap.put(step.getId(), step);

    for (CommandWorkflowStep toStep : this.steps) {

      final Map<String, StepPort> inputs =
          this.workflowCommand.getStepInputs(toStep.getId());

      for (Map.Entry<String, StepPort> e : inputs.entrySet()) {

        final String toPortName = e.getKey();
        final String fromStepId = e.getValue().stepId;
        final String fromPortName = e.getValue().portName;

        // final DataFormat inputFormat = e.getKey();
        final CommandWorkflowStep fromStep = stepsMap.get(fromStepId);

        // Check if fromStep step exists
        if (fromStep == null)
          throw new EoulsanException("No workflow step found with id: "
              + fromStepId);

        // Check if the fromPort exists
        if (!fromStep.getWorkflowOutputPorts().contains(fromPortName))
          throw new EoulsanException("No port with name \""
              + fromPortName + "\" found for step with id: " + fromStep.getId());

        // Check if the toPort exists
        if (!toStep.getWorkflowInputPorts().contains(toPortName))
          throw new EoulsanException("No port with name \""
              + toPortName + "\" found for step with id: " + toStep.getId());

        final WorkflowOutputPort fromPort =
            fromStep.getWorkflowOutputPorts().getPort(fromPortName);
        final WorkflowInputPort toPort =
            toStep.getWorkflowInputPorts().getPort(toPortName);

        addDependency(toPort, fromPort);
      }
    }
  }

  /**
   * Search dependency between steps.
   * @throws EoulsanException if an error occurs while search dependencies
   */
  private void searchDependencies() throws EoulsanException {

    final Map<DataFormat, CommandWorkflowStep> generatorAdded =
        Maps.newHashMap();
    searchDependencies(generatorAdded);
  }

  /**
   * Search dependency between steps.
   * @throws EoulsanException if an error occurs while search dependencies
   */
  private void searchDependencies(
      final Map<DataFormat, CommandWorkflowStep> generatorAdded)
      throws EoulsanException {

    // final Set<DataFormat> dataFormatsFromDesign = getDesignDataFormats();
    final List<CommandWorkflowStep> steps = this.steps;

    for (int i = steps.size() - 1; i >= 0; i--) {

      final CommandWorkflowStep step = steps.get(i);

      // If step need no data, the step depends from the previous step
      if (step.getWorkflowInputPorts().isEmpty() && i > 0) {
        step.addDependency(steps.get(i - 1));
      }

      // If previous step has no output, the current step depend on it
      if (i > 0 && steps.get(i - 1).getWorkflowOutputPorts().isEmpty()) {
        step.addDependency(steps.get(i - 1));
      }

      for (WorkflowInputPort inputPort : step.getWorkflowInputPorts()) {

        // Do not search dependency for the format if already has been manually
        // set
        if (inputPort.isLinked())
          continue;

        final DataFormat format = inputPort.getFormat();

        // Check if more than one input have the same type
        int formatCount = 0;
        for (WorkflowInputPort p : step.getWorkflowInputPorts()
            .getPortsWithDataFormat(format))
          if (!p.isLinked())
            formatCount++;
        if (formatCount > 1)
          throw new EoulsanException("Step \""
              + step.getId()
              + "\" contains more than one of port of the same format ("
              + format + "). Please manually define all inputs for this ports.");

        boolean found = false;

        for (int j = i - 1; j >= 0; j--) {

          // For each step before current step
          final CommandWorkflowStep stepTested = steps.get(j);

          // Test each port
          for (WorkflowOutputPort outputPort : stepTested
              .getWorkflowOutputPorts().getPortsWithDataFormat(format)) {

            // The tested step is a standard/generator step
            if (stepTested.getType() == StepType.STANDARD_STEP
                || stepTested.getType() == StepType.GENERATOR_STEP
                || stepTested.getType() == StepType.DESIGN_STEP) {

              addDependency(inputPort, outputPort);

              found = true;
              break;
            }

            // The tested step is the design step
            // if (stepTested.getType() == StepType.DESIGN_STEP
            // && dataFormatsFromDesign.contains(format)) {
            //
            // addDependency(inputPort, outputPort);
            //
            // found = true;
            // break;
            // }
          }

          // Dependency found, do not search in other steps
          if (found)
            break;
        }

        if (!found) {

          // A generator is available for the DataType
          if (format.isGenerator()) {

            if (!generatorAdded.containsKey(format)) {

              final CommandWorkflowStep generatorStep =
                  new CommandWorkflowStep(this, format);

              generatorStep.configure();

              // Add after checker
              addStep(indexOfStep(getCheckerStep()) + 1, generatorStep);
              generatorAdded.put(format, generatorStep);

              // Rerun search dependencies
              searchDependencies(generatorAdded);
              return;
            }

            if (step.getType() == StepType.GENERATOR_STEP) {

              // Swap generators order
              Collections.swap(this.steps, indexOfStep(step),
                  indexOfStep(generatorAdded.get(format)));

              searchDependencies(generatorAdded);
              return;
            }

            throw new EoulsanException("Cannot found \""
                + format.getName() + "\" for step " + step.getId() + ".");
          }
        }
      }
    }

    // Add dependencies for terminal steps
    final List<CommandWorkflowStep> terminalSteps = Lists.newArrayList();
    for (CommandWorkflowStep step : this.steps) {

      for (CommandWorkflowStep terminalStep : terminalSteps)
        step.addDependency(terminalStep);

      if (step.isTerminalStep())
        terminalSteps.add(step);
    }

    // Add steps to copy output data from steps to output directory if
    // necessary
    for (CommandWorkflowStep step : Lists.newArrayList(this.steps)) {

      if (step.isCopyResultsToOutput()
          && !step.getStepWorkingDir().equals(getOutputDir())
          && !step.getWorkflowOutputPorts().isEmpty()) {

        CommandWorkflowStep newStep =
            newOutputFormatCopyStep(this, step.getWorkflowOutputPorts());

        // Add the copy step in the list of steps just before the step given as
        // method argument
        addStep(indexOfStep(step) + 1, newStep);

        // Add the copy dependencies
        for (WorkflowOutputPort outputPort : step.getWorkflowOutputPorts())
          newStep.addDependency(
              newStep.getWorkflowInputPorts().getPort(outputPort.getName()),
              outputPort);
      }
    }

    // Check if all input port are linked
    for (CommandWorkflowStep step : this.steps)
      for (WorkflowInputPort inputPort : step.getWorkflowInputPorts())
        if (!inputPort.isLinked())
          throw new EoulsanException("The \""
              + inputPort.getName() + "\" of step \""
              + inputPort.getStep().getId() + "\" is not linked");
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

    // final Set<WorkflowStepOutputDataFile> inFiles = Sets.newHashSet();
    // final Set<WorkflowStepOutputDataFile> reusedFiles = Sets.newHashSet();
    // final Set<WorkflowStepOutputDataFile> outFiles = Sets.newHashSet();
    //
    // boolean firstStepFound = false;
    //
    // for (CommandWorkflowStep step : this.steps) {
    //
    // if (!firstStepFound) {
    //
    // if (step == originStep)
    // firstStepFound = true;
    // else
    // continue;
    // }
    //
    // if (step.getType() == STANDARD_STEP && !step.isSkip()) {
    //
    // // If a terminal step exist don't go further
    // if (step.getStep().isTerminalStep())
    // return new WorkflowFiles(inFiles, reusedFiles, outFiles);
    // }
    //
    // for (Sample sample : getDesign().getSamples()) {
    // for (WorkflowInputPort inputPort : step.getWorkflowInputPorts()) {
    //
    // // Nothing to do if port is not linked
    // if (!inputPort.isLinked())
    // continue;
    //
    // final DataFormat format = inputPort.getFormat();
    //
    // final List<WorkflowStepOutputDataFile> files;
    //
    // if (format.getMaxFilesCount() == 1) {
    //
    // WorkflowStepOutputDataFile f =
    // new WorkflowStepOutputDataFile(inputPort.getLink(), sample);
    //
    // files = Collections.singletonList(f);
    // } else {
    // files = Lists.newArrayList();
    // final int count =
    // step.getInputPortData(inputPort.getName()).getDataFileCount(false);
    //
    // for (int i = 0; i < count; i++) {
    //
    // WorkflowStepOutputDataFile f =
    // new WorkflowStepOutputDataFile(inputPort.getLink(), sample, i);
    //
    // files.add(f);
    // }
    // }
    //
    // for (WorkflowStepOutputDataFile file : files) {
    //
    // if (reusedFiles.contains(file))
    // continue;
    //
    // if (outFiles.contains(file)) {
    // outFiles.remove(file);
    // reusedFiles.add(file);
    // continue;
    // }
    //
    // inFiles.add(file);
    // }
    // }
    // }
    //
    // // Add output files of the step
    // if (!step.isSkip()) {
    // for (Sample sample : getDesign().getSamples()) {
    // for (WorkflowOutputPort outputPort : step.getWorkflowOutputPorts()) {
    // // for (DataFormat format : step.getWorkflowOutputPorts()) {
    //
    // final DataFormat format = outputPort.getFormat();
    //
    // if (format.getMaxFilesCount() == 1) {
    //
    // WorkflowStepOutputDataFile f =
    // new WorkflowStepOutputDataFile(outputPort, sample);
    //
    // outFiles.add(f);
    // } else {
    // final int count =
    // WorkflowStepOutputDataFile.dataFileCount(outputPort, sample,
    // false);
    //
    // for (int i = 0; i < count; i++) {
    // WorkflowStepOutputDataFile f =
    // new WorkflowStepOutputDataFile(outputPort, sample, i);
    // outFiles.add(f);
    // }
    // }
    // }
    // }
    // }
    // }
    //
    // return new WorkflowFiles(inFiles, reusedFiles, outFiles);

    return null;
  }

  @Override
  protected void saveConfigurationFiles() throws EoulsanException {

    // Save design file
    super.saveConfigurationFiles();

    try {
      DataFile logDir = new DataFile(getWorkflowContext().getLogPathname());

      if (!logDir.exists()) {
        logDir.mkdirs();
      }

      // Save design file

      BufferedWriter writer =
          FileUtils.createBufferedWriter(new DataFile(logDir,
              WORKFLOW_COPY_FILENAME).create());
      writer.write(this.workflowCommand.toXML());
      writer.close();

    } catch (IOException | EoulsanRuntimeException e) {
      throw new EoulsanException("Error while writing workflow file: "
          + e.getMessage());
    }

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
   * @param executionArguments execution arguments
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

    // Set manually defined input format source
    addManualDependencies();

    // Search others input format sources
    searchDependencies();
  }

}
