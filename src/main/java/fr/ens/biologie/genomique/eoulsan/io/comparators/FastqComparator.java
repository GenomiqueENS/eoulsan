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

import fr.ens.biologie.genomique.eoulsan.util.EnhancedBloomFilter;
import fr.ens.biologie.genomique.kenetre.bio.BadBioEntryException;
import fr.ens.biologie.genomique.kenetre.bio.ReadSequence;
import fr.ens.biologie.genomique.kenetre.bio.io.FastqReader;

/**
 * This class allow compare two FastQ files with use BloomFilter.
 * @since 2.0
 * @author Sandrine Perrin
 */
public class FastqComparator extends AbstractComparatorWithBloomFilter {

  public static final String COMPARATOR_NAME = "FastqComparator";
  private static final Collection<String> EXTENSIONS =
      Sets.newHashSet(".fastq", ".fq");

  private int numberElementsCompared;

  @Override
  public boolean compareFiles(final EnhancedBloomFilter filter,
      final InputStream is) throws IOException {

    final FastqReader fastqReader = new FastqReader(is);
    this.numberElementsCompared = 0;

    // Search each ReadSequence in BFilter source
    for (ReadSequence read : fastqReader) {
      this.numberElementsCompared++;

      if (!filter.mightContain(read.toFastQ() + '\n')) {
        // Save line occurs fail comparison
        setCauseFailComparison(read.toFastQ() + '\n');
        fastqReader.close();
        return false;
      }
    }
    fastqReader.close();

    // Check count element is the same between two files
    if (this.numberElementsCompared != filter.getAddedNumberOfElements()) {
      setCauseFailComparison("Different count elements "
          + this.numberElementsCompared + " was "
          + filter.getAddedNumberOfElements() + " expected.");
      return false;
    }

    return true;
  }

  @Override
  protected EnhancedBloomFilter buildBloomFilter(final InputStream is)
      throws IOException {

    final EnhancedBloomFilter filter =
        initBloomFilter(getExpectedNumberOfElements());

    final FastqReader fastqReader = new FastqReader(is);

    // Search each ReadSequence in BFilter source
    for (ReadSequence read : fastqReader) {
      filter.put(read.toFastQ() + '\n');
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

  //
  // Constructor
  //

  /**
   * Public constructor
   * @param useSerializeFile true if it needed to save BloomFilter in file with
   *          extension '.ser'
   */
  public FastqComparator(final boolean useSerializeFile) {
    super(useSerializeFile);
  }
}
