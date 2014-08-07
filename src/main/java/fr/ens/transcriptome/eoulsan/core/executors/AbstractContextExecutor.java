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

package fr.ens.transcriptome.eoulsan.core.executors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.core.workflow.AbstractWorkflowStep;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepContext;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepContextExecutor;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepContextResult;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepResult;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepStatus;

/**
 * This class define an abstract context executor.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractContextExecutor implements ContextExecutor {

  private static final int SLEEP_TIME_IN_MS = 500;

  private Multimap<WorkflowStep, Integer> submittedContexts = HashMultimap
      .create();
  private Multimap<WorkflowStep, Integer> runningContexts = HashMultimap
      .create();
  private Multimap<WorkflowStep, Integer> doneContexts = HashMultimap.create();
  private Map<Integer, WorkflowStep> contexts = Maps.newHashMap();

  // private int submitedContextCount;
  // private int runningContextCount;
  // private int doneContextCount;

  private final Map<WorkflowStep, WorkflowStepStatus> status = Maps
      .newHashMap();
  private final Map<WorkflowStep, WorkflowStepResult> results = Maps
      .newHashMap();

  //
  // Protected methods
  //

  private void addResult(final WorkflowStep step,
      final WorkflowStepContextResult result) {

    this.results.get(step).addResult(result);
  }

  protected WorkflowStep getStep(final WorkflowStepContext context) {

    checkNotNull(context, "context argument cannot be null");

    return getStep(context.getId());
  }

  protected WorkflowStep getStep(final int contextId) {

    // Test if the contextId has been submitted
    checkState(this.contexts.containsKey(contextId), "The context ("
        + contextId + ") has never been submitted");

    return this.contexts.get(contextId);
  }

  private void addRunningContext(final WorkflowStepContext context) {

    checkNotNull(context, "context argument cannot be null");

    addRunningContext(context.getId());
  }

  private void addRunningContext(final int contextId) {

    // Test if the contextId has been submitted
    checkState(this.contexts.containsKey(contextId), "The context ("
        + contextId + ") has never been submitted");

    // Test if the context is already running
    checkState(!this.runningContexts.containsValue(contextId), "The context ("
        + contextId + ") already running");

    // Test if the context has been already done
    checkState(!this.doneContexts.containsValue(contextId), "The context ("
        + contextId + ") has been already done");

    synchronized (this) {
      this.runningContexts.put(getStep(contextId), contextId);
    }
  }

  private void addDoneContext(final WorkflowStepContext context) {

    checkNotNull(context, "context argument cannot be null");

    addDoneContext(context.getId());
  }

  private void addDoneContext(final int contextId) {

    // Test if the contextId has been submitted
    checkState(this.contexts.containsKey(contextId), "The context ("
        + contextId + ") has never been submitted");

    // Test if the context is running
    checkState(this.runningContexts.containsValue(contextId), "The context ("
        + contextId + ") is not running");

    // Test if the context has been already done
    checkState(!this.doneContexts.containsValue(contextId), "The context ("
        + contextId + ") has been already done");

    final WorkflowStep step = getStep(contextId);
    synchronized (this) {
      this.runningContexts.remove(step, contextId);
      this.doneContexts.put(step, contextId);
    }
  }

  protected void execute(final WorkflowStepContext context) {

    checkNotNull(context, "context argument is null");

    // Get the step of the context
    final WorkflowStep step = getStep(context.getId());

    // Create context executor
    final WorkflowStepContextExecutor contextExecutor =
        new WorkflowStepContextExecutor(context, getStatus(step));

    // Update counters
    addRunningContext(context);

    // Execute the context
    contextExecutor.execute();

    // Get the context result
    final WorkflowStepContextResult contextResult = contextExecutor.getResult();

    // Add the context result to the step result
    addResult(step, contextResult);

    // Update counters
    addDoneContext(context);

    // If context result is not a success do not process other contexts
    if (!contextResult.isSuccess()) {
      stop();
    }
  }

  //
  // ContextExecutor interface
  //

  @Override
  public void submit(final WorkflowStep step,
      final Set<WorkflowStepContext> contexts) {

    checkNotNull(contexts, "contexts argument cannot be null");

    for (WorkflowStepContext context : contexts) {
      submit(step, context);
    }
  }

  @Override
  public void submit(final WorkflowStep step, final WorkflowStepContext context) {

    checkNotNull(step, "step argument cannot be null");
    checkNotNull(context, "context argument cannot be null");

    // Test if the context has been already submitted
    checkState(!this.submittedContexts.containsEntry(step, context.getId()),
        "The context (#" + context.getId() + ") has been already submitted");

    synchronized (this) {

      // If this the first context of the step
      if (!this.status.containsKey(step)) {

        this.status.put(step, new WorkflowStepStatus(
            (AbstractWorkflowStep) step));
        this.results.put(step, new WorkflowStepResult(
            (AbstractWorkflowStep) step));
      }

      this.submittedContexts.put(step, context.getId());
      this.contexts.put(context.getId(), step);
    }
  }

  @Override
  public WorkflowStepStatus getStatus(final WorkflowStep step) {

    return this.status.get(step);
  }

  public WorkflowStepResult getResult(final WorkflowStep step) {

    return this.results.get(step);
  }

  @Override
  public int getContextSubmitedCount(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    // Test if contexts for the step has been submitted
    if (!this.submittedContexts.containsKey(step)) {
      return 0;
    }

    return this.submittedContexts.get(step).size();
  }

  @Override
  public int getContextRunningCount(WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    // Test if contexts for the step has been submitted
    if (!this.runningContexts.containsKey(step)) {
      return 0;
    }

    return this.runningContexts.get(step).size();
  }

  @Override
  public int getContextDoneCount(WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    // Test if contexts for the step has been submitted
    if (!this.doneContexts.containsKey(step)) {
      return 0;
    }

    return this.doneContexts.get(step).size();
  }

  @Override
  public int getTotalContextSubmitedCount() {

    return this.submittedContexts.size();
  }

  @Override
  public int getTotalContextRunningCount() {

    return this.runningContexts.size();
  }

  @Override
  public int getTotalContextDoneCount() {

    return this.doneContexts.size();
  }

  @Override
  public void waitEndOfContexts() {

    while (this.runningContexts.size() > 0
        || this.submittedContexts.size() > this.doneContexts.size()) {

      try {
        Thread.sleep(SLEEP_TIME_IN_MS);
      } catch (InterruptedException e) {
        EoulsanLogger.getLogger().severe(e.getMessage());
      }
    }

    // Stop the executor
    stop();
  }

  @Override
  public void start() {
    // Do nothing
  }

}
