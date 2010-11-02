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
