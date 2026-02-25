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
import static org.junit.Assert.fail;

import org.junit.Test;

public class UnmodifiableDesignTest {

  @Test
  public void testUnmodifiableMethods() {

    Design d = DesignFactory.createEmptyDesign();
    Design ud = DesignFactory.unmodifiableDesign(d);

    try {
      ud.setName(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      ud.removeSample(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      ud.removeExperiment(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      ud.addSample(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      ud.addExperiment(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }
  }

  @Test
  public void testSample() {
    Design d = DesignFactory.createEmptyDesign();
    Design ud = DesignFactory.unmodifiableDesign(d);

    // test addSample
    d.addSample("1");

    // test containsSample
    assertTrue(ud.containsSample("1"));
    assertEquals(1, ud.getSamples().size());

    // test getSample
    assertNotNull(ud.getSample("1"));

    Sample s1 = d.getSample("1");
    s1.setName("MySample1");

    // test getSamples
    d.addSample("2");
    assertTrue(ud.containsSample("2"));
    assertNotNull(ud.getSamples());
    assertEquals(2, ud.getSamples().size());

    Sample s2 = d.getSample("2");
    s2.setName("MySample2");

    // test removeSample
    d.removeSample("2");

    // test the negative response of containsSample
    assertFalse(ud.containsSample("2"));

    // test containsSampleName
    assertTrue(ud.containsSampleName("MySample1"));
    assertFalse(ud.containsSampleName("MySample2"));
  }

  @Test
  public void testExperiment() {
    Design d = DesignFactory.createEmptyDesign();
    Design ud = DesignFactory.unmodifiableDesign(d);

    // test addExperiment
    d.addExperiment("1");

    // test containsExperiment
    assertTrue(ud.containsExperiment("1"));

    // test getExperiment
    assertNotNull(ud.getExperiment("1"));

    // test getExperiments
    d.addExperiment("2");
    assertTrue(ud.containsExperiment("2"));
    assertNotNull(ud.getExperiments());

    Experiment exp2 = d.getExperiment("2");
    exp2.setName("ExperimentName2");

    // test removeExperiment
    d.removeExperiment("2");

    // test negative response of containsExperiment
    assertFalse(ud.containsExperiment("2"));

    Experiment exp1 = d.getExperiment("1");
    exp1.setName("ExperimentName1");

    // test containsExperimentName
    assertTrue(ud.containsExperimentName("ExperimentName1"));
    assertFalse(ud.containsExperimentName("ExperimentName2"));
  }

  @Test
  public void testMetadata() {
    Design d = DesignFactory.createEmptyDesign();
    Design ud = DesignFactory.unmodifiableDesign(d);

    // test getMetadata
    assertNotNull(ud.getMetadata());
  }

  @Test
  public void testDesignAttribute() {

    Design d = DesignFactory.createEmptyDesign();
    Design ud = DesignFactory.unmodifiableDesign(d);

    d.setName("MyDesign");

    // test getDesignNumber
    assertEquals("MyDesign", ud.getName());
  }
}
