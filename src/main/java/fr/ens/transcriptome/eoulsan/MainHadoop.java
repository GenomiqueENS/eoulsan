/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
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
