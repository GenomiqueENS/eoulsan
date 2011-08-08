package fr.ens.transcriptome.eoulsan.bio;

import static fr.ens.transcriptome.eoulsan.bio.FastqFormat.FASTQ_ILLUMINA;
import static fr.ens.transcriptome.eoulsan.bio.FastqFormat.FASTQ_SANGER;
import static fr.ens.transcriptome.eoulsan.bio.FastqFormat.FASTQ_SOLEXA;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FastqFormatTest {

  @Test
  public void testGetName() {

    assertEquals("fastq-sanger", FASTQ_SANGER.getName());
    assertEquals("fastq-solexa", FASTQ_SOLEXA.getName());
    assertEquals("fastq-illumina", FASTQ_ILLUMINA.getName());
  }

  @Test
  public void testGetQualityMin() {

    assertEquals(0, FASTQ_SANGER.getQualityMin());
    assertEquals(-5, FASTQ_SOLEXA.getQualityMin());
    assertEquals(0, FASTQ_ILLUMINA.getQualityMin());
  }

  @Test
  public void testGetQualityMax() {

    assertEquals(93, FASTQ_SANGER.getQualityMax());
    assertEquals(62, FASTQ_SOLEXA.getQualityMax());
    assertEquals(62, FASTQ_ILLUMINA.getQualityMax());
  }

  @Test
  public void testGetCharMin() {

    assertEquals('!', FASTQ_SANGER.getCharMin());
    assertEquals(';', FASTQ_SOLEXA.getCharMin());
    assertEquals('@', FASTQ_ILLUMINA.getCharMin());
  }

  @Test
  public void testGetCharMax() {

    assertEquals('~', FASTQ_SANGER.getCharMax());
    assertEquals('~', FASTQ_SOLEXA.getCharMax());
    assertEquals('~', FASTQ_ILLUMINA.getCharMax());
  }

  @Test
  public void testGetOffset() {

    assertEquals(33, FASTQ_SANGER.getAsciiOffset());
    assertEquals(64, FASTQ_SOLEXA.getAsciiOffset());
    assertEquals(64, FASTQ_ILLUMINA.getAsciiOffset());
  }

  @Test
  public void testGetQuality() {
    assertEquals(0, FASTQ_SANGER.getQuality('!'));
    assertEquals(0, FASTQ_SOLEXA.getQuality('@'));
    assertEquals(0, FASTQ_ILLUMINA.getQuality('@'));
  }

  //@Test
  // public void testConvertQualityTo() {
  //
  // assertEquals(0, FASTQ_SANGER.convertQualityTo(0, FASTQ_SANGER));
  // assertEquals(1, FASTQ_SANGER.convertQualityTo(1, FASTQ_SANGER));
  // assertEquals(2, FASTQ_SANGER.convertQualityTo(2, FASTQ_SANGER));
  // assertEquals(3, FASTQ_SANGER.convertQualityTo(3, FASTQ_SANGER));
  // assertEquals(4, FASTQ_SANGER.convertQualityTo(4, FASTQ_SANGER));
  // assertEquals(5, FASTQ_SANGER.convertQualityTo(5, FASTQ_SANGER));
  // assertEquals(6, FASTQ_SANGER.convertQualityTo(6, FASTQ_SANGER));
  // assertEquals(7, FASTQ_SANGER.convertQualityTo(7, FASTQ_SANGER));
  // assertEquals(8, FASTQ_SANGER.convertQualityTo(8, FASTQ_SANGER));
  // assertEquals(9, FASTQ_SANGER.convertQualityTo(9, FASTQ_SANGER));
  // assertEquals(10, FASTQ_SANGER.convertQualityTo(10, FASTQ_SANGER));
  //
  // assertEquals(0, FASTQ_SOLEXA.convertQualityTo(0, FASTQ_SOLEXA));
  // assertEquals(1, FASTQ_SOLEXA.convertQualityTo(1, FASTQ_SOLEXA));
  // assertEquals(2, FASTQ_SOLEXA.convertQualityTo(2, FASTQ_SOLEXA));
  // assertEquals(3, FASTQ_SOLEXA.convertQualityTo(3, FASTQ_SOLEXA));
  // assertEquals(4, FASTQ_SOLEXA.convertQualityTo(4, FASTQ_SOLEXA));
  // assertEquals(5, FASTQ_SOLEXA.convertQualityTo(5, FASTQ_SOLEXA));
  // assertEquals(6, FASTQ_SOLEXA.convertQualityTo(6, FASTQ_SOLEXA));
  // assertEquals(7, FASTQ_SOLEXA.convertQualityTo(7, FASTQ_SOLEXA));
  // assertEquals(8, FASTQ_SOLEXA.convertQualityTo(8, FASTQ_SOLEXA));
  // assertEquals(9, FASTQ_SOLEXA.convertQualityTo(9, FASTQ_SOLEXA));
  // assertEquals(10, FASTQ_SOLEXA.convertQualityTo(10, FASTQ_SOLEXA));
  //
  // assertEquals(0, FASTQ_ILLUMINA.convertQualityTo(0, FASTQ_ILLUMINA));
  // assertEquals(1, FASTQ_ILLUMINA.convertQualityTo(1, FASTQ_ILLUMINA));
  // assertEquals(2, FASTQ_ILLUMINA.convertQualityTo(2, FASTQ_ILLUMINA));
  // assertEquals(3, FASTQ_ILLUMINA.convertQualityTo(3, FASTQ_ILLUMINA));
  // assertEquals(4, FASTQ_ILLUMINA.convertQualityTo(4, FASTQ_ILLUMINA));
  // assertEquals(5, FASTQ_ILLUMINA.convertQualityTo(5, FASTQ_ILLUMINA));
  // assertEquals(6, FASTQ_ILLUMINA.convertQualityTo(6, FASTQ_ILLUMINA));
  // assertEquals(7, FASTQ_ILLUMINA.convertQualityTo(7, FASTQ_ILLUMINA));
  // assertEquals(8, FASTQ_ILLUMINA.convertQualityTo(8, FASTQ_ILLUMINA));
  // assertEquals(9, FASTQ_ILLUMINA.convertQualityTo(9, FASTQ_ILLUMINA));
  // assertEquals(10, FASTQ_ILLUMINA.convertQualityTo(10, FASTQ_ILLUMINA));
  //
  // assertEquals(0, FASTQ_ILLUMINA.convertQualityTo(0, FASTQ_SANGER));
  // assertEquals(1, FASTQ_ILLUMINA.convertQualityTo(1, FASTQ_SANGER));
  // assertEquals(2, FASTQ_ILLUMINA.convertQualityTo(2, FASTQ_SANGER));
  // assertEquals(3, FASTQ_ILLUMINA.convertQualityTo(3, FASTQ_SANGER));
  // assertEquals(4, FASTQ_ILLUMINA.convertQualityTo(4, FASTQ_SANGER));
  // assertEquals(5, FASTQ_ILLUMINA.convertQualityTo(5, FASTQ_SANGER));
  // assertEquals(6, FASTQ_ILLUMINA.convertQualityTo(6, FASTQ_SANGER));
  // assertEquals(7, FASTQ_ILLUMINA.convertQualityTo(7, FASTQ_SANGER));
  // assertEquals(8, FASTQ_ILLUMINA.convertQualityTo(8, FASTQ_SANGER));
  // assertEquals(9, FASTQ_ILLUMINA.convertQualityTo(9, FASTQ_SANGER));
  // assertEquals(10, FASTQ_ILLUMINA.convertQualityTo(10, FASTQ_SANGER));
  //
  // assertEquals(0, FASTQ_SANGER.convertQualityTo(0, FASTQ_ILLUMINA));
  // assertEquals(1, FASTQ_SANGER.convertQualityTo(1, FASTQ_ILLUMINA));
  // assertEquals(2, FASTQ_SANGER.convertQualityTo(2, FASTQ_ILLUMINA));
  // assertEquals(3, FASTQ_SANGER.convertQualityTo(3, FASTQ_ILLUMINA));
  // assertEquals(4, FASTQ_SANGER.convertQualityTo(4, FASTQ_ILLUMINA));
  // assertEquals(5, FASTQ_SANGER.convertQualityTo(5, FASTQ_ILLUMINA));
  // assertEquals(6, FASTQ_SANGER.convertQualityTo(6, FASTQ_ILLUMINA));
  // assertEquals(7, FASTQ_SANGER.convertQualityTo(7, FASTQ_ILLUMINA));
  // assertEquals(8, FASTQ_SANGER.convertQualityTo(8, FASTQ_ILLUMINA));
  // assertEquals(9, FASTQ_SANGER.convertQualityTo(9, FASTQ_ILLUMINA));
  // assertEquals(10, FASTQ_SANGER.convertQualityTo(10, FASTQ_ILLUMINA));
  //
  // //assertEquals(-5, FASTQ_SANGER.convertQualityTo(0, FASTQ_SOLEXA));
  // assertEquals(-5, FASTQ_SANGER.convertQualityTo(1, FASTQ_SOLEXA));
  // assertEquals(-2, FASTQ_SANGER.convertQualityTo(2, FASTQ_SOLEXA));
  // assertEquals(0, FASTQ_SANGER.convertQualityTo(3, FASTQ_SOLEXA));
  // assertEquals(2, FASTQ_SANGER.convertQualityTo(4, FASTQ_SOLEXA));
  // assertEquals(3, FASTQ_SANGER.convertQualityTo(5, FASTQ_SOLEXA));
  // assertEquals(5, FASTQ_SANGER.convertQualityTo(6, FASTQ_SOLEXA));
  // assertEquals(6, FASTQ_SANGER.convertQualityTo(7, FASTQ_SOLEXA));
  // assertEquals(7, FASTQ_SANGER.convertQualityTo(8, FASTQ_SOLEXA));
  // assertEquals(8, FASTQ_SANGER.convertQualityTo(9, FASTQ_SOLEXA));
  // assertEquals(10, FASTQ_SANGER.convertQualityTo(10, FASTQ_SOLEXA));
  // }

}
