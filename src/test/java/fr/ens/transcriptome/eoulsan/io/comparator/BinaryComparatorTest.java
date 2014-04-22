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
package fr.ens.transcriptome.eoulsan.io.comparator;

import static fr.ens.transcriptome.eoulsan.io.CompressionType.getCompressionTypeByFilename;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.Test;

public class BinaryComparatorTest {
  private File dir = new File(new File(".").getAbsolutePath()
      + "/src/test/java/files");

  private InputStream isA;
  private InputStream isB;

  private File fileA = new File(dir, "mapper_results_1.bam");
  private File fileB = new File(dir, "mapper_results_2.bam");
  private File fileC = new File(dir, "mapper_results_1_modif.bam");

  private final AbstractComparator comparator = new BinaryComparator();

  @Test
  public void testSameBinaryFiles() throws Exception {

    final InputStream isA1 =
        getCompressionTypeByFilename(fileA.getAbsolutePath())
            .createInputStream(new FileInputStream(fileA));

    final InputStream isA2 =
        getCompressionTypeByFilename(fileA.getAbsolutePath())
            .createInputStream(new FileInputStream(fileA));

    assertTrue("files are same", comparator.compareFiles(isA1, isA2));
  }

  @Test
  public void testDifferentBinaryFiles() throws Exception {

    isA =
        getCompressionTypeByFilename(fileA.getAbsolutePath())
            .createInputStream(new FileInputStream(fileA));

    isB =
        getCompressionTypeByFilename(fileB.getAbsolutePath())
            .createInputStream(new FileInputStream(fileB));

    assertFalse("files are different", comparator.compareFiles(isA, isB));

  }

  @Test
  public void testDivergentSAM() throws Exception {

    // File mapper_results_SE.bam change few character in one read then generate
    // bam
    assertFalse("files are different: characters change",
        comparator.compareFiles(fileA, fileC));
  }

}
