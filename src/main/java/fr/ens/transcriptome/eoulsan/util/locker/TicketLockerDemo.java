package fr.ens.transcriptome.eoulsan.util.locker;

import java.io.IOException;
import java.util.Random;

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
