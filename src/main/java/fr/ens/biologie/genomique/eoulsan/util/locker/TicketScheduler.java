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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

/**
 * Define the rmi interface for the TicketSchedulerServer.
 *
 * @since 1.1
 * @author Laurent Jourdren
 */
public interface TicketScheduler extends Remote {

  /**
   * Update a ticket by the TicketScheduler and get all the valid tickets.
   *
   * @param ticket the ticket to update
   * @return a set with all the valid tickets
   * @throws RemoteException if an error occurs with the server
   */
  Set<Ticket> getTickets(final Ticket ticket) throws RemoteException;

  /**
   * Inform the scheduler that the ticket job has been finished.
   *
   * @param ticket the ticket that the job has been finished
   * @throws RemoteException if an error occurs with the server
   */
  void endWork(final Ticket ticket) throws RemoteException;
}
