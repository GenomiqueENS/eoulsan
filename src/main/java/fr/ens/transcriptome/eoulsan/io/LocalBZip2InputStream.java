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

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * This class define an input stream for Bzip2 files in local mode.
 * @author Laurent Jourdren
 */
public class LocalBZip2InputStream extends InputStream {

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

  private InputStream is;

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
