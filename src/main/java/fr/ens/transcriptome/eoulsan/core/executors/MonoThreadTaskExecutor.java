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

import java.util.Queue;

import com.google.common.collect.Queues;

import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskContext;

/**
 * This class define a mono thread executor.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class MonoThreadTaskExecutor extends AbstractTaskExecutor implements
    Runnable {

  private static final int SLEEP_TIME_IN_MS = 100;
  private Queue<TaskContext> queue = Queues.newLinkedBlockingQueue();

  //
  // TaskExcutor methods
  //

  @Override
  public void submit(final WorkflowStep step, final TaskContext context) {

    super.submit(step, context);

    queue.add(context);
  }

  @Override
  public void start() {

    super.start();
    new Thread(this, "TaskExecutor_monothread").start();
  }

  //
  // Runnable method
  //

  @Override
  public void run() {

    while (!this.isStopped()) {

      // Do nothing if the queue is empty or the executor paused
      if (!this.isPaused() && !this.queue.isEmpty()) {

        // Get context to execute
        final TaskContext context = this.queue.remove();

        // Execute the context
        beforeExecuteTask(context);
        afterExecuteTask(context, executeTask(context));
      }

      // Wait
      try {
        Thread.sleep(SLEEP_TIME_IN_MS);
      } catch (InterruptedException e) {
        EoulsanLogger.getLogger().severe(e.getMessage());
      }
    }
  }

}
