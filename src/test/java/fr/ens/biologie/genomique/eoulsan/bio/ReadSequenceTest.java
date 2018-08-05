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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Modifier;

import org.junit.Test;

public class ReadSequenceTest {

  @Test
  public void testSuper() {

    assertTrue(new ReadSequence() instanceof Sequence);

    try {

      Sequence sequence = new ReadSequence();
      assertTrue(true);
    } catch (ClassCastException e) {
      assertTrue(false);
    }

    assertTrue(Modifier.isFinal(ReadSequence.class.getModifiers()));
  }

  @Test
  public void testHashCode() {

    ReadSequence s1 = new ReadSequence("read1", "ATGC", "!!!!");
    ReadSequence s2 = new ReadSequence("read1", "ATGC", "!!!!");
    ReadSequence s3 = new ReadSequence("read1", "ATGC", "!!!#");

    assertEquals(s1.hashCode(), s2.hashCode());
    assertNotSame(s1.hashCode(), s3.hashCode());

    s3.setQuality("!!!!");
    assertEquals(s1.hashCode(), s3.hashCode());

    s3.setFastqFormat(FastqFormat.FASTQ_ILLUMINA);
    assertNotSame(s1.hashCode(), s3.hashCode());
  }

  @Test
  public void testEqualsObject() {

    ReadSequence s1 = new ReadSequence("read1", "ATGC", "!!!!");
    ReadSequence s2 = new ReadSequence("read1", "ATGC", "!!!!");
    ReadSequence s3 = new ReadSequence("read1", "ATGC", "!!!#");

    assertTrue(s1.equals(s2));
    assertFalse(s1.equals(s3));

    s3.setQuality("!!!!");
    assertTrue(s1.equals(s3));

    s3.setFastqFormat(FastqFormat.FASTQ_ILLUMINA);
    assertFalse(s1.equals(s3));

    s3.setFastqFormat(FastqFormat.FASTQ_SANGER);
    assertTrue(s1.equals(s3));

    s3.setSequence("TTTT");
    assertFalse(s1.equals(s3));

    assertFalse(s1.equals(null));
    assertFalse(s1.getSequence().equals(""));
    assertTrue(s1.equals(s1));
  }

  @Test
  public void testValidate() {

    ReadSequence s = new ReadSequence("read1", "ATGC", "!!!!");
    assertTrue(s.validate());
    s.setFastqFormat(FastqFormat.FASTQ_ILLUMINA);
    assertFalse(s.validate());
    s.setFastqFormat(FastqFormat.FASTQ_SANGER);
    assertTrue(s.validate());
    s.setQuality(null);
    assertFalse(s.validate());
    s.setQuality("!!!");
    assertFalse(s.validate());
    s.setQuality("");
    assertFalse(s.validate());
  }

  @Test
  public void testToString() {

    ReadSequence s = new ReadSequence("read1", "ATGC", "!!!!");
    assertEquals(
        "ReadSequence{name=read1, description=null, alphabet=ReadDNA, sequence=ATGC,"
            + " fastqFormat=fastq-sanger, quality=!!!!}",
        s.toString());

  }

  @Test
  public void testSetGetFastqFormat() {

    ReadSequence s = new ReadSequence("read1", "ATGC", "!!!!");

    assertEquals(FastqFormat.FASTQ_SANGER, s.getFastqFormat());

    try {
      s.setFastqFormat(null);
      assertTrue(false);
    } catch (NullPointerException e) {
      assertTrue(true);
    }

    assertEquals(FastqFormat.FASTQ_SANGER, s.getFastqFormat());
    s.setFastqFormat(FastqFormat.FASTQ_ILLUMINA);
    assertEquals(FastqFormat.FASTQ_ILLUMINA, s.getFastqFormat());
  }

  @Test
  public void testSetReadSequence() {

    ReadSequence s1 = new ReadSequence("read1", "ATGC", "!!!!");
    ReadSequence s2 = new ReadSequence("read2", "ATGCATGC", "@@@@@@@@");
    s2.setFastqFormat(FastqFormat.FASTQ_ILLUMINA);

    assertEquals("read1", s1.getName());
    assertEquals("ATGC", s1.getSequence());
    assertEquals(Alphabets.READ_DNA_ALPHABET, s1.getAlphabet());
    assertEquals(FastqFormat.FASTQ_SANGER, s1.getFastqFormat());
    assertEquals("!!!!", s1.getQuality());

    s1.set(null);

    assertEquals("read1", s1.getName());
    assertEquals("ATGC", s1.getSequence());
    assertEquals(Alphabets.READ_DNA_ALPHABET, s1.getAlphabet());
    assertEquals(FastqFormat.FASTQ_SANGER, s1.getFastqFormat());
    assertEquals("!!!!", s1.getQuality());

    s1.set(s2);

    assertEquals("read2", s1.getName());
    assertEquals("ATGCATGC", s1.getSequence());
    assertEquals(Alphabets.READ_DNA_ALPHABET, s1.getAlphabet());
    assertEquals(FastqFormat.FASTQ_ILLUMINA, s1.getFastqFormat());
    assertEquals("@@@@@@@@", s1.getQuality());

    assertTrue(s1.equals(s2));
    assertFalse(s1 == s2);
  }

  @Test
  public void testQualityScores() {

    assertNull(new ReadSequence().qualityScores());

    ReadSequence s = new ReadSequence("read1", "ATGC", "!!!!");
    assertArrayEquals(new int[] {0, 0, 0, 0}, s.qualityScores());

  }

  @Test
  public void testErrorProbabilities() {

    assertNull(new ReadSequence().errorProbabilities());

    ReadSequence s = new ReadSequence("read1", "ATGC", "!!!!");
    assertEquals(1, s.errorProbabilities()[0], 0.1);

    s.setQuality("++++");
    assertEquals(0.01, s.errorProbabilities()[0], 0.1);

  }

  @Test
  public void testSubSequenceIntInt() {

    ReadSequence s1 = new ReadSequence("toto", "ATGC", "!#!#");

    try {
      s1.subSequence(-1, 2);
      assertTrue(false);
    } catch (StringIndexOutOfBoundsException e) {
      assertTrue(true);
    }

    try {
      s1.subSequence(0, 5);
      assertTrue(false);
    } catch (StringIndexOutOfBoundsException e) {
      assertTrue(true);
    }

    try {
      s1.subSequence(2, 1);
      assertTrue(false);
    } catch (StringIndexOutOfBoundsException e) {
      assertTrue(true);
    }

    ReadSequence s2 = s1.subSequence(0, 4);
    assertEquals("ATGC", s2.getSequence());

    s2 = s1.subSequence(1, 4);
    assertEquals("TGC", s2.getSequence());
    assertEquals("#!#", s2.getQuality());
    assertEquals("toto[part]", s2.getName());
    assertEquals(FastqFormat.FASTQ_SANGER, s2.getFastqFormat());

    s1.setName(null);
    s2 = s1.subSequence(1, 4);
    assertNull(s2.getName());

    s1.setSequence(null);
    assertNull(s1.subSequence(1, 4));

    s1.setName("titi");
    s1.setSequence("ATGC");
    s1.setFastqFormat(FastqFormat.FASTQ_ILLUMINA);
    s2 = s1.subSequence(1, 4);
    assertEquals(FastqFormat.FASTQ_ILLUMINA, s2.getFastqFormat());

    s1.setQuality(null);
    assertNull(s1.subSequence(1, 4));

    s1.setQuality("!!!!");
    assertNotNull(s1.subSequence(1, 4));
    s1.setQuality("!!!");
    assertNull(s1.subSequence(1, 4));
  }

  @Test
  public void testConcatReadSequence() {

    ReadSequence s1 = new ReadSequence("toto", "AATT", "!!!!");
    ReadSequence s2 = new ReadSequence("titi", "GGCC", "####");

    s1.setFastqFormat(FastqFormat.FASTQ_ILLUMINA);
    s2.setFastqFormat(FastqFormat.FASTQ_SOLEXA);

    ReadSequence s3 = s1.concat(s2);
    assertEquals("AATTGGCC", s3.getSequence());
    assertEquals("toto[merged]", s3.getName());
    assertEquals("!!!!####", s3.getQuality());
    assertEquals(FastqFormat.FASTQ_ILLUMINA, s3.getFastqFormat());

    s1.setSequence(null);
    s1.setQuality(null);
    s3 = s1.concat(s2);
    assertEquals("GGCC", s3.getSequence());
    assertEquals("####", s3.getQuality());

    s1.setSequence("AATT");
    s1.setQuality("!!!!");
    s2.setSequence(null);
    s2.setQuality(null);
    s3 = s1.concat(s2);
    assertEquals("AATT", s3.getSequence());
    assertEquals("!!!!", s3.getQuality());

    s3 = s1.concat(null);
    assertEquals(s1.getName() + "[merged]", s3.getName());
    assertEquals(s1.getAlphabet(), s3.getAlphabet());
    assertEquals(s1.getSequence(), s3.getSequence());
    assertEquals(s1.getFastqFormat(), s3.getFastqFormat());
    assertEquals(s1.getQuality(), s3.getQuality());
    assertFalse(s1 == s3);

  }

  @Test
  public void testToFastQ() {

    ReadSequence s = new ReadSequence("read1", "ATGC", "!!!!");
    assertEquals("@read1\nATGC\n+\n!!!!", s.toFastQ());

  }

  @Test
  public void testToFastQBoolean() {

    ReadSequence s = new ReadSequence("read1", "ATGC", "!!!!");
    assertEquals("@read1\nATGC\n+\n!!!!", s.toFastQ(false));
    assertEquals("@read1\nATGC\n+read1\n!!!!", s.toFastQ(true));
  }

  @Test
  public void testToFastQStringStringString() {

    assertEquals("@read1\nATGC\n+\n!!!!",
        ReadSequence.toFastQ("read1", "ATGC", "!!!!"));
    assertNull(ReadSequence.toFastQ(null, null, null));
    assertNull(ReadSequence.toFastQ("read1", "ATGC", null));
    assertNull(ReadSequence.toFastQ("read1", null, "!!!!"));
    assertNull(ReadSequence.toFastQ(null, "ATGC", "!!!!"));
  }

  @Test
  public void testToFastQStringStringStringBoolean() {

    assertEquals("@read1\nATGC\n+\n!!!!",
        ReadSequence.toFastQ("read1", "ATGC", "!!!!", false));
    assertEquals("@read1\nATGC\n+read1\n!!!!",
        ReadSequence.toFastQ("read1", "ATGC", "!!!!", true));

    assertNull(ReadSequence.toFastQ(null, null, null, false));
    assertNull(ReadSequence.toFastQ("read1", "ATGC", null, false));
    assertNull(ReadSequence.toFastQ("read1", null, "!!!!", false));
    assertNull(ReadSequence.toFastQ(null, "ATGC", "!!!!", false));

    assertNull(ReadSequence.toFastQ(null, null, null, true));
    assertNull(ReadSequence.toFastQ("read1", "ATGC", null, true));
    assertNull(ReadSequence.toFastQ("read1", null, "!!!!", true));
    assertNull(ReadSequence.toFastQ(null, "ATGC", "!!!!", true));
  }

  @Test
  public void testToTFQ() {

    ReadSequence s = new ReadSequence("read1", "ATGC", "!!!!");
    assertEquals("read1\tATGC\t!!!!", s.toTFQ());
  }

  @Test
  public void testToTFQBoolean() {

    ReadSequence s = new ReadSequence("read1", "ATGC", "!!!!");
    assertEquals("read1\tATGC\t!!!!", s.toTFQ(true));
    assertEquals("\tATGC\t!!!!", s.toTFQ(false));
  }

  @Test
  public void testToTFQStringStringString() {

    assertEquals("read1\tATGC\t!!!!",
        ReadSequence.toTFQ("read1", "ATGC", "!!!!"));

    assertNull(ReadSequence.toTFQ(null, null, null));
    assertNull(ReadSequence.toTFQ(null, "ATGC", "!!!!"));
    assertNull(ReadSequence.toTFQ("read1", null, "!!!!"));
    assertNull(ReadSequence.toTFQ("read1", "ATGC", null));
  }

  @Test
  public void testToTFQBooleanStringStringString() {

    assertEquals("read1\tATGC\t!!!!",
        ReadSequence.toTFQ(true, "read1", "ATGC", "!!!!"));

    assertNull(ReadSequence.toTFQ(true, null, null, null));
    assertNull(ReadSequence.toTFQ(true, null, "ATGC", "!!!!"));
    assertNull(ReadSequence.toTFQ(true, "read1", null, "!!!!"));
    assertNull(ReadSequence.toTFQ(true, "read1", "ATGC", null));

    assertEquals("\tATGC\t!!!!",
        ReadSequence.toTFQ(false, "read1", "ATGC", "!!!!"));

    assertNull(ReadSequence.toTFQ(false, null, null, null));
    assertNull(ReadSequence.toTFQ(false, null, "ATGC", "!!!!"));
    assertNull(ReadSequence.toTFQ(false, "read1", null, "!!!!"));
    assertNull(ReadSequence.toTFQ(false, "read1", "ATGC", null));
  }

  @Test
  public void testToOutKey() {

    ReadSequence s = new ReadSequence("read1", "ATGC", "!!!!");
    assertEquals("read1", s.toOutKey());
    s.setName(null);
    assertNull(s.toOutKey());
  }

  @Test
  public void testToOutValue() {

    ReadSequence s = new ReadSequence("read1", "ATGC", "!!!!");
    assertEquals("ATGC\t!!!!", s.toOutValue());
    s.setSequence(null);
    assertEquals("null\t!!!!", s.toOutValue());
    s.setQuality(null);
    assertEquals("null\tnull", s.toOutValue());
  }

  @Test
  public void testParseFastQ() {

    ReadSequence s = new ReadSequence();

    s.parseFastQ("@read1\nATGC\n+\n!!!!");
    assertEquals("read1", s.getName());
    assertEquals("ATGC", s.getSequence());
    assertEquals("!!!!", s.getQuality());

    s.parseFastQ("@read1\nATGC\n+\n!!!!\n");
    assertEquals("read1", s.getName());
    assertEquals("ATGC", s.getSequence());
    assertEquals("!!!!", s.getQuality());

    s.parseFastQ(null);
    s.parseFastQ("@read1\nATGC\n+\n!!!!\n");
    assertEquals("read1", s.getName());
    assertEquals("ATGC", s.getSequence());
    assertEquals("!!!!", s.getQuality());

  }

  @Test
  public void testParse() {

    ReadSequence s = new ReadSequence();

    s.parse("read1\tATGC\t!!!!");
    assertEquals("read1", s.getName());
    assertEquals("ATGC", s.getSequence());
    assertEquals("!!!!", s.getQuality());

    s.parse(null);
    assertEquals("read1", s.getName());
    assertEquals("ATGC", s.getSequence());
    assertEquals("!!!!", s.getQuality());

  }

  @Test
  public void testParseKeyValue() {

    ReadSequence s = new ReadSequence();

    s.parseKeyValue("read1", "ATGC\t!!!!");
    assertEquals("read1", s.getName());
    assertEquals("ATGC", s.getSequence());
    assertEquals("!!!!", s.getQuality());

    s.parseKeyValue(null, "ATGC\t!!!!");
    assertEquals("read1", s.getName());
    assertEquals("ATGC", s.getSequence());
    assertEquals("!!!!", s.getQuality());

    s.parseKeyValue("read1", null);
    assertEquals("read1", s.getName());
    assertEquals("ATGC", s.getSequence());
    assertEquals("!!!!", s.getQuality());

    s.parseKeyValue(null, null);
    assertEquals("read1", s.getName());
    assertEquals("ATGC", s.getSequence());
    assertEquals("!!!!", s.getQuality());
  }

  @Test
  public void testValidateQuality() {

    ReadSequence s = new ReadSequence("read1", "ATGC", "!!!!");
    assertTrue(s.validate());

    s.setQuality("! !!");
    assertFalse(s.validate());
    s.setQuality("!!!!");
    assertTrue(s.validate());

    s.setQuality("!!!!!");
    assertFalse(s.validate());
    s.setQuality("!!!!");
    assertTrue(s.validate());

    s.setQuality("");
    assertFalse(s.validate());
    s.setQuality("!!!!");
    assertTrue(s.validate());

    s.setQuality(null);
    assertFalse(s.validate());
    s.setQuality("!!!!");
    assertTrue(s.validate());

  }

}
