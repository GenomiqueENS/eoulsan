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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.io.AbstractCompareFiles;
import fr.ens.transcriptome.eoulsan.io.BinaryCompareFile;

/**
 * This class allow to compare two fastq files.
 * @author Laurent Jourdren
 * @since 1.3
 */
public class FastqCompareFiles extends AbstractCompareFiles {

  @Override
  public boolean compareNonOrderedFiles(final InputStream inA, final InputStream inB)
      throws IOException {

    // Multiset where store hashcodes of the lines of the file
    final Multiset<Integer> hashcodes = HashMultiset.create();

    final ReadSequenceReader reader1 = new FastqReader(inA);

    // Read the first file and store hashcodes
    for (ReadSequence read : reader1)
      hashcodes.add(read.toFastQ().hashCode());

    reader1.close();
    // Throw an exception if an error has occurred while reading data
    try {
      reader1.throwException();
    } catch (BadBioEntryException e) {
      throw new IOException(e.getMessage());
    }

    final int count1 = hashcodes.size();
    int count2 = 0;

    final ReadSequenceReader reader2 = new FastqReader(inB);

    // Read the second file and check if ALL lines hashcode has been seen
    for (ReadSequence read : reader1) {

      final int hashcode = read.toFastQ().hashCode();

      if (!hashcodes.contains(hashcode)) {
        reader2.close();
        return false;
      }

      count2++;
      hashcodes.remove(hashcode);
    }

    reader2.close();

    return count1 == count2;
  }

  @Override
  public boolean compareOrderedFiles(final InputStream inA,
      final InputStream inB) throws IOException {

    return new BinaryCompareFile().compareOrderedFiles(inA, inB);
  }

}
