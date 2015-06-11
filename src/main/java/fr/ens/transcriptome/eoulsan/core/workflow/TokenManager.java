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
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.Globals.STEP_RESULT_EXTENSION;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.DONE;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.FAILED;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.PARTIALLY_DONE;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.READY;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.WORKING;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType.DESIGN_STEP;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType.GENERATOR_STEP;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType.STANDARD_STEP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.InputPort;
import fr.ens.transcriptome.eoulsan.core.OutputPort;
import fr.ens.transcriptome.eoulsan.core.schedulers.TaskScheduler;
import fr.ens.transcriptome.eoulsan.core.schedulers.TaskSchedulerFactory;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;

/**
 * This class define a token manager for a step.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TokenManager implements Runnable {

  private static final int CHECKING_DELAY_MS = 1000;

  private final AbstractWorkflowStep step;
  private final TaskScheduler scheduler;
  private final WorkflowInputPorts inputPorts;
  private final WorkflowOutputPorts outputPorts;

  private final Set<Integer> receivedTokens = new HashSet<>();
  private final Multimap<InputPort, Data> inputTokens = ArrayListMultimap
      .create();
  private final Multimap<OutputPort, Data> outputTokens = ArrayListMultimap
      .create();
  private final Set<InputPort> closedPorts = new HashSet<>();
  private final Set<ImmutableMap<InputPort, Data>> cartesianProductsUsed =
      new HashSet<>();

  private final Set<Data> failedOutputDataToRemove = new HashSet<>();

  private volatile boolean endOfStep;
  private boolean isStarted;

  /**
   * Class needed for cartesian product computation.
   */
  private static class CartesianProductEntry {
    final WorkflowInputPort port;
    final Data data;

    CartesianProductEntry(final WorkflowInputPort port, final Data data) {
      this.port = port;
      this.data = data;
    }
  }

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

    for (WorkflowInputPort port : this.inputPorts) {
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

    for (WorkflowInputPort port : this.inputPorts) {
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
  public void logSendingToken(final WorkflowOutputPort outputPort,
      final Token token) {

    checkNotNull(token);
    checkNotNull(outputPort);

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
      if (EoulsanRuntime.getSettings().getBooleanSetting(
          "debug.compatibility.result.file.links")
          && this.step.getType() == StepType.STANDARD_STEP) {
        createCompatibilityLinkResultFiles(data);
      }

      // Create symbolic links in output directory
      createSymlinksInOutputDirectory(data);

      // Get the metadata storage
      final DataMetadataStorage metadataStorage =
          DataMetadataStorage.getInstance(this.step.getAbstractWorkflow()
              .getOutputDirectory());

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

    checkNotNull(data, "data argument cannot be null");

    for (Data e : data.getListElements()) {

      // Get the sample id from metadata
      final int sampleNumber = e.getMetadata().getSampleNumber();

      // Do nothing if sample id is not set in metadata
      if (sampleNumber == -1) {
        continue;
      }

      // For all data
      for (DataFile f : DataUtils.getDataFiles(e)) {

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

    Preconditions.checkNotNull(data, "data argument cannot be null");

    final DataFile outputDir =
        this.step.getAbstractWorkflow().getOutputDirectory();
    final DataFile workingDir = this.step.getStepOutputDirectory();

    // Nothing to to if the step working directory is the output directory
    if (this.step.getType() == DESIGN_STEP || outputDir.equals(workingDir)) {
      return;
    }

    for (Data dataElement : data.getListElements()) {
      for (DataFile file : DataUtils.getDataFiles(dataElement)) {

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
          EoulsanLogger.getLogger().severe(
              "Cannot create symbolic link: " + link);
        }
      }
    }
  }

  /**
   * Post a token to the the token manager.
   * @param inputPort port where the token must be posted
   * @param token the token to post
   */
  public void postToken(final WorkflowInputPort inputPort, final Token token) {

    checkNotNull(token);
    checkNotNull(inputPort);

    // Check origin step state
    final StepState originStepState = token.getOrigin().getStep().getState();
    checkState(originStepState == WORKING
        || originStepState == PARTIALLY_DONE || originStepState == DONE,
        "Invalid token step origin state: " + originStepState);

    // Check if token has already been processed
    checkState(!this.receivedTokens.contains(token.getId()),
        "Token has been already received: " + token.getId());

    // Check if the input is linked to the step
    checkState(this.inputPorts.contains(inputPort), "Unknown port: "
        + inputPort);

    // Check if the origin of the token and the input port are linked
    checkState(inputPort.getLink() == token.getOrigin(), "The input port ("
        + inputPort + ") and the output port (" + token.getOrigin()
        + ") are not linked:");

    // Check if the input port is closed
    checkState(!this.closedPorts.contains(inputPort),
        "The input port is closed for the step "
            + this.step.getId() + ": " + inputPort.getName());

    // Test if the token is an end token
    if (token.isEndOfStepToken()) {

      // Check if input port is empty for non skipped steps
      checkState(!(!this.step.isSkip() && this.inputTokens.get(inputPort)
          .isEmpty()), "No data receive for port on step "
          + this.step.getId() + ": " + inputPort.getName());

      // The input port must be closed
      this.closedPorts.add(inputPort);
    } else {

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
  }

  /**
   * Add data to the token manager.
   * @param inputPort inputPort port for the data
   * @param data the data
   */
  private void addData(final WorkflowInputPort inputPort, final Data data) {

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

    for (WorkflowOutputPort outputPort : this.outputPorts) {
      this.step.sendToken(new Token(outputPort));
    }
  }

  /**
   * Send all the tokens of a skip step.
   */
  private void sendSkipStepTokens() {

    // Create a map with the samples
    final Map<String, Sample> samples = new HashMap<>();
    for (Sample sample : this.step.getWorkflow().getDesign().getSamples()) {
      samples.put(FileNaming.toValidName(sample.getName()), sample);
    }

    for (WorkflowOutputPort port : this.outputPorts) {

      // If port is not linked or only connected to skipped steps there is need
      // to check if output data exists
      if (port.getLinks().isEmpty() || port.isAllLinksToSkippedSteps()) {
        continue;
      }

      final Set<Data> existingData = port.getExistingData();

      if (existingData.size() == 0) {
        throw new EoulsanRuntimeException("No output files of the step \""
            + this.step.getId() + "\" matching with " + FileNaming.glob(port)
            + " found");
      }

      for (Data data : existingData) {

        // Get the metadata storage
        final DataMetadataStorage metadataStorage =
            DataMetadataStorage.getInstance(this.step.getAbstractWorkflow()
                .getOutputDirectory());

        // Set the metadata of data from the storage of metadata
        final boolean isMetadataSet = metadataStorage.loadMetadata(data);

        // If metadata has not been found from metadata storage
        if (!isMetadataSet) {

          // Set the metadata from sample metadata
          if (samples.containsKey(data.getName())) {
            DataUtils.setDataMetaData(data, samples.get(data.getName()));
          }

        }

        this.step.sendToken(new Token(port, data));
      }
    }

    // Send end of step token
    sendEndOfStepTokens();
  }

  //
  // TaskContext creation methods
  //

  /**
   * Compute the cartesian product.
   * @return a set with the result of the cartesian product
   */
  private Set<ImmutableMap<InputPort, Data>> dataCartesianProduct() {

    if (!checkIfAllPortsHasReceivedSomeData()
        || !checkIfAllListPortsAreClosed()) {
      return Collections.emptySet();
    }

    final Set<ImmutableMap<InputPort, Data>> result = new HashSet<>();
    final List<WorkflowInputPort> portsList =
        Lists.newArrayList(this.inputPorts.iterator());

    // First create the lists for Sets.cartesianProduct()
    final List<Set<CartesianProductEntry>> sets = new ArrayList<>();
    for (WorkflowInputPort port : portsList) {
      final Set<CartesianProductEntry> s = new HashSet<>();
      for (Data d : this.inputTokens.get(port)) {
        s.add(new CartesianProductEntry(port, d));
      }
      sets.add(s);
    }

    // Compute cartesian product
    final Set<List<CartesianProductEntry>> cartesianProduct =
        Sets.cartesianProduct(sets);

    // Now convert result of cartesianProduct() to final result
    for (List<CartesianProductEntry> l : cartesianProduct) {

      final ImmutableMap.Builder<InputPort, Data> imb = ImmutableMap.builder();

      for (CartesianProductEntry e : l) {
        imb.put(e.port, e.data);
      }

      result.add(imb.build());
    }

    return result;
  }

  /**
   * Create output data for a new context.
   * @return a map with the output data
   */
  private Map<OutputPort, AbstractData> createContextOutputData() {

    // Design is required by metadata
    final Design design = this.step.getAbstractWorkflow().getDesign();

    final Map<OutputPort, AbstractData> result = new HashMap<>();
    for (WorkflowOutputPort outputPort : this.outputPorts) {

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
  private Set<TaskContext> createContexts(final WorkflowContext workflowContext) {

    final Set<TaskContext> result = new HashSet<>();

    final Set<ImmutableMap<InputPort, Data>> cartesianProductToProcess;

    // Process only the cartesian products of data that have not been converted
    // in Context
    synchronized (this.cartesianProductsUsed) {
      cartesianProductToProcess = dataCartesianProduct();
      cartesianProductToProcess.removeAll(this.cartesianProductsUsed);
      this.cartesianProductsUsed.addAll(cartesianProductToProcess);
    }

    // For each result of the cartesian product, create a context object
    for (Map<InputPort, Data> inputData : cartesianProductToProcess) {

      // Create the Data object for the output port
      Map<OutputPort, AbstractData> outputData = createContextOutputData();
      // Create the context object
      result.add(new TaskContext(workflowContext, this.step, inputData,
          outputData));
    }

    return result;
  }

  /**
   * Create a context when no input port exists.
   * @param workflowContext the workflow context
   * @return a singleton set with the context
   */
  private Set<TaskContext> createContextWhenNoInputPortExist(
      final WorkflowContext workflowContext) {

    // Empty input Data for the context
    Map<InputPort, Data> inputData = Collections.emptyMap();

    // Create the Data object for the output port
    Map<OutputPort, AbstractData> outputData = createContextOutputData();

    return Collections.singleton(new TaskContext(workflowContext, this.step,
        inputData, outputData));
  }

  //
  // Step results methods
  //

  /**
   * This method write the step result in a file.
   */
  private void writeStepResult(final WorkflowStepResult result) {

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

      Common.showAndLogErrorMessage("Unable to create log file for "
          + this.step.getId() + " step.");
    }

    // Write the result file in old format
    if (EoulsanRuntime.getSettings().isUseOldEoulsanResultFormat()) {

      // Step result file
      logFile =
          new DataFile(this.step.getAbstractWorkflow().getJobDirectory(),
              this.step.getId() + Globals.STEP_RESULT_OLD_FORMAT_EXTENSION);

      try {

        result.write(logFile, true);

      } catch (IOException e) {

        Common.showAndLogErrorMessage("Unable to create log file for "
            + this.step.getId() + " step.");
      }

    }

  }

  //
  // Cleanup methods
  //

  /**
   * Add a failed task.
   * @param failedContext failed task context
   */
  void addFailedOutputData(final TaskContext failedContext) {

    checkNotNull(failedContext, "failedContext cannot be null");

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

    final DataFile outputStepDir = this.step.getStepOutputDirectory();
    final DataFile outputWorkflowDir =
        this.step.getAbstractWorkflow().getOutputDirectory();

    // Do nothing if the output of the step is the output of the workflow
    if (outputWorkflowDir.equals(outputStepDir)) {
      return;
    }

    for (Data entry : this.outputTokens.values()) {

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
   * Remove all outputs of the step.
   */
  void removeAllOutputs() {

    // Remove output only for standard steps
    if (!(this.step.getType() == STANDARD_STEP || this.step.getType() == GENERATOR_STEP)) {
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
      file.delete();
    } catch (IOException e) {
      getLogger().severe(
          "Cannot remove data to discard: "
              + file + " (" + e.getMessage() + ")");
    }

    // Remove the symbolic link
    final DataFile link = new DataFile(symlinkDir, file.getName());
    try {

      if (link.exists(false) && link.getMetaData().isSymbolicLink()) {

        getLogger().fine("Remove symbolic link: " + link);
        link.delete();
      }
    } catch (IOException e) {
      getLogger().severe(
          "Cannot remove data symbolic link to discard: "
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
    checkState(this.isStarted,
        "The token manager thread for step "
            + this.step.getId() + " is not started");

    this.isStarted = false;
    this.endOfStep = true;
  }

  @Override
  public void run() {

    try {

      do {

        try {
          Thread.sleep(CHECKING_DELAY_MS);
        } catch (InterruptedException e) {
          getLogger().severe(e.getMessage());
        }

        // Do nothing until the step is not ready
        final StepState state = this.step.getState();
        if (!(state == READY || state == WORKING || state == PARTIALLY_DONE)) {
          continue;
        }

        // Set the step to the workng state
        if (state == READY) {
          this.step.setState(WORKING);
        }

        // Create new contexts to submit
        final Set<TaskContext> contexts;
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

        // Submit execution of the available contexts
        if (!this.step.isSkip()) {
          this.scheduler.submit(this.step, contexts);
        }

        // If no more token to receive
        if (isNoTokenToReceive()) {

          // Log received tokens
          logReceivedTokens();

          if (!this.step.isSkip()) {

            // Wait end of all context
            this.scheduler.waitEndOfTasks(this.step);

            // Get the result
            final WorkflowStepResult result =
                this.scheduler.getResult(this.step);

            // Set the result immutable
            result.setImmutable();

            // Change Step state
            if (result.isSuccess()) {
              this.step.setState(DONE);

              // Write step result
              if (this.step.isCreateLogFiles()) {
                writeStepResult(result);
              }

              // Send end of step tokens
              sendEndOfStepTokens();

            } else {
              this.step.setState(FAILED);
            }
          } else {

            // If the step is skip the result is always OK
            this.step.setState(DONE);

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
  }

  /**
   * Log received tokens.
   */
  private void logReceivedTokens() {

    String msg =
        "Step #"
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

    String msg =
        "Step #"
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
  TokenManager(final AbstractWorkflowStep step) {

    Preconditions.checkNotNull(step, "step argument cannot be null");

    this.step = step;
    this.inputPorts = step.getWorkflowInputPorts();
    this.outputPorts = step.getWorkflowOutputPorts();

    // Get the scheduler
    this.scheduler = TaskSchedulerFactory.getScheduler();
  }

}
