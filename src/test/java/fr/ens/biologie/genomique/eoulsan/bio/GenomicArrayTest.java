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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

/**
 * TODO: finish the test class.
 * @since 1.2
 * @author Laurent Jourdren
 * @author Claire Wallon
 */
public class GenomicArrayTest {

  private class FastGenomicInterval {

    final String chromosome;
    final char strand;

    GenomicInterval iv(final int start, final int end) {

      return new GenomicInterval(this.chromosome, start, end, this.strand);
    }

    FastGenomicInterval(final String chromosome, final char strand) {

      this.chromosome = chromosome;
      this.strand = strand;
    }

  }

  private GenomicArray<String> ga;
  private FastGenomicInterval fgi;
  private FastGenomicInterval fgi_stranded;
  private Map<GenomicInterval, Set<String>> r;

  // /**
  // * @throws java.lang.Exception
  // */
  @Before
  public void setUp() throws Exception {

    this.ga = new GenomicArray<>();
    this.fgi = new FastGenomicInterval("chr1", '.');
    this.fgi_stranded = new FastGenomicInterval("chr1", '-');
  }

  /**
   * Test method for
   * {fr.ens.biologie.genomique.eoulsan.bio.GenomicArray#addEntry(fr.ens
   * .biologie.genomique.eoulsan.bio.GenomicInterval, java.lang.Object)}.
   */
  @Test
  public void testAddEntry() {

    /*
     * In comments of this function, the order of intervals that is indicated is
     * defined by the starting positions of each intervals on the chromosome.
     * For example, "the first interval is before the second one" means that the
     * starting position of the first interval is less than the starting
     * position of the second interval.
     */

    /*
     * The two intervals are non-overlapping and the first one is before the
     * second one.
     */
    // Add [10,50]=a
    GenomicInterval iv1 = this.fgi.iv(10, 50);
    this.ga.addEntry(iv1, "a");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(10, 50)));
    assertFalse(this.r.containsKey(this.fgi.iv(60, 70)));

    // Add [60,70]=b
    this.ga.addEntry(this.fgi.iv(60, 70), "b");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(60, 70)));

    /*
     * The two intervals are non overlapping and the first one is after the
     * second.
     */
    this.ga.clear();
    // Add [15,20]=a
    iv1 = this.fgi.iv(15, 20);
    this.ga.addEntry(iv1, "a");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(15, 20)));
    assertEquals(1, this.r.get(this.fgi.iv(15, 20)).size());
    assertFalse(this.r.containsKey(this.fgi.iv(60, 70)));

    // Add [5,10]=b
    this.ga.addEntry(this.fgi.iv(5, 10), "b");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(15, 20)));
    assertEquals(1, this.r.get(this.fgi.iv(15, 20)).size());
    assertTrue(this.r.containsKey(this.fgi.iv(5, 10)));
    assertEquals(1, this.r.get(this.fgi.iv(5, 10)).size());

    /*
     * The two intervals are overlapping and the first one is before the second
     * one.
     */
    this.ga.clear();
    // Add [10,20]=a
    iv1 = this.fgi_stranded.iv(10, 20);
    this.ga.addEntry(iv1, "a");

    this.r = this.ga.getEntries(this.fgi_stranded.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi_stranded.iv(10, 20)));
    assertFalse(this.r.containsKey(this.fgi_stranded.iv(60, 70)));

    // Add [15,25]=b
    this.ga.addEntry(this.fgi_stranded.iv(15, 25), "b");

    this.r = this.ga.getEntries(this.fgi_stranded.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi_stranded.iv(10, 14)));
    assertEquals(1, this.r.get(this.fgi_stranded.iv(10, 14)).size());

    assertTrue(this.r.containsKey(this.fgi_stranded.iv(15, 20)));
    assertEquals(2, this.r.get(this.fgi_stranded.iv(15, 20)).size());

    assertTrue(this.r.containsKey(this.fgi_stranded.iv(21, 25)));
    assertEquals(1, this.r.get(this.fgi_stranded.iv(21, 25)).size());

    /*
     * The two intervals are overlapping and the first one is after the second
     * one.
     */
    this.ga.clear();
    // Add [10,40]=a
    iv1 = this.fgi.iv(10, 40);
    this.ga.addEntry(iv1, "a");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(10, 40)));
    assertFalse(this.r.containsKey(this.fgi.iv(60, 70)));

    // Add [5,30]=b
    this.ga.addEntry(this.fgi.iv(5, 30), "b");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(5, 9)));
    assertEquals(1, this.r.get(this.fgi.iv(5, 9)).size());

    assertTrue(this.r.containsKey(this.fgi.iv(10, 30)));
    assertEquals(2, this.r.get(this.fgi.iv(10, 30)).size());

    assertTrue(this.r.containsKey(this.fgi.iv(31, 40)));
    assertEquals(1, this.r.get(this.fgi.iv(31, 40)).size());

    /*
     * The two intervals are overlapping on only one base and the first interval
     * is before the second one.
     */
    this.ga.clear();
    // Add [10,20]=a
    iv1 = this.fgi.iv(10, 20);
    this.ga.addEntry(iv1, "a");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(10, 20)));
    assertFalse(this.r.containsKey(this.fgi.iv(60, 70)));

    // Add [20,30]=b
    this.ga.addEntry(this.fgi.iv(20, 30), "b");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(10, 19)));
    assertEquals(1, this.r.get(this.fgi.iv(10, 19)).size());

    assertTrue(this.r.containsKey(this.fgi.iv(20, 20)));
    assertEquals(2, this.r.get(this.fgi.iv(20, 20)).size());

    assertTrue(this.r.containsKey(this.fgi.iv(21, 30)));
    assertEquals(1, this.r.get(this.fgi.iv(21, 30)).size());

    /*
     * The two intervals are overlapping on only one base and the first interval
     * is after the second one.
     */
    this.ga.clear();
    // Add [20,30]=a
    iv1 = this.fgi.iv(20, 30);
    this.ga.addEntry(iv1, "a");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(20, 30)));
    assertFalse(this.r.containsKey(this.fgi.iv(60, 70)));

    // Add [10,20]=b
    this.ga.addEntry(this.fgi.iv(10, 20), "b");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(10, 19)));
    assertEquals(1, this.r.get(this.fgi.iv(10, 19)).size());

    assertTrue(this.r.containsKey(this.fgi.iv(20, 20)));
    assertEquals(2, this.r.get(this.fgi.iv(20, 20)).size());

    assertTrue(this.r.containsKey(this.fgi.iv(21, 30)));
    assertEquals(1, this.r.get(this.fgi.iv(21, 30)).size());

    /*
     * The first interval is included in the second one and they have the same
     * starting position.
     */
    this.ga.clear();
    // Add [20,40]=a
    iv1 = this.fgi.iv(20, 40);
    this.ga.addEntry(iv1, "a");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(20, 40)));
    assertFalse(this.r.containsKey(this.fgi.iv(60, 70)));

    // Add [20,55]=b
    this.ga.addEntry(this.fgi.iv(20, 55), "b");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(20, 40)));
    assertEquals(2, this.r.get(this.fgi.iv(20, 40)).size());

    assertTrue(this.r.containsKey(this.fgi.iv(41, 55)));
    assertEquals(1, this.r.get(this.fgi.iv(41, 55)).size());

    /*
     * The second interval is included in the first one and they have the same
     * starting position.
     */
    this.ga.clear();
    // Add [20,40]=a
    iv1 = this.fgi.iv(20, 40);
    this.ga.addEntry(iv1, "a");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(20, 40)));
    assertFalse(this.r.containsKey(this.fgi.iv(60, 70)));

    // Add [20,35]=b
    this.ga.addEntry(this.fgi.iv(20, 35), "b");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(20, 35)));
    assertEquals(2, this.r.get(this.fgi.iv(20, 35)).size());

    assertTrue(this.r.containsKey(this.fgi.iv(36, 40)));
    assertEquals(1, this.r.get(this.fgi.iv(36, 40)).size());

    /*
     * The first interval is included in the second one and they have the same
     * ending position.
     */
    this.ga.clear();
    // Add [10,40]=a
    iv1 = this.fgi.iv(10, 40);
    this.ga.addEntry(iv1, "a");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(10, 40)));
    assertFalse(this.r.containsKey(this.fgi.iv(60, 70)));

    // Add [5,40]=b
    this.ga.addEntry(this.fgi.iv(5, 40), "b");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(5, 9)));
    assertEquals(1, this.r.get(this.fgi.iv(5, 9)).size());

    assertTrue(this.r.containsKey(this.fgi.iv(10, 40)));
    assertEquals(2, this.r.get(this.fgi.iv(10, 40)).size());

    /*
     * The second interval is included in the first one and they have the same
     * ending position.
     */
    this.ga.clear();
    // Add [5,40]=a
    iv1 = this.fgi.iv(5, 40);
    this.ga.addEntry(iv1, "a");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(5, 40)));
    assertFalse(this.r.containsKey(this.fgi.iv(60, 70)));

    // Add [10,40]=b
    this.ga.addEntry(this.fgi.iv(10, 40), "b");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(5, 9)));
    assertEquals(1, this.r.get(this.fgi.iv(5, 9)).size());

    assertTrue(this.r.containsKey(this.fgi.iv(10, 40)));
    assertEquals(2, this.r.get(this.fgi.iv(10, 40)).size());

    /*
     * The second interval is included in the first one.
     */
    this.ga.clear();
    // Add [20,40]=a
    iv1 = this.fgi.iv(20, 40);
    this.ga.addEntry(iv1, "a");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(20, 40)));
    assertFalse(this.r.containsKey(this.fgi.iv(60, 70)));

    // Add [25,35]=b
    this.ga.addEntry(this.fgi.iv(25, 35), "b");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(20, 24)));
    assertEquals(1, this.r.get(this.fgi.iv(20, 24)).size());

    assertTrue(this.r.containsKey(this.fgi.iv(25, 35)));
    assertEquals(2, this.r.get(this.fgi.iv(25, 35)).size());

    assertTrue(this.r.containsKey(this.fgi.iv(36, 40)));
    assertEquals(1, this.r.get(this.fgi.iv(36, 40)).size());

    /*
     * The first interval is included in the second one.
     */
    this.ga.clear();
    // Add [25,35]=a
    iv1 = this.fgi.iv(25, 35);
    this.ga.addEntry(iv1, "a");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(25, 35)));
    assertFalse(this.r.containsKey(this.fgi.iv(60, 70)));

    // Add [20,40]=b
    this.ga.addEntry(this.fgi.iv(20, 40), "b");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(20, 24)));
    assertEquals(1, this.r.get(this.fgi.iv(20, 24)).size());

    assertTrue(this.r.containsKey(this.fgi.iv(25, 35)));
    assertEquals(2, this.r.get(this.fgi.iv(25, 35)).size());

    assertTrue(this.r.containsKey(this.fgi.iv(36, 40)));
    assertEquals(1, this.r.get(this.fgi.iv(36, 40)).size());

    /*
     * The two intervals are identical.
     */
    this.ga.clear();
    // Add [10,40]=a
    iv1 = this.fgi.iv(10, 40);
    this.ga.addEntry(iv1, "a");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(10, 40)));
    assertFalse(this.r.containsKey(this.fgi.iv(60, 70)));

    // Add [10,40]=b
    this.ga.addEntry(this.fgi.iv(10, 40), "b");

    this.r = this.ga.getEntries(this.fgi.chromosome, 1, 100);

    assertTrue(this.r.containsKey(this.fgi.iv(10, 40)));
    assertEquals(2, this.r.get(this.fgi.iv(10, 40)).size());
  }

  //
  // /**
  // * Test method for
  // * {fr.ens.biologie.genomique.eoulsan.bio.GenomicArray#getEntries(fr
  // * .ens.biologie.genomique.eoulsan.bio.GenomicInterval)}.
  // */
  // @Test
  // public void testGetEntriesGenomicInterval() {
  // fail("Not yet implemented");
  // }
  //
  // /**
  // * Test method for
  // *
  // {fr.ens.biologie.genomique.eoulsan.bio.GenomicArray#getEntries(java.lang.String,
  // * int, int)}.
  // */
  // @Test
  // public void testGetEntriesStringIntInt() {
  // fail("Not yet implemented");
  // }
  //
  // /**
  // * Test method for
  // * {fr.ens.biologie.genomique.eoulsan.bio.GenomicArray#containsChromosome
  // * (java.lang.String)}.
  // */
  // @Test
  // public void testContainsChromosome() {
  // fail("Not yet implemented");
  // }

}
