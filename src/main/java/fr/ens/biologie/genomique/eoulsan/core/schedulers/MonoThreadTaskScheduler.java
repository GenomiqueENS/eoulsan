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

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import com.google.common.collect.Queues;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.workflow.TaskContextImpl;
import fr.ens.biologie.genomique.eoulsan.core.workflow.TaskResultImpl;
import java.util.Queue;

/**
 * This class define a mono thread scheduler.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class MonoThreadTaskScheduler extends AbstractTaskScheduler implements Runnable {

  private static final int SLEEP_TIME_IN_MS = 100;
  private final Queue<TaskContextImpl> queue = Queues.newLinkedBlockingQueue();

  //
  // TaskExecutor methods
  //

  @Override
  public void submit(final Step step, final TaskContextImpl context) {

    // Call to the super method
    super.submit(step, context);

    this.queue.add(context);
  }

  @Override
  public void start() {

    // Call to the super method
    super.start();

    new Thread(this, "TaskScheduler_mono_thread").start();
  }

  //
  // Runnable method
  //

  @Override
  public void run() {

    while (!this.isStopped()) {

      // Do nothing if the queue is empty or the scheduler paused
      if (!this.isPaused() && !this.queue.isEmpty()) {

        // Get context to execute
        final TaskContextImpl context = this.queue.remove();

        // Do nothing if scheduler is stopped
        if (isStopped()) {
          return;
        }

        // Set task in running state
        beforeExecuteTask(context);

        // Do nothing if scheduler is stopped
        if (isStopped()) {
          return;
        }

        // Execute the context
        final TaskResultImpl result = executeTask(context);

        // Do nothing if scheduler is stopped
        if (isStopped()) {
          return;
        }

        // Set task in done state
        afterExecuteTask(context, result);
      }

      // Wait
      try {
        Thread.sleep(SLEEP_TIME_IN_MS);
      } catch (InterruptedException e) {
        getLogger().severe(e.getMessage());
      }
    }
  }
}
