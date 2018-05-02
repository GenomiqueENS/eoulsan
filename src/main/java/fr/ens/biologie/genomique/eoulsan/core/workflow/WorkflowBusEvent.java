package fr.ens.biologie.genomique.eoulsan.core.workflow;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;

/**
 * This class define a single for the event bus.
 * @since 2.3
 * @author Laurent Jourdren
 */
public class WorkflowBusEvent {

  private static WorkflowBusEvent singleton;

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
  public void post(final Object object) {

    this.eventBus.post(object);
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
  public static synchronized WorkflowBusEvent getInstance() {

    if (singleton == null) {
      singleton = new WorkflowBusEvent();
    }

    return singleton;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private WorkflowBusEvent() {

    this.eventBus = new EventBus("Eoulsan");
    this.eventBus.register(this);
  }

}
