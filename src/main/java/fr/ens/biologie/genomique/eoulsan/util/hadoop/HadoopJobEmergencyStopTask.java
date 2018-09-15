package fr.ens.biologie.genomique.eoulsan.util.hadoop;

import static fr.ens.biologie.genomique.eoulsan.CommonHadoop.createConfiguration;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static java.util.Objects.requireNonNull;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobID;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapreduce.Job;

import fr.ens.biologie.genomique.eoulsan.core.workflow.EmergencyStopTask;
import fr.ens.biologie.genomique.eoulsan.core.workflow.EmergencyStopTasks;

/**
 * This class define an EmergencyStopTask for Hadoop Jobs.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class HadoopJobEmergencyStopTask implements EmergencyStopTask {

  private final String jobId;

  @Override
  public void stop() {

    getLogger().info("Try to kill " + this.jobId + " Hadoop job");

    // Create configuration object
    final Configuration conf = createConfiguration();

    try {

      final JobClient client = new JobClient(conf);

      if (client != null) {

        final RunningJob job = client.getJob(JobID.forName(this.jobId));

        if (job != null) {
          job.killJob();
        }
      }
    } catch (IOException e) {
      getLogger().severe(e.getMessage());
    }

    getLogger().info("Hadoop job " + this.jobId + " killed");
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

    if (obj == this) {
      return true;
    }

    if (!(obj instanceof HadoopJobEmergencyStopTask)) {
      return false;
    }

    final HadoopJobEmergencyStopTask that = (HadoopJobEmergencyStopTask) obj;

    return this.jobId.equals(that.jobId);
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

    requireNonNull(job, "job argument cannot be null");

    this.jobId = job.getStatus().getJobID().toString();
  }

}
