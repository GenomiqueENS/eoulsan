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

package fr.ens.biologie.genomique.eoulsan.util.hadoop;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.TaskInputOutputContext;

import com.google.common.collect.Sets;

import fr.ens.biologie.genomique.kenetre.util.Reporter;

/**
 * This class define a Hadoop reporter.
 * @since 1.0
 * @author Laurent Jourdren
 */
@SuppressWarnings("rawtypes")
public class HadoopReporter implements Reporter {

  private final TaskInputOutputContext context;
  private final Counters counters;

  @Override
  public void incrCounter(final String counterGroup, final String counterName,
      final long amount) {

    if (this.context != null) {
      // Use in mappers and reducers
      this.context.getCounter(counterGroup, counterName).increment(amount);
    } else {
      // Use in other cases
      this.counters.getGroup(counterGroup).findCounter(counterName)
          .increment(amount);
    }
  }

  @Override
  public long getCounterValue(final String counterGroup,
      final String counterName) {

    // This method does not works in Hadoop mappers and reducers
    if (this.context != null) {
      throw new UnsupportedOperationException();
    }

    return this.counters.findCounter(counterGroup, counterName).getValue();
  }

  @Override
  public Set<String> getCounterGroups() {

    // This method does not works in Hadoop mappers and reducers
    if (this.context != null) {
      throw new UnsupportedOperationException();
    }

    return Sets.newHashSet(this.counters.getGroupNames());
  }

  @Override
  public Set<String> getCounterNames(final String group) {

    // This method does not works in Hadoop mappers and reducers
    if (this.context != null) {
      throw new UnsupportedOperationException();
    }

    final Set<String> result = new HashSet<>();

    for (Counter c : this.counters.getGroup(group)) {
      result.add(c.getName());
    }

    return result;
  }

  //
  // Constructors
  //

  /**
   * Constructor. This constructor is used by Hadoop mappers and reducers.
   * @param context context to use for counter incrementation
   */
  public HadoopReporter(final TaskInputOutputContext context) {

    requireNonNull(context, "context is null");

    this.counters = null;
    this.context = context;
  }

  /**
   * Constructor.
   * @param counters counters to use for counter incrementation
   */
  public HadoopReporter(final Counters counters) {

    requireNonNull(counters, "counters is null");

    this.counters = counters;
    this.context = null;
  }

}
