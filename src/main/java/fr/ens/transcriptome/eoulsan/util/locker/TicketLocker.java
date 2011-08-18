package fr.ens.transcriptome.eoulsan.util.locker;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Set;

/**
 * This class define a global locker using RMI messaging.
 * @author Laurent Jourdren TODO tune waiting times
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
     * Get the stub of TicketScheduler.
     * @return a TicketScheduler object
     */
    private final TicketScheduler getStub() {

      try {
        Registry registry = LocateRegistry.getRegistry(port);
        return (TicketScheduler) registry.lookup(RMI_SERVICE_PREFIX
            + lockerName);

      }

      catch (IOException e) {
        return null;
      } catch (NotBoundException e) {
        return null;
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

}