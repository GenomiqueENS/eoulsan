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
import static org.junit.Assert.fail;

import java.util.Collections;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.bio.FastqFormat;

public class UnmodifiableSampleMetadataTest {

  @Test
  public void testUnmodifiableMethods() {

    Design d = DesignFactory.createEmptyDesign();
    Design ud = DesignFactory.unmodifiableDesign(d);
    d.addSample("1");
    SampleMetadata usm = ud.getSample("1").getMetadata();

    try {
      usm.setReads(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      usm.setDescription(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      usm.setOperator(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      usm.setComment(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      usm.setDate(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      usm.setSerialNumber(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      usm.setUUID(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      usm.setRepTechGroup(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      usm.setReference(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      usm.setFastqFormat(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    try {
      usm.setCondition(null);
      fail();
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

  }

  @Test
  public void test() {
    Design d = DesignFactory.createEmptyDesign();
    Design ud = DesignFactory.unmodifiableDesign(d);
    d.addSample("1");
    SampleMetadata sm = d.getSample("1").getMetadata();
    SampleMetadata usm = ud.getSample("1").getMetadata();

    // test getReads
    assertNull(usm.getReads());
    // test setReads
    sm.setReads(Collections.singletonList("toto"));
    assertEquals(Collections.singletonList("toto"), usm.getReads());
    // test containsReads
    assertTrue(usm.containsReads());

    // test getDescription
    assertNull(usm.getDescription());
    // test setDescription
    sm.setDescription("toto");
    assertEquals("toto", usm.getDescription());
    // test containsDescription
    assertTrue(usm.containsDescription());

    // test getOperator
    assertNull(usm.getOperator());
    // test setOperator
    sm.setOperator("toto");
    assertEquals("toto", usm.getOperator());
    // test containsOperator
    assertTrue(usm.containsOperator());

    // test getComment
    assertNull(usm.getComment());
    // test setComment
    sm.setComment("toto");
    assertEquals("toto", usm.getComment());
    // test containsComment
    assertTrue(usm.containsComment());

    // test getDate
    assertNull(usm.getDate());
    // test setDate
    sm.setDate("toto");
    assertEquals("toto", usm.getDate());
    // test containsDate
    assertTrue(usm.containsDate());

    // test getSerialNumber
    assertNull(usm.getSerialNumber());
    // test setSerialNumber
    sm.setSerialNumber("toto");
    assertEquals("toto", usm.getSerialNumber());
    // test containsSerialNumber
    assertTrue(usm.containsSerialNumber());

    // test getUUID
    assertNull(usm.getUUID());
    // test setUUID
    sm.setUUID("toto");
    assertEquals("toto", usm.getUUID());
    // test containsUUID
    assertTrue(usm.containsUUID());

    // test getRepTechGroup
    assertNull(usm.getRepTechGroup());
    // test setRepTechGroup
    sm.setRepTechGroup("toto");
    assertEquals("toto", usm.getRepTechGroup());
    // test containsRepTechGroup
    assertTrue(usm.containsRepTechGroup());

    // test getFastqFormat
    assertNull(usm.getFastqFormat());
    // test setFastqFormat
    sm.setFastqFormat(FastqFormat.FASTQ_SOLEXA);
    assertEquals(FastqFormat.FASTQ_SOLEXA, usm.getFastqFormat());
    // test containsFastqFormat
    assertTrue(usm.containsFastqFormat());

    // test getCondition
    assertNull(usm.getCondition());
    // test setCondition
    sm.setCondition("toto");
    assertEquals("toto", usm.getCondition());
    // test containsCondition
    assertTrue(usm.containsCondition());
  }

}
