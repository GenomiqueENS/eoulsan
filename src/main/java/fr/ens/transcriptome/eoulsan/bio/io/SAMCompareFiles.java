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

package fr.ens.transcriptome.eoulsan.bio.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.io.AbstractCompareFiles;
import fr.ens.transcriptome.eoulsan.util.BloomFilterUtils;

public class SAMCompareFiles extends AbstractCompareFiles {

  public static final String NAME_COMPARE_FILES = "SAMCompare";
  public static final List<String> EXTENSION_READED = Lists
      .newArrayList(".sam");

  final Set<String> tagsToNotCompare;

  private double falsePositiveProba = 0.1;
  private int expectedNumberOfElements = 30000000;
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

      // Header
      if (line.charAt(0) == '@') {
        // Skip specified tag in header sam file
        if (!this.tagsToNotCompare.contains(getTag(line))) {

          if (!filter.mightContain(line)) {
            reader.close();
            return false;
          }
        }
      } else {
        // Line
        if (!filter.mightContain(line)) {
          reader.close();
          return false;
        }
      }
    }
    reader.close();

    // Check count element is the same between two files
    if (numberElementsCompared != filter.getAddedNumberOfElements())
      return false;

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
  // Other methods
  //

  private static String getTag(final String samHeaderLine) {

    if (samHeaderLine.length() == 0)
      return "";

    final int pos = samHeaderLine.indexOf('\t');

    if (pos == -1)
      return samHeaderLine.substring(1);

    return samHeaderLine.substring(1, pos);
  }

  @Override
  public int getExpectedNumberOfElements() {
    return this.expectedNumberOfElements;
  }

  @Override
  public double getFalsePositiveProba() {
    return this.falsePositiveProba;
  }

  @Override
  public String getName() {

    return NAME_COMPARE_FILES;
  }

  @Override
  public List<String> getExtensionReaded() {
    return EXTENSION_READED;
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
  //
  // Constructor
  //

  public SAMCompareFiles() {

    this.tagsToNotCompare = Sets.newHashSet();
  }

  public SAMCompareFiles(String... headersTags) {

    if (headersTags == null)
      throw new NullPointerException("headersTags is null");

    this.tagsToNotCompare = Sets.newHashSet(headersTags);
  }

}


