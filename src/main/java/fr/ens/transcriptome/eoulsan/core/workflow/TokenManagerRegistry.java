package fr.ens.transcriptome.eoulsan.core.workflow;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * This class store all the TokenManager instances.
 * @author Laurent Jourdren
 * @since 1.3
 */
public class TokenManagerRegistry {

  private static TokenManagerRegistry singleton;

  private Map<AbstractWorkflowStep, TokenManager> map = Maps.newHashMap();

  /**
   * Get the requested TokenManager.
   * @param step step which TokenManager is requested
   * @return a TokenManager instance
   */
  TokenManager getTokenManager(final AbstractWorkflowStep step) {

    Preconditions.checkNotNull(step, "step cannot be null");

    if (!this.map.containsKey(step)) {
      this.map.put(step, new TokenManager(step));
    }

    return this.map.get(step);
  }

  //
  // Static method
  //

  /**
   * Get the TokenManagerRegistry instance.
   * @return the TokenManagerRegistry instance
   */
  static TokenManagerRegistry getInstance() {

    if (singleton == null) {
      singleton = new TokenManagerRegistry();
    }

    return singleton;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private TokenManagerRegistry() {
  }

}
