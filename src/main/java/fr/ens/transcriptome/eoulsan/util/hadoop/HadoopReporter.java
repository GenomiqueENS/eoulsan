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

package fr.ens.transcriptome.eoulsan.util.hadoop;

import java.util.Set;

import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Counters;

import com.google.common.collect.Sets;
import fr.ens.transcriptome.eoulsan.util.Reporter;

public class HadoopReporter implements Reporter {

  private final Counters counters;

  @Override
  public void incrCounter(final String counterGroup, final String counterName,
      long amount) {

    this.counters.getGroup(counterGroup).findCounter(counterName)
        .increment(amount);
  }

  @Override
  public long getCounterValue(final String counterGroup,
      final String counterName) {

    return this.counters.findCounter(counterGroup, counterName).getValue();
  }

  @Override
  public Set<String> getCounterGroups() {

    return Sets.newHashSet(counters.getGroupNames());
  }

  @Override
  public Set<String> getCounterNames(final String group) {

    final Set<String> result = Sets.newHashSet();

    for (Counter c : counters.getGroup(group))
      result.add(c.getName());

    return result;
  }

  public HadoopReporter(final Counters counter) {

    this.counters = counter;
  }

}
