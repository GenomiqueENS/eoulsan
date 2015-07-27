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

package fr.ens.transcriptome.eoulsan.bio.readsmappers;

import static fr.ens.transcriptome.eoulsan.util.StringUtils.join;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;

/**
 * This class allow to convert the SOAP2 output to SAM format.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class SOAP2SAM {

  private final StringBuilder sb = new StringBuilder();
  private final GenomeDescription gd;
  private boolean first = true;

  private static final Pattern PATTERN =
      Pattern.compile("^([AaCcGgTt])->(\\d+)");

  private static final Pattern TAB_PATTERN = Pattern.compile("\t");

  private String[] sLast;
  private List<String> result = new ArrayList<>();

  public List<String> parse(String line, boolean isPaired) {

    this.result.clear();

    if (this.first) {
      if (this.gd != null) {

        this.result.add("@HD\tVN:1.0\tSO:unsorted");

        for (String sequenceName : this.gd.getSequencesNames()) {
          this.result.add("@SQ\tSN:"
              + sequenceName + "\tLN:"
              + this.gd.getSequenceLength(sequenceName));
        }
      }
      this.first = false;
    }

    internalParse(line, isPaired);

    return this.result;
  }

  private void internalParse(String line, boolean isPaired) {

    // If end of the input file
    if (line == null) {

      if (this.sLast != null) {
        this.result.add(join(sLast, "\t"));
        this.sLast = null;
      }

      return;
    }

    final String[] sCurr = convert(line, isPaired);

    if (sCurr == null) {
      return;
    }

    if (sLast != null && sLast[0].equals(sCurr[0])) {

      if (isPaired) { // Fix single end mode, added by Laurent Jourdren
        mating(sLast, sCurr);
      }
      this.result.add(join(this.sLast, "\t"));
      this.result.add(join(sCurr, "\t"));

      sLast = null;

    } else {

      if (sLast != null) {
        this.result.add(join(this.sLast, "\t"));
      }
      sLast = sCurr;
    }
  }

  public String[] convert(final String line, final boolean isPaired) {

    if (line == null) {
      return null;
    }

    final String s = removeForbiddenCharacter(line);

    final String[] tab = TAB_PATTERN.split(s);

    if (tab.length < 9 || s.charAt(0) == ' ' || s.charAt(0) == '\t') {
      return null;
    }

    final String[] result = new String[13];

    // Read name
    if (tab[0].endsWith("/1") || tab[0].endsWith("/2")) {
      result[0] = tab[0].substring(0, tab[0].length() - 2);
    } else {
      result[0] = tab[0];
    }

    // Initial flag (will be updated later)
    int flag = 0;
    if (isPaired) { // Fix single end mode, added by Laurent Jourdren
      flag |= 1 | 1 << ("a".equals(tab[4]) ? 6 : 7);
      // if (isPaired) {
      flag |= 2;
    }

    // Read and quality
    result[9] = tab[1];
    result[10] = tab[2].length() > tab[1].length()
        ? tab[2].substring(0, tab[1].length()) : tab[2];

    // Cigar
    result[5] = result[9].length() + "M";

    // Coor
    result[2] = tab[7];
    result[3] = tab[8];
    if ("-".equals(tab[6])) {
      flag |= 0x10;
    }
    result[1] = Integer.toString(flag);

    // MapQ
    result[4] = Integer.parseInt(tab[3]) == 1 ? "30" : "0";

    // Mate coordinate
    result[6] = "*";
    result[7] = "0";
    result[8] = "0";

    // Aux
    result[11] = "NM:i:" + tab[9];

    if (tab.length > 9) {

      final List<String> x = new ArrayList<>();

      for (int i = 10; i < tab.length; i++) {

        final Matcher m = PATTERN.matcher(tab[i]);
        if (m.find()) {
          x.add(String.format("%3d,%s", Integer.parseInt(m.group(2)),
              m.group(1)));
        }

      }

      Collections.sort(x);

      this.sb.setLength(0);
      int a = 0;

      for (String xs : x) {
        final String[] tab2 = xs.split(",");
        final int y = Integer.parseInt(tab2[0].trim());
        this.sb.append(y - a);
        this.sb.append(tab2[1]);
        a += y - a + 1;
      }
      this.sb.append(tab[1].length() - a);
      result[12] = "MD:Z:" + this.sb.toString();
    } else {
      result[12] = "MD:Z:";
    }

    return result;
  }

  private String removeForbiddenCharacter(final String line) {

    if (line == null) {
      return null;
    }

    this.sb.setLength(0);

    for (int i = 0; i < line.length(); i++) {

      final int c = line.charAt(i);

      if (c == '\t' || (c > 32 && c < 127)) {
        this.sb.append((char) c);
      }

    }
    return this.sb.toString();
  }

  private final static void mating(final String[] s1, final String[] s2) {

    int isize = 0;

    int s11 = Integer.parseInt(s1[1]);
    int s21 = Integer.parseInt(s2[1]);

    final int s13 = Integer.parseInt(s1[3]);
    final int s23 = Integer.parseInt(s2[3]);

    if (!s1[2].equals("*") && s1[2].equals(s2[2])) {
      // then calculate $isize

      final int x1 = (s11 & 0x10) == 0 ? s13 : s13 + s1[9].length();
      final int x2 = (s21 & 0x10) == 0 ? s23 : s23 + s2[9].length();

      isize = x2 - x1;
    }

    // update mate coordinate
    if (s2[2].equals("*")) {
      s11 |= 0x8;

    } else {
      s1[6] = s2[2].equals(s1[2]) ? "=" : s2[2];
      s1[7] = s2[3];
      s1[8] = Integer.toString(isize);

      if ((s21 & 0x10) != 0) {
        s11 |= 0x20;
      }
    }

    if (s1[2].equals("*")) {
      s21 |= 0x8;

    } else {
      s2[6] = s1[2].equals(s2[2]) ? "=" : s1[2];
      s2[7] = s1[3];
      s2[8] = Integer.toString(-isize);

      if ((s11 & 0x10) != 0) {
        s21 |= 0x20;
      }
    }

    s1[1] = Integer.toString(s11);
    s2[1] = Integer.toString(s21);

  }

  //
  // Constructor
  //

  public SOAP2SAM(final GenomeDescription gd) {

    this.gd = gd;
  }

}
