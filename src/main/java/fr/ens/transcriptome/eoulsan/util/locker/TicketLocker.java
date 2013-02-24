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
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Set;

/**
 * This class define a global locker using RMI messaging. TODO tune waiting
 * times
 * @since 1.1
 * @author Laurent Jourdren
 */
public class TicketLocker implements Locker {

  static final String RMI_SERVICE_PREFIX = "locker-";

  private LockerThread thread;
  private final String lockerName;
  private final int port;

  /**
   * Define the locker thread that wait the end of the lock.
   * @author Laurent Jourdren
   */
  private final class LockerThread implements Runnable {

    private final Ticket ticket;
    private Set<Ticket> tickets;

    @Override
    public void run() {

      while (true) {

        try {

          final TicketScheduler stub = getStub();

          if (stub != null) {

            this.tickets = stub.getTickets(ticket);

            for (Ticket t : tickets)
              if (t.equals(ticket)) {

                if (t.isWorking())
                  return;
              }
          } else
            startRMIServer(tickets);

        } catch (RemoteException e) {
          // e.printStackTrace();
        }

        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
        }

      }
    }

    /**
     * Stop the thread when release the lock.
     */
    public void end() {

      final TicketScheduler stub = getStub();

      if (stub != null)
        try {
          stub.endWork(ticket);
        } catch (RemoteException e) {

          startRMIServer(tickets);
        }

    }

    /**
     * Private constructor.
     * @param ticket the ticket to wait
     */
    private LockerThread(final Ticket ticket) {

      this.ticket = ticket;
    }

  }

  /**
   * Get the stub of TicketScheduler.
   * @return a TicketScheduler object
   */
  private final TicketScheduler getStub() {

    try {
      Registry registry = LocateRegistry.getRegistry(port);
      return (TicketScheduler) registry.lookup(RMI_SERVICE_PREFIX + lockerName);

    }

    catch (IOException e) {
      return null;
    } catch (NotBoundException e) {
      return null;
    }
  }

  //
  // Locker methods
  //

  @Override
  public void lock() throws IOException {

    if (thread == null)
      thread = new LockerThread(new Ticket());

    final Thread t = new Thread(thread);

    t.start();

    while (t.isAlive())

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }

  }

  @Override
  public void unlock() throws IOException {

    this.thread.end();
  }

  //
  // Other methods
  //

  /**
   * Start a new RMI server.
   * @param tickets a set with tickets to populate the TicketScheduler at
   *          startup
   */
  private final void startRMIServer(final Set<Ticket> tickets) {

    new Thread(new Runnable() {

      @Override
      public void run() {
        try {
          LocateRegistry.createRegistry(port);
          TicketSchedulerServer.newServer(tickets, lockerName, port);
          while (true)
            Thread.sleep(10000);

        } catch (InterruptedException e) {
        } catch (RemoteException e) {
        }

      }
    }).start();

  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param lockerName The name of the locker
   * @param port port to use
   */
  public TicketLocker(final String lockerName, final int port) {

    this.lockerName = lockerName;
    this.port = port;
  }

  //
  // Main method : list current tickets
  //

  /**
   * Main method.
   * @param args command line arguments
   * @throws RemoteException if an error occurs while inspecting tickets
   */
  public static final void main(String[] args) throws RemoteException {

    if (args.length < 2) {

      System.err.println("List current lock tickets\nSyntax: java "
          + TicketLocker.class.getName() + " locker_name server_port");
      return;
    }

    TicketLocker locker = new TicketLocker(args[0], Integer.parseInt(args[1]));

    for (Ticket t : locker.getStub().getTickets(null)) {
      System.out.println(t);
    }

  }
}
