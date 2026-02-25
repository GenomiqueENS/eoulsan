package fr.ens.biologie.genomique.eoulsan.core.workflow;

/**
 * This interface define a task that will executed if the execution of the workflow fail.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface EmergencyStopTask {

  /** Execute the stop task. */
  void stop();
}
