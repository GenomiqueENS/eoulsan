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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

import fr.ens.biologie.genomique.kenetre.bio.FastqFormat;

public class SampleMetadataTest {

  @Test
  public void test() {
    Design d = DesignFactory.createEmptyDesign();
    d.addSample("1");
    SampleMetadata sm = d.getSample("1").getMetadata();

    // test getReads
    assertNull(sm.getReads());
    // test setReads
    sm.setReads(Collections.singletonList("toto"));
    assertEquals(Collections.singletonList("toto"), sm.getReads());
    // test containsReads
    assertTrue(sm.containsReads());

    // test getDescription
    assertNull(sm.getDescription());
    // test setDescription
    sm.setDescription("toto");
    assertEquals("toto", sm.getDescription());
    // test containsDescription
    assertTrue(sm.containsDescription());

    // test getOperator
    assertNull(sm.getOperator());
    // test setOperator
    sm.setOperator("toto");
    assertEquals("toto", sm.getOperator());
    // test containsOperator
    assertTrue(sm.containsOperator());

    // test getComment
    assertNull(sm.getComment());
    // test setComment
    sm.setComment("toto");
    assertEquals("toto", sm.getComment());
    // test containsComment
    assertTrue(sm.containsComment());

    // test getDate
    assertNull(sm.getDate());
    // test setDate
    sm.setDate("toto");
    assertEquals("toto", sm.getDate());
    // test containsDate
    assertTrue(sm.containsDate());

    // test getSerialNumber
    assertNull(sm.getSerialNumber());
    // test setSerialNumber
    sm.setSerialNumber("toto");
    assertEquals("toto", sm.getSerialNumber());
    // test containsSerialNumber
    assertTrue(sm.containsSerialNumber());

    // test getUUID
    assertNull(sm.getUUID());
    // test setUUID
    sm.setUUID("toto");
    assertEquals("toto", sm.getUUID());
    // test containsUUID
    assertTrue(sm.containsUUID());

    // test getRepTechGroup
    assertNull(sm.getRepTechGroup());
    // test setRepTechGroup
    sm.setRepTechGroup("toto");
    assertEquals("toto", sm.getRepTechGroup());
    // test containsRepTechGroup
    assertTrue(sm.containsRepTechGroup());

    // test getFastqFormat
    assertNull(sm.getFastqFormat());
    // test setFastqFormat
    sm.setFastqFormat(FastqFormat.FASTQ_SOLEXA);
    assertEquals(FastqFormat.FASTQ_SOLEXA, sm.getFastqFormat());
    // test containsFastqFormat
    assertTrue(sm.containsFastqFormat());

    // test getCondition
    assertNull(sm.getCondition());
    // test setCondition
    sm.setCondition("toto");
    assertEquals("toto", sm.getCondition());
    // test containsCondition
    assertTrue(sm.containsCondition());
  }

}
