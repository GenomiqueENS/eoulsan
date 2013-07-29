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
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public abstract class AbstractWorkflow implements Workflow {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private final Design design;
  private final WorkflowContext context = new WorkflowContext();
  private final Set<String> stepIds = Sets.newHashSet();
  private final Map<AbstractWorkflowStep, StepState> steps = Maps.newHashMap();
  private final Multimap<StepState, AbstractWorkflowStep> states =
      ArrayListMultimap.create();

  //
  // Inner interface
  //

  public interface WorkflowStepResultProcessor {

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

  WorkflowContext getWorkflowContext() {

    return this.context;
  }

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

  List<AbstractWorkflowStep> getSortedStepsByState(final StepState state) {

    Preconditions.checkNotNull(state, "state argument is null");

    final Collection<AbstractWorkflowStep> collection;

    synchronized (this) {
      collection = this.states.get(state);
    }

    final List<AbstractWorkflowStep> result = Lists.newArrayList(collection);

    sortListSteps(result);

    return result;
  }

  public Set<WorkflowStep> getSteps() {

    final Set<WorkflowStep> result = Sets.newHashSet();
    result.addAll(this.steps.keySet());

    return Collections.unmodifiableSet(result);
  }

  //
  // Setters
  //

  void register(final AbstractWorkflowStep step) {

    Preconditions.checkNotNull(step, "step cannot be null");

    if (step.getWorkflow() != this)
      throw new IllegalStateException(
          "step cannot be part of more than one workflow");

    if (this.stepIds.contains(step.getId()))
      throw new IllegalStateException("2 step cannot had the same id: "
          + step.getId());

    synchronized (this) {
      this.steps.put(step, step.getState());
      this.states.put(step.getState(), step);
    }
  }

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

  public void showDependenies() {

    System.out.println("=== dependencies ===");

    for (WorkflowStep step : this.steps.keySet()) {

      ((AbstractWorkflowStep) step).printDeps();

    }

  }

  //
  // Check methods
  //

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

  private void checkExistingInputFiles() throws EoulsanException {

    final WorkflowFiles files = getWorkflowFilesAtRootStep();

    for (WorkflowStepOutputDataFile file : files.getInputFiles())
      if (!file.isMayNotExist() && !file.getDataFile().exists())
        throw new EoulsanException("For sample "
            + file.getSample().getId() + " in step " + file.getStep().getId()
            + ", input file for " + file.getFormat().getFormatName()
            + " not exists (" + file.getDataFile() + ").");
  }

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
   * @param startTime start time of the analysis is milliseconds since Java
   *          epoch
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

  protected AbstractWorkflow(final Design design) {

    Preconditions.checkNotNull(design, "Design argument cannot be null");

    this.design = design;
  }

}
