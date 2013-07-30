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

import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.READY;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.WAITING;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType.GENERATOR_STEP;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType.STANDARD_STEP;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.samtools.util.StopWatch;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define a Workflow. This class must be extended by a class to be
 * able to work with a specific worklow file format.
 * @author Laurent Jourdren
 * @since 1.3
 */
public abstract class AbstractWorkflow implements Workflow {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private final Design design;
  private final WorkflowContext context;
  private final Set<String> stepIds = Sets.newHashSet();
  private final Map<AbstractWorkflowStep, StepState> steps = Maps.newHashMap();
  private final Multimap<StepState, AbstractWorkflowStep> states =
      ArrayListMultimap.create();

  private AbstractWorkflowStep rootStep;
  private AbstractWorkflowStep designStep;
  private AbstractWorkflowStep firstStep;

  //
  // Inner interface
  //

  /**
   * This interface is used by the Executor to process the step result.
   * @author Laurent Jourdren
   * @since 1.3
   */
  public interface WorkflowStepResultProcessor {

    /**
     * Process a step result.
     * @param step Step that has been executed
     * @param result result of the step
     * @throws EoulsanException if an error occurs while processing the result
     */
    void processResult(WorkflowStep step, StepResult result)
        throws EoulsanException;

  }

  //
  // Getters
  //

  @Override
  public Design getDesign() {

    return this.design;
  }

  @Override
  public Context getContext() {

    return this.context;
  }

  /**
   * Get the real Context object. This method is useful to redefine context
   * values like base directory.
   * @return The Context object
   */
  WorkflowContext getWorkflowContext() {

    return this.context;
  }

  @Override
  public Set<WorkflowStep> getSteps() {

    final Set<WorkflowStep> result = Sets.newHashSet();
    result.addAll(this.steps.keySet());

    return Collections.unmodifiableSet(result);
  }

  @Override
  public WorkflowStep getRootStep() {

    return this.rootStep;
  }

  @Override
  public WorkflowStep getDesignStep() {

    return this.designStep;
  }

  @Override
  public WorkflowStep getFirstStep() {

    return this.firstStep;
  }

  //
  // Setters
  //

  /**
   * Register a step of the workflow.
   * @param step step to register
   */
  protected void register(final AbstractWorkflowStep step) {

    Preconditions.checkNotNull(step, "step cannot be null");

    if (step.getWorkflow() != this)
      throw new IllegalStateException(
          "step cannot be part of more than one workflow");

    if (this.stepIds.contains(step.getId()))
      throw new IllegalStateException("2 step cannot had the same id: "
          + step.getId());

    // Register root step
    if (step.getType() == StepType.ROOT_STEP) {

      if (this.rootStep != null && step != this.rootStep)
        throw new IllegalStateException(
            "Cannot add 2 root steps to the workflow");
      this.rootStep = step;
    }

    // Register design step
    if (step.getType() == StepType.DESIGN_STEP) {

      if (this.designStep != null && step != this.designStep)
        throw new IllegalStateException(
            "Cannot add 2 design steps to the workflow");
      this.designStep = step;
    }

    // Register first step
    if (step.getType() == StepType.FIRST_STEP) {

      if (this.firstStep != null && step != this.firstStep)
        throw new IllegalStateException(
            "Cannot add 2 first steps to the workflow");
      this.firstStep = step;
    }

    synchronized (this) {
      this.steps.put(step, step.getState());
      this.states.put(step.getState(), step);
    }
  }

  /**
   * Update the status of a step. This method is used by steps to inform the
   * workflow object that the status of the step has been changed.
   * @param step Step that the status has been changed.
   */
  void updateStepState(final AbstractWorkflowStep step) {

    Preconditions.checkNotNull(step, "step argument is null");

    if (step.getWorkflow() != this)
      throw new IllegalStateException("step is not part of the workflow");

    synchronized (this) {

      StepState oldState = this.steps.get(step);
      StepState newState = step.getState();

      this.states.remove(oldState, step);
      this.states.put(newState, step);
      this.steps.put(step, newState);
    }
  }

  //
  // Check methods
  //

  /**
   * Check if the output file of the workflow already exists.
   * @throws EoulsanException if output files of the workflow already exists
   */
  private void checkExistingOutputFiles() throws EoulsanException {

    final WorkflowFiles files = getWorkflowFilesAtRootStep();

    for (WorkflowStepOutputDataFile file : files.getOutputFiles())
      if (file.getDataFile().exists())
        throw new EoulsanException("For sample "
            + file.getSample().getId() + ", generated \""
            + file.getFormat().getFormatName() + "\" already exists ("
            + file.getDataFile() + ").");

    for (WorkflowStepOutputDataFile file : files.getReusedFiles())
      if (file.getDataFile().exists())
        throw new EoulsanException("For sample "
            + file.getSample().getId() + " in step " + file.getStep().getId()
            + ", generated \"" + file.getFormat().getFormatName()
            + "\" already exists (" + file.getDataFile() + ").");
  }

  /**
   * Check if the input file of the workflow already exists.
   * @throws EoulsanException if input files of the workflow already exists
   */
  private void checkExistingInputFiles() throws EoulsanException {

    final WorkflowFiles files = getWorkflowFilesAtRootStep();

    for (WorkflowStepOutputDataFile file : files.getInputFiles())
      if (!file.isMayNotExist() && !file.getDataFile().exists())
        throw new EoulsanException("For sample "
            + file.getSample().getId() + " in step " + file.getStep().getId()
            + ", input file for " + file.getFormat().getFormatName()
            + " not exists (" + file.getDataFile() + ").");
  }

  //
  // Workflow lifetime methods
  //

  /**
   * Execute the workflow.
   * @param processor result step processor
   * @throws EoulsanException if an error occurs while executing the workflow
   */
  public void execute(final WorkflowStepResultProcessor processor)
      throws EoulsanException {

    // check if output files does not exists
    checkExistingOutputFiles();

    // check if input files exists
    checkExistingInputFiles();

    // Set Steps to WAITING state
    for (AbstractWorkflowStep step : this.steps.keySet())
      step.setState(WAITING);

    final StopWatch stopWatch = new StopWatch();

    while (!getSortedStepsByState(READY, WAITING).isEmpty()) {

      final List<AbstractWorkflowStep> stepsToExecute =
          getSortedStepsByState(READY);

      if (!stepsToExecute.isEmpty()) {

        final AbstractWorkflowStep step = stepsToExecute.get(0);

        // Skip step is necessary
        if (step.isSkip()) {
          step.setState(StepState.DONE);
          continue;
        }

        LOGGER.info("Execute step: " + step.getId());

        // Execute step
        final StepResult result = step.execute();

        if (step.getType() == GENERATOR_STEP || step.getType() == STANDARD_STEP) {

          // Process result
          if (processor != null)
            processor.processResult(step, result);

          // End of the analysis if the analysis fail
          if (!result.isSuccess()) {

            stopWatch.stop();

            LOGGER.severe("Fail of the analysis: " + result.getErrorMessage());
            logEndAnalysis(false, stopWatch);

            if (result.getException() != null)
              Common.errorExit(result.getException(), result.getErrorMessage());
            else
              Common.errorExit(new EoulsanException("Fail of the analysis."),
                  result.getErrorMessage());
          }
        }

        // If the step is terminal step, end of the execution of the workflow
        if (step.getStep() != null && step.getStep().isTerminalStep())
          break;

      }
      logEndAnalysis(true, stopWatch);

    }

  }

  //
  // Utility methods
  //

  /**
   * Get the steps which has some step status. The step are ordered.
   * @param states step status to retrieve
   * @return a sorted list with the steps
   */
  private List<AbstractWorkflowStep> getSortedStepsByState(
      final StepState... states) {

    Preconditions.checkNotNull(states, "states argument is null");

    final List<AbstractWorkflowStep> result = Lists.newArrayList();

    for (StepState state : states)
      result.addAll(getSortedStepsByState(state));

    // Sort steps
    sortListSteps(result);

    return result;
  }

  /**
   * Get the steps which has a step status. The step are ordered.
   * @param states step status to retrieve
   * @return a sorted list with the steps
   */
  private List<AbstractWorkflowStep> getSortedStepsByState(final StepState state) {

    Preconditions.checkNotNull(state, "state argument is null");

    final Collection<AbstractWorkflowStep> collection;

    synchronized (this) {
      collection = this.states.get(state);
    }

    final List<AbstractWorkflowStep> result = Lists.newArrayList(collection);

    sortListSteps(result);

    return result;
  }

  /**
   * Sort a list of step by priority and then by step number.
   * @param list the list of step to sort
   */
  private static final void sortListSteps(final List<AbstractWorkflowStep> list) {

    if (list == null)
      return;

    Collections.sort(list, new Comparator<AbstractWorkflowStep>() {

      @Override
      public int compare(AbstractWorkflowStep a, AbstractWorkflowStep b) {

        int result = a.getType().getPriority() - b.getType().getPriority();

        if (result != 0)
          return result;

        return a.getNumber() - b.getNumber();
      }
    });

  }

  /**
   * Log the state and the time of the analysis
   * @param success true if analysis was successful
   * @param stopwatch stopwatch of the workflow epoch
   */
  private void logEndAnalysis(final boolean success, final StopWatch stopWatch) {

    stopWatch.stop();

    final String successString = success ? "Successful" : "Unsuccessful";

    // Log the end of the analysis
    LOGGER.info(successString
        + " end of the analysis in "
        + StringUtils.toTimeHumanReadable(stopWatch.getElapsedTime() / 1000000)
        + " s.");

    // Send a mail

    final String mailSubject =
        "["
            + Globals.APP_NAME + "] " + successString + " end of your job "
            + context.getJobId() + " on " + context.getJobHost();

    final String mailMessage =
        "THIS IS AN AUTOMATED MESSAGE.\n\n"
            + successString
            + " end of your job "
            + context.getJobId()
            + " on "
            + context.getJobHost()
            + ".\nJob finished at "
            + new Date(System.currentTimeMillis())
            + " in "
            + StringUtils
                .toTimeHumanReadable(stopWatch.getElapsedTime() / 1000000)
            + " s.\n\nOutput files and logs can be found in the following location:\n"
            + context.getOutputPathname() + "\n\nThe " + Globals.APP_NAME
            + "team.";

    // Send mail
    Common.sendMail(mailSubject, mailMessage);
  }

  //
  // Constructor
  //

  /**
   * Protected constructor.
   * @param context the context of the workflow
   * @param design design to use for the workflow
   */
  protected AbstractWorkflow(final WorkflowContext context, final Design design) {

    Preconditions.checkNotNull(context, "Context argument cannot be null");
    Preconditions.checkNotNull(design, "Design argument cannot be null");

    this.context = context;
    this.design = design;
  }

}
