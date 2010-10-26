/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.mapreduce.Counter;

/**
 * This class implements a FilterInputStream that inform Hadoop of the progress
 * of task using counters.
 * @author Laurent Jourdren
 */
public class ProgressCounterInputStream extends FilterInputStream {

  private static final int MAX = 10 * 1024 * 1024;

  private Counter counter;
  private int sum;

  @Override
  public int read() throws IOException {

    return incrementCounter(super.read());
  }

  @Override
  public int read(final byte[] b, final int off, final int len)
      throws IOException {

    return incrementCounter(super.read(b, off, len));
  }

  @Override
  public int read(final byte[] b) throws IOException {

    return incrementCounter(super.read(b));
  }

  @Override
  public void close() throws IOException {

    super.close();
    incrementCounter(0);
  }

  //
  // Other methods
  //

  private int incrementCounter(final int bytes) {

    this.sum += bytes;

    if (bytes == 0 || this.sum > MAX) {
      counter.increment(this.sum);
      this.sum = 0;
    }

    return bytes;
  }

  //
  // Constructor
  //

  public ProgressCounterInputStream(final InputStream is, final Counter counter) {

    super(is);

    if (counter == null)
      throw new NullPointerException("The counter to use is null.");

    this.counter = counter;

  }

}
