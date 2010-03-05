/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.hadoop.gmorse;

import java.nio.charset.CharacterCodingException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableUtils;

/**
 * This class implements a comparator for Coverage identifiers.
 * @author Laurent Jourdren
 */
public class CoverageIdSortComparator extends Text.Comparator {

  @Override
  public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {

    try {
      final int n1 = WritableUtils.decodeVIntSize(b1[s1]);
      final int n2 = WritableUtils.decodeVIntSize(b2[s2]);
      
      final String sb1 = Text.decode(b1, s1+n1, l1-n1);
      final String sb2 = Text.decode(b2, s2+n2, l2-n2);

      return compare(sb1, sb2);

    } catch (CharacterCodingException e) {

      throw new RuntimeException(e.getMessage());
    } catch (NumberFormatException e) {

      return super.compare(b1, s1, l1, b2, s2, l2);
    }

  }

  /**
   * Compare two coverage identifier strings
   * @param s1 First identifier
   * @param s2 Second identifier
   * @return an integer as comparator result
   */
  public static final int compare(final String s1, final String s2) {

    if (s1.equals(s2))
      return 0;

    final int spacePos1 = s1.indexOf(' ');
    final int spacePos2 = s1.indexOf(' ');

    if (spacePos1 == -1 || spacePos2 == -1)
      return s1.compareTo(s2);

    final String chr1 = s1.substring(0, spacePos1);
    final String chr2 = s2.substring(0, spacePos2);

    if (chr1.equals(chr2)) {

      final int pos1 =
          Integer.parseInt(s1.substring(spacePos1 + 1, s1.length()));
      final int pos2 =
          Integer.parseInt(s2.substring(spacePos2 + 1, s2.length()));

      return pos1 - pos2;
    }
    return chr1.compareTo(chr2);
  }

}
