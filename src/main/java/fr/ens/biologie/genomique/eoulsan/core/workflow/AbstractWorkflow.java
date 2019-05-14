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

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.ABORTED;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.FAILED;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.PARTIALLY_DONE;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.READY;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.WAITING;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.WORKING;
import static fr.ens.biologie.genomique.eoulsan.util.StringUtils.stackTraceToString;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;

import fr.ens.biologie.genomique.eoulsan.Common;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.Settings;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.Step.StepState;
import fr.ens.biologie.genomique.eoulsan.core.Step.StepType;
import fr.ens.biologie.genomique.eoulsan.core.Workflow;
import fr.ens.biologie.genomique.eoulsan.core.schedulers.TaskSchedulerFactory;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.io.DesignWriter;
import fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan2DesignWriter;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;
import fr.ens.biologie.genomique.eoulsan.util.process.DockerManager;

/**
 * This class define a Workflow. This class must be extended by a class to be
 * able to work with a specific workflow file format.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractWorkflow implements Workflow {

  /** Serialization version UID. */
  private static final long serialVersionUID = 4865597995432347155L;

  private static final String DESIGN_COPY_FILENAME = "design.txt";
  protected static final String WORKFLOW_COPY_FILENAME = "workflow.xml";
  private static final String WORKFLOW_GRAPHVIZ_FILENAME = "workflow.dot";
  private static final String WORKFLOW_IMAGE_FILENAME = "workflow.png";

  private final DataFile localWorkingDir;
  private final DataFile hadoopWorkingDir;
  private final DataFile outputDir;
  private final DataFile jobDir;
  private final DataFile taskDir;
  private final DataFile dataDir;
  private final DataFile tmpDir;

  private final Design design;
  private final WorkflowContext workflowContext;
  private final Set<String> stepIds = new HashSet<>();
  private final Map<AbstractStep, StepState> steps = new HashMap<>();
  private final Multimap<StepState, AbstractStep> states =
      ArrayListMultimap.create();
  private final SerializableStopwatch stopwatch = new SerializableStopwatch();

  private AbstractStep rootStep;
  private AbstractStep designStep;
  private AbstractStep checkerStep;
  private AbstractStep firstStep;

  private final Set<DataFile> deleteOnExitFiles = new HashSet<>();

  private volatile boolean shutdownNow;

  //
  // Getters
  //

  /**
   * Get the local working directory.
   * @return Returns the local working directory
   */
  DataFile getLocalWorkingDirectory() {
    return this.localWorkingDir;
  }

  /**
   * Get the local working directory.
   * @return Returns the local working directory
   */
  DataFile getHadoopWorkingDirectory() {
    return this.hadoopWorkingDir;
  }

  /**
   * Get the output directory.
   * @return Returns the output directory
   */
  DataFile getOutputDirectory() {
    return this.outputDir;
  }

  /**
   * Get the job directory.
   * @return Returns the log directory
   */
  DataFile getJobDirectory() {
    return this.jobDir;
  }

  /**
   * Get the task directory.
   * @return Returns the task directory
   */
  DataFile getTaskDirectory() {
    return this.taskDir;
  }

  @Override
  public Design getDesign() {

    return this.design;
  }

  @Override
  public Set<Step> getSteps() {

    final Set<Step> result = new HashSet<>(this.steps.keySet());

    return Collections.unmodifiableSet(result);
  }

  @Override
  public Step getRootStep() {

    return this.rootStep;
  }

  @Override
  public Step getDesignStep() {

    return this.designStep;
  }

  @Override
  public Step getFirstStep() {

    return this.firstStep;
  }

  /**
   * Get checker step.
   * @return the checker step
   */
  protected Step getCheckerStep() {

    return this.checkerStep;
  }

  /**
   * Get the real Context object. This method is useful to redefine context
   * values like base directory.
   * @return The Context object
   */
  public WorkflowContext getWorkflowContext() {

    return this.workflowContext;
  }

  //
  // Setters
  //

  /**
   * Register a step of the workflow.
   * @param step step to register
   */
  protected void register(final AbstractStep step) {

    requireNonNull(step, "step cannot be null");

    if (step.getWorkflow() != this) {
      throw new IllegalStateException(
          "step cannot be part of more than one workflow");
    }

    if (this.stepIds.contains(step.getId())) {
      throw new IllegalStateException(
          "2 step cannot had the same id: " + step.getId());
    }

    // Register root step
    if (step.getType() == StepType.ROOT_STEP) {

      if (this.rootStep != null && step != this.rootStep) {
        throw new IllegalStateException(
            "Cannot add 2 root steps to the workflow");
      }
      this.rootStep = step;
    }

    // Register design step
    if (step.getType() == StepType.DESIGN_STEP) {

      if (this.designStep != null && step != this.designStep) {
        throw new IllegalStateException(
            "Cannot add 2 design steps to the workflow");
      }
      this.designStep = step;
    }

    // Register checker step
    if (step.getType() == StepType.CHECKER_STEP) {

      if (this.checkerStep != null && step != this.checkerStep) {
        throw new IllegalStateException(
            "Cannot add 2 checkers steps to the workflow");
      }
      this.checkerStep = step;
    }

    // Register first step
    if (step.getType() == StepType.FIRST_STEP) {

      if (this.firstStep != null && step != this.firstStep) {
        throw new IllegalStateException(
            "Cannot add 2 first steps to the workflow");
      }
      this.firstStep = step;
    }

    synchronized (this) {
      this.stepIds.add(step.getId());
      this.steps.put(step, step.getState());
      this.states.put(step.getState(), step);
    }
  }

  /**
   * Listen StepState events. Update the status of a step. This method is used
   * by steps to inform the workflow object that the status of the step has been
   * changed.
   * @param event the event to handle
   */
  @Subscribe
  public void stepStateEvent(final StepStateEvent event) {

    if (event == null) {
      return;
    }

    final AbstractStep step = event.getStep();
    final StepState newState = event.getState();

    if (step.getWorkflow() != this) {
      throw new IllegalStateException("step is not part of the workflow");
    }

    synchronized (this) {

      StepState oldState = this.steps.get(step);

      // Test if the state has changed
      if (oldState == newState) {
        return;
      }

      this.states.remove(oldState, step);
      this.states.put(newState, step);
      this.steps.put(step, newState);
    }
  }

  @Override
  public void deleteOnExit(final DataFile file) {

    requireNonNull(file, "file argument is null");
    this.deleteOnExitFiles.add(file);
  }

  //
  // Check methods
  //

  /**
   * Check if the output file of the workflow already exists.
   * @throws EoulsanException if output files of the workflow already exists
   */
  private void checkExistingOutputFiles() throws EoulsanException {

    // For each step
    for (AbstractStep step : this.steps.keySet()) {

      // that is a standard step that is not skip
      if (step.getType() == StepType.STANDARD_STEP && !step.isSkip()) {

        // and for each port
        for (StepOutputPort port : step.getWorkflowOutputPorts()) {

          // Check if files that can generate the port already exists
          List<DataFile> files = port.getExistingOutputFiles();
          if (!files.isEmpty()) {

            throw new EoulsanException("For the step "
                + step.getId() + " data generated by the port " + port.getName()
                + " already exists: " + files.get(0));
          }
        }
      }
    }
  }

  /**
   * Check if the input file of the workflow already exists.
   * @throws EoulsanException if input files of the workflow already exists
   */
  private void checkExistingInputFiles() throws EoulsanException {

    // For each step
    for (AbstractStep step : this.steps.keySet()) {

      // that is a standard step that is not skip
      if (step.getType() == StepType.STANDARD_STEP && !step.isSkip()) {

        // and for each port
        for (StepInputPort port : step.getWorkflowInputPorts()) {

          // Get the link
          final StepOutputPort link = port.getLink();

          // If the step that generate the data is skip
          if (link.getStep().getType() == StepType.STANDARD_STEP
              && link.getStep().isSkip()) {

            // Check if files that can generate the port already exists
            List<DataFile> files = link.getExistingOutputFiles();
            if (files.isEmpty()) {

              throw new EoulsanException("For the step \""
                  + step.getId() + "\" data needed by the port \""
                  + port.getName()
                  + "\" not exists (this data is generated by the port \""
                  + link.getName() + "\" of the step \""
                  + link.getStep().getId() + "\")");
            }
          }
        }
      }
    }
  }

  /**
   * Skip the generators that are only required by skipped steps.
   */
  private void skipGeneratorsIfNotNeeded() {

    for (AbstractStep step : this.steps.keySet()) {

      // Search for generator steps
      if (step.getType() == StepType.GENERATOR_STEP) {

        boolean allStepSkipped = true;

        // Check if all linked step are skipped
        for (StepOutputPort outputPort : step.getWorkflowOutputPorts()) {

          if (!outputPort.isAllLinksToSkippedSteps()) {
            allStepSkipped = false;
            break;
          }
        }

        // If all linked steps are skipped, skip the generator
        if (allStepSkipped) {
          step.setSkipped(true);
        }
      }
    }
  }

  //
  // Workflow lifetime methods
  //

  /**
   * Execute the workflow.
   * @throws EoulsanException if an error occurs while executing the workflow
   */
  public void execute() throws EoulsanException {

    // Skip generators if needed
    skipGeneratorsIfNotNeeded();

    // check if output files does not exists
    checkExistingOutputFiles();

    // check if input files exists
    checkExistingInputFiles();

    // Save configuration files (design and workflow files)
    saveConfigurationFiles();

    // Initialize scheduler
    TaskSchedulerFactory.initialize();

    // Start scheduler
    TaskSchedulerFactory.getScheduler().start();

    // Get the token manager registry
    final TokenManagerRegistry registry = TokenManagerRegistry.getInstance();

    // Get event bus
    WorkflowEventBus eventBus = WorkflowEventBus.getInstance();

    // Set Steps to WAITING state
    for (AbstractStep step : this.steps.keySet()) {

      // Create Token manager of each step
      registry.getTokenManager(step);

      // Set state to WAITING
      eventBus.postStepStateChange(step, WAITING);
    }

    // Register Shutdown hook
    final Thread shutdownThread = createShutdownHookThread();
    Runtime.getRuntime().addShutdownHook(shutdownThread);

    // Start stop watch
    this.stopwatch.start();

    while (!getSortedStepsByState(READY, WAITING, PARTIALLY_DONE, WORKING)
        .isEmpty()) {

      try {
        // TODO 2000 must be a constant
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      if (this.shutdownNow) {

        final EoulsanException e = new EoulsanException(
            "Shutdown of the workflow required by the user (e.g. Ctrl-C)");

        emergencyStop(e, e.getMessage());

        break;
      }

      // Get the step that had failed
      final List<AbstractStep> failedSteps =
          getSortedStepsByState(StepState.FAILED);

      if (!failedSteps.isEmpty()) {

        StepResult firstResult = null;

        // Log error messages
        for (AbstractStep failedStep : failedSteps) {

          final StepResult result =
              TaskSchedulerFactory.getScheduler().getResult(failedStep);
          getLogger()
              .severe("Fail of the analysis: " + result.getErrorMessage());

          if (firstResult == null) {
            firstResult = result;
          }
        }

        // Get the exception that cause the fail of the analysis
        final Throwable exception;
        if (firstResult.getException() != null) {
          exception = firstResult.getException();
        } else {
          exception = new EoulsanException("Fail of the analysis.");
        }

        // Log exception stacktrace
        EoulsanLogger.logSevere("Cause of the fail of the analysis: "
            + stackTraceToString(exception));

        // Stop the analysis
        emergencyStop(exception, firstResult.getErrorMessage());

        break;
      }
    }

    // Remove shutdown hook
    EoulsanLogger.logInfo("Remove shutdownThread");
    Runtime.getRuntime().removeShutdownHook(shutdownThread);

    // Remove outputs to discard
    removeOutputsToDiscard();

    // Stop the workflow
    stop();

    logEndAnalysis(true);
  }

  /**
   * Stop the threads used by the workflow.
   */
  private void stop() {

    final TokenManagerRegistry registry = TokenManagerRegistry.getInstance();
    for (AbstractStep step : this.steps.keySet()) {

      // Stop Token manager dedicated thread
      final TokenManager tokenManager = registry.getTokenManager(step);

      if (tokenManager.isStarted()) {
        tokenManager.stop();
      }
    }

    // Stop scheduler
    TaskSchedulerFactory.getScheduler().stop();

    // Delete files on exit
    for (DataFile file : this.deleteOnExitFiles) {
      try {
        if (file.exists()) {
          file.delete(true);
        }
      } catch (IOException e) {
        EoulsanLogger
            .logWarning("Cannot remove file " + file + " on exit: " + file);
      }
    }

    // Close Docker connections
    try {
      DockerManager.closeConnections();
    } catch (IOException e) {
      EoulsanLogger.logWarning("Error while closing Docker connection");
    }
  }

  /**
   * Stop the workflow if the analysis failed.
   * @param exception exception
   * @param errorMessage error message
   */
  void emergencyStop(final Throwable exception, final String errorMessage) {

    // Get event bus
    WorkflowEventBus eventBus = WorkflowEventBus.getInstance();

    // Change working step state to aborted
    for (AbstractStep step : getSortedStepsByState(PARTIALLY_DONE, WORKING)) {
      eventBus.postStepStateChange(step, ABORTED);
    }

    // Stop the workflow
    stop();

    final TokenManagerRegistry registry = TokenManagerRegistry.getInstance();

    // Remove all outputs of failed steps
    for (AbstractStep step : getSortedStepsByState(FAILED)) {
      registry.getTokenManager(step).removeAllOutputs();
    }

    // Remove all outputs of aborted steps
    for (AbstractStep step : getSortedStepsByState(ABORTED)) {
      registry.getTokenManager(step).removeAllOutputs();
    }

    // Stop tasks
    EmergencyStopTasks.getInstance().stop();

    // Close Docker connections
    try {
      DockerManager.closeConnections();
    } catch (IOException e) {
      EoulsanLogger.logWarning("Error while closing Docker connection");
    }
    // Log end of analysis
    logEndAnalysis(false);

    // Exit Eoulsan
    Common.errorHalt(exception, errorMessage);
  }

  /**
   * Remove outputs to discard.
   */
  private void removeOutputsToDiscard() {

    final TokenManagerRegistry registry = TokenManagerRegistry.getInstance();
    for (AbstractStep step : this.steps.keySet()) {

      // Stop Token manager dedicated thread
      final TokenManager tokenManager = registry.getTokenManager(step);
      tokenManager.removeOutputsToDiscard();
    }
  }

  /**
   * Create a shutdown hook thread.
   * @return a new thread
   */
  public Thread createShutdownHookThread() {

    final AbstractWorkflow workflow = this;
    final Thread mainThread = Thread.currentThread();

    return new Thread() {

      @Override
      public void run() {

        workflow.shutdownNow = true;
        try {
          mainThread.join();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    };
  }

  //
  // Utility methods
  //

  /**
   * Save configuration files.
   * @throws EoulsanException if an error while writing files
   */
  protected void saveConfigurationFiles() throws EoulsanException {

    try {
      DataFile jobDir = getWorkflowContext().getJobDirectory();

      if (!jobDir.exists()) {
        jobDir.mkdirs();
      }

      // Save design file
      DesignWriter designWriter = new Eoulsan2DesignWriter(
          new DataFile(jobDir, DESIGN_COPY_FILENAME).create());
      designWriter.write(getDesign());

      // Save the workflow as a Graphviz and an image files
      Workflow2Graphviz graphviz = new Workflow2Graphviz(this,
          new DataFile(jobDir, WORKFLOW_GRAPHVIZ_FILENAME),
          new DataFile(jobDir, WORKFLOW_IMAGE_FILENAME));

      // Create an image or only the dot file
      if (!this.workflowContext.getSettings().isSaveWorkflowImage()
          || !graphviz.saveImageFile()) {
        graphviz.saveDotFile();
      }

    } catch (IOException e) {
      throw new EoulsanException(
          "Error while writing design file or Graphiviz workflow file: "
              + e.getMessage(),
          e);
    }
  }

  /**
   * Get the steps which has some step status. The step are ordered.
   * @param states step status to retrieve
   * @return a sorted list with the steps
   */
  private List<AbstractStep> getSortedStepsByState(final StepState... states) {

    requireNonNull(states, "states argument is null");

    final List<AbstractStep> result = new ArrayList<>();

    for (StepState state : states) {
      result.addAll(getSortedStepsByState(state));
    }

    // Sort steps
    sortListSteps(result);

    return result;
  }

  /**
   * Get the steps which has a step status. The step are ordered.
   * @param state step status to retrieve
   * @return a sorted list with the steps
   */
  private List<AbstractStep> getSortedStepsByState(final StepState state) {

    requireNonNull(state, "state argument is null");

    final List<AbstractStep> result;

    synchronized (this) {
      result = Lists.newArrayList(this.states.get(state));
    }

    sortListSteps(result);

    return result;
  }

  /**
   * Sort a list of step by priority and then by step number.
   * @param list the list of step to sort
   */
  private static void sortListSteps(final List<AbstractStep> list) {

    if (list == null) {
      return;
    }

    Collections.sort(list, new Comparator<AbstractStep>() {

      @Override
      public int compare(final AbstractStep a, final AbstractStep b) {

        int result = a.getType().getPriority() - b.getType().getPriority();

        if (result != 0) {
          return result;
        }

        return a.getNumber() - b.getNumber();
      }
    });

  }

  /**
   * Create a DataFile object from a path.
   * @param path the path
   * @return null if the path is null or a new DataFile object with the required
   *         path
   */
  private static DataFile newDataFile(final String path) {

    if (path == null) {
      return null;
    }

    return new DataFile(URI.create(path));
  }

  /**
   * Check directories needed by the workflow.
   * @throws EoulsanException if an error about the directories is found
   */
  public void checkDirectories() throws EoulsanException {

    requireNonNull(this.jobDir, "the job directory is null");
    requireNonNull(this.taskDir, "the task directory is null");
    requireNonNull(this.outputDir, "the output directory is null");
    requireNonNull(this.localWorkingDir, "the local working directory is null");

    // Get Eoulsan settings
    final Settings settings = EoulsanRuntime.getSettings();

    // Define the list of directories to create
    final List<DataFile> dirsToCheck =
        Lists.newArrayList(this.jobDir, this.outputDir, this.localWorkingDir,
            this.hadoopWorkingDir, this.taskDir);

    // If the temporary directory has not been defined by user
    if (!settings.isUserDefinedTempDirectory()) {

      // Set the temporary directory
      requireNonNull(this.tmpDir, "The temporary directory is null");
      settings.setTempDirectory(this.tmpDir.toFile().toString());
      dirsToCheck.add(this.tmpDir);
    }

    try {
      for (DataFile dir : dirsToCheck) {

        if (dir == null) {
          continue;
        }

        createDirectory(dir);
      }
    } catch (IOException e) {
      throw new EoulsanException(e);
    }

    // Check temporary directory
    checkTemporaryDirectory();
  }

  /**
   * Create an "eoulsan-data" directory if mapper indexes or genome description
   * storage has not been defined.
   * @throws EoulsanException if an error about the directories is found
   */
  public void createEoulsanDataDirectoryIfRequired() throws EoulsanException {

    try {

      // Get Eoulsan settings
      final Settings settings = EoulsanRuntime.getSettings();

      // Create genome mapper index storage if not defined
      if (settings.getGenomeMapperIndexStoragePath() == null) {

        DataFile mapperIndexDir = new DataFile(this.dataDir, "mapperindexes");
        settings.setGenomeMapperIndexStoragePath(mapperIndexDir.getSource());
        createDirectory(mapperIndexDir);
      }

      // Create genome description storage if not defined
      if (settings.getGenomeDescStoragePath() == null) {

        DataFile genomeDescriptionDir =
            new DataFile(this.dataDir, "genomedescriptions");
        settings.setGenomeDescStoragePath(genomeDescriptionDir.getSource());
        createDirectory(genomeDescriptionDir);
      }

      // Define singularity directory
      if (settings.getDockerSingularityStoragePath() == null) {

        DataFile singularityDir = new DataFile(this.dataDir, "singularity");
        settings.setDockerSingularityStoragePath(singularityDir.getSource());
      }

    } catch (IOException e) {
      throw new EoulsanException(e);
    }

  }

  /**
   * Create a directory.
   * @param directory the directory to create
   * @throws IOException if an error occurs while creating the directory
   * @throws EoulsanException
   */
  private static void createDirectory(DataFile directory) throws IOException {

    if (directory.exists() && !directory.getMetaData().isDir()) {
      throw new IOException("the directory is not a directory: " + directory);
    }

    if (!directory.exists()) {
      directory.mkdirs();
    }
  }

  /**
   * Check temporary directory.
   * @throws EoulsanException if the checking of the temporary directory fails
   */
  private void checkTemporaryDirectory() throws EoulsanException {

    final File tempDir = EoulsanRuntime.getSettings().getTempDirectoryFile();

    if (tempDir == null) {
      throw new EoulsanException("Temporary directory is null");
    }

    if ("".equals(tempDir.getAbsolutePath())) {
      throw new EoulsanException("Temporary directory is null");
    }

    if (!tempDir.exists()) {
      throw new EoulsanException(
          "Temporary directory does not exists: " + tempDir);
    }

    if (!tempDir.isDirectory()) {
      throw new EoulsanException(
          "Temporary directory is not a directory: " + tempDir);
    }

    if (!tempDir.canRead()) {
      throw new EoulsanException(
          "Temporary directory cannot be read: " + tempDir);
    }

    if (!tempDir.canWrite()) {
      throw new EoulsanException(
          "Temporary directory cannot be written: " + tempDir);
    }

    if (!tempDir.canExecute()) {
      throw new EoulsanException(
          "Temporary directory is not executable: " + tempDir);
    }
  }

  /**
   * Log the state and the time of the analysis.
   * @param success true if analysis was successful
   */
  private void logEndAnalysis(final boolean success) {

    this.stopwatch.stop();

    final String successString = success ? "Successful" : "Unsuccessful";

    // Log the end of the analysis
    getLogger().info(successString
        + " end of the analysis in "
        + StringUtils.toTimeHumanReadable(this.stopwatch.elapsed(MILLISECONDS))
        + " s.");

    // Inform observers of the end of the analysis
    WorkflowEventBus.getInstance()
        .postUIEvent(new UIWorkflowEvent(success,
            "(Job done in "
                + StringUtils.toTimeHumanReadable(
                    this.stopwatch.elapsed(MILLISECONDS))
                + " s.)"));

    // Send a mail

    final String mailSubject = "["
        + Globals.APP_NAME + "] " + successString + " end of your job "
        + this.workflowContext.getJobId() + " on "
        + this.workflowContext.getJobHost();

    final String mailMessage = "THIS IS AN AUTOMATED MESSAGE.\n\n"
        + successString + " end of your job " + this.workflowContext.getJobId()
        + " on " + this.workflowContext.getJobHost() + ".\nJob finished at "
        + new Date(System.currentTimeMillis()) + " in "
        + StringUtils.toTimeHumanReadable(this.stopwatch.elapsed(MILLISECONDS))
        + " s.\n\nOutput files and logs can be found in the following location:\n"
        + this.workflowContext.getOutputDirectory() + "\n\nThe "
        + Globals.APP_NAME + "team.";

    // Send mail
    Common.sendMail(mailSubject, mailMessage);
  }

  //
  // Serialization method
  //

  private void writeObject(ObjectOutputStream s) throws IOException {

    // Avoid change of state while serialization
    synchronized (this) {
      s.defaultWriteObject();
    }
  }

  //
  // Constructor
  //

  /**
   * Protected constructor.
   * @param executionArguments execution arguments
   * @param design design to use for the workflow
   * @throws EoulsanException if an error occurs while configuring the workflow
   */
  protected AbstractWorkflow(final ExecutorArguments executionArguments,
      final Design design) throws EoulsanException {

    requireNonNull(executionArguments, "Argument cannot be null");
    requireNonNull(design, "Design argument cannot be null");

    this.design = design;

    this.jobDir = newDataFile(executionArguments.getJobPathname());

    this.taskDir = newDataFile(executionArguments.getTaskPathname());

    this.tmpDir = newDataFile(executionArguments.getTemporaryPathname());

    this.localWorkingDir =
        newDataFile(executionArguments.getLocalWorkingPathname());

    this.hadoopWorkingDir =
        newDataFile(executionArguments.getHadoopWorkingPathname());

    this.outputDir = newDataFile(executionArguments.getOutputPathname());

    this.dataDir = newDataFile(executionArguments.getDataPathname());

    this.workflowContext = new WorkflowContext(executionArguments, this);

    // Register the object in the event bus
    WorkflowEventBus.getInstance().register(this);
  }
}
