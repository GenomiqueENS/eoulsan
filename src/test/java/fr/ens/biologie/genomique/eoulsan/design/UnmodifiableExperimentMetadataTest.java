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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class UnmodifiableExperimentMetadataTest {

  @Test
  public void testUnmodifiableMethods() {

    Design d = DesignFactory.createEmptyDesign();
    Design ud = DesignFactory.unmodifiableDesign(d);
    d.addExperiment("1");
    ExperimentMetadata uem = ud.getExperiment("1").getMetadata();

    try {
      uem.setSkip(true);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      uem.setReference(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      uem.setModel(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      uem.setContrast(true);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      uem.setBuildContrast(true);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      uem.setDesignFile(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      uem.setComparisons(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      uem.setContrastFile(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }
  }

  @Test
  public void test() {
    Design d = DesignFactory.createEmptyDesign();
    Design ud = DesignFactory.unmodifiableDesign(d);

    d.addExperiment("1");
    ExperimentMetadata em = d.getExperiment("1").getMetadata();
    ExperimentMetadata uem = ud.getExperiment("1").getMetadata();

    // test getSkip
    assertFalse(uem.containsSkip());
    // test setSkip
    em.setSkip(false);
    assertTrue(uem.containsSkip());
    assertFalse(uem.isSkip());
    em.setSkip(true);
    assertTrue(uem.isSkip());
    // test containsSkip
    assertTrue(uem.containsSkip());

    // test getReference
    assertNull(uem.getReference());
    // test setReference
    em.setReference("toto");
    assertEquals("toto", uem.getReference());
    // test containsReference
    assertTrue(uem.containsReference());

    // test getModel
    assertNull(uem.getModel());
    // test setModel
    em.setModel("toto");
    assertEquals("toto", uem.getModel());
    // test containsModel
    assertTrue(uem.containsModel());

    // test getContrast
    assertFalse(uem.containsContrast());
    // test setContrast
    em.setContrast(true);
    assertTrue(uem.isContrast());
    // test containsContrast
    assertTrue(uem.containsContrast());

    // test getBuildContrast
    assertFalse(uem.containsBuildContrast());
    // test setBuildContrast
    em.setBuildContrast(true);
    assertTrue(uem.isBuildContrast());
    // test containsBuildContrast
    assertTrue(uem.containsBuildContrast());

    // test getDesignFile
    assertNull(uem.getDesignFile());
    // test setDesignFile
    em.setDesignFile("toto");
    assertEquals("toto", uem.getDesignFile());
    // test containsDesignFile
    assertTrue(uem.containsDesignFile());

    // test getComparisonFile
    assertNull(uem.getComparisons());
    // test setComparisonFile
    em.setComparisons("toto");
    assertEquals("toto", uem.getComparisons());
    // test containsComparisonFile
    assertTrue(uem.containsComparisons());

    // test getContrastFile
    assertNull(uem.getContrastFile());
    // test setContrastFile
    em.setContrastFile("toto");
    assertEquals("toto", uem.getContrastFile());
    // test containsContrastFile
    assertTrue(uem.containsContrastFile());
  }
}
