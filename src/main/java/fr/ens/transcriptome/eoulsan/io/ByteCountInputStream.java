package fr.ens.transcriptome.eoulsan.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This filter class allow to count the number of bytes read by an inputStream.
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

    if (attemptNRead < 0)
      return;

    if (this.nRead != attemptNRead)
      throw new IOException("Error read "
          + this.nRead + " bytes, attempted: " + this.attemptNRead + " bytes");
  }

  @Override
  public int read() throws IOException {

    final int c = in.read();
    if (c >= 0)
      nRead++;

    return c;
  }

  @Override
  public int read(final byte b[], final int off, final int len)
      throws IOException {

    final int nr = this.in.read(b, off, len);

    if (nr > 0)
      this.nRead += nr;

    return nr;
  }

  @Override
  public int read(final byte b[]) throws IOException {

    final int nr = in.read(b);

    if (nr > 0)
      this.nRead += nr;

    return nr;
  }

  @Override
  public synchronized void reset() throws IOException {

    this.in.reset();
    this.nRead = size - this.in.available();
  }

  @Override
  public long skip(final long n) throws IOException {

    final long nr = this.in.skip(n);

    if (nr > 0)
      this.nRead += nr;

    return nr;
  }

  //
  // Other methods
  //

  /**
   * Get the number of bytes read.
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
   * @param is inputStream
   */
  public ByteCountInputStream(final InputStream is, final long attemptNRead) {

    this(is);
    this.attemptNRead = attemptNRead;
  }

}
