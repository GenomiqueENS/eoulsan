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

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;

/**
 * This class define a factory for TaskExecutor that can create only one
 * instance. This class avoid the serialization of the task executor classes
 * when serialize TaskContext object.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TaskExecutorFactory {

  private static TaskExecutor executor;

  //
  // Static method
  //

  /**
   * Get the executor
   * @return the TaskExecutor object
   */
  public static TaskExecutor getExecutor() {

    if (executor == null) {

      // Get the thread number to use by the task executor
      final int threadNumber =
          EoulsanRuntime.getSettings().getLocalThreadsNumber();

      if (EoulsanRuntime.getRuntime().isClusterMode()) {

        // Cluster mode
        executor = new ClusterCombinedTaskExecutor(threadNumber);
      } else {

        // Standard mode
        executor = new CombinedTaskExecutor(threadNumber);
      }
    }

    return executor;
  }

  // Local
  // Constructor
  //

  /**
   * Private constructor.
   */
  private TaskExecutorFactory() {
  }

}
