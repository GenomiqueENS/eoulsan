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

package fr.ens.transcriptome.eoulsan.core;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.core.ProcessSample.ProcessSampleException;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;

/**
 * This class allow to process samples of a step with multithreading.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class ProcessSampleExecutor {

  /** Logger */
  private static final Logger LOGGER = EoulsanLogger.getLogger();

  private static final int CHECKING_DELAY_MS = 5000;
  private static final int WAIT_SHUTDOWN_MINUTES = 60;

  private static final class SampleThread implements Runnable {

    private boolean success;

    private final Context context;
    private final Sample sample;
    private final StepStatus status;
    private final ProcessSample ps;

    private Exception exception;
    private String errorMsg;

    //
    // Getters
    //

    public Exception getException() {
      return this.exception;
    }

    public String getErrorMessage() {
      return this.errorMsg;
    }

    public boolean isSuccess() {
      return this.success;
    }

    @Override
    public void run() {

      try {
        this.ps.processSample(this.context, this.sample, this.status);
        this.success = true;
      } catch (ProcessSampleException e) {
        this.exception = e.getException();
        this.errorMsg = e.getMessage();
      }

    }

    //
    // Constructor
    //

    public SampleThread(final Context context, final ProcessSample ps,
        final Sample sample, final StepStatus status) {

      this.context = context;
      this.sample = sample;
      this.status = status;
      this.ps = ps;

    }
  }

  /**
   * Process all the samples of a design.
   * @param context The Eouslan context of execution
   * @param design Design of to process
   * @param ps ProcessSample Object
   * @return a StepResult object
   */
  public static final StepResult processAllSamples(final Context context,
      final Design design, final StepStatus status, final ProcessSample ps) {

    return processAllSamples(System.currentTimeMillis(), context, design,
        status, EoulsanRuntime.getSettings().getLocalThreadsNumber(), ps);
  }

  /**
   * Process all the samples of a design.
   * @param context The Eouslan context of execution
   * @param design Design of to process
   * @param localThreads number of threads
   * @param maxLocalThreads maximum number of threads
   * @param ps ProcessSample Object
   * @return a StepResult object
   */
  public static final StepResult processAllSamples(final Context context,
      final Design design, final StepStatus status, final int localThreads,
      final int maxLocalThreads, final ProcessSample ps) {

    return processAllSamples(System.currentTimeMillis(), context, design,
        status, getThreadsNumber(localThreads, maxLocalThreads), ps);
  }

  /**
   * Process all the samples of a design.
   * @param startTime the time of the start of step
   * @param context The Eouslan context of execution
   * @param design Design of to process
   * @param ps ProcessSample Object
   * @return a StepResult object
   */
  public static final StepResult processAllSamples(final long startTime,
      final Context context, final Design design, final StepStatus status,
      final ProcessSample ps) {

    return processAllSamples(startTime, context, design, status, EoulsanRuntime
        .getSettings().getLocalThreadsNumber(), ps);
  }

  /**
   * Process all the samples of a design.
   * @param context The Eouslan context of execution
   * @param design Design of to process
   * @param threadNumber the number of threads to use
   * @param ps ProcessSample Object
   * @return a StepResult object
   */
  public static final StepResult processAllSamples(final Context context,
      final Design design, final StepStatus status, final int threadNumber,
      final ProcessSample ps) {

    return processAllSamples(System.currentTimeMillis(), context, design,
        status, threadNumber, ps);
  }

  /**
   * Process all the samples of a design.
   * @param startTime the time of the start of step
   * @param context The Eouslan context of execution
   * @param design Design of to process
   * @param threadNumber the number of threads to use
   * @param ps ProcessSample Object
   * @return a StepResult object
   */
  public static final StepResult processAllSamples(final long startTime,
      final Context context, final Design design, final StepStatus status,
      final int threadNumber, final ProcessSample ps) {

    if (threadNumber > 1) {

      LOGGER.info("Process step with " + threadNumber + " threads.");
      return processAllSamplesWithThreads(startTime, context, design, status,
          ps, threadNumber);
    }

    LOGGER.info("Process step without thread.");
    return processAllSamplesWithNoThread(startTime, context, design, status, ps);
  }

  /**
   * Process all the samples of a design.
   * @param startTime the time of the start of step
   * @param context The Eouslan context of execution
   * @param design Design of to process
   * @param ps ProcessSample Object
   * @return a StepResult object
   */
  private static final StepResult processAllSamplesWithNoThread(
      final long startTime, final Context context, final Design design,
      final StepStatus status, final ProcessSample ps) {

    try {

      // Process all the samples
      for (Sample sample : design.getSamples())
        ps.processSample(context, sample, status);

    } catch (ProcessSampleException e) {
      return status.createStepResult(e.getException(), e.getMessage());
    }

    return status.createStepResult();
  }

  /**
   * Process all the samples of a design with threads.
   * @param startTime the time of the start of step
   * @param context The Eouslan context of execution
   * @param design Design of to process
   * @param ps ProcessSample Object
   * @return a StepResult object
   */
  private static final StepResult processAllSamplesWithThreads(
      final long startTime, final Context context, final Design design,
      final StepStatus status, final ProcessSample ps, final int threadNumber) {

    // Create executor service
    ExecutorService executor = Executors.newFixedThreadPool(threadNumber);

    List<Future<SampleThread>> threads = Lists.newArrayList();

    // Submit all the samples to process
    for (Sample sample : design.getSamples()) {

      final SampleThread st = new SampleThread(context, ps, sample, status);
      threads.add(executor.submit(st, st));
    }

    int samplesNotProcessed = 0;

    // Wait until all samples are processed
    do {

      try {
        Thread.sleep(CHECKING_DELAY_MS);
      } catch (InterruptedException e) {
        LOGGER.warning("InterruptedException: " + e.getMessage());
      }

      samplesNotProcessed = 0;

      for (Future<SampleThread> fst : threads) {

        if (fst.isDone()) {

          try {

            final SampleThread st = fst.get();

            if (!st.isSuccess()) {

              // Close the thread pool
              executor.shutdownNow();

              // Wait the termination of current running task
              executor
                  .awaitTermination(WAIT_SHUTDOWN_MINUTES, TimeUnit.MINUTES);

              // Return error Step Result
              return status.createStepResult(st.getException(),
                  st.getErrorMessage());
            }

          } catch (InterruptedException e) {
            LOGGER.warning("InterruptedException: " + e.getMessage());
          } catch (ExecutionException e) {
            LOGGER.warning("ExecutionException: " + e.getMessage());
          }

        } else {
          samplesNotProcessed++;
        }

      }

    } while (samplesNotProcessed > 0);

    // Close the thread pool
    executor.shutdown();

    return status.createStepResult();
  }

  /**
   * Get the number of threads to use from localThreads, maxLocalThreads and
   * global threads number.
   * @param localThreads number of threads
   * @param maxLocalThreads maximum number of threads
   * @return the number of threads to use
   */
  public static final int getThreadsNumber(final int localThreads,
      final int maxLocalThreads) {
    int threads = EoulsanRuntime.getSettings().getLocalThreadsNumber();

    if (localThreads > 0)
      threads = localThreads;

    if (maxLocalThreads > 0)
      threads = Math.min(threads, maxLocalThreads);

    return threads;
  }

}
