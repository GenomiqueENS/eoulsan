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

package fr.ens.transcriptome.eoulsan;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import fr.ens.transcriptome.eoulsan.io.CompressionFactory;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define an absract EoulsanRuntime.
 * @author Laurent Jourdren
 */
public abstract class AbstractEoulsanRuntime {

  private Settings settings;

  /**
   * Get Settings.
   * @return a Settings object
   */
  public Settings getSettings() {

    return this.settings;
  }

  /**
   * Test if Eoulsan runs in Hadoop mode.
   * @return true if Eoulsan runs in Hadoop mode
   */
  public abstract boolean isHadoopMode();

  /**
   * Test if Eoulsan runs in an Amazon MapReduce cluster.
   * @return true if Eoulsan in an Amazon MapReduce cluster
   */
  public abstract boolean isAmazonMode();

  /**
   * Create an InputStream to load data.
   * @param dataSource the source of the data to load
   * @return an InputStream corresponding to the source
   * @throws IOException if an error occurs the InputStream
   */
  public abstract InputStream getInputStream(String dataSource)
      throws IOException;

  /**
   * Create a raw InputStream (without decompression of input data) to load
   * data.
   * @param dataSource the source of the data to load
   * @return an InputStream corresponding to the source
   * @throws IOException if an error occurs the InputStream
   */
  public abstract InputStream getRawInputStream(String dataSource)
      throws IOException;

  /**
   * Create an OutputStream to load data.
   * @param dataSource the source of the data to load
   * @return an OutputStream corresponding to the source
   * @throws IOException if an error occurs the OutputStream
   */
  public abstract OutputStream getOutputStream(String dataSource)
      throws IOException;

  /**
   * Decompress an inputStream if needed.
   * @param is the InputStream
   * @param source source of the inputStream
   * @return a InputStream with decompression integrated or not
   * @throws IOException if an error occurs while creating decompressor
   *           InputStream
   */
  protected InputStream decompressInputStreamIsNeeded(final InputStream is,
      final String source) throws IOException {

    final String extension = StringUtils.compressionExtension(source);

    if (Common.GZIP_EXTENSION.equals(extension))
      return CompressionFactory.createGZipInputStream(is);

    if (Common.BZIP2_EXTENSION.equals(extension))
      return CompressionFactory.createBZip2InputStream(is);

    return is;
  }

  //
  // Constructor
  //

  protected AbstractEoulsanRuntime(final Settings settings) {

    if (settings == null)
      throw new EoulsanError("The settings are null");

    this.settings = settings;
  }
}
