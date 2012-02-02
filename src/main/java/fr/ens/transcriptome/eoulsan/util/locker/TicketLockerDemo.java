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

package fr.ens.transcriptome.eoulsan.util.locker;

import java.io.IOException;
import java.util.Random;

/**
 * This class is a test class for the locker.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class TicketLockerDemo implements Runnable {

  public void run() {

    try {
      final Locker lock = new TicketLocker("test", 9999);

      System.out.println("Thread "
          + Thread.currentThread().getId() + ",before lock()");
      long t = System.currentTimeMillis();

      // Lock
      lock.lock();

      System.out.println("Thread "
          + Thread.currentThread().getId() + ", after lock(), wait "
          + (System.currentTimeMillis() - t) + " ms.");

      final Random ran = new Random(System.currentTimeMillis());
      final int time = ran.nextInt(120);

      System.out.println("Thread "
          + Thread.currentThread().getId() + ", start wait " + time
          + " seconds");

      try {
        Thread.sleep(time * 1000);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      System.out.println("Thread "
          + Thread.currentThread().getId() + ", end wait " + time + " seconds");

      // Unlock
      lock.unlock();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public static void main(String[] args) throws InterruptedException {

    final Random ran = new Random(System.currentTimeMillis());

    final int threadCount = ran.nextInt(5) + 1;
    System.out.println("Create " + threadCount + " threads.");
    for (int i = 0; i < threadCount; i++) {

      new Thread(new TicketLockerDemo()).start();
      Thread.sleep(ran.nextInt(15));
    }

  }

}
