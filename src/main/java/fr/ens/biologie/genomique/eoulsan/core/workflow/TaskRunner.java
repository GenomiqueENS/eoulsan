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
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.Globals.TASK_LOG_EXTENSION;
import static fr.ens.biologie.genomique.eoulsan.annotations.EoulsanAnnotationUtils.isNoLog;
import static fr.ens.biologie.genomique.eoulsan.annotations.EoulsanAnnotationUtils.isReuseStepInstance;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.PARTIALLY_DONE;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.WORKING;
import static fr.ens.biologie.genomique.kenetre.util.StringUtils.stackTraceToString;
import static fr.ens.biologie.genomique.kenetre.util.StringUtils.toTimeHumanReadable;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Joiner;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.Main;
import fr.ens.biologie.genomique.eoulsan.core.Module;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.Step.StepType;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.kenetre.util.Version;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

/**
 * This class allow to run a task context.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TaskRunner {

  private final TaskContextImpl context;
  private final Module module;
  private final TaskStatusImpl status;
  private volatile TaskResult result;
  private boolean isTokensSent;
  private boolean forceStepInstanceReuse;

  //
  // Getter
  //

  /**
   * Get the context result.
   *
   * @return a TaskResult object
   */
  public TaskResultImpl getResult() {

    checkState(this.result != null, "The context has not been run");

    return (TaskResultImpl) this.result;
  }

  //
  // Setter
  //

  /**
   * Force the TaskRunner to reuse the original step instance when execute the task.
   *
   * @param reuse true if the step instance must be reuse when execute the task
   */
  public void setForceStepInstanceReuse(final boolean reuse) {

    this.forceStepInstanceReuse = reuse;
  }

  //
  // Execute methods
  //

  /**
   * Run the task context.
   *
   * @return a task result object
   */
  public TaskResultImpl run() {

    // Check if task has been already executed
    checkState(this.result == null, "task has been already executed");

    // Thread group name
    final String threadGroupName =
        "TaskRunner_" + this.context.getStep().getId() + "_#" + this.context.getId();

    // Define thread group
    final ThreadGroup threadGroup = new ThreadGroup(threadGroupName);

    // Create Log handler and register it
    final Logger logger = isNoLog(this.module) ? null : createStepLogger(threadGroupName);

    // Register the logger
    if (logger != null) {
      EoulsanLogger.registerThreadGroupLogger(threadGroup, logger);
    }

    // We use here a thread to execute the step
    // This allow to save log of step in distinct files
    final Runnable r =
        new Runnable() {

          @Override
          public void run() {

            getLogger().info("Start of task #" + TaskRunner.this.context.getId());
            final long startTime = System.currentTimeMillis();

            final Module module;
            final StepType stepType = TaskRunner.this.context.getWorkflowStep().getType();
            final boolean reuseAnnot = isReuseStepInstance(TaskRunner.this.module);

            final String stepDescLog =
                String.format(
                    "step (id: %s, name: %s, class: %s) for task #%d",
                    TaskRunner.this.context.getWorkflowStep().getId(),
                    TaskRunner.this.module.getName(),
                    TaskRunner.this.module.getClass().getName(),
                    TaskRunner.this.context.getId());

            try {

              // If step is a standard step and reuse of step instance is not
              // required by step
              // Create a new instance of the step for the task
              if (stepType == StepType.STANDARD_STEP
                  && !reuseAnnot
                  && !TaskRunner.this.forceStepInstanceReuse) {

                // Create the new instance of the step
                getLogger().fine("Create new instance of " + stepDescLog);

                final String stepName = TaskRunner.this.module.getName();
                final Version stepVersion = TaskRunner.this.module.getVersion();

                module = ModuleRegistry.getInstance().loadModule(stepName, stepVersion.toString());

                // Log step parameters
                logStepParameters();

                // Configure the new step instance
                getLogger().fine("Configure step instance");
                module.configure(
                    new StepConfigurationContextImpl(TaskRunner.this.context.getStep()),
                    TaskRunner.this.context.getCurrentStep().getParameters());

              } else {

                // Use the original step instance for the task
                getLogger().fine("Reuse original instance of " + stepDescLog);
                module = TaskRunner.this.module;

                // Log step parameters
                logStepParameters();
              }

              // Execute task
              getLogger().info("Execute task");
              TaskRunner.this.result =
                  module.execute(TaskRunner.this.context, TaskRunner.this.status);

            } catch (Throwable t) {

              getLogger().severe("Exception while executing task: " + t.getMessage());

              // Handle exception not catch by step code
              TaskRunner.this.result = TaskRunner.this.status.createTaskResult(t);
            }

            final long duration = System.currentTimeMillis() - startTime;
            final TaskResult result = TaskRunner.this.result;
            final boolean success = result.isSuccess();

            getLogger().info("End of task #" + TaskRunner.this.context.getId());
            getLogger().info("Duration: " + toTimeHumanReadable(duration));
            getLogger().info("Result: " + (success ? "Success" : "Fail"));

            if (!success) {

              final String errorMessage = result.getErrorMessage();
              final Throwable exception = result.getException();

              if (errorMessage != null) {
                getLogger().severe("Error message: " + errorMessage);
              }

              if (exception != null) {
                getLogger().severe("Exception: " + stackTraceToString(exception));
              }
            }
          }

          /** Log step parameters. */
          private void logStepParameters() {

            final Set<Parameter> parameters = context.getCurrentStep().getParameters();

            if (parameters.isEmpty()) {
              getLogger().fine("Step has no parameter");
            } else {
              for (Parameter p : parameters) {
                getLogger().fine("Step parameter: " + p.getName() + "=" + p.getValue());
              }
            }
          }
        };

    // Set the progress of the task to 0%
    this.status.setProgress(0);

    // Start the time watch
    this.status.durationStart();

    try {

      // Create thread, reuse the thread group name as thread name
      final Thread thread = new Thread(threadGroup, r, threadGroupName);

      // Start thread
      thread.start();

      // Wait the end of the thread
      thread.join();

    } catch (InterruptedException e) {
      getLogger()
          .severe(
              e.getMessage() == null
                  ? "Interruption of the thread " + threadGroupName
                  : e.getMessage());

      // Inform the step token manager of the failed output data
      TokenManagerRegistry.getInstance()
          .getTokenManager(this.context.getStep())
          .addFailedOutputData(this.context);

    } finally {

      if (logger != null) {

        Handler[] handlers = logger.getHandlers();

        // Check if an handler has been set
        if (handlers != null && handlers.length > 0) {

          // Get the first handler
          Handler handler = handlers[0];

          // Close handler
          handler.close();

          // Remove logger from EoulsanLogger registry
          EoulsanLogger.removeThreadGroupLogger(threadGroup);

          // Remove handler
          logger.removeHandler(handler);
        }
      }
    }

    if (this.result == null) {

      this.result =
          this.status.createTaskResult(
              new EoulsanException(
                  "The step "
                      + this.context.getStep().getId()
                      + " has not generate a result object"));
    }

    // Send the tokens
    sendTokens();

    return (TaskResultImpl) this.result;
  }

  /** Send token. */
  private void sendTokens() {

    // Check if result has been created
    checkState(this.result != null, "Cannot send tokens of a null result task");

    // Check if tokens has been already sent
    checkState(!this.isTokensSent, "Cannot send tokens twice");

    this.isTokensSent = true;

    // Do not send data if the task has not been successful
    if (!this.result.isSuccess()) {
      return;
    }

    // For all output ports
    for (String portName : this.context.getCurrentStep().getOutputPorts().getPortNames()) {

      // Get data required for token creation
      final StepOutputPort port = this.context.getStep().getWorkflowOutputPorts().getPort(portName);
      final Data data = this.context.getOutputData(port);

      // Send the token
      WorkflowEventBus.getInstance().postToken(port, data);
    }

    // Change the state of the step to PARTIALY_DONE if it the end first task of
    // the step
    final AbstractStep step = this.context.getWorkflowStep();
    if (step.getState() == WORKING) {
      WorkflowEventBus.getInstance().postStepStateChange(step, PARTIALLY_DONE);
    }
  }

  /**
   * Create default context name.
   *
   * @return a string with the default context name
   */
  private String createDefaultContextName() {

    final Set<String> namedData = new LinkedHashSet<>();
    final Set<String> defaultNamedData = new LinkedHashSet<>();
    final Set<String> fileNames = new LinkedHashSet<>();
    final Set<String> otherDataNames = new LinkedHashSet<>();

    // Collect the names of the data and files names
    for (String inputPortName : this.context.getCurrentStep().getInputPorts().getPortNames()) {

      final AbstractData data =
          ((UnmodifiableData) this.context.getInputData(inputPortName)).getData();

      if (!data.isList()) {

        if (!data.isDefaultName()) {
          namedData.add(data.getName());
        } else if (data.isNameSet()) {
          defaultNamedData.add(data.getName());
        } else {

          for (DataFile file : WorkflowDataUtils.getDataFiles(data)) {
            fileNames.add(file.getName());
          }
        }

      } else {
        otherDataNames.add(data.getName());
      }
    }

    // Choose the name of the context
    if (!namedData.isEmpty()) {
      return Joiner.on('-').join(namedData);
    } else if (!defaultNamedData.isEmpty()) {
      return Joiner.on('-').join(defaultNamedData);
    } else if (!fileNames.isEmpty()) {
      return Joiner.on('-').join(fileNames);
    } else {
      return Joiner.on('-').join(otherDataNames);
    }
  }

  /**
   * Create the logger for a step.
   *
   * @param threadGroupName the name of the thread group
   * @return a Logger instance
   */
  private Logger createStepLogger(final String threadGroupName) {

    // Define the log file for the step
    final DataFile logDir = this.context.getStep().getAbstractWorkflow().getTaskDirectory();
    final DataFile logFile =
        new DataFile(logDir, this.context.getTaskFilePrefix() + TASK_LOG_EXTENSION);

    OutputStream logOut;
    try {

      logOut = logFile.create();

    } catch (IOException e) {
      return null;
    }

    // Get the logger for the step
    final Logger logger = Logger.getLogger(threadGroupName);

    final Handler handler = new StreamHandler(logOut, Globals.LOG_FORMATTER);

    // Disable parent Handler
    logger.setUseParentHandlers(false);

    // Set log level to all before setting the real log level
    logger.setLevel(Level.ALL);

    // Set the Handler
    logger.addHandler(handler);

    // Get the Log level on command line
    String logLevel = Main.getInstance().getLogLevelArgument();
    if (logLevel == null) {
      logLevel = Globals.LOG_LEVEL.getName();
    }

    // Set log level
    handler.setLevel(Level.parse(logLevel.toUpperCase(Globals.DEFAULT_LOCALE)));

    return logger;
  }

  //
  // Static methods
  //

  /**
   * Create a step result for an exception.
   *
   * @param taskContext task context
   * @param exception exception
   * @return a new TaskResult object
   */
  public static TaskResultImpl createStepResult(
      final TaskContextImpl taskContext, final Throwable exception) {

    return createStepResult(
        taskContext, exception, exception != null ? exception.getMessage() : null);
  }

  /**
   * Create a step result for an exception.
   *
   * @param taskContext task context
   * @param exception exception
   * @param errorMessage error message
   * @return a new TaskResult object
   */
  public static TaskResultImpl createStepResult(
      final TaskContextImpl taskContext, final Throwable exception, final String errorMessage) {

    final TaskRunner runner = new TaskRunner(taskContext);

    // Start the time watch
    runner.status.durationStart();

    // Create the result object
    return (TaskResultImpl) runner.status.createTaskResult(exception, errorMessage);
  }

  /**
   * Send tokens for a serialized task result.
   *
   * @param taskContext task context
   * @param taskResult task result
   */
  public static void sendTokens(
      final TaskContextImpl taskContext, final TaskResultImpl taskResult) {

    new TaskRunner(taskContext, taskResult).sendTokens();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   *
   * @param taskContext task context to execute
   */
  public TaskRunner(final TaskContextImpl taskContext) {

    this(taskContext, (StepStatus) null);
  }

  /**
   * Constructor.
   *
   * @param taskContext task context to execute
   * @param stepStatus step status
   */
  public TaskRunner(final TaskContextImpl taskContext, final StepStatus stepStatus) {

    requireNonNull(taskContext, "taskContext cannot be null");

    this.context = taskContext;
    this.module = StepInstances.getInstance().getModule(taskContext.getCurrentStep());

    this.status = new TaskStatusImpl(taskContext, stepStatus);

    // Set the task context name for the status
    this.context.setContextName(createDefaultContextName());
  }

  /**
   * Private constructor used to send token for serialized result.
   *
   * @param taskContext task context
   * @param taskResult task result
   */
  private TaskRunner(final TaskContextImpl taskContext, final TaskResultImpl taskResult) {

    requireNonNull(taskContext, "taskContext cannot be null");
    requireNonNull(taskResult, "taskResult cannot be null");

    // Check if the task result has been created for the task context
    checkArgument(taskContext.getId() == taskResult.getContext().getId(), "");

    this.context = taskContext;
    this.result = taskResult;

    // Step object and status are not necessary in this case
    this.module = null;
    this.status = null;
  }
}
