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
import fr.ens.transcriptome.eoulsan.core.workflow.TaskContext;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepResult;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepStatus;

/**
 * This class defined a combined task executor that use several context
 * executors according to the parallelization mode of the step.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class CombinedTaskExecutor implements TaskExecutor, Runnable {

  private static final int SLEEP_TIME_IN_MS = 200;

  private final AbstractTaskExecutor noTaskExecutor;
  private final AbstractTaskExecutor stdTaskExecutor;
  private final AbstractTaskExecutor ownTaskExecutor;

  boolean isStarted;
  boolean isStopped;

  @Override
  public void submit(final WorkflowStep step, final Set<TaskContext> contexts) {

    checkNotNull(contexts, "contexts argument cannot be null");

    // Check execution state
    checkExecutionState();

    for (TaskContext context : contexts) {
      submit(step, context);
    }
  }

  @Override
  public void submit(final WorkflowStep step, final TaskContext context) {

    checkNotNull(step, "step argument cannot be null");
    checkNotNull(context, "context argument cannot be null");

    // Check execution state
    checkExecutionState();

    getTaskExecutor(step).submit(step, context);
  }

  @Override
  public WorkflowStepStatus getStatus(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    return getTaskExecutor(step).getStatus(step);
  }

  @Override
  public WorkflowStepResult getResult(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    return getTaskExecutor(step).getResult(step);
  }

  @Override
  public int getTaskSubmitedCount(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    return getTaskExecutor(step).getTaskSubmitedCount(step);
  }

  @Override
  public int getTaskRunningCount(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    return getTaskExecutor(step).getTaskRunningCount(step);
  }

  @Override
  public int getTaskDoneCount(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    return getTaskExecutor(step).getTaskDoneCount(step);
  }

  @Override
  public void waitEndOfTasks(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    // Check execution state
    checkExecutionState();

    getTaskExecutor(step).waitEndOfTasks(step);
  }

  @Override
  public int getTotalTaskSubmitedCount() {

    return this.noTaskExecutor.getTotalTaskSubmitedCount()
        + this.stdTaskExecutor.getTotalTaskSubmitedCount()
        + this.ownTaskExecutor.getTotalTaskSubmitedCount();
  }

  @Override
  public int getTotalTaskRunningCount() {

    return this.noTaskExecutor.getTotalTaskRunningCount()
        + this.stdTaskExecutor.getTotalTaskRunningCount()
        + this.ownTaskExecutor.getTotalTaskRunningCount();
  }

  @Override
  public int getTotalTaskDoneCount() {

    return this.noTaskExecutor.getTotalTaskDoneCount()
        + this.stdTaskExecutor.getTotalTaskDoneCount()
        + this.ownTaskExecutor.getTotalTaskDoneCount();
  }

  @Override
  public void start() {

    // Check execution state
    checkState(!this.isStopped, "The executor is stopped");

    synchronized (this) {
      this.isStarted = true;
    }

    this.noTaskExecutor.start();
    this.stdTaskExecutor.start();
    this.ownTaskExecutor.start();

    // Pause ownTaskExecutor
    this.ownTaskExecutor.pause();

    // Start the thread
    new Thread(this, "TaskExecutor_combined").start();
  }

  @Override
  public void stop() {

    // Check execution state
    checkExecutionState();

    synchronized (this) {
      this.isStopped = true;
    }

    this.noTaskExecutor.stop();
    this.stdTaskExecutor.stop();
    this.ownTaskExecutor.stop();
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
   * Get the task executor of a step.
   * @param step the step
   * @return the task executor that the step must use
   */
  private TaskExecutor getTaskExecutor(final WorkflowStep step) {

    switch (getParallelizationMode(step)) {

    case NOT_NEEDED:
      return this.noTaskExecutor;

    case STANDARD:
      return this.stdTaskExecutor;

    case OWN_PARALELIZATION:
      return this.ownTaskExecutor;

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

      // Is there some task to do by ownTaskExecutor ?
      if (this.ownTaskExecutor.isPaused()
          && this.ownTaskExecutor.getTotalWaitingCount() > 0) {

        // If std executor running, pause it
        if (!this.stdTaskExecutor.isPaused()) {
          this.stdTaskExecutor.pause();
        }

        // When std executor has finishing current running task resume own
        // executor
        if (this.stdTaskExecutor.getTotalTaskRunningCount() == 0) {
          this.ownTaskExecutor.resume();
        }
      }

      if (!this.ownTaskExecutor.isPaused()
          && this.ownTaskExecutor.getTotalWaitingCount() == 0) {

        this.ownTaskExecutor.pause();
        this.stdTaskExecutor.resume();
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
   * @param threadNumber number of thread to use by the task executor
   */
  public CombinedTaskExecutor(final int threadNumber) {

    checkArgument(threadNumber > 0, "threadNumber must be > 0");

    // Create executor service
    this.stdTaskExecutor = new MultiThreadTaskExecutor(threadNumber);
    this.noTaskExecutor = new MonoThreadTaskExecutor();
    this.ownTaskExecutor = new MonoThreadTaskExecutor();
  }

}
