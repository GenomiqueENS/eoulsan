package fr.ens.transcriptome.eoulsan.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.util.Reporter;

public class LogCompareFiles extends AbstractCompareFiles {

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

    int numberElements = logExpected.getCounterGroups().size();

    long diffExpectedTested;

    for (String counterGroup : logExpected.getCounterGroups()) {

      numberElements += logExpected.getCounterGroup(counterGroup).size();

      for (String counter : logExpected.getCounterGroup(counterGroup)) {
        numberElementsCompared++;

        diffExpectedTested =
            logExpected.getCounterValue(counterGroup, counter)
                - logTested.getCounterValue(counterGroup, counter);

        if (Math.abs(diffExpectedTested) > 1)
          return false;
      }

    }

    if (numberElements != numberElementsCompared)
      return false;

    return true;
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
  
  //
  // Internal class
  //
  public class LongComparator implements Comparator {
    // TODO in case it is necessary to compare 2 long

    @Override
    public int compare(Object obj1, Object obj2) {

      return (obj1.toString()).compareTo(obj2.toString());
    }

  }

  
}
