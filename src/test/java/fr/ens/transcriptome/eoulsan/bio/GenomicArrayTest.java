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

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Claire Wallon
 */
public class GenomicArrayTest {

  @Test
  public void bidon() {
  }

  // /**
  // * @throws java.lang.Exception
  // */
  // @Before
  // public void setUp() throws Exception {
  // }
  //

  private class FastGemomicInterval {

    final String chromosome;
    final char strand;

    GenomicInterval iv(final int start, final int end) {

      return new GenomicInterval(chromosome, start, end, strand);
    }

    FastGemomicInterval(final String chromosome, final char strand) {

      this.chromosome = chromosome;
      this.strand = strand;
    }

  }

  /**
   * Test method for
   * {fr.ens.transcriptome.eoulsan.bio.GenomicArray#addEntry(fr.ens
   * .transcriptome.eoulsan.bio.GenomicInterval, java.lang.Object)}.
   */
  @Test
  public void testAddEntry() {

    GenomicArray<String> ga = new GenomicArray<String>();

    final FastGemomicInterval fgi = new FastGemomicInterval("chr1", '.');
    
    

    // Add [10,50]=a
    GenomicInterval iv1 = fgi.iv(10, 50);
    ga.addEntry(iv1, "a");
    
    Map<GenomicInterval,String> r = ga.getEntries(fgi.chromosome, 1, 100);
    System.out.println(r);
    
    
    assertTrue(r.containsKey(fgi.iv(10,50)));
    assertFalse(r.containsKey(fgi.iv(60,70)));
    
    // Add [60,70]=a
    ga.addEntry(fgi.iv(60, 70), "b");
    
    r = ga.getEntries(fgi.chromosome, 1, 100);
    System.out.println(r);
    
    assertTrue(r.containsKey(fgi.iv(60,70)));

    System.out.println(ga.getEntries(fgi.chromosome, 1, 100));
    
  }

  
  @Test
  public void testAddEntry2() {

    System.out.println("=====");
    
    GenomicArray<String> ga = new GenomicArray<String>();

    final FastGemomicInterval fgi = new FastGemomicInterval("chr1", '.');
    
    

    // Add [10,20]=a
    GenomicInterval iv1 = fgi.iv(10, 20);
    ga.addEntry(iv1, "a");
    
    Map<GenomicInterval,String> r = ga.getEntries(fgi.chromosome, 1, 100);
    System.out.println(r);
    
    
    assertTrue(r.containsKey(fgi.iv(10,20)));
    assertFalse(r.containsKey(fgi.iv(60,70)));
    
    // Add [15,25]=a
    ga.addEntry(fgi.iv(15, 25), "b");
    
    r = ga.getEntries(fgi.chromosome, 1, 100);
    System.out.println(r);
    
    assertTrue(r.containsKey(fgi.iv(10,14)));
    assertTrue(r.containsKey(fgi.iv(15,20)));
    assertTrue(r.containsKey(fgi.iv(21,25)));
    

    System.out.println(ga.getEntries(fgi.chromosome, 1, 100));
    
  }
  
  //@Test
  public void testAddEntry3() {

    System.out.println("=====");
    
    GenomicArray<String> ga = new GenomicArray<String>();

    final FastGemomicInterval fgi = new FastGemomicInterval("chr1", '.');
    
    


    GenomicInterval iv1 = fgi.iv(15, 20);
    ga.addEntry(iv1, "a");
    
    Map<GenomicInterval,String> r = ga.getEntries(fgi.chromosome, 1, 100);
    System.out.println(r);
    
    
    assertTrue(r.containsKey(fgi.iv(15,20)));
    assertFalse(r.containsKey(fgi.iv(60,70)));
    
    // Add [15,25]=a
    ga.addEntry(fgi.iv(5, 10), "b");
    
    r = ga.getEntries(fgi.chromosome, 1, 100);
    System.out.println(r);
    
    assertTrue(r.containsKey(fgi.iv(15,20)));
    assertTrue(r.containsKey(fgi.iv(5,10)));
  
    

    System.out.println(ga.getEntries(fgi.chromosome, 1, 100));
    
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
