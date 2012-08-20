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

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class define a server for the scheduler.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class TicketSchedulerServer implements TicketScheduler {

  private Ticket currentActive;
  private final Map<Ticket, Ticket> tickets = new HashMap<Ticket, Ticket>();
  private final Set<Ticket> toRemove = new HashSet<Ticket>();

  private int maxWorkingTime = 2 * 60 * 1000;
  private int deadTime = 30 * 1000;

  private int checkingTime = 10 * 1000;
  private long lastCheckingTime = 0;

  @Override
  public Set<Ticket> getTickets(final Ticket ticket) {

    if (ticket == null)
      return null;

    synchronized (this.tickets) {

      if (tickets.containsKey(ticket)) {

        final Ticket t = tickets.get(ticket);
        t.updateLastActiveTime();

      } else {
        ticket.updateLastActiveTime();
        tickets.put(ticket, ticket);
      }

      check();

      return new HashSet<Ticket>(tickets.values());
    }
  }

  @Override
  public void endWork(final Ticket ticket) {

    if (ticket == null)
      return;

    synchronized (this.tickets) {

      if (ticket.equals(this.currentActive)) {

        this.tickets.remove(this.currentActive);
        this.currentActive = null;

        check();
      }
    }
  }

  private void check() {

    final long currentTime = System.currentTimeMillis();
    if (currentTime > lastCheckingTime + checkingTime) {

      // Check for waiting dead ticket
      for (Ticket t : this.tickets.values())
        if (!t.isWorking() && currentTime > t.getLastActiveTime() + deadTime)
          this.toRemove.add(t);

      // Check for active ticket dead
      if (this.currentActive != null
          && currentTime > this.currentActive.getLastActiveTime()
              + maxWorkingTime) {
        this.toRemove.add(this.currentActive);
        this.currentActive = null;
      }

      // Check JVM PIDs
      final Set<Integer> pids = LockerUtils.getJVMsPids();
      for (Ticket t : this.tickets.values()) {
        if (!pids.contains(t.getPid())) {

          if (t.equals(this.currentActive))
            this.currentActive = null;

          toRemove.add(t);
        }
      }

      // Remove dead
      for (Ticket t : toRemove)
        this.tickets.remove(t);

      toRemove.clear();
      lastCheckingTime = currentTime;
    }

    if (this.currentActive == null && this.tickets.size() > 0) {

      for (Ticket t : this.tickets.values()) {
        if (t.isWorking()) {
          this.currentActive = t;
          return;
        }
      }

      List<Ticket> list = new ArrayList<Ticket>(this.tickets.values());
      Collections.sort(list);
      this.currentActive = list.get(0);
      this.currentActive.setWorking(true);
    }

  }

  //
  // Constructor
  //

  public TicketSchedulerServer(final Set<Ticket> tickets) {

    if (tickets != null) {
      for (Ticket t : tickets)
        this.tickets.put(t, t);

      check();
    }
  }

  //
  // Server methods
  //

  public static void newServer(final Set<Ticket> tickets,
      final String lockerName, final int port) {

    try {

      final TicketScheduler stub =
          (TicketScheduler) UnicastRemoteObject.exportObject(
              new TicketSchedulerServer(tickets), 0);

      // Bind the remote object's stub in the registry
      Registry registry = LocateRegistry.getRegistry(port);
      if (registry == null)
        registry = LocateRegistry.createRegistry(port);
      registry.bind(TicketLocker.RMI_SERVICE_PREFIX + lockerName, stub);

    } catch (Exception e) {
      Utils.nop();
    }
  }

}
