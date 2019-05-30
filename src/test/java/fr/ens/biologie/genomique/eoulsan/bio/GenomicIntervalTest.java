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

import java.util.Objects;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Claire Wallon
 */
public class GenomicIntervalTest {

  String gffStr = "chr18\tprotein_coding\texon\t32322743\t32323204\t.\t+\t."
      + "\tID=exon:ENSMUST00000025242:1; PARENT=ENSMUST00000025242;";
  GFFEntry gffEnt = new GFFEntry();
  GenomicInterval gi;

  @Before
  public void setUp() throws Exception {
    try {
      gffEnt.parseGFF3(gffStr);
    } catch (BadBioEntryException e) {
      fail();
    }

    // valid GenomicInterval object
    gi = new GenomicInterval(gffEnt);
  }

  @Test
  public void testGetChromosome() {
    assertEquals(gi.getChromosome(), "chr18");
      assertNotEquals("chr1", gi.getChromosome());
  }

  @Test
  public void testGetStart() {
    assertEquals(gi.getStart(), 32322743);
  }

  @Test
  public void testGetEnd() {
    assertEquals(gi.getEnd(), 32323204);
  }

  @Test
  public void testGetStrand() {
    assertEquals(gi.getStrand(), '+');
  }

  @Test
  public void testGetLength() {
    assertEquals(gi.getLength(), 462);
  }

  @Test
  public void testInclude() {

    // the interval is included in "gi"
    assertTrue(gi.include(32322800, 32323000));
    // the interval is before "gi"
    assertFalse(gi.include(32322500, 32322600));
    // the interval is after "gi"
    assertFalse(gi.include(32323300, 32323400));
    // the interval overlaps the begin of "gi"
    assertFalse(gi.include(32322700, 32323000));
    // the interval overlaps the end of "gi"
    assertFalse(gi.include(32322800, 32323300));
    // the interval covers "gi"
    assertFalse(gi.include(32322700, 32323300));

    // the interval is included in "gi"
    assertTrue(gi.include(32323000, 32322800));
    // the interval is before "gi"
    assertFalse(gi.include(32322600, 32322500));
    // the interval is after "gi"
    assertFalse(gi.include(32323400, 32323300));
    // the interval overlaps the begin of "gi"
    assertFalse(gi.include(32323000, 32322700));
    // the interval overlaps the end of "gi"
    assertFalse(gi.include(32323300, 32322800));
    // the interval covers "gi"
    assertFalse(gi.include(32323300, 32322700));
  }

  @Test
  public void testIntersect() {

    // the interval is included in "gi"
    assertTrue(gi.intersect(32322800, 32323000));
    // the interval is before "gi"
    assertFalse(gi.intersect(32322500, 32322600));
    // the interval is after "gi"
    assertFalse(gi.intersect(32323300, 32323400));
    // the interval overlaps the begin of "gi"
    assertTrue(gi.intersect(32322700, 32323000));
    // the interval overlaps the end of "gi"
    assertTrue(gi.intersect(32322800, 32323300));
    // the interval covers "gi"
    assertTrue(gi.intersect(32322700, 32323300));

    // the interval is included in "gi"
    assertTrue(gi.intersect(32323000, 32322800));
    // the interval is before "gi"
    assertFalse(gi.intersect(32322600, 32322500));
    // the interval is after "gi"
    assertFalse(gi.intersect(32323400, 32323300));
    // the interval overlaps the begin of "gi"
    assertTrue(gi.intersect(32323000, 32322700));
    // the interval overlaps the end of "gi"
    assertTrue(gi.intersect(32323300, 32322800));
    // the interval covers "gi"
    assertTrue(gi.intersect(32323300, 32322700));

    assertTrue(
        gi.intersect(new GenomicInterval("chr18", 32322800, 32322800, '+')));
    assertFalse(gi.intersect(null));
    assertFalse(
        gi.intersect(new GenomicInterval("chr1", 32322800, 32322800, '+')));
    assertFalse(
        gi.intersect(new GenomicInterval("chr18", 32322800, 32322800, '-')));
    assertTrue(
        gi.intersect(new GenomicInterval("chr18", 32322800, 32322800, '.')));
    assertTrue(new GenomicInterval("chr18", 32322743, 32323204, '.')
        .intersect(new GenomicInterval("chr18", 32322800, 32322800, '+')));

  }

  @Test
  public void testIntersectLength() {

    GenomicInterval gi = new GenomicInterval("chr18", 1000, 2000, '+');

    assertEquals(201, gi.intersectLength(1200, 1400));
    assertEquals(0, gi.intersectLength(500, 999));
    assertEquals(0, gi.intersectLength(2001, 3000));
    assertEquals(1001, gi.intersectLength(1000, 2000));
    assertEquals(1001, gi.intersectLength(500, 3000));
    assertEquals(1, gi.intersectLength(500, 1000));
    assertEquals(1, gi.intersectLength(2000, 3000));

    assertEquals(201, gi.intersectLength(1400, 1200));
    assertEquals(0, gi.intersectLength(999, 500));
    assertEquals(0, gi.intersectLength(3000, 2001));
    assertEquals(1001, gi.intersectLength(2000, 1000));
    assertEquals(1001, gi.intersectLength(3000, 500));
    assertEquals(1, gi.intersectLength(1000, 500));
    assertEquals(1, gi.intersectLength(3000, 2000));

    assertEquals(1001,
        gi.intersectLength(new GenomicInterval("chr18", 1000, 2000, '+')));
    assertEquals(0, gi.intersectLength(null));
    assertEquals(0,
        gi.intersectLength(new GenomicInterval("chr1", 1000, 2000, '+')));
    assertEquals(0,
        gi.intersectLength(new GenomicInterval("chr18", 1000, 2000, '-')));
    assertEquals(1001,
        gi.intersectLength(new GenomicInterval("chr18", 1000, 2000, '.')));
    assertEquals(1001, new GenomicInterval("chr18", 1000, 2000, '.')
        .intersectLength(new GenomicInterval("chr18", 1000, 2000, '+')));

  }

  @Test
  public void testCompareTo() {

    String str = "chr18\tprotein_coding\texon\t32351555\t32351624\t.\t-\t.\t"
        + "ID=exon:ENSMUST00000025242:2; PARENT=ENSMUST00000025242; ";
    GFFEntry gff = new GFFEntry();
    try {
      gff.parseGFF3(str);
    } catch (BadBioEntryException e) {
      fail();
    }

    GenomicInterval genInt = new GenomicInterval(gff);
    assertNotNull(genInt);

    assertEquals(-1, gi.compareTo(null));

    assertTrue(gi.compareTo(genInt) < 0);
    assertTrue(genInt.compareTo(gi) > 0);
      assertEquals(0, gi.compareTo(gi));

    str = "chr19\tprotein_coding\texon\t32351555\t32351624\t.\t-\t.\t"
        + "ID=exon:ENSMUST00000025242:2; PARENT=ENSMUST00000025242; ";
    gff = new GFFEntry();
    try {
      gff.parseGFF3(str);
    } catch (BadBioEntryException e) {
      fail();
    }
    genInt = new GenomicInterval(gff);

    assertTrue(gi.compareTo(genInt) < 0);
  }

  @Test
  public void testEqualsObject() {

      assertEquals(gi, gi);

    Object o = new GenomicInterval("chr18", 32322743, 32323204, '+');
      assertEquals(gi, o);

    o = new GenomicInterval("chr19", 32322743, 32323204, '+');
      assertNotEquals(gi, o);

    o = new GenomicInterval("chr18", 32322740, 32323204, '+');
      assertNotEquals(gi, o);

    o = new GenomicInterval("chr18", 32322743, 32323200, '+');
      assertNotEquals(gi, o);

    o = new GenomicInterval("chr18", 32322743, 32323204, '-');
      assertNotEquals(gi, o);

    o = new GenomicInterval("chr18", 32322743, 32323204, '.');
      assertNotEquals(gi, o);

      assertNotEquals(null, gi);

      assertNotEquals("toto", gi);
  }

  @Test
  public void testToString() {
    String str = "GenomicInterval{chr18 [32322743-32323204]+}";
    assertEquals(str, gi.toString());
  }

  @Test
  public void testGenomicIntervalStringIntIntChar() {

    // test the condition on the chromosome name
    try {
      GenomicInterval t0 = new GenomicInterval(null, 12, 0, '.');
        fail();
      assertNull(t0);
    } catch (NullPointerException e) {
      assertTrue(true);
    }

    // first test on start and end positions: the end position
    // have to be greater than the start position
    try {
      GenomicInterval t1 = new GenomicInterval("chrTest", 12, 0, '.');
        fail();
      assertNull(t1);
    } catch (IllegalArgumentException e) {
      assertTrue(true);
    }

    // second test on start and end positions: the start position
    // have to be greater than 1
    try {
      GenomicInterval t2 = new GenomicInterval("chrTest", 0, 10, '.');
        fail();
      assertNull(t2);
    } catch (IllegalArgumentException e) {
      assertTrue(true);
    }

    // test on the strand
    try {
      GenomicInterval t3 = new GenomicInterval("chrTest", 0, 10, '!');
        fail();
      assertNull(t3);
    } catch (IllegalArgumentException e) {
      assertTrue(true);
    }

    // valid test
    GenomicInterval tok = new GenomicInterval("chrOk", 1, 10, '.');
    assertNotNull(tok);
  }

  @Test
  public void testGenomicIntervalGFFEntry() {
    GenomicInterval tok = new GenomicInterval(gffEnt);
    assertNotNull(tok);
    assertEquals("chr18", tok.getChromosome());
    assertEquals(32322743, tok.getStart());
    assertEquals(32323204, tok.getEnd());
    assertEquals('+', tok.getStrand());
  }

  @Test
  public void testGenomicIntervalGFFEntryString() {
    GenomicInterval tok = new GenomicInterval(gffEnt, true);
    assertNotNull(tok);
    assertEquals("chr18", tok.getChromosome());
    assertEquals(32322743, tok.getStart());
    assertEquals(32323204, tok.getEnd());
    assertEquals('+', tok.getStrand());

    GenomicInterval tok2 = new GenomicInterval(gffEnt, false);
    assertNotNull(tok2);
    assertEquals("chr18", tok2.getChromosome());
    assertEquals(32322743, tok2.getStart());
    assertEquals(32323204, tok2.getEnd());
    assertEquals('.', tok2.getStrand());
  }

  @Test
  public void testNew() {

    try {
      new GenomicInterval("chr1", 10, 100, '.');
    } catch (IllegalArgumentException e) {
      fail();
    }
    try {
      new GenomicInterval("chr1", 10, 100, '+');
    } catch (IllegalArgumentException e) {
      fail();
    }
    try {
      new GenomicInterval("chr1", 10, 100, '-');
    } catch (IllegalArgumentException e) {
      fail();
    }

    try {
      new GenomicInterval("chr1", 10, 100, '#');
      fail();
    } catch (IllegalArgumentException e) {
    }
  }

  @Test
  public void testHashcode() {

    assertEquals(Objects.hash("chr1", 10, 100, '.'),
        new GenomicInterval("chr1", 10, 100, '.').hashCode());
  }

}
