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
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

public class SamComparatorTest {

  private final File dir =
      new File(new File(".").getAbsolutePath() + "/src/test/java/files");

  private InputStream isA;
  private InputStream isB;

  private final File fileA = new File(this.dir, "mapper_results_1_a.sam");
  // Difference tag @PG
  private final File fileB = new File(this.dir, "mapper_results_1_b.sam");
  private File fileC;

  @Test
  public void testSameSAMFiles() throws Exception {

    this.isA = new FileInputStream(this.fileA);
    this.isB = new FileInputStream(this.fileB);

    AbstractComparatorWithBloomFilter comparator =
        new SAMComparator(false, "PG");
    assertTrue("files are same without tag header @PG",
        comparator.compareFiles(this.isA, this.isB));

    this.isA = new FileInputStream(this.fileA);
    this.isB = new FileInputStream(this.fileB);

    AbstractComparatorWithBloomFilter comparator2 = new SAMComparator(false);
    assertFalse("files are different with all tag header",
        comparator2.compareFiles(this.isA, this.isB));

    this.isA = new FileInputStream(this.fileA);
    this.isB = new FileInputStream(this.fileB);

    AbstractComparatorWithBloomFilter comparator3 =
        new SAMComparator(false, "PG", "SQ");
    assertTrue("files are same without all tags",
        comparator3.compareFiles(this.isA, this.isB));
  }

  @Test
  public void testDifferentSAMFilesWithTag() throws Exception {
    AbstractComparatorWithBloomFilter comparator = new SAMComparator(false);

    this.isA = new FileInputStream(this.fileA);
    this.isB = new FileInputStream(this.fileB);

    assertFalse("files are different",
        comparator.compareFiles(this.isA, this.isB));
  }

  @Test
  public void testDivergentSAM() throws Exception {
    AbstractComparatorWithBloomFilter comparator =
        new SAMComparator(false, "@PG");

    modifyFile(0);
    assertFalse("files are different: duplicate SAM line",
        comparator.compareFiles(this.fileA, this.fileC));

    modifyFile(1);
    assertFalse("files are different: remove SAM line",
        comparator.compareFiles(this.fileA, this.fileC));

    modifyFile(2);
    assertFalse("files are different: add SAM line",
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

  private void modifyFile(final int typeModification) throws IOException {
    this.fileC = new File(this.dir, "modify.sam");

    if (this.fileC.exists()) {
      this.fileC.delete();
    }

    final BufferedReader br = new BufferedReader(new FileReader(this.fileA));
    final BufferedWriter bw = new BufferedWriter(new FileWriter(this.fileC));

    String line = "";
    // Chose no header line
    final int numberLine = 33;
    int comp = 0;

    while ((line = br.readLine()) != null) {

      comp++;

      if (comp == numberLine) {

        switch (typeModification) {

        case 0:
          // duplicate SAMline, no header
          // first time
          bw.write(line + "\n");
          // second time
          bw.write(line + "\n");
          break;

        case 1:
          // Remove read
          // no write current line
          break;

        case 2:
          // Add read
          String newSAMline =
              "HWI-1KL110:37:C0BE6ACXX:7:1101:1426:2207  147 chr17   35400811    40  101M    =   35400491    -421    GTTTCAGGCTGGGGGAGGGGAGACTACATCTCCTCNNNNCTCCTCTTCCATGCGGCGAAGGGTCTCACTGATGAAC   ##############################################EEE:E=<?5=?#BAAF=AFFEFFFDE?EEE   MD:Z:101    NH:i:1  HI:i:1  NM:i:0  SM:i:40 XQ:i:40 X2:i:0";

          bw.write(newSAMline);
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
      }
    }
    br.close();
    bw.close();
  }

}
