package fr.ens.transcriptome.eoulsan.core.workflow;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * Created by jourdren on 27/07/14.
 */
public class WorkflowStepContextResult implements StepResult {

  private final WorkflowStepContext context;

  private final Date startTime;
  private final Date endTime;
  private final long duration;
  private final boolean success;
  private final Throwable exception;
  private final String errorMessage;
  private final Map<String, Long> counters = Maps.newHashMap();
  private final String contextMessage;
  private final String contextDescription;

  WorkflowStepContext getContext() {
    return this.context;
  }

  Date getStartTime() {
    return this.startTime;
  }

  Date getEndTime() {
    return this.endTime;
  }

  Map<String, Long> getCounters() {
    return Collections.unmodifiableMap(this.counters);
  }

  String getDescription() {
    return this.contextDescription;
  }

  String getMessage() {
    return this.contextMessage;
  }

  @Override
  public long getDuration() {
    return this.duration;
  }

  @Override
  public boolean isSuccess() {
    return this.success;
  }

  @Override
  public Throwable getException() {
    return this.exception;
  }

  @Override
  public String getErrorMessage() {
    return this.errorMessage;
  }

  @Override
  public void write(final DataFile file) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void write(final OutputStream out) throws IOException {
    throw new UnsupportedOperationException();
  }

  //
  // Constructor
  //

  WorkflowStepContextResult(final WorkflowStepContext context,
      final Date startTime, final Date endTime, final long duration,
      final String contextMessage, final String contextDescription,
      final Map<String, Long> counters, final boolean success) {

    Preconditions.checkNotNull(context, "context argument cannot be null");
    Preconditions.checkNotNull(startTime, "startTime argument cannot be null");
    Preconditions.checkNotNull(endTime, "endTime argument cannot be null");
    Preconditions.checkNotNull(contextDescription,
        "contextDescription argument cannot be null");
    Preconditions.checkNotNull(counters, "counter argument cannot be null");

    this.context = context;
    this.startTime = startTime;
    this.endTime = endTime;
    this.duration = duration;
    this.success = success;
    this.contextMessage = contextMessage;
    this.contextDescription = contextDescription;
    this.counters.putAll(counters);
    this.exception = null;
    this.errorMessage = null;
  }

  WorkflowStepContextResult(final WorkflowStepContext context,
      final Date startTime, final Date endTime, final long duration,
      final Throwable exception, final String errorMessage) {

    Preconditions.checkNotNull(context, "context argument cannot be null");

    this.context = context;
    this.startTime = startTime;
    this.endTime = endTime;
    this.duration = duration;
    this.success = false;
    this.contextMessage = null;
    this.contextDescription = null;
    this.exception = exception;
    this.errorMessage = errorMessage;
  }

}
