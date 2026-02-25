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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.hadoop.mapreduce.Counter;

/**
 * This class implements a FilterInputStream that inform Hadoop of the progress of task using
 * counters.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class ProgressCounterInputStream extends FilterInputStream {

  private static final int MAX = 10 * 1024 * 1024;

  private final Counter counter;
  private int sum;

  @Override
  public final int read() throws IOException {

    return incrementCounter(super.read());
  }

  @Override
  public final int read(final byte[] b, final int off, final int len) throws IOException {

    return incrementCounter(super.read(b, off, len));
  }

  @Override
  public final int read(final byte[] b) throws IOException {

    return incrementCounter(super.read(b));
  }

  @Override
  public final void close() throws IOException {

    super.close();
    this.counter.increment(this.sum);
  }

  //
  // Other methods
  //

  private int incrementCounter(final int bytes) {

    this.sum += bytes;

    if (this.sum > MAX) {
      this.counter.increment(this.sum);
      this.sum = 0;
    }

    return bytes;
  }

  //
  // Constructor
  //

  public ProgressCounterInputStream(final InputStream is, final Counter counter) {

    super(is);

    if (counter == null) {
      throw new NullPointerException("The counter to use is null.");
    }

    this.counter = counter;
  }
}
