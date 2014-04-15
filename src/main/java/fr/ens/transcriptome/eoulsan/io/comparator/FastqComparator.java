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

package fr.ens.transcriptome.eoulsan.io.comparator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.bio.io.FastqReader;
import fr.ens.transcriptome.eoulsan.util.BloomFilterUtils;

public class FastqComparator extends AbstractComparatorWithBloomFilter {

  public static final String COMPARATOR_NAME = "FastqComparator";
  private static final Collection<String> EXTENSIONS = Sets.newHashSet(".fastq",
      ".fq");

  private int numberElementsCompared;

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

  @Override
  public BloomFilterUtils buildBloomFilter(final InputStream is)
      throws IOException {

    final BloomFilterUtils filter =
        initBloomFilter(getExpectedNumberOfElements());

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

  // @Override
  // public int getExpectedNumberOfElements() {
  // return expectedNumberOfElements;
  // }
  //
  
  //
  // @Override
  // public int getNumberElementsCompared() {
  // return this.numberElementsCompared;
  // }
  //
  //
  // public void setUseBloomfilterAvailable(boolean useBloomfilterAvailable) {
  // this.useBloomfilterAvailable = useBloomfilterAvailable;
  // }

  //
  // Constructor
  //

  public FastqComparator() {

  }

}
