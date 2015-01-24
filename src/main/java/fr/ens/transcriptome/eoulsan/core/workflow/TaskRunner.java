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
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_LOG_EXTENSION;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType.DESIGN_STEP;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Main;
import fr.ens.transcriptome.eoulsan.annotations.ReuseStepInstance;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.core.StepRegistry;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class allow to run a task context.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TaskRunner {

  private final TaskContext context;
  private final Step step;
  private final TaskStatus status;
  private volatile StepResult result;
  private boolean isTokensSent;
  private boolean forceStepInstanceReuse;

  //
  // Getter
  //

  /**
   * Get the context result.
   * @return a TaskResult object
   */
  public TaskResult getResult() {

    checkState(this.result != null, "The context has not been run");

    return (TaskResult) this.result;
  }

  //
  // Setter
  //

  /**
   * Force the TaskRunner to reuse the original step instance when execute the
   * task.
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
   * @return a task result object
   */
  public TaskResult run() {

    // Check if task has been already executed
    checkState(this.result == null, "task has been already executed");

    // Thread group name
    final String threadGroupName =
        "TaskRunner_"
            + this.context.getStep().getId() + "_#" + this.context.getId();

    // Define thread group
    final ThreadGroup threadGroup = new ThreadGroup(threadGroupName);

    // Create Log handler and register it
    final Logger logger =
        this.step.isCreateLogFiles() ? createStepLogger(this.context.getStep(),
            threadGroupName) : null;

    // Register the logger
    if (logger != null) {
      EoulsanLogger.registerThreadGroupLogger(threadGroup, logger);
    }

    // We use here a thread to execute the step
    // This allow to save log of step in distinct files
    final Runnable r = new Runnable() {

      @Override
      public void run() {

        getLogger().info("Start of task #" + TaskRunner.this.context.getId());

        final Step stepInstance;
        final StepType stepType =
            TaskRunner.this.context.getWorkflowStep().getType();
        final boolean reuseAnnot =
            TaskRunner.this.step.getClass().getAnnotation(
                ReuseStepInstance.class) != null;

        final String stepDescLog =
            String
                .format("step (id:  %s, name: %s) for task #%d",
                    TaskRunner.this.context.getWorkflowStep().getId(),
                    TaskRunner.this.step.getName(),
                    TaskRunner.this.context.getId());

        try {

          // If step is a standard step and reuse of step instance is not
          // required by step
          // Create a new instance of the step for the task
          if (stepType == StepType.STANDARD_STEP
              && !reuseAnnot && !TaskRunner.this.forceStepInstanceReuse) {

            // Create the new instance of the step
            getLogger().fine("Create new instance of " + stepDescLog);

            final String stepName = TaskRunner.this.step.getName();
            final Version stepVersion = TaskRunner.this.step.getVersion();

            stepInstance =
                StepRegistry.getInstance().loadStep(stepName,
                    stepVersion.toString());

            // Log step parameters
            logStepParameters();

            // Configure the new step instance
            getLogger().fine("Configure step instance");
            stepInstance.configure(new WorkflowStepConfigurationContext(
                TaskRunner.this.context.getStep()), TaskRunner.this.context
                .getCurrentStep().getParameters());

          } else {

            // Use the original step instance for the task
            getLogger().fine("Reuse original instance of " + stepDescLog);
            stepInstance = TaskRunner.this.step;

            // Log step parameters
            logStepParameters();
          }

          // Execute task
          getLogger().info("Execute task");
          TaskRunner.this.result =
              stepInstance.execute(TaskRunner.this.context,
                  TaskRunner.this.status);

        } catch (Throwable t) {

          // Handle exception not catch by step code
          TaskRunner.this.result = TaskRunner.this.status.createStepResult(t);
        }

        getLogger().info("End of task #" + TaskRunner.this.context.getId());
      }

      /**
       * Log step parameters.
       */
      private void logStepParameters() {

        final Set<Parameter> parameters =
            context.getCurrentStep().getParameters();

        if (parameters.isEmpty()) {
          getLogger().fine("Step has no parameter");
        } else {
          for (Parameter p : parameters) {
            getLogger().fine(
                "Step parameter: " + p.getName() + "=" + p.getValue());
          }
        }
      }

    };

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
      getLogger().severe(e.getMessage());
    } finally {

      if (logger != null) {

        Handler handler = logger.getHandlers()[0];

        // Close handler
        handler.close();

        // Remove logger from EoulsanLogger registry
        EoulsanLogger.removeThreadGroupLogger(threadGroup);

        // Remove handler
        logger.removeHandler(handler);
      }
    }

    if (this.result == null) {

      this.result =
          this.status.createStepResult(new EoulsanException("The step "
              + this.context.getStep().getId()
              + " has not generate a result object"));
    }

    // Send the tokens
    sendTokens();

    return (TaskResult) this.result;
  }

  /**
   * Send token.
   */
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
    for (String portName : this.context.getCurrentStep().getOutputPorts()
        .getPortNames()) {

      // Get data required for token creation
      final WorkflowOutputPort port =
          this.context.getStep().getWorkflowOutputPorts().getPort(portName);
      final Data data = this.context.getOutputData(port);

      // Create symbolic links
      createSymlinksInOutputDirectory(data);

      // Send the token
      this.context.getStep().sendToken(new Token(port, data));
    }
  }

  /**
   * Create default context name.
   * @return a string with the default context name
   */
  private String createDefaultContextName() {

    final List<String> namedData = new ArrayList<>();
    final List<String> fileNames = new ArrayList<>();
    final List<String> otherDataNames = new ArrayList<>();

    // Collect the names of the data and files names
    for (String inputPortName : this.context.getCurrentStep().getInputPorts()
        .getPortNames()) {

      final AbstractData data =
          ((UnmodifiableData) this.context.getInputData(inputPortName))
              .getData();

      if (!data.isList()) {

        if (!data.isDefaultName()) {
          namedData.add(data.getName());
        } else {

          for (DataFile file : DataUtils.getDataFiles(data)) {
            fileNames.add(file.getName());
          }
        }

      } else {
        otherDataNames.add(data.getName());
      }
    }

    // Choose the name of the context
    if (namedData.size() > 0) {
      return Joiner.on('-').join(namedData);
    } else if (fileNames.size() > 0) {
      return Joiner.on('-').join(fileNames);
    } else {
      return Joiner.on('-').join(otherDataNames);
    }
  }

  /**
   * Create the logger for a step.
   * @param step the step
   * @param threadGroupName the name of the thread group
   * @return a Logger instance
   */
  private Logger createStepLogger(final AbstractWorkflowStep step,
      final String threadGroupName) {

    // Define the log file for the step
    final DataFile logDir =
        this.context.getStep().getAbstractWorkflow().getTaskDirectory();
    final DataFile logFile =
        new DataFile(logDir, createTaskPrefixFile(this.context)
            + TASK_LOG_EXTENSION);

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
    handler.setLevel(Level.parse(logLevel.toUpperCase()));

    return logger;
  }

  /**
   * Check if the the output data exists.
   * @param outData data to test
   */
  private void createSymlinksInOutputDirectory(final Data outData) {

    Preconditions.checkNotNull(outData, "outData argument cannot be null");

    final DataFile outputDir =
        this.context.getStep().getAbstractWorkflow().getOutputDirectory();

    final DataFile workingDir = this.context.getStep().getStepOutputDirectory();

    // Nothing to to if the step working directory is the output directory
    if (this.context.getStep().getType() == DESIGN_STEP
        || outputDir.equals(workingDir)) {
      return;
    }

    for (Data data : outData.getListElements()) {
      for (DataFile file : DataUtils.getDataFiles(data)) {

        final DataFile link = new DataFile(outputDir, file.getName());

        try {

          // TODO Use the Java 7 api to check if existing link is really a link
          // Remove existing file/symlink
          if (link.exists()) {
            link.delete();
          }

          // Create symbolic link
          file.symlink(link);
        } catch (IOException e) {
          EoulsanLogger.getLogger().severe(
              "Cannot create symbolic link: " + link);
        }
      }
    }
  }

  //
  // Static methods
  //

  /**
   * Create the prefix of a related task file.
   * @param context the context
   * @return a string with the prefix of the task file
   */
  public static String createTaskPrefixFile(final TaskContext context) {

    if (context == null) {
      return null;
    }

    return context.getStep().getId() + "_context#" + context.getId();
  }

  /**
   * Create a step result for an exception.
   * @param taskContext task context
   * @param exception exception
   * @return a new TaskResult object
   */
  public static TaskResult createStepResult(final TaskContext taskContext,
      final Throwable exception) {

    return createStepResult(taskContext, exception, null);
  }

  /**
   * Create a step result for an exception.
   * @param taskContext task context
   * @param exception exception
   * @param errorMessage error message
   * @return a new TaskResult object
   */
  public static TaskResult createStepResult(final TaskContext taskContext,
      final Throwable exception, final String errorMessage) {

    final TaskRunner runner = new TaskRunner(taskContext);

    // Start the time watch
    runner.status.durationStart();

    // Create the result object
    return (TaskResult) runner.status.createStepResult(exception, errorMessage);
  }

  /**
   * Send tokens for a serialized task result.
   * @param taskContext task context
   * @param taskResult task result
   */
  public static void sendTokens(final TaskContext taskContext,
      final TaskResult taskResult) {

    new TaskRunner(taskContext, taskResult).sendTokens();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param taskContext task context to execute
   */
  public TaskRunner(final TaskContext taskContext) {

    this(taskContext, (WorkflowStepStatus) null);
  }

  /**
   * Constructor.
   * @param taskContext task context to execute
   * @param stepStatus step status
   */
  public TaskRunner(final TaskContext taskContext,
      final WorkflowStepStatus stepStatus) {

    checkNotNull(taskContext, "taskContext cannot be null");

    this.context = taskContext;
    this.step =
        StepInstances.getInstance().getStep(taskContext.getCurrentStep());

    this.status = new TaskStatus(taskContext, stepStatus);

    // Set the task context name for the status
    this.context.setContextName(createDefaultContextName());
  }

  /**
   * Private constructor used to send token for serialized result.
   * @param taskContext task context
   * @param taskResult task result
   */
  private TaskRunner(final TaskContext taskContext, final TaskResult taskResult) {

    checkNotNull(taskContext, "taskContext cannot be null");
    checkNotNull(taskResult, "taskResult cannot be null");

    // Check if the task result has been created for the task context
    checkArgument(taskContext.getId() == taskResult.getContext().getId(), "");

    this.context = taskContext;
    this.result = taskResult;

    // Step object and status are not necessary in this case
    this.step = null;
    this.status = null;
  }

}
