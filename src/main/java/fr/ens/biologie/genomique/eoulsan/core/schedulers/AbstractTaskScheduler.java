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

package fr.ens.biologie.genomique.eoulsan.core.schedulers;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Multimaps.synchronizedMultimap;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.workflow.AbstractStep;
import fr.ens.biologie.genomique.eoulsan.core.workflow.StepResult;
import fr.ens.biologie.genomique.eoulsan.core.workflow.StepStatus;
import fr.ens.biologie.genomique.eoulsan.core.workflow.TaskContextImpl;
import fr.ens.biologie.genomique.eoulsan.core.workflow.TaskResultImpl;
import fr.ens.biologie.genomique.eoulsan.core.workflow.TaskRunner;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class define an abstract task scheduler.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractTaskScheduler implements TaskScheduler {

  private static final int SLEEP_TIME_IN_MS = 500;

  private final Multimap<Step, Integer> submittedContexts;
  private final Multimap<Step, Integer> runningContexts;
  private final Multimap<Step, Integer> doneContexts;
  private final Map<Integer, Step> contexts;
  private final Map<Step, StepStatus> status;
  private final Map<Step, StepResult> results;

  private volatile boolean isStarted;
  private volatile boolean isStopped;
  private volatile boolean isPaused;

  //
  // Protected methods
  //

  /**
   * Add a task result to its step result.
   *
   * @param context the context to execute
   * @param result the result to add
   */
  private void addResult(final TaskContextImpl context, final TaskResultImpl result) {

    this.results.get(getStep(context.getId())).addResult(context, result);
  }

  /**
   * Get the step related to a context.
   *
   * @param context the context
   * @return the step related to the context
   */
  protected Step getStep(final TaskContextImpl context) {

    requireNonNull(context, "context argument cannot be null");

    return getStep(context.getId());
  }

  /**
   * Get the step related to a context.
   *
   * @param contextId the context id
   * @return the step related to the context
   */
  protected Step getStep(final int contextId) {

    // Test if the contextId has been submitted
    checkState(
        this.contexts.containsKey(contextId),
        "The context (" + contextId + ") has never been submitted");

    return this.contexts.get(contextId);
  }

  /**
   * Set a context in the running state
   *
   * @param context the context
   */
  private void addRunningContext(final TaskContextImpl context) {

    requireNonNull(context, "context argument cannot be null");

    addRunningContext(context.getId());
  }

  /**
   * Set a context in the running state
   *
   * @param contextId the context id
   */
  private void addRunningContext(final int contextId) {

    // Check execution state
    checkExecutionState();

    // Test if the contextId has been submitted
    checkState(
        this.contexts.containsKey(contextId),
        "The context (" + contextId + ") has never been submitted");

    // Test if the context is already running
    checkState(
        !this.runningContexts.containsValue(contextId),
        "The context (" + contextId + ") already running");

    // Test if the context has been already done
    checkState(
        !this.doneContexts.containsValue(contextId),
        "The context (" + contextId + ") has been already done");

    final Step step = getStep(contextId);
    synchronized (this) {
      this.runningContexts.put(step, contextId);
    }

    // Update the UI
    this.status.get(step).setTaskRunning(contextId);

    getLogger()
        .fine(
            "Scheduler: task #"
                + contextId
                + " (step #"
                + step.getNumber()
                + " "
                + step.getId()
                + ") is running");
  }

  /**
   * Set a context in done state.
   *
   * @param context the context
   */
  private void addDoneContext(final TaskContextImpl context) {

    requireNonNull(context, "context argument cannot be null");

    addDoneContext(context.getId());
  }

  /**
   * Set a context in done state.
   *
   * @param contextId the context id
   */
  private void addDoneContext(final int contextId) {

    // Check execution state
    checkExecutionState();

    // Test if the contextId has been submitted
    checkState(
        this.contexts.containsKey(contextId),
        "The context (" + contextId + ") has never been submitted");

    // Test if the context is running
    checkState(
        this.runningContexts.containsValue(contextId),
        "The context (" + contextId + ") is not running");

    // Test if the context has been already done
    checkState(
        !this.doneContexts.containsValue(contextId),
        "The context (" + contextId + ") has been already done");

    final Step step = getStep(contextId);
    synchronized (this) {
      this.runningContexts.remove(step, contextId);
      this.doneContexts.put(step, contextId);
    }

    // Update the UI
    this.status.get(step).setTaskDone(contextId);

    getLogger()
        .fine(
            "Scheduler: task #"
                + contextId
                + " (step #"
                + step.getNumber()
                + " "
                + step.getId()
                + ") is done");
  }

  /**
   * Set the state of the context before executing a task.
   *
   * @param context the context to execute
   */
  protected void beforeExecuteTask(final TaskContextImpl context) {

    requireNonNull(context, "context argument is null");

    // Check execution state
    checkExecutionState();

    // Update counters
    addRunningContext(context);
  }

  /**
   * Set the state of the context after executing a task.
   *
   * @param context the context to execute
   * @param result the task result
   */
  protected void afterExecuteTask(final TaskContextImpl context, final TaskResultImpl result) {

    requireNonNull(context, "context argument is null");
    requireNonNull(result, "result argument is null");

    // Add the context result to the step result
    addResult(context, result);

    // Update counters
    addDoneContext(context);
  }

  /**
   * Default executing context method.
   *
   * @param context the context
   * @return a TaskResult object
   */
  protected TaskResultImpl executeTask(final TaskContextImpl context) {

    requireNonNull(context, "context argument is null");

    // Get the step of the context
    final Step step = getStep(context.getId());

    // Create context runner
    final TaskRunner contextRunner = new TaskRunner(context, getStatus(step));

    // Run the step context
    contextRunner.run();

    // Return the result
    return contextRunner.getResult();
  }

  //
  // TaskScheduler interface
  //

  @Override
  public void submit(final Step step, final Set<TaskContextImpl> contexts) {

    requireNonNull(contexts, "contexts argument cannot be null");

    for (TaskContextImpl context : contexts) {
      submit(step, context);
    }
  }

  @Override
  public void submit(final Step step, final TaskContextImpl context) {

    // Check execution state
    checkExecutionState();

    requireNonNull(step, "step argument cannot be null");
    requireNonNull(context, "context argument cannot be null");

    // Test if the context has been already submitted
    checkState(
        !this.submittedContexts.containsEntry(step, context.getId()),
        "The context (#" + context.getId() + ") has been already submitted");

    synchronized (this) {

      // If this the first context of the step
      if (!this.status.containsKey(step)) {

        this.status.put(step, new StepStatus((AbstractStep) step));
        this.results.put(step, new StepResult((AbstractStep) step));
      }

      this.submittedContexts.put(step, context.getId());
      this.contexts.put(context.getId(), step);
    }

    // Update the UI
    this.status.get(step).setTaskSubmitted(context.getId());

    getLogger()
        .fine(
            "Scheduler: task #"
                + context.getId()
                + " (step #"
                + step.getNumber()
                + " "
                + step.getId()
                + ") has been submitted");
  }

  @Override
  public StepStatus getStatus(final Step step) {

    return this.status.get(step);
  }

  @Override
  public StepResult getResult(final Step step) {

    return this.results.get(step);
  }

  @Override
  public int getTaskSubmittedCount(final Step step) {

    requireNonNull(step, "step argument cannot be null");

    // Test if contexts for the step has been submitted
    if (!this.submittedContexts.containsKey(step)) {
      return 0;
    }

    return this.submittedContexts.get(step).size();
  }

  @Override
  public int getTaskRunningCount(final Step step) {

    requireNonNull(step, "step argument cannot be null");

    // Test if contexts for the step has been submitted
    if (!this.runningContexts.containsKey(step)) {
      return 0;
    }

    return this.runningContexts.get(step).size();
  }

  @Override
  public int getTaskDoneCount(final Step step) {

    requireNonNull(step, "step argument cannot be null");

    // Test if contexts for the step has been submitted
    if (!this.doneContexts.containsKey(step)) {
      return 0;
    }

    return this.doneContexts.get(step).size();
  }

  @Override
  public int getTotalTaskSubmittedCount() {

    return this.submittedContexts.size();
  }

  @Override
  public int getTotalTaskRunningCount() {

    return this.runningContexts.size();
  }

  @Override
  public int getTotalTaskDoneCount() {

    return this.doneContexts.size();
  }

  int getTotalWaitingCount() {

    return getTotalTaskSubmittedCount() - getTotalTaskRunningCount() - getTotalTaskDoneCount();
  }

  @Override
  public void waitEndOfTasks(final Step step) {

    // Check execution state
    checkExecutionState();

    while (!isStopped()
        && (getTaskRunningCount(step) > 0
            || getTaskSubmittedCount(step) > getTaskDoneCount(step))) {

      try {
        Thread.sleep(SLEEP_TIME_IN_MS);
      } catch (InterruptedException e) {
        getLogger().severe(e.getMessage());
      }
    }
  }

  @Override
  public void start() {

    // Check execution state
    checkState(!this.isStopped, "The scheduler is stopped");

    synchronized (this) {
      this.isStarted = true;
    }
  }

  protected boolean isStarted() {
    return this.isStarted;
  }

  @Override
  public void stop() {

    // Check execution state
    checkExecutionState();

    synchronized (this) {
      this.isStopped = true;
    }
  }

  protected boolean isStopped() {
    return this.isStopped;
  }

  /** Pause the scheduler. */
  void pause() {

    // Check execution state
    checkExecutionState();

    checkState(!this.isPaused, "The execution is already paused");

    synchronized (this) {
      this.isPaused = true;
    }
  }

  /** Resume the scheduler. */
  void resume() {

    // Check execution state
    checkExecutionState();

    checkState(this.isPaused, "The execution is not paused");

    synchronized (this) {
      this.isPaused = false;
    }
  }

  /**
   * Test if the scheduler is paused.
   *
   * @return true if the scheduler is paused
   */
  boolean isPaused() {
    return this.isPaused;
  }

  private void checkExecutionState() {

    checkState(this.isStarted, "The scheduler is not started");
    checkState(!this.isStopped, "The scheduler is stopped");
  }

  //
  // Constructor
  //

  /** Protected constructor. */
  protected AbstractTaskScheduler() {

    final Multimap<Step, Integer> mm1 = HashMultimap.create();
    final Multimap<Step, Integer> mm2 = HashMultimap.create();
    final Multimap<Step, Integer> mm3 = HashMultimap.create();

    this.submittedContexts = synchronizedMultimap(mm1);
    this.runningContexts = synchronizedMultimap(mm2);
    this.doneContexts = synchronizedMultimap(mm3);

    final Map<Integer, Step> m1 = new HashMap<>();
    final Map<Step, StepStatus> m2 = new HashMap<>();
    final Map<Step, StepResult> m3 = new HashMap<>();

    this.contexts = synchronizedMap(m1);
    this.status = synchronizedMap(m2);
    this.results = synchronizedMap(m3);
  }
}
