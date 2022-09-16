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
package fr.ens.biologie.genomique.eoulsan.io.comparators;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import com.google.common.collect.Sets;

import fr.ens.biologie.genomique.eoulsan.io.LogReader;
import fr.ens.biologie.genomique.kenetre.util.Reporter;

/**
 * This class allow compare two log files writing by step Eoulsan.
 * @since 2.0
 * @author Sandrine Perrin
 */
public class LogComparator extends AbstractComparator {

  private static final String COMPARATOR_NAME = "LogComparator";

  // Only file corresponding to the report step log
  private static final Collection<String> EXTENSIONS = Sets.newHashSet(".log");

  private int numberElementsCompared;

  @Override
  public boolean compareFiles(final InputStream isA, final InputStream isB)
      throws IOException {

    Reporter logExpected = new LogReader(isA).read();
    Reporter logTested = new LogReader(isB).read();

    int numberElements = 0;

    long diffExpectedTested;

    // Parse counter group in log file
    for (String counterGroup : logExpected.getCounterGroups()) {

      numberElements += logExpected.getCounterNames(counterGroup).size();

      // Parse counter
      for (String counter : logExpected.getCounterNames(counterGroup)) {
        this.numberElementsCompared++;

        // Compute difference between two reporter
        diffExpectedTested = logExpected.getCounterValue(counterGroup, counter)
            - getCounterValue(logTested, counterGroup, counter);

        if (Math.abs(diffExpectedTested) >= 1) {
          setCauseFailComparison("Invalid value found: "
              + getCounterValue(logTested, counterGroup, counter) + ", "
              + logExpected.getCounterValue(counterGroup, counter)
              + " was expected.");
          return false;
        }
      }
    }

    // Check all elements present in first log are compare from second log
    if (numberElements != this.numberElementsCompared) {
      setCauseFailComparison("Found "
          + this.numberElementsCompared + " elements, " + numberElements
          + " were expected.");
      return false;
    }

    return true;
  }

  /**
   * Retrieve value for counterGroup and counter from a instance reporter, by
   * using the prefix of the key counterGroup (string before the first virgule)
   * @param logTested reporter contains values
   * @param counterGroupExpected key of counterGroup
   * @param counter key of counter
   * @return value corresponding to counterGroup and counter
   */
  private long getCounterValue(final Reporter logTested,
      final String counterGroupExpected, final String counter) {

    int pos = counterGroupExpected.indexOf(",");
    // Retrieve prefix of key CounterGroup, without file path
    final String prefix = counterGroupExpected.substring(0, pos);

    for (String counterGroup : logTested.getCounterGroups()) {
      // Retrieve counterGroup corresponding to the same sample file that
      // expected
      if (counterGroup.startsWith(prefix)) {
        return logTested.getCounterValue(counterGroup, counter);
      }
    }
    return -1;
  }

  @Override
  public Collection<String> getExtensions() {
    return EXTENSIONS;
  }

  @Override
  public String getName() {
    return COMPARATOR_NAME;
  }

  @Override
  public int getNumberElementsCompared() {
    return this.numberElementsCompared;
  }

}
