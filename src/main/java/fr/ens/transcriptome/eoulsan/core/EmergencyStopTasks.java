package fr.ens.transcriptome.eoulsan.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * This class define a class where the emergency tasks are stored
 * @author Laurent Jourdren
 * @since 2.0
 */
public class EmergencyStopTasks {

  private static EmergencyStopTasks instance;

  private Set<EmergencyStopTask> tasks = new HashSet<>();

  /**
   * Add an emergency task.
   * @param task the task to add
   */
  public void add(final EmergencyStopTask task) {

    checkNotNull(task, "task argument cannot be null");

    synchronized (this) {
      this.tasks.add(task);
    }
  }

  /**
   * Remove an emergency task.
   * @param task the task to add
   */
  public void remove(final EmergencyStopTask task) {

    checkNotNull(task, "task argument cannot be null");

    synchronized (this) {
      this.tasks.remove(task);
    }
  }

  /**
   * Stop all the tasks.
   */
  public void stop() {

    synchronized (this) {
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
   * @return the singleton instance of the class
   */
  public static EmergencyStopTasks getInstance() {

    if (instance == null) {
      instance = new EmergencyStopTasks();
    }

    return instance;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private EmergencyStopTasks() {
  }
}
