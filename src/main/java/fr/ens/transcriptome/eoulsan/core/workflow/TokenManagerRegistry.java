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

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

/**
 * This class store all the TokenManager instances.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TokenManagerRegistry {

  private static TokenManagerRegistry singleton;

  private Map<AbstractWorkflowStep, TokenManager> map = new HashMap<>();

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
