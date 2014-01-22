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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.bio.io.FastqReader;
import fr.ens.transcriptome.eoulsan.io.AbstractCompareFiles;
import fr.ens.transcriptome.eoulsan.io.BloomFilterUtils;

public class FastqCompareFiles extends AbstractCompareFiles {

  public static final String NAME_COMPARE_FILES = "FastqCompare";
  public static final List<String> EXTENSION_READED = Lists.newArrayList(
      ".fastq", ".fq");

  private static double falsePositiveProba = 0.1;
  private static int expectedNumberOfElements = 30000000;

  private int numberElementsCompared;

  @Override
  public boolean compareFiles(InputStream isA, InputStream isB)
      throws IOException {
    return compareFiles(buildBloomFilter(isA), isB);
  }

  @Override
  public boolean compareFiles(BloomFilterUtils filter, InputStream is)
      throws IOException {

    final FastqReader fastqReader = new FastqReader(is);
    numberElementsCompared = 0;
    
    // Search each ReadSequence in BFilter source
    for (ReadSequence read : fastqReader) {
      this.numberElementsCompared++;

      if (!filter.mightContain(read.toFastQ())) {
        fastqReader.close();
        return false;
      }
    }
    fastqReader.close();

    // Check count element is the same between two files
    if (this.numberElementsCompared != filter.getAddedNumberOfElements())
      return false;

    return true;
  }

  public BloomFilterUtils buildBloomFilter(final InputStream is)
      throws IOException {

    final BloomFilterUtils filter = initBloomFilter(expectedNumberOfElements);

    final FastqReader fastqReader = new FastqReader(is);

    // Search each ReadSequence in BFilter source
    for (ReadSequence read : fastqReader) {
      filter.put(read.toFastQ());
    }
    fastqReader.close();

    try {
      fastqReader.throwException();
    } catch (BadBioEntryException e) {
      throw new IOException("Fail BadBioEntry exception: " + e.getMessage());
    }

    return filter;
  }

  //
  // Getter and setters
  //

  @Override
  public int getExpectedNumberOfElements() {
    return expectedNumberOfElements;
  }

  @Override
  public double getFalsePositiveProba() {
    return falsePositiveProba;
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
  public int getNumberElementsCompared() {
    return this.numberElementsCompared;
  }

  //
  // Constructor
  //

  public FastqCompareFiles() {

  }

}
