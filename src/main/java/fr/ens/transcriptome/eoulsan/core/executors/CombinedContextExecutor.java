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

package fr.ens.transcriptome.eoulsan.core.executors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.core.ParallelizationMode;
import fr.ens.transcriptome.eoulsan.core.workflow.AbstractWorkflowStep;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepContext;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepResult;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepStatus;

/**
 * This class defined a combined context executor that use several context
 * executors according to the paralellization mode of the step.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class CombinedContextExecutor implements ContextExecutor, Runnable {

  private static final int SLEEP_TIME_IN_MS = 200;

  private final AbstractContextExecutor noContextExecutor;
  private final AbstractContextExecutor stdContextExecutor;
  private final AbstractContextExecutor ownContextExecutor;

  boolean isStarted;
  boolean isStopped;

  @Override
  public void submit(final WorkflowStep step,
      final Set<WorkflowStepContext> contexts) {

    checkNotNull(contexts, "contexts argument cannot be null");

    // Check execution state
    checkExecutionState();

    for (WorkflowStepContext context : contexts) {
      submit(step, context);
    }
  }

  @Override
  public void submit(final WorkflowStep step, final WorkflowStepContext context) {

    checkNotNull(step, "step argument cannot be null");
    checkNotNull(context, "context argument cannot be null");

    // Check execution state
    checkExecutionState();

    getContextExecutor(step).submit(step, context);
  }

  @Override
  public WorkflowStepStatus getStatus(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    return getContextExecutor(step).getStatus(step);
  }

  @Override
  public WorkflowStepResult getResult(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    return getContextExecutor(step).getResult(step);
  }

  @Override
  public int getContextSubmitedCount(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    return getContextExecutor(step).getContextSubmitedCount(step);
  }

  @Override
  public int getContextRunningCount(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    return getContextExecutor(step).getContextRunningCount(step);
  }

  @Override
  public int getContextDoneCount(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    return getContextExecutor(step).getContextDoneCount(step);
  }

  @Override
  public void waitEndOfContexts(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    // Check execution state
    checkExecutionState();

    getContextExecutor(step).waitEndOfContexts(step);
  }

  @Override
  public int getTotalContextSubmitedCount() {

    return this.noContextExecutor.getTotalContextSubmitedCount()
        + this.stdContextExecutor.getTotalContextSubmitedCount()
        + this.ownContextExecutor.getTotalContextSubmitedCount();
  }

  @Override
  public int getTotalContextRunningCount() {

    return this.noContextExecutor.getTotalContextRunningCount()
        + this.stdContextExecutor.getTotalContextRunningCount()
        + this.ownContextExecutor.getTotalContextRunningCount();
  }

  @Override
  public int getTotalContextDoneCount() {

    return this.noContextExecutor.getTotalContextDoneCount()
        + this.stdContextExecutor.getTotalContextDoneCount()
        + this.ownContextExecutor.getTotalContextDoneCount();
  }

  @Override
  public void start() {

    // Check execution state
    checkState(!this.isStopped, "The executor is stopped");

    synchronized (this) {
      this.isStarted = true;
    }

    this.noContextExecutor.start();
    this.stdContextExecutor.start();
    this.ownContextExecutor.start();

    // Pause ownContextExecutor
    this.ownContextExecutor.pause();

    // Start the thread
    new Thread(this, "ContextExecutor_combined").start();
  }

  @Override
  public void stop() {

    // Check execution state
    checkExecutionState();

    synchronized (this) {
      this.isStopped = true;
    }

    this.noContextExecutor.stop();
    this.stdContextExecutor.stop();
    this.ownContextExecutor.stop();
  }

  //
  // Other method
  //

  /**
   * Check execution state.
   */
  private void checkExecutionState() {

    checkState(this.isStarted, "The executor is not started");
    checkState(!this.isStopped, "The executor is stopped");
  }

  /**
   * Get the parallelization mode of a step.
   * @param step the step
   * @return the parallelization mode of the step
   */
  private static ParallelizationMode getParallelizationMode(
      final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    return ((AbstractWorkflowStep) step).getParallelizationMode();
  }

  /**
   * Get the context executor of a step.
   * @param step the step
   * @return the context executor that the step must use
   */
  private ContextExecutor getContextExecutor(final WorkflowStep step) {

    switch (getParallelizationMode(step)) {

    case NOT_NEEDED:
      return this.noContextExecutor;

    case STANDARD:
      return this.stdContextExecutor;

    case OWN_PARALELIZATION:
      return this.ownContextExecutor;

    default:
      throw new IllegalStateException("Unknown Parallelization mode");
    }
  }

  //
  // Runnable method
  //

  @Override
  public void run() {

    while (!this.isStopped) {

      // Is there some task to do by ownContextExecutor ?
      if (this.ownContextExecutor.isPaused()
          && this.ownContextExecutor.getTotalWaitingCount() > 0) {

        // If std executor running, pause it
        if (!this.stdContextExecutor.isPaused()) {
          this.stdContextExecutor.pause();
        }

        // When std executor has finishing current running task resume own
        // executor
        if (this.stdContextExecutor.getTotalContextRunningCount() == 0) {
          this.ownContextExecutor.resume();
        }
      }

      if (!this.ownContextExecutor.isPaused()
          && this.ownContextExecutor.getTotalWaitingCount() == 0) {

        this.ownContextExecutor.pause();
        this.stdContextExecutor.resume();
      }

      // Wait
      try {
        Thread.sleep(SLEEP_TIME_IN_MS);
      } catch (InterruptedException e) {
        EoulsanLogger.getLogger().severe(e.getMessage());
      }
    }
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param threadNumber number of thread to use by the context executor
   */
  public CombinedContextExecutor(final int threadNumber) {

    checkArgument(threadNumber > 0, "threadNumber must be > 0");

    // Create executor service
    this.stdContextExecutor = new MultiThreadContextExecutor(threadNumber);
    this.noContextExecutor = new MonoThreadContextExecutor();
    this.ownContextExecutor = new MonoThreadContextExecutor();
  }

}
