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

package fr.ens.transcriptome.eoulsan.actions;

import java.util.List;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.LocalEoulsanRuntime;

/**
 * This class define the cluster exec Action.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class ClusterExecAction extends ExecAction {

  /** Name of this action. */
  public static final String ACTION_NAME = "clusterexec";

  //
  // Action methods
  //

  @Override
  public String getName() {
    return ACTION_NAME;
  }

  @Override
  public String getDescription() {
    return "execute " + Globals.APP_NAME + " on a cluster.";
  }

  @Override
  public void action(final List<String> arguments) {

    System.err.println("WARNING: the action \""
        + getName()
        + "\" is currently under developpement for the next version of "
        + Globals.APP_NAME + " and may actually not work.");

    // Get Eoulsan runtime
    final LocalEoulsanRuntime localRuntime =
        (LocalEoulsanRuntime) EoulsanRuntime.getRuntime();

    // Set the cluster mode to true
    localRuntime.setClusterMode(true);

    // Launch the action like a standard exec action
    super.action(arguments);
  }

}
