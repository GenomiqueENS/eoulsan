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

public class UnmodifiableExperimentSampleMetadataTest {

  @Test
  public void testUnmodifiableMethods() {

    Design d = DesignFactory.createEmptyDesign();
    Design ud = DesignFactory.unmodifiableDesign(d);

    d.addExperiment("1");

    Sample s1 = d.addSample("s1");

    Experiment e = d.getExperiment("1");
    Experiment ue = ud.getExperiment("1");
    e.addSample(s1);
    ExperimentSample ues = ue.getExperimentSamples().get(0);
    ExperimentSampleMetadata uesm = ues.getMetadata();

    try {
      uesm.setRepTechGroup(null);
        fail();
    } catch (UnsupportedOperationException ex) {
      assertTrue(true);
    }

    try {
      uesm.setReference(true);
        fail();
    } catch (UnsupportedOperationException ex) {
      assertTrue(true);
    }

    try {
      uesm.setReference(null);
        fail();
    } catch (UnsupportedOperationException ex) {
      assertTrue(true);
    }

    try {
      uesm.setCondition(null);
        fail();
    } catch (UnsupportedOperationException ex) {
      assertTrue(true);
    }

  }

  @Test
  public void test() {
    Design d = DesignFactory.createEmptyDesign();
    Design ud = DesignFactory.unmodifiableDesign(d);

    d.addExperiment("1");

    Sample s1 = d.addSample("s1");

    Experiment e = d.getExperiment("1");
    Experiment ue = ud.getExperiment("1");
    e.addSample(s1);
    ExperimentSample es = e.getExperimentSamples().get(0);
    ExperimentSample ues = ue.getExperimentSamples().get(0);
    ExperimentSampleMetadata esm = es.getMetadata();
    ExperimentSampleMetadata uesm = ues.getMetadata();

    // test getRepTechGroup
    assertNull(uesm.getRepTechGroup());
    // test setRepTechGroup
    esm.setRepTechGroup("toto");
    assertEquals("toto", uesm.getRepTechGroup());
    // test containsRepTechGroup
    assertTrue(uesm.containsRepTechGroup());

    // test getCondition
    assertNull(uesm.getCondition());
    // test setCondition
    esm.setCondition("toto");
    assertEquals("toto", uesm.getCondition());
    // test containsCondition
    assertTrue(uesm.containsCondition());

    // test getReference
    assertFalse(uesm.isReference());
    // test setReference
    esm.setReference(true);
    assertTrue(uesm.isReference());
    // test containsReference
    assertTrue(uesm.containsReference());
  }

}
