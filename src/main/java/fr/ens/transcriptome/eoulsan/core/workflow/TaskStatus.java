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

package fr.ens.transcriptome.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class define a task status.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TaskStatus implements StepStatus {

  private final TaskContext context;
  private final WorkflowStepStatus status;

  private String message;
  private final Map<String, Long> counters = Maps.newHashMap();
  private String taskDescription;
  private double progress;

  private TaskResult result;

  private Date startDate;
  private Date endDate;
  private final Stopwatch stopwatch = Stopwatch.createUnstarted();

  //
  // Getters
  //

  @Override
  public String getMessage() {

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
  public double getProgress() {

    return this.progress;
  }

  //
  // Setters
  //

  @Override
  public void setMessage(String message) {

    synchronized (this) {
      this.message = message;
    }
  }

  @Override
  public void setDescription(final String description) {

    checkNotNull(description, "the description argument cannot be null");

    synchronized (this) {
      this.taskDescription = description;
    }
  }

  @Override
  public void setCounters(final Reporter reporter, final String counterGroup) {

    checkNotNull(reporter, "Reporter is null");
    checkNotNull(counterGroup, "Counter group is null");

    // Add all counters
    for (String counterName : reporter.getCounterNames(counterGroup)) {
      synchronized (this.counters) {
        this.counters.put(counterName,
            reporter.getCounterValue(counterGroup, counterName));
      }
    }
  }

  @Override
  public void setProgress(final int min, final int max, final int value) {

    checkProgress(min, max, value);

    if (min == max)
      setProgress(1.0);
    else
      setProgress(((double) (value - min)) / (max - min));
  }

  @Override
  public void setProgress(final double progress) {

    // Check result state
    checkResultState();

    // Check progress value
    checkProgress(progress);

    synchronized (this) {

      // Set progress value
      this.progress = progress;

      // If a status for the step exist, inform the step status
      if (this.status != null) {
        this.status.setTaskProgress(this.context.getId(),
            context.getContextName(), progress);
      }
    }
  }

  //
  // Step result creation
  //

  /**
   * Stop the step.
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
      this.endDate = new Date(System.currentTimeMillis());

      // The step is completed
      setProgress(1.0);
    }

    checkState(this.endDate != null, "stopwatch has been never stopped");

    // Compute elapsed time
    return this.stopwatch.elapsed(TimeUnit.MILLISECONDS);
  }

  @Override
  public StepResult createStepResult() {

    return createStepResult(true);
  }

  @Override
  public StepResult createStepResult(final boolean success) {

    // Check result state
    checkResultState();

    // Get the duration of the context execution
    final long duration = endOfStep();

    // Create the context result
    this.result =
        new TaskResult(context, startDate, endDate, duration, this.message,
            this.taskDescription == null ? "" : this.taskDescription,
            this.counters, success);

    return this.result;
  }

  @Override
  public StepResult createStepResult(final Throwable exception,
      final String exceptionMessage) {

    // Check result state
    checkResultState();

    // Get the duration of the context execution
    final long duration = endOfStep();

    // Create the context result
    this.result =
        new TaskResult(context, startDate, endDate, duration, exception,
            exceptionMessage);

    return this.result;
  }

  @Override
  public StepResult createStepResult(final Throwable exception) {

    return createStepResult(exception, exception.getMessage());
  }

  //
  // Utility methods
  //

  /**
   * Check the state of the result creation.
   */
  private void checkResultState() {

    checkState(this.result == null, "Step result has been created");
  }

  /**
   * Check progress value.
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
   * @param min minimal value
   * @param max maximal value
   * @param value value to test
   */
  private static void checkProgress(final int min, final int max,
      final int value) {

    checkArgument(min <= max, "Max is lower than min");
    checkArgument(min <= value, "Value is lower than min");
    checkArgument(value <= max, "Value is greater than max");
  }

  //
  // Other methods
  //

  /**
   * Start the timer.
   */
  void durationStart() {

    // Get the start date
    this.startDate = new Date(System.currentTimeMillis());

    // Start stopWatch
    this.stopwatch.start();
  }

  //
  // Constructors
  //

  /**
   * Constructor.
   * @param taskContext the task context object
   * @param status the status object
   */
  TaskStatus(final TaskContext taskContext, final WorkflowStepStatus status) {

    Preconditions.checkNotNull(taskContext, "context cannot be null");

    this.context = taskContext;
    this.status = status;
  }

}
