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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.hadoop.mapreduce.Counter;

/**
 * This class implements an OutputStream that inform Hadoop of the progress of
 * task using counters.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class ProgressCounterOutputStream extends OutputStream {

  private static final int MAX = 100 * 1024 * 1024;

  private final OutputStream out;
  private final Counter counter;
  private int sum;

  //
  // Override methods
  //

  @Override
  public final void write(final byte[] b, final int off, final int len)
      throws IOException {

    this.out.write(b, off, len);
    incrementCounter(len);
  }

  @Override
  public final void write(final byte[] b) throws IOException {

    this.out.write(b);
    incrementCounter(b.length);
  }

  @Override
  public final void write(final int b) throws IOException {

    this.out.write(b);
    incrementCounter(1);
  }

  @Override
  public void flush() throws IOException {
    this.out.flush();
  }

  @Override
  public final void close() throws IOException {

    this.out.flush();
    this.out.close();
    this.counter.increment(this.sum);
  }

  //
  // Other methods
  //

  private void incrementCounter(final int bytes) {

    this.sum += bytes;

    if (this.sum > MAX) {
      this.counter.increment(this.sum);
      this.sum = 0;
    }

  }

  //
  // Constructor
  //

  public ProgressCounterOutputStream(final OutputStream os,
      final Counter counter) {

    requireNonNull(os, "OutputStream is null");
    requireNonNull(counter, "The counter to use is null.");

    this.out = os;
    this.counter = counter;
  }

}
