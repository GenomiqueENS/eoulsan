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
 * This class define a step status.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class WorkflowStepContextStatus implements StepStatus {

  private final WorkflowStepContext context;
  private final WorkflowStepStatus status;

  private String message;
  private Map<String, Long> counters = Maps.newHashMap();
  private String contextDescription;
  private double progress;

  private WorkflowStepContextResult result;

  private Date startDate;
  private Date endDate;
  private Stopwatch stopwatch = new Stopwatch();

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

    return this.contextDescription;
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

    this.message = message;
  }

  @Override
  public void setDescription(final String description) {

    checkNotNull(description, "the description argument cannot be null");

    this.contextDescription = description;
  }

  @Override
  public void setCounters(final Reporter reporter, final String counterGroup) {

    checkNotNull(reporter, "Reporter is null");
    checkNotNull(counterGroup, "Counter group is null");

    // Add all counters
    for (String counterName : reporter.getCounterNames(counterGroup))
      this.counters.put(counterName,
          reporter.getCounterValue(counterGroup, counterName));
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

    // Check context status
    checkState();

    // Check progress value
    checkProgress(progress);

    // Set progress value
    this.progress = progress;

    // If a status for the step exist, inform the step status
    if (this.status != null) {
      this.status.setContextProgress(this.context.getContextName(), progress);
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

    // Stop the stopwatch
    this.stopwatch.stop();

    // Get the end Date
    this.endDate = new Date(System.currentTimeMillis());

    // The step is completed
    setProgress(1.0);

    // Compute elapsed time
    return this.stopwatch.elapsedTime(TimeUnit.MILLISECONDS);
  }

  @Override
  public StepResult createStepResult() {

    return createStepResult(true);
  }

  @Override
  public StepResult createStepResult(final boolean success) {

    // Check context status
    checkState();

    // Get the duration of the context execution
    final long duration = endOfStep();

    // Create the context result
    this.result =
        new WorkflowStepContextResult(context, startDate, endDate, duration,
            this.message, this.contextDescription == null
                ? "" : this.contextDescription, this.counters, success);

    return this.result;
  }

  @Override
  public StepResult createStepResult(final Throwable exception,
      final String exceptionMessage) {

    // Check context status
    checkState();

    // Get the duration of the context execution
    final long duration = endOfStep();

    // Create the context result
    this.result =
        new WorkflowStepContextResult(context, startDate, endDate, duration,
            exception, exceptionMessage);

    return this.result;
  }

  @Override
  public StepResult createStepResult(final Throwable exception) {

    return createStepResult(exception, exception.getMessage());
  }

  //
  // Utility methods
  //

  private void checkState() {

    Preconditions.checkState(this.result == null,
        "Step result has been created");
  }

  private static void checkProgress(final double progress) {

    checkArgument(progress >= 0.0, "Progress is lower than 0: " + progress);
    checkArgument(progress <= 1.0, "Progress is greater than 1: " + progress);
    checkArgument(!Double.isInfinite(progress), "Progress is infinite");
    checkArgument(!Double.isNaN(progress), "Progress is NaN");
  }

  private static void checkProgress(final int min, final int max,
      final int value) {

    checkArgument(min <= max, "Max is lower than min");
    checkArgument(min <= value, "Value is lower than min");
    checkArgument(value <= max, "Value is greater than max");
  }

  //
  // Other methods
  //

  void durationStart() {

    // Get the start date
    this.startDate = new Date(System.currentTimeMillis());

    // Start stopWatch
    this.stopwatch.start();
  }

  //
  // Constructors
  //

  public WorkflowStepContextStatus(final WorkflowStepContext context,
      final WorkflowStepStatus status) {

    Preconditions.checkNotNull(context, "context cannot be null");

    this.context = context;
    this.status = status;

    this.result = null; // new WorkflowStepResult(step.getContext(), step);
  }

}
