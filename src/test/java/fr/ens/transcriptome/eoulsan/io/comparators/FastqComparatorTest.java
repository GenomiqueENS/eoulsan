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

package fr.ens.transcriptome.eoulsan.io.comparators;

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

import fr.ens.transcriptome.eoulsan.io.comparators.AbstractComparatorWithBloomFilter;
import fr.ens.transcriptome.eoulsan.io.comparators.FastqComparator;

public class FastqComparatorTest {

  private File dir = new File(new File(".").getAbsolutePath()
      + "/src/test/java/files");

  private InputStream isA;
  private InputStream isB;

  private File fileA = new File(dir, "illumina_1_8.fastq");
  private File fileB = new File(dir, "illumina_1_8.fastq");
  private File fileC;
  
  private void readFiles() throws Exception {

    isA = new FileInputStream(fileA);
    isB = new FileInputStream(fileB);
  }

  private void modifyFile(final int modification) throws Exception {
    fileC = new File(dir, "modify.fastq");

    if (fileC.exists()) {
      fileC.delete();
    }

    final BufferedReader br = new BufferedReader(new FileReader(fileA));
    final BufferedWriter bw = new BufferedWriter(new FileWriter(fileC));

    String line = "";
    // Chose multi 4 corresponding to header fastq line
    final int numberLine = getRandomNumberLine();
    int comp = 1;

    while ((line = br.readLine()) != null) {

      comp++;

      if (comp == numberLine) {

        switch (modification) {
        case 0:
          // duplicate read
          String header = line;
          String seq = br.readLine();
          String plus = br.readLine();
          String quality = br.readLine();

          // Read first time
          bw.write(header + "\n" + seq + "\n" + plus + "\n" + quality + "\n");
          // Read second time
          bw.write(header + "\n" + seq + "\n" + plus + "\n" + quality + "\n");
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
    assertTrue("files are same", comparator.compareFiles(isA, isB));
  }

  @Test
  public void testSameFastqWithSerialization() throws Exception {

    AbstractComparatorWithBloomFilter comparator = new FastqComparator(true);
    
    // First call with creation serialisation file for save BloomFilter from
    // FileA
    modifyFile(0);
    assertFalse("files are different: duplicate read",
        comparator.compareFiles(fileA, fileC));

    File ser = new File(dir, fileA.getName() + ".ser");
    assertTrue("Check serialization exists ", ser.exists());

    // Use serialisation file
    modifyFile(1);
    assertFalse("files are different: remove read",
        comparator.compareFiles(fileA, fileC));

    modifyFile(2);
    assertFalse("files are different: add read",
        comparator.compareFiles(fileA, fileC));

    modifyFile(3);
    assertFalse("files are different: remove a char in one line",
        comparator.compareFiles(fileA, fileC));

    modifyFile(4);
    assertFalse("files are different: add a char in one line",
        comparator.compareFiles(fileA, fileC));

    // remove serialisation file
    if (ser.exists())
      ser.delete();

    if (fileC.exists())
      fileC.delete();
  }

  @Test
  public void testDivergentFastq() throws Exception {

    AbstractComparatorWithBloomFilter comparator = new FastqComparator(false);
    
    modifyFile(0);
    assertFalse("files are different: duplicate read",
        comparator.compareFiles(fileA, fileC));

    modifyFile(1);
    assertFalse("files are different: remove read",
        comparator.compareFiles(fileA, fileC));

    modifyFile(2);
    assertFalse("files are different: add read",
        comparator.compareFiles(fileA, fileC));

    modifyFile(3);
    assertFalse("files are different: remove a char in one line",
        comparator.compareFiles(fileA, fileC));

    modifyFile(4);
    assertFalse("files are different: add a char in one line",
        comparator.compareFiles(fileA, fileC));

    if (fileC.exists())
      fileC.delete();
  }

  private int getRandomNumberLine() {
    // Choice read in 200 first read in file
    final int max = 200;
    final int readNumber = (int) (Math.random() * max);

    // number line to header
    return ((readNumber - 1) * 4 + 1);
  }
}
