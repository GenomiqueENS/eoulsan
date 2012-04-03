/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.bio.alignmentsfilters;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMParser;
import net.sf.samtools.SAMRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;

/**
 * This class is a JUnit test class to test the class 
 * RemoveMultiMatchesReadAlignments.java.
 * @author Claire Wallon
 */
public class RemoveMultiMatchesReadAlignmentsFilterTest {

  private String recordSE1, recordSE2;
  private String recordPE1, recordPE2, recordPE3, recordPE4; 
  private SAMRecord samRecordSE1, samRecordSE2;
  private SAMRecord samRecordPE1, samRecordPE2, samRecordPE3, samRecordPE4;
  private List<SAMRecord> records;
  private RemoveMultiMatchesReadAlignmentsFilter filter;
  
  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    
    // recordSE1 and recordSE2 have the same read name (two matches for the 
    // same read)
    recordSE1 = "HWI-EAS285_0001_'':1:1:1259:2442#0/1\t16\tchr9\t59513044\t25\t76M\t*\t0\t0" +
            "\tGGGACTGCCTTCANNNAGANNCAGNANCTCCNNNNNNNNNNNNNNGACACCTTCCTGNAACACATGTGCCGCCTGG" +
            "\t#############################################ABE@?E@>CC@C#BBDEDGGGGFFEGEGGGG" +
            "\tXA:i:1\tMD:Z:13T0T0C3C0C3C1G4A0T0G0C0A0G0C0C0A0T0G0G0C0T12G18\tNM:i:22";
    recordSE2 = "HWI-EAS285_0001_'':1:1:1259:2442#0/1\t0\tchr5\t22231887\t55\t76M\t*\t0\t0" +
            "\tCCAGGCGGCACATGTGTTNCAGGAAGGTGTCNNNNNNNNNNNNNNGGAGNTNCTGNNTCTNNNTGAAGGCAGTCCC" +
            "\tGGGGEGEFFGGGGDEDBB#C@CC>@E?@EBA#############################################\t" +
            "XA:i:1\tMD:Z:18C12A0G0C0C0A0T0G0G0C0T0G0C0A0T4C1G3G0G3G0A0A13\tNM:i:22";
    
    // recordPE1, recordPE2, recordPE3 and recordPE4 have the same read name
    // (two matches for the same read)
    // recordPE1 and recordPE2 paired
    recordPE1 = "HWI-1KL110:37:C0BE6ACXX:7:1101:2584:2222\t99\tchr1\t173235257\t255\t101M\t=\t173235280\t124" +
            "\tCTTGTATCGCTCCTCAAACTTGACCTTGGCCTCCCGCCTGGCCTTGCGCTTCAAAGCTGGGTCCCTGAACACATCCTTGTTGACAACAGTCTTGTCCAAGG" +
            "\tCCCFFFFFHHHHHJJJJJJJJJJJJJJJJJJJJJJJJJHIIJIJJIIIIJJJJJIIJHHHHFFFFFDEEEEDDDDDDDDDDDDDDDDDDDDDEDDDDDDDD" +
            "\tXA:i:0\tMD:Z:101\tNM:i:0";
    recordPE2 = "HWI-1KL110:37:C0BE6ACXX:7:1101:2584:2222\t147\tchr1\t173235280\t255\t101M\t=\t173235257\t-124" +
            "\tCCTTGGCCTCCCGCCTGGCCTTGCGCTTCAAAGCTGGGTCCCTGAACACATCCTTGTTGACAACAGTCTTGTCCAAGGGGATATCCACAGAGTACCTTGTG" +
            "\tDDDDDDDDDDDDDDDDDDDDDDDFFHHHHHHJJJJJJJJJHDJJJJJIJIHIJJJJJIIIJJJIJJIJJJJJHJJJJJJJJJJJJJJJHHHHHDFFFFCCC" +
            "\tXA:i:0\tMD:Z:101\tNM:i:0";
    // recordPE3 and recordPE4 paired
    recordPE3 = "HWI-1KL110:37:C0BE6ACXX:7:1101:2584:2222\t99\tchr11\t93898574\t255\t101M\t=\t93898597\t124" +
            "\tCTTGTATCGCTCCTCAAACTTGACCTTGGCCTCCCGCCTGGCCTTGCGCTTCAAAGCTGGGTCCCTGAACACATCCTTGTTGACAACAGTCTTGTCCAAGG" +
            "\tCCCFFFFFHHHHHJJJJJJJJJJJJJJJJJJJJJJJJJHIIJIJJIIIIJJJJJIIJHHHHFFFFFDEEEEDDDDDDDDDDDDDDDDDDDDDEDDDDDDDD" +
            "\tXA:i:0\tMD:Z:101\tNM:i:0";
    recordPE4 = "HWI-1KL110:37:C0BE6ACXX:7:1101:2584:2222\t147\tchr11\t93898597\t255\t101M\t=\t93898574\t-124" +
            "\tCCTTGGCCTCCCGCCTGGCCTTGCGCTTCAAAGCTGGGTCCCTGAACACATCCTTGTTGACAACAGTCTTGTCCAAGGGGATATCCACAGAGTACCTTGTG" +
            "\tDDDDDDDDDDDDDDDDDDDDDDDFFHHHHHHJJJJJJJJJHDJJJJJIJIHIJJJJJIIIJJJIJJIJJJJJHJJJJJJJJJJJJJJJHHHHHDFFFFCCC" +
            "\tXA:i:0\tMD:Z:101\tNM:i:0";
    
    final GenomeDescription desc = new GenomeDescription();
    desc.addSequence("chr1", 197195432);
    desc.addSequence("chr5", 152537259);
    desc.addSequence("chr9", 124076172);
    desc.addSequence("chr11", 121843856);
    
    SAMParser parser = new SAMParser();
    parser.setGenomeDescription(desc);
    
    samRecordSE1 = parser.parseLine(recordSE1);
    samRecordSE2 = parser.parseLine(recordSE2);
    
    samRecordPE1 = parser.parseLine(recordPE1);
    samRecordPE2 = parser.parseLine(recordPE2);
    samRecordPE3 = parser.parseLine(recordPE3);
    samRecordPE4 = parser.parseLine(recordPE4);
    
    records = new ArrayList<SAMRecord>();
    
    filter = new RemoveMultiMatchesReadAlignmentsFilter();
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }
  
  /**
   * Test method for {@link fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.RemoveMultiMatchesReadAlignmentsFilter#getName()}.
   */
  @Test
  public void testGetName() {
    assertEquals("removemultimatches", filter.getName());
  }

  /**
   * Test method for {@link fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.RemoveMultiMatchesReadAlignmentsFilter#getDescription()}.
   */
  @Test
  public void testGetDescription() {
    assertEquals("Remove all the alignments with several matches", 
        filter.getDescription());
  }

  /**
   * Test method for {@link fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.RemoveMultiMatchesReadAlignmentsFilter#filterReadAlignments(java.util.List, boolean)}.
   */
  @Test
  public void testFilterReadAlignments() {
    
    List<SAMRecord> recordsVerif = new ArrayList<SAMRecord>();
    
    // single-end mode
    records.add(samRecordSE1);
    recordsVerif.add(samRecordSE1);
    filter.filterReadAlignments(records);
    assertEquals(records, recordsVerif);
    
    records.add(samRecordSE2);
    recordsVerif.clear();
    filter.filterReadAlignments(records);
    assertEquals(records, recordsVerif);
    
    records.clear();
    recordsVerif.clear();
    
    // paired-end mode
    records.add(samRecordPE1);
    records.add(samRecordPE2);
    recordsVerif.add(samRecordPE1);
    recordsVerif.add(samRecordPE2);
    filter.filterReadAlignments(records);
    assertEquals(records, recordsVerif);
    
    records.add(samRecordPE3);
    records.add(samRecordPE4);
    recordsVerif.clear();
    filter.filterReadAlignments(records);
    assertEquals(records, recordsVerif);
  }
  
  /* ORIGINAL VERSION
  
  @Test
  public void test() {

    final String s1 =
        "AEGIR:25:B0866ABXX:8:1101:1299:2173 1:N:0:CGATGT\t4\t*\t0\t0\t*\t*\t0\t0\t"
            + "ACAAGGGCTTCCGTAAGGCAACATAGAGTATATTGAGTGCTGGTTAGCTGG\t"
            + "BCCFFFFFHHHHHHIJJJJJJJIJJJIIFHIJJJJIJHGIIJJFHIJJJIJ\tXM:i:0";

    final String s2 =
        "AEGIR:25:B0866ABXX:8:1101:7885:2185\t0\tscaffold_9\t1039569\t255\t51M\t*\t0\t0\t"
            + "TGCGTCAAATTAAGCCGCAGGCTCCACTCCTGGTGGTGCCCTTCCGTCAAT\t"
            + "BBCFFFFFBHHHHJJJJJJJJJJJIIJGIIGIIFGHDHGIJJIJJIHIIIJ\t"
            + "XA:i:2\tMD:Z:2A24C23\tNM:i:2";

    final String s3 =
        "AEGIR:25:B0866ABXX:8:1101:13139:2362\t16\tscaffold_1\t870110\t255\t51M\t*\t0\t0\t"
            + "ACTACTACTACTACTACTACTACTACTACTACTACTACTACTACTACTACC\t"
            + "???99B:?<2?<<<CACBA@HBGFBGEJGHGGHEIIHIHHGHHFFFFFCCC\t"
            + "XA:i:0\tMD:Z:51\tNM:i:0";

    final GenomeDescription desc = new GenomeDescription();
    desc.addSequence("scaffold_1", 3756989);
    desc.addSequence("scaffold_9", 1219543);

    SAMParser parser = new SAMParser();
    parser.setGenomeDescription(desc);

    final SAMRecord r1 = parser.parseLine(s1);
    final SAMRecord r2 = parser.parseLine(s2);
    final SAMRecord r3 = parser.parseLine(s3);

    final List<SAMRecord> records = new ArrayList<SAMRecord>();

    ReadAlignmentsFilter filter = new RemoveMultiMatchesReadAlignmentsFilter();

//    try {
//      filter.filterReadAlignments(null);
//      assertTrue(true);
//    } catch (Exception e) {
//      assertTrue(false);
//    }
//
//    records.add(r1);
//    assertEquals(1, records.size());
//    filter.filterReadAlignments(records);
//    assertEquals(1, records.size());
//
//    records.add(r2);
//    records.add(r3);
//    assertEquals(3, records.size());
//    filter.filterReadAlignments(records);
//    assertEquals(0, records.size());
  }
  */

}
