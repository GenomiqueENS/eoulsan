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
package fr.ens.transcriptome.eoulsan.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.util.BloomFilterUtils;

public class TextCompareFiles extends AbstractCompareFiles {

  private static final String NAME_COMPARE_FILES = "TextCompare";
  public static final List<String> EXTENSION_READED = Lists.newArrayList(
      ".txt", ".tsv", ".csv", ".ebwt", ".xml");

  private static double falsePositiveProba = 0.1;
  private static int expectedNumberOfElements = 30000000;
  private boolean useBloomfilterAvailable = true;

  private int numberElementsCompared;

  @Override
  public boolean compareFiles(InputStream isA, InputStream isB)
      throws IOException {
    return compareFiles(buildBloomFilter(isA), isB);
  }

  @Override
  public boolean compareFiles(BloomFilterUtils filter, InputStream is)
      throws IOException {

    final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String line = null;
    numberElementsCompared = 0;

    while ((line = reader.readLine()) != null) {
      numberElementsCompared++;

      if (!filter.mightContain(line)) {
        reader.close();
        return false;
      }
    }
    reader.close();

    // Check count element is the same between two files
    if (numberElementsCompared != filter.getAddedNumberOfElements()) {
      return false;
    }
    return true;
  }

  @Override
  public BloomFilterUtils buildBloomFilter(InputStream is) throws IOException {
    final BloomFilterUtils filter = initBloomFilter(expectedNumberOfElements);

    final BufferedReader reader = new BufferedReader(new InputStreamReader(is));

    String line = null;

    // Read the first file and store hashcodes
    while ((line = reader.readLine()) != null) {
      filter.put(line);
    }

    reader.close();
    return filter;
  }

  //
  // Getter
  //

  @Override
  public int getExpectedNumberOfElements() {
    return expectedNumberOfElements;
  }

  @Override
  public double getFalsePositiveProba() {
    return falsePositiveProba;
  }

  public List<String> getExtensionReaded() {
    return EXTENSION_READED;
  }

  @Override
  public String getName() {

    return NAME_COMPARE_FILES;
  }

  @Override
  public int getNumberElementsCompared() {
    return this.numberElementsCompared;
  }

  @Override
  public boolean isUseBloomfilterAvailable() {
    return useBloomfilterAvailable;
  }

  public void setUseBloomfilterAvailable(boolean useBloomfilterAvailable) {
    this.useBloomfilterAvailable = useBloomfilterAvailable;
  }
}
