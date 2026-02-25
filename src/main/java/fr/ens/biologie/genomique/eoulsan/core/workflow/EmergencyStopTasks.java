package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class define a class where the emergency tasks are stored
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class EmergencyStopTasks {

  private static EmergencyStopTasks instance;

  private final Set<EmergencyStopTask> tasks = Collections.synchronizedSet(new HashSet<>());
  private volatile boolean stopped;

  /**
   * Add an emergency task.
   *
   * @param task the task to add
   */
  public void add(final EmergencyStopTask task) {

    requireNonNull(task, "task argument cannot be null");

    // If stopped, stop immediately the new task
    if (stopped) {
      task.stop();
    } else {
      this.tasks.add(task);
    }
  }

  /**
   * Remove an emergency task.
   *
   * @param task the task to add
   */
  public void remove(final EmergencyStopTask task) {

    requireNonNull(task, "task argument cannot be null");

    // Only remove task if not stopped
    if (!stopped) {
      this.tasks.remove(task);
    }
  }

  /** Stop all the tasks. */
  public void stop() {

    // Do nothing if already stopped
    if (this.stopped) {
      return;
    }

    // Prevent adding or removing tasks
    this.stopped = true;

    synchronized (this.tasks) {
      for (EmergencyStopTask task : this.tasks) {
        task.stop();
      }
    }
  }

  //
  // Static methods
  //

  /**
   * Get the singleton instance of the class.
   *
   * @return the singleton instance of the class
   */
  public static synchronized EmergencyStopTasks getInstance() {

    if (instance == null) {
      instance = new EmergencyStopTasks();
    }

    return instance;
  }

  //
  // Constructor
  //

  /** Private constructor. */
  private EmergencyStopTasks() {}
}
