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

  @Override
  protected void beforeExecute(final Thread t, final Runnable r) {

    super.beforeExecute(t, r);

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
