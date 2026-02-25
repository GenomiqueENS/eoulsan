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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This class define a filter that count the number of written bytes by an InputStream.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public class ByteCountOutputStream extends FilterOutputStream {

  private long nWritten;
  private long attemptedNWritten = -1;
  private boolean currentWrite;

  //
  // OutputStream methods
  //

  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {

    if (b == null) {
      throw new NullPointerException("the array of bytes argument cannot be null");
    }

    boolean add = false;
    if (!this.currentWrite) {
      add = true;
    }

    this.currentWrite = true;

    super.write(b, off, len);

    if (add) {
      this.nWritten += len;
      this.currentWrite = false;
    }
  }

  @Override
  public void write(final byte[] b) throws IOException {

    if (b == null) {
      throw new NullPointerException("the array of bytes argument cannot be null");
    }

    boolean add = false;
    if (!this.currentWrite) {
      add = true;
    }
    this.currentWrite = true;

    super.write(b);
    if (add) {
      this.nWritten += b.length;
      this.currentWrite = false;
    }
  }

  @Override
  public void write(final int b) throws IOException {

    boolean add = false;
    if (!this.currentWrite) {
      add = true;
    }

    this.currentWrite = true;

    super.write(b);
    if (add && this.currentWrite) {
      this.nWritten++;
      this.currentWrite = false;
    }
  }

  @Override
  public void close() throws IOException {

    super.close();

    if (this.attemptedNWritten < 0) {
      return;
    }

    if (this.nWritten != this.attemptedNWritten) {
      throw new IOException(
          "Error wrote "
              + this.nWritten
              + " bytes, attempted: "
              + this.attemptedNWritten
              + " bytes.");
    }
  }

  //
  // Other methods
  //

  /**
   * Get the number of bytes written.
   *
   * @return the number of bytes written
   */
  public long getBytesNumberWritten() {

    return this.nWritten;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   *
   * @param os output stream
   */
  public ByteCountOutputStream(final OutputStream os) {

    super(os);
  }

  /**
   * Public constructor.
   *
   * @param os output stream
   * @param attemptedNWritten attempt write count
   */
  public ByteCountOutputStream(final OutputStream os, final long attemptedNWritten) {

    this(os);
    this.attemptedNWritten = attemptedNWritten;
  }
}
