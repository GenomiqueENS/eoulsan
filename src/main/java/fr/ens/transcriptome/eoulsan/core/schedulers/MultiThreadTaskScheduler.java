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

import static com.google.common.base.Preconditions.checkArgument;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskContext;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskResult;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;

/**
 * This class define a muti thread scheduler.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class MultiThreadTaskScheduler extends AbstractTaskScheduler
    implements Runnable {

  private static final int SLEEP_TIME_IN_MS = 500;
  private static final int WAIT_SHUTDOWN_MINUTES = 60;

  private final PausableThreadPoolExecutor executor;
  private final Set<Future<TaskThread>> threads = new HashSet<>();

  /**
   * Wrapper class around a call to executeTask methods.
   * @author Laurent Jourdren
   */
  private final class TaskThread implements Runnable {

    private final TaskContext context;
    private final long submissionTime;
    private Throwable e;
    private boolean done;

    @Override
    public void run() {

      getLogger()
          .finest("Start of TaskThread.run() for Task #" + context.getId());

      try {

        // Execute the context
        beforeExecuteTask(this.context);
        afterExecuteTask(this.context, executeTask(this.context));

      } catch (Throwable e) {

        this.e = e;
      }

      getLogger()
          .finest("End of TaskThread.run() for Task #" + context.getId());
    }

    public void fail(final boolean cancel) {

      final long endTime = System.currentTimeMillis();

      final Throwable exception = cancel
          ? new EoulsanRuntimeException(
              "Task #" + context.getId() + "has been canceled")
          : this.e;

      final TaskResult result = new TaskResult(this.context,
          new Date(this.submissionTime), new Date(endTime),
          endTime - this.submissionTime, exception, exception.getMessage());

      afterExecuteTask(this.context, result);

      this.done = true;
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param context context to execute
     */
    TaskThread(final TaskContext context) {

      this.context = context;
      this.submissionTime = System.currentTimeMillis();
    }
  }

  @Override
  public void submit(final WorkflowStep step, final TaskContext context) {

    // Call to the super method
    super.submit(step, context);

    getLogger().finest(
        "MultiThreadTaskScheduler.submit(): before creating TaskThread for Task #"
            + context.getId());

    // Create context thread
    final TaskThread st = new TaskThread(context);

    getLogger().finest(
        "MultiThreadTaskScheduler.submit(): before submitting TaskThread for Task #"
            + context.getId());

    // Submit the context thread the thread executor
    synchronized (this.threads) {
      this.threads.add(this.executor.submit(st, st));
    }

    getLogger().finest(
        "MultiThreadTaskScheduler.submit(): after submitting TaskThread for Task #"
            + context.getId());
  }

  @Override
  public void start() {

    super.start();
    new Thread(this, "TaskScheduler_multi_thread").start();
  }

  @Override
  public void stop() {

    try {

      // Shutdown the executor
      this.executor.shutdownNow();

      // Wait the termination of current running task
      this.executor.awaitTermination(WAIT_SHUTDOWN_MINUTES, TimeUnit.MINUTES);

    } catch (InterruptedException e) {
      getLogger().severe(e.getMessage());
    }
  }

  @Override
  public void pause() {

    this.executor.pause();
  }

  @Override
  public void resume() {

    this.executor.resume();
  }

  //
  // Runnable method
  //

  @Override
  public void run() {

    // The list of finished tasks
    final List<Future<TaskThread>> threadsToRemove = new ArrayList<>();

    while (!this.isStopped()) {

      for (Future<TaskThread> ftt : this.threads) {

        // For all finished tasks
        if (ftt.isDone()) {

          try {

            final TaskThread tt = ftt.get();

            // Check if the task has been correctly executed
            if (!tt.done) {

              getLogger().finest("Task #"
                  + tt.context.getId() + " has failed in "
                  + this.getClass().getName());

              tt.fail(ftt.isCancelled());
            }

            // Add the task to the list of task to remove
            threadsToRemove.add(ftt);

          } catch (InterruptedException | ExecutionException e) {
            // Never occurs
          }
        }
      }

      // Remove the finished tasks from the list of tasks
      if (!this.threads.isEmpty()) {

        synchronized (threadsToRemove) {
          for (Future<TaskThread> ftt : threadsToRemove) {
            this.threads.remove(ftt);
          }
        }

        threadsToRemove.clear();
      }

      // Wait
      try {
        Thread.sleep(SLEEP_TIME_IN_MS);
      } catch (InterruptedException e) {
        getLogger().severe(e.getMessage());
      }
    }
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param threadNumber number of thread to use by the task scheduler
   */
  public MultiThreadTaskScheduler(final int threadNumber) {

    checkArgument(threadNumber > 0, "threadNumber must be > 0");

    // Create executor service
    this.executor = new PausableThreadPoolExecutor(threadNumber);
  }

}
