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

package fr.ens.transcriptome.eoulsan.core.workflow;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;

/**
 * This class allow to execute step context in mono or multi-thread mode.
 * @author Laurent Jourdren
 * @since 1.3
 */
public class WorkflowStepContextExecutors {

  /** Logger */
  private static final Logger LOGGER = EoulsanLogger.getLogger();

  private static final int CHECKING_DELAY_MS = 5000;
  private static final int WAIT_SHUTDOWN_MINUTES = 60;

  private final ExecutorService executor;
  private final List<Future<ContextThread>> threads = Lists.newArrayList();

  private final WorkflowStepResult result;
  private final WorkflowStepStatus status;

  private static final class ContextThread implements Runnable {

    private final WorkflowStepContextExecutor executor;

    //
    // Getters
    //

    WorkflowStepContextExecutor getExecutor() {
      return this.executor;
    }

    //
    // Runnable method
    //

    @Override
    public void run() {
      executor.execute();
    }

    //
    // Constructor
    //

    public ContextThread(final WorkflowStepContextExecutor executor) {

      this.executor = executor;
    }
  }

  //
  // Getters
  //

  WorkflowStepResult getStepResult() {
    return this.result;
  }

  WorkflowStepStatus getStepStatus() {
    return this.status;
  }

  //
  // Other methods
  //

  /**
   * Process the contexts.
   * @param contexts contexts to process
   */
  void processContexts(final Set<WorkflowStepContext> contexts) {

    Preconditions.checkNotNull(contexts, "contexts argument cannot be null");

    if (this.executor == null) {
      processContextsWithNoThread(contexts);
    } else {
      processContextsWithThreads(contexts);
    }
  }

  /**
   * Process the contexts with no threads.
   * @param contexts contexts to process
   */
  private void processContextsWithNoThread(
      final Set<WorkflowStepContext> contexts) {

    // Do nothing if a context has already failed
    if (!this.result.isSuccess()) {
      return;
    }

    // Process all the contexts
    for (WorkflowStepContext context : contexts) {

      // Create context executor
      final WorkflowStepContextExecutor contextExecutor =
          new WorkflowStepContextExecutor(context, this.status);

      // Execute the context
      contextExecutor.execute();

      // Get the context result
      final WorkflowStepContextResult contextResult =
          contextExecutor.getResult();

      // Add the context result to the step result
      this.result.addResult(contextResult);

      // If context result is not a success do not process other contexts
      if (!contextResult.isSuccess()) {
        return;
      }
    }

  }

  /**
   * Process the contexts with threads.
   * @param contexts contexts to process
   */
  private void processContextsWithThreads(
      final Set<WorkflowStepContext> contexts) {

    // Do nothing if a context has already failed
    if (!this.result.isSuccess()) {
      return;
    }

    // Submit all the context to process
    for (WorkflowStepContext context : contexts) {

      // Create context executor
      final WorkflowStepContextExecutor contextExecutor =
          new WorkflowStepContextExecutor(context, this.status);

      // Create context thread
      final ContextThread st = new ContextThread(contextExecutor);

      // Submit the context thread the thread executor
      synchronized (this.threads) {
        this.threads.add(executor.submit(st, st));
      }
    }

  }

  /**
   * Wait the end of the threads.
   */
  void waitEndOfThreads() {

    // Do nothing if threads are not used
    if (this.executor == null) {
      return;
    }

    int contextNotProcessed;

    // Wait until all samples are processed
    do {

      try {
        Thread.sleep(CHECKING_DELAY_MS);
      } catch (InterruptedException e) {
        LOGGER.warning("InterruptedException: " + e.getMessage());
      }

      contextNotProcessed = 0;

      for (Future<ContextThread> fst : this.threads) {

        if (fst.isDone()) {

          try {

            final ContextThread st = fst.get();

            // Get the context result
            final WorkflowStepContextResult contextResult =
                st.getExecutor().getResult();

            // Add the context result to the step result
            this.result.addResult(contextResult);

            // If context result is not a success do not process other contexts
            if (!contextResult.isSuccess()) {

              // Close the thread pool
              executor.shutdownNow();

              // Wait the termination of current running task
              executor
                  .awaitTermination(WAIT_SHUTDOWN_MINUTES, TimeUnit.MINUTES);
            }

          } catch (InterruptedException e) {
            LOGGER.severe("InterruptedException: " + e.getMessage());
          } catch (ExecutionException e) {
            LOGGER.severe("ExecutionException: " + e.getMessage());
          }

        } else {
          contextNotProcessed++;
        }

      }

    } while (contextNotProcessed > 0);

    // Close the thread pool
    executor.shutdown();
  }

  /**
   * Stop all current processes.
   */
  void stop() {

    if (this.executor != null) {
      this.executor.shutdownNow();
    }
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param step step of which context must be processed
   * @param threadNumber the number of threads to use
   */
  WorkflowStepContextExecutors(final AbstractWorkflowStep step,
      final int threadNumber) {

    this.result = new WorkflowStepResult(step);
    this.status = new WorkflowStepStatus(step);

    // Create executor service
    this.executor =
        threadNumber < 2 ? null : Executors.newFixedThreadPool(threadNumber);
  }

}
