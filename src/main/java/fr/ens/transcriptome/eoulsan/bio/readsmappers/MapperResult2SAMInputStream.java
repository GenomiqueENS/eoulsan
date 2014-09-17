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

package fr.ens.transcriptome.eoulsan.bio.readsmappers;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 * This class allow to transform the output of a mapper into SAM format.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class MapperResult2SAMInputStream extends FilterInputStream {

  private byte[] buffer = new byte[0];
  private int pos;
  private final BufferedReader reader;
  private boolean endStream;

  private static final Charset CHARSET = Charsets.ISO_8859_1;
  private final StringBuilder sb = new StringBuilder();

  protected List<String> transform(final String s) {

    if (s == null)
      return null;

    return Lists.newArrayList(Splitter.on('\t').split(s));
  }

  private boolean fillBuffer(final int minSize) throws IOException {

    final int finalPos = this.pos + minSize;

    if (finalPos < this.buffer.length)
      return false;

    if (this.endStream)
      return true;

    this.sb.setLength(0);
    this.sb.append(new String(this.buffer, this.pos, this.buffer.length
        - this.pos));

    do {
      String line = this.reader.readLine();
      final List<String> lines;

      if (line == null) {
        lines = transform(null);
        this.endStream = true;
      } else {
        lines = transform(line);
      }

      if (lines != null)
        for (String l : lines) {
          this.sb.append(l);
          this.sb.append('\n');
        }

    } while (sb.length() < minSize || this.endStream);

    this.buffer = this.sb.toString().getBytes(CHARSET);
    this.pos = 0;

    return false;
  }

  @Override
  public void close() throws IOException {

    this.reader.close();
  }

  @Override
  public int read() throws IOException {

    if (fillBuffer(1))
      return -1;

    return buffer[pos++];
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {

    if (len < 1)
      throw new IllegalArgumentException("len must be > 0");

    if (fillBuffer(len))
      return -1;

    final int copyLen = Math.min(len, this.buffer.length - this.pos);

    System.arraycopy(this.buffer, pos, b, off, copyLen);
    this.pos += copyLen;

    return copyLen;
  }

  @Override
  public int available() throws IOException {

    return this.buffer.length - this.pos;
  }

  @Override
  public synchronized void mark(final int readlimit) {
  }

  @Override
  public boolean markSupported() {

    return false;
  }

  @Override
  public synchronized void reset() throws IOException {
    throw new IOException("mark/reset not supported");
  }

  @Override
  public long skip(final long n) throws IOException {
    // TODO Auto-generated method stub
    return super.skip(n);
  }

  //
  // Constructor
  //

  protected MapperResult2SAMInputStream(final InputStream in) {

    super(in);
    this.reader =
        new BufferedReader(new InputStreamReader(this.in, Charsets.ISO_8859_1));

  }

}
