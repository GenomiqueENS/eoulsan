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
 * This class implements a comparator for BuildCovtigsReducer identifier.
 * @author Laurent Jourdren
 */
public class CoverageIdBuildCovtigsGroupComparator extends Text.Comparator {

  @Override
  public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {

    try {

      final int n1 = WritableUtils.decodeVIntSize(b1[s1]);
      final int n2 = WritableUtils.decodeVIntSize(b2[s2]);

      final String sb1 = Text.decode(b1, s1 + n1, l1 - n1);
      final String sb2 = Text.decode(b2, s2 + n2, l2 - n2);

      return compare(sb1, sb2);

    } catch (CharacterCodingException e) {

      throw new RuntimeException(e.getMessage());
    }

  }

  /**
   * Compare two coverage identifier strings
   * @param s1 First identifier
   * @param s2 Second identifier
   * @return an integer as comparator result
   */
  public static final int compare(String s1, String s2) {

    if (s1.equals(s2))
      return 0;

    final int spacePos1 = s1.indexOf(' ');
    final int spacePos2 = s1.indexOf(' ');

    if (spacePos1 == -1 || spacePos2 == -1) {
      int result = s1.compareTo(s2);
      if (result != 0)
        System.out.println("toto - " + s1 + " " + s2 + " " + result);
      return result;
    }

    final String chr1 = s1.substring(0, spacePos1);
    final String chr2 = s2.substring(0, spacePos2);

    int result = chr1.compareTo(chr2);
    if (result != 0) {

      System.out
          .println(("toto + " + toOrd(s1) + " " + toOrd(s2) + " " + result));
    }
    return result;
  }

  private static String toOrd(String s) {

    final int l = s.length();

    StringBuilder sb = new StringBuilder();
    sb.append(s);
    sb.append(" (");
    sb.append(l);
    sb.append(") ");

    for (int i = 0; i < l; i++) {
      sb.append((int) s.charAt(i));
      sb.append(' ');
    }

    return sb.toString();
  }

}
