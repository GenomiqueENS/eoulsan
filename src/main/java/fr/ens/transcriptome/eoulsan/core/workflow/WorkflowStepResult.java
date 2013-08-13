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
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.Reporter;
import fr.ens.transcriptome.eoulsan.util.Version;

public class WorkflowStepResult implements StepResult {

  private static final String TAB = "  ";

  private List<Sample> samples = Lists.newArrayList();

  private String jobId;
  private String jobUUID;
  private String jobDescription;
  private String jobEnvironment;
  private String stepId;
  private String stepName;
  private String stepClass;
  private Version stepVersion;
  private Date startTime;
  private Date endTime;
  private long duration;
  private Set<Parameter> parameters;

  private final Map<Sample, Map<String, Long>> sampleCounters = Maps
      .newHashMap();
  private final Map<Sample, String> sampleCountersHeader = Maps.newHashMap();
  private final Map<Sample, String> sampleMessages = Maps.newHashMap();
  private final Map<String, Long> stepCounters = Maps.newHashMap();
  private String stepMessage;

  private boolean success;
  private Exception exception;
  private String errorMessage;

  private boolean immutable;

  //
  // Getters
  //

  /**
   * Get step message.
   * @return the step message in a String
   */
  public String getStepMessage() {

    return this.stepMessage;
  }

  /**
   * Get the step counters.
   * @return the step counters in a map
   */
  public Map<String, Long> getStepCounters() {

    return Collections.unmodifiableMap(this.stepCounters);
  }

  /**
   * Get sample message.
   * @param sample sample
   * @return the message for the sample
   */
  public String getSampleMessage(final Sample sample) {

    checkSample(sample);

    return this.sampleMessages.get(sample);
  }

  /**
   * Get the sample counters.
   * @param sample sample
   * @return the sample counters as a map
   */
  public Map<String, Long> getSampleCounters(final Sample sample) {

    checkSample(sample);

    final Map<String, Long> result = this.sampleCounters.get(sample);

    return Collections.unmodifiableMap(result);
  }

  /**
   * Test if the object immutable.
   * @return true if the object is immutable
   */
  public boolean isImmutable() {

    return this.immutable;
  }

  /**
   * Test if the step result is a success.
   * @return true if the step result is a success
   */
  public boolean isSuccess() {

    return this.success;
  }

  /**
   * Get the exception.
   * @return an Exception object or null if the step has not returned an
   *         Exception
   */
  public Exception getException() {

    return this.exception;
  }

  /**
   * Get the error message.
   * @return the error message
   */
  public String getErrorMessage() {

    return this.errorMessage;
  }

  /**
   * Get the duration of the step.
   * @return the duration of the step in milliseconds
   */
  public long getDuration() {

    return this.duration;
  }

  //
  // Setters
  //

  /**
   * Set the start date.
   * @param date the start date
   */
  public void setStartTime(final Date date) {

    checkNotNull(date, "date is null");

    this.startTime = date;
  }

  /**
   * Set the end date.
   * @param date the end date
   */
  public void setEndTime(final Date date) {

    checkNotNull(date, "date is null");

    this.endTime = date;
  }

  /**
   * Set the duration of the step.
   * @param millis duration in milliseconds
   */
  public void setDuration(final long millis) {

    this.duration = millis;
  }

  /**
   * Set the step message.
   * @param message message to set
   */
  public void setStepMessage(final String message) {

    checkState();

    if (message != null)
      this.stepMessage = message;
  }

  /**
   * Set the step counters.
   * @param reporter the reporter
   * @param counterGroup counter group to use with the reporter
   * @param sampleCounterHeader header for the sample (optional)
   */
  public void setStepCounters(final Reporter reporter,
      final String counterGroup, final String sampleCounterHeader) {

    checkState();

    checkNotNull(reporter, "Reporter is null");
    checkNotNull(counterGroup, "Counter group is null");

    // Add all counters
    for (String counterName : reporter.getCounterNames(counterGroup))
      this.stepCounters.put(counterName,
          reporter.getCounterValue(counterGroup, counterName));
  }

  /**
   * Set the sample message.
   * @param sample the sample
   * @param message the message to set
   */
  public void setSampleMessage(final Sample sample, final String message) {

    checkState();
    checkSample(sample);

    if (message != null)
      this.sampleMessages.put(sample, message);

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

    checkState();
    checkSample(sample);

    checkNotNull(reporter, "Reporter is null");
    checkNotNull(counterGroup, "Counter group is null");

    // Check if sample counter has been already set
    if (this.sampleCounters.containsKey(sample))
      throw new IllegalStateException("result for sample "
          + sample.getName() + " has already set");

    if (sampleCounterHeader != null)
      this.sampleCountersHeader.put(sample, sampleCounterHeader);

    // Create map for sample counter
    final Map<String, Long> values = Maps.newHashMap();
    this.sampleCounters.put(sample, values);

    // Add all counters
    for (String counterName : reporter.getCounterNames(counterGroup))
      values.put(counterName,
          reporter.getCounterValue(counterGroup, counterName));
  }

  /**
   * Set the object immutable.
   */
  public void setImmutable() {

    this.immutable = true;
  }

  /**
   * Set if the step result is a success.
   * @param success the step result
   */
  public void setSuccess(final boolean success) {

    checkState();

    this.success = success;
  }

  public void setException(final Exception exception, final String errorMessage) {

    checkState();

    checkNotNull(exception, "exception is null");

    this.success = false;
    this.exception = exception;
    this.errorMessage = errorMessage;

  }

  //
  // Checker
  //

  private final void checkState() {

    Preconditions.checkState(!this.immutable, "Step result has been created");
  }

  private final void checkSample(final Sample sample) {

    checkNotNull(sample, "Sample is null");
    checkArgument(this.samples.contains(sample),
        "The design does not contains the sample");
  }

  //
  // JSON
  //

  private static final String toJSON(final int level, final String key,
      Object value) {

    final StringBuilder sb = new StringBuilder();

    if (value == null)
      return sb.toString();

    sb.append(Strings.repeat(TAB, level));
    sb.append('\"');
    sb.append(key);
    sb.append("\" : ");

    if ((value instanceof Number)) {
      sb.append(((Number) value).longValue());
    } else {
      sb.append('\"');
      sb.append(value.toString().trim());
      sb.append('\"');
    }
    sb.append('\n');

    return sb.toString();
  }

  private static final String toJSON(final int level, final String key,
      final Map<String, ?> counters) {

    final StringBuilder sb = new StringBuilder();

    sb.append(Strings.repeat(TAB, level));
    sb.append('\"');
    sb.append(key);
    sb.append("\" : {");
    if (counters == null || counters.isEmpty())
      sb.append("}\n");
    else {
      sb.append('\n');
      for (Map.Entry<String, ?> e : counters.entrySet())
        sb.append(toJSON(level + 2, e.getKey(), e.getValue()));
      sb.append(Strings.repeat(TAB, level));
      sb.append("}\n");
    }

    return sb.toString();
  }

  public String toJSON() {

    final StringBuilder sb = new StringBuilder();
    sb.append('{');
    sb.append('\n');

    sb.append(toJSON(1, "Job id", this.jobId));
    sb.append(toJSON(1, "Job UUID", this.jobUUID));
    sb.append(toJSON(1, "Job description", this.jobDescription));
    sb.append(toJSON(1, "Job environment", this.jobEnvironment));
    sb.append(toJSON(1, "Step id", this.stepId));
    sb.append(toJSON(1, "Step name", this.stepName));
    sb.append(toJSON(1, "Step class", this.stepClass));
    sb.append(toJSON(1, "Step version", this.stepVersion == null
        ? null : this.stepVersion.toString()));
    sb.append(toJSON(1, "Start time", this.startTime.toString()));
    sb.append(toJSON(1, "End time", this.endTime.toString()));
    sb.append(toJSON(1, "Duration", toTimeHumanReadable(this.duration)));
    sb.append(toJSON(1, "Duration in milliseconds", this.duration));
    sb.append(toJSON(1, "Success", Boolean.toString(this.success)));
    sb.append(toJSON(1, "Step message", this.stepMessage));

    if (!this.success) {
      sb.append(toJSON(1, "Exception", this.exception == null
          ? "" : this.exception.getClass().getSimpleName()));
      sb.append(toJSON(1, "Exception message", this.errorMessage));
    }

    // Step parameters
    sb.append(toJSON(1, "Step parameters", convert(this.parameters)));

    sb.append(Strings.repeat(TAB, 1));
    sb.append("\"Samples\" : [");
    boolean sampleProcessed = false;
    for (Sample sample : this.samples) {

      // Do not log non processed samples
      if (!this.sampleCounters.containsKey(sample)
          && !this.sampleMessages.containsKey(sample))
        continue;

      if (!sampleProcessed) {
        sampleProcessed = true;
        sb.append('\n');
      }

      sb.append(Strings.repeat(TAB, 2));
      sb.append("{\n");
      sb.append(toJSON(4, "Sample id", sample.getId()));
      sb.append(toJSON(4, "Sample name", sample.getName()));
      sb.append(toJSON(4, "Sample info", this.sampleCountersHeader.get(sample)));
      sb.append(toJSON(4, "Sample message", this.sampleMessages.get(sample)));

      // sample counters
      sb.append(toJSON(4, "Sample counters", this.sampleCounters.get(sample)));
      sb.append(Strings.repeat(TAB, 2));
      sb.append("}\n");
    }
    if (sampleProcessed) {
      sb.append(Strings.repeat(TAB, 1));
      sb.append("]\n");
    } else {
      sb.append("]\n");
    }

    sb.append('}');
    return sb.toString();
  }

  private static final Map<String, String> convert(
      final Set<Parameter> parameters) {

    if (parameters == null)
      return Collections.emptyMap();

    final Map<String, String> result = Maps.newLinkedHashMap();
    for (Parameter p : parameters)
      result.put(p.getName(), p.getStringValue());

    return result;
  }

  //
  // I/O
  //

  public void read(final DataFile file) throws IOException {

    checkNotNull(file, "file is null");

    read(file.open());
  }

  public void read(InputStream in) {

    checkNotNull(in);
    checkState();

    throw new NotImplementedException();
  }

  public void write(final DataFile file) throws IOException {

    checkNotNull(file, "file is null");

    write(file.create());
  }

  public void write(final OutputStream out) throws IOException {

    checkNotNull(out, "output stream is null");

    BufferedWriter writer = FileUtils.createBufferedWriter(out);
    writer.write(toJSON());
    writer.close();
  }

  //
  // Constructor
  //

  WorkflowStepResult() {

  }

  WorkflowStepResult(final StepContext context, final AbstractWorkflowStep step) {

    Preconditions.checkNotNull(context, "context is null");
    Preconditions.checkNotNull(step, "step is null");

    this.jobId = context.getJobId();
    this.jobUUID = context.getJobUUID();
    this.jobDescription = context.getJobDescription();
    this.jobEnvironment = context.getJobEnvironment();

    this.stepId = step.getId();
    this.stepName = step.getStepName();
    this.stepClass =
        step.getStep() == null ? null : step.getStep().getClass().getName();
    this.stepVersion =
        step.getStep() == null ? null : step.getStep().getVersion();
    this.parameters = step.getParameters();
    this.samples = context.getWorkflow().getDesign().getSamples();
  }

}
