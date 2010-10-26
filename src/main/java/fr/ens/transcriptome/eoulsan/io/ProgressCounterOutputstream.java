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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.hadoop.mapreduce.Counter;

/**
 * This class implements a FilterOutputStream that inform Hadoop of the progress
 * of task using counters.
 * @author Laurent Jourdren
 */
public class ProgressCounterOutputstream extends FilterOutputStream {

  private static final int MAX = 10 * 1024 * 1024;

  private Counter counter;
  private int sum;

  //
  // Override methods
  //

  @Override
  public void write(final byte[] b, final int off, final int len)
      throws IOException {

    super.write(b, off, len);
    incrementCounter(len);
  }

  @Override
  public void write(final byte[] b) throws IOException {

    super.write(b);
    incrementCounter(b.length);
  }

  @Override
  public void write(final int b) throws IOException {

    super.write(b);
    incrementCounter(1);
  }

  @Override
  public void close() throws IOException {

    super.close();
    incrementCounter(0);
  }

  //
  // Other methods
  //

  private final void incrementCounter(final int bytes) {

    this.sum += bytes;

    if (bytes == 0 || this.sum > MAX) {
      counter.increment(this.sum);
      this.sum = 0;
    }

  }

  //
  // Constructor
  //

  public ProgressCounterOutputstream(final OutputStream os,
      final Counter counter) {

    super(os);

    if (counter == null)
      throw new NullPointerException("The counter to use is null.");
    this.counter = counter;
  }

}
