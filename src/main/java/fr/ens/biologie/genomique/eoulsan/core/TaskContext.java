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

import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;

/**
 * This interface define the context of a step.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public interface TaskContext extends StepConfigurationContext {

  /**
   * Get the context name.
   *
   * @return a String with the context name
   */
  String getContextName();

  /**
   * Set the context name.
   *
   * @param contextName the name of the context
   */
  void setContextName(String contextName);

  /**
   * Get the creation time of the context.
   *
   * @return the creation time of the context in milliseconds since epoch (1.1.1970)
   */
  long getContextCreationTime();

  /**
   * Get the workflow description
   *
   * @return the workflow description
   */
  Workflow getWorkflow();

  /**
   * Get the input data for an input DataType and a Sample.
   *
   * @param format the DataFormat of the source
   * @return a String with the pathname
   */
  Data getInputData(DataFormat format);

  /**
   * Get the input data for a port name and a Sample.
   *
   * @param portName the name of the port
   * @return a String with the pathname
   */
  Data getInputData(String portName);

  /**
   * Get the output data for an input DataType and a Sample.
   *
   * @param format the DataFormat of the source
   * @param dataName the name of the data
   * @return a String with the pathname
   */
  Data getOutputData(DataFormat format, String dataName);

  /**
   * Get the output data for an input DataType and a Sample.
   *
   * @param format the DataFormat of the source
   * @param dataName the name of the data
   * @param part data part
   * @return a String with the pathname
   */
  Data getOutputData(DataFormat format, String dataName, int part);

  /**
   * Get the output data for an input DataType and a Sample.
   *
   * @param format the DataFormat of the source
   * @param origin origin of the new data
   * @return a String with the pathname
   */
  Data getOutputData(DataFormat format, Data origin);

  /**
   * Get the output data for a port name and a Sample.
   *
   * @param portName the name of the port
   * @param dataName the name of the data
   * @return a String with the pathname
   */
  Data getOutputData(String portName, String dataName);

  /**
   * Get the output data for a port name and a Sample.
   *
   * @param portName the name of the port
   * @param dataName the name of the data
   * @param part data part
   * @return a String with the pathname
   */
  Data getOutputData(String portName, String dataName, int part);

  /**
   * Get the output data for a port name and a Sample.
   *
   * @param portName the name of the port
   * @param origin origin of the new data
   * @return a String with the pathname
   */
  Data getOutputData(String portName, Data origin);
}
