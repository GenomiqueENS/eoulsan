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
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.bio;

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

      return new GenomicInterval(chromosome, start, end, strand);
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

    ga = new GenomicArray<>();
    fgi = new FastGenomicInterval("chr1", '.');
    fgi_stranded = new FastGenomicInterval("chr1", '-');
  }

  /**
   * Test method for
   * {fr.ens.transcriptome.eoulsan.bio.GenomicArray#addEntry(fr.ens
   * .transcriptome.eoulsan.bio.GenomicInterval, java.lang.Object)}.
   */
  @Test
  public void testAddEntry() {

    /**
     * In comments of this function, the order of intervals that is indicated is
     * defined by the starting positions of each intervals on the chromosome.
     * For example, "the first interval is before the second one" means that the
     * starting position of the first interval is less than the starting
     * position of the second interval.
     */

    /**
     * The two intervals are non-overlapping and the first one is before the
     * second one.
     */
    // Add [10,50]=a
    GenomicInterval iv1 = fgi.iv(10, 50);
    ga.addEntry(iv1, "a");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(10, 50)));
    assertFalse(r.containsKey(fgi.iv(60, 70)));

    // Add [60,70]=b
    ga.addEntry(fgi.iv(60, 70), "b");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(60, 70)));

    /**
     * The two intervals are non overlapping and the first one is after the
     * second.
     */
    ga.clear();
    // Add [15,20]=a
    iv1 = fgi.iv(15, 20);
    ga.addEntry(iv1, "a");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(15, 20)));
    assertEquals(1, r.get(fgi.iv(15, 20)).size());
    assertFalse(r.containsKey(fgi.iv(60, 70)));

    // Add [5,10]=b
    ga.addEntry(fgi.iv(5, 10), "b");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(15, 20)));
    assertEquals(1, r.get(fgi.iv(15, 20)).size());
    assertTrue(r.containsKey(fgi.iv(5, 10)));
    assertEquals(1, r.get(fgi.iv(5, 10)).size());

    /**
     * The two intervals are overlapping and the first one is before the second
     * one.
     */
    ga.clear();
    // Add [10,20]=a
    iv1 = fgi_stranded.iv(10, 20);
    ga.addEntry(iv1, "a");

    r = ga.getEntries(fgi_stranded.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi_stranded.iv(10, 20)));
    assertFalse(r.containsKey(fgi_stranded.iv(60, 70)));

    // Add [15,25]=b
    ga.addEntry(fgi_stranded.iv(15, 25), "b");

    r = ga.getEntries(fgi_stranded.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi_stranded.iv(10, 14)));
    assertEquals(1, r.get(fgi_stranded.iv(10, 14)).size());

    assertTrue(r.containsKey(fgi_stranded.iv(15, 20)));
    assertEquals(2, r.get(fgi_stranded.iv(15, 20)).size());

    assertTrue(r.containsKey(fgi_stranded.iv(21, 25)));
    assertEquals(1, r.get(fgi_stranded.iv(21, 25)).size());

    /**
     * The two intervals are overlapping and the first one is after the second
     * one.
     */
    ga.clear();
    // Add [10,40]=a
    iv1 = fgi.iv(10, 40);
    ga.addEntry(iv1, "a");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(10, 40)));
    assertFalse(r.containsKey(fgi.iv(60, 70)));

    // Add [5,30]=b
    ga.addEntry(fgi.iv(5, 30), "b");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(5, 9)));
    assertEquals(1, r.get(fgi.iv(5, 9)).size());

    assertTrue(r.containsKey(fgi.iv(10, 30)));
    assertEquals(2, r.get(fgi.iv(10, 30)).size());

    assertTrue(r.containsKey(fgi.iv(31, 40)));
    assertEquals(1, r.get(fgi.iv(31, 40)).size());

    /**
     * The two intervals are overlapping on only one base and the first interval
     * is before the second one.
     */
    ga.clear();
    // Add [10,20]=a
    iv1 = fgi.iv(10, 20);
    ga.addEntry(iv1, "a");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(10, 20)));
    assertFalse(r.containsKey(fgi.iv(60, 70)));

    // Add [20,30]=b
    ga.addEntry(fgi.iv(20, 30), "b");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(10, 19)));
    assertEquals(1, r.get(fgi.iv(10, 19)).size());

    assertTrue(r.containsKey(fgi.iv(20, 20)));
    assertEquals(2, r.get(fgi.iv(20, 20)).size());

    assertTrue(r.containsKey(fgi.iv(21, 30)));
    assertEquals(1, r.get(fgi.iv(21, 30)).size());

    /**
     * The two intervals are overlapping on only one base and the first interval
     * is after the second one.
     */
    ga.clear();
    // Add [20,30]=a
    iv1 = fgi.iv(20, 30);
    ga.addEntry(iv1, "a");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(20, 30)));
    assertFalse(r.containsKey(fgi.iv(60, 70)));

    // Add [10,20]=b
    ga.addEntry(fgi.iv(10, 20), "b");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(10, 19)));
    assertEquals(1, r.get(fgi.iv(10, 19)).size());

    assertTrue(r.containsKey(fgi.iv(20, 20)));
    assertEquals(2, r.get(fgi.iv(20, 20)).size());

    assertTrue(r.containsKey(fgi.iv(21, 30)));
    assertEquals(1, r.get(fgi.iv(21, 30)).size());

    /**
     * The first interval is included in the second one and they have the same
     * starting position.
     */
    ga.clear();
    // Add [20,40]=a
    iv1 = fgi.iv(20, 40);
    ga.addEntry(iv1, "a");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(20, 40)));
    assertFalse(r.containsKey(fgi.iv(60, 70)));

    // Add [20,55]=b
    ga.addEntry(fgi.iv(20, 55), "b");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(20, 40)));
    assertEquals(2, r.get(fgi.iv(20, 40)).size());

    assertTrue(r.containsKey(fgi.iv(41, 55)));
    assertEquals(1, r.get(fgi.iv(41, 55)).size());

    /**
     * The second interval is included in the first one and they have the same
     * starting position.
     */
    ga.clear();
    // Add [20,40]=a
    iv1 = fgi.iv(20, 40);
    ga.addEntry(iv1, "a");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(20, 40)));
    assertFalse(r.containsKey(fgi.iv(60, 70)));

    // Add [20,35]=b
    ga.addEntry(fgi.iv(20, 35), "b");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(20, 35)));
    assertEquals(2, r.get(fgi.iv(20, 35)).size());

    assertTrue(r.containsKey(fgi.iv(36, 40)));
    assertEquals(1, r.get(fgi.iv(36, 40)).size());

    /**
     * The first interval is included in the second one and they have the same
     * ending position.
     */
    ga.clear();
    // Add [10,40]=a
    iv1 = fgi.iv(10, 40);
    ga.addEntry(iv1, "a");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(10, 40)));
    assertFalse(r.containsKey(fgi.iv(60, 70)));

    // Add [5,40]=b
    ga.addEntry(fgi.iv(5, 40), "b");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(5, 9)));
    assertEquals(1, r.get(fgi.iv(5, 9)).size());

    assertTrue(r.containsKey(fgi.iv(10, 40)));
    assertEquals(2, r.get(fgi.iv(10, 40)).size());

    /**
     * The second interval is included in the first one and they have the same
     * ending position.
     */
    ga.clear();
    // Add [5,40]=a
    iv1 = fgi.iv(5, 40);
    ga.addEntry(iv1, "a");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(5, 40)));
    assertFalse(r.containsKey(fgi.iv(60, 70)));

    // Add [10,40]=b
    ga.addEntry(fgi.iv(10, 40), "b");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(5, 9)));
    assertEquals(1, r.get(fgi.iv(5, 9)).size());

    assertTrue(r.containsKey(fgi.iv(10, 40)));
    assertEquals(2, r.get(fgi.iv(10, 40)).size());

    /**
     * The second interval is included in the first one.
     */
    ga.clear();
    // Add [20,40]=a
    iv1 = fgi.iv(20, 40);
    ga.addEntry(iv1, "a");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(20, 40)));
    assertFalse(r.containsKey(fgi.iv(60, 70)));

    // Add [25,35]=b
    ga.addEntry(fgi.iv(25, 35), "b");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(20, 24)));
    assertEquals(1, r.get(fgi.iv(20, 24)).size());

    assertTrue(r.containsKey(fgi.iv(25, 35)));
    assertEquals(2, r.get(fgi.iv(25, 35)).size());

    assertTrue(r.containsKey(fgi.iv(36, 40)));
    assertEquals(1, r.get(fgi.iv(36, 40)).size());

    /**
     * The first interval is included in the second one.
     */
    ga.clear();
    // Add [25,35]=a
    iv1 = fgi.iv(25, 35);
    ga.addEntry(iv1, "a");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(25, 35)));
    assertFalse(r.containsKey(fgi.iv(60, 70)));

    // Add [20,40]=b
    ga.addEntry(fgi.iv(20, 40), "b");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(20, 24)));
    assertEquals(1, r.get(fgi.iv(20, 24)).size());

    assertTrue(r.containsKey(fgi.iv(25, 35)));
    assertEquals(2, r.get(fgi.iv(25, 35)).size());

    assertTrue(r.containsKey(fgi.iv(36, 40)));
    assertEquals(1, r.get(fgi.iv(36, 40)).size());

    /**
     * The two intervals are identical.
     */
    ga.clear();
    // Add [10,40]=a
    iv1 = fgi.iv(10, 40);
    ga.addEntry(iv1, "a");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(10, 40)));
    assertFalse(r.containsKey(fgi.iv(60, 70)));

    // Add [10,40]=b
    ga.addEntry(fgi.iv(10, 40), "b");

    r = ga.getEntries(fgi.chromosome, 1, 100);

    assertTrue(r.containsKey(fgi.iv(10, 40)));
    assertEquals(2, r.get(fgi.iv(10, 40)).size());
  }

  //
  // /**
  // * Test method for
  // * {fr.ens.transcriptome.eoulsan.bio.GenomicArray#getEntries(fr
  // * .ens.transcriptome.eoulsan.bio.GenomicInterval)}.
  // */
  // @Test
  // public void testGetEntriesGenomicInterval() {
  // fail("Not yet implemented");
  // }
  //
  // /**
  // * Test method for
  // *
  // {fr.ens.transcriptome.eoulsan.bio.GenomicArray#getEntries(java.lang.String,
  // * int, int)}.
  // */
  // @Test
  // public void testGetEntriesStringIntInt() {
  // fail("Not yet implemented");
  // }
  //
  // /**
  // * Test method for
  // * {fr.ens.transcriptome.eoulsan.bio.GenomicArray#containsChromosome
  // * (java.lang.String)}.
  // */
  // @Test
  // public void testContainsChromosome() {
  // fail("Not yet implemented");
  // }

}
