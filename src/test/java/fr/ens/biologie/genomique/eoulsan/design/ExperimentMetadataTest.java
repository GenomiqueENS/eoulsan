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

import org.junit.Test;

public class ExperimentMetadataTest {

  @Test
  public void test() {
    Design d = DesignFactory.createEmptyDesign();
    d.addExperiment("1");
    ExperimentMetadata em = d.getExperiment("1").getMetadata();

    // test getSkip
    assertFalse(em.containsSkip());
    // test setSkip
    em.setSkip(false);
    assertTrue(em.containsSkip());
    assertFalse(em.isSkip());
    em.setSkip(true);
    assertTrue(em.isSkip());
    // test containsSkip
    assertTrue(em.containsSkip());

    // test getReference
    assertNull(em.getReference());
    // test setReference
    em.setReference("toto");
    assertEquals("toto", em.getReference());
    // test containsReference
    assertTrue(em.containsReference());

    // test getModel
    assertNull(em.getModel());
    // test setModel
    em.setModel("toto");
    assertEquals("toto", em.getModel());
    // test containsModel
    assertTrue(em.containsModel());

    // test getContrast
    assertFalse(em.containsContrast());
    // test setContrast
    em.setContrast(true);
    assertTrue(em.isContrast());
    // test containsContrast
    assertTrue(em.containsContrast());

    // test getBuildContrast
    assertFalse(em.containsBuildContrast());
    // test setBuildContrast
    em.setBuildContrast(true);
    assertTrue(em.isBuildContrast());
    // test containsBuildContrast
    assertTrue(em.containsBuildContrast());

    // test getDesignFile
    assertNull(em.getDesignFile());
    // test setDesignFile
    em.setDesignFile("toto");
    assertEquals("toto", em.getDesignFile());
    // test containsDesignFile
    assertTrue(em.containsDesignFile());

    // test getComparisonFile
    assertNull(em.getComparisons());
    // test setComparisonFile
    em.setComparisons("toto");
    assertEquals("toto", em.getComparisons());
    // test containsComparisonFile
    assertTrue(em.containsComparisons());

    // test getContrastFile
    assertNull(em.getContrastFile());
    // test setContrastFile
    em.setContrastFile("toto");
    assertEquals("toto", em.getContrastFile());
    // test containsContrastFile
    assertTrue(em.containsContrastFile());
  }
}
