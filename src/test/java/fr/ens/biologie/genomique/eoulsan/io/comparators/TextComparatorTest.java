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

public class TextComparatorTest {
  private final File dir =
      new File(new File(".").getAbsolutePath() + "/src/test/java/files");

  private final File fileA = new File(this.dir, "testdataformat.xml");
  private final File fileB = new File(this.dir, "phix.fasta");
  private File fileC;

  @Test
  public void testSameTextFiles() throws Exception {

    final InputStream isA1 = new FileInputStream(this.fileA);
    final InputStream isA2 = new FileInputStream(this.fileA);

    AbstractComparatorWithBloomFilter comparator =
        new SAMComparator(false, "@PG");
    assertTrue("files are same", comparator.compareFiles(isA1, isA2));
  }

  @Test
  public void testDifferentTextFiles() throws Exception {

    final InputStream isA = new FileInputStream(this.fileA);
    final InputStream isB = new FileInputStream(this.fileB);

    AbstractComparatorWithBloomFilter comparator =
        new SAMComparator(false, "@PG");
    assertFalse("files are different", comparator.compareFiles(isA, isB));
  }

  @Test
  public void testDivergentText() throws Exception {
    AbstractComparatorWithBloomFilter comparator =
        new SAMComparator(false, "@PG");

    modifyFile(0);
    assertFalse("files are different: duplicate line",
        comparator.compareFiles(this.fileA, this.fileC));

    modifyFile(1);
    assertFalse("files are different: remove line",
        comparator.compareFiles(this.fileA, this.fileC));

    modifyFile(2);
    assertFalse("files are different: add line",
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
    this.fileC = new File(this.dir, "modify.txt");

    if (this.fileC.exists()) {
      this.fileC.delete();
    }

    final BufferedReader br = new BufferedReader(new FileReader(this.fileA));
    final BufferedWriter bw = new BufferedWriter(new FileWriter(this.fileC));

    String line = "";
    // Chose multi 4 corresponding to header fastq line
    final int numberLine = getRandomNumberLine();
    int comp = 0;

    while ((line = br.readLine()) != null) {

      comp++;

      if (comp == numberLine) {

        switch (typeModification) {

        case 0:
          // duplicate line
          // first time
          bw.write(line + "\n");
          // second time
          bw.write(line + "\n");
          break;

        case 1:
          // Remove line
          // no write current line
          break;

        case 2:
          // Add line
          String newLine = "<!--totoformat -->\n";

          bw.write(newLine);
          bw.write(line + "\n");
          break;

        case 3:
          // remove a char in header line
          int pos = line.length() / 2;
          String modifiedLine =
              line.substring(0, pos) + line.substring(pos + 2);

          bw.write(modifiedLine + "\n");
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

  private int getRandomNumberLine() {
    // choice line in the first 10

    final int max = 10;
    return (int) (Math.random() * max);
  }
}
