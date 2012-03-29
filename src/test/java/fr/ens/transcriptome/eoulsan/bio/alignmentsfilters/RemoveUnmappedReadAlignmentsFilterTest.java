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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
 * RemoveUnmappedReadAlignmentsFilter.java.
 * @author Claire Wallon
 *
 */
public class RemoveUnmappedReadAlignmentsFilterTest {

  private String recordSE1, recordSE2;
  private String recordPE1, recordPE2, recordPE3, recordPE4; 
  private String recordPE5, recordPE6, recordPE7, recordPE8;
  private SAMRecord samRecordSE1, samRecordSE2;
  private SAMRecord samRecordPE1, samRecordPE2, samRecordPE3, samRecordPE4;
  private SAMRecord samRecordPE5, samRecordPE6, samRecordPE7, samRecordPE8;
  private List<SAMRecord> records;
  private RemoveUnmappedReadAlignmentsFilter filter;
  private boolean pairedEnd;
  
  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    
    // recordSE1 mapped
    recordSE1 = "HWI-EAS285_0001_'':1:1:1260:18686#0/1\t16\tchr4\t129576419\t72\t76M\t*\t0\t0" +
    		"\tGACGGATCCGAGANANTGANNTGANAAGAGGNNNNNNNNNNNNNNAATTTGAGGACCNAAGGGATGCAGATGATGC" +
    		"\tGEGEGE:@=BB><#7#AA:##@CC#CA9A8;##############=GGGGGDCDCDD#DDCGFGDGGGGBGGGGGG" +
    		"\tXA:i:1\tMD:Z:13T1T3C0T3A2C3T0T0T0T0G0G0T0T0T0T0G0T0G0G12C18\tNM:i:21";
    // recordSE2 unmapped
    recordSE2 = "HWI-EAS285_0001_'':1:1:1259:6203#0/1\t4\t*\t0\t0\t*\t*\t0\t0" +
    		"\tCGGCCGGACCGACCCCGTNGGGGTCCGACAANNNNNNNNNNNNNNCACANGNACGNNGCANANCCAACCCGAGCGT" +
    		"\tGGGFGBGGGEGGGGGDD?#A=>B3BA=BD###############################################" +
    		"\tXM:i:0";
    
    // recordPE1 and recordPE2 paired and mapped
    recordPE1 = "HWI-1KL110:37:C0BE6ACXX:7:1101:2584:2222\t99\tchr1\t173235257\t255\t101M\t=\t173235280\t124" +
            "\tCTTGTATCGCTCCTCAAACTTGACCTTGGCCTCCCGCCTGGCCTTGCGCTTCAAAGCTGGGTCCCTGAACACATCCTTGTTGACAACAGTCTTGTCCAAGG" +
            "\tCCCFFFFFHHHHHJJJJJJJJJJJJJJJJJJJJJJJJJHIIJIJJIIIIJJJJJIIJHHHHFFFFFDEEEEDDDDDDDDDDDDDDDDDDDDDEDDDDDDDD" +
            "\tXA:i:0\tMD:Z:101\tNM:i:0";
    recordPE2 = "HWI-1KL110:37:C0BE6ACXX:7:1101:2584:2222\t147\tchr1\t173235280\t255\t101M\t=\t173235257\t-124" +
            "\tCCTTGGCCTCCCGCCTGGCCTTGCGCTTCAAAGCTGGGTCCCTGAACACATCCTTGTTGACAACAGTCTTGTCCAAGGGGATATCCACAGAGTACCTTGTG" +
            "\tDDDDDDDDDDDDDDDDDDDDDDDFFHHHHHHJJJJJJJJJHDJJJJJIJIHIJJJJJIIIJJJIJJIJJJJJHJJJJJJJJJJJJJJJHHHHHDFFFFCCC" +
            "\tXA:i:0\tMD:Z:101\tNM:i:0";
    // recordPE3 and recordPE4 paired and unmapped
    recordPE3 = "HWI-1KL110:37:C0BE6ACXX:7:1101:2740:2239 1:N:0:GCCAAT\t77\t*\t0\t0\t*\t*\t0\t0" +
    		"\tGGTCGATGATCTTCTCTTTGACCTGAGAGATGGTGTCACAGTTGAGGACCTTAACCGGGATGGCATCGATCCCTTCATCCTGAACAATCACGCTCACGGTC" +
    		"\tCCBFFFFFHHHHHJJJJJJJJJJJJJJJJEHIIGHHIJJJJIIIJJJIJJJJIHHHIJJJJJHHHHHFFFFDEEEEDEEDDDDDDDDDDDDDDDDDDDDDB" +
    		"\tXM:i:0";
    recordPE4 = "HWI-1KL110:37:C0BE6ACXX:7:1101:2740:2239 2:N:0:GCCAAT\t141\t*\t0\t0\t*\t*\t0\t0" +
    		"\tAGAAGGCCAAGTACACCCTCAATGACACAGGCCTGCTCGGGGACGATGTTGAGTATGCGCCTCTGACCGTGAGCGTGATTGTTCAGGATGAAGGGATCGAT" +
    		"\tCCCFFFFFHHHHHJIJJJJJJJJJJJJJJJJIJJJJIIJJJJGGIIIIHHHHHHHFFFFDDDDDDDDDDDDBDDDBDDDEDDDDEDDDDDDDDDDDDDDDB" +
    		"\tXM:i:0";
    // recordPE5 and recordPE6 paired, recordPE5 mapped and recordPE6 unmapped
    recordPE5 = "HWI-1KL110:37:C0BE6ACXX:7:1101:1617:2229\t99\tchr4\t129192614\t74\t101M\t=\t129192649\t136" +
        "\tCTTTTTGCCCTCCTGTGGATTCTCCCATCAGCCATTTGGTCTTACTCTTAAGGCCAGTTGAAGATGGTCCCTTACGGTTTCCCAAGTTAGGTTAGTGATGT" +
        "\tCCCFFFFFHHHHHJJHJIJJJIJJJJJJJJJIJJJJJJIIHIJJJJIJJJJJJJJJJIJJJJJIJJJHIJJJJHHEHDFFFEEEEDDDEDDCCDDCDEEDC" +
        "\tXA:i:1\tMD:Z:5C95\tNM:i:1";
    recordPE6 = "HWI-1KL110:37:C0BE6ACXX:7:1101:1617:2229\t141\t*\t0\t0\t*\t*\t0\t0" +
        "\tTTGGTCTTACTCTTAAGGCCAGTTGAAGATGGTCCCTTACGGTTTCCCAAGTTAGGTTAGTGATGTGAGATGCCCTGTCCCTACCTCCTTCCCGAGCCCCG" +
        "\tDDDDDCDCADDDDDDDDDDDDDDEEDDDDDDDDDDDDDBDEEFFFFHHHHEHIEJJJIGJIIGGIIJIJJIIJJJJIHJHGD?JIHJJHFHHGFFFFFCCC" +
        "\tXA:i:2\tMD:Z:87T12A0\tNM:i:2";
    // recordPE7 and recordPE8 paired, recordPE7 unmapped and recordPE8 mapped
    recordPE7 = "HWI-1KL110:37:C0BE6ACXX:7:1101:1802:2241\t77\t*\t0\t0\t*\t*\t0\t0" +
        "\tCCAGCCTTAGCGCCTGGTGCCTCCATCATGGCTAAAGCATGGGCCGTGGGGACACTGACGACTAGAAGGGAAGGGGCAAGGGAAATTCCAAGCAGTACATT" +
        "\tCCCFFFFFHHHHHJJJJFHHJJJJJJJJJJJJJJJJHIJJJJIJJJFHJJJHHHFFFFFDDDDDDDDDDDDDDDB>BDDDDDDDDDDDDEDDDDDCCCDED" +
        "\tXA:i:0\tMD:Z:101\tNM:i:0";
    recordPE8 = "HWI-1KL110:37:C0BE6ACXX:7:1101:1802:2241\t147\tchr15\t74592729\t212\t101M\t=\t74592687\t-143" +
        "\tGCCGTGGGGACACTGACGACTAGAAGGGAAGGGGCAAGGGAAATTCCAAGCAGTACATTACAGGCAGAGAGCCATAACAGTGAGCAGGCTGAGGCTCGTTG" +
        "\tDDDDDDDDEDDDDDDDDEEEEFFFFFHHHHHJJJJJJJJJJJJJJJJJJHHJIIFIGIIHIJJJJJIHIJJJJJJJIJJJJJJJJJJJHHHHHFFFFFCCC" +
        "\tXA:i:0\tMD:Z:101\tNM:i:0";
    
    final GenomeDescription desc = new GenomeDescription();
    desc.addSequence("chr1", 197195432);
    desc.addSequence("chr4", 155630120);
    desc.addSequence("chr9", 124076172);
    desc.addSequence("chr11", 121843856);
    desc.addSequence("chr15", 103494974);
    
    SAMParser parser = new SAMParser();
    parser.setGenomeDescription(desc);
    
    samRecordSE1 = parser.parseLine(recordSE1);
    samRecordSE2 = parser.parseLine(recordSE2);
    
    samRecordPE1 = parser.parseLine(recordPE1);
    samRecordPE2 = parser.parseLine(recordPE2);
    samRecordPE3 = parser.parseLine(recordPE3);
    samRecordPE4 = parser.parseLine(recordPE4);
    samRecordPE5 = parser.parseLine(recordPE5);
    samRecordPE6 = parser.parseLine(recordPE6);
    samRecordPE7 = parser.parseLine(recordPE7);
    samRecordPE8 = parser.parseLine(recordPE8);
    
    records = new ArrayList<SAMRecord>();
    
    filter = new RemoveUnmappedReadAlignmentsFilter();
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }
  
  /**
   * Test method for {@link fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.RemoveUnmappedReadAlignmentsFilter#getName()}.
   */
  @Test
  public void testGetName() {
    assertEquals("removeunmapped", filter.getName());
  }

  /**
   * Test method for {@link fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.RemoveUnmappedReadAlignmentsFilter#getDescription()}.
   */
  @Test
  public void testGetDescription() {
    assertEquals("Remove all the unmapped alignments", filter.getDescription());
  }

  /**
   * Test method for {@link fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.RemoveUnmappedReadAlignmentsFilter#filterReadAlignments(java.util.List, boolean)}.
   */
  @Test
  public void testFilterReadAlignments() {
    
    List<SAMRecord> recordsVerif = new ArrayList<SAMRecord>();
    
    // single-end mode
    pairedEnd = false;
    
    records.add(samRecordSE1);
    records.add(samRecordSE2);
    recordsVerif.add(samRecordSE1);
    filter.filterReadAlignments(records, pairedEnd);
    assertEquals(records, recordsVerif);
    
    records.clear();
    recordsVerif.clear();
    
    // paired-end mode
    pairedEnd = true;
    
    records.add(samRecordPE1);
    records.add(samRecordPE2);
    records.add(samRecordPE3);
    records.add(samRecordPE4);
    records.add(samRecordPE5);
    records.add(samRecordPE6);
    records.add(samRecordPE7);
    records.add(samRecordPE8);
    recordsVerif.add(samRecordPE1);
    recordsVerif.add(samRecordPE2);
    filter.filterReadAlignments(records, pairedEnd);
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

    ReadAlignmentsFilter filter = new RemoveUnmappedReadAlignmentsFilter();

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
//    assertEquals(0, records.size());
//
//    records.add(r2);
//    records.add(r3);
//    assertEquals(2, records.size());
//    filter.filterReadAlignments(records);
//    assertEquals(2, records.size());
//
//    records.add(r1);
//    assertEquals(3, records.size());
//    filter.filterReadAlignments(records);
//    assertEquals(2, records.size());
//    assertFalse(records.contains(r1));

  }
  */

}
