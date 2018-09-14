package fr.ens.biologie.genomique.eoulsan.core.schedulers.clusters;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static java.util.Objects.requireNonNull;

import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.core.workflow.EmergencyStopTask;
import fr.ens.biologie.genomique.eoulsan.core.workflow.EmergencyStopTasks;

/**
 * This class define an EmergencyStopTask for cluster Jobs.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ClusterJobEmergencyStopTask implements EmergencyStopTask {

  private final ClusterTaskScheduler scheduler;
  private final String jobId;

  @Override
  public void stop() {

    getLogger().info("Try to kill "
        + this.jobId + " " + this.scheduler.getSchedulerName() + " job");

    try {
      this.scheduler.statusJob(this.jobId);
    } catch (IOException e) {
      EoulsanLogger.getLogger().severe(e.getMessage());
    }

    getLogger().info(
        this.scheduler.getSchedulerName() + " job " + this.jobId + " killed");
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

    if (!(obj instanceof ClusterJobEmergencyStopTask)) {
      return false;
    }

    final ClusterJobEmergencyStopTask that = (ClusterJobEmergencyStopTask) obj;

    return this.jobId.equals(that.jobId);
  }

  //
  // Static method
  //

  /**
   * Add a cluster Job to the EmergencyStopTasks.
   * @param scheduler the cluster scheduler
   * @param jobId the job id
   */
  public static void addHadoopJobEmergencyStopTask(
      final ClusterTaskScheduler scheduler, final String jobId) {

    EmergencyStopTasks.getInstance()
        .add(new ClusterJobEmergencyStopTask(scheduler, jobId));
  }

  /**
   * Remove a cluster Job to the EmergencyStopTasks.
   * @param scheduler the cluster scheduler
   * @param jobId the job id
   */
  public static void removeHadoopJobEmergencyStopTask(
      final ClusterTaskScheduler scheduler, final String jobId) {

    EmergencyStopTasks.getInstance()
        .remove(new ClusterJobEmergencyStopTask(scheduler, jobId));
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param scheduler the cluster scheduler
   * @param jobId the job id
   */
  public ClusterJobEmergencyStopTask(final ClusterTaskScheduler scheduler,
      final String jobId) {

    requireNonNull(scheduler, "scheduler argument cannot be null");
    requireNonNull(jobId, "jobId argument cannot be null");

    this.scheduler = scheduler;
    this.jobId = jobId;
  }

}
