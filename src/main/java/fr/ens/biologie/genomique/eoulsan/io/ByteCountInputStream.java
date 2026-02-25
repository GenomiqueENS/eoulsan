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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This filter class allow to count the number of bytes read by an inputStream.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public class ByteCountInputStream extends FilterInputStream {

  private long nRead = 0;
  private long size = 0;
  private long attemptNRead = -1;

  //
  // InputStream methods
  //

  @Override
  public void close() throws IOException {

    this.in.close();

    if (this.attemptNRead < 0) {
      return;
    }

    if (this.nRead != this.attemptNRead) {
      throw new IOException(
          "Error read " + this.nRead + " bytes, attempted: " + this.attemptNRead + " bytes");
    }
  }

  @Override
  public int read() throws IOException {

    final int c = this.in.read();
    if (c >= 0) {
      this.nRead++;
    }

    return c;
  }

  @Override
  public int read(final byte[] b, final int off, final int len) throws IOException {

    final int nr = this.in.read(b, off, len);

    if (nr > 0) {
      this.nRead += nr;
    }

    return nr;
  }

  @Override
  public int read(final byte[] b) throws IOException {

    final int nr = this.in.read(b);

    if (nr > 0) {
      this.nRead += nr;
    }

    return nr;
  }

  @Override
  public synchronized void reset() throws IOException {

    this.in.reset();
    this.nRead = this.size - this.in.available();
  }

  @Override
  public long skip(final long n) throws IOException {

    final long nr = this.in.skip(n);

    if (nr > 0) {
      this.nRead += nr;
    }

    return nr;
  }

  //
  // Other methods
  //

  /**
   * Get the number of bytes read.
   *
   * @return the number of bytes read
   */
  public long getBytesRead() {

    return this.nRead;
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   *
   * @param is inputStream
   */
  public ByteCountInputStream(final InputStream is) {

    super(is);

    try {
      this.size = is.available();
    } catch (IOException ioe) {
      this.size = 0;
    }
  }

  /**
   * Public constructor
   *
   * @param is inputStream
   * @param attemptNRead attempt read count
   */
  public ByteCountInputStream(final InputStream is, final long attemptNRead) {

    this(is);
    this.attemptNRead = attemptNRead;
  }
}
