/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class implements a reporter class like Counter class in Hadoop
 * framework.
 * @author Laurent Jourdren
 */
public class Reporter {

  private Map<String, Map<String, Long>> map =
      new HashMap<String, Map<String, Long>>();

  /**
   * Increment a counter.
   * @param counterGroup group of the counter
   * @param counter the counter to increment
   * @param amount amount to add to the counter
   */
  public void incrCounter(final String counterGroup, final String counter,
      final long amount) {

    if (counterGroup == null || counter == null || amount <= 0)
      return;

    final Map<String, Long> group;

    if (!this.map.containsKey(counterGroup)) {
      group = new HashMap<String, Long>();
      this.map.put(counterGroup, group);
    } else
      group = this.map.get(counterGroup);

    long value = 0;

    if (group.containsKey(counter))
      value = group.get(counter);

    value += amount;
    group.put(counter, value);
  }

  /**
   * Set the value of a counter.
   * @param counterGroup group of the counter
   * @param counter the counter to increment
   * @param value value of the counter
   */
  public void setCounter(final String counterGroup, final String counter,
      final long value) {

    if (counterGroup == null || counter == null || value <= 0)
      return;

    final Map<String, Long> group;

    if (!this.map.containsKey(counterGroup)) {
      group = new HashMap<String, Long>();
      this.map.put(counterGroup, group);
    } else
      group = this.map.get(counterGroup);

    group.put(counter, value);
  }

  /**
   * Get the value of a counter.
   * @param counterGroup the group of the counter
   * @param counter the counter name
   * @return the value of the counter or -1 if the counter does not exists
   */
  public long getCounterValue(final String counterGroup, final String counter) {

    if (counterGroup == null || counter == null)
      return -1;

    final Map<String, Long> group = this.map.get(counterGroup);
    if (group == null)
      return -1;

    Long value = group.get(counter);
    if (value == null)
      return -1;

    return value;
  }

  /**
   * Get a list of counter groups
   * @return a unmodifiable list of the counter groups
   */
  public Set<String> getCounterGroups() {

    return Collections.unmodifiableSet(this.map.keySet());
  }

  /**
   * Clear all the counters in the reporter.
   */
  public void clear() {

    this.map.clear();
  }

  /**
   * Get the values of the counter of a group.
   * @param counterGroup counter group
   * @param header header before counter values
   * @return a string with all the values of counter of the counter group
   */
  public String countersValuesToString(final String counterGroup,
      final String header) {

    final StringBuilder sb = new StringBuilder();

    if (header != null) {
      sb.append(header);
      sb.append('\n');
    }

    final Map<String, Long> group = this.map.get(counterGroup);
    if (group != null)
      for (Map.Entry<String, Long> e : group.entrySet()) {
        sb.append('\t');
        sb.append(e.getKey());
        sb.append('=');
        sb.append(e.getValue());
        sb.append('\n');
      }

    return sb.toString();
  }

  @Override
  public String toString() {

    final StringBuilder sb = new StringBuilder();

    for (String counterGroup : getCounterGroups()) {
      sb.append(counterGroup);
      sb.append('\n');
      sb.append(countersValuesToString(counterGroup, null));
    }

    return sb.toString();
  }
}
