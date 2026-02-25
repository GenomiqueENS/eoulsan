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

package fr.ens.biologie.genomique.eoulsan.util.hadoop;

import static fr.ens.biologie.genomique.kenetre.util.Utils.silentSleep;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import java.io.IOException;
import org.apache.hadoop.mapreduce.Job;

/**
 * This class contains utility method to easily manipulate the new Hadoop MapReduce API.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class MapReduceUtils {

  private static final int COMPLETION_POLL_INTERVAL = 5000;
  private static final int MAX_CONNECTION_TRY_TO_JOB_TRACK = 12;

  /**
   * Wait the completion of a job.
   *
   * @param job the job to submit
   * @param jobDescription the description of the job
   * @param waitTimeInMillis waiting time between 2 checks of the completion of jobs
   * @param status step status
   * @param counterGroup group of the counter to log
   * @throws EoulsanException if the job fail or if an exception occurs while submitting or waiting
   *     the end of the job
   */
  public static void submitAndWaitForJob(
      final Job job,
      final String jobDescription,
      final int waitTimeInMillis,
      final TaskStatus status,
      final String counterGroup)
      throws EoulsanException {

    if (job == null) {
      throw new NullPointerException("The job is null");
    }

    if (jobDescription == null) {
      throw new NullPointerException("The jobDescription is null");
    }

    try {

      // Set the description of the context
      status.setDescription(job.getJobName());

      // Submit the job
      job.submit();

      // Add the Hadoop job to the list of job to kill if workflow fails
      HadoopJobEmergencyStopTask.addHadoopJobEmergencyStopTask(job);

      // Job the completion of the job (non verbose mode)
      waitForCompletion(job, MAX_CONNECTION_TRY_TO_JOB_TRACK);

      // Remove the Hadoop job to the list of job to kill if workflow fails
      HadoopJobEmergencyStopTask.removeHadoopJobEmergencyStopTask(job);

      // Check if the job has been successfully executed
      if (!job.isSuccessful()) {

        status.setProgressMessage("FAILED");

        throw new EoulsanException("Fail of the Hadoop job: " + job.getJobFile());
      }

      // Set the counters
      status.setCounters(new HadoopReporter(job.getCounters()), counterGroup);

    } catch (ClassNotFoundException | InterruptedException | IOException e) {
      throw new EoulsanException(e);
    }
  }

  /**
   * Wait for job completion.
   *
   * @param job job to submit
   * @param maxTry number of try to connect to JobTracker before throwing an exception
   * @throws IOException thrown if the communication with the JobTracker is lost
   */
  public static void waitForCompletion(final Job job, final int maxTry) throws IOException {

    if (job == null) {
      throw new NullPointerException("The job is null");
    }

    waitForCompletion(job, maxTry, 0);
  }

  /**
   * Wait for job completion.
   *
   * @param job job to submit
   * @param maxTry number of try to connect to JobTracker before throwing an exception
   * @throws IOException thrown if the communication with the JobTracker is lost
   */
  private static void waitForCompletion(final Job job, final int maxTry, int failedTry)
      throws IOException {

    try {
      while (!job.isComplete()) {
        failedTry = 0;
        silentSleep(COMPLETION_POLL_INTERVAL);
      }
    } catch (IOException e) {

      failedTry += 1;

      EoulsanLogger.getLogger()
          .severe(
              "Fail to check if Hadoop Job ("
                  + job.getJobName()
                  + ") is completed, "
                  + failedTry
                  + "/"
                  + maxTry
                  + " trys: "
                  + e.getMessage());

      if (failedTry >= maxTry) {
        throw new IOException(e);
      }

      waitForCompletion(job, maxTry, failedTry);
    }
  }

  //
  // Constructor
  //

  private MapReduceUtils() {}
}
