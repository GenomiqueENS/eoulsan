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
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.io;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

/**
 * This class define an output stream for Bzip2 files in local mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class LocalBZip2OutputStream extends OutputStream {

  private OutputStream os;

  @Override
  public void close() throws IOException {

    this.os.close();
  }

  @Override
  public void flush() throws IOException {

    this.os.flush();
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {

    this.os.write(b, off, len);
  }

  @Override
  public void write(byte[] b) throws IOException {

    this.os.write(b);
  }

  @Override
  public void write(int b) throws IOException {

    this.os.write(b);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param os outputStream
   */
  public LocalBZip2OutputStream(final OutputStream os) throws IOException {

    this.os = new BZip2CompressorOutputStream(os);
  }

}
