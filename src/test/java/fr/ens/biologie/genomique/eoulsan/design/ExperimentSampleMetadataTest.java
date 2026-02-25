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

public class ExperimentSampleMetadataTest {

  @Test
  public void test() {
    Design d = DesignFactory.createEmptyDesign();
    d.addExperiment("1");

    Sample s1 = d.addSample("s1");

    Experiment e = d.getExperiment("1");
    e.addSample(s1);
    ExperimentSample es = e.getExperimentSamples().get(0);
    ExperimentSampleMetadata esm = es.getMetadata();

    // test getRepTechGroup
    assertNull(esm.getRepTechGroup());
    // test setRepTechGroup
    esm.setRepTechGroup("toto");
    assertEquals("toto", esm.getRepTechGroup());
    // test containsRepTechGroup
    assertTrue(esm.containsRepTechGroup());

    // test getCondition
    assertNull(esm.getCondition());
    // test setCondition
    esm.setCondition("toto");
    assertEquals("toto", esm.getCondition());
    // test containsCondition
    assertTrue(esm.containsCondition());

    // test getReference
    assertFalse(esm.isReference());
    // test setReference
    esm.setReference(true);
    assertTrue(esm.isReference());
    // test containsReference
    assertTrue(esm.containsReference());
  }
}
