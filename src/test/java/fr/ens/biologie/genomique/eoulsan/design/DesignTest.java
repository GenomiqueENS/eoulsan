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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DesignTest {

  @Test
  public void testSample() {
    Design d = DesignFactory.createEmptyDesign();

    // test addSample
    d.addSample("1");

    // test containsSample
    assertTrue(d.containsSample("1"));
    assertEquals(1, d.getSamples().size());

    // test getSample
    assertNotNull(d.getSample("1"));

    Sample s1 = d.getSample("1");
    s1.setName("MySample1");

    // test getSamples
    d.addSample("2");
    assertTrue(d.containsSample("2"));
    assertNotNull(d.getSamples());
    assertEquals(2, d.getSamples().size());

    Sample s2 = d.getSample("2");
    s2.setName("MySample2");

    // test removeSample
    d.removeSample("2");

    // test the negative response of containsSample
    assertFalse(d.containsSample("2"));

    // test containsSampleName
    assertTrue(d.containsSampleName("MySample1"));
    assertFalse(d.containsSampleName("MySample2"));
  }

  @Test
  public void testExperiment() {
    Design d = DesignFactory.createEmptyDesign();

    // test addExperiment
    d.addExperiment("1");

    // test containsExperiment
    assertTrue(d.containsExperiment("1"));

    // test getExperiment
    assertNotNull(d.getExperiment("1"));

    // test getExperiments
    d.addExperiment("2");
    assertTrue(d.containsExperiment("2"));
    assertNotNull(d.getExperiments());

    Experiment exp2 = d.getExperiment("2");
    exp2.setName("ExperimentName2");

    // test removeExperiment
    d.removeExperiment("2");

    // test negative response of containsExperiment
    assertFalse(d.containsExperiment("2"));

    Experiment exp1 = d.getExperiment("1");
    exp1.setName("ExperimentName1");

    // test containsExperimentName
    assertTrue(d.containsExperimentName("ExperimentName1"));
    assertFalse(d.containsExperimentName("ExperimentName2"));
  }

  @Test
  public void testMetadata() {
    Design d = DesignFactory.createEmptyDesign();

    // test getMetadata
    assertNotNull(d.getMetadata());
  }

  @Test
  public void testDesignAttribute() {

    Design d = DesignFactory.createEmptyDesign();

    d.setName("MyDesign");

    // test getDesignNumber
    assertEquals("MyDesign", d.getName());
  }
}
