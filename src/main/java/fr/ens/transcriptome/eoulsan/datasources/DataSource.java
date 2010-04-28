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
 * of the �cole Normale Sup�rieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.datasources;

import java.io.InputStream;
import java.util.Properties;

/**
 * This interface define a source of data for designs.
 * @author Laurent Jourdren
 */
public interface DataSource {

  /**
   * Get the source type (file, database...)
   * @return The type of source
   */
  String getSourceType();

  /**
   * Get a message that describe the source.
   * @return a message that describe the source
   */
  String getSourceInfo();

  /**
   * Configure the source with properties
   * @param properties Properties to config the source
   */
  void configSource(Properties properties);

  /**
   * Configure the source with a string
   * @param config String to configure the source
   */
  void configSource(String config);

  /**
   * Get the inputStream for the source
   * @return The input stream
   */
  InputStream getInputStream();

}
