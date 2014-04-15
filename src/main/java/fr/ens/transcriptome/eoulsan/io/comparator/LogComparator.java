package fr.ens.transcriptome.eoulsan.io.comparator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.io.LogReader;
import fr.ens.transcriptome.eoulsan.util.Reporter;

public class LogComparator extends AbstractComparator {

  private static final String COMPARATOR_NAME = "LogComparator";

  // Only file corresponding to the report step log
  private static final Collection<String> EXTENSIONS = Sets.newHashSet(".log");

  private int numberElementsCompared;

  @Override
  public boolean compareFiles(InputStream isA, InputStream isB)
      throws IOException {

    Reporter logExpected = new LogReader(isA).read();
    Reporter logTested = new LogReader(isB).read();

    int numberElements = 0;

    long diffExpectedTested;

    for (String counterGroup : logExpected.getCounterGroups()) {

      numberElements += logExpected.getCounterGroup(counterGroup).size();

      for (String counter : logExpected.getCounterGroup(counterGroup)) {
        numberElementsCompared++;

        diffExpectedTested =
            logExpected.getCounterValue(counterGroup, counter)
                - getCounterValue(logTested, counterGroup, counter);

        // TODO
        // LOGGER.fine("\t"
        // + logExpected.getCounterValue(counterGroup, counter) + "\t"
        // + getCounterValue(logTested, counterGroup, counter) + "\t"
        // + Math.abs(diffExpectedTested) + "\t"
        // + (Math.abs(diffExpectedTested) > 1));
        // System.out.println("\t"
        // + logExpected.getCounterValue(counterGroup, counter) + "\t"
        // + getCounterValue(logTested, counterGroup, counter) + "\t"
        // + Math.abs(diffExpectedTested) + "\t"
        // + (Math.abs(diffExpectedTested) > 1));

        if (Math.abs(diffExpectedTested) > 1) {
          return false;
        }
      }
    }

    // // TODO
    // LOGGER.fine("\n\t" + numberElements + "\t" + numberElementsCompared);
    // System.out.println("\n\t" + numberElements + "\t" +
    // numberElementsCompared);

    // if (numberElements != numberElementsCompared)
    // return false;

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
      if (counterGroup.startsWith(prefix))
        return logTested.getCounterValue(counterGroup, counter);
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
