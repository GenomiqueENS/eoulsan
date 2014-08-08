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

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Main;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * This class allow to run a step context.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class WorkflowStepContextRunner {

  private final WorkflowStepContext context;
  private final Step step;
  private final WorkflowStepContextStatus status;
  private StepResult result;

  //
  // Getter
  //

  /**
   * Get the context result.
   * @return a WorkflowStepContextResult object
   */
  public WorkflowStepContextResult getResult() {

    checkState(this.result != null, "The context has not been run");

    return (WorkflowStepContextResult) this.result;
  }

  //
  // Execute methods
  //

  /**
   * Run the context.
   * @return a context result object
   */
  public WorkflowStepContextResult run() {

    // check if input files exists
    // checkExistingInputFiles();

    // Thread group name
    final String threadGroupName =
        "StepContextExecutor_"
            + this.context.getStep().getId() + "_#" + this.context.getId();

    // Define thread group
    final ThreadGroup threadGroup = new ThreadGroup(threadGroupName);

    // Create Log handler and register it
    final Logger logger =
        step.isCreateLogFiles() ? createStepLogger(this.context.getStep(),
            threadGroupName) : null;

    // Register the logger
    if (logger != null)
      EoulsanLogger.registerThreadGroupLogger(threadGroup, logger);

    // We use here a thread to execute the step
    // This allow to save log of step in distinct files
    final Runnable r = new Runnable() {

      @Override
      public void run() {

        try {
          result = step.execute(context, status);
        } catch (Throwable t) {

          // Handle exception not catch by step code
          result = status.createStepResult(t);
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
      EoulsanLogger.getLogger().severe(e.getMessage());
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

    return (WorkflowStepContextResult) this.result;
  }

  /**
   * Send token.
   */
  private void sendTokens() {

    if (this.result == null)
      throw new IllegalStateException(
          "Cannot send tokens of a null result step");

    // Do not send data if the step has not been successful
    if (!this.result.isSuccess()) {
      return;
    }

    // For all output ports
    for (String portName : context.getCurrentStep().getOutputPorts()
        .getPortNames()) {

      // Get data required for token creation
      final WorkflowOutputPort port =
          context.getStep().getWorkflowOutputPorts().getPort(portName);
      final Data data = context.getOutputData(port);

      // Create symbolic links
      // TODO enable symlink creation
      // createSymlinksInOutputDirectory(data);

      // Send the token
      context.getStep().sendToken(new Token(port, data));
    }
  }

  /**
   * Create default context name.
   * @return a string with the default context name
   */
  private String createDefaultContextName() {

    final List<String> namedData = Lists.newArrayList();
    final List<String> fileNames = Lists.newArrayList();
    final List<String> otherDataNames = Lists.newArrayList();

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
    } else
      return Joiner.on('-').join(otherDataNames);
  }

  /**
   * Get the input data files.
   * @return a list a DataFile
   */
  private List<DataFile> getInputDataFile() {

    final List<DataFile> result = Lists.newArrayList();

    for (String inputPortName : this.context.getCurrentStep().getInputPorts()
        .getPortNames()) {

      result.addAll(DataUtils.getDataFiles(this.context
          .getInputData(inputPortName)));
    }

    return result;
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
        this.context.getStep().getAbstractWorkflow().getLogDir();
    final DataFile logFile =
        new DataFile(logDir, step.getId()
            + "_context#" + this.context.getId() + ".log");
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

    // Set log level
    handler.setLevel(Level.parse(Main.getInstance().getLogLevelArgument()
        .toUpperCase()));

    return logger;
  }

  /**
   * Check if the input files exists.
   * @throws EoulsanException if the input files does not exists
   */
  private void checkExistingInputFiles() throws EoulsanException {

    for (DataFile file : getInputDataFile()) {

      if (!file.exists()) {
        throw new EoulsanException("A file required by the "
            + context.getCurrentStep().getId() + " step does not exists ("
            + file + ")");
      }
    }
  }

  /**
   * Check if the the output data exists.
   * @param outData data to test
   */
  private void createSymlinksInOutputDirectory(final Data outData) {

    Preconditions.checkNotNull(outData, "outData argument cannot be null");

    final DataFile outputDir =
        this.context.getStep().getAbstractWorkflow().getOutputDir();

    for (Data data : outData.getListElements()) {
      for (DataFile file : DataUtils.getDataFiles(data)) {

        final DataFile link = new DataFile(outputDir, file.getName());

        try {
          // Remove existing file/symlink
          if (link.exists())
            link.delete();

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
  // Constructor
  //

  /**
   * Constructor.
   * @param stepContext stepContext to execute
   */
  public WorkflowStepContextRunner(final WorkflowStepContext stepContext) {

    this(stepContext, null);
  }

  /**
   * Constructor.
   * @param stepContext stepContext to execute
   * @param stepStatus step status
   */
  public WorkflowStepContextRunner(final WorkflowStepContext stepContext,
      final WorkflowStepStatus stepStatus) {

    Preconditions.checkNotNull(stepContext, "stepContext cannot be null");

    this.context = stepContext;
    this.step =
        StepInstances.getInstance().getStep(stepContext.getCurrentStep());

    this.status = new WorkflowStepContextStatus(stepContext, stepStatus);

    // Set the stepContext name for the status
    this.context.setContextName(createDefaultContextName());
  }

}
