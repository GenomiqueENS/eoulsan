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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.nullToEmpty;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.FAILED;
import static fr.ens.biologie.genomique.eoulsan.util.StringUtils.toTimeHumanReadable;
import static java.util.Objects.requireNonNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.Version;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;

/**
 * This class define a step result.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class StepResult {

  private static final String TASK_COUNTERS_TAG = "Task counters";
  private static final String TASK_MESSAGE_TAG = "Task message";
  private static final String TASK_DESCRIPTION_TAG = "Task description";
  private static final String TASK_COMMAND_LINE_TAG = "Task command line";
  private static final String TASK_NAME_TAG = "Task name";
  private static final String TASK_ID_TAG = "Task id";
  private static final String TASKS_TAG = "Tasks";
  private static final String COUNTERS_TAG = "Counters";
  private static final String STEP_PARAMETERS_TAG = "Step parameters";
  private static final String STEP_MESSAGE_TAG = "Step message";
  private static final String SUCCESS_TAG = "Success";
  private static final String START_TIME_TAG = "Start time";
  private static final String END_TIME_TAG = "End time";
  private static final String DURATION_TAG = "Duration";
  private static final String DURATION_IN_MILLISECONDS_TAG =
      "Duration in milliseconds";
  private static final String STEP_VERSION_TAG = "Step version";
  private static final String STEP_CLASS_TAG = "Step class";
  private static final String STEP_NAME_TAG = "Step name";
  private static final String STEP_ID_TAG = "Step id";
  private static final String JOB_ENVIRONMENT_TAG = "Job environment";
  private static final String JOB_DESCRIPTION_TAG = "Job description";
  private static final String JOB_UUID_TAG = "Job UUID";
  private static final String JOB_ID_TAG = "Job id";
  private static final String EXCEPTION_MESSAGE_TAG = "Exception message";
  private static final String EXCEPTION_TAG = "Exception";

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
  private final Map<String, Map<String, Long>> counters = new HashMap<>();
  private final Map<Integer, String> taskDescriptions = new HashMap<>();
  private final Map<Integer, String> taskMessages = new HashMap<>();
  private final Map<Integer, String> taskCommandLines = new HashMap<>();
  private final Map<String, Long> stepCounters = new HashMap<>();
  private String stepMessage;

  private boolean success = true;
  private Throwable exception;
  private String errorMessage;

  private boolean immutable;

  private final DateFormat dateFormat = DateFormat.getDateTimeInstance(
      DateFormat.LONG, DateFormat.LONG, Locale.getDefault());

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

  /**
   * Add a task result to the step result.
   * @param context the context to execute
   * @param result the result to add
   */
  public void addResult(final TaskContextImpl context,
      final TaskResultImpl result) {

    requireNonNull(context, "result cannot be null");
    requireNonNull(result, "result cannot be null");

    // Check immutable state
    checkImmutableState();

    // Check if result has been already added
    final int contextId = context.getId();
    checkState(!this.taskNames.containsKey(contextId),
        "Context #"
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

    final String taskName = context.getContextName();
    this.taskNames.put(contextId, taskName);

    // Set counters information
    this.taskCounters.put(contextId, result.getCounters());
    this.taskDescriptions.put(contextId, result.getDescription());
    this.taskCommandLines.put(contextId, result.getCommandLine());
    addCounters(taskName, result.getCounters());

    // Set success (Keep only the first error)
    if (this.success) {
      if (!result.isSuccess()) {
        this.success = false;
        this.errorMessage = result.getErrorMessage();

        if (this.exception == null && result.getException() != null) {
          this.exception = result.getException();
        }

        // Set the state of the step as fail
        WorkflowEventBus.getInstance().postStepStateChange(context.getStep(),
            FAILED);
      }

    }
  }

  /**
   * Add counters task to the group counters.
   * @param counterGroup the name of the counter group
   * @param counters the counters of the task
   */
  private void addCounters(final String counterGroup,
      Map<String, Long> counters) {

    final Map<String, Long> map;

    if (!this.counters.containsKey(counterGroup)) {

      map = new LinkedHashMap<>();
      this.counters.put(counterGroup, map);
    } else {

      map = this.counters.get(counterGroup);
    }

    for (Map.Entry<String, Long> e : counters.entrySet()) {

      final String key = e.getKey();

      if (map.containsKey(key)) {

        map.put(key, map.get(key) + e.getValue());
      } else {
        map.put(key, e.getValue());
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

  /**
   * Convert the object to JSON
   * @return a string with the object content at the JSON format
   */
  public String toJSON() {

    final StringWriter writer = new StringWriter();

    // Create a pretty Json generator
    final Map<String, Object> properties = new HashMap<>(1);
    properties.put(JsonGenerator.PRETTY_PRINTING, true);
    JsonGeneratorFactory jgf = Json.createGeneratorFactory(properties);
    JsonGenerator jg = jgf.createGenerator(writer);

    jg.writeStartObject();
    jg.write(JOB_ID_TAG, this.jobId);
    jg.write(JOB_UUID_TAG, this.jobUUID);
    jg.write(JOB_DESCRIPTION_TAG, this.jobDescription);
    jg.write(JOB_ENVIRONMENT_TAG, this.jobEnvironment);
    jg.write(STEP_ID_TAG, this.stepId);
    jg.write(STEP_NAME_TAG, this.stepName);
    jg.write(STEP_CLASS_TAG, this.stepClass);
    jg.write(STEP_VERSION_TAG,
        this.stepVersion == null ? null : this.stepVersion.toString());
    jg.write(START_TIME_TAG, this.dateFormat.format(this.startTime));
    jg.write(END_TIME_TAG, this.dateFormat.format(this.endTime));
    jg.write(DURATION_TAG, toTimeHumanReadable(this.duration));
    jg.write(DURATION_IN_MILLISECONDS_TAG, this.duration);
    jg.write(SUCCESS_TAG, this.success);
    jg.write(STEP_MESSAGE_TAG, nullToEmpty(this.stepMessage));

    if (!this.success) {
      jg.write(EXCEPTION_TAG, this.exception == null
          ? "" : this.exception.getClass().getSimpleName());
      jg.write(EXCEPTION_MESSAGE_TAG);
    }

    // Step parameters
    jg.writeStartObject(STEP_PARAMETERS_TAG);
    for (Parameter p : this.parameters) {
      jg.write(p.getName(), p.getStringValue());
    }
    jg.writeEnd();

    // Counters
    jg.writeStartObject(COUNTERS_TAG);

    for (Map.Entry<String, Map<String, Long>> e : this.counters.entrySet()) {

      jg.writeStartObject(e.getKey());

      for (Map.Entry<String, Long> e2 : e.getValue().entrySet()) {
        jg.write(e2.getKey(), e2.getValue());
      }

      jg.writeEnd(); // Counter group
    }
    jg.writeEnd(); // Counters

    // Tasks
    jg.writeStartArray(TASKS_TAG);

    for (int contextId : this.taskNames.keySet()) {

      // Do not log non processed samples
      if (!this.taskCounters.containsKey(contextId)
          && !this.taskMessages.containsKey(contextId)) {
        continue;
      }

      jg.writeStartObject();
      jg.write(TASK_ID_TAG, contextId);
      jg.write(TASK_NAME_TAG, this.taskNames.get(contextId));
      jg.write(TASK_DESCRIPTION_TAG,
          nullToEmpty(this.taskDescriptions.get(contextId)));
      jg.write(TASK_MESSAGE_TAG, nullToEmpty(this.taskMessages.get(contextId)));
      jg.write(TASK_COMMAND_LINE_TAG,
          nullToEmpty(this.taskCommandLines.get(contextId)));

      // contextName counters
      jg.writeStartObject(TASK_COUNTERS_TAG);
      for (Map.Entry<String, Long> e : this.taskCounters.get(contextId)
          .entrySet()) {
        jg.write(e.getKey(), e.getValue());
      }
      jg.writeEnd(); // Tasks counters

      jg.writeEnd(); // Task
    }
    jg.writeEnd(); // Tasks array
    jg.writeEnd(); // JSON
    jg.close();

    return writer.toString();
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
    parametersList.sort(Comparator.comparing(Parameter::getName));

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

      if (this.taskCounters.containsKey(contextId)) {
        for (Map.Entry<String, Long> counter : this.taskCounters.get(contextId)
            .entrySet()) {
          sb.append('\t');
          sb.append(counter.getKey());
          sb.append('=');
          sb.append(counter.getValue());
          sb.append('\n');
        }
      }
    }

    return sb.toString();
  }

  //
  // I/O
  //

  /**
   * Read a step result file.
   * @param file the file to read
   * @throws IOException if an error occurs while reading the file
   */
  public void read(final DataFile file) throws IOException {

    requireNonNull(file, "file is null");

    read(file.open());
  }

  /**
   * Read a step result file.
   * @param in the input stream to read
   */
  public void read(final InputStream in) {

    requireNonNull(in);
    checkImmutableState();

    final JsonReader reader = Json.createReader(new InputStreamReader(in));
    final JsonObject obj = reader.readObject();

    this.jobId = obj.getString(JOB_ID_TAG);
    this.jobUUID = obj.getString(JOB_UUID_TAG);
    this.jobDescription = obj.getString(JOB_DESCRIPTION_TAG);
    this.jobEnvironment = obj.getString(JOB_ENVIRONMENT_TAG);
    this.stepId = obj.getString(STEP_ID_TAG);
    this.stepName = obj.getString(STEP_NAME_TAG);
    this.stepClass = obj.getString(STEP_CLASS_TAG);
    this.stepVersion = new Version(obj.getString(STEP_VERSION_TAG));
    this.startTime = parseDate(obj.getString(START_TIME_TAG));
    this.endTime = parseDate(obj.getString(END_TIME_TAG));
    this.duration = obj.getInt(DURATION_IN_MILLISECONDS_TAG);
    this.success = obj.getBoolean(SUCCESS_TAG);
    this.stepMessage = obj.getString(STEP_MESSAGE_TAG);

    // Parse parameters
    this.parameters = new LinkedHashSet<>();
    final JsonObject parametersObj = obj.getJsonObject(STEP_PARAMETERS_TAG);
    for (String key : parametersObj.keySet()) {
      this.parameters.add(new Parameter(key, parametersObj.getString(key)));
    }

    // Parse counters
    final JsonObject countersObj = obj.getJsonObject(COUNTERS_TAG);
    for (String group : countersObj.keySet()) {

      if (!this.counters.containsKey(group)) {
        this.counters.put(group, new HashMap<>());
      }
      final Map<String, Long> map = this.counters.get(group);

      JsonObject groupObj = countersObj.getJsonObject(group);
      for (String counterName : groupObj.keySet()) {
        map.put(counterName, groupObj.getJsonNumber(counterName).longValue());
      }
    }

    // Parse task
    final JsonArray tasksArray = obj.getJsonArray(TASKS_TAG);
    for (JsonValue entry : tasksArray) {

      final JsonObject entryObj = (JsonObject) entry;

      final int taskId = entryObj.getInt(TASK_ID_TAG);

      this.taskNames.put(taskId, entryObj.getString(TASK_NAME_TAG));
      this.taskDescriptions.put(taskId,
          entryObj.getString(TASK_DESCRIPTION_TAG));
      this.taskMessages.put(taskId, entryObj.getString(TASK_MESSAGE_TAG));

      final Map<String, Long> map = new HashMap<>();
      this.taskCounters.put(taskId, map);

      final JsonObject taskCountersObj =
          entryObj.getJsonObject(TASK_COUNTERS_TAG);
      for (String counterName : taskCountersObj.keySet()) {
        map.put(counterName,
            taskCountersObj.getJsonNumber(counterName).longValue());
      }
    }

  }

  /**
   * Write the result.
   * @param file output file
   * @param oldFormat write the result in old Eoulsan format instead of JSON
   * @throws IOException if an error occurs while writing result
   */
  public void write(final DataFile file, final boolean oldFormat)
      throws IOException {

    requireNonNull(file, "file is null");

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

    requireNonNull(out, "output stream is null");
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
  // Other methods
  //

  /**
   * Parse date.
   * @param s the string to parse
   * @return a Date object or null if the date cannot be parsed
   */
  private Date parseDate(final String s) {

    if (s == null) {
      return null;
    }

    try {
      return this.dateFormat.parse(s);
    } catch (ParseException e) {
      return null;
    }
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   */
  StepResult() {
  }

  /**
   * Constructor.
   * @param step the step
   */
  public StepResult(final AbstractStep step) {

    requireNonNull(step, "step is null");

    final WorkflowContext workflowContext =
        step.getAbstractWorkflow().getWorkflowContext();

    this.jobId = workflowContext.getJobId();
    this.jobUUID = workflowContext.getJobUUID();
    this.jobDescription = workflowContext.getJobDescription();
    this.jobEnvironment = workflowContext.getJobEnvironment();

    this.stepId = step.getId();
    this.stepName = step.getModuleName();
    this.stepClass =
        step.getModule() == null ? null : step.getModule().getClass().getName();
    this.stepVersion =
        step.getModule() == null ? null : step.getModule().getVersion();
    this.parameters = step.getParameters();
  }

}
