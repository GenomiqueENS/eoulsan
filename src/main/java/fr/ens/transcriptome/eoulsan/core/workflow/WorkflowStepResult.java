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
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.FAIL;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
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

  private final Set<String> contextNames = Sets.newHashSet();
  private final Map<String, Map<String, Long>> contextCounters = Maps
      .newHashMap();
  private final Map<String, String> contextDescriptions = Maps.newHashMap();
  private final Map<String, String> contextMessages = Maps.newHashMap();
  private final Map<String, Long> stepCounters = Maps.newHashMap();
  private String stepMessage;

  private boolean success = true;
  private Throwable exception;
  private String errorMessage;

  private Set<String> contextIds = Sets.newHashSet();
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
   * @param contextName context name
   * @return the message for the context
   */
  public String getContextMessage(final String contextName) {

    return this.contextMessages.get(contextName);
  }

  /**
   * Get the contextName counters.
   * @param contextName contextName
   * @return the contextName counters as a map
   */
  public Map<String, Long> getContextCounters(final String contextName) {

    final Map<String, Long> result = this.contextCounters.get(contextName);

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

    this.immutable = true;
  }

  void addResult(final WorkflowStepContextResult result) {

    checkState();

    Preconditions.checkNotNull(result, "result cannot be null");

    // Set start and end times
    if (this.contextIds.isEmpty()) {
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

    final String contextName = result.getContext().getContextName();
    this.contextNames.add(contextName);

    // Set counters information
    this.contextCounters.put(contextName, result.getCounters());
    this.contextDescriptions.put(contextName, result.getDescription());

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

  private void checkState() {

    Preconditions.checkState(!this.immutable, "Step result has been created");
  }

  //
  // JSON
  //

  private static String toJSON(final int level, final String key, Object value) {

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

  private static String toJSON(final int level, final String key,
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
    sb.append("\"Contexts\" : [");
    boolean sampleProcessed = false;
    for (String contextName : this.contextNames) {

      // Do not log non processed samples
      if (!this.contextCounters.containsKey(contextName)
          && !this.contextMessages.containsKey(contextName))
        continue;

      if (!sampleProcessed) {
        sampleProcessed = true;
        sb.append('\n');
      }

      sb.append(Strings.repeat(TAB, 2));
      sb.append("{\n");
      sb.append(toJSON(4, "Context name", contextName));
      sb.append(toJSON(4, "Context description",
          this.contextDescriptions.get(contextName)));
      sb.append(toJSON(4, "Context message",
          this.contextMessages.get(contextName)));

      // contextName counters
      sb.append(toJSON(4, "Context counters",
          this.contextCounters.get(contextName)));
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

  private static Map<String, String> convert(final Set<Parameter> parameters) {

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

    // TODO implement this method
    throw new UnsupportedOperationException();
  }

  public void write(final DataFile file) throws IOException {

    checkNotNull(file, "file is null");

    write(file.create());
  }

  public void write(final OutputStream out) throws IOException {

    checkNotNull(out, "output stream is null");
    Preconditions.checkState(this.immutable,
        "Cannot write non immutable object");

    BufferedWriter writer = FileUtils.createBufferedWriter(out);
    writer.write(toJSON());
    writer.close();
  }

  //
  // Constructor
  //

  WorkflowStepResult() {
  }

  WorkflowStepResult(final AbstractWorkflowStep step) {

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
