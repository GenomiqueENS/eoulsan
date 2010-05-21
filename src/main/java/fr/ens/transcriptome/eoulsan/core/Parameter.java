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

import org.apache.hadoop.conf.Configuration;

import fr.ens.transcriptome.eoulsan.Globals;

public class Parameter {

  /**
   * Get an integer parameter value
   * @param conf Configuration
   * @param parameterName Name of the parameter
   * @param errorMessage Error message
   * @param defaultValue default value of the parameter if not set
   * @return the parameter
   * @throws IOException if the parameter does not exists or if its value is
   *           invalid
   */
  public static int getInt(final Configuration conf,
      final String parameterName, final String errorMessage) throws IOException {

    final String key = Globals.PARAMETER_PREFIX + parameterName;
    final String value = conf.get(key);

    if (value == null)
      throw new IOException(errorMessage != null
          ? errorMessage : "Unable to find parameter: " + key);

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new IOException(errorMessage != null
          ? errorMessage : "Invalid parameter value: " + key);
    }

  }

  /**
   * Get an integer parameter value
   * @param conf Configuration
   * @param parameterName Name of the parameter
   * @param errorMessage Error message
   * @param defaultValue default value of the parameter if not set
   * @return the parameter
   * @throws IOException if the parameter does not exists or if its value is
   *           invalid
   */
  public static int getIntParameter(final Configuration conf,
      final String parameterName, final String errorMessage,
      final int defaultValue) throws IOException {

    final String key = Globals.PARAMETER_PREFIX + parameterName;
    final String value = conf.get(key, "" + defaultValue);

    if (value == null)
      throw new IOException(errorMessage != null
          ? errorMessage : "Unable to find parameter: " + key);

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new IOException(errorMessage != null
          ? errorMessage : "Invalid parameter value: " + key);
    }

  }

  /**
   * Get an String parameter value
   * @param conf Configuration
   * @param parameterName Name of the parameter
   * @param errorMessage Error message
   * @param defaultValue default value of the parameter if not set
   * @return the parameter
   * @throws IOException if the parameter does not exists or if its value is
   *           invalid
   */
  public static String getStringParameter(final Configuration conf,
      final String parameterName, final String errorMessage,
      final String defaultValue) throws IOException {

    final String key = Globals.PARAMETER_PREFIX + parameterName;
    final String value = conf.get(key, defaultValue);

    if (value == null)
      throw new IOException(errorMessage != null
          ? errorMessage : "Unable to find parameter: " + key);

    return value;

  }

  /**
   * Get an String parameter value
   * @param conf Configuration
   * @param parameterName Name of the parameter
   * @param errorMessage Error message
   * @return the parameter
   * @throws IOException if the parameter does not exists or if its value is
   *           invalid
   */
  public static String getStringParameter(final Configuration conf,
      final String parameterName, final String errorMessage) throws IOException {

    final String key = Globals.PARAMETER_PREFIX + parameterName;
    final String value = conf.get(key);

    if (value == null)
      throw new IOException(errorMessage != null
          ? errorMessage : "Unable to find parameter: " + key);

    return value;

  }

}
