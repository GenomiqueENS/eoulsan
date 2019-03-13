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

import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.ABORTED;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.DONE;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.FAILED;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.PARTIALLY_DONE;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.WORKING;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

import com.googlecode.lanterna.TerminalFacade;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.Terminal.Color;
import com.googlecode.lanterna.terminal.Terminal.SGR;
import com.googlecode.lanterna.terminal.TerminalSize;
import com.googlecode.lanterna.terminal.text.UnixTerminal;

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.Step.StepState;
import fr.ens.biologie.genomique.eoulsan.core.Workflow;

/**
 * This class define an UI using Lanterna library.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class LanternaUI extends AbstractUI implements Terminal.ResizeListener {

  private Workflow workflow;
  private final Map<Step, Double> steps = new HashMap<>();
  private final Map<Step, Integer> submittedTasks = new HashMap<>();
  private final Map<Step, Integer> runningTasks = new HashMap<>();
  private final Map<Step, Integer> doneTasks = new HashMap<>();
  private final Map<Step, Double> stepProgress = new HashMap<>();
  private final Map<Step, StepState> stepState = new HashMap<>();
  private double globalProgress;

  private UnixTerminal terminal;
  private TerminalSize terminalSize;

  private final Map<Step, Integer> stepLines = new HashMap<>();
  private int lineCount;

  private boolean jobDone;

  //
  // UI methods
  //

  @Override
  public String getName() {
    return "lanterna";
  }

  @Override
  public void init(final Workflow workflow) {

    requireNonNull(workflow, "workflow is null");
    this.workflow = workflow;

    // Search step to follow
    searchSteps();

    // Test if is interactive mode
    if (!isInteractiveMode()) {
      return;
    }

    // Set terminal object
    this.terminal = TerminalFacade.createUnixTerminal();

    // Get terminal size
    this.terminal.enterPrivateMode();
    this.terminalSize = this.terminal.getTerminalSize();
    this.terminal.exitPrivateMode();

    // Add resize listener
    this.terminal.addResizeListener(this);

    // Show Welcome message
    System.out.println(Globals.WELCOME_MSG);
  }

  @Override
  public void notifyStepState(final Step step, final StepState stepState) {

    if (step == null
        || step.getWorkflow() != this.workflow || stepState == null) {
      return;
    }

    this.stepState.put(step, stepState);

    switch (stepState) {

    case READY:
      notifyStepState(step, 0, 0, 0.0);
      break;

    case DONE:
    case FAILED:
    case ABORTED:
      notifyStepState(step, 0, 0, 1.0);
      break;

    default:
      break;
    }

  }

  @Override
  public void notifyStepState(final Step step, final int contextId,
      final String contextName, final double progress) {

    // Do nothing
  }

  @Override
  public void notifyStepState(final Step step, final int terminatedTasks,
      final int submittedTasks, final double progress) {

    synchronized (this) {

      if (!checkStep(step)) {
        return;
      }

      if (step == null
          || step.getWorkflow() != this.workflow
          || !this.steps.containsKey(step)) {
        return;
      }

      final StepState state = this.stepState.get(step);

      if (!(state == WORKING
          || state == PARTIALLY_DONE || state == DONE || state == FAILED
          || state == ABORTED)) {
        return;
      }

      this.stepProgress.put(step, progress);
      this.globalProgress = computeGlobalProgress(step, progress);

      print(step, false, false, null);
    }
  }

  @Override
  public void notifyStepState(final Step step, final String note) {

    // Do nothing
  }

  @Override
  public void notifyWorkflowSuccess(final boolean success,
      final String message) {

    synchronized (this) {

      // Do nothing if there is no terminal or if the job is completed
      if (this.terminal == null || this.jobDone) {
        return;
      }
      print(null, true, success, message);
    }

  }

  @Override
  public void notifyTaskSubmitted(final Step step, final int contextId) {

    synchronized (this) {

      if (!checkStep(step)) {
        return;
      }

      notifyTask(step, contextId, this.submittedTasks, 1);
      print(step, false, false, null);
    }
  }

  @Override
  public void notifyTaskRunning(final Step step, final int contextId) {

    synchronized (this) {

      if (!checkStep(step)) {
        return;
      }

      notifyTask(step, contextId, this.runningTasks, 1);
      print(step, false, false, null);
    }
  }

  @Override
  public void notifyTaskDone(final Step step, final int contextId) {

    synchronized (this) {

      if (!checkStep(step)) {
        return;
      }

      notifyTask(step, contextId, this.runningTasks, -1);
      notifyTask(step, contextId, this.doneTasks, 1);
      print(step, false, false, null);
    }
  }

  private void notifyTask(final Step step, final int contextId,
      final Map<Step, Integer> map, final int diff) {

    if (map.containsKey(step)) {

      map.put(step, map.get(step) + diff);
    } else {
      map.put(step, 1);
    }
  }

  //
  // Update progress
  //

  private boolean checkStep(final Step step) {

    // Do nothing if there is no terminal or if the job is completed
    if (this.terminal == null || this.jobDone) {
      return false;
    }

    if (step == null
        || step.getWorkflow() != this.workflow
        || !this.steps.containsKey(step)) {
      return false;
    }

    final StepState state = this.stepState.get(step);

    if (!(state == WORKING
        || state == PARTIALLY_DONE || state == DONE || state == FAILED
        || state == ABORTED)) {
      return false;
    }

    return true;
  }

  private synchronized void print(final Step step, final boolean endWorkflow,
      final boolean success, final String successMessage) {

    this.terminal.setCursorVisible(false);

    if (endWorkflow) {

      if (this.jobDone) {
        return;
      }

      final int lastLineY = this.terminalSize.getRows() - 1;

      // Update workflow progress
      showWorkflowProgress(lastLineY, 1.0, success, successMessage);

      this.terminal.moveCursor(0, lastLineY);
      this.terminal.setCursorVisible(true);
      this.jobDone = true;
      return;
    }

    if (!this.stepLines.containsKey(step)) {
      this.stepLines.put(step, this.lineCount);
      this.lineCount++;
      this.terminal.putCharacter('\n');
    }

    final int lastLineY = this.terminalSize.getRows() - 1;
    final int stepLineY = lastLineY - this.lineCount + this.stepLines.get(step);

    final Integer submittedTasks = this.submittedTasks.get(step);
    final Integer runningTasks = this.runningTasks.get(step);
    final Integer doneTasks = this.doneTasks.get(step);
    final Double stepProgress = this.stepProgress.get(step);

    // Update step progress
    showStepProgress(stepLineY, step.getId(),
        submittedTasks == null ? 0 : submittedTasks,
        runningTasks == null ? 0 : runningTasks,
        doneTasks == null ? 0 : doneTasks,
        stepProgress == null ? 0.0 : stepProgress, this.stepState.get(step));

    // Update workflow progress
    showWorkflowProgress(lastLineY, globalProgress, null, null);

    this.terminal.moveCursor(0, lastLineY);
    this.terminal.setCursorVisible(true);
  }

  /**
   * Show the progress of a step.
   * @param y y position of the line of the step
   * @param stepId id of the step
   * @param terminatedTasks the terminated tasks count
   * @param submittedTasks the submitted tasks count
   * @param progress progress of the step
   */
  private void showStepProgress(final int y, final String stepId,
      final int submittedTasks, final int runningTasks,
      final int terminatedTasks, final double progress, final StepState state) {

    int x = 0;
    x = putString(x, y, " * Step ");
    x = putStringSGR(x, y, String.format("%-40s", stepId), SGR.ENTER_BOLD);
    x = putString(x, y, " ");

    switch (state) {

    case WORKING:
    case PARTIALLY_DONE:
      final String plural1 = submittedTasks > 1 ? "s" : "";
      final String plural2 = runningTasks > 1 ? "s" : "";
      x = putString(x, y,
          format("%3.0f%%    (%d/%d task%s done, %d task%s running)",
              progress * 100, terminatedTasks, submittedTasks, plural1,
              runningTasks, plural2));
      break;

    case DONE:
      x = putStringColor(x, y, state.name(), Color.GREEN);
      break;

    case ABORTED:
    case FAILED:
      x = putStringColor(x, y, state.name(), Color.RED);
      break;

    default:
      break;
    }

    clearEndOfLine(x, y);
  }

  /**
   * Show the progress of the workflow.
   * @param y y position of the line of the workflow
   * @param progress progress of the workflow
   * @param success success of the workflow
   * @param message successMessage
   */
  private void showWorkflowProgress(final int y, final double progress,
      final Boolean success, final String message) {

    int x = 0;

    if (success != null) {

      x = putString(x, y, " * Workflow                                      ");

      if (success) {
        x = putStringColor(x, y, "DONE  ", Color.GREEN);

      } else {
        x = putStringColor(x, y, "FAILED", Color.RED);
      }

      x = putString(x, y, "  " + message);

      this.terminal.putCharacter('\n');
    } else {

      x = putString(x, y,
          String.format("%.0f%% workflow done", progress * 100.0));
    }

    clearEndOfLine(x, y);
  }

  //
  // Terminal methods
  //

  /**
   * Put a string on the terminal.
   * @param x x position of the string
   * @param y y position of the string
   * @param s the string to print
   * @return x position after printing the line
   */
  private int putString(final int x, final int y, final String s) {

    // Do nothing if the string is null
    if (s == null) {
      return x;
    }

    int currentX = x;
    final int max = Math.min(s.length(), this.terminalSize.getColumns() - x);

    for (int i = 0; i < max; i++) {

      this.terminal.moveCursor(currentX, y);
      this.terminal.putCharacter(s.charAt(i));
      currentX++;
    }

    return currentX;
  }

  /**
   * Put a string on the terminal with a defined color.
   * @param x x position of the string
   * @param y y position of the string
   * @param s the string to print
   * @param color the foreground color of the string
   * @return x position after printing the line
   */
  private int putStringColor(final int x, final int y, final String s,
      final Terminal.Color color) {

    if (color != null) {
      this.terminal.applyForegroundColor(color);
    }

    final int result = putString(x, y, s);

    if (color != null) {
      this.terminal.applyForegroundColor(Color.DEFAULT);
    }

    return result;
  }

  /**
   * Put a string on the terminal with a defined color.
   * @param x x position of the string
   * @param y y position of the string
   * @param s the string to print
   * @param sgr the formatting attribute to use
   * @return x position after printing the line
   */
  private int putStringSGR(final int x, final int y, final String s,
      final Terminal.SGR sgr) {

    if (sgr != null) {
      this.terminal.applySGR(sgr);
    }

    final int result = putString(x, y, s);

    if (sgr != null) {

      final Terminal.SGR exitSGR;

      switch (sgr) {

      case ENTER_BLINK:
        exitSGR = Terminal.SGR.EXIT_BLINK;
        break;

      case ENTER_BOLD:
        exitSGR = Terminal.SGR.EXIT_BOLD;
        break;
      case ENTER_REVERSE:
        exitSGR = Terminal.SGR.EXIT_REVERSE;
        break;
      case ENTER_UNDERLINE:
        exitSGR = Terminal.SGR.EXIT_UNDERLINE;
        break;

      default:
        exitSGR = Terminal.SGR.RESET_ALL;
        break;
      }

      this.terminal.applySGR(exitSGR);
    }

    return result;
  }

  /**
   * Clear the end of a terminal line.
   * @param x x position where to start cleaning
   * @param y y position of the line
   */
  private void clearEndOfLine(final int x, final int y) {

    final int max = this.terminalSize.getColumns();

    for (int i = x; i < max; i++) {
      this.terminal.moveCursor(i, y);
      this.terminal.putCharacter(' ');
    }
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

  //
  // Listener
  //

  @Override
  public void onResized(final TerminalSize terminalSize) {

    synchronized (this) {
      this.terminalSize = terminalSize;
    }
  }

}
