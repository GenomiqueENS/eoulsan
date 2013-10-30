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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.io.AbstractCompareFiles;
import fr.ens.transcriptome.eoulsan.io.BinaryCompareFile;

public class SAMCompareFiles extends AbstractCompareFiles {

  private final Set<String> tagsToNotCompare;

  @Override
  protected boolean checkFileSize() {

    return false;
  }

  @Override
  public boolean compareNonOrderedFiles(final InputStream inA,
      final InputStream inB) throws IOException {

    String line = null;

    // SAM headers
    final List<String> headers = Lists.newArrayList();

    // Multiset where store hashcodes of the lines of the file
    final Multiset<Integer> hashcodes = HashMultiset.create();

    final BufferedReader reader1 =
        new BufferedReader(new InputStreamReader(inA));

    // Read the first file and store hashcodes
    while ((line = reader1.readLine()) != null) {

      if (line.charAt(0) == '@') {

        if (hashcodes.size() > 0)
          throw new IOException(
              "Invalid SAM format (header found after the first entry)");

        headers.add(line);
        continue;
      }

      hashcodes.add(line.hashCode());
    }

    reader1.close();

    final BufferedReader reader2 =
        new BufferedReader(new InputStreamReader(inB));

    int headerIndex = 0;
    final int count1 = hashcodes.size();
    int count2 = 0;

    // Read the second file and check if ALL lines hashcode has been seen
    while ((line = reader2.readLine()) != null) {

      if (line.charAt(0) == '@') {

        final String tag = getTag(line);

        if (!this.tagsToNotCompare.contains(tag)) {

          if (headerIndex >= headers.size())
            return false;

          if (!headers.get(headerIndex).equals(line))
            return false;
        }

        headerIndex++;
        continue;
      }

      if (headerIndex != headers.size())
        return false;

      final int hashcode = line.hashCode();

      if (!hashcodes.contains(hashcode)) {
        reader2.close();
        return false;
      }

      hashcodes.remove(hashcode);
      count2++;
    }

    reader2.close();

    return count1 == count2;
  }

  @Override
  public boolean compareOrderedFiles(final InputStream inA,
      final InputStream inB) throws IOException {

    return new BinaryCompareFile().compareOrderedFiles(inA, inB);
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
