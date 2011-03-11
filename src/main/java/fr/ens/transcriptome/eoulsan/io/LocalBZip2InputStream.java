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
import java.io.InputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * This class define an input stream for Bzip2 files in local mode.
 * @author Laurent Jourdren
 */
public class LocalBZip2InputStream extends InputStream {

  private InputStream is;

  @Override
  public int available() throws IOException {

    return is.available();
  }

  @Override
  public void close() throws IOException {

    is.close();
  }

  @Override
  public synchronized void mark(int readlimit) {

    is.mark(readlimit);
  }

  @Override
  public boolean markSupported() {

    return is.markSupported();
  }

  @Override
  public int read() throws IOException {

    return is.read();
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {

    return is.read(b, off, len);
  }

  @Override
  public int read(byte[] b) throws IOException {

    return is.read(b);
  }

  @Override
  public synchronized void reset() throws IOException {

    super.reset();
  }

  @Override
  public long skip(long n) throws IOException {

    return is.skip(n);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param is InputStream
   */
  public LocalBZip2InputStream(final InputStream is) throws IOException {

    this.is = new BZip2CompressorInputStream(is);
  }

}
