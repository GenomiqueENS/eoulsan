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

package fr.ens.biologie.genomique.eoulsan.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import fr.ens.biologie.genomique.eoulsan.util.StringUtils;
import fr.ens.biologie.genomique.eoulsan.util.SystemUtils;

/**
 * This enum allow to create InputStreams and OutputStream for Gzip and Bzip2
 * according environment (local or hadoop mode).
 * @since 1.0
 * @author Laurent Jourdren
 */
public enum CompressionType {

  GZIP("gzip", ".gz"), BZIP2("bzip2", ".bz2"), NONE("", "");

  private final String contentEncoding;
  private final String extension;

  //
  // Getters
  //

  /**
   * Get the content encoding for this type
   * @return a String with the content type
   */
  public String getContentEncoding() {

    return this.contentEncoding;
  }

  /**
   * Get the extension for this type
   * @return a String with the extension
   */
  public String getExtension() {

    return this.extension;
  }

  /**
   * Test if a file is compressed.
   * @return true if the file is compressed
   */
  public boolean isCompressed() {

    return this != NONE;
  }

  //
  // Other methods
  //

  /**
   * Get the compression input stream required by a content encoding
   * @param is the input stream
   * @return an input stream
   * @throws IOException if an error occurs while creating the input stream
   */
  public InputStream createInputStream(final InputStream is)
      throws IOException {

    if (is == null) {
      return null;
    }

    switch (this) {

    case GZIP:
      return createGZipInputStream(is);

    case BZIP2:
      return createBZip2InputStream(is);

    case NONE:
      return is;

    default:
      return null;

    }

  }

  /**
   * Get the compression output stream required by a content encoding.
   * @param os the output stream
   * @return an output stream
   * @throws IOException if an error occurs while creating the input stream
   */
  public OutputStream createOutputStream(final OutputStream os)
      throws IOException {

    if (os == null) {
      return null;
    }

    switch (this) {

    case GZIP:
      return createGZipOutputStream(os);

    case BZIP2:
      return createBZip2OutputStream(os);

    case NONE:
      return os;

    default:
      return null;

    }

  }

  //
  // Static methods
  //

  /**
   * Get a compression type from the content encoding.
   * @param contentType the contentType to search
   * @return the requested CompressionType
   */
  public static CompressionType getCompressionTypeByContentEncoding(
      final String contentType) {

    if (contentType == null) {
      return null;
    }

    for (CompressionType ct : CompressionType.values()) {
      if (contentType.equals(ct.contentEncoding)) {
        return ct;
      }
    }

    return NONE;
  }

  /**
   * Get a compression type from an extension.
   * @param extension the extension of the file
   * @return the requested CompressionType
   */
  public static CompressionType getCompressionTypeByExtension(
      final String extension) {

    if (extension == null) {
      return null;
    }

    for (CompressionType ct : CompressionType.values()) {
      if (extension.toLowerCase().equals(ct.extension)) {
        return ct;
      }
    }

    return NONE;
  }

  /**
   * Get a compression type from a filename
   * @param filename the name of the file
   * @return the requested CompressionType
   */
  public static CompressionType getCompressionTypeByFilename(
      final String filename) {

    if (filename == null) {
      return null;
    }

    return getCompressionTypeByExtension(StringUtils.extension(filename));
  }

  /**
   * Create a GZip input stream.
   * @param is the input stream to uncompress
   * @return a uncompressed input stream
   * @throws IOException if an error occurs while creating the input stream
   */
  public static InputStream createGZipInputStream(final InputStream is)
      throws IOException {

    return new GZIPInputStream(is);
  }

  /**
   * Create a BZip2 input stream.
   * @param is the input stream to uncompress
   * @return a uncompressed input stream
   * @throws IOException if an error occurs while creating the input stream
   */
  public static InputStream createBZip2InputStream(final InputStream is)
      throws IOException {

    if (SystemUtils.isClass(
        "org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream")) {
      return ApacheCommonCompressionCodecs.createBZip2InputStream(is);
    }

    if (SystemUtils.isClass("org.apache.hadoop.io.compress.BZip2Codec")) {
      return HadoopCompressionCodecs.createBZip2InputStream(is);
    }

    throw new IOException(
        "Unable to find a class to create a BZip2InputStream.");
  }

  //
  // OutputStream
  //

  /**
   * Create a GZip output stream.
   * @param os the output stream to compress
   * @return a compressed output stream
   * @throws IOException if an error occurs while creating the output stream
   */
  public static OutputStream createGZipOutputStream(final OutputStream os)
      throws IOException {

    return new GZIPOutputStream(os);
  }

  /**
   * Create a BZip2 output stream.
   * @param os the output stream to compress
   * @return a compressed output stream
   * @throws IOException if an error occurs while creating the output stream
   */
  public static OutputStream createBZip2OutputStream(final OutputStream os)
      throws IOException {

    if (SystemUtils.isClass(
        "org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream")) {
      return ApacheCommonCompressionCodecs.createBZip2OutputStream(os);
    }

    if (SystemUtils.isClass("org.apache.hadoop.io.compress.BZip2Codec")) {
      return HadoopCompressionCodecs.createBZip2OutputStream(os);
    }

    throw new IOException(
        "Unable to find a class to create a BZip2InputStream.");
  }

  //
  // Other methods
  //

  /**
   * Remove the compression extension to a string if exists.
   * @param s String to process
   * @return the String without the compression extension or the original String
   *         id the String does not ends with a compression extension
   */
  public static String removeCompressionExtension(final String s) {

    if (s == null) {
      return null;
    }

    for (CompressionType ct : CompressionType.values()) {

      if (ct == NONE) {
        continue;
      }

      if (s.endsWith(ct.getExtension())) {

        return s.substring(0, s.length() - ct.getExtension().length());
      }
    }

    return s;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param contentEncoding content encoding of the compression
   * @param extension extension for the compression
   */
  CompressionType(final String contentEncoding, final String extension) {

    this.contentEncoding = contentEncoding;
    this.extension = extension;
  }

}
