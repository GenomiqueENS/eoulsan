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

package fr.ens.transcriptome.eoulsan.core.schedulers;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType.GENERATOR_STEP;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.core.ParallelizationMode;
import fr.ens.transcriptome.eoulsan.core.workflow.AbstractWorkflowStep;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskContext;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepResult;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepStatus;

/**
 * This class defined a combined task scheduler for cluster mode.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ClusterCombinedTaskScheduler implements TaskScheduler {

  private final AbstractTaskScheduler noTaskScheduler;
  private final AbstractTaskScheduler stdTaskScheduler;
  private final AbstractTaskScheduler clusterTaskScheduler;

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

    getTaskScheduler(step).submit(step, context);
  }

  @Override
  public WorkflowStepStatus getStatus(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    return getTaskScheduler(step).getStatus(step);
  }

  @Override
  public WorkflowStepResult getResult(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    return getTaskScheduler(step).getResult(step);
  }

  @Override
  public int getTaskSubmitedCount(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    return getTaskScheduler(step).getTaskSubmitedCount(step);
  }

  @Override
  public int getTaskRunningCount(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    return getTaskScheduler(step).getTaskRunningCount(step);
  }

  @Override
  public int getTaskDoneCount(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    return getTaskScheduler(step).getTaskDoneCount(step);
  }

  @Override
  public void waitEndOfTasks(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    // Check execution state
    checkExecutionState();

    getTaskScheduler(step).waitEndOfTasks(step);
  }

  @Override
  public int getTotalTaskSubmitedCount() {

    return this.noTaskScheduler.getTotalTaskSubmitedCount()
        + this.stdTaskScheduler.getTotalTaskSubmitedCount()
        + this.clusterTaskScheduler.getTotalTaskSubmitedCount();
  }

  @Override
  public int getTotalTaskRunningCount() {

    return this.noTaskScheduler.getTotalTaskRunningCount()
        + this.stdTaskScheduler.getTotalTaskRunningCount()
        + this.clusterTaskScheduler.getTotalTaskRunningCount();
  }

  @Override
  public int getTotalTaskDoneCount() {

    return this.noTaskScheduler.getTotalTaskDoneCount()
        + this.stdTaskScheduler.getTotalTaskDoneCount()
        + this.clusterTaskScheduler.getTotalTaskDoneCount();
  }

  @Override
  public void start() {

    synchronized (this) {

      // Check execution state
      checkState(!this.isStopped, "The scheduler is stopped");

      this.isStarted = true;
    }

    this.noTaskScheduler.start();
    this.stdTaskScheduler.start();
    this.clusterTaskScheduler.start();
  }

  @Override
  public void stop() {

    // Check execution state
    checkExecutionState();

    synchronized (this) {
      this.isStopped = true;
    }

    this.noTaskScheduler.stop();
    this.stdTaskScheduler.stop();
    this.clusterTaskScheduler.stop();
  }

  //
  // Other method
  //

  /**
   * Check execution state.
   */
  private void checkExecutionState() {

    synchronized (this) {
      checkState(this.isStarted, "The scheduler is not started");
      checkState(!this.isStopped, "The scheduler is stopped");
    }
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
   * Get the task scheduler of a step.
   * @param step the step
   * @return the task scheduler that the step must use
   */
  private TaskScheduler getTaskScheduler(final WorkflowStep step) {

    if (step.getType() == GENERATOR_STEP) {
      return this.stdTaskScheduler;
    }

    switch (getParallelizationMode(step)) {

    case NOT_NEEDED:
      return this.noTaskScheduler;

    case STANDARD:
    case OWN_PARALELIZATION:
      return this.clusterTaskScheduler;

    default:
      throw new IllegalStateException("Unknown Parallelization mode");
    }
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param threadNumber number of thread to use by the task scheduler
   */
  public ClusterCombinedTaskScheduler(final int threadNumber) {

    // Create the schedulers
    this.noTaskScheduler = new MonoThreadTaskScheduler();
    this.stdTaskScheduler = new MultiThreadTaskScheduler(threadNumber);
    this.clusterTaskScheduler = new ClusterMultiThreadTaskScheduler();
  }
}
