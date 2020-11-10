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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class define a Pausable thread pool executor. This class is based on the
 * javadoc documentation of the ThreadPoolExecutor class.
 * @since 2.0
 */
public class PausableThreadPoolExecutor extends ThreadPoolExecutor {

  private boolean isPaused;
  private final ReentrantLock pauseLock = new ReentrantLock();
  private final Condition unPaused = this.pauseLock.newCondition();

  private final int maxThreads;
  private volatile int threadsAvailable;
  private final Map<Future<?>, Integer> requirements =
      new ConcurrentHashMap<>();

  /**
   * Submit a task.
   * @param task the task to submmit
   * @param result the result
   * @param requiredProcessors the required processor number
   * @return a Future task
   */
  public <T> Future<T> submit(Runnable task, T result, int requiredProcessors) {

    // The number of thread of the task cannot excess the maximal number of
    // threads and if the number of required processors is not set, use 1 as
    // default value
    int requiredThreads = requiredProcessors < 1
        ? 1 : Math.min(requiredProcessors, this.maxThreads);

    Future<T> submitResult = super.submit(task, result);
    this.requirements.put(submitResult, requiredThreads);

    return submitResult;
  }

  @Override
  protected void beforeExecute(final Thread t, final Runnable r) {

    super.beforeExecute(t, r);

    // Wait if requirements has not been yet updated
    while (!this.requirements.containsKey(r)) {
      sleep();
    }

    int requiredThreads = this.requirements.get(r);

    this.pauseLock.lock();

    try {
      while (this.isPaused) {
        this.unPaused.await();
      }
    } catch (InterruptedException ie) {
      t.interrupt();
    } finally {
      this.pauseLock.unlock();
    }

    // Sleep until threads are available
    while (this.threadsAvailable - requiredThreads < 0) {
      sleep();
    }

    synchronized (this) {
      this.threadsAvailable -= requiredThreads;
    }

  }

  @Override
  protected void afterExecute(Runnable task, Throwable t) {

    int requiredThreads = this.requirements.get(task);

    synchronized (this) {
      this.threadsAvailable += requiredThreads;
    }

    this.requirements.remove(task);

    super.afterExecute(task, t);
  }

  /**
   * Pause the executor.
   */
  public void pause() {

    this.pauseLock.lock();

    try {
      this.isPaused = true;
    } finally {
      this.pauseLock.unlock();
    }
  }

  /**
   * Resume the executor.
   */
  public void resume() {

    this.pauseLock.lock();

    try {
      this.isPaused = false;
      this.unPaused.signalAll();
    } finally {
      this.pauseLock.unlock();
    }
  }

  /**
   * Sleep 1 second.
   */
  private static void sleep() {

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      // Do nothing
    }
  }

  //
  // Constructor
  //

  /**
   * public constructor.
   * @param threadNumber number of threads
   */
  public PausableThreadPoolExecutor(final int threadNumber) {

    super(threadNumber, threadNumber, 0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>());

    this.maxThreads = threadNumber < 1 ? 1 : threadNumber;
    this.threadsAvailable = this.maxThreads;
  }

}
