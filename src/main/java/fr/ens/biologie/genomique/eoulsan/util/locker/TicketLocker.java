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

package fr.ens.biologie.genomique.eoulsan.util.locker;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
  private final String description;

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

            this.tickets = stub.getTickets(this.ticket);

            // Safety: if ticket scheduler return one or zero ticket, the locker
            // ticket is allowed to work
            if (this.tickets == null
                || this.tickets.isEmpty() || this.tickets.size() == 1) {

              this.ticket.setWorking(true);
              return;
            }

            // Test if locker ticket is allowed to work
            for (Ticket t : this.tickets) {
              if (t.equals(this.ticket)) {

                if (t.isWorking()) {
                  return;
                }
              }
            }

          } else {
            startRMIServer(this.tickets);
          }

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

      if (stub != null) {
        try {
          stub.endWork(this.ticket);
        } catch (RemoteException e) {

          startRMIServer(this.tickets);
        }
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
  private TicketScheduler getStub() {

    try {
      Registry registry = LocateRegistry.getRegistry(this.port);
      return (TicketScheduler) registry
          .lookup(RMI_SERVICE_PREFIX + this.lockerName);

    }

    catch (IOException | NotBoundException e) {
      return null;
    }
  }

  //
  // Locker methods
  //

  @Override
  public void lock() throws IOException {

    if (this.thread == null) {
      this.thread = new LockerThread(new Ticket(this.description));
    }

    final Thread t = new Thread(this.thread);

    t.start();

    while (t.isAlive()) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
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
  private void startRMIServer(final Set<Ticket> tickets) {

    new Thread(() -> {
      try {
        LocateRegistry.createRegistry(TicketLocker.this.port);
        TicketSchedulerServer.newServer(tickets, TicketLocker.this.lockerName,
            TicketLocker.this.port);

        // TODO the server must be halted if no more tickets to process since
        // several minutes
        while (true) {
          Thread.sleep(10000);
        }

      } catch (InterruptedException | RemoteException e) {
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

    this(lockerName, port, null);
  }

  /**
   * Public constructor.
   * @param lockerName The name of the locker
   * @param port port to use
   * @param description locker description
   */
  public TicketLocker(final String lockerName, final int port,
      final String description) {

    this.lockerName = lockerName;
    this.port = port;
    this.description = description;
  }

  //
  // Main method : list current tickets
  //

  /**
   * Main method.
   * @param args command line arguments
   * @throws RemoteException if an error occurs while inspecting tickets
   * @throws MalformedURLException if the URL of the server is malformed
   */
  public static final void main(final String[] args)
      throws RemoteException, MalformedURLException {

    if (args.length < 2) {

      System.err.println("List current lock tickets\nSyntax: java "
          + TicketLocker.class.getName() + " locker_name server_port");
      return;
    }

    // System.out.println("Available servers:");
    // for (String name : Naming.list("//localhost:" + args[1] + "/"))
    // System.out.println("\t" + name);

    TicketLocker locker =
        new TicketLocker(args[0], Integer.parseInt(args[1]), null);

    List<Ticket> tickets = new ArrayList<>(locker.getStub().getTickets(null));
    Collections.sort(tickets);

    for (Ticket t : tickets) {
      System.out.println(t);
    }

  }
}
