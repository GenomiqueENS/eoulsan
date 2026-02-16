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

import static com.google.common.base.Preconditions.checkArgument;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntimeException;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.workflow.TaskContextImpl;
import fr.ens.biologie.genomique.eoulsan.core.workflow.TaskResultImpl;

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

    private final TaskContextImpl context;
    private final long submissionTime;
    private Throwable e;
    private boolean done;

    @Override
    public void run() {

      try {

        // Do nothing if scheduler is stopped
        if (isStopped()) {
          this.done = true;
          return;
        }

        // Set task in running state
        beforeExecuteTask(this.context);

        // Execute the context
        final TaskResultImpl result = executeTask(this.context);

        // Do nothing if scheduler is stopped
        if (isStopped()) {
          this.done = true;
          return;
        }

        // Set task in done state
        afterExecuteTask(this.context, result);

        this.done = true;

      } catch (Throwable e) {

        this.e = e;
      }

    }

    public void fail(final boolean cancel) {

      final long endTime = System.currentTimeMillis();

      final Throwable exception = this.e != null
          ? this.e
          : new EoulsanRuntimeException("Task #"
              + context.getId() + "has failed without exception, cancel="
              + cancel);

      final TaskResultImpl result = new TaskResultImpl(this.context,
          Instant.ofEpochMilli(this.submissionTime),
          Instant.ofEpochMilli(endTime), endTime - this.submissionTime,
          exception, exception.getMessage());

      afterExecuteTask(this.context, result);

    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param context context to execute
     */
    TaskThread(final TaskContextImpl context) {

      this.context = context;
      this.submissionTime = System.currentTimeMillis();
    }
  }

  @Override
  public void submit(final Step step, final TaskContextImpl context) {

    // Call to the super method
    super.submit(step, context);

    // Create context thread
    final TaskThread st = new TaskThread(context);

    // Get the number of required processors
    final int requiredProcessors =
        context.getCurrentStep().getRequiredProcessors();

    // Submit the context thread the thread executor
    synchronized (this.threads) {
      this.threads.add(this.executor.submit(st, st, requiredProcessors));
    }
  }

  @Override
  public void start() {

    // Call to the super method
    super.start();

    new Thread(this, "TaskScheduler_multi_thread").start();
  }

  @Override
  public void stop() {

    // Call to the super method
    super.stop();

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

    // Call to the super method
    super.pause();

    this.executor.pause();
  }

  @Override
  public void resume() {

    // Call to the super method
    super.resume();

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

              tt.fail(ftt.isCancelled());
            }

            // Add the task to the list of task to remove
            threadsToRemove.add(ftt);

          } catch (InterruptedException | ExecutionException e) {
            getLogger().severe("Unexcepted exception in "
                + this.getClass().getSimpleName() + ".run(): "
                + e.getMessage());
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
