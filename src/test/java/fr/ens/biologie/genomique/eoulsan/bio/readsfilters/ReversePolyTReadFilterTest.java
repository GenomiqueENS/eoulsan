package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

public class ReversePolyTReadFilterTest {

  @Test
  public void testAcceptReadSequenceReadSequence() {

    ReadFilter filter = new ReversePolyTReadFilter();

    ReadSequence s1 = new ReadSequence(
        "96f7d3fa-263e-465d-b2e9-bbe2998426f4 runid=93f969536487e5b7e7f11d78f5b14fcf7708f8f5 sampleid=TOTO_A2018 read=10004 ch=355 start_time=2018-09-27T16:29:47Z tail_type=\"polyT\" tail_is_valid=\"TRUE\" tailStart=\"\" tailEnd=\"\" samples_per_nt=\"8.53\" tail_length=\"\"",
        "ATGC", "ABCD");
    assertEquals("ATGC", s1.getSequence());
    assertEquals("ABCD", s1.getQuality());
    assertTrue(filter.accept(s1));
    assertEquals("GCAT", s1.getSequence());
    assertEquals("DCBA", s1.getQuality());

    ReadSequence s2 = new ReadSequence(
        "d35c4c91-a387-4a88-b472-067d0caf0603 runid=93f969536487e5b7e7f11d78f5b14fcf7708f8f5 sampleid=TOTO_A2018 read=9046 ch=337 start_time=2018-09-27T16:29:42Z",
        "ATGC", "ABCD");
    assertEquals("ATGC", s2.getSequence());
    assertEquals("ABCD", s2.getQuality());
    assertTrue(filter.accept(s2));
    assertEquals("GCAT", s1.getSequence());
    assertEquals("DCBA", s1.getQuality());

    ReadSequence s3 = new ReadSequence(
        "94cfd9d4-b9c2-4892-96dd-c13277a0d180 runid=93f969536487e5b7e7f11d78f5b14fcf7708f8f5 sampleid=TOTO_A2018 read=10003 ch=396 start_time=2018-09-27T16:30:00Z tail_type=\"polyT\" tail_is_valid=\"TRUE\" tailStart=\"792\" tailEnd=\"2052\" samples_per_nt=\"7.31\" tail_length=\"172.44\"",
        "ATGC", "ABCD");
    assertEquals("ATGC", s3.getSequence());
    assertEquals("ABCD", s3.getQuality());
    assertTrue(filter.accept(s3));
    assertEquals("GCAT", s1.getSequence());
    assertEquals("DCBA", s1.getQuality());
    assertEquals("GCAT", s1.getSequence());
    assertEquals("DCBA", s1.getQuality());

    ReadSequence s4 = new ReadSequence(
        "de3e503c-f1c6-4e8a-9ff6-a6d53486e278 runid=93f969536487e5b7e7f11d78f5b14fcf7708f8f5 sampleid=TOTO_A2018 read=10006 ch=431 start_time=2018-09-27T16:29:39Z tail_type=\"polyA\" tail_is_valid=\"FALSE\" tailStart=\"\" tailEnd=\"\" samples_per_nt=\"10.77\" tail_length=\"\"",
        "ATGC", "ABCD");
    assertEquals("ATGC", s4.getSequence());
    assertEquals("ABCD", s4.getQuality());
    assertTrue(filter.accept(s4));
    assertEquals("GCAT", s1.getSequence());
    assertEquals("DCBA", s1.getQuality());

    ReadSequence s5 = new ReadSequence(
        "daff168f-ab3c-4a39-93b8-ac22d157622e runid=93f969536487e5b7e7f11d78f5b14fcf7708f8f5 sampleid=TOTO_A2018 read=10007 ch=26 start_time=2018-09-27T16:30:02Z tail_type=\"invalid\" tail_is_valid=\"FALSE\" tailStart=\"\" tailEnd=\"\" samples_per_nt=\"8.8\" tail_length=\"\"",
        "ATGC", "ABCD");
    assertEquals("ATGC", s5.getSequence());
    assertEquals("ABCD", s5.getQuality());
    assertTrue(filter.accept(s5));
    assertEquals("GCAT", s1.getSequence());
    assertEquals("DCBA", s1.getQuality());

  }

}

