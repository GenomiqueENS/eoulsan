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

package fr.ens.transcriptome.eoulsan.core;

public interface ExecutorInfo {

  /**
   * Get the base path
   * @return Returns the basePath
   */
  public String getBasePathname();

  /**
   * Get the log path
   * @return Returns the log Path
   */
  public String getLogPathname();

  /**
   * Get the output path
   * @return Returns the output Path
   */
  public String getOutputPathname();

  /**
   * Get the execution name.
   * @return the execution name
   */
  public String getExecutionName();

}
