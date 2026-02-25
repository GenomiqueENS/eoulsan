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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.core;

import fr.ens.biologie.genomique.eoulsan.AbstractEoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Settings;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;
import java.io.File;
import java.util.logging.Logger;

public interface StepConfigurationContext {

  /**
   * Get the command name.
   *
   * @return the command name
   */
  String getCommandName();

  /**
   * Get the UUID of the job.
   *
   * @return the job UUID
   */
  String getJobUUID();

  /**
   * Get the job description.
   *
   * @return the job description
   */
  String getJobDescription();

  /**
   * Get the job environment.
   *
   * @return the job environment
   */
  String getJobEnvironment();

  /**
   * Get command description.
   *
   * @return the command description
   */
  String getCommandDescription();

  /**
   * Get the command author.
   *
   * @return the command author
   */
  String getCommandAuthor();

  /**
   * Get the output path.
   *
   * @return Returns the output Path
   */
  DataFile getOutputDirectory();

  /**
   * Get the job path.
   *
   * @return Returns the log Path
   */
  DataFile getJobDirectory();

  /**
   * Get the step working directory.
   *
   * @return Returns the step working directory
   */
  DataFile getStepOutputDirectory();

  /**
   * Get the job id.
   *
   * @return the job id
   */
  String getJobId();

  /**
   * Get the host of the job.
   *
   * @return a string with the host of the job
   */
  String getJobHost();

  /**
   * Get the design file path.
   *
   * @return the design file path
   */
  DataFile getDesignFile();

  /**
   * Get the workflow file path.
   *
   * @return the workflow file path
   */
  DataFile getWorkflowFile();

  /**
   * Get EoulsanRuntime.
   *
   * @return the EoulsanRuntime
   */
  AbstractEoulsanRuntime getRuntime();

  /**
   * Get Eoulsan settings.
   *
   * @return the Settings
   */
  Settings getSettings();

  /**
   * Get the logger.
   *
   * @return the logger
   */
  Logger getLogger();

  /**
   * Get the generic logger.
   *
   * @return the generic logger
   */
  GenericLogger getGenericLogger();

  /**
   * Get the current step.
   *
   * @return the current Step or null if no Step is currently running.
   */
  Step getCurrentStep();

  /**
   * Get local temporary directory.
   *
   * @return the local temporary directory
   */
  File getLocalTempDirectory();
}
