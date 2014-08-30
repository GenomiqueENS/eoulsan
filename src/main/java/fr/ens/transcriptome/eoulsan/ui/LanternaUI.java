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

package fr.ens.transcriptome.eoulsan.ui;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.Map;

import com.google.common.collect.Maps;
import com.googlecode.lanterna.TerminalFacade;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.Terminal.Color;
import com.googlecode.lanterna.terminal.Terminal.SGR;
import com.googlecode.lanterna.terminal.TerminalSize;
import com.googlecode.lanterna.terminal.text.UnixTerminal;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.workflow.Workflow;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState;

/**
 * This class define an UI using Lanterna library.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class LanternaUI implements UI, Terminal.ResizeListener {

  private Workflow workflow;
  private final Map<WorkflowStep, Double> steps = Maps.newHashMap();
  private UnixTerminal terminal;
  private TerminalSize terminalSize;

  private final Map<WorkflowStep, Integer> stepLines = Maps.newHashMap();
  private int lineCount;

  //
  // UI methods
  //

  @Override
  public String getName() {
    return "lanterna";
  }

  @Override
  public void init(final Workflow workflow) {

    checkNotNull(workflow, "workflow is null");
    this.workflow = workflow;

    // Search step to follow
    searchSteps();

    // Test if Eoulsan has been launched in interactive mode
    final boolean interactiveMode = System.console() != null;

    this.terminal =
        interactiveMode ? TerminalFacade.createUnixTerminal() : null;

    // Get terminal size
    this.terminal.enterPrivateMode();
    this.terminalSize = terminal.getTerminalSize();
    this.terminal.exitPrivateMode();

    // Add resize listener
    this.terminal.addResizeListener(this);

    // Show Welcome message
    System.out.println(Globals.WELCOME_MSG);
  }

  @Override
  public void notifyStepState(final WorkflowStep step) {

    if (step == null || step.getWorkflow() != this.workflow)
      return;

    if (step.getState() == StepState.WORKING)
      notifyStepState(step, 0.0);
  }

  @Override
  public void notifyStepState(final WorkflowStep step, final int contextId,
      final String contextName, final double progress) {

    // Do nothing
  }

  @Override
  public void notifyStepState(final WorkflowStep step, double progress) {

    if (this.terminal == null
        || step == null || step.getWorkflow() != this.workflow
        || step.getState() != StepState.WORKING
        || !this.steps.containsKey(step))
      return;

    final double globalProgress = computeGlobalProgress(step, progress);

    if (!this.stepLines.containsKey(step)) {
      this.stepLines.put(step, this.lineCount);
      this.lineCount++;
      this.terminal.putCharacter('\n');
    }

    final int lastLineY = this.terminalSize.getRows() - 1;
    final int stepLineY = lastLineY - this.lineCount + this.stepLines.get(step);

    // Update step progress
    showStepProgress(stepLineY, step.getId(), progress);

    // Update workflow progress
    showWorkflowProgress(lastLineY, globalProgress);

    this.terminal.moveCursor(0, lastLineY);
  }

  @Override
  public void notifyStepState(final WorkflowStep step, final String note) {

    // Do nothing
  }

  //
  // Update progress
  //

  /**
   * Show the progress of a step.
   * @param y y position of the line of the step
   * @param stepId id of the step
   * @param progress progress of the step
   */
  private void showStepProgress(final int y, final String stepId,
      final double progress) {

    int x = 0;
    x = putString(x, y, " * Step ");
    x = putStringSGR(x, y, String.format("%-40s", stepId), SGR.ENTER_BOLD);
    x = putString(x, y, " ");

    if (progress == 1.0) {
      x = putStringColor(x, y, "DONE", Color.GREEN);
    } else {
      x = putString(x, y, format("%.0f%% done", progress * 100));
    }

    clearEndOfLine(x, y);
  }

  /**
   * Show the progress of the workflow.
   * @param y y position of the line of the workflow
   * @param progress progress of the workflow
   */
  private void showWorkflowProgress(final int y, final double progress) {

    int x = 0;

    if (progress == 1.0) {

      x = putString(x, y, " * Workflow                                      ");
      x = putStringColor(x, y, "DONE", Color.GREEN);
      this.terminal.putCharacter('\n');
    } else {

      x =
          putString(x, y,
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
      this.terminal.applyForegroundColor(Color.BLACK);
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

    for (WorkflowStep step : this.workflow.getSteps()) {

      if (step == null)
        continue;

      switch (step.getType()) {
      case CHECKER_STEP:
      case GENERATOR_STEP:
      case STANDARD_STEP:

        if (!step.isSkip())
          this.steps.put(step, 0.0);

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
  private double computeGlobalProgress(final WorkflowStep step,
      final double progress) {

    if (!this.steps.containsKey(step))
      return -1;

    // Update progress
    this.steps.put(step, progress);

    double sum = 0;
    for (double p : this.steps.values())
      sum += p;

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
