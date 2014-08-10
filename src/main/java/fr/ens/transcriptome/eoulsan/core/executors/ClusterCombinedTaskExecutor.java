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
 * This class defined a combined task executor for cluster mode.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ClusterCombinedTaskExecutor implements TaskExecutor {

  private final AbstractTaskExecutor noTaskExecutor;
  private final AbstractTaskExecutor stdTaskExecutor;
  private final AbstractTaskExecutor clusterTaskExecutor;

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
        + this.clusterTaskExecutor.getTotalTaskSubmitedCount();
  }

  @Override
  public int getTotalTaskRunningCount() {

    return this.noTaskExecutor.getTotalTaskRunningCount()
        + this.stdTaskExecutor.getTotalTaskRunningCount()
        + this.clusterTaskExecutor.getTotalTaskRunningCount();
  }

  @Override
  public int getTotalTaskDoneCount() {

    return this.noTaskExecutor.getTotalTaskDoneCount()
        + this.stdTaskExecutor.getTotalTaskDoneCount()
        + this.clusterTaskExecutor.getTotalTaskDoneCount();
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
    this.clusterTaskExecutor.start();
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
    this.clusterTaskExecutor.stop();
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

    if (step.getType() == GENERATOR_STEP) {
      return this.stdTaskExecutor;
    }

    switch (getParallelizationMode(step)) {

    case NOT_NEEDED:
      return this.noTaskExecutor;

    case STANDARD:
    case OWN_PARALELIZATION:
      return this.clusterTaskExecutor;

    default:
      throw new IllegalStateException("Unknown Parallelization mode");
    }
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param threadNumber number of thread to use by the task executor
   */
  public ClusterCombinedTaskExecutor(final int threadNumber) {

    // Create executors
    this.noTaskExecutor = new MonoThreadTaskExecutor();
    this.stdTaskExecutor = new MultiThreadTaskExecutor(threadNumber);
    this.clusterTaskExecutor = new ClusterMultiThreadTaskExecutor();
  }
}
