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

package fr.ens.biologie.genomique.eoulsan.ui;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Strings;

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.Step.StepState;
import fr.ens.biologie.genomique.eoulsan.core.Workflow;

/**
 * This class define a basic UI for Eoulsan.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class BasicUI extends AbstractUI {

  private Workflow workflow;
  private final Map<Step, Double> steps = new HashMap<>();
  private final Map<Step, StepState> stepState = new HashMap<>();


  private int lastMessageLength = 0;

  //
  // UI methods
  //

  @Override
  public String getName() {
    return "basic";
  }

  @Override
  public void init(final Workflow workflow) {

    checkNotNull(workflow, "workflow is null");
    this.workflow = workflow;

    // Search step to follow
    searchSteps();
  }

  @Override
  public void notifyStepState(final Step step, final StepState stepState) {

    // Check if the UI has been initialized
    checkState(this.workflow != null, "The UI has not been initialized");

    if (step == null
        || stepState == null || step.getWorkflow() != this.workflow) {
      return;
    }

    this.stepState.put(step, stepState);

    if (stepState == StepState.WORKING) {
      notifyStepState(step, 0, 0, 0.0);
    }
  }

  @Override
  public void notifyStepState(final Step step, final int contextId,
      final String contextName, final double progress) {

    // Check if the UI has been initialized
    checkState(this.workflow != null, "The UI has not been initialized");

    // DO nothing
  }

  @Override
  public void notifyStepState(final Step step, final int terminatedTasks,
      final int submittedTasks, final double progress) {

    // Check if the UI has been initialized
    checkState(this.workflow != null, "The UI has not been initialized");

    if (!isInteractiveMode()
        || step == null || step.getWorkflow() != this.workflow
        || this.stepState.get(step) != StepState.WORKING
        || !this.steps.containsKey(step)) {
      return;
    }

    final double globalProgress = computeGlobalProgress(step, progress);

    if (globalProgress == 0 && this.lastMessageLength == 0) {
      System.out.println(Globals.WELCOME_MSG);
    }

    final String msg = String.format(
        "%.0f%% workflow done (currently process step %s #%d, %.0f%% done)",
        globalProgress * 100.0, step.getId(), step.getNumber(),
        progress * 100.0);

    // Clear previous message
    System.out.print(Strings.repeat("\r", this.lastMessageLength));
    System.out.print(Strings.repeat(" ", this.lastMessageLength));
    System.out.print(Strings.repeat("\r", this.lastMessageLength));
    this.lastMessageLength = msg.length();

    System.out.print(msg);

    // At the end of the workflow print \n
    if (globalProgress == 1.0) {
      System.out.println();
    }

    System.out.flush();

  }

  @Override
  public void notifyStepState(final Step step, final String note) {

    // Check if the UI has been initialized
    checkState(this.workflow != null, "The UI has not been initialized");

    if (step == null || step.getWorkflow() != this.workflow) {
      return;
    }

    System.out.printf("Step "
        + step.getId() + " " + this.stepState.get(step).toString() + " note: "
        + note);
  }

  @Override
  public void notifyWorkflowSuccess(final boolean success,
      final String message) {
    // Do nothing
  }

  @Override
  public void notifyTaskSubmitted(Step step, int contextId) {
    // Do nothing
  }

  @Override
  public void notifyTaskRunning(Step step, int contextId) {
    // Do nothing
  }

  @Override
  public void notifyTaskDone(Step step, int contextId) {
    // Do nothing
  }

  //
  // Other methods
  //

  /**
   * Search steps to follow.
   */
  private void searchSteps() {

    for (Step step : this.workflow.getSteps()) {

      if (step == null) {
        continue;
      }

      switch (step.getType()) {
      case CHECKER_STEP:
      case GENERATOR_STEP:
      case STANDARD_STEP:

        if (!step.isSkip()) {
          this.steps.put(step, 0.0);
        }

        break;

      default:
        break;
      }
    }
  }

  /**
   * Compute global progress.
   * @param step step to update progress
   * @param progress progress value of the step
   * @return global progress as percent
   */
  private double computeGlobalProgress(final Step step, final double progress) {

    if (!this.steps.containsKey(step)) {
      return -1;
    }

    // Update progress
    this.steps.put(step, progress);

    double sum = 0;
    for (double p : this.steps.values()) {
      sum += p;
    }

    return sum / this.steps.size();
  }

}
