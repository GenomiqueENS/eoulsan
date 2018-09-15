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
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.Globals.STEP_RESULT_EXTENSION;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.ABORTED;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.DONE;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.READY;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.WORKING;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepType.DESIGN_STEP;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepType.GENERATOR_STEP;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepType.STANDARD_STEP;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;

import fr.ens.biologie.genomique.eoulsan.AbstractEoulsanRuntime.EoulsanExecMode;
import fr.ens.biologie.genomique.eoulsan.Common;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntimeException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.core.FileNaming;
import fr.ens.biologie.genomique.eoulsan.core.InputPort;
import fr.ens.biologie.genomique.eoulsan.core.Naming;
import fr.ens.biologie.genomique.eoulsan.core.OutputPort;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.Step.StepState;
import fr.ens.biologie.genomique.eoulsan.core.Step.StepType;
import fr.ens.biologie.genomique.eoulsan.core.schedulers.TaskScheduler;
import fr.ens.biologie.genomique.eoulsan.core.schedulers.TaskSchedulerFactory;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.protocols.HDFSPathDataProtocol;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.Sample;

/**
 * This class define a token manager for a step.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TokenManager implements Runnable {

  private static final int CHECKING_DELAY_MS = 1000;

  private final AbstractStep step;
  private final TaskScheduler scheduler;
  private final StepInputPorts inputPorts;
  private final StepOutputPorts outputPorts;

  private final Set<Integer> receivedTokens = new HashSet<>();
  private final HashMultiset<StepInputPort> receivedPortTokens =
      HashMultiset.create();
  private final HashMultiset<StepInputPort> expectedPortTokens =
      HashMultiset.create();
  private int contextCount;
  private final Multimap<InputPort, Data> inputTokens =
      ArrayListMultimap.create();
  private final Multimap<OutputPort, Data> outputTokens =
      ArrayListMultimap.create();
  private final Set<InputPort> closedPorts = new HashSet<>();

  private final Set<ImmutableMap<InputPort, Data>> cartesianProductsUsed =
      new HashSet<>();

  private final Set<Data> failedOutputDataToRemove = new HashSet<>();

  private volatile boolean endOfStep;
  private boolean isStarted;

  //
  // Getters
  //

  /**
   * Test if there is no token to be received by the token manager.
   * @return true if no token to be received by the token manager
   */
  public boolean isNoTokenToReceive() {
    return this.inputPorts.size() == this.closedPorts.size();
  }

  /**
   * Get the number of context created by the token manager.
   * @return the number of context created by the token manager
   */
  public int getContextCount() {
    return this.inputPorts.size() == 0 ? 1 : this.cartesianProductsUsed.size();
  }

  /**
   * Test if this is the end of the step.
   * @return true if this is the end of the step
   */
  public boolean isEndOfStep() {
    return this.endOfStep;
  }

  //
  // Port checking methods
  //

  /**
   * Check if all the ports has received some data.
   * @return true if all the ports has received some data
   */
  private boolean checkIfAllPortsHasReceivedSomeData() {

    for (StepInputPort port : this.inputPorts) {
      if (this.inputTokens.get(port).isEmpty()) {
        return false;
      }
    }

    return true;
  }

  /**
   * Check if all the list ports are closed.
   * @return true if all the list ports are closed
   */
  private boolean checkIfAllListPortsAreClosed() {

    for (StepInputPort port : this.inputPorts) {
      if (!port.isList()) {
        break;
      }

      if (!this.closedPorts.contains(port)) {
        return false;
      }
    }

    return true;
  }

  //
  // Token handling methods
  //

  /**
   * Log the token that are send by the step.
   * @param outputPort the output port
   * @param token the token sent
   */
  private void logSendingToken(final StepOutputPort outputPort,
      final Token token) {

    Objects.requireNonNull(token);
    Objects.requireNonNull(outputPort);

    // Test if the token is an end token
    if (!token.isEndOfStepToken()) {

      // Get the data
      final Data data = token.getData();

      synchronized (this.outputTokens) {
        for (Data e : data.getListElements()) {
          this.outputTokens.put(outputPort, e);
        }
      }

      // Create compatibility link for result files
      if (EoulsanRuntime.getSettings()
          .getBooleanSetting("debug.compatibility.result.file.links")
          && this.step.getType() == StepType.STANDARD_STEP) {
        createCompatibilityLinkResultFiles(data);
      }

      // Create symbolic links in output directory
      createSymlinksInOutputDirectory(data);

      // Get the metadata storage
      final DataMetadataStorage metadataStorage = DataMetadataStorage
          .getInstance(this.step.getAbstractWorkflow().getOutputDirectory());

      // Store token metadata only if step is not skipped
      if (!this.step.isSkip()) {
        metadataStorage.saveMetaData(data);
      }
    }
  }

  /**
   * This method allow to create symbolic link on step result file with the same
   * name as in Eoulsan 1.x.
   * @param data the data
   */
  private void createCompatibilityLinkResultFiles(final Data data) {

    requireNonNull(data, "data argument cannot be null");

    for (Data e : data.getListElements()) {

      // Get the sample id from metadata
      final int sampleNumber = e.getMetadata().getSampleNumber();

      // Do nothing if sample id is not set in metadata
      if (sampleNumber == -1) {
        continue;
      }

      // For all data
      for (DataFile f : WorkflowDataUtils.getDataFiles(e)) {

        try {

          final DataFile parentDir = f.getParent();

          // Do something only if local file
          if (!parentDir.isLocalFile()) {
            continue;
          }

          // Parse the filename
          final FileNaming name = FileNaming.parse(f.getName());
          name.setSampleNumber(sampleNumber);

          // Create link name
          final DataFile link =
              new DataFile(parentDir, name.compatibilityFilename());

          // Create link only if not already exists
          if (!link.exists()) {
            f.symlink(link, true);
          }

        } catch (IOException exp) {
          getLogger().warning(
              "Error while creating compatibility link: " + exp.getMessage());
        }
      }
    }

  }

  /**
   * Create symbolic links in output directory.
   * @param data the data
   */
  private void createSymlinksInOutputDirectory(final Data data) {

    requireNonNull(data, "data argument cannot be null");

    final DataFile outputDir =
        this.step.getAbstractWorkflow().getOutputDirectory();
    final DataFile workingDir = this.step.getStepOutputDirectory();

    // Nothing to to if the step working directory is the output directory
    if (this.step.getType() == DESIGN_STEP || outputDir.equals(workingDir)) {
      return;
    }

    for (Data dataElement : data.getListElements()) {
      for (DataFile file : WorkflowDataUtils.getDataFiles(dataElement)) {

        final DataFile link = new DataFile(outputDir, file.getName());

        try {

          // Remove existing symlink
          if (link.exists(false)) {

            if (link.getMetaData().isSymbolicLink()) {
              link.delete();
            } else {
              throw new IOException();
            }
          }

          // Create symbolic link
          file.symlink(link, true);
        } catch (IOException e) {
          EoulsanLogger.getLogger()
              .severe("Cannot create symbolic link: " + link);
        }
      }
    }
  }

  @Subscribe
  public void tokenEvent(final Token token) {

    Objects.requireNonNull(token);

    final StepOutputPort tokenOrigin = token.getOrigin();
    final AbstractStep stepOrigin = tokenOrigin.getStep();
    final int currentStepNumber = this.step.getNumber();

    // Post the token if the token must be post to the current step
    for (StepInputPort linkInputPort : tokenOrigin.getLinks()) {

      // Check if the link is linked to the current step
      if (linkInputPort.getStep().getNumber() != currentStepNumber) {
        continue;
      }

      // Find the linked port(s)
      for (StepInputPort sip : this.inputPorts) {

        if (sip.getName().equals(linkInputPort.getName())) {
          postToken(linkInputPort, token);
        }
      }
    }

    // Log token sending by the step that created the token
    if (this.step.getNumber() == stepOrigin.getNumber()) {
      logSendingToken(tokenOrigin, token);
      return;
    }
  }

  /**
   * Post a token to the the token manager.
   * @param inputPort port where the token must be posted
   * @param token the token to post
   */
  private void postToken(final StepInputPort inputPort, final Token token) {

    Objects.requireNonNull(token);
    Objects.requireNonNull(inputPort);

    // Check origin step state
    final StepState originStepState = token.getOrigin().getStep().getState();
    checkState(originStepState.isWorkingState() || originStepState == DONE,
        "Invalid token step origin state: " + originStepState);

    // Check if token has already been processed
    checkState(!this.receivedTokens.contains(token.getId()),
        "Token has been already received: " + token.getId());

    // Check if the input is linked to the step
    checkState(this.inputPorts.contains(inputPort),
        "Unknown port: " + inputPort);

    // Check if the origin of the token and the input port are linked
    checkState(inputPort.getLink() == token.getOrigin(),
        "The input port ("
            + inputPort + ") and the output port (" + token.getOrigin()
            + ") are not linked:");

    // Check if the input port is closed
    checkState(!this.closedPorts.contains(inputPort),
        "The input port is closed for the step "
            + this.step.getId() + ": " + inputPort.getName());

    synchronized (this.receivedTokens) {
      this.receivedTokens.add(token.getId());
    }

    // Test if the token is an end token
    if (token.isEndOfStepToken()) {

      checkState(token.getTokenCount() > -1,
          "the number of expected token is not set");

      checkState(expectedPortTokens.count(inputPort) == 0,
          "the number of expected token has been already set");

      // Set the number of expected tokens
      synchronized (this.expectedPortTokens) {
        this.expectedPortTokens.setCount(inputPort, token.getTokenCount());
      }

    } else {

      // Count the number of tokens received for the input port
      synchronized (this.receivedPortTokens) {
        this.receivedPortTokens.add(inputPort);
      }

      // Register data to process
      final Data data = token.getData();

      // Synchronized this part to avoid the lost of some data when creating
      // Cartesian product
      synchronized (this.cartesianProductsUsed) {

        if (data.isList()) {
          for (Data e : data.getListElements()) {
            addData(inputPort, e);
          }
        } else {

          addData(inputPort, data);
        }
      }
    }

    // Close ports if all the expected tokens has been received
    if (this.step.isSkip()
        || (this.expectedPortTokens.contains(inputPort)
            && this.receivedPortTokens.count(
                inputPort) == this.expectedPortTokens.count(inputPort))) {

      // Check if input port is empty for non skipped steps
      checkState(
          !(!this.step.isSkip() && this.inputTokens.get(inputPort).isEmpty()),
          "No data receive for port on step "
              + this.step.getId() + ": " + inputPort.getName());

      // The input port must be closed
      this.closedPorts.add(inputPort);
    }

  }

  /**
   * Add data to the token manager.
   * @param inputPort inputPort port for the data
   * @param data the data
   */
  private void addData(final StepInputPort inputPort, final Data data) {

    if (!inputPort.isList()) {

      synchronized (this.inputTokens) {
        this.inputTokens.put(inputPort, data);
      }

    } else {

      // Get or create the data list
      final DataList dataList;

      final Collection<Data> inputData = this.inputTokens.get(inputPort);

      // Design is required by metadata
      final Design design = this.step.getAbstractWorkflow().getDesign();

      synchronized (inputData) {

        if (inputData.size() == 0) {
          dataList = new DataList(inputPort, design);
          inputData.add(dataList);
        } else {
          dataList = (DataList) inputData.iterator().next();
        }

        // Add the data to the data list
        dataList.getModifiableList().add(data);
      }
    }
  }

  /**
   * Send end of step tokens.
   */
  private void sendEndOfStepTokens() {

    for (StepOutputPort outputPort : this.outputPorts) {

      // Send the token on the event bus
      WorkflowEventBus.getInstance().postToken(outputPort, this.contextCount);
    }
  }

  /**
   * Send all the tokens of a skip step.
   */
  private void sendSkipStepTokens() {

    // Create a map with the samples
    final Map<String, Sample> samples = new HashMap<>();
    for (Sample sample : this.step.getWorkflow().getDesign().getSamples()) {
      samples.put(Naming.toValidName(sample.getId()), sample);
    }

    int maxExistingDataCount = 0;

    for (StepOutputPort port : this.outputPorts) {

      // If port is not linked or only connected to skipped steps there is need
      // to check if output data exists
      if (port.getLinks().isEmpty() || port.isAllLinksToSkippedSteps()) {
        continue;
      }

      final Set<Data> existingData = port.getExistingData();
      maxExistingDataCount =
          Math.max(maxExistingDataCount, existingData.size());

      if (existingData.size() == 0) {
        throw new EoulsanRuntimeException("No output files of the step \""
            + this.step.getId() + "\" matching with "
            + WorkflowFileNaming.glob(port) + " found");
      }

      for (Data data : existingData) {

        // Get the metadata storage
        final DataMetadataStorage metadataStorage = DataMetadataStorage
            .getInstance(this.step.getAbstractWorkflow().getOutputDirectory());

        // Set the metadata of data from the storage of metadata
        final boolean isMetadataSet = metadataStorage.loadMetadata(data);

        // If metadata has not been found from metadata storage
        if (!isMetadataSet) {

          // Set the metadata from sample metadata
          if (samples.containsKey(data.getName())) {
            WorkflowDataUtils.setDataMetaData(data,
                samples.get(data.getName()));
          }
        }

        // Send the token on the event bus
        WorkflowEventBus.getInstance().postToken(port, data);
      }
    }

    // Save the number of context if the step was not skipped
    // This number is equals to the number of posted data
    synchronized (this) {
      this.contextCount = maxExistingDataCount;
    }

    // Send end of step token
    sendEndOfStepTokens();
  }

  //
  // TaskContext creation methods
  //

  /**
   * Create output data for a new context.
   * @return a map with the output data
   */
  private Map<OutputPort, AbstractData> createContextOutputData() {

    // Design is required by metadata
    final Design design = this.step.getAbstractWorkflow().getDesign();

    final Map<OutputPort, AbstractData> result = new HashMap<>();
    for (StepOutputPort outputPort : this.outputPorts) {

      final AbstractData data;

      if (outputPort.isList()) {
        data = new DataList(outputPort, design);
      } else {
        data = new DataElement(outputPort, design);
      }

      result.put(outputPort, data);
    }

    return result;
  }

  /**
   * Create the contexts of the step.
   * @param workflowContext the Workflow context
   * @return a set with the context
   */
  private Set<TaskContextImpl> createContexts(
      final WorkflowContext workflowContext) {

    final Set<TaskContextImpl> result = new HashSet<>();

    final Set<ImmutableMap<InputPort, Data>> cartesianProductToProcess;

    // Process only the cartesian products of data that have not been converted
    // in Context
    synchronized (this.cartesianProductsUsed) {

      if (!checkIfAllPortsHasReceivedSomeData()
          || !checkIfAllListPortsAreClosed()) {
        cartesianProductToProcess = Collections.emptySet();
      } else {
        cartesianProductToProcess = this.step.getDataProduct()
            .makeProduct(this.inputPorts, this.inputTokens);
      }

      cartesianProductToProcess.removeAll(this.cartesianProductsUsed);
      this.cartesianProductsUsed.addAll(cartesianProductToProcess);
    }

    // For each result of the cartesian product, create a context object
    for (Map<InputPort, Data> inputData : cartesianProductToProcess) {

      // Create the Data object for the output port
      Map<OutputPort, AbstractData> outputData = createContextOutputData();
      // Create the context object
      result.add(new TaskContextImpl(workflowContext, this.step, inputData,
          outputData));
    }

    return result;
  }

  /**
   * Create a context when no input port exists.
   * @param workflowContext the workflow context
   * @return a singleton set with the context
   */
  private Set<TaskContextImpl> createContextWhenNoInputPortExist(
      final WorkflowContext workflowContext) {

    // Empty input Data for the context
    Map<InputPort, Data> inputData = Collections.emptyMap();

    // Create the Data object for the output port
    Map<OutputPort, AbstractData> outputData = createContextOutputData();

    return Collections.singleton(
        new TaskContextImpl(workflowContext, this.step, inputData, outputData));
  }

  //
  // Step results methods
  //

  /**
   * This method write the step result in a file.
   */
  private void writeStepResult(final StepResult result) {

    if (result == null) {
      return;
    }

    // Step result file
    DataFile logFile =
        new DataFile(this.step.getAbstractWorkflow().getJobDirectory(),
            this.step.getId() + STEP_RESULT_EXTENSION);

    try {

      result.write(logFile, false);

    } catch (IOException e) {

      Common.showAndLogErrorMessage(
          "Unable to create log file for " + this.step.getId() + " step.");
    }

    // Write the result file in old format
    if (EoulsanRuntime.getSettings().isUseOldEoulsanResultFormat()) {

      // Step result file
      logFile = new DataFile(this.step.getAbstractWorkflow().getJobDirectory(),
          this.step.getId() + Globals.STEP_RESULT_OLD_FORMAT_EXTENSION);

      try {

        result.write(logFile, true);

      } catch (IOException e) {

        Common.showAndLogErrorMessage(
            "Unable to create log file for " + this.step.getId() + " step.");
      }

    }

  }

  //
  // Cleanup methods
  //

  /**
   * Remove inputs of the step if required by user
   */
  private void removeInputsIfRequired() {

    final TokenManagerRegistry registry = TokenManagerRegistry.getInstance();

    for (AbstractStep step : this.step.getWorkflowInputPorts()
        .getLinkedSteps()) {

      final TokenManager tokenManager = registry.getTokenManager(step);
      tokenManager.removeOutputsIfRequired();
    }
  }

  /**
   * Remove outputs of the step if required by user
   */
  private void removeOutputsIfRequired() {

    // Do nothing if removing output as soon as possible is not required
    if (this.step.getDiscardOutput() != Step.DiscardOutput.ASAP) {
      return;
    }

    // Check if all the step that require step's output is done
    for (AbstractStep step : this.step.getWorkflowOutputPorts()
        .getLinkedSteps()) {

      if (step.getState() != StepState.DONE) {
        return;
      }
    }

    // Remove the outputs of the step
    removeOutputsToDiscard();
  }

  /**
   * Add a failed task.
   * @param failedContext failed task context
   */
  void addFailedOutputData(final TaskContextImpl failedContext) {

    requireNonNull(failedContext, "failedContext cannot be null");

    for (OutputPort port : this.outputPorts) {
      this.failedOutputDataToRemove.add(failedContext.getOutputData(port));
    }
  }

  /**
   * Remove outputs to discard.
   */
  void removeOutputsToDiscard() {

    // Remove output only for standard steps
    if (this.step.getType() != STANDARD_STEP) {
      return;
    }

    final DataFile outputWorkflowDir =
        this.step.getAbstractWorkflow().getOutputDirectory();

    // Only remove symbolic links if the output directory of the step is not the
    // expected output directory
    final boolean remove =
        this.step.getDiscardOutput() != Step.DiscardOutput.NO;

    // In debug mode do not remove links
    if (!remove
        && EoulsanRuntime.getSettings()
            .getBooleanSetting("debug.keep.step.output.links")) {
      return;
    }

    for (Data entry : this.outputTokens.values()) {

      for (Data data : entry.getListElements()) {

        // Standard data file
        if (data.getFormat().getMaxFilesCount() < 2) {

          if (remove) {
            removeFileAndSymLink(data.getDataFile(), outputWorkflowDir);
          } else {
            removeSymLink(data.getDataFile(), outputWorkflowDir);
          }
        }
        // Multi file data file
        else {

          for (int i = 0; i < data.getDataFileCount(); i++) {
            if (remove) {
              removeFileAndSymLink(data.getDataFile(i), outputWorkflowDir);
            } else {
              removeSymLink(data.getDataFile(i), outputWorkflowDir);
            }
          }
        }
      }
    }
  }

  /**
   * Remove all outputs of the step.
   */
  void removeAllOutputs() {

    // Remove output only for standard steps
    if (!(this.step.getType() == STANDARD_STEP
        || this.step.getType() == GENERATOR_STEP)) {
      return;
    }

    final DataFile outputWorkflowDir =
        this.step.getAbstractWorkflow().getOutputDirectory();

    final List<Data> list = new ArrayList<>();
    list.addAll(this.outputTokens.values());
    list.addAll(this.failedOutputDataToRemove);

    for (Data entry : list) {

      for (Data data : entry.getListElements()) {

        // Standard data file
        if (data.getFormat().getMaxFilesCount() < 2) {

          removeFileAndSymLink(data.getDataFile(), outputWorkflowDir);
        }
        // Multi file data file
        else {

          for (int i = 0; i < data.getDataFileCount(); i++) {
            removeFileAndSymLink(data.getDataFile(i), outputWorkflowDir);
          }
        }
      }
    }

  }

  /**
   * Remove a file and its symbolic link.
   * @param file file to remove
   * @param symlinkDir the directory where is the symbolic link to remove
   */
  private void removeFileAndSymLink(final DataFile file,
      final DataFile symlinkDir) {

    // Remove the file
    getLogger().fine("Remove output file: " + file);
    try {

      if (HDFSPathDataProtocol.PROTOCOL_NAME
          .equals(file.getProtocol().getName())) {

        // If file is on HDFS, file removing must be recursive
        file.delete(true);
      } else {

        // In other case, do not use recursion is more safe
        file.delete();
      }
    } catch (IOException e) {
      getLogger().severe("Cannot remove data to discard: "
          + file + " (" + e.getMessage() + ")");
    }

    // Remove the symbolic link
    removeSymLink(file, symlinkDir);
  }

  /**
   * Remove the symbolic link of a file.
   * @param file file to remove
   * @param symlinkDir the directory where is the symbolic link to remove
   */
  private void removeSymLink(final DataFile file, final DataFile symlinkDir) {

    final DataFile link = new DataFile(symlinkDir, file.getName());
    try {

      if (link.exists(false) && link.getMetaData().isSymbolicLink()) {

        getLogger().fine("Remove symbolic link: " + link);
        link.delete();
      }
    } catch (IOException e) {
      getLogger().severe("Cannot remove data symbolic link to discard: "
          + link + " (" + e.getMessage() + ")");
    }
  }

  //
  // Thread methods
  //

  /**
   * Start the Token manager thread.
   */
  void start() {

    // Do not start the thread if is a cluster task
    if (EoulsanRuntime.getRuntime().getMode() == EoulsanExecMode.CLUSTER_TASK) {
      return;
    }

    // Check if the thread has been already started
    checkState(!this.isStarted, "The token manager thread for step "
        + this.step.getId() + " is already started");

    // Start the thread
    new Thread(this, "TokenManager_" + this.step.getId()).start();

    this.isStarted = true;
  }

  /**
   * Test if the thread for the token is started.
   * @return true if the thread for the token is started
   */
  public boolean isStarted() {

    return this.isStarted;
  }

  /**
   * Stop the Token manager thread.
   */
  void stop() {

    // Check if the thread has been started
    checkState(this.isStarted, "The token manager thread for step "
        + this.step.getId() + " is not started");

    this.isStarted = false;
    this.endOfStep = true;
  }

  @Override
  public void run() {

    try {

      boolean firstSubmission = true;
      final WorkflowEventBus eventBus = WorkflowEventBus.getInstance();

      do {

        try {
          Thread.sleep(CHECKING_DELAY_MS);
        } catch (InterruptedException e) {
          getLogger().severe(e.getMessage());
        }

        // Do nothing until the step is not ready
        final StepState state = this.step.getState();
        if (!(state == READY || state.isWorkingState())) {
          continue;
        }

        // Set the step to the working state
        if (state == READY) {
          eventBus.postStepStateChange(this.step, WORKING);
        }

        // Create new contexts to submit
        final Set<TaskContextImpl> contexts;
        synchronized (this) {

          // Get the Workflow context
          final WorkflowContext workflowContext =
              this.step.getAbstractWorkflow().getWorkflowContext();

          if (this.inputPorts.size() > 0) {

            // Standard case
            contexts = createContexts(workflowContext);
          } else {

            // When the step has no input port
            contexts = createContextWhenNoInputPortExist(workflowContext);
          }
        }

        // Save the number of tasks of the step
        synchronized (this) {
          this.contextCount += contexts.size();
        }

        // Submit execution of the available contexts
        if (!this.step.isSkip()) {

          // Create the step output directory if this is the first submission
          if (firstSubmission) {

            final DataFile outputDirectory = this.step.getStepOutputDirectory();

            if (!outputDirectory.exists()) {
              outputDirectory.mkdirs();
            }
            firstSubmission = false;
          }

          this.scheduler.submit(this.step, contexts);
        }

        // If no more token to receive
        if (isNoTokenToReceive()) {

          // Log received tokens
          logReceivedTokens();

          if (!this.step.isSkip()) {

            // Wait end of all context
            this.scheduler.waitEndOfTasks(this.step);

            if (this.step.getState() != ABORTED) {

              // Get the result
              final StepResult result = this.scheduler.getResult(this.step);

              // Set the result immutable
              result.setImmutable();

              // Change Step state
              if (result.isSuccess()) {
                eventBus.postStepStateChange(this.step, DONE);

                // Write step result
                if (this.step.isCreateLogFiles()) {
                  writeStepResult(result);
                }

                // Send end of step tokens
                sendEndOfStepTokens();
              }
            }
          } else {

            // If the step is skip the result is always OK
            eventBus.postStepStateChange(this.step, DONE);

            // Send all the tokens of step tokens
            sendSkipStepTokens();
          }

          // Log sent tokens
          logSentTokens();

          this.endOfStep = true;
        }

      } while (!this.endOfStep);

    } catch (Throwable exception) {

      // Stop the analysis
      this.step.getAbstractWorkflow().emergencyStop(exception,
          "Error while executing the workflow");
    }

    // Register the token manager to the event bus
    WorkflowEventBus.getInstance().register(this);

    // Remove inputs of the step if required by user
    removeInputsIfRequired();
  }

  /**
   * Log received tokens.
   */
  private void logReceivedTokens() {

    String msg = "Step #"
        + this.step.getNumber() + " " + this.step.getId()
        + " has received tokens: ";

    if (this.inputTokens.size() == 0) {
      msg += "no token received";
    } else {

      List<String> list = new ArrayList<>();
      for (InputPort port : this.inputTokens.keySet()) {
        list.add(port.getName()
            + " (" + port.getFormat().getName() + "): "
            + this.inputTokens.get(port).size());
      }

      msg += Joiner.on(", ").join(list);
    }

    getLogger().fine(msg);
  }

  /**
   * Log sent tokens.
   */
  private void logSentTokens() {

    String msg = "Step #"
        + this.step.getNumber() + " " + this.step.getId()
        + " has sent tokens: ";

    if (this.outputTokens.size() == 0) {
      msg += " no token sent";
    } else {

      List<String> list = new ArrayList<>();
      for (OutputPort port : this.outputTokens.keySet()) {
        list.add(port.getName()
            + " (" + port.getFormat().getName() + "): "
            + this.outputTokens.get(port).size());
      }
      msg += Joiner.on(", ").join(list);
    }

    getLogger().fine(msg);
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param step step that tokens must be managed by this instance
   */
  TokenManager(final AbstractStep step) {

    requireNonNull(step, "step argument cannot be null");

    this.step = step;
    this.inputPorts = step.getWorkflowInputPorts();
    this.outputPorts = step.getWorkflowOutputPorts();

    // Get the scheduler
    this.scheduler = TaskSchedulerFactory.getScheduler();

    // Register the token manager to the event bus
    WorkflowEventBus.getInstance().register(this);
  }

}
