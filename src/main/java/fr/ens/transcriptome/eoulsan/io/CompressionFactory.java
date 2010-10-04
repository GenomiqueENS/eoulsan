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

package fr.ens.transcriptome.eoulsan.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import fr.ens.transcriptome.eoulsan.util.SystemUtils;

/**
 * This allow to create InputStreams and OutputStream for Gzip and Bzip2
 * according environment (local or hadoop mode).
 * @author Laurent Jourdren
 */
public class CompressionFactory {

  //
  // InputStreams
  //

  public static InputStream createGZInputStream(final InputStream is)
      throws IOException {

    return new GZIPInputStream(is);
  }

  public static InputStream createBZip2InputStream(final InputStream is)
      throws IOException {

    if (SystemUtils
        .isClass("org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream"))
      return new LocalBZip2InputStream(is);

    if (SystemUtils.isClass("org.apache.hadoop.io.compress.BZip2Codec"))
      return new HadoopBZip2InputStream(is);

    throw new IOException(
        "Unable to find a class to create a BZip2InputStream.");
  }

  //
  // OutputStream
  //

  public static OutputStream createGZipOutputStream(final OutputStream os)
      throws IOException {

    return new GZIPOutputStream(os);
  }

}
