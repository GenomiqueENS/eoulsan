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

package fr.ens.transcriptome.eoulsan.datasources;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;

/**
 * This class provides utility methods for DataSource.
 * @author Laurent Jourdren
 */
public final class DataSourceUtils {

  /**
   * Identify the type of the DataSource from the source.
   * @param baseDir baseDir of the source if this a file
   * @param source source to identify
   * @return a new DataSource object
   */
  public static DataSource identifyDataSource(final String source) {

    return identifyDataSource(System.getProperty("user.dir"), source);
  }

  /**
   * Identify the type of the DataSource from the source.
   * @param baseDir baseDir of the source if this a file
   * @param source source to identify
   * @return a new DataSource object
   */
  public static DataSource identifyDataSource(final String baseDir,
      final String source) {

    if (source == null)
      return null;

    if (source.startsWith(Common.S3_PROTOCOL + "://")
        && EoulsanRuntime.getRuntime().isHadoopMode())
      return new S3DataSource(source);

    if (source.startsWith("file:/"))
      return new FileDataSource(new File(source.substring(5)));

    boolean malformedURL = false;

    try {
      new URL(source);
    } catch (MalformedURLException e) {
      malformedURL = true;
    }

    if (!malformedURL)
      return new URLDataSource(source);

    return new FileDataSource(baseDir, source);
  }

  //
  // Constructor
  //

  private DataSourceUtils() {
  }
}
