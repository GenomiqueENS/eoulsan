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

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class define a step status.
 * @author Laurent Jourdren
 * @since 1.3
 */
public class WorkflowStepStatus implements StepStatus {

  private final AbstractWorkflowStep step;
  private final Design design;

  private final WorkflowStepResult result;

  private final Map<Sample, Double> sampleProgress = Maps.newHashMap();
  private double progress = Double.NaN;
  private String note;

  private Date startDate;
  private Date endDate;
  private Stopwatch stopwatch = new Stopwatch();

  private Set<WorkflowStepObserver> observers = WorkflowStepObserverRegistry
      .getInstance().getObservers();

  //
  // Counters
  //

  @Override
  public String getStepMessage() {

    return this.result.getStepMessage();
  }

  @Override
  public Map<String, Long> getStepCounters() {

    return this.result.getStepCounters();
  }

  @Override
  public String getSampleMessage(final Sample sample) {

    return this.result.getSampleMessage(sample);
  }

  @Override
  public Map<String, Long> getSampleCounters(final Sample sample) {

    return this.result.getSampleCounters(sample);
  }

  @Override
  public void setStepMessage(final String message) {

    this.result.setStepMessage(message);

    // The step has been fully processed
    setProgress(1.0);
  }

  @Override
  public void setStepCounters(final Reporter reporter,
      final String counterGroup, final String sampleCounterHeader) {

    this.result.setStepCounters(reporter, counterGroup, sampleCounterHeader);

    // The step has been fully processed
    setProgress(1.0);
  }

  @Override
  public void setSampleMessage(final Sample sample, final String message) {

    this.result.setSampleMessage(sample, message);

    // The sample has been fully processed
    setSampleProgress(sample, 1.0);
  }

  @Override
  public void setSampleCounters(final Sample sample, final Reporter reporter,
      final String counterGroup, final String sampleCounterHeader) {

    this.result.setSampleCounters(sample, reporter, counterGroup,
        sampleCounterHeader);

    // The sample has been fully processed
    setSampleProgress(sample, 1.0);
  }

  //
  // Progress Methods
  //

  @Override
  public String getNote() {

    return this.note;
  }

  @Override
  public double getProgress() {

    if (Double.isNaN(this.progress)) {

      double sum = 0.0;
      for (Double p : this.sampleProgress.values())
        sum += p;

      return sum / this.design.getSampleCount();
    }

    return this.progress;
  }

  @Override
  public double getSampleProgress(final Sample sample) {

    checkSample(sample);

    return this.sampleProgress.get(sample);
  }

  @Override
  public void setNote(final String note) {

    this.note = note;

    // Inform listener that status has changed
    noteStatusUpdated();
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

    checkState();
    checkProgress(progress);

    synchronized (this) {
      this.progress = progress;
    }

    // Inform listener that status has changed
    progressStatusUpdated();
  }

  @Override
  public void setSampleProgress(final Sample sample, final int min,
      final int max, final int value) {

    checkProgress(min, max, value);

    if (min == max)
      setSampleProgress(sample, 1.0);
    else
      setSampleProgress(sample, ((double) (value - min)) / (max - min));

  }

  @Override
  public void setSampleProgress(final Sample sample, final double progress) {

    checkState();
    checkSample(sample);
    checkProgress(progress);

    // Inform observers that status has changed
    progressSampleStatusUpdated(sample, progress);

    // Save progress sample for step progress computation
    synchronized (this) {
      this.sampleProgress.put(sample, progress);
    }

    // Inform observers that status has changed
    progressStatusUpdated();
  }

  //
  // Step result creation
  //

  private void endOfStep() {

    // Stop the stopwatch
    this.stopwatch.stop();

    // Get the end Date
    this.endDate = new Date(System.currentTimeMillis());

    // Set the end and the start date in the result object
    this.result.setStartTime(this.startDate);
    this.result.setEndTime(this.endDate);

    // Set the duration
    this.result.setDuration(this.stopwatch.elapsedTime(TimeUnit.MILLISECONDS));

    // The step is completed
    setProgress(1.0);

    // The object cannot modified now
    this.result.setImmutable();
  }

  //
  // Step result creation
  //

  @Override
  public WorkflowStepResult createStepResult() {

    return createStepResult(true);
  }

  @Override
  public WorkflowStepResult createStepResult(final boolean success) {

    this.result.setSuccess(success);
    endOfStep();

    return this.result;
  }

  @Override
  public WorkflowStepResult createStepResult(final Exception exception,
      final String exceptionMessage) {

    this.result.setException(exception, exceptionMessage);
    endOfStep();

    return this.result;
  }

  @Override
  public WorkflowStepResult createStepResult(final Exception exception) {

    return createStepResult(exception, exception.getMessage());
  }

  //
  // Observers
  //

  /**
   * Inform observers that the status has been changed.
   */
  private void progressSampleStatusUpdated(final Sample sample, final double progress) {

    // Inform listeners
    for (WorkflowStepObserver o : this.observers)
      o.notifyStepState(this.step, sample, progress);
  }

  /**
   * Inform observers that the status has been changed.
   */
  private void progressStatusUpdated() {

    // Inform listeners
    for (WorkflowStepObserver o : this.observers)
      o.notifyStepState(this.step, getProgress());
  }

  /**
   * Inform observers that the status has been changed.
   */
  private void noteStatusUpdated() {

    // Inform listeners
    for (WorkflowStepObserver o : this.observers)
      o.notifyStepState(this.step, this.note);
  }

  //
  // Check progress
  //

  private void checkState() {

    Preconditions.checkState(!this.result.isImmutable(),
        "Step result has been created");
  }

  private void checkSample(final Sample sample) {

    checkNotNull(sample, "Sample is null");
    checkArgument(this.design.contains(sample),
        "The design does not contains the sample");
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
  // Constructor
  //

  public WorkflowStepStatus(final AbstractWorkflowStep step) {

    Preconditions.checkNotNull(step, "Step is null");

    this.step = step;
    this.design = step.getWorkflow().getDesign();

    this.result = new WorkflowStepResult(step.getContext(), step);

    // Get the start date
    this.startDate = new Date(System.currentTimeMillis());

    // Start stopWatch
    this.stopwatch.start();
  }

}
