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

  public static InputStream getCompressionInputStream(final InputStream is,
      final String contentEncoding) throws IOException {

    if (contentEncoding == null)
      throw new IOException("Content encoding is null");

    if (".gz".equals(contentEncoding) || "gz".equals(contentEncoding))
      return createGZipInputStream(is);

    if (".bz2".equals(contentEncoding) || "bz2".equals(contentEncoding))
      return createBZip2InputStream(is);

    throw new IOException(
        "Unable to find a compression input stream for this content encoding: "
            + contentEncoding);
  }

  public static OutputStream getCompressionOutputStream(final OutputStream os,
      final String contentEncoding) throws IOException {

    if (contentEncoding == null)
      throw new IOException("Content encoding is null");

    if (".gz".equals(contentEncoding) || "gz".equals(contentEncoding))
      return createGZipOutputStream(os);

    if (".bz2".equals(contentEncoding) || "bz2".equals(contentEncoding))
      return createBZip2OutputStream(os);

    throw new IOException(
        "Unable to find a compression output stream for this content encoding: "
            + contentEncoding);
  }

  //
  // InputStreams
  //

  public static InputStream createGZipInputStream(final InputStream is)
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

  public static OutputStream createBZip2OutputStream(final OutputStream os)
      throws IOException {

    if (SystemUtils
        .isClass("org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream"))
      return new LocalBZip2OutputStream(os);

    if (SystemUtils.isClass("org.apache.hadoop.io.compress.BZip2Codec"))
      return new HadoopBZip2OutputStream(os);

    throw new IOException(
        "Unable to find a class to create a BZip2InputStream.");
  }

}
