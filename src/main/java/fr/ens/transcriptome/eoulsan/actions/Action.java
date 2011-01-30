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

package fr.ens.transcriptome.eoulsan.actions;

/**
 * This interface define an action.
 * @author Laurent Jourdren
 */
public interface Action {

  /**
   * Get the name of the action.
   * @return the name of the action
   */
  public String getName();

  /**
   * Get action description.
   * @return the description description
   */
  public String getDescription();

  /**
   * Execute action.
   * @param arguments arguments of the action.
   */
  public void action(String[] arguments);

  /**
   * Test if the action can be executed in hadoop Jar mode.
   * @return true if the action can be executed in hadoop Jar mode
   */
  public boolean isHadoopJarMode();

  /**
   * Test if the action can be executed with current platform.
   * @return true if the action can be executed with current platform
   */
  public boolean isCurrentArchCompatible();

  /**
   * Test if the action must be hidden from the list of available actions.
   * @return true if the action must be hidden
   */
  public boolean isHidden();

}
