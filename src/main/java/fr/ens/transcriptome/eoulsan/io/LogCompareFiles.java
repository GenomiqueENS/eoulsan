package fr.ens.transcriptome.eoulsan.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.BloomFilterUtils;
import fr.ens.transcriptome.eoulsan.util.Reporter;

public class LogCompareFiles extends AbstractCompareFiles {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String NAME_COMPARE_FILES = "LogCompare";

  // Only file corresponding to the report step log
  public static final List<String> EXTENSION_READED = Lists
      .newArrayList(".log");

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

  /**
   * @param reporter instance of Reporter
   * @return number elements in reporter
   */
  private int counterGroupCount(final Reporter reporter) {

    if (reporter == null)
      return 0;

    int n = 0;

    for (String counterGroup : reporter.getCounterGroups()) {
      n += reporter.getCounterGroup(counterGroup).size();
    }

    return n;
  }

  @Override
  public int getNumberElementsCompared() {
    return this.numberElementsCompared;
  }

  @Override
  public List<String> getExtensionReaded() {
    return EXTENSION_READED;
  }

  @Override
  public String getName() {
    return NAME_COMPARE_FILES;
  }

  @Override
  public boolean compareFiles(BloomFilterUtils filter, InputStream is)
      throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public BloomFilterUtils buildBloomFilter(InputStream is) throws IOException {

    throw new UnsupportedOperationException();
  }

  @Override
  public int getExpectedNumberOfElements() {
    throw new UnsupportedOperationException();
  }

  @Override
  public double getFalsePositiveProba() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isUseBloomfilterAvailable() {
    return false;
  }

}
