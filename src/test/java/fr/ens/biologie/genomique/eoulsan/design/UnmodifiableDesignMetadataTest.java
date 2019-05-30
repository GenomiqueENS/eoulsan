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

package fr.ens.biologie.genomique.eoulsan.design;

import org.junit.Test;

import static org.junit.Assert.*;

public class UnmodifiableDesignMetadataTest {

  @Test
  public void testUnmodifiableMethods() {

    Design d = DesignFactory.createEmptyDesign();
    Design ud = DesignFactory.unmodifiableDesign(d);
    DesignMetadata udm = ud.getMetadata();

    try {
      udm.setGenomeFile(null);
        fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      udm.setGffFile(null);
        fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      udm.setGtfFile(null);
        fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      udm.setAdditionalAnnotationFile(null);
        fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

  }

  @Test
  public void test() {

    Design d = DesignFactory.createEmptyDesign();
    Design ud = DesignFactory.unmodifiableDesign(d);

    DesignMetadata dm = d.getMetadata();
    DesignMetadata udm = ud.getMetadata();

    // test getGenomeFile
    assertNull(udm.getGenomeFile());
    // test setGenomeFile
    dm.setGenomeFile("/home/toto/titi.fasta");
    assertEquals("/home/toto/titi.fasta", udm.getGenomeFile());
    // test containsGenomeFile
    assertTrue(udm.containsGenomeFile());

    // test getGffFile
    assertNull(udm.getGffFile());
    // test setGffFile
    dm.setGffFile("/home/toto/titi.gff");
    assertEquals("/home/toto/titi.gff", udm.getGffFile());
    // test containsGffFile
    assertTrue(udm.containsGffFile());

    // test getAdditionalAnnotationFile
    assertNull(udm.getAdditionalAnnotationFile());
    // test setAdditionalAnnotationFile
    dm.setAdditionalAnnotationFile("/home/toto/titi.txt");
    assertEquals("/home/toto/titi.txt", udm.getAdditionalAnnotationFile());
    // test containsAdditionalAnnotationFile
    assertTrue(udm.containsAdditionalAnnotationFile());

  }

}
