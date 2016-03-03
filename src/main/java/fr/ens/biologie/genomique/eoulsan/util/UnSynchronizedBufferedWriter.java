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

package fr.ens.biologie.genomique.eoulsan.util;

import java.io.IOException;
import java.io.Writer;

/**
 * This class define an unsynchronized buffered writer.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class UnSynchronizedBufferedWriter extends Writer {
  private final static int CAPACITY = 8192;

  private final char[] buffer = new char[CAPACITY];
  private int position = 0;
  private final Writer out;
  private boolean closed = false;

  public UnSynchronizedBufferedWriter(final Writer out) {
    this.out = out;
  }

  @Override
  public void write(final char[] text, int offset, int length)
      throws IOException {
    checkClosed();
    while (length > 0) {
      int n = Math.min(CAPACITY - this.position, length);
      System.arraycopy(text, offset, this.buffer, this.position, n);
      this.position += n;
      offset += n;
      length -= n;
      if (this.position >= CAPACITY) {
        flushInternal();
      }
    }
  }

  @Override
  public void write(final String s) throws IOException {
    write(s, 0, s.length());
  }

  @Override
  public void write(final String s, int offset, int length) throws IOException {
    checkClosed();
    while (length > 0) {
      int n = Math.min(CAPACITY - this.position, length);
      s.getChars(offset, offset + n, this.buffer, this.position);
      this.position += n;
      offset += n;
      length -= n;
      if (this.position >= CAPACITY) {
        flushInternal();
      }
    }
  }

  @Override
  public void write(final int c) throws IOException {
    checkClosed();
    if (this.position >= CAPACITY) {
      flushInternal();
    }
    this.buffer[this.position] = (char) c;
    this.position++;
  }

  @Override
  public void flush() throws IOException {
    flushInternal();
    this.out.flush();
  }

  private void flushInternal() throws IOException {
    if (this.position != 0) {
      this.out.write(this.buffer, 0, this.position);
      this.position = 0;
    }
  }

  @Override
  public void close() throws IOException {
    this.closed = true;
    this.flush();
    this.out.close();
  }

  private void checkClosed() throws IOException {
    if (this.closed) {
      throw new IOException("Writer is closed");
    }
  }
}
