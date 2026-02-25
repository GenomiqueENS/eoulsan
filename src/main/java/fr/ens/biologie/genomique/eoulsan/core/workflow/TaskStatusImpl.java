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

package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.kenetre.util.Reporter;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This class define a task status.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TaskStatusImpl implements TaskStatus {

  private final TaskContextImpl context;
  private final StepStatus status;

  private String message;
  private final Map<String, Long> counters = new HashMap<>();
  private String taskDescription;
  private String taskCommandLine;
  private String taskDockerImage;
  private double progress;

  private volatile boolean done;

  private Instant startDate;
  private Instant endDate;
  private final SerializableStopwatch stopwatch = new SerializableStopwatch();

  //
  // Getters
  //

  @Override
  public String getProgressMessage() {

    return this.message;
  }

  @Override
  public Map<String, Long> getCounters() {

    return Collections.unmodifiableMap(this.counters);
  }

  @Override
  public String getDescription() {

    return this.taskDescription;
  }

  @Override
  public String getCommandLine() {

    return this.taskCommandLine;
  }

  @Override
  public String getDockerImage() {

    return this.taskDockerImage;
  }

  @Override
  public double getProgress() {

    return this.progress;
  }

  //
  // Setters
  //

  @Override
  public void setProgressMessage(final String message) {

    synchronized (this) {
      this.message = message;
    }
  }

  @Override
  public void setDescription(final String description) {

    requireNonNull(description, "the description argument cannot be null");

    synchronized (this) {
      this.taskDescription = description;
    }
  }

  @Override
  public void setCommandLine(final String commandLine) {

    requireNonNull(commandLine, "the commandLine argument cannot be null");

    synchronized (this) {
      this.taskCommandLine = commandLine;
    }
  }

  @Override
  public void setDockerImage(final String dockerImage) {

    requireNonNull(dockerImage, "the dockerImage argument cannot be null");

    synchronized (this) {
      this.taskDockerImage = dockerImage;
    }
  }

  @Override
  public void setCounters(final Reporter reporter, final String counterGroup) {

    requireNonNull(reporter, "Reporter is null");
    requireNonNull(counterGroup, "Counter group is null");

    // Add all counters
    for (String counterName : reporter.getCounterNames(counterGroup)) {
      synchronized (this.counters) {
        this.counters.put(counterName, reporter.getCounterValue(counterGroup, counterName));
      }
    }
  }

  @Override
  public void setProgress(final int min, final int max, final int value) {

    checkProgress(min, max, value);

    if (min == max) {
      setProgress(1.0);
    } else {
      setProgress(((double) (value - min)) / (max - min));
    }
  }

  @Override
  public void setProgress(final double progress) {

    // Do nothing if the task is done
    if (this.done) {
      return;
    }

    // Check progress value
    checkProgress(progress);

    synchronized (this) {

      // Set progress value
      this.progress = progress;

      // If a status for the step exist, inform the step status
      if (this.status != null) {
        this.status.setTaskProgress(this.context.getId(), this.context.getContextName(), progress);
      }
    }
  }

  //
  // Step result creation
  //

  /**
   * Stop the step.
   *
   * @return the duration of the step in milliseconds
   */
  private long endOfStep() {

    checkState(this.startDate != null, "stopwatch has been never started");

    // If an exception is thrown while creating StepResult object or after, this
    // method
    // can be called two times
    if (this.stopwatch.isRunning()) {

      // Stop the stopwatch
      this.stopwatch.stop();

      // Get the end Date
      this.endDate = Instant.now();

      // The step is completed
      setProgress(1.0);
    }

    checkState(this.endDate != null, "stopwatch has been never stopped");

    // Compute elapsed time
    return this.stopwatch.elapsed(TimeUnit.MILLISECONDS);
  }

  @Override
  public TaskResult createTaskResult() {

    return createTaskResult(true);
  }

  @Override
  public TaskResult createTaskResult(final boolean success) {

    // Get the duration of the context execution
    final long duration = endOfStep();
    this.done = true;

    // Create the context result
    return new TaskResultImpl(
        this.context,
        this.startDate,
        this.endDate,
        duration,
        this.message,
        this.taskDescription == null ? "" : this.taskDescription,
        this.taskCommandLine == null ? "" : this.taskCommandLine,
        this.taskDockerImage == null ? "" : this.taskDockerImage,
        this.counters,
        success);
  }

  @Override
  public TaskResult createTaskResult(final Throwable exception, final String exceptionMessage) {

    // Get the duration of the context execution
    final long duration = endOfStep();
    this.done = true;

    // Create the context result
    return new TaskResultImpl(
        this.context, this.startDate, this.endDate, duration, exception, exceptionMessage);
  }

  @Override
  public TaskResult createTaskResult(final Throwable exception) {

    return createTaskResult(exception, exception.getMessage());
  }

  //
  // Utility methods
  //

  /**
   * Check progress value.
   *
   * @param progress the progress value to test
   */
  private static void checkProgress(final double progress) {

    checkArgument(progress >= 0.0, "Progress is lower than 0: " + progress);
    checkArgument(progress <= 1.0, "Progress is greater than 1: " + progress);
    checkArgument(!Double.isInfinite(progress), "Progress is infinite");
    checkArgument(!Double.isNaN(progress), "Progress is NaN");
  }

  /**
   * Check progress value.
   *
   * @param min minimal value
   * @param max maximal value
   * @param value value to test
   */
  private static void checkProgress(final int min, final int max, final int value) {

    checkArgument(min <= max, "Max is lower than min");
    checkArgument(min <= value, "Value is lower than min");
    checkArgument(value <= max, "Value is greater than max");
  }

  //
  // Other methods
  //

  /** Start the timer. */
  void durationStart() {

    // Get the start date
    this.startDate = Instant.now();

    // Start stopWatch
    this.stopwatch.start();
  }

  //
  // Constructors
  //

  /**
   * Constructor.
   *
   * @param taskContext the task context object
   * @param status the status object
   */
  TaskStatusImpl(final TaskContextImpl taskContext, final StepStatus status) {

    requireNonNull(taskContext, "context cannot be null");

    this.context = taskContext;
    this.status = status;
  }
}
