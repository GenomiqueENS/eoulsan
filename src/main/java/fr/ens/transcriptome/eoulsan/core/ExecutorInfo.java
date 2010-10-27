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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import fr.ens.transcriptome.eoulsan.datatypes.DataType;
import fr.ens.transcriptome.eoulsan.design.Sample;

public interface ExecutorInfo {

  /**
   * Get the command name.
   * @return the command name
   */
  String getCommandName();

  /**
   * Get command description.
   * @return the command description
   */
  String getCommandDescription();

  /**
   * Get the command author.
   * @return the command author
   */
  String getCommandAuthor();

  /**
   * Get the base path.
   * @return Returns the basePath
   */
  String getBasePathname();

  /**
   * Get the log path.
   * @return Returns the log Path
   */
  String getLogPathname();

  /**
   * Get the output path.
   * @return Returns the output Path
   */
  String getOutputPathname();

  /**
   * Get the execution name.
   * @return the execution name
   */
  String getExecutionName();

  /**
   * Get the design path.
   * @return the design path
   */
  String getDesignPathname();

  /**
   * Get the parameter path.
   * @return the parameter path
   */
  String getParameterPathname();

  /**
   * Add executor information to log.
   */
  void logInfo();

  /**
   * Get the pathname for a DataType and a Sample.
   * @param dt the DataType of the source
   * @param sample the sample for the source
   * @return a String with the pathname
   */
  String getPathname(DataType dt, Sample sample);

  /**
   * Create an InputStream to load data.
   * @param dt the DataType of the data to load
   * @param sample the sample
   * @return an InputStream corresponding to DataType and Sample
   */
  InputStream getInputStream(DataType dt, Sample sample) throws IOException;

  /**
   * Create a raw InputStream (without decompression of input data) to load
   * data.
   * @param dt the DataType of the data to load
   * @param sample the sample
   * @return an InputStream corresponding to DataType and Sample
   */
  InputStream getRawInputStream(DataType dt, Sample sample) throws IOException;

  /**
   * Create an OutputStream to load data.
   * @param dt the DataType of the data to write
   * @param sample the sample
   * @return an InputStream corresponding to DataType and Sample
   */
  OutputStream getOutputStream(DataType dt, Sample sample) throws IOException;
}
