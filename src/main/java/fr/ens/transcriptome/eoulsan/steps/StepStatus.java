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

package fr.ens.transcriptome.eoulsan.steps;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.core.workflow.AbstractWorkflowStep;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class define a step status.
 * @author Laurent Jourdren
 * @since 1.3
 */
public class StepStatus {

  private final AbstractWorkflowStep step;
  private final Design design;

  private final SimpleStepResult result;

  private final Map<Sample, Double> sampleProgress = Maps.newHashMap();
  private double progress = Double.NaN;
  private String note;

  private Date startDate;
  private Date endDate;
  private Stopwatch stopwatch = new Stopwatch();

  //
  // Counters
  //

  /**
   * Get step message.
   * @return the step message in a String
   */
  public String getStepMessage() {

    return this.result.getStepMessage();
  }

  /**
   * Get the step counters.
   * @return the step counters in a map
   */
  public Map<String, Long> getStepCounters() {

    return this.result.getStepCounters();
  }

  /**
   * Get sample message.
   * @param sample sample
   * @return the message for the sample
   */
  public String getSampleMessage(final Sample sample) {

    return this.result.getSampleMessage(sample);
  }

  /**
   * Get the sample counters.
   * @param sample sample
   * @return the sample counters as a map
   */
  public Map<String, Long> getSampleCounters(final Sample sample) {

    return this.getSampleCounters(sample);
  }

  /**
   * Set the step message.
   * @param message message to set
   */
  public void setStepMessage(final String message) {

    this.result.setStepMessage(message);

    // The step has been fully processed
    setProgress(1.0);
  }

  /**
   * Set the step counters.
   * @param reporter the reporter
   * @param counterGroup counter group to use with the reporter
   * @param sampleCounterHeader header for the sample (optional)
   */
  public void setStepCounters(final Reporter reporter,
      final String counterGroup, final String sampleCounterHeader) {

    this.result.setStepCounters(reporter, counterGroup, sampleCounterHeader);

    // The step has been fully processed
    setProgress(1.0);
  }

  /**
   * Set the sample message.
   * @param sample the sample
   * @param message the message to set
   */
  public void setSampleMessage(final Sample sample, final String message) {

    this.result.setSampleMessage(sample, message);

    // The sample has been fully processed
    setSampleProgress(sample, 1.0);
  }

  /**
   * Set the sample counters
   * @param sample the sample
   * @param reporter the reporter
   * @param counterGroup counter group to use with the reporter
   * @param sampleCounterHeader header for the sample (optional)
   */
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

  /**
   * Get a note about the step progress
   * @return a String. The result can be null is not note is set
   */
  public String getNote() {

    return this.note;
  }

  /**
   * Get progress of the step.
   * @return the progress of the step as percent (between 0.0 and 1.0)
   */
  public double getProgress() {

    if (Double.isNaN(this.progress)) {

      double sum = 0.0;
      for (Double p : this.sampleProgress.values())
        sum += p;

      return sum / this.design.getSampleCount();
    }

    return this.progress;
  }

  /**
   * Get the progress of a sample processing.
   * @param sample sample to process
   * @return the progress of the processing of the sample as percent (between
   *         0.0 and 1.0)
   */
  public double getSampleProgress(final Sample sample) {

    checkSample(sample);

    return this.sampleProgress.get(sample);
  }

  /**
   * Set a note in step progress
   * @param note a note. Can be null to remove a previous note
   */
  public void setNote(final String note) {

    this.note = note;
  }

  /**
   * Set the progress of the step.
   * @param min minimal value of the progress
   * @param max maximal value of the progress
   * @param value current value of the progress
   */
  public void setProgress(final int min, final int max, final int value) {

    checkProgress(min, max, value);

    if (min == max)
      setProgress(1.0);
    else
      setProgress(((double) (value - min)) / (max - min));
  }

  /**
   * Set the value of the progress.
   * @param progress value of the progress. This value must be greater or equals
   *          to 0 and lower or equals to 1.0
   */
  public void setProgress(final double progress) {

    checkState();
    checkProgress(progress);

    synchronized (this) {
      this.progress = progress;
    }

    // Inform listener that status has changed
    statusUpdated();
  }

  /**
   * Set the progress of the processing of a sample by the step.
   * @param sample the sample to process
   * @param min minimal value of the progress
   * @param max maximal value of the progress
   * @param value current value of the progress
   */
  public void setSampleProgress(final Sample sample, final int min,
      final int max, final int value) {

    checkProgress(min, max, value);

    if (min == max)
      setSampleProgress(sample, 1.0);
    else
      setSampleProgress(sample, ((double) (value - min)) / (max - min));

  }

  /**
   * Set the progress of the processing of a sample by the step.
   * @param sample the sample to process
   * @param progress value of the progress. This value must be greater or equals
   *          to 0 and lower or equals to 1.0
   */
  public void setSampleProgress(final Sample sample, final double progress) {

    checkState();
    checkSample(sample);
    checkProgress(progress);

    synchronized (this) {
      this.sampleProgress.put(sample, progress);
    }

    // Inform listener that status has changed
    statusUpdated();
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

  /**
   * Public constructor for a successful result.
   */
  public SimpleStepResult createStepResult() {

    return createStepResult(true);
  }

  /**
   * Public constructor.
   * @param success true if the step is successful
   * @param logMsg log message
   */
  public SimpleStepResult createStepResult(final boolean success) {

    this.result.setSuccess(success);
    endOfStep();

    return this.result;
  }

  /**
   * Public constructor.
   * @param exception exception of the error
   * @param errorMsg Error message
   * @param logMsg log message
   */
  public SimpleStepResult createStepResult(final Exception exception,
      final String exceptionMessage) {

    this.result.setException(exception, exceptionMessage);
    endOfStep();

    return this.result;
  }

  /**
   * Public constructor.
   * @param exception exception of the error
   */
  public SimpleStepResult createStepResult(final Exception exception) {

    return createStepResult(exception, null);
  }

  //
  // Listeners
  //

  /**
   * Inform listener that the status has been changed.
   */
  private void statusUpdated() {

    // for (StepStatusListener listener : this.listeners)
    // listener.statusChanged(this);
  }

  // /**
  // * Add a listener.
  // * @param listener listener to add
  // */
  // public void addListener(final Object listener) {
  //
  // if (listener == null)
  // return;
  //
  // this.listeners.add(listener);
  // }
  //
  // /**
  // * Remove listener.
  // * @param listener listener to remove
  // */
  // public void removeListener(final Object listener) {
  //
  // if (listener == null)
  // return;
  //
  // this.listeners.remove(listener);
  // }

  //
  // Check progress
  //

  private final void checkState() {

    Preconditions.checkState(!this.result.isImmutable(),
        "Step result has been created");
  }

  private final void checkSample(final Sample sample) {

    checkNotNull(sample, "Sample is null");
    checkArgument(this.design.contains(sample),
        "The design does not contains the sample");
  }

  private static final void checkProgress(final double progress) {

    checkArgument(progress >= 0.0, "Progress is lower than 0: " + progress);
    checkArgument(progress <= 1.0, "Progress is greater than 1: " + progress);
    checkArgument(!Double.isInfinite(progress), "Progress is infinite");
    checkArgument(!Double.isNaN(progress), "Progress is NaN");
  }

  private static final void checkProgress(final int min, final int max,
      final int value) {

    checkArgument(min <= max, "Max is lower than min");
    checkArgument(min <= value, "Value is lower than min");
    checkArgument(value <= max, "Value is greater than max");
  }

  //
  // Constructor
  //

  public StepStatus(final AbstractWorkflowStep step) {

    Preconditions.checkNotNull(step, "Step is null");

    this.step = step;
    this.design = step.getWorkflow().getDesign();

    this.result = new SimpleStepResult(step.getWorkflow().getContext(), step);

    // Get the start date
    this.startDate = new Date(System.currentTimeMillis());

    // Start stopWatch
    this.stopwatch.start();
  }

}
