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

package fr.ens.transcriptome.eoulsan.core.schedulers.clusters;

import java.io.IOException;
import java.util.List;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.core.schedulers.TaskScheduler;

/**
 * This interface define a cluster task scheduler.
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface ClusterTaskScheduler extends TaskScheduler {

  /**
   * This enum define the values of the status of a job.
   * @author Laurent Jourdren
   */
  public enum StatusValue {
    WAITING, RUNNING, COMPLETE, UNKNOWN
  }

  /**
   * This class define a Status result return by the statusJob() method of the
   * interface.
   * @author Laurent Jourdren
   */
  public static final class StatusResult {

    private final StatusValue statusValue;
    private final int exitCode;

    //
    // Getters
    //

    /**
     * Get status value.
     * @return the status value
     */
    public StatusValue getStatusValue() {

      return this.statusValue;
    }

    /**
     * Return the exit code.
     * @return the exit code
     */
    public int getExitCode() {

      return exitCode;
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param statusValue status value
     */
    public StatusResult(final StatusValue statusValue) {

      this(statusValue, 0);
    }

    /**
     * Constructor.
     * @param statusValue status value
     * @param exitCode exit code
     */
    public StatusResult(final StatusValue statusValue, final int exitCode) {

      this.statusValue = statusValue;
      this.exitCode = exitCode;
    }

  }

  //
  // Interface method
  //

  /**
   * Get the scheduler name.
   * @return the name of the scheduler
   */
  String getSchedulerName();

  /**
   * Configure the scheduler.
   * @param settings Eoulsan settings
   * @throws EoulsanException if an error occurs while configuring the scheduler
   */
  void configure(Settings settings) throws EoulsanException;

  /**
   * Submit a job.
   * @param jobName job name
   * @param jobCommand job command
   * @return a String with the id of the submitted job
   * @throws IOException if an error occurs while submitting job
   */
  String submitJob(final String jobName, final List<String> jobCommand)
      throws IOException;

  /**
   * Stop a job.
   * @param jobId job id
   * @throws IOException if an error occurs while stopping the job
   */
  void stopJob(final String jobId) throws IOException;

  /**
   * Get the status of a job.
   * @param jobId job id
   * @throws IOException if an error occurs while getting the status of the job
   */
  StatusResult statusJob(final String jobId) throws IOException;

  /**
   * Cleanup after a job.
   * @param jobId job id
   * @throws IOException if an error occurs while cleanup
   */
  void cleanupJob(final String jobId) throws IOException;

}
