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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.bio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.bio.BadBioEntryException;
import fr.ens.biologie.genomique.eoulsan.bio.GFFEntry;

public class GFFEntryTest {

  private final String[] TEST_STRINGS = {

      "ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN",
      "ctg123\t.\tTF_binding_site\t1000\t1012\t.\t+\t.\tID=tfbs00001;Parent=gene00001",
      "ctg123\t.\tmRNA\t1050\t9000\t.\t+\t.\tID=mRNA00001;Parent=gene00001;Name=EDEN.1",
      "ctg123\t.\tmRNA\t1050\t9000\t.\t+\t.\tID=mRNA00002;Parent=gene00001;Name=EDEN.2",
      "ctg123\t.\tmRNA\t1300\t9000\t.\t+\t.\tID=mRNA00003;Parent=gene00001;Name=EDEN.3",
      "ctg123\t.\texon\t1300\t1500\t.\t+\t.\tID=exon00001;Parent=mRNA00003",
      "ctg123\t.\texon\t1050\t1500\t.\t+\t.\tID=exon00002;Parent=mRNA00001,mRNA00002",
      "ctg123\t.\texon\t3000\t3902\t.\t+\t.\tID=exon00003;Parent=mRNA00001,mRNA00003",
      "ctg123\t.\texon\t5000\t5500\t.\t+\t.\tID=exon00004;Parent=mRNA00001,mRNA00002,mRNA00003",
      "ctg123\t.\texon\t7000\t9000\t.\t+\t.\tID=exon00005;Parent=mRNA00001,mRNA00002,mRNA00003",
      "ctg123\t.\tCDS\t1201\t1500\t.\t+\t0\tID=cds00001;Parent=mRNA00001;Name=edenprotein.1",
      "ctg123\t.\tCDS\t3000\t3902\t.\t+\t0\tID=cds00001;Parent=mRNA00001;Name=edenprotein.1",
      "ctg123\t.\tCDS\t5000\t5500\t.\t+\t0\tID=cds00001;Parent=mRNA00001;Name=edenprotein.1",
      "ctg123\t.\tCDS\t7000\t7600\t.\t+\t0\tID=cds00001;Parent=mRNA00001;Name=edenprotein.1",
      "ctg123\t.\tCDS\t1201\t1500\t.\t+\t0\tID=cds00002;Parent=mRNA00002;Name=edenprotein.2",
      "ctg123\t.\tCDS\t5000\t5500\t.\t+\t0\tID=cds00002;Parent=mRNA00002;Name=edenprotein.2",
      "ctg123\t.\tCDS\t7000\t7600\t.\t+\t0\tID=cds00002;Parent=mRNA00002;Name=edenprotein.2",
      "ctg123\t.\tCDS\t3301\t3902\t.\t+\t0\tID=cds00003;Parent=mRNA00003;Name=edenprotein.3",
      "ctg123\t.\tCDS\t5000\t5500\t.\t+\t1\tID=cds00003;Parent=mRNA00003;Name=edenprotein.3",
      "ctg123\t.\tCDS\t7000\t7600\t.\t+\t2\tID=cds00003;Parent=mRNA00003;Name=edenprotein.3",
      "ctg123\t.\tCDS\t3391\t3902\t.\t+\t0\tID=cds00004;Parent=mRNA00003;Name=edenprotein.4",
      "ctg123\t.\tCDS\t5000\t5500\t.\t+\t1\tID=cds00004;Parent=mRNA00003;Name=edenprotein.4",
      "Ctg123\t.\tCDS\t7000\t7600\t.\t+\t2\tID=cds00004;Parent=mRNA00003;Name=edenprotein.4", };

  @Test
  public void testParse() {

    GFFEntry e = new GFFEntry();

    for (String s : this.TEST_STRINGS) {

      try {
        e.parse(s);
      } catch (BadBioEntryException exp) {
        assertTrue(false);
      }

      assertEquals(s, e.toString());
    }

  }

  @Test
  public void testMinimalEntry() {

    GFFEntry e = new GFFEntry();
    e.setSeqId("theId");
    e.setSource("thesource");
    e.setType("thetype");

    e.setStart(1);
    e.setEnd(1000);

    assertTrue(e.isValidPhase());
    assertTrue(e.isValidStartAndEnd());
    assertTrue(e.isValidStrand());
    assertTrue(e.isValidEntry());
  }

  @Test
  public void testGetId() {

    GFFEntry e = new GFFEntry();
    e.setId(9999);
    assertEquals(9999, e.getId());
  }

  @Test
  public void testGetSeqId() {

    GFFEntry e = new GFFEntry();
    try {
      e.parse("ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      assertTrue(false);
    }

    assertEquals("ctg123", e.getSeqId());
  }

  @Test
  public void testGetSource() {

    GFFEntry e = new GFFEntry();
    try {
      e.parse(
          "ctg123\tGenbank\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      assertTrue(false);
    }

    assertEquals("Genbank", e.getSource());
  }

  @Test
  public void testGetType() {

    GFFEntry e = new GFFEntry();
    try {
      e.parse("ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      assertTrue(false);
    }

    assertEquals("gene", e.getType());
  }

  @Test
  public void testGetStart() {

    GFFEntry e = new GFFEntry();
    try {
      e.parse("ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      assertTrue(false);
    }

    assertEquals(1000, e.getStart());
  }

  @Test
  public void testGetEnd() {

    GFFEntry e = new GFFEntry();
    try {
      e.parse("ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      assertTrue(false);
    }

    assertEquals(9000, e.getEnd());
  }

  @Test
  public void testGetScore() {

    GFFEntry e = new GFFEntry();
    try {
      e.parse("ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      assertTrue(false);
    }

    assertEquals(Double.NaN, e.getScore(), 0.0);
  }

  @Test
  public void testGetStrand() {

    GFFEntry e = new GFFEntry();
    try {
      e.parse("ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      assertTrue(false);
    }

    assertEquals('+', e.getStrand());
  }

  @Test
  public void testGetPhase() {

    GFFEntry e = new GFFEntry();
    try {
      e.parse("ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      assertTrue(false);
    }

    assertEquals(-1, e.getPhase());

    e.setType("CDS");

    e.setPhase(-2);
    assertEquals(-1, e.getPhase());

    e.setPhase(-1);
    assertEquals(-1, e.getPhase());

    e.setPhase(0);
    assertEquals(0, e.getPhase());

    e.setPhase(1);
    assertEquals(1, e.getPhase());

    e.setPhase(2);
    assertEquals(2, e.getPhase());

    e.setPhase(3);
    assertEquals(-1, e.getPhase());
  }

  @Test
  public void testGetMetadataKeyNames() {

    GFFEntry e = new GFFEntry();
    try {
      e.parse("ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      assertTrue(false);
    }

    assertEquals(0, e.getMetadataKeyNames().size());

    e.addMetaDataEntry("key1", "val1");
    assertEquals(1, e.getMetadataKeyNames().size());

    assertFalse(e.getMetadataKeyNames().contains("key0"));
    assertTrue(e.getMetadataKeyNames().contains("key1"));

    e.addMetaDataEntry("key2", "val2");
    assertEquals(2, e.getMetadataKeyNames().size());

    assertFalse(e.getMetadataKeyNames().contains("key0"));
    assertTrue(e.getMetadataKeyNames().contains("key1"));
    assertTrue(e.getMetadataKeyNames().contains("key2"));
  }

  @Test
  public void testGetAttributesNames() {

    GFFEntry e = new GFFEntry();

    assertEquals(0, e.getAttributesNames().size());

    try {
      e.parse("ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      assertTrue(false);
    }

    assertEquals(2, e.getAttributesNames().size());

    assertFalse(e.getAttributesNames().contains("key0"));
    assertTrue(e.getAttributesNames().contains("ID"));
    assertTrue(e.getAttributesNames().contains("Name"));
  }

  @Test
  public void testIsMetaDataEntry() {

    GFFEntry e = new GFFEntry();
    try {
      e.parse("ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      assertTrue(false);
    }

    assertFalse(e.isMetaDataEntry("key0"));
    assertEquals(0, e.getMetadataKeyNames().size());

    e.addMetaDataEntry("key1", "val1");
    assertFalse(e.isMetaDataEntry("key0"));
    assertTrue(e.isMetaDataEntry("key1"));

    e.addMetaDataEntry("key2", "val2");
    assertFalse(e.isMetaDataEntry("key0"));
    assertTrue(e.isMetaDataEntry("key1"));
    assertTrue(e.isMetaDataEntry("key2"));
  }

  @Test
  public void testIsAttribute() {

    GFFEntry e = new GFFEntry();

    assertFalse(e.isAttribute("key0"));
    assertFalse(e.isAttribute("ID"));
    assertFalse(e.isAttribute("Name"));

    try {
      e.parse("ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      assertTrue(false);
    }

    assertEquals(2, e.getAttributesNames().size());

    assertFalse(e.isAttribute("key0"));
    assertTrue(e.isAttribute("ID"));
    assertTrue(e.isAttribute("Name"));
    assertFalse(e.isAttribute("id"));
    assertFalse(e.isAttribute("name"));
  }

  @Test
  public void testGetMetadataEntryValues() {

    GFFEntry e = new GFFEntry();
    try {
      e.parse("ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      assertTrue(false);
    }

    e.addMetaDataEntry("key1", "val1");
    assertEquals(1, e.getMetadataEntryValues("key1").size());
    assertEquals("val1", e.getMetadataEntryValues("key1").get(0));

    e.addMetaDataEntry("key2", "val2");
    assertEquals(1, e.getMetadataEntryValues("key1").size());
    assertEquals("val1", e.getMetadataEntryValues("key1").get(0));
    assertEquals(1, e.getMetadataEntryValues("key2").size());
    assertEquals("val2", e.getMetadataEntryValues("key2").get(0));
  }

  @Test
  public void testGetAttributeValue() {

    GFFEntry e = new GFFEntry();

    try {
      e.parse("ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      assertTrue(false);
    }

    assertEquals("gene00001", e.getAttributeValue("ID"));
    assertEquals("EDEN", e.getAttributeValue("Name"));

  }

  @Test
  public void testSetId() {

    GFFEntry e = new GFFEntry();
    assertEquals(0, e.getId());
    e.setId(8888);
    assertEquals(8888, e.getId());
  }

  @Test
  public void testSetSeqId() {

    GFFEntry e = new GFFEntry();
    assertEquals("", e.getSeqId());
    e.setSeqId("myseqid");
    assertEquals("myseqid", e.getSeqId());
    e.setSeqId(" ");
    assertEquals("", e.getSeqId());
    e.setSeqId(null);
    assertEquals("", e.getSeqId());
  }

  @Test
  public void testSetSource() {

    GFFEntry e = new GFFEntry();
    assertEquals("", e.getSource());
    e.setSource("mysource");
    assertEquals("mysource", e.getSource());
    e.setSource(" ");
    assertEquals("", e.getSource());
    e.setSource(null);
    assertEquals("", e.getSource());
  }

  @Test
  public void testSetType() {

    GFFEntry e = new GFFEntry();
    assertEquals("", e.getType());
    e.setType("mytype");
    assertEquals("mytype", e.getType());
    e.setType(" ");
    assertEquals("", e.getType());
    e.setType(null);
    assertEquals("", e.getType());
  }

  @Test
  public void testSetStart() {
    GFFEntry e = new GFFEntry();
    assertEquals(-1, e.getStart());
    e.setStart(0);
    assertEquals(-1, e.getStart());
    e.setStart(1);
    assertEquals(1, e.getStart());
    e.setStart(10);
    assertEquals(10, e.getStart());
  }

  @Test
  public void testSetEnd() {

    GFFEntry e = new GFFEntry();
    assertEquals(-1, e.getEnd());
    e.setEnd(0);
    assertEquals(-1, e.getEnd());
    e.setEnd(1);
    assertEquals(1, e.getEnd());
    e.setEnd(10);
    assertEquals(10, e.getEnd());
  }

  @Test
  public void testSetScore() {

    GFFEntry e = new GFFEntry();
    assertEquals(Double.NaN, e.getScore(), 0.0);
    e.setScore(0.0);
    assertEquals(0.0, e.getScore(), 0.0);
    e.setScore(1);
    assertEquals(1.0, e.getScore(), 0.0);
    e.setScore(10);
    assertEquals(10.0, e.getScore(), 0.0);
  }

  @Test
  public void testSetStrand() {

    GFFEntry e = new GFFEntry();
    assertEquals('.', e.getStrand());
    e.setStrand('+');
    assertEquals('+', e.getStrand());
    e.setStrand('.');
    assertEquals('.', e.getStrand());
    e.setStrand('-');
    assertEquals('-', e.getStrand());
    e.setStrand('a');
    assertEquals('.', e.getStrand());
  }

  @Test
  public void testSetPhase() {

    GFFEntry e = new GFFEntry();
    assertEquals(-1, e.getPhase());
    e.setPhase(-2);
    assertEquals(-1, e.getPhase());
    e.setPhase(-1);
    assertEquals(-1, e.getPhase());
    e.setPhase(0);
    assertEquals(0, e.getPhase());
    e.setPhase(1);
    assertEquals(1, e.getPhase());
    e.setPhase(2);
    assertEquals(2, e.getPhase());
    e.setPhase(3);
    assertEquals(-1, e.getPhase());
  }

}
