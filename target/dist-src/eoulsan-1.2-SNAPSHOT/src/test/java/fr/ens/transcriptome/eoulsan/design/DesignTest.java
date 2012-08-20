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

package fr.ens.transcriptome.eoulsan.design;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;

public class DesignTest {

  @Test
  public void testCompareStringString() throws EoulsanIOException {

    Design d = DesignFactory.createEmptyDesign();

    d.addSample("G5_1");
    assertEquals(true, d.isSample("G5_1"));
    assertEquals(1, d.getSample("G5_1").getId());
    d.addSample("G3_1");
    assertEquals(true, d.isSample("G5_1"));
    assertEquals(true, d.isSample("G3_1"));
    assertEquals(1, d.getSample("G5_1").getId());
    assertEquals(2, d.getSample("G3_1").getId());

    d.getSample("G5_1").setId(5);
    assertEquals(5, d.getSample("G5_1").getId());

    d.getSample("G5_1").getMetadata().setDescription("Test G5");
    assertEquals("Test G5", d.getSample("G5_1").getMetadata().getDescription());
    d.getSample("G3_1").getMetadata().setDescription("Test G3");
    assertEquals("Test G5", d.getSample("G5_1").getMetadata().getDescription());
    assertEquals("Test G3", d.getSample("G3_1").getMetadata().getDescription());
    assertEquals(5, d.getSample("G5_1").getId());

  }
}
