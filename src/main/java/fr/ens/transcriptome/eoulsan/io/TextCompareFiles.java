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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * This class allow to compare ordered or non ordered text file with one entry
 * by line.
 * @author Laurent Jourdren
 * @since 1.3
 */
public class TextCompareFiles extends AbstractCompareFiles {

  @Override
  public boolean compareNonOrderedFiles(InputStream inA, InputStream inB)
      throws IOException {

    // Multiset where store hashcodes of the lines of the file
    final Multiset<Integer> hashcodes = HashMultiset.create();
    String line;

    final BufferedReader reader1 =
        new BufferedReader(new InputStreamReader(inA));

    // Read the first file and store hashcodes
    while ((line = reader1.readLine()) != null)
      hashcodes.add(line.hashCode());

    reader1.close();

    final int count1 = hashcodes.size();
    int count2 = 0;

    final BufferedReader reader2 =
        new BufferedReader(new InputStreamReader(inB));

    // Read the second file and check if ALL lines hashcode has been seen
    while ((line = reader2.readLine()) != null) {

      final int hashcode = line.hashCode();

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
  public boolean compareOrderedFiles(InputStream inA, InputStream inB)
      throws IOException {

    return new BinaryCompareFile().compareOrderedFiles(inA, inB);
  }

}
