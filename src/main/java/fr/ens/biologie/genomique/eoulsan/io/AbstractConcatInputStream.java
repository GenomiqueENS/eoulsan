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
 * This class define an InputStream that concatenate InputStream.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class AbstractConcatInputStream extends InputStream {

  private InputStream is;

  /**
   * Test if there is an other InputStream to concatenate
   *
   * @return true if an other InputStream is available
   */
  protected abstract boolean hasNextInputStream();

  /**
   * Get the next InputStream to concatenate
   *
   * @return the next InputStream to concatenate
   * @throws IOException if an error occurs while creating InputStream
   */
  protected abstract InputStream nextInputStream() throws IOException;

  @Override
  public boolean markSupported() {
    return false;
  }

  @Override
  public int read() throws IOException {

    if (this.is == null) {

      if (!hasNextInputStream()) {
        return -1;
      }

      this.is = nextInputStream();
    }

    int b = this.is.read();

    if (b == -1) {

      if (!hasNextInputStream()) {
        return -1;
      }

      this.is.close();
      this.is = nextInputStream();

      return read();
    }

    return b;
  }

  @Override
  public int read(final byte[] b, final int off, final int len) throws IOException {

    // No input stream
    if (this.is == null) {

      if (!hasNextInputStream()) {
        return -1;
      }

      this.is = nextInputStream();
    }

    final int l = this.is.read(b, off, len);

    // Nothing to read
    if (l == -1) {

      if (!hasNextInputStream()) {
        return -1;
      }

      this.is.close();
      this.is = nextInputStream();

      return read(b, off, len);
    }

    // read the start of the next stream if needed
    if (l < len) {

      for (int i = l; i < len; i++) {

        final int b2 = read();

        if (b2 == -1) {
          return i;
        }

        b[i] = (byte) b2;
      }
      return len;
    }

    // return the number of byte read
    return l;
  }

  @Override
  public int read(final byte[] b) throws IOException {

    return read(b, 0, b.length);
  }

  @Override
  public void close() throws IOException {

    if (this.is != null) {
      this.is.close();
    }
  }
}
