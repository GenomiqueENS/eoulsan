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

import static com.google.common.base.Preconditions.checkState;

import fr.ens.biologie.genomique.eoulsan.AbstractEoulsanRuntime.EoulsanExecMode;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Settings;
import fr.ens.biologie.genomique.eoulsan.core.schedulers.clusters.ClusterTaskScheduler;
import fr.ens.biologie.genomique.eoulsan.core.schedulers.clusters.ClusterTaskSchedulerService;

/**
 * This class define a factory for TaskScheduler that can create only one
 * instance. This class avoid the serialization of the task scheduler classes
 * when serialize TaskContext object.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TaskSchedulerFactory {

  private static TaskScheduler scheduler;

  //
  // Static method
  //

  /**
   * Initialize the scheduler.
   * @throws EoulsanException if an error occurs while configuring the scheduler
   */
  public static void initialize() throws EoulsanException {

    checkState(scheduler == null,
        "The TaskSchedulerFactory has been already initialized");

    final Settings settings = EoulsanRuntime.getSettings();

    // Get the thread number to use by the task scheduler
    final int threadNumber = settings.getLocalThreadsNumber();

    if (EoulsanRuntime.getRuntime().getMode() == EoulsanExecMode.CLUSTER) {

      final String clusterSchedulerName = settings.getClusterSchedulerName();

      // Check if the cluster scheduler setting has been set
      if (clusterSchedulerName == null
          || clusterSchedulerName.trim().isEmpty()) {
        throw new EoulsanException(
            "No cluster scheduler defined. Use the \"main.cluster.scheduler.name\" setting to define it");
      }

      // Get cluster scheduler
      final ClusterTaskScheduler clusterScheduler = ClusterTaskSchedulerService
          .getInstance().newService(clusterSchedulerName);

      // Check if the cluster scheduler exists
      if (clusterScheduler == null) {
        throw new EoulsanException(
            "Unknown cluster scheduler name: " + clusterSchedulerName);
      }

      // Configure cluster scheduler
      clusterScheduler.configure(settings);

      // Set Cluster mode
      scheduler =
          new ClusterCombinedTaskScheduler(threadNumber, clusterScheduler);
    } else {

      // Standard mode
      scheduler = new CombinedTaskScheduler(threadNumber);
    }

  }

  /**
   * Get the scheduler
   * @return the TaskScheduler object
   */
  public static TaskScheduler getScheduler() {

    checkState(scheduler != null,
        "The TaskSchedulerFactory has not been initialized");

    return scheduler;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private TaskSchedulerFactory() {
  }

}
