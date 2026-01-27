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

import java.util.Map;

import fr.ens.biologie.genomique.kenetre.util.Reporter;

/**
 * This interface define a step status.
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface TaskStatus extends Progress {

  /**
   * Get the sample counters.
   * @return the sample counters as a map
   */
  Map<String, Long> getCounters();

  /**
   * Get the context description.
   * @return a String with the context description
   */
  String getDescription();

  /**
   * Set the context description.
   * @param description the description to set
   */
  void setDescription(String description);

  /**
   * Get the context command line.
   * @return a String with the context command line
   */
  String getCommandLine();

  /**
   * Set the context command line.
   * @param commandLine the command line to set
   */
  void setCommandLine(String commandLine);

  /**
   * Get the context docker image.
   * @return a String with the context docker image
   */
  String getDockerImage();

  /**
   * Set the context docker image.
   * @param dockerImage the command line to set
   */
  void setDockerImage(String dockerImage);

  /**
   * Set the context counters.
   * @param reporter the reporter
   * @param counterGroup counter group to use with the reporter
   */
  void setCounters(Reporter reporter, String counterGroup);

  /**
   * Create a TaskResult object for a successful result.
   * @return a TaskResult object
   */
  TaskResult createTaskResult();

  /**
   * Create a TaskResult object.
   * @param success true if the task is successful
   * @return a TaskResult object
   */
  TaskResult createTaskResult(boolean success);

  /**
   * Create a TaskResult object.
   * @param exception exception of the error
   * @param exceptionMessage Error message
   * @return a TaskResult object
   */
  TaskResult createTaskResult(Throwable exception, String exceptionMessage);

  /**
   * Create a TaskResult object.
   * @param exception exception of the error
   * @return a TaskResult object
   */
  TaskResult createTaskResult(Throwable exception);

}
