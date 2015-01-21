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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.FAIL;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define a step result.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class WorkflowStepResult {

  private static final String TAB = "  ";

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

  private final Map<Integer, String> taskNames = new HashMap<>();
  private final Map<Integer, Map<String, Long>> taskCounters = new HashMap<>();
  private final Map<Integer, String> taskDescriptions = new HashMap<>();
  private final Map<Integer, String> taskMessages = new HashMap<>();
  private final Map<String, Long> stepCounters = new HashMap<>();
  private String stepMessage;

  private boolean success = true;
  private Throwable exception;
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
   * Get a context message.
   * @param contextId context id
   * @return the message for the context
   */
  public String getContextMessage(final int contextId) {

    return this.taskMessages.get(contextId);
  }

  /**
   * Get the contextName counters.
   * @param contextId context id
   * @return the contextName counters as a map
   */
  public Map<String, Long> getContextCounters(final int contextId) {

    final Map<String, Long> result = this.taskCounters.get(contextId);

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
  public Throwable getException() {

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

  /**
   * Set the object immutable.
   */
  public void setImmutable() {

    // Check immutable state
    checkImmutableState();

    // Check if at least one context result has been added to the step result
    checkState(!this.taskNames.isEmpty(),
        "No context result has been added for step " + this.stepId);

    this.immutable = true;
  }

  public void addResult(final TaskResult result) {

    checkNotNull(result, "result cannot be null");

    // Check immutable state
    checkImmutableState();

    // Check if result has been already added
    final int contextId = result.getContext().getId();
    checkState(!this.taskNames.containsKey(contextId), "Context #"
        + contextId + " has already been added to result of step "
        + this.stepId);

    // Set start and end times
    if (this.taskNames.isEmpty()) {
      this.startTime = result.getStartTime();
      this.endTime = result.getEndTime();
    } else {

      if (result.getStartTime().before(this.startTime)) {
        this.startTime = result.getStartTime();
      }

      if (result.getEndTime().after(this.endTime)) {
        this.endTime = result.getEndTime();
      }
    }

    // Compute duration
    this.duration = this.endTime.getTime() - this.startTime.getTime();

    this.taskNames.put(contextId, result.getContext().getContextName());

    // Set counters information
    this.taskCounters.put(contextId, result.getCounters());
    this.taskDescriptions.put(contextId, result.getDescription());

    // Set success (Keep only the first error)
    if (this.success) {
      if (!result.isSuccess()) {
        this.success = false;
        this.errorMessage = result.getErrorMessage();

        if (this.exception == null && result.getException() != null) {
          this.exception = result.getException();
        }

        // Set the state of the step as fail
        result.getContext().getStep().setState(FAIL);
      }

    }
  }

  //
  // Checker
  //

  private void checkImmutableState() {

    checkState(!this.immutable,
        "Step result has been already created for step " + this.stepId);
  }

  //
  // JSON
  //

  private static String toJSON(final int level, final String key,
      final Object value, boolean lastValue) {

    final StringBuilder sb = new StringBuilder();

    if (value == null) {
      return sb.toString();
    }

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

    if (!lastValue) {
      sb.append(',');
    }

    sb.append('\n');

    return sb.toString();
  }

  private static String toJSON(final int level, final String key,
      final Map<String, ?> counters, boolean lastValue) {

    final StringBuilder sb = new StringBuilder();

    sb.append(Strings.repeat(TAB, level));
    sb.append('\"');
    sb.append(key);
    sb.append("\" : {");
    if (counters == null || counters.isEmpty()) {
      sb.append("}\n");
    } else {
      sb.append('\n');
      int count = 0;
      for (Map.Entry<String, ?> e : counters.entrySet()) {
        sb.append(toJSON(level + 2, e.getKey(), e.getValue(),
            ++count == counters.size()));
      }

      sb.append(Strings.repeat(TAB, level));
      sb.append("}");

      if (!lastValue) {
        sb.append(',');
      }

      sb.append("\n");
    }

    return sb.toString();
  }

  public String toJSON() {

    final StringBuilder sb = new StringBuilder();
    sb.append('{');
    sb.append('\n');

    sb.append(toJSON(1, "Job id", this.jobId, false));
    sb.append(toJSON(1, "Job UUID", this.jobUUID, false));
    sb.append(toJSON(1, "Job description", this.jobDescription, false));
    sb.append(toJSON(1, "Job environment", this.jobEnvironment, false));
    sb.append(toJSON(1, "Step id", this.stepId, false));
    sb.append(toJSON(1, "Step name", this.stepName, false));
    sb.append(toJSON(1, "Step class", this.stepClass, false));
    sb.append(toJSON(1, "Step version", this.stepVersion == null
        ? null : this.stepVersion.toString(), false));
    sb.append(toJSON(1, "Start time", this.startTime.toString(), false));
    sb.append(toJSON(1, "End time", this.endTime.toString(), false));
    sb.append(toJSON(1, "Duration", toTimeHumanReadable(this.duration), false));
    sb.append(toJSON(1, "Duration in milliseconds", this.duration, false));
    sb.append(toJSON(1, "Success", Boolean.toString(this.success), false));
    sb.append(toJSON(1, "Step message", this.stepMessage, false));

    if (!this.success) {
      sb.append(toJSON(1, "Exception", this.exception == null
          ? "" : this.exception.getClass().getSimpleName(), false));
      sb.append(toJSON(1, "Exception message", this.errorMessage, false));
    }

    // Step parameters
    sb.append(toJSON(1, "Step parameters", convert(this.parameters), false));

    sb.append(Strings.repeat(TAB, 1));
    sb.append("\"Tasks\" : [");
    boolean sampleProcessed = false;
    for (int contextId : this.taskNames.keySet()) {

      // Do not log non processed samples
      if (!this.taskCounters.containsKey(contextId)
          && !this.taskMessages.containsKey(contextId)) {
        continue;
      }

      if (!sampleProcessed) {
        sampleProcessed = true;
        sb.append('\n');
      }

      sb.append(Strings.repeat(TAB, 2));
      sb.append("{\n");
      sb.append(toJSON(4, "Task id", contextId, false));
      sb.append(toJSON(4, "Task name", this.taskNames.get(contextId), false));
      sb.append(toJSON(4, "Task description",
          this.taskDescriptions.get(contextId), false));
      sb.append(toJSON(4, "Task message", this.taskMessages.get(contextId),
          false));

      // contextName counters
      sb.append(toJSON(4, "Task counters", this.taskCounters.get(contextId),
          true));
      sb.append(Strings.repeat(TAB, 2));
      sb.append("},\n");
    }

    // Remove last comma
    sb.delete(sb.length() - 2, sb.length() - 1);

    if (sampleProcessed) {
      sb.append(Strings.repeat(TAB, 1));
      sb.append("]\n");
    } else {
      sb.append("]\n");
    }

    sb.append("}\n");
    return sb.toString();
  }

  /**
   * Get a representation of the result in the old Eoulsan format.
   * @return a String with the result
   */
  public String toEoulsanLogV1() {

    final StringBuilder sb = new StringBuilder();

    sb.append("Job Id: ");
    sb.append(this.jobId);
    sb.append(" [");
    sb.append(this.jobUUID);
    sb.append(']');
    sb.append("\nJob description: ");
    sb.append(this.jobDescription);
    sb.append("\nJob environment: ");
    sb.append(this.jobEnvironment);
    sb.append("\nStep: ");
    sb.append(this.stepId);
    sb.append(" [");
    sb.append(this.stepClass);
    sb.append("]");
    sb.append("\nParameters:\n");

    // Sort the parameters
    final List<Parameter> parametersList = Lists.newArrayList(this.parameters);
    Collections.sort(parametersList, new Comparator<Parameter>() {

      @Override
      public int compare(final Parameter p1, final Parameter p2) {

        return p1.getName().compareTo(p2.getName());
      }
    });

    for (Parameter p : parametersList) {
      sb.append('\t');
      sb.append(p.getName());
      sb.append(": ");
      sb.append(p.getStringValue());
      sb.append('\n');
    }

    sb.append("Start time: ");
    sb.append(this.startTime);
    sb.append("\nEnd time: ");
    sb.append(this.endTime);
    sb.append("\nDuration: ");
    sb.append(StringUtils.toTimeHumanReadable(this.duration));
    sb.append('\n');

    for (int contextId : this.taskNames.keySet()) {

      sb.append(this.taskDescriptions.get(contextId));
      sb.append('\n');

      for (Map.Entry<String, Long> counter : this.taskCounters.get(contextId)
          .entrySet()) {
        sb.append('\t');
        sb.append(counter.getKey());
        sb.append('=');
        sb.append(counter.getValue());
        sb.append('\n');
      }
    }

    return sb.toString();
  }

  private static Map<String, String> convert(final Set<Parameter> parameters) {

    if (parameters == null) {
      return Collections.emptyMap();
    }

    final Map<String, String> result = new LinkedHashMap<>();
    for (Parameter p : parameters) {
      result.put(p.getName(), p.getStringValue());
    }

    return result;
  }

  //
  // I/O
  //

  public void read(final DataFile file) throws IOException {

    checkNotNull(file, "file is null");

    read(file.open());
  }

  public void read(final InputStream in) {

    checkNotNull(in);
    checkImmutableState();

    // TODO implement this method
    throw new UnsupportedOperationException();
  }

  /**
   * Write the result.
   * @param file output file
   * @param oldFormat write the result in old Eoulsan format instead of JSON
   * @throws IOException if an error occurs while writing result
   */
  public void write(final DataFile file, final boolean oldFormat)
      throws IOException {

    checkNotNull(file, "file is null");

    write(file.create(), oldFormat);
  }

  /**
   * Write the result.
   * @param out output stream
   * @param oldFormat write the result in old Eoulsan format instead of JSON
   * @throws IOException if an error occurs while writing result
   */
  public void write(final OutputStream out, final boolean oldFormat)
      throws IOException {

    checkNotNull(out, "output stream is null");
    checkState(this.immutable, "Cannot write non immutable object");

    BufferedWriter writer = FileUtils.createBufferedWriter(out);

    if (oldFormat) {
      writer.write(toEoulsanLogV1());
    } else {
      writer.write(toJSON());
    }
    writer.close();
  }

  //
  // Constructor
  //

  WorkflowStepResult() {
  }

  public WorkflowStepResult(final AbstractWorkflowStep step) {

    Preconditions.checkNotNull(step, "step is null");

    final WorkflowContext workflowContext =
        step.getAbstractWorkflow().getWorkflowContext();

    this.jobId = workflowContext.getJobId();
    this.jobUUID = workflowContext.getJobUUID();
    this.jobDescription = workflowContext.getJobDescription();
    this.jobEnvironment = workflowContext.getJobEnvironment();

    this.stepId = step.getId();
    this.stepName = step.getStepName();
    this.stepClass =
        step.getStep() == null ? null : step.getStep().getClass().getName();
    this.stepVersion =
        step.getStep() == null ? null : step.getStep().getVersion();
    this.parameters = step.getParameters();
  }

}
