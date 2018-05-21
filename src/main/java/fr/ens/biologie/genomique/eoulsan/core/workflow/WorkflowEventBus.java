package fr.ens.biologie.genomique.eoulsan.core.workflow;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.core.Step.StepState;
import fr.ens.biologie.genomique.eoulsan.data.Data;;

/**
 * This class define a single for the event bus.
 * @since 2.3
 * @author Laurent Jourdren
 */
public class WorkflowEventBus {

  private static WorkflowEventBus singleton;

  private final EventBus eventBus;

  /**
   * Register an listener object.
   * @param object the listener object
   */
  public void register(final Object object) {

    this.eventBus.register(object);
  }

  /**
   * Unregister an listener object.
   * @param object the listener object
   */
  public void unregister(final Object object) {

    this.eventBus.unregister(object);
  }

  /**
   * Post an event.
   * @param object the event to post
   */
  private void post(final Object object) {

    this.eventBus.post(object);
  }

  /**
   * Post a step state change event.
   * @param step the step
   * @param state the new state
   */
  void postStepStateChange(final AbstractStep step, final StepState state) {

    post(new StepStateEvent(step, state));
  }

  /**
   * Post a token.
   * @param fromPort the port emiting the token
   */
  void postToken(final StepOutputPort fromPort) {

    post(new Token(fromPort));
  }

  /**
   * Post a token.
   * @param fromPort the port emiting the token
   * @param data of the token
   */
  void postToken(final StepOutputPort fromPort, final Data data) {

    post(new Token(fromPort, data));
  }

  /**
   * Post an UI event.
   * @param uiEvent UI event
   */
  void postUIEvent(final UIEvent uiEvent) {

    post(uiEvent);
  }

  @Subscribe
  public void deadEvent(final DeadEvent event) {

    EoulsanLogger.getLogger().severe("Dead event: " + event);
  }

  //
  // Static methods
  //

  /**
   * Get the singleton instance of WorkflowBusEvent.
   * @return the singleton instance of WorkflowBusEvent
   */
  public static synchronized WorkflowEventBus getInstance() {

    if (singleton == null) {
      singleton = new WorkflowEventBus();
    }

    return singleton;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private WorkflowEventBus() {

    this.eventBus = new EventBus(Globals.APP_NAME);
    this.eventBus.register(this);
  }

}
