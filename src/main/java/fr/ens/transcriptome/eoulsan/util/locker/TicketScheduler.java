package fr.ens.transcriptome.eoulsan.util.locker;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

/**
 * Define the rmi interface for the TicketSchedulerServer.
 * @author Laurent Jourdren
 */
public interface TicketScheduler extends Remote {

  /**
   * Update a ticket by the TicketScheduler and get all the valids tickets.
   * @param ticket the ticket to update
   * @return a set with all the valid tickets
   * @throws RemoteException if an error occurs with the server
   */
  Set<Ticket> getTickets(final Ticket ticket) throws RemoteException;

  /**
   * Inform the scheduler that the ticket job has been finished.
   * @param ticket the ticket that the job has been finished
   * @throws RemoteException if an error occurs with the server
   */
  void endWork(final Ticket ticket) throws RemoteException;
}
