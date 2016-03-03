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

import org.junit.Test;

/**
 * @author Claire Wallon
 */
public class GenomicIntervalTest {

  @Test
  public void bidon() {
  }

  // String gffStr = "chr18\tprotein_coding\texon\t32322743\t32323204\t.\t+\t."
  // + "\tID=exon:ENSMUST00000025242:1; PARENT=ENSMUST00000025242;";
  // GFFEntry gffEnt = new GFFEntry();
  // GenomicInterval gi;
  //
  // /**
  // * @throws java.lang.Exception
  // */
  // @Before
  // public void setUp() throws Exception {
  // try {
  // gffEnt.parse(gffStr);
  // } catch (BadBioEntryException e) {
  // fail();
  // }
  //
  // // valid GenomicInterval object
  // gi = new GenomicInterval(gffEnt);
  // }
  //
  // /**
  // * Test method for
  // * {fr.ens.biologie.genomique.eoulsan.bio.GenomicInterval#getChromosome()}.
  // */
  // @Test
  // public void testGetChromosome() {
  // assertEquals(gi.getChromosome(), "chr18");
  // assertFalse(gi.getChromosome().equals("chr1"));
  // }
  //
  // /**
  // * Test method for
  // * {fr.ens.biologie.genomique.eoulsan.bio.GenomicInterval#getStart()}.
  // */
  // @Test
  // public void testGetStart() {
  // assertEquals(gi.getStart(), 32322743);
  // }
  //
  // /**
  // * Test method for
  // * {fr.ens.biologie.genomique.eoulsan.bio.GenomicInterval#getEnd()}.
  // */
  // @Test
  // public void testGetEnd() {
  // assertEquals(gi.getEnd(), 32323204);
  // }
  //
  // /**
  // * Test method for
  // * {fr.ens.biologie.genomique.eoulsan.bio.GenomicInterval#getStrand()}.
  // */
  // @Test
  // public void testGetStrand() {
  // assertEquals(gi.getStrand(), '+');
  // }
  //
  // /**
  // * Test method for
  // * {fr.ens.biologie.genomique.eoulsan.bio.GenomicInterval#getLength()}.
  // */
  // @Test
  // public void testGetLength() {
  // assertEquals(gi.getLength(), 462);
  // }
  //
  // /**
  // * Test method for
  // * {fr.ens.biologie.genomique.eoulsan.bio.GenomicInterval#include(int, int)}.
  // */
  // @Test
  // public void testInclude() {
  // // the interval is included in "gi"
  // assertTrue(gi.include(32322800, 32323000));
  // // the interval is before "gi"
  // assertFalse(gi.include(32322500, 32322600));
  // // the interval is after "gi"
  // assertFalse(gi.include(32323300, 32323400));
  // // the interval overlaps the begin of "gi"
  // assertFalse(gi.include(32322700, 32323000));
  // // the interval overlaps the end of "gi"
  // assertFalse(gi.include(32322800, 32323300));
  // // the interval covers "gi"
  // assertFalse(gi.include(32322700, 32323300));
  // }
  //
  // /**
  // * Test method for
  // * {fr.ens.biologie.genomique.eoulsan.bio.GenomicInterval#intersect(int, int)}.
  // */
  // @Test
  // public void testIntersect() {
  // // the interval is included in "gi"
  // assertTrue(gi.intersect(32322800, 32323000));
  // // the interval is before "gi"
  // assertFalse(gi.include(32322500, 32322600));
  // // the interval is after "gi"
  // assertFalse(gi.intersect(32323300, 32323400));
  // // the interval overlaps the begin of "gi"
  // assertTrue(gi.intersect(32322700, 32323000));
  // // the interval overlaps the end of "gi"
  // assertTrue(gi.intersect(32322800, 32323300));
  // // the interval covers "gi"
  // assertTrue(gi.intersect(32322700, 32323300));
  // }
  //
  // /**
  // * Test method for
  // * {fr.ens.biologie.genomique.eoulsan.bio.GenomicInterval#compareTo(
  // * fr.ens.biologie.genomique.eoulsan.bio.GenomicInterval)}.
  // */
  // @Test
  // public void testCompareTo() {
  // String str =
  // "chr18\tprotein_coding\texon\t32351555\t32351624\t.\t-\t.\t"
  // + "ID=exon:ENSMUST00000025242:2; PARENT=ENSMUST00000025242; ";
  // GFFEntry gff = new GFFEntry();
  // try {
  // gff.parse(str);
  // } catch (BadBioEntryException e) {
  // fail();
  // }
  //
  // GenomicInterval genInt = new GenomicInterval(gff);
  // assertNotNull(genInt);
  //
  // assertTrue(gi.compareTo(genInt) < 0);
  // assertTrue(genInt.compareTo(gi) > 0);
  // assertTrue(gi.compareTo(gi) == 0);
  // }
  //
  // /**
  // * Test method for
  // * {fr.ens.biologie.genomique.eoulsan.bio.GenomicInterval#equals(java
  // * .lang.Object)}.
  // */
  // @Test
  // public void testEqualsObject() {
  //
  // assertTrue(gi.equals(gi));
  //
  // Object o = new GenomicInterval("chr18", 32322743, 32323204, '+');
  // assertFalse(gi.equals(o)); // ??????
  //
  // // parentId : ENSMUST00000025242
  // // Object exon =
  // // new Exon("chr18", 32322743, 32323204, '+', null);
  // // assertTrue(gi.equals(exon));
  // }
  //
  // /**
  // * Test method for
  // * {fr.ens.biologie.genomique.eoulsan.bio.GenomicInterval#toString()}.
  // */
  // @Test
  // public void testToString() {
  // String str = "GenomicInterval{chr18 [32322743-32323204]+}";
  // assertEquals(str, gi.toString());
  // }
  //
  // /**
  // * Test method for
  // * {fr.ens.biologie.genomique.eoulsan.bio.GenomicInterval#GenomicInterval
  // * (java.lang.String, int, int, char)}.
  // */
  // @Test
  // public void testGenomicIntervalStringIntIntChar() {
  //
  // // test the condition on the chromosome name
  // try {
  // GenomicInterval t0 = new GenomicInterval(null, 12, 0, '.');
  // assertTrue(false);
  // assertNull(t0);
  // } catch (NullPointerException e) {
  // assertTrue(true);
  // }
  //
  // // first test on start and end positions: the end position
  // // have to be greater than the start position
  // try {
  // GenomicInterval t1 = new GenomicInterval("chrTest", 12, 0, '.');
  // assertTrue(false);
  // assertNull(t1);
  // } catch (IllegalArgumentException e) {
  // assertTrue(true);
  // }
  //
  // // second test on start and end positions: the start position
  // // have to be greater than 1
  // try {
  // GenomicInterval t2 = new GenomicInterval("chrTest", 0, 10, '.');
  // assertTrue(false);
  // assertNull(t2);
  // } catch (IllegalArgumentException e) {
  // assertTrue(true);
  // }
  //
  // // test on the strand
  // try {
  // GenomicInterval t3 = new GenomicInterval("chrTest", 0, 10, '!');
  // assertTrue(false);
  // assertNull(t3);
  // } catch (IllegalArgumentException e) {
  // assertTrue(true);
  // }
  //
  // // valid test
  // GenomicInterval tok = new GenomicInterval("chrOk", 1, 10, '.');
  // assertNotNull(tok);
  // }
  //
  // /**
  // * Test method for
  // * {fr.ens.biologie.genomique.eoulsan.bio.GenomicInterval#GenomicInterval
  // * (fr.ens.biologie.genomique.eoulsan.bio.GFFEntry)}.
  // */
  // @Test
  // public void testGenomicIntervalGFFEntry() {
  // GenomicInterval tok = new GenomicInterval(gffEnt);
  // assertNotNull(tok);
  // assertEquals("chr18", tok.getChromosome());
  // assertEquals(32322743, tok.getStart());
  // assertEquals(32323204, tok.getEnd());
  // assertEquals('+', tok.getStrand());
  // }
  //
  // /**
  // * Test method for
  // * {fr.ens.biologie.genomique.eoulsan.bio.GenomicInterval#GenomicInterval
  // * (fr.ens.biologie.genomique.eoulsan.bio.GFFEntry, java.lang.String)}.
  // */
  // @Test
  // public void testGenomicIntervalGFFEntryString() {
  // GenomicInterval tok = new GenomicInterval(gffEnt, "true");
  // assertNotNull(tok);
  // assertEquals("chr18", tok.getChromosome());
  // assertEquals(32322743, tok.getStart());
  // assertEquals(32323204, tok.getEnd());
  // assertEquals('+', tok.getStrand());
  //
  // GenomicInterval tok2 = new GenomicInterval(gffEnt, "false");
  // assertNotNull(tok2);
  // assertEquals("chr18", tok2.getChromosome());
  // assertEquals(32322743, tok2.getStart());
  // assertEquals(32323204, tok2.getEnd());
  // assertEquals('.', tok2.getStrand());
  // }

}
