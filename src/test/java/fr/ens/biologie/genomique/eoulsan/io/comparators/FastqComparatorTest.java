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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;

import org.junit.Test;

public class FastqComparatorTest {

  private final File dir =
      new File(new File(".").getAbsolutePath() + "/src/test/java/files");

  private InputStream isA;
  private InputStream isB;

  private final File fileA = new File(this.dir, "illumina_1_8.fastq");
  private final File fileB = new File(this.dir, "illumina_1_8.fastq");
  private File fileC;

  private void readFiles() throws Exception {

    this.isA = new FileInputStream(this.fileA);
    this.isB = new FileInputStream(this.fileB);
  }

  private void modifyFile(final int modification) throws Exception {
    this.fileC = new File(this.dir, "modify.fastq");

    if (this.fileC.exists()) {
      this.fileC.delete();
    }

    final BufferedReader br = new BufferedReader(new FileReader(this.fileA));
    final BufferedWriter bw = new BufferedWriter(new FileWriter(this.fileC));

    String line = "";
    // Number line for a header read
    final int numberLine = 53;
    int comp = 1;

    while ((line = br.readLine()) != null) {

      comp++;

      if (comp == numberLine) {

        switch (modification) {
        case 0:
          // duplicate read
          String seq = br.readLine();
          String plus = br.readLine();
          String quality = br.readLine();

          // Read first time
          bw.write(line + "\n" + seq + "\n" + plus + "\n" + quality + "\n");
          // Read second time
          bw.write(line + "\n" + seq + "\n" + plus + "\n" + quality + "\n");
          break;

        case 1:
          // Remove read
          // skip seq
          br.readLine();
          // Skip '+'
          br.readLine();
          // skip quality
          br.readLine();

          break;

        case 2:
          // Add read
          String newRead =
              "@HWI-1KL110:111:C3UVUACXX:3:1101:1224:2149 1:N:0:CTTGTA\n";
          newRead += "GTGTATTTGCTAATTTTTATTCTAGTTTTTCATTAAATAAATTTGACTTTC\n";
          newRead += "+\n";
          newRead += "B@BDFFFFHHHHHHJJJJHIJIJJJIJJJJJIIJJIIIJIIJJJJJJJJJJ\n";

          bw.write(newRead);
          bw.write(line + "\n");
          break;

        case 3:
          // remove a char in header line
          int pos = line.length() / 2;
          String newLine = line.substring(0, pos) + line.substring(pos + 2);

          bw.write(newLine + "\n");
          break;

        case 4:
          // add a char in header line
          int pos2 = line.length() / 2;
          String newLine2 =
              line.substring(0, pos2) + "t" + line.substring(pos2 + 1);
          bw.write(newLine2 + "\n");
          break;
        }
      } else {

        bw.write(line + "\n");
      }
    }

    br.close();
    bw.close();
  }

  @Test
  public void testSameFastq() throws Exception {
    AbstractComparatorWithBloomFilter comparator = new FastqComparator(false);

    readFiles();
    assertTrue("files are same", comparator.compareFiles(this.isA, this.isB));
  }

  @Test
  public void testDivergentFastq() throws Exception {

    AbstractComparatorWithBloomFilter comparator = new FastqComparator(false);

    modifyFile(0);
    assertFalse("files are different: duplicate read",
        comparator.compareFiles(this.fileA, this.fileC));

    modifyFile(1);
    assertFalse("files are different: remove read",
        comparator.compareFiles(this.fileA, this.fileC));

    modifyFile(2);
    assertFalse("files are different: add read",
        comparator.compareFiles(this.fileA, this.fileC));

    modifyFile(3);
    assertFalse("files are different: remove a char in one line",
        comparator.compareFiles(this.fileA, this.fileC));

    modifyFile(4);
    assertFalse("files are different: add a char in one line",
        comparator.compareFiles(this.fileA, this.fileC));

    if (this.fileC.exists()) {
      this.fileC.delete();
    }
  }

}
