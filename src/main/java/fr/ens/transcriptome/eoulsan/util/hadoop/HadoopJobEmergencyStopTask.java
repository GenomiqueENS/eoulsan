package fr.ens.transcriptome.eoulsan.util.hadoop;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;

import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobID;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapreduce.Job;

import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.core.EmergencyStopTask;
import fr.ens.transcriptome.eoulsan.core.EmergencyStopTasks;

/**
 * This class define an EmergencyStopTask for Hadoop Jobs.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class HadoopJobEmergencyStopTask implements EmergencyStopTask {

  private final String jobId;

  @Override
  public void stop() {

    final JobClient client = new JobClient();

    if (client != null) {

      try {
        final RunningJob job = client.getJob(JobID.forName(this.jobId));

        if (job != null) {
          job.killJob();
        }

      } catch (IOException e) {
        EoulsanLogger.getLogger().severe(e.getMessage());
      }
    }
  }

  //
  // Object tasks
  //

  @Override
  public int hashCode() {

    return this.jobId.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {

    return this.jobId.equals(obj);
  }

  //
  // Static method
  //

  /**
   * Add an Hadoop Job to the EmergencyStopTasks.
   * @param job the Hadoop job
   * @throws IOException if an error occurs while getting the job id
   * @throws InterruptedException if an error occurs while getting the job id
   */
  public static void addHadoopJobEmergencyStopTask(final Job job)
      throws IOException, InterruptedException {

    EmergencyStopTasks.getInstance().add(new HadoopJobEmergencyStopTask(job));
  }

  /**
   * Remove an Hadoop Job to the EmergencyStopTasks.
   * @param job the Hadoop job
   * @throws IOException if an error occurs while getting the job id
   * @throws InterruptedException if an error occurs while getting the job id
   */
  public static void removeHadoopJobEmergencyStopTask(final Job job)
      throws IOException, InterruptedException {

    EmergencyStopTasks.getInstance()
        .remove(new HadoopJobEmergencyStopTask(job));
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param job the Hadoop job
   * @throws IOException if an error occurs while getting the job id
   * @throws InterruptedException if an error occurs while getting the job id
   */
  public HadoopJobEmergencyStopTask(final Job job)
      throws IOException, InterruptedException {

    checkNotNull(job, "job argument cannot be null");

    this.jobId = job.getStatus().getJobID().toString();
  }

}
