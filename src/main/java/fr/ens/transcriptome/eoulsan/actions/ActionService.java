package fr.ens.transcriptome.eoulsan.actions;

import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import com.google.common.collect.Lists;

/**
 * This class define a service to retrieve an Action
 * @author Laurent Jourdren
 */
public class ActionService {

  private static ActionService service;
  private final ServiceLoader<Action> loader;

  //
  // Static method
  //

  /**
   * Retrieve the singleton static instance of an ActionService.
   * @return A ActionService instance
   */
  public static synchronized ActionService getInstance() {

    if (service == null) {
      service = new ActionService();
    }

    return service;
  }

  //
  // Instance methods
  //

  /**
   * Get an Action.
   * @param actionName name of the mapper to get
   * @return an Action
   */
  public Action getAction(final String actionName) {

    if (actionName == null) {
      return null;
    }

    final String actionNameLower = actionName.toLowerCase();

    final Iterator<Action> it = this.loader.iterator();

    while (it.hasNext()) {

      final Action action = it.next();

      if (actionNameLower.equals(action.getName().toLowerCase())) {
        return action;
      }
    }

    return null;
  }

  /**
   * Get the list of actions available.
   * @return a list with all the available actions
   */
  public List<Action> getActions() {
    
    final List<Action> result = Lists.newArrayList();
    final Iterator<Action> it = this.loader.iterator();

    while (it.hasNext()) {
      result.add(it.next());
    }
    
    return result;
  }
  
  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private ActionService() {

    loader = ServiceLoader.load(Action.class);
  }

}
