package fr.ens.biologie.genomique.eoulsan.core.workflow;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;
import fr.ens.biologie.genomique.eoulsan.core.Step;

import java.util.*;

import static java.util.Objects.requireNonNull;

public class KWorkflow {

  private final AbstractWorkflow workflow;
  private final KConfiguration conf = new KConfiguration();
  private final WorkflowContext workflowContext;
  private final Set<String> stepIds = new HashSet<>();
  private final Map<KStep, Step.StepState> steps = new HashMap<>();
  private final Multimap<Step.StepState, KStep> states =
    ArrayListMultimap.create();

  private KStep rootStep;
  private KStep firstStep;

  //
  // Getters
  //

  public KConfiguration getConfiguration() {
    return this.conf;
  }

  /**
   * Get the real Context object. This method is useful to redefine context
   * values like base directory.
   * @return The Context object
   */
  public WorkflowContext getWorkflowContext() {

    return this.workflowContext;
  }

  public Set<KStep> getSteps() {

    final Set<KStep> result = new HashSet<>(this.steps.keySet());

    return Collections.unmodifiableSet(result);
  }

  public boolean containsStepId(String stepId) {

    return this.stepIds.contains(stepId);
  }

  public KStep getRootStep() {

    return this.rootStep;
  }

  public KStep getFirstStep() {

    return this.firstStep;
  }


  public void register(final KStep step) {

    Objects.requireNonNull(step);

    // Register root step
    if (step.getType() == Step.StepType.ROOT_STEP) {
      setRootStep(step);
    }

    // Register first step
    if (step.getType() == Step.StepType.FIRST_STEP) {
      setFirstStep(step);
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
    final Step.StepState newState = event.getState();

    if (step.getWorkflow() != this.workflow) {
      throw new IllegalStateException("step is not part of the workflow");
    }

    synchronized (this) {

      Step.StepState oldState = this.steps.get(step.getKStep());

      // Test if the state has changed
      if (oldState == newState) {
        return;
      }

      this.states.remove(oldState, step.getKStep());
      this.states.put(newState, step.getKStep());
      this.steps.put(step.getKStep(), newState);
    }
  }

    /**
     * Get the steps which has some step status. The step are ordered.
     * @param states step status to retrieve
     * @return a sorted list with the steps
     */
    public List<KStep> getSortedStepsByState(final Step.StepState... states) {

      requireNonNull(states, "states argument is null");

      final List<KStep> result = new ArrayList<>();

      for (Step.StepState state : states) {
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
  public List<KStep> getSortedStepsByState(final Step.StepState state) {

    requireNonNull(state, "state argument is null");

    final List<KStep> result;

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
  private static void sortListSteps(final List<KStep> list) {

    if (list == null) {
      return;
    }

    list.sort(
      Comparator.comparingInt((KStep a) -> a.getType().getPriority())
        .thenComparingInt(KStep::getNumber));

  }

  //
  // Setters
  //

  private void setRootStep(KStep rootStep) {

    if (this.rootStep != null && this.rootStep != rootStep) {
      throw new IllegalStateException(
        "Cannot add 2 root steps to the workflow");
    }

    this.rootStep = rootStep;
  }

  private void setFirstStep(KStep firstStep) {

    if (this.firstStep != null && this.firstStep != firstStep) {
      throw new IllegalStateException(
        "Cannot add 2 first steps to the workflow");
    }

    this.firstStep = firstStep;
  }

  //
  // Constructor
  //

  public KWorkflow(AbstractWorkflow workflow, WorkflowContext workflowContext) {

    Objects.requireNonNull(workflow);
    Objects.requireNonNull(workflowContext);

    this.workflow = workflow;
    this.workflowContext = workflowContext;

    // Register the object in the event bus
    WorkflowEventBus.getInstance().register(this);
  }

}
