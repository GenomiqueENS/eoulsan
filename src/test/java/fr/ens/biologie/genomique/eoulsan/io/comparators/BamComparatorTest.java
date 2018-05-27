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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.junit.Test;

import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

public class BamComparatorTest {

  private final File dir =
      new File(new File(".").getAbsolutePath() + "/src/test/java/files");

  private final File fileA = new File(this.dir, "mapper_results_2.bam");
  // Same file then fileA
  private final File fileB = new File(this.dir, "mapper_results_2bis.bam");
  private File fileC;

  @Test
  public void testDifferentBAMFilesWithTag() throws Exception {
    final AbstractComparatorWithBloomFilter comparator =
        new BAMComparator(false);

    final InputStream isA = new FileInputStream(this.fileA);
    final InputStream isB = new FileInputStream(this.fileB);

    assertTrue("files are equals", comparator.compareFiles(isA, isB));
  }

  @Test
  public void testDivergentBAM() throws Exception {
    final AbstractComparatorWithBloomFilter comparator =
        new BAMComparator(false, "");

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

    this.fileC = new File(this.dir, "modify.bam");

    if (this.fileC.exists()) {
      this.fileC.delete();
    }

    try (final SamReader bamReader =
        SamReaderFactory.makeDefault().open(this.fileA)) {

      final SAMFileWriter samWriter =
          new SAMFileWriterFactory().setCreateIndex(false)
              .setTempDirectory(new File(System.getProperty("java.io.tmpdir")))
              .makeBAMWriter(bamReader.getFileHeader(), false, this.fileC);

      // Get iterator on file
      final Iterator<SAMRecord> it = bamReader.iterator();

      // Chose no header line
      final int numberLine = 33;
      int comp = 0;

      // Parse file
      while (it.hasNext()) {

        final SAMRecord r = it.next();

        comp++;

        if (comp == numberLine) {

          switch (typeModification) {

          case 0:
            // duplicate SAM line, no header
            // first time
            samWriter.addAlignment(r);
            // second time
            samWriter.addAlignment(r);
            break;

          case 1:
            // Remove read
            // no write current line
            break;

          case 2:
            samWriter.addAlignment(r);

            // Modify record
            final SAMRecord newSAMRecord = (SAMRecord) r.clone();
            newSAMRecord
                .setReadName("HWI-1KL110:37:C0BE6ACXX:7:1101:1426:2207");
            newSAMRecord.setBaseQualityString(
                "##############################################EEE:E=<?5=?#BAAF=AFFEFFFDE?EEE");

            samWriter.addAlignment(newSAMRecord);
            break;

          case 3:
            // remove a char in header line
            r.setReadName(r.getReadName().substring(2));

            samWriter.addAlignment(r);
            break;

          case 4:
            // add a char in header line
            final String txt = r.getReadName();
            final int pos2 = txt.length() / 2;
            r.setReadName(
                txt.substring(0, pos2) + "t" + txt.substring(pos2 + 1));

            samWriter.addAlignment(r);
            break;
          }
        }
      }
    } catch (final Exception e) {

    }

    assertTrue("Create modify BAM file, cann't be empty ?",
        this.fileC.length() < 10);

  }
}
