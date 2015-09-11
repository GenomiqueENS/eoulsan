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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.python.google.common.base.Joiner;
import org.python.google.common.base.Splitter;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Settings;

/**
 * This class allow to submit, stop and get the status of jobs using Bpipe
 * scheduler wrappers.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class BpipeTaskScheduler extends AbstractClusterTaskScheduler {

  /**
   * Get the path to the Bpipe command wrapper.
   * @return the File object with the path to the Bpipe command wrapper
   */
  protected abstract File getBpipeCommandWrapper();

  //
  // ClusterTaskScheduler methods
  //

  @Override
  public void configure(final Settings settings) throws EoulsanException {

  }

  @Override
  public String submitJob(final String jobName, final List<String> jobCommand,
      final File jobDirectory, final int taskId) throws IOException {

    checkNotNull(jobName, "jobName argument cannot be null");
    checkNotNull(jobCommand, "jobCommand argument cannot be null");
    checkNotNull(jobDirectory, "jobDirectory argument cannot be null");
    checkArgument(jobDirectory.isDirectory(),
        "The job directory does not exists or is not a directory: "
            + jobDirectory);

    final String jobCommandString = Joiner.on(' ').join(jobCommand);

    try {
      final Process process =
          startJobProcess(jobName, jobCommandString, jobDirectory, taskId);

      // Read output of the submit command
      final BufferedReader reader =
          new BufferedReader(new InputStreamReader(process.getInputStream()));
      final String jobId = reader.readLine();
      reader.close();

      final int exitCode = process.waitFor();

      if (exitCode == 0) {

        getLogger().fine("Job "
            + jobId + " submitted to " + getSchedulerName()
            + " scheduler. Job name: " + jobName + " Job command: "
            + jobCommand);

        // Add the cluster job to the list of job to kill if workflow fails
        ClusterJobEmergencyStopTask.addHadoopJobEmergencyStopTask(this, jobId);

        return jobId;
      } else {

        getLogger().warning("Job submission failed with "
            + getSchedulerName() + " scheduler. Job name: " + jobName
            + " Job command: " + jobCommand);
        throw new IOException("Job submission failed, exit code: " + exitCode);
      }
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void stopJob(final String jobId) throws IOException {

    checkNotNull(jobId, "jobId argument cannot be null");

    try {
      final Process process = stopJobProcess(jobId);
      final int exitCode = process.waitFor();

      if (exitCode == 0) {
        getLogger().fine("Job "
            + jobId + " removed from " + getSchedulerName() + " cluster");
      } else {
        getLogger().warning("Job "
            + jobId + " not removed from " + getSchedulerName() + " cluster");
      }
    } catch (InterruptedException e) {
      getLogger().severe(e.getMessage());
    }

  }

  @Override
  public StatusResult statusJob(final String jobId) throws IOException {

    checkNotNull(jobId, "jobId argument cannot be null");

    try {
      final Process process = statusJobProcess(jobId);

      // Read output of the submit command
      final BufferedReader reader =
          new BufferedReader(new InputStreamReader(process.getInputStream()));
      final String jobStatus = reader.readLine();
      reader.close();

      final int exitCode = process.waitFor();

      if (exitCode == 0) {

        getLogger().fine("Job "
            + jobId + " status on " + getSchedulerName()
            + " scheduler. Job status: " + jobStatus);

        final List<String> fields =
            Lists.newArrayList(Splitter.on(' ').split(jobStatus.trim()));

        switch (fields.get(0)) {

        case "WAITING":
          return new StatusResult(StatusValue.WAITING);

        case "RUNNING":
          return new StatusResult(StatusValue.RUNNING);

        case "COMPLETE":

          // Remove the cluster job to the list of job to kill if workflow fails
          ClusterJobEmergencyStopTask.removeHadoopJobEmergencyStopTask(this,
              jobId);

          if (fields.size() != 2) {
            throw new IOException("Invalid complete string: " + jobStatus);
          }

          try {
            return new StatusResult(StatusValue.COMPLETE,
                Integer.parseInt(fields.get(1)));
          } catch (NumberFormatException e) {
            throw new IOException("Invalid complete string: " + jobStatus, e);
          }

        case "UNKNOWN":
          return new StatusResult(StatusValue.UNKNOWN);

        default:
          throw new IOException("Unknown status: " + jobStatus);
        }

      } else {
        getLogger()
            .warning("Job status command failed. Exit code: " + exitCode);
        throw new IOException("Job status failed, exit code: " + exitCode);
      }
    } catch (InterruptedException e) {
      throw new IOException(e);
    }

  }

  @Override
  public void cleanupJob(final String jobId) throws IOException {

    // Nothing to do
  }

  //
  // Process builders methods
  //

  /**
   * Create process to submit a job.
   * @param jobName job name
   * @param jobCommand job command
   * @param jobDirectory job directory
   * @param taskId task id
   * @return a Process object
   * @throws IOException if an error occurs while creating the process
   */
  private Process startJobProcess(final String jobName, final String jobCommand,
      final File jobDirectory, final int taskId) throws IOException {

    final List<String> command = new ArrayList<>();
    command.add(getBpipeCommandWrapper().getAbsolutePath());
    command.add("start");

    final ProcessBuilder builder = new ProcessBuilder(command);

    builder.environment().put("NAME", jobName);
    builder.environment().put("COMMAND", jobCommand);
    builder.environment().put("JOBDIR", jobDirectory.getAbsolutePath());
    builder.environment().put("EOULSAN_TASK_ID", "" + taskId);

    return builder.start();
  }

  /**
   * Create process to stop a job.
   * @param jobId job id
   * @return a Process object
   * @throws IOException if an error occurs while creating the process
   */
  private Process stopJobProcess(final String jobId) throws IOException {

    final List<String> command = new ArrayList<>();
    command.add(getBpipeCommandWrapper().getAbsolutePath());
    command.add("stop");
    command.add(jobId);

    final ProcessBuilder builder = new ProcessBuilder(command);

    return builder.start();
  }

  /**
   * Create process to get the status of a job.
   * @param jobId job id
   * @return a Process object
   * @throws IOException if an error occurs while creating the process
   */
  private Process statusJobProcess(final String jobId) throws IOException {

    final List<String> command = new ArrayList<>();
    command.add(getBpipeCommandWrapper().getAbsolutePath());
    command.add("status");
    command.add(jobId);

    final ProcessBuilder builder = new ProcessBuilder(command);

    return builder.start();
  }

}
