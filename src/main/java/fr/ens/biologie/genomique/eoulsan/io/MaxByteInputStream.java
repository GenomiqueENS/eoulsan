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

/**
 * This class allow to create an InputStream that can read a maximum of byte from another
 * InputStream. Warning: this class never close the original stream.
 *
 * @since 2.0
 * @author Laurent Jourdren
 */
public class MaxByteInputStream extends InputStream {

  private final InputStream in;
  private final long max;
  private long nRead = 0;

  //
  // InputStream methods
  //

  @Override
  public void close() throws IOException {}

  @Override
  public int read() throws IOException {

    if (this.nRead == this.max) {
      return -1;
    }

    final int c = this.in.read();

    if (c >= 0) {
      this.nRead++;
    }

    return c;
  }

  @Override
  public int read(final byte[] b, final int off, final int len) throws IOException {

    if (this.nRead == this.max) {
      return -1;
    }

    final int l = (this.nRead + len) > this.max ? (int) (this.max - this.nRead) : len;

    final int nr = this.in.read(b, off, l);

    if (nr > 0) {
      this.nRead += nr;
    }

    return nr;
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   *
   * @param in inputStream
   * @param max stream size
   */
  public MaxByteInputStream(final InputStream in, final long max) {

    if (in == null) {
      throw new NullPointerException("InputStream is null");
    }

    if (max < 1) {
      throw new IllegalArgumentException("Max length cannot be lower than 1: " + max);
    }

    this.in = in;
    this.max = max;
  }
}
