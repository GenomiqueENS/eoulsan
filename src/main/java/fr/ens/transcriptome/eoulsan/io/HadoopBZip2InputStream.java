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

import org.apache.hadoop.io.compress.BZip2Codec;

/**
 * This class define an input stream for Bzip2 files in hadoop mode.
 * @author Laurent Jourdren
 */
public class HadoopBZip2InputStream extends InputStream {

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
    // TODO Auto-generated method stub
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
  public HadoopBZip2InputStream(final InputStream is) throws IOException {

    BZip2Codec codec = new BZip2Codec();
    this.is = codec.createInputStream(is);
  }

}
