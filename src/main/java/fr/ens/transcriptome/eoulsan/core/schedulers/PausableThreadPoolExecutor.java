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
  private ReentrantLock pauseLock = new ReentrantLock();
  private Condition unPaused = pauseLock.newCondition();

  @Override
  protected void beforeExecute(Thread t, Runnable r) {

    super.beforeExecute(t, r);

    pauseLock.lock();

    try {
      while (isPaused) {
        unPaused.await();
      }
    } catch (InterruptedException ie) {
      t.interrupt();
    } finally {
      pauseLock.unlock();
    }
  }

  /**
   * Pause the executor.
   */
  public void pause() {

    pauseLock.lock();

    try {
      isPaused = true;
    } finally {
      pauseLock.unlock();
    }
  }

  /**
   * Resume the executor.
   */
  public void resume() {

    pauseLock.lock();

    try {
      isPaused = false;
      unPaused.signalAll();
    } finally {
      pauseLock.unlock();
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
        new LinkedBlockingQueue<Runnable>());
  }

}
