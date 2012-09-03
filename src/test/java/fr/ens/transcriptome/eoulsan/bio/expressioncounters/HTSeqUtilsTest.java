package fr.ens.transcriptome.eoulsan.bio.expressioncounters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.GenomicArray;
import fr.ens.transcriptome.eoulsan.bio.GenomicInterval;

public class HTSeqUtilsTest {

  @Before
  public void setUp() throws Exception {

    // GenomicArray<String> ga = new GenomicArray<String>();
    //
    // final FastGemomicInterval fgi = new FastGemomicInterval("chr1", '+');
    //
    // GenomicInterval iv1 = fgi.iv(10, 50);
    // ga.addEntry(iv1, "a");
  }

  // @Test
  public void testStoreAnnotation() {

  }

  // @Test
  public void testAddIntervals() {

  }

  // @Test
  public void testParseCigar() {

  }

  @Test
  public void testFeaturesOverlapped() throws EoulsanException, IOException {

    final String chromosome = "chr1";
    final char strand = '+';
    Set<String> results;

    // intervals of a SAM alignment
    List<GenomicInterval> ivSeq = new ArrayList<GenomicInterval>();
    ivSeq.add(new GenomicInterval(chromosome, 5, 15, strand));

    // annotation
    GenomicArray<String> annot = new GenomicArray<String>();
    annot.addEntry(new GenomicInterval(chromosome, 1, 20, strand), "a");
    annot.addEntry(new GenomicInterval(chromosome, 25, 45, strand), "b");

    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.UNION, StrandUsage.YES);
    assertTrue(results.contains("a"));
    assertFalse(results.contains("b"));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_NONEMPTY, StrandUsage.YES);
    assertTrue(results.contains("a"));
    assertFalse(results.contains("b"));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_STRICT, StrandUsage.YES);
    assertTrue(results.contains("a"));
    assertFalse(results.contains("b"));
    
    /*************************************************************/

    ivSeq.clear();
    ivSeq.add(new GenomicInterval(chromosome, 23, 40, strand));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_NONEMPTY, StrandUsage.YES);
    assertFalse(results.contains("a"));
    assertTrue(results.contains("b"));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_STRICT, StrandUsage.YES);
    assertFalse(results.contains("a"));
    assertFalse(results.contains("b"));
    
    /*************************************************************/

    ivSeq.clear();
    ivSeq.add(new GenomicInterval(chromosome, 5, 23, strand));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_NONEMPTY, StrandUsage.YES);
    assertTrue(results.contains("a"));
    assertFalse(results.contains("b"));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_STRICT, StrandUsage.YES);
    assertFalse(results.contains("a"));
    assertFalse(results.contains("b"));
    
    /*************************************************************/

    ivSeq.clear();
    ivSeq.add(new GenomicInterval(chromosome, 15, 30, strand));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.UNION, StrandUsage.YES);
    assertTrue(results.contains("a"));
    assertTrue(results.contains("b"));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_NONEMPTY, StrandUsage.YES);
    assertFalse(results.contains("a"));
    assertFalse(results.contains("b"));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_STRICT, StrandUsage.YES);
    assertFalse(results.contains("a"));
    assertFalse(results.contains("b"));
    
    /*************************************************************/

    ivSeq.clear();
    ivSeq.add(new GenomicInterval(chromosome, 30, 55, strand));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.UNION, StrandUsage.YES);
    assertFalse(results.contains("a"));
    assertTrue(results.contains("b"));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_NONEMPTY, StrandUsage.YES);
    assertFalse(results.contains("a"));
    assertTrue(results.contains("b"));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_STRICT, StrandUsage.YES);
    assertFalse(results.contains("a"));
    assertFalse(results.contains("b"));
  }

}
