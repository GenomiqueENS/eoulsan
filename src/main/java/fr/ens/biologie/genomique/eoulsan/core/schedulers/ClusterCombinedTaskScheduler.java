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

package fr.ens.biologie.genomique.eoulsan.core.schedulers;

import static com.google.common.base.Preconditions.checkState;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepType.GENERATOR_STEP;
import static java.util.Objects.requireNonNull;

import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.core.ParallelizationMode;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.schedulers.clusters.ClusterTaskScheduler;
import fr.ens.biologie.genomique.eoulsan.core.workflow.AbstractStep;
import fr.ens.biologie.genomique.eoulsan.core.workflow.StepResult;
import fr.ens.biologie.genomique.eoulsan.core.workflow.StepStatus;
import fr.ens.biologie.genomique.eoulsan.core.workflow.TaskContextImpl;

/**
 * This class defined a combined task scheduler for cluster mode.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ClusterCombinedTaskScheduler implements TaskScheduler {

  private final AbstractTaskScheduler noTaskScheduler;
  private final AbstractTaskScheduler stdTaskScheduler;
  private final AbstractTaskScheduler clusterTaskScheduler;

  private volatile boolean isStarted;
  private volatile boolean isStopped;

  @Override
  public void submit(final Step step, final Set<TaskContextImpl> contexts) {

    requireNonNull(contexts, "contexts argument cannot be null");

    // Check execution state
    checkExecutionState();

    for (TaskContextImpl context : contexts) {
      submit(step, context);
    }
  }

  @Override
  public void submit(final Step step, final TaskContextImpl context) {

    requireNonNull(step, "step argument cannot be null");
    requireNonNull(context, "context argument cannot be null");

    // Check execution state
    checkExecutionState();

    getTaskScheduler(step).submit(step, context);
  }

  @Override
  public StepStatus getStatus(final Step step) {

    requireNonNull(step, "step argument cannot be null");

    return getTaskScheduler(step).getStatus(step);
  }

  @Override
  public StepResult getResult(final Step step) {

    requireNonNull(step, "step argument cannot be null");

    return getTaskScheduler(step).getResult(step);
  }

  @Override
  public int getTaskSubmittedCount(final Step step) {

    requireNonNull(step, "step argument cannot be null");

    return getTaskScheduler(step).getTaskSubmittedCount(step);
  }

  @Override
  public int getTaskRunningCount(final Step step) {

    requireNonNull(step, "step argument cannot be null");

    return getTaskScheduler(step).getTaskRunningCount(step);
  }

  @Override
  public int getTaskDoneCount(final Step step) {

    requireNonNull(step, "step argument cannot be null");

    return getTaskScheduler(step).getTaskDoneCount(step);
  }

  @Override
  public void waitEndOfTasks(final Step step) {

    requireNonNull(step, "step argument cannot be null");

    // Check execution state
    checkExecutionState();

    getTaskScheduler(step).waitEndOfTasks(step);
  }

  @Override
  public int getTotalTaskSubmittedCount() {

    return this.noTaskScheduler.getTotalTaskSubmittedCount()
        + this.stdTaskScheduler.getTotalTaskSubmittedCount()
        + this.clusterTaskScheduler.getTotalTaskSubmittedCount();
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
  private static ParallelizationMode getParallelizationMode(final Step step) {

    requireNonNull(step, "step argument cannot be null");

    return ((AbstractStep) step).getParallelizationMode();
  }

  /**
   * Get the task scheduler of a step.
   * @param step the step
   * @return the task scheduler that the step must use
   */
  private TaskScheduler getTaskScheduler(final Step step) {

    if (step.getType() == GENERATOR_STEP) {
      return this.stdTaskScheduler;
    }

    switch (getParallelizationMode(step)) {

    case NOT_NEEDED:
      return this.noTaskScheduler;

    case STANDARD:
    case OWN_PARALLELIZATION:
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
   * @param clusterScheduler cluster scheduler to use
   */
  public ClusterCombinedTaskScheduler(final int threadNumber,
      final ClusterTaskScheduler clusterScheduler) {

    requireNonNull(clusterScheduler, "clusterScheduler argument cannot be null");

    // Create the schedulers
    this.noTaskScheduler = new MonoThreadTaskScheduler();
    this.stdTaskScheduler = new MultiThreadTaskScheduler(threadNumber);
    this.clusterTaskScheduler = (AbstractTaskScheduler) clusterScheduler;
  }
}
