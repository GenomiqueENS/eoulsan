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

package fr.ens.biologie.genomique.eoulsan.core.workflow;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class SerializableStopwatch implements Serializable {

  private static final long serialVersionUID = 75544289032075405L;

  private Stopwatch stopWatch;

  public boolean isRunning() {

    return this.stopWatch.isRunning();
  }

  public Stopwatch stop() {

    return this.stopWatch.stop();
  }

  public Stopwatch start() {

    return this.stopWatch.start();
  }

  public long elapsed(final TimeUnit timeUnit) {

    return this.stopWatch.elapsed(timeUnit);
  }

  //
  // Serialization methods
  //

  private void writeObject(ObjectOutputStream s) throws IOException {}

  private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {

    this.stopWatch = Stopwatch.createUnstarted();
  }

  //
  // Constructor
  //

  public SerializableStopwatch() {

    this.stopWatch = Stopwatch.createUnstarted();
  }
}
