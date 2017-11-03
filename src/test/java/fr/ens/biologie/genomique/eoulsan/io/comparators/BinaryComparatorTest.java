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

import static fr.ens.biologie.genomique.eoulsan.io.CompressionType.getCompressionTypeByFilename;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.Test;

public class BinaryComparatorTest {
  private final File dir =
      new File(new File(".").getAbsolutePath() + "/src/test/java/files");

  private final File fileA = new File(this.dir, "mapper_results_1.bam");
  private final File fileB = new File(this.dir, "mapper_results_2.bam");
  private final File fileC = new File(this.dir, "mapper_results_1_modif.bam");

  private final AbstractComparator comparator = new BinaryComparator();

  @Test
  public void testSameBinaryFiles() throws Exception {

    final InputStream isA1 =
        getCompressionTypeByFilename(this.fileA.getAbsolutePath())
            .createInputStream(new FileInputStream(this.fileA));

    final InputStream isA2 =
        getCompressionTypeByFilename(this.fileA.getAbsolutePath())
            .createInputStream(new FileInputStream(this.fileA));

    assertTrue("files are same", this.comparator.compareFiles(isA1, isA2));
  }

  @Test
  public void testDifferentBinaryFiles() throws Exception {

    final InputStream isA =
        getCompressionTypeByFilename(this.fileA.getAbsolutePath())
            .createInputStream(new FileInputStream(this.fileA));

    final InputStream isB =
        getCompressionTypeByFilename(this.fileB.getAbsolutePath())
            .createInputStream(new FileInputStream(this.fileB));

    assertFalse("files are different", this.comparator.compareFiles(isA, isB));

  }

  @Test
  public void testDivergentSAM() throws Exception {

    // File mapper_results_SE.bam change few character in one read then generate
    // bam
    assertFalse("files are different: characters change",
        this.comparator.compareFiles(this.fileA, this.fileC));
  }

}
