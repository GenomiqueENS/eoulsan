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

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.util.List;

import com.google.common.base.Joiner;

import fr.ens.transcriptome.eoulsan.Globals;

/**
 * This class define a fake action that is called when user launch EMRExecAction
 * with its old name.
 * @since 1.2
 * @author Laurent Jourdren
 */
public class AWSExecAction extends AbstractAction {

  /** Name of this action. */
  public static final String ACTION_NAME = "awsexec";

  @Override
  public boolean isHidden() {

    return true;
  }

  @Override
  public String getName() {

    return ACTION_NAME;
  }

  @Override
  public String getDescription() {

    return "Fake action";
  }

  @Override
  public void action(final List<String> arguments) {

    getLogger().severe("The \""
        + getName() + "\" action has been renamed to \""
        + EMRExecAction.ACTION_NAME + "\".");

    System.err.println("The \""
        + getName() + "\" action has been renamed to \""
        + EMRExecAction.ACTION_NAME + "\". Please relaunch " + Globals.APP_NAME
        + " with the following command:\n\t " + Globals.APP_NAME_LOWER_CASE
        + ".sh " + EMRExecAction.ACTION_NAME + " "
        + Joiner.on(' ').join(arguments));

  }
}
