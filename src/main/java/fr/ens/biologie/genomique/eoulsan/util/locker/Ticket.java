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

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * This class define a ticket for the TicketLocker.
 *
 * @since 1.1
 * @author Laurent Jourdren
 */
public final class Ticket implements Comparable<Ticket>, Serializable {

  private static final long serialVersionUID = -7934169474677708526L;

  private final int pid;
  private final long threadId;
  private final long creationTime;
  private final long nanoCreationTime;
  private final String description;
  private long lastActiveTime;
  private boolean working;

  //
  // Getter
  //

  public int getPid() {
    return this.pid;
  }

  public long getThreadId() {
    return this.threadId;
  }

  public long getCreationTime() {
    return this.creationTime;
  }

  public String getDescription() {
    return this.description;
  }

  public long getLastActiveTime() {
    return this.lastActiveTime;
  }

  public boolean isWorking() {
    return this.working;
  }

  //
  // Setter
  //

  public void setWorking(final boolean working) {
    this.working = working;
  }

  public void updateLastActiveTime() {
    this.lastActiveTime = System.currentTimeMillis();
  }

  //
  // Other methods
  //

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof Ticket)) {
      return false;
    }

    final Ticket t = (Ticket) o;

    return this.creationTime == t.creationTime
        && this.nanoCreationTime == t.nanoCreationTime
        && this.pid == t.pid
        && this.threadId == t.threadId;
  }

  @Override
  public int hashCode() {

    return hashCode(this.creationTime, this.nanoCreationTime, this.pid, this.threadId);
  }

  @Override
  public int compareTo(final Ticket ticket) {

    if (ticket == null) {
      return 1;
    }

    // Compare creation time
    final int comp1 = Long.compare(this.creationTime, ticket.creationTime);

    if (comp1 != 0) {
      return comp1;
    }

    // Compare Nano creation time
    final int comp2 = Long.compare(this.nanoCreationTime, ticket.nanoCreationTime);
    if (comp2 != 0) {
      return comp2;
    }

    // Compare PID
    final int comp3 = Integer.compare(this.pid, ticket.pid);
    if (comp3 != 0) {
      return comp3;
    }

    return Long.compare(this.threadId, ticket.threadId);
  }

  @Override
  public String toString() {

    final DateTimeFormatter timeFormatter =
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    Instant creationInstant = Instant.ofEpochMilli(this.creationTime);
    Instant lastActiveInstant = Instant.ofEpochMilli(this.lastActiveTime);
    Instant uptimeInstant = Instant.ofEpochMilli(System.currentTimeMillis() - this.creationTime);

    return timeFormatter.format(creationInstant)
        + " "
        + timeFormatter.format(lastActiveInstant)
        + " "
        + timeFormatter.format(uptimeInstant)
        + " "
        + " "
        + (this.working ? "WORKING" : "NOT WORKING")
        + " ["
        + this.pid
        + '.'
        + this.threadId
        + " "
        + (this.description != null ? this.description : "")
        + "]";
  }

  //
  // Static methods
  //

  public static int hashCode(final Object... objects) {
    return Arrays.hashCode(objects);
  }

  private static int getCurrentPid() {

    final String beanName = ManagementFactory.getRuntimeMXBean().getName();

    final int index = beanName.indexOf('@');

    return Integer.parseInt(beanName.substring(0, index));
  }

  //
  // Constructor
  //

  public Ticket() {
    this((String) null);
  }

  public Ticket(final String description) {
    this(
        getCurrentPid(),
        Thread.currentThread().getId(),
        System.currentTimeMillis(),
        System.nanoTime(),
        description,
        -1,
        false);
  }

  public Ticket(final Ticket ticket) {

    this(
        ticket.pid,
        ticket.threadId,
        ticket.creationTime,
        ticket.nanoCreationTime,
        ticket.description,
        ticket.lastActiveTime,
        ticket.working);
  }

  public Ticket(
      final int pid,
      final long threadId,
      final long creationTime,
      final long nanoCreationTime,
      final String description,
      final long lastActiveTime,
      final boolean working) {

    this.pid = pid;
    this.threadId = threadId;
    this.creationTime = creationTime;
    this.nanoCreationTime = nanoCreationTime;
    this.description = description;
    this.lastActiveTime = lastActiveTime == -1 ? this.creationTime : lastActiveTime;
    this.working = working;
  }
}
