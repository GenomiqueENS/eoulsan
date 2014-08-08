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
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.DONE;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.FAIL;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.READY;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.WORKING;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.core.InputPort;
import fr.ens.transcriptome.eoulsan.core.OutputPort;
import fr.ens.transcriptome.eoulsan.core.executors.ContextExecutor;
import fr.ens.transcriptome.eoulsan.core.executors.ContextExecutorFactory;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * This class define a token manager for a step.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TokenManager implements Runnable {

  // TODO may be not the good place to define this
  private static final String STEP_RESULT_FILE_EXTENSION = ".result";

  private static final int CHECKING_DELAY_MS = 1000;

  private final AbstractWorkflowStep step;
  private final ContextExecutor executor;
  private final WorkflowInputPorts inputPorts;
  private final WorkflowOutputPorts outputPorts;

  private Set<Integer> receivedToken = Sets.newHashSet();
  private Multimap<InputPort, Data> inputTokens = ArrayListMultimap.create();
  private Set<InputPort> closedPorts = Sets.newHashSet();
  private Set<ImmutableMap<InputPort, Data>> cartesianProductsUsed = Sets
      .newHashSet();

  private boolean endOfStep;
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

  /**
   * Get the step result.
   * @return the step result object
   */
  WorkflowStepResult getStepResult() {

    return this.executor.getResult(this.step);
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
   * Post a token to the the token manager.
   * @param inputPort port where the token must be posted
   * @param token the token to post
   */
  public void postToken(final WorkflowInputPort inputPort, final Token token) {

    Preconditions.checkNotNull(token);
    Preconditions.checkNotNull(inputPort);

    if (this.receivedToken.contains(token.getId())) {
      return;
    } else {
      this.receivedToken.add(token.getId());
    }

    // Check if the input is linked to the step
    if (!this.inputPorts.contains(inputPort))
      throw new IllegalStateException("Unknown port: " + inputPort);

    if (inputPort.getLink() != token.getOrigin())
      throw new IllegalStateException("The input port ("
          + inputPort + ") and the output port (" + outputPorts
          + ") are not linked:");

    // Check if the input port is closed
    if (this.closedPorts.contains(inputPort))
      throw new IllegalStateException("The input port is closed for the step: "
          + inputPort);

    // Test if the token is an end token
    if (token.isEndOfStepToken()) {

      if (this.inputTokens.get(inputPort).isEmpty())
        throw new IllegalStateException("No data receive for port: "
            + inputPort);

      // The input port must be closed
      this.closedPorts.add(inputPort);
    } else {

      // Register data to process
      final Data data = token.getData();

      if (data.isList())
        for (Data e : data.getListElements())
          addData(inputPort, e);
      else
        addData(inputPort, data);
    }
  }

  /**
   * Add data to the token manager.
   * @param inputPort inputPort port for the data
   * @param data the data
   */
  private void addData(final WorkflowInputPort inputPort, final Data data) {

    if (!inputPort.isList()) {

      synchronized (this) {
        inputTokens.put(inputPort, data);
      }

    } else {

      // Get or create the data list
      final DataList dataList;

      final Collection<Data> inputData = this.inputTokens.get(inputPort);

      if (inputData.size() == 0) {
        dataList = new DataList(inputPort);
      } else {
        dataList = (DataList) inputData.iterator().next();
      }

      // Add the data to the data list
      dataList.getModifiableList().add(data);
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

  //
  // WorkflowStepContext creation methods
  //

  /**
   * Compute the cartesian product.
   * @return a set with the result of the cartesian product
   */
  private Set<ImmutableMap<InputPort, Data>> dataCartesianProduct() {

    if (!checkIfAllPortsHasReceivedSomeData()
        || !checkIfAllListPortsAreClosed())
      return Collections.emptySet();

    final Set<ImmutableMap<InputPort, Data>> result = Sets.newHashSet();
    final List<WorkflowInputPort> portsList =
        Lists.newArrayList(this.inputPorts.iterator());

    // First create the lists for Sets.cartesianProduct()
    final List<Set<CartesianProductEntry>> sets = Lists.newArrayList();
    for (WorkflowInputPort port : portsList) {
      final Set<CartesianProductEntry> s = Sets.newHashSet();
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

    final Map<OutputPort, AbstractData> result = Maps.newHashMap();
    for (WorkflowOutputPort outputPort : this.outputPorts) {

      final AbstractData data;

      if (outputPort.isList()) {
        data = new DataList(outputPort);
      } else {
        data = new DataElement(outputPort);
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
  private Set<WorkflowStepContext> createContexts(
      final WorkflowContext workflowContext) {

    final Set<WorkflowStepContext> result = Sets.newHashSet();

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
      result.add(new WorkflowStepContext(workflowContext, this.step, inputData,
          outputData));
    }

    return result;
  }

  /**
   * Create a context when no input port exists.
   * @param workflowContext the workflow context
   * @return a singleton set with the context
   */
  private Set<WorkflowStepContext> createContextWhenNoInputPortExist(
      final WorkflowContext workflowContext) {

    // Empty input Data for the context
    Map<InputPort, Data> inputData = Collections.emptyMap();

    // Create the Data object for the output port
    Map<OutputPort, AbstractData> outputData = createContextOutputData();

    return Collections.singleton(new WorkflowStepContext(workflowContext,
        this.step, inputData, outputData));
  }

  //
  // Step results methods
  //

  /**
   * This method write the step result in a file.
   */
  private void writeStepResult(final WorkflowStepResult result) {

    if (result == null)
      return;

    // Step result file
    final DataFile logFile =
        new DataFile(this.step.getAbstractWorkflow().getLogDir(), step.getId()
            + STEP_RESULT_FILE_EXTENSION);

    try {

      result.write(logFile);

    } catch (IOException e) {

      Common.showAndLogErrorMessage("Unable to create log file for "
          + step.getId() + " step.");
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
   * Stop the Token manager thread.
   */
  void stop() {

    // Check if the thread has been started
    checkState(this.isStarted,
        "The token manager thread for step "
            + this.step.getId() + " is already started");

    this.endOfStep = true;
  }

  @Override
  public void run() {

    do {

      try {
        Thread.sleep(CHECKING_DELAY_MS);
      } catch (InterruptedException e) {
        EoulsanLogger.getLogger().severe(e.getMessage());
      }

      // Do nothing until the step is not ready
      final StepState state = this.step.getState();
      if (!(state == READY || state == WORKING)) {
        continue;
      }

      // Set the step to the workng state
      if (state == READY) {
        this.step.setState(WORKING);
      }

      // Create new contexts to submit
      final Set<WorkflowStepContext> contexts;
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
      this.executor.submit(this.step, contexts);

      // If no more token to receive
      if (isNoTokenToReceive()) {

        // Wait end of all context
        this.executor.waitEndOfContexts(this.step);

        // Get the result
        final WorkflowStepResult result = this.executor.getResult(this.step);

        // Set the result immutable
        result.setImmutable();

        // Change Step state
        if (result.isSuccess()) {
          this.step.setState(DONE);

          // Write step result
          if (this.step.isCreateLogFiles()) {
            writeStepResult(result);
          }

        } else {
          this.step.setState(FAIL);
        }

        // Send end of step tokens
        sendEndOfStepTokens();

        this.endOfStep = true;
      }

    } while (!this.endOfStep);
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

    // Get the executor
    this.executor = ContextExecutorFactory.getExecutor();
  }

}
