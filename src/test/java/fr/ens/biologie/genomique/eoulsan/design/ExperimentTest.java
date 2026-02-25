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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ExperimentTest {

  @Test
  public void test() {
    Design d = DesignFactory.createEmptyDesign();
    d.addExperiment("1");
    d.addExperiment("2");

    // test getExperimentId
    assertEquals("1", d.getExperiment("1").getId());

    Experiment exp = d.getExperiment("1");
    exp.setName("ExperimentName1");

    // test getExperimentName
    assertEquals("ExperimentName1", d.getExperiment("1").getName());

    // test getExperimentMetadata
    assertNotNull(d.getExperiment("1").getMetadata());

    // test getExperimentSamples
    assertNotNull(d.getExperiment("1").getSamples());

    // test setExperimentName
    d.getExperiment("1").setName("toto");
    assertEquals("toto", d.getExperiment("1").getName());

    // test setExperimentName exception
    // set an exception off
    try {
      d.getExperiment("2").setName("toto");
      fail();
    } catch (IllegalArgumentException e) {
      assertTrue(true);
    }
    // don't set an exception off
    try {
      d.getExperiment("2").setName("titi");
      assertTrue(true);
    } catch (IllegalArgumentException e) {
      assertTrue(true);
    }
  }
}
