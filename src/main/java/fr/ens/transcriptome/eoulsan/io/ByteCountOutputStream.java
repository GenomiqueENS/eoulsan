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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This class define a filter that count the number of written bytes by an
 * InputSream.
 * @author jourdren
 */
public class ByteCountOutputStream extends FilterOutputStream {

  private long nWritten;
  private long attemptedNWritten = -1;
  private boolean currentWrite;

  //
  // OutputStream methods
  //

  @Override
  public void write(final byte[] b, final int off, final int len)
      throws IOException {

    boolean add = false;
    if (!currentWrite)
      add = true;

    currentWrite = true;

    super.write(b, off, len);
    
    if (add && b != null) {
      nWritten += len;
      currentWrite = false;
    }
  }

  @Override
  public void write(final byte[] b) throws IOException {

    boolean add = false;
    if (!currentWrite)
      add = true;
    currentWrite = true;

    super.write(b);
    if (add && b != null) {
      nWritten += b.length;
      currentWrite = false;
    }
  }

  @Override
  public void write(final int b) throws IOException {

    boolean add = false;
    if (!currentWrite)
      add = true;

    currentWrite = true;

    super.write(b);
    if (add && currentWrite) {
      nWritten++;
      currentWrite = false;
    }
  }

  @Override
  public void close() throws IOException {

    super.close();

    if (attemptedNWritten < 0)
      return;

    if (this.nWritten != attemptedNWritten)
      throw new IOException("Error wrote "
          + this.nWritten + " bytes, attempted: " + this.attemptedNWritten
          + " bytes.");

  }

  //
  // Other methods
  //

  /**
   * Get the number of bytes written.
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
   * @param os output stream
   */
  public ByteCountOutputStream(final OutputStream os) {

    super(os);
  }

  /**
   * Public constructor.
   * @param os output stream
   */
  public ByteCountOutputStream(final OutputStream os,
      final long attemptedNWritten) {

    this(os);
    this.attemptedNWritten = attemptedNWritten;
  }

}
