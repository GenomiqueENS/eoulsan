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
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

/**
 * This class allow to create input and output stream for compression codecs of the Apache Common
 * Compression library.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ApacheCommonCompressionCodecs {

  //
  // InputStreams
  //

  /**
   * Create a bzip2 input stream.
   *
   * @param is input stream
   * @return an uncompressed input stream
   * @throws IOException if an error occurs while creating the input stream
   */
  public static InputStream createBZip2InputStream(final InputStream is) throws IOException {

    return new BZip2CompressorInputStream(is);
  }

  //
  // OutputStreams
  //

  /**
   * Create a bzip2 output stream.
   *
   * @param os the output stream to compress
   * @return a compressed output stream
   * @throws IOException if an error occurs while creating the output stream
   */
  public static OutputStream createBZip2OutputStream(final OutputStream os) throws IOException {

    return new BZip2CompressorOutputStream(os);
  }
}
