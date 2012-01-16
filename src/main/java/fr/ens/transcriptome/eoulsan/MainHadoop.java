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

package fr.ens.transcriptome.eoulsan;

import fr.ens.transcriptome.eoulsan.actions.Action;
import fr.ens.transcriptome.eoulsan.actions.ActionService;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * Main class in Hadoop mode.
 * @author Laurent Jourdren
 */
public final class MainHadoop {

  /**
   * Main method. This method is called by MainHadoop.
   * @param args command line arguments
   */
  public static void main(final String[] args) {

    if (args.length == 0) {

      System.out.println("hadoop jar "
          + Globals.APP_NAME_LOWER_CASE + ".jar command [arguments]");

      Common.exit(0);
    }

    // Initialize Eoulsan runtime
    try {
      HadoopEoulsanRuntime.newEoulsanRuntime();
    } catch (EoulsanException e) {
      Common.showErrorMessageAndExit(e.getMessage());
    }

    // Set action name and arguments
    final String actionName = args[0].trim().toLowerCase();
    final String[] arguments = StringUtils.arrayWithoutFirstsElement(args, 1);

    // Search action
    final Action action = ActionService.getInstance().getAction(actionName);

    // Action not found ?
    if (action == null || !action.isHadoopJarMode()) {
      Common.showErrorMessageAndExit("Unknown action: "
          + actionName + ".\n" + "type: " + Globals.APP_NAME_LOWER_CASE
          + " -help for more help.\n");
    }

    // Test if action can be executed with current platform
    if (!action.isCurrentArchCompatible()) {
      Common.showErrorMessageAndExit(Globals.WELCOME_MSG
          + "\nThe " + action.getName() + " of " + Globals.APP_NAME
          + " is not available for your platform.");

    }

    // Run action
    action.action(arguments);

  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private MainHadoop() {
  }

}
