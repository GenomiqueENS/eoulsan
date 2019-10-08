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

package fr.ens.biologie.genomique.eoulsan.bio;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Test;

@SuppressWarnings("deprecation")
public class GFFEntryTest {

  private final String[] TEST_GFF3_STRINGS = {

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
      "Ctg123\t.\tCDS\t7000\t7600\t.\t+\t2\tID=cds00004;Parent=mRNA00003;Name=edenprotein.4",};

  private final String[] TEST_GTF_STRINGS = {
      "1\thavana\tgene\t11869\t14409\t.\t+\t.\tgene_id \"ENSG00000223972\"; gene_version \"5\"; gene_name \"DDX11L1\"; gene_source \"havana\"; gene_biotype \"transcribed_unprocessed_pseudogene\"; havana_gene \"OTTHUMG00000000961\"; havana_gene_version \"2\";",
      "1\thavana\ttranscript\t11869\t14409\t.\t+\t.\tgene_id \"ENSG00000223972\"; gene_version \"5\"; transcript_id \"ENST00000456328\"; transcript_version \"2\"; gene_name \"DDX11L1\"; gene_source \"havana\"; gene_biotype \"transcribed_unprocessed_pseudogene\"; havana_gene \"OTTHUMG00000000961\"; havana_gene_version \"2\"; transcript_name \"DDX11L1-002\"; transcript_source \"havana\"; transcript_biotype \"processed_transcript\"; havana_transcript \"OTTHUMT00000362751\"; havana_transcript_version \"1\"; tag \"basic\"; transcript_support_level \"1\";",
      "1\thavana\texon\t11869\t12227\t.\t+\t.\tgene_id \"ENSG00000223972\"; gene_version \"5\"; transcript_id \"ENST00000456328\"; transcript_version \"2\"; exon_number \"1\"; gene_name \"DDX11L1\"; gene_source \"havana\"; gene_biotype \"transcribed_unprocessed_pseudogene\"; havana_gene \"OTTHUMG00000000961\"; havana_gene_version \"2\"; transcript_name \"DDX11L1-002\"; transcript_source \"havana\"; transcript_biotype \"processed_transcript\"; havana_transcript \"OTTHUMT00000362751\"; havana_transcript_version \"1\"; exon_id \"ENSE00002234944\"; exon_version \"1\"; tag \"basic\"; transcript_support_level \"1\";",
      "1\thavana\texon\t12613\t12721\t.\t+\t.\tgene_id \"ENSG00000223972\"; gene_version \"5\"; transcript_id \"ENST00000456328\"; transcript_version \"2\"; exon_number \"2\"; gene_name \"DDX11L1\"; gene_source \"havana\"; gene_biotype \"transcribed_unprocessed_pseudogene\"; havana_gene \"OTTHUMG00000000961\"; havana_gene_version \"2\"; transcript_name \"DDX11L1-002\"; transcript_source \"havana\"; transcript_biotype \"processed_transcript\"; havana_transcript \"OTTHUMT00000362751\"; havana_transcript_version \"1\"; exon_id \"ENSE00003582793\"; exon_version \"1\"; tag \"basic\"; transcript_support_level \"1\";",
      "1\thavana\texon\t13221\t14409\t.\t+\t.\tgene_id \"ENSG00000223972\"; gene_version \"5\"; transcript_id \"ENST00000456328\"; transcript_version \"2\"; exon_number \"3\"; gene_name \"DDX11L1\"; gene_source \"havana\"; gene_biotype \"transcribed_unprocessed_pseudogene\"; havana_gene \"OTTHUMG00000000961\"; havana_gene_version \"2\"; transcript_name \"DDX11L1-002\"; transcript_source \"havana\"; transcript_biotype \"processed_transcript\"; havana_transcript \"OTTHUMT00000362751\"; havana_transcript_version \"1\"; exon_id \"ENSE00002312635\"; exon_version \"1\"; tag \"basic\"; transcript_support_level \"1\";",
      "1\thavana\ttranscript\t12010\t13670\t.\t+\t.\tgene_id \"ENSG00000223972\"; gene_version \"5\"; transcript_id \"ENST00000450305\"; transcript_version \"2\"; gene_name \"DDX11L1\"; gene_source \"havana\"; gene_biotype \"transcribed_unprocessed_pseudogene\"; havana_gene \"OTTHUMG00000000961\"; havana_gene_version \"2\"; transcript_name \"DDX11L1-001\"; transcript_source \"havana\"; transcript_biotype \"transcribed_unprocessed_pseudogene\"; havana_transcript \"OTTHUMT00000002844\"; havana_transcript_version \"2\"; tag \"basic\"; transcript_support_level \"NA\";",
      "1\thavana\texon\t12010\t12057\t.\t+\t.\tgene_id \"ENSG00000223972\"; gene_version \"5\"; transcript_id \"ENST00000450305\"; transcript_version \"2\"; exon_number \"1\"; gene_name \"DDX11L1\"; gene_source \"havana\"; gene_biotype \"transcribed_unprocessed_pseudogene\"; havana_gene \"OTTHUMG00000000961\"; havana_gene_version \"2\"; transcript_name \"DDX11L1-001\"; transcript_source \"havana\"; transcript_biotype \"transcribed_unprocessed_pseudogene\"; havana_transcript \"OTTHUMT00000002844\"; havana_transcript_version \"2\"; exon_id \"ENSE00001948541\"; exon_version \"1\"; tag \"basic\"; transcript_support_level \"NA\";",
      "1\thavana\texon\t12179\t12227\t.\t+\t.\tgene_id \"ENSG00000223972\"; gene_version \"5\"; transcript_id \"ENST00000450305\"; transcript_version \"2\"; exon_number \"2\"; gene_name \"DDX11L1\"; gene_source \"havana\"; gene_biotype \"transcribed_unprocessed_pseudogene\"; havana_gene \"OTTHUMG00000000961\"; havana_gene_version \"2\"; transcript_name \"DDX11L1-001\"; transcript_source \"havana\"; transcript_biotype \"transcribed_unprocessed_pseudogene\"; havana_transcript \"OTTHUMT00000002844\"; havana_transcript_version \"2\"; exon_id \"ENSE00001671638\"; exon_version \"2\"; tag \"basic\"; transcript_support_level \"NA\";",
      "1\thavana\texon\t12613\t12697\t.\t+\t.\tgene_id \"ENSG00000223972\"; gene_version \"5\"; transcript_id \"ENST00000450305\"; transcript_version \"2\"; exon_number \"3\"; gene_name \"DDX11L1\"; gene_source \"havana\"; gene_biotype \"transcribed_unprocessed_pseudogene\"; havana_gene \"OTTHUMG00000000961\"; havana_gene_version \"2\"; transcript_name \"DDX11L1-001\"; transcript_source \"havana\"; transcript_biotype \"transcribed_unprocessed_pseudogene\"; havana_transcript \"OTTHUMT00000002844\"; havana_transcript_version \"2\"; exon_id \"ENSE00001758273\"; exon_version \"2\"; tag \"basic\"; transcript_support_level \"NA\";",
      "1\thavana\texon\t12975\t13052\t.\t+\t.\tgene_id \"ENSG00000223972\"; gene_version \"5\"; transcript_id \"ENST00000450305\"; transcript_version \"2\"; exon_number \"4\"; gene_name \"DDX11L1\"; gene_source \"havana\"; gene_biotype \"transcribed_unprocessed_pseudogene\"; havana_gene \"OTTHUMG00000000961\"; havana_gene_version \"2\"; transcript_name \"DDX11L1-001\"; transcript_source \"havana\"; transcript_biotype \"transcribed_unprocessed_pseudogene\"; havana_transcript \"OTTHUMT00000002844\"; havana_transcript_version \"2\"; exon_id \"ENSE00001799933\"; exon_version \"2\"; tag \"basic\"; transcript_support_level \"NA\";",
      "1\thavana\texon\t13221\t13374\t.\t+\t.\tgene_id \"ENSG00000223972\"; gene_version \"5\"; transcript_id \"ENST00000450305\"; transcript_version \"2\"; exon_number \"5\"; gene_name \"DDX11L1\"; gene_source \"havana\"; gene_biotype \"transcribed_unprocessed_pseudogene\"; havana_gene \"OTTHUMG00000000961\"; havana_gene_version \"2\"; transcript_name \"DDX11L1-001\"; transcript_source \"havana\"; transcript_biotype \"transcribed_unprocessed_pseudogene\"; havana_transcript \"OTTHUMT00000002844\"; havana_transcript_version \"2\"; exon_id \"ENSE00001746346\"; exon_version \"2\"; tag \"basic\"; transcript_support_level \"NA\";",
      "1\thavana\texon\t13453\t13670\t.\t+\t.\tgene_id \"ENSG00000223972\"; gene_version \"5\"; transcript_id \"ENST00000450305\"; transcript_version \"2\"; exon_number \"6\"; gene_name \"DDX11L1\"; gene_source \"havana\"; gene_biotype \"transcribed_unprocessed_pseudogene\"; havana_gene \"OTTHUMG00000000961\"; havana_gene_version \"2\"; transcript_name \"DDX11L1-001\"; transcript_source \"havana\"; transcript_biotype \"transcribed_unprocessed_pseudogene\"; havana_transcript \"OTTHUMT00000002844\"; havana_transcript_version \"2\"; exon_id \"ENSE00001863096\"; exon_version \"1\"; tag \"basic\"; transcript_support_level \"NA\";"};

  @Test
  public void testParseGFF3() {

    GFFEntry e = new GFFEntry();

    try {
      e.parseGFF3(null);
    } catch (NullPointerException exp) {
      assertTrue(true);
    } catch (BadBioEntryException e1) {
      fail();
    }

    try {
      e.parseGFF3("");
    } catch (BadBioEntryException exp) {
      assertTrue(true);
    }

    for (String s : this.TEST_GFF3_STRINGS) {

      try {
        e.parseGFF3(s);
      } catch (BadBioEntryException exp) {
        fail();
      }

      assertEquals(s, e.toGFF3());
    }

    try {
      e.parseGFF3(
          "ctg123\tbidon\tgene\tBIDON\t9000\t111.11\t+\t2\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException e1) {
      fail();
    }

    try {
      e.parseGFF3(
          "ctg123\tbidon\tgene\tBIDON\t9000\t111.11\t+\t2\tID=gene00001; ; Parent ; Name=EDEN");
    } catch (BadBioEntryException e1) {
      fail();
    }

    try {
      e.parseGFF3("ctg123\tbidon\tgene\tBIDON\t9000\t111.11\t\t2\t");
    } catch (BadBioEntryException e1) {
      fail();
    }

    try {
      e.parseGFF3("ctg123\tbidon\tgene\tBIDON\t9000\t111.11\t+\t2\t.");
    } catch (BadBioEntryException e1) {
      fail();
    }

    try {
      e.parse(
          "ctg123\tbidon\tgene\tBIDON\t9000\t111.11\t+\t2\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException e1) {
      fail();
    }

  }

  @Test
  public void testParseGTF() {

    GFFEntry e = new GFFEntry();

    for (String s : this.TEST_GTF_STRINGS) {

      try {
        e.parseGTF(s);
      } catch (BadBioEntryException exp) {
        fail();
      }

      assertEquals(s, e.toGTF());
    }

    try {
      e.parseGTF(
          "IV\tcurated\tmRNA\t5506800\t5508917\t.\t+\t.\tTranscript B0273.1; Note \"Zn-Finger\"");

      assertEquals(2, e.getAttributesNames().size());
      assertEquals("B0273.1", e.getAttributeValue("Transcript"));
      assertEquals("Zn-Finger", e.getAttributeValue("Note"));

      e.parseGTF(
          "IV\tcurated\t5'UTR\t5506800\t5508999\t.\t+\t.\tTranscript B0273.1");

      assertEquals(1, e.getAttributesNames().size());
      assertEquals("B0273.1", e.getAttributeValue("Transcript"));

      e.parseGTF(
          "Chr3\tgiemsa\theterochromatin\t4500000\t6000000\t.\t.\t.\tBand 3q12.1 ; Note \"Marfan's syndrome\"");

      assertEquals(2, e.getAttributesNames().size());
      assertEquals("3q12.1", e.getAttributeValue("Band"));
      assertEquals("Marfan's syndrome", e.getAttributeValue("Note"));

      e.parseGTF(
          "Chr3\tgiemsa\theterochromatin\t4500000\t6000000\t.\t.\t.\tBand 3q12.1 ; Note \"Marfan's syndrome\" ; Note \"dystrophic dysplasia\"");

      assertEquals(2, e.getAttributesNames().size());
      assertEquals("3q12.1", e.getAttributeValue("Band"));
      assertEquals("Marfan's syndrome,dystrophic dysplasia",
          e.getAttributeValue("Note"));

      e.parseGTF(
          "Chr3\tgiemsa\theterochromatin\t4500000\t6000000\t.\t.\t.\tBand 3q12.1 ; Alias MFX");

      assertEquals(2, e.getAttributesNames().size());
      assertEquals("3q12.1", e.getAttributeValue("Band"));
      assertEquals("MFX", e.getAttributeValue("Alias"));

      e.parseGTF(
          "Chr1\tassembly\tchromosome\t1\t14972282\t.\t+\t.\tSequence Chr1");

      assertEquals(1, e.getAttributesNames().size());
      assertEquals("Chr1", e.getAttributeValue("Sequence"));

    } catch (BadBioEntryException exp) {
      exp.printStackTrace();
      fail();
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
  public void testGetSeqId() {

    GFFEntry e = new GFFEntry();
    try {
      e.parseGFF3(
          "ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      fail();
    }

    assertEquals("ctg123", e.getSeqId());
  }

  @Test
  public void testGetSource() {

    GFFEntry e = new GFFEntry();
    try {
      e.parseGFF3(
          "ctg123\tGenbank\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      fail();
    }

    assertEquals("Genbank", e.getSource());
  }

  @Test
  public void testGetType() {

    GFFEntry e = new GFFEntry();
    try {
      e.parseGFF3(
          "ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      fail();
    }

    assertEquals("gene", e.getType());
  }

  @Test
  public void testGetStart() {

    GFFEntry e = new GFFEntry();
    try {
      e.parseGFF3(
          "ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      fail();
    }

    assertEquals(1000, e.getStart());
  }

  @Test
  public void testGetEnd() {

    GFFEntry e = new GFFEntry();
    try {
      e.parseGFF3(
          "ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      fail();
    }

    assertEquals(9000, e.getEnd());
  }

  @Test
  public void testGetLength() {

    GFFEntry e = new GFFEntry();
    try {
      e.parseGFF3(
          "ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      fail();
    }

    assertEquals(8001, e.getLength());
  }

  @Test
  public void testGetScore() {

    GFFEntry e = new GFFEntry();
    try {
      e.parseGFF3(
          "ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      fail();
    }

    assertEquals(Double.NaN, e.getScore(), 0.0);
  }

  @Test
  public void testGetStrand() {

    GFFEntry e = new GFFEntry();
    try {
      e.parseGFF3(
          "ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      fail();
    }

    assertEquals('+', e.getStrand());
  }

  @Test
  public void testGetPhase() {

    GFFEntry e = new GFFEntry();
    try {
      e.parseGFF3(
          "ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      fail();
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

    assertEquals(Collections.emptySet(), e.getMetadataKeyNames());

    try {
      e.parseGFF3(
          "ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      fail();
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

    assertEquals(Collections.emptySet(), e.getAttributesNames());
    assertEquals(0, e.getAttributesNames().size());

    try {
      e.parseGFF3(
          "ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      fail();
    }

    assertEquals(2, e.getAttributesNames().size());

    assertFalse(e.getAttributesNames().contains("key0"));
    assertTrue(e.getAttributesNames().contains("ID"));
    assertTrue(e.getAttributesNames().contains("Name"));
  }

  @Test
  public void testIsMetaDataEntry() {

    GFFEntry e = new GFFEntry();

    assertFalse(e.isMetaDataEntry("toto"));

    try {
      e.parseGFF3(
          "ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      fail();
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

    assertFalse(e.isAttribute(null));
    assertFalse(e.isAttribute("key0"));
    assertFalse(e.isAttribute("ID"));
    assertFalse(e.isAttribute("Name"));

    try {
      e.parseGFF3(
          "ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      fail();
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

    assertEquals(Collections.emptyList(), e.getMetadataEntryValues(null));
    assertEquals(Collections.emptyList(), e.getMetadataEntryValues("toto"));

    try {
      e.parseGFF3(
          "ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      fail();
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
    assertNull(e.getAttributeValue(null));
    assertNull(e.getAttributeValue("toto"));

    try {
      e.parseGFF3(
          "ctg123\t.\tgene\t1000\t9000\t.\t+\t.\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException exp) {
      fail();
    }

    assertEquals("gene00001", e.getAttributeValue("ID"));
    assertEquals("EDEN", e.getAttributeValue("Name"));

  }

  @Test
  public void testSetSeqId() {

    GFFEntry e = new GFFEntry();
    assertEquals("", e.getSeqId());
    e.setSeqId("myseqid");
    assertEquals("myseqid", e.getSeqId());
    e.setSeqId(" ");
    assertEquals("", e.getSeqId());
    e.setSeqId("myseqid");
    assertEquals("myseqid", e.getSeqId());
    e.setSeqId(null);
    assertEquals("", e.getSeqId());
    e.setSeqId("myseqid");
    assertEquals("myseqid", e.getSeqId());
    e.setSeqId(".");
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
    e.setType("mytype");
    assertEquals("mytype", e.getType());
    e.setType(null);
    assertEquals("", e.getType());
    e.setType("mytype");
    assertEquals("mytype", e.getType());
    e.setType(".");
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

  @Test
  public void testAddMetaDataEntry() {

    GFFEntry e = new GFFEntry();
    assertEquals(Collections.emptySet(), e.getMetadataKeyNames());
    assertFalse(e.addMetaDataEntry("key", null));
    assertEquals(Collections.emptySet(), e.getMetadataKeyNames());
    assertFalse(e.addMetaDataEntry(null, "value"));
    assertEquals(Collections.emptySet(), e.getMetadataKeyNames());
    assertFalse(e.addMetaDataEntry(null, null));
    assertEquals(Collections.emptySet(), e.getMetadataKeyNames());

    assertTrue(e.addMetaDataEntry("key", "val1"));
    assertEquals(Collections.singleton("key"), e.getMetadataKeyNames());
    assertEquals(Collections.singletonList("val1"),
        e.getMetadataEntryValues("key"));
    assertTrue(e.addMetaDataEntry("key", "val2"));
    assertEquals(Collections.singleton("key"), e.getMetadataKeyNames());
    assertEquals(Arrays.asList("val1", "val2"),
        e.getMetadataEntryValues("key"));

    assertTrue(e.addMetaDataEntry("key2", "val3"));
    assertEquals(new HashSet<>(Arrays.asList("key", "key2")),
        e.getMetadataKeyNames());
    assertEquals(Collections.singletonList("val3"),
        e.getMetadataEntryValues("key2"));
  }

  @Test
  public void testAddMetaDataEntries() {

    GFFEntry e = new GFFEntry();

    assertFalse(e.addMetaDataEntries(null));
    Map<String, List<String>> entries = new HashMap<>();
    assertTrue(e.addMetaDataEntries(entries));
    entries.put("key0", null);
    assertFalse(e.addMetaDataEntries(entries));
    entries.clear();
    List<String> l = new ArrayList<>();
    l.add(null);
    entries.put("key00", l);
    assertFalse(e.addMetaDataEntries(entries));
    entries.clear();
    entries.put("key1", Collections.singletonList("val1"));
    assertTrue(e.addMetaDataEntries(entries));
    entries.clear();
    entries.put("key2", Arrays.asList("val2", "val3"));
    assertTrue(e.addMetaDataEntries(entries));
  }

  @Test
  public void testSetAttributeValue() {

    GFFEntry e = new GFFEntry();
    assertFalse(e.setAttributeValue(null, "value"));
    assertFalse(e.setAttributeValue("key", null));
    assertFalse(e.setAttributeValue(null, null));
    assertTrue(e.setAttributeValue("key", "value"));

    assertEquals("value", e.getAttributeValue("key"));
  }

  @Test
  public void testRemoveMetaDataEntry() {

    GFFEntry e = new GFFEntry();

    assertEquals(Collections.emptySet(), e.getMetadataKeyNames());
    e.addMetaDataEntry("key1", "value1");
    assertEquals(Collections.singleton("key1"), e.getMetadataKeyNames());
    e.addMetaDataEntry("key2", "value2");
    assertEquals(new HashSet<>(asList("key1", "key2")),
        e.getMetadataKeyNames());
    assertFalse(e.removeMetaDataEntry("key3"));
    assertEquals(new HashSet<>(asList("key1", "key2")),
        e.getMetadataKeyNames());
    assertFalse(e.removeMetaDataEntry(null));
    assertEquals(new HashSet<>(asList("key1", "key2")),
        e.getMetadataKeyNames());
    assertTrue(e.removeMetaDataEntry("key1"));
    assertEquals(Collections.singleton("key2"), e.getMetadataKeyNames());
  }

  @Test
  public void testRemoveAttributeEntry() {

    GFFEntry e = new GFFEntry();

    assertFalse(e.removeAttribute("toto"));

    assertEquals(Collections.emptySet(), e.getAttributesNames());

    e.setAttributeValue("key1", "value1");
    assertEquals(Collections.singleton("key1"), e.getAttributesNames());

    e.setAttributeValue("key2", "value2");
    assertEquals(new HashSet<>(asList("key1", "key2")), e.getAttributesNames());

    assertFalse(e.removeAttribute("key3"));
    assertEquals(new HashSet<>(asList("key1", "key2")), e.getAttributesNames());

    assertFalse(e.removeAttribute(null));
    assertEquals(new HashSet<>(asList("key1", "key2")), e.getAttributesNames());

    assertTrue(e.removeAttribute("key1"));
    assertEquals(Collections.singleton("key2"), e.getAttributesNames());
  }

  @Test
  public void testClear() {

    GFFEntry e = new GFFEntry();

    try {
      e.parseGFF3("ctg123\tbidon\tgene\t1000\t9000\t111.11\t+\t2\t");
    } catch (BadBioEntryException e1) {
      fail();
    }

    assertEquals("ctg123", e.getSeqId());
    assertEquals("bidon", e.getSource());
    assertEquals(1000, e.getStart());
    assertEquals(9000, e.getEnd());
    assertEquals(111.11, e.getScore(), 0.0);
    assertEquals('+', e.getStrand());
    assertEquals(2, e.getPhase());
    assertEquals(Collections.emptySet(), e.getAttributesNames());

    e.clear();

    assertEquals("", e.getSeqId());
    assertEquals("", e.getSource());
    assertEquals(-1, e.getStart());
    assertEquals(-1, e.getEnd());
    assertEquals(Double.NaN, e.getScore(), 0.0);
    assertEquals('.', e.getStrand());
    assertEquals(-1, e.getPhase());
    assertEquals(Collections.emptySet(), e.getAttributesNames());

    try {
      e.parseGFF3(
          "ctg123\tbidon\tgene\t1000\t9000\t111.11\t+\t2\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException e1) {
      fail();
    }

    assertEquals("ctg123", e.getSeqId());
    assertEquals("bidon", e.getSource());
    assertEquals(1000, e.getStart());
    assertEquals(9000, e.getEnd());
    assertEquals(111.11, e.getScore(), 0.0);
    assertEquals('+', e.getStrand());
    assertEquals(2, e.getPhase());
    assertEquals(new HashSet<>(asList("ID", "Name")), e.getAttributesNames());

    e.clear();

    assertEquals("", e.getSeqId());
    assertEquals("", e.getSource());
    assertEquals(-1, e.getStart());
    assertEquals(-1, e.getEnd());
    assertEquals(Double.NaN, e.getScore(), 0.0);
    assertEquals('.', e.getStrand());
    assertEquals(-1, e.getPhase());
    assertEquals(Collections.emptySet(), e.getAttributesNames());
  }

  @Test
  public void testClearMetaData() {

    GFFEntry e = new GFFEntry();

    assertEquals(Collections.emptySet(), e.getMetadataKeyNames());
    e.addMetaDataEntry("key1", "value1");
    assertEquals(Collections.singleton("key1"), e.getMetadataKeyNames());
    e.clearMetaData();
    assertEquals(Collections.emptySet(), e.getMetadataKeyNames());
  }

  @Test
  public void testIsValidStartAndEnd() {

    GFFEntry e = new GFFEntry();

    assertFalse(e.isValidStartAndEnd());
    e.setStart(1000);
    e.setEnd(2000);
    assertTrue(e.isValidStartAndEnd());

    e.setStart(Integer.MIN_VALUE);
    assertFalse(e.isValidStartAndEnd());
    e.setStart(1000);
    assertTrue(e.isValidStartAndEnd());

    e.setEnd(Integer.MAX_VALUE);
    assertTrue(e.isValidStartAndEnd());
    e.setEnd(2000);
    assertTrue(e.isValidStartAndEnd());

    e.setStart(Integer.MIN_VALUE);
    e.setEnd(Integer.MAX_VALUE);
    assertFalse(e.isValidStartAndEnd());
    e.setStart(1000);
    e.setEnd(2000);
    assertTrue(e.isValidStartAndEnd());

    e.setStart(3000);
    assertFalse(e.isValidStartAndEnd());
  }

  @Test
  public void testIsValidStrand() {

    GFFEntry e = new GFFEntry();
    assertTrue(e.isValidStrand());

  }

  @Test
  public void testIsValidPhase() {

    GFFEntry e = new GFFEntry();
    assertTrue(e.isValidPhase());

    e.setPhase(-1);
    assertTrue(e.isValidPhase());

    e.setPhase(0);
    assertFalse(e.isValidPhase());

    e.setPhase(1);
    assertFalse(e.isValidPhase());

    e.setPhase(2);
    assertFalse(e.isValidPhase());

    e.setType("CDS");

    e.setPhase(-1);
    assertFalse(e.isValidPhase());

    e.setPhase(0);
    assertTrue(e.isValidPhase());

    e.setPhase(1);
    assertTrue(e.isValidPhase());

    e.setPhase(2);
    assertTrue(e.isValidPhase());
  }

  @Test
  public void testToGFF3() {

    GFFEntry e = new GFFEntry();

    try {
      e.parseGFF3(
          "ctg123\tbidon\tgene\t1000\t9000\t111.11\t+\t2\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException e1) {
      fail();
    }

    assertEquals(
        "ctg123\tbidon\tgene\t1000\t9000\t111.11\t+\t2\tID=gene00001;Name=EDEN",
        e.toGFF3());

    try {
      e.parseGFF3(".\t.\t.\t.\t.\t.\t.\t.\t");
    } catch (BadBioEntryException e1) {
      fail();
    }

    assertEquals(".\t.\t.\t.\t.\t.\t.\t.\t.", e.toGFF3());

    assertEquals(".\t.\t.\t.\t.\t.\t.\t.\t.", e.toString());

  }

  @Test
  public void testToGTF() {

    GFFEntry e = new GFFEntry();

    try {
      e.parseGFF3(
          "ctg123\tbidon\tgene\t1000\t9000\t111.11\t+\t2\tID=gene00001;Name=EDEN");
    } catch (BadBioEntryException e1) {
      fail();
    }

    assertEquals(
        "ctg123\tbidon\tgene\t1000\t9000\t111.11\t+\t2\tID \"gene00001\"; Name \"EDEN\";",
        e.toGTF());

    try {
      e.parseGTF(
          "ctg123\tbidon\tgene\t1000\t9000\t111.11\t+\t2\tID \"gene00001\";Name \"EDEN\";");
    } catch (BadBioEntryException e1) {
      fail();
    }

    assertEquals(
        "ctg123\tbidon\tgene\t1000\t9000\t111.11\t+\t2\tID \"gene00001\"; Name \"EDEN\";",
        e.toGTF());

    try {
      e.parseGTF(
          "ctg123\tbidon\tgene\t1000\t9000\t111.11\t+\t2\tID \"gene00001\";; TOTO ;Name \"EDEN\";");
    } catch (BadBioEntryException e1) {
      fail();
    }

    assertEquals(
        "ctg123\tbidon\tgene\t1000\t9000\t111.11\t+\t2\tID \"gene00001\"; Name \"EDEN\";",
        e.toGTF());

    try {
      e.parseGTF(".\t.\t.\t.\t.\t.\t.\t.\t.");
    } catch (BadBioEntryException e1) {
      fail();
    }

    assertEquals(".\t.\t.\t.\t.\t.\t.\t.\t.", e.toGTF());
    try {
      e.parseGTF(".\t.\t.\t.\t.\t.\t.\t.\t");
    } catch (BadBioEntryException e1) {
      fail();
    }

    assertEquals(".\t.\t.\t.\t.\t.\t.\t.\t.", e.toGTF());

  }

  @Test
  public void testEqualsObject() {

    GFFEntry e1 = new GFFEntry();
    GFFEntry e2 = new GFFEntry();

    assertEquals(e1, e1);
    assertEquals(e1, e1);

    assertNotEquals(null, e1);
    assertNotEquals("toto", e1);
    assertEquals(e1, e2);
    assertEquals(e1, e2);

    e1.setSource("value");
    assertNotEquals(e1, e2);
    assertNotEquals(e1, e2);

    e2.setSource("value");
    assertEquals(e1, e2);
    assertEquals(e1, e2);
  }

  @Test
  public void testHashCode() {

    GFFEntry e1 = new GFFEntry();
    GFFEntry e2 = new GFFEntry();

    assertEquals(e1.hashCode(), e2.hashCode());

    e1.setSource("value");
    assertNotEquals(e1.hashCode(), e2.hashCode());
  }

  @Test
  public void testGetMetadata() {

    GFFEntry e1 = new GFFEntry();
    assertEquals(Collections.emptyMap(), e1.getMetadata().entries());

    e1.addMetaDataEntry("key1", "value1");
    assertEquals(
        Collections.singletonMap("key1", Collections.singletonList("value1")),
        e1.getMetadata().entries());

    e1.clearMetaData();
    assertEquals(Collections.emptyMap(), e1.getMetadata().entries());
    e1.getMetadata().add("key2", "value2");
    assertEquals(
        Collections.singletonMap("key2", Collections.singletonList("value2")),
        e1.getMetadata().entries());

    EntryMetadata m = new EntryMetadata();
    m.add("key3", "value3");

    GFFEntry e2 = new GFFEntry(m);

    assertEquals(
        Collections.singletonMap("key3", Collections.singletonList("value3")),
        e2.getMetadata().entries());
  }

}
