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

package fr.ens.transcriptome.eoulsan.bio.alignmentsfilters;

import static org.junit.Assert.*;

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
 * ReadAlignmentsFilterBuffer.java. 
 * @author Claire Wallon
 */
public class ReadAlignmentsFilterBufferTest {
  
  private String recordSE1, recordSE2, recordSE3, recordSE4, recordSE5;
  private String recordSE6, recordSE7;
  private String recordPE1, recordPE2, recordPE3, recordPE4; 
  private String recordPE5, recordPE6, recordPE7, recordPE8;
  private String recordPE9, recordPE10;
  private SAMRecord samRecordSE1, samRecordSE2;
  private SAMRecord samRecordSE3, samRecordSE4, samRecordSE5;
  private SAMRecord samRecordSE6, samRecordSE7;
  private SAMRecord samRecordPE1, samRecordPE2, samRecordPE3, samRecordPE4;
  private SAMRecord samRecordPE5, samRecordPE6, samRecordPE7, samRecordPE8;
  private SAMRecord samRecordPE9, samRecordPE10;
  private List<SAMRecord> recordsVerif;
  private QualityReadAlignmentsFilter filter;
  private ReadAlignmentsFilterBuffer rafb; 
  
  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    
    // all records (single-end and paired-end modes) have a good mapping
    // quality score (above the threshold) 
    
    recordSE1 = "HWI-EAS285_0001_'':1:1:1260:18686#0/1\t16\tchr4\t129576419\t72\t76M\t*\t0\t0" +
    		"\tGACGGATCCGAGANANTGANNTGANAAGAGGNNNNNNNNNNNNNNAATTTGAGGACCNAAGGGATGCAGATGATGC" +
    		"\tGEGEGE:@=BB><#7#AA:##@CC#CA9A8;##############=GGGGGDCDCDD#DDCGFGDGGGGBGGGGGG" +
    		"\tXA:i:1\tMD:Z:13T1T3C0T3A2C3T0T0T0T0G0G0T0T0T0T0G0T0G0G12C18\tNM:i:21";
    recordSE2 = "HWI-EAS285_0001_'':1:1:1260:13682#0/1\t0\tchr16\t23360177\t55\t76M\t*\t0\t0\t" +
            "ATTTGCGACAGGTAGTTTNAAATCTGTGACTNNNNNNNNNNNNNNAGTGNCNTTCNNCGTNGNCACTGACGTCACT" +
            "\tGGGGFGGGGFCECEEBCB#ACCCCCGGFGGA##############AA=A#A#A?A##A=?#9#8?CCB>CGEGGGA\tXA:i:1\tMD:Z:18A12G0A0G0T0G0C0T0A0T0A0G0G0A0A4T1T3T0A3G1G13\tNM:i:21";
    // recordSE3, recordSE4 and recordSE5 are various matches of the same read
    recordSE3 = "HWI-EAS285_0001_'':1:1:1259:2442#0/1\t16\tchr9\t59513044\t50\t76M\t*\t0\t0" +
    		"\tGGGACTGCCTTCANNNAGANNCAGNANCTCCNNNNNNNNNNNNNNGACACCTTCCTGNAACACATGTGCCGCCTGG" +
    		"\t#############################################ABE@?E@>CC@C#BBDEDGGGGFFEGEGGGG" +
    		"\tXA:i:1\tMD:Z:13T0T0C3C0C3C1G4A0T0G0C0A0G0C0C0A0T0G0G0C0T12G18\tNM:i:22";
    recordSE4 = "HWI-EAS285_0001_'':1:1:1259:2442#0/1\t0\tchr5\t22231887\t55\t76M\t*\t0\t0" +
    		"\tCCAGGCGGCACATGTGTTNCAGGAAGGTGTCNNNNNNNNNNNNNNGGAGNTNCTGNNTCTNNNTGAAGGCAGTCCC" +
    		"\tGGGGEGEFFGGGGDEDBB#C@CC>@E?@EBA#############################################" +
    		"\tXA:i:1\tMD:Z:18C12A0G0C0C0A0T0G0G0C0T0G0C0A0T4C1G3G0G3G0A0A13" +
    		"\tNM:i:22";
    recordSE5 = "HWI-EAS285_0001_'':1:1:1259:2442#0/1\t0\tchr13\t14002582\t72\t76M\t*\t0\t0" +
    		"\tCCAGGCGGCACATGTGTTNCAGGAAGGTGTCNNNNNNNNNNNNNNGGAGNTNCTGNNTCTNNNTGAAGGCAGTCCC" +
    		"\tGGGGEGEFFGGGGDEDBB#C@CC>@E?@EBA#############################################" +
    		"\tXA:i:2\tMD:Z:18C3A8A0G0C0C0A0T0T0G0C0T0C0C0A0T4C1G2A0G0G3G0A0A5A6A0\tNM:i:26";
    // recordSE6 and recordSE7 are various matches of the same read
    recordSE6 = "HWI-EAS285_0001_'':1:1:1259:1873#0/1\t16\tchr2\t28011331\t255\t76M\t*\t0\t0" +
    		"\tGTCTGGCTCCGACNCNCAGGNACCNCNGCCCNNNNNNNNNNNNNNAAGAGCCAGTTCNGGGGTCCCTGGGCCACAC" +
    		"\t##############################################EEE:E=<?5=?#BAAF=AFFEFFFDE?EEE" +
    		"\tXA:i:1\tMD:Z:0C2A3A0G0T3T1T4C3A1A1A2G0C0T0G0G0G0T0A0C0T0G0A0A0G12T18\tNM:i:26";
    recordSE7 = "HWI-EAS285_0001_'':1:1:1259:1873#0/1\t16\tchr12\t56412446\t255\t76M\t*\t0\t0" +
    		"\tGTCTGGCTCCGACNCNCAGGNACCNCNGCCCNNNNNNNNNNNNNNAAGAGCCAGTTCNGGGGTCCCTGGGCCACAC" +
    		"\t##############################################EEE:E=<?5=?#BAAF=AFFEFFFDE?EEE" +
    		"\tXA:i:1\tMD:Z:13T1T4T3A1A1A2A0C0T0G0G0G0T0A0C0C0A0C0A0G12T18\tNM:i:21";
    
    // recordPE1 and recordPE2 paired
    recordPE1 = "HWI-1KL110:37:C0BE6ACXX:7:1101:2462:2222\t99\tchr13\t34124505\t75\t101M\t=\t34124588\t184" +
    		"\tCTGANAGAGAAAGTTTACCAAATGCTTCAGAAGTGGCTGATGCGGGAAGGCACCAAAGGGGCCACAGTGGGAAAGTTGGCCCAGGCACTTCACCAATGTTG" +
    		"\tCCCF#2ADHHGHHIIJJJJJJJJJJJJJJIJJIGIIJJJGGIDHIJGIIIIIJJIJH?CHFFDDDEDCDDDBDDBDCDDDDDD?BDDDDDDDDDDDDDDED" +
    		"\tXA:i:1\tMD:Z:4A96\tNM:i:1";
    recordPE2 = "HWI-1KL110:37:C0BE6ACXX:7:1101:2462:2222\t147\tchr13\t34124588\t145\t101M\t=\t34124505\t-184" +
    		"\tGGCACTTCACCAATGTTGCAGGATAGACCTGCTGAACCACTTGATTCGTGCCAGCCAGAGCTAAGCCTGGGCAGGCTCTGGCAGTGGGAAGCAAACTATTT" +
    		"\tDDDCDDDDDDDDDDDDDDDEEDEEDEBFFFFHGHHHHJIJJJJJJJJJJIGJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJHHHHHFFFFFCCC" +
    		"\tXA:i:0\tMD:Z:101\tNM:i:0";
    // recordPE3, recordPE4, recordPE5 and recordPE6 are various matches of 
    // the same read
    // recordPE3 and recordPE4 paired
    recordPE3 = "HWI-1KL110:37:C0BE6ACXX:7:1101:1668:2230\t99\tchr12\t76732159\t128\t101M\t=\t76732246\t188" +
            "\tCTTTCCATTCAGCTCACTGATGACCTTGTTGAGCCGATCATCGACCGCTTCGATGCCCACGCTGTCTAGTATTTTCTTGATGTCTTTGGCGCTAGGAGAGG" +
            "\tCCCFFFFFHHHHHJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJ@GIIIJJJJJJJHHHHHFFDDDDDEDDDFFFEDDDDDDDEDEDDDDDDDDDDDDDBDD\tXA:i:0\tMD:Z:43T20C36\tNM:i:2";
    recordPE4 = "HWI-1KL110:37:C0BE6ACXX:7:1101:1668:2230\t147\tchr12\t76732246\t60\t101M\t=\t76732159\t-188" +
            "\tGGCGCTAGGAGAGGAGTTGCCCCCGAGGGCGGCCAGCAGGTAAGAGGCGACGTAGCGCATGTCGGCTGCGGGGGACAGACCTCACGCGTGCGACCTCGGCG" +
            "\tBDBDDDCDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDBDDDDBDDDDDDDFFFHHJJJJJJJJJHFJJIHGJJJJHHHHHFFFFFCCC\tXA:i:1\tMD:Z:100A0\tNM:i:1";
    // recordPE5 and recordPE6 paired
    recordPE5 = "HWI-1KL110:37:C0BE6ACXX:7:1101:1668:2230\t99\tchr4\t129192614\t74\t101M\t=\t129192649\t136" +
            "\tCTTTTTGCCCTCCTGTGGATTCTCCCATCAGCCATTTGGTCTTACTCTTAAGGCCAGTTGAAGATGGTCCCTTACGGTTTCCCAAGTTAGGTTAGTGATGT" +
            "\tCCCFFFFFHHHHHJJHJIJJJIJJJJJJJJJIJJJJJJIIHIJJJJIJJJJJJJJJJIJJJJJIJJJHIJJJJHHEHDFFFEEEEDDDEDDCCDDCDEEDC\tXA:i:1\tMD:Z:5C95\tNM:i:1";
    recordPE6 = "HWI-1KL110:37:C0BE6ACXX:7:1101:1668:2230\t147\tchr4\t129192649\t230\t101M\t=\t129192614\t-136" +
            "\tTTGGTCTTACTCTTAAGGCCAGTTGAAGATGGTCCCTTACGGTTTCCCAAGTTAGGTTAGTGATGTGAGATGCCCTGTCCCTACCTCCTTCCCGAGCCCCG" +
            "\tDDDDDCDCADDDDDDDDDDDDDDEEDDDDDDDDDDDDDBDEEFFFFHHHHEHIEJJJIGJIIGGIIJIJJIIJJJJIHJHGD?JIHJJHFHHGFFFFFCCC\tXA:i:2\tMD:Z:87T12A0\tNM:i:2";
    
    // recordPE7 and recordPE8 paired
    recordPE7 = "HWI-1KL110:37:C0BE6ACXX:7:1101:2584:2222\t99\tchr1\t173235257\t255\t101M\t=\t173235280\t124" +
    		"\tCTTGTATCGCTCCTCAAACTTGACCTTGGCCTCCCGCCTGGCCTTGCGCTTCAAAGCTGGGTCCCTGAACACATCCTTGTTGACAACAGTCTTGTCCAAGG" +
    		"\tCCCFFFFFHHHHHJJJJJJJJJJJJJJJJJJJJJJJJJHIIJIJJIIIIJJJJJIIJHHHHFFFFFDEEEEDDDDDDDDDDDDDDDDDDDDDEDDDDDDDD" +
    		"\tXA:i:0\tMD:Z:101\tNM:i:0";
    recordPE8 = "HWI-1KL110:37:C0BE6ACXX:7:1101:2584:2222\t147\tchr1\t173235280\t255\t101M\t=\t173235257\t-124" +
    		"\tCCTTGGCCTCCCGCCTGGCCTTGCGCTTCAAAGCTGGGTCCCTGAACACATCCTTGTTGACAACAGTCTTGTCCAAGGGGATATCCACAGAGTACCTTGTG" +
    		"\tDDDDDDDDDDDDDDDDDDDDDDDFFHHHHHHJJJJJJJJJHDJJJJJIJIHIJJJJJIIIJJJIJJIJJJJJHJJJJJJJJJJJJJJJHHHHHDFFFFCCC" +
    		"\tXA:i:0\tMD:Z:101\tNM:i:0";
    recordPE9 = "HWI-1KL110:37:C0BE6ACXX:7:1101:2584:2222\t99\tchr11\t93898574\t255\t101M\t=\t93898597\t124" +
    		"\tCTTGTATCGCTCCTCAAACTTGACCTTGGCCTCCCGCCTGGCCTTGCGCTTCAAAGCTGGGTCCCTGAACACATCCTTGTTGACAACAGTCTTGTCCAAGG" +
    		"\tCCCFFFFFHHHHHJJJJJJJJJJJJJJJJJJJJJJJJJHIIJIJJIIIIJJJJJIIJHHHHFFFFFDEEEEDDDDDDDDDDDDDDDDDDDDDEDDDDDDDD" +
    		"\tXA:i:0\tMD:Z:101\tNM:i:0";
    recordPE10 = "HWI-1KL110:37:C0BE6ACXX:7:1101:2584:2222\t147\tchr11\t93898597\t255\t101M\t=\t93898574\t-124" +
    		"\tCCTTGGCCTCCCGCCTGGCCTTGCGCTTCAAAGCTGGGTCCCTGAACACATCCTTGTTGACAACAGTCTTGTCCAAGGGGATATCCACAGAGTACCTTGTG" +
    		"\tDDDDDDDDDDDDDDDDDDDDDDDFFHHHHHHJJJJJJJJJHDJJJJJIJIHIJJJJJIIIJJJIJJIJJJJJHJJJJJJJJJJJJJJJHHHHHDFFFFCCC" +
    		"\tXA:i:0\tMD:Z:101\tNM:i:0";
    
    final GenomeDescription desc = new GenomeDescription();
    desc.addSequence("chr1", 197195432);
    desc.addSequence("chr2", 181748087);
    desc.addSequence("chr4", 155630120);
    desc.addSequence("chr5", 152537259);
    desc.addSequence("chr9", 124076172);
    desc.addSequence("chr11", 121843856);
    desc.addSequence("chr12", 121257530);
    desc.addSequence("chr13", 120284312);
//    desc.addSequence("chr15", 103494974);
    desc.addSequence("chr16", 98319150);
    
    SAMParser parser = new SAMParser();
    parser.setGenomeDescription(desc);
    
    samRecordSE1 = parser.parseLine(recordSE1);
    samRecordSE2 = parser.parseLine(recordSE2);
    samRecordSE3 = parser.parseLine(recordSE3);
    samRecordSE4 = parser.parseLine(recordSE4);
    samRecordSE5 = parser.parseLine(recordSE5);
    samRecordSE6 = parser.parseLine(recordSE6);
    samRecordSE7 = parser.parseLine(recordSE7);
    
    samRecordPE1 = parser.parseLine(recordPE1);
    samRecordPE2 = parser.parseLine(recordPE2);
    samRecordPE3 = parser.parseLine(recordPE3);
    samRecordPE4 = parser.parseLine(recordPE4);
    samRecordPE5 = parser.parseLine(recordPE5);
    samRecordPE6 = parser.parseLine(recordPE6);
    samRecordPE7 = parser.parseLine(recordPE7);
    samRecordPE8 = parser.parseLine(recordPE8);
    samRecordPE9 = parser.parseLine(recordPE9);
    samRecordPE10 = parser.parseLine(recordPE10);
    
    recordsVerif = new ArrayList<SAMRecord>();
    
    filter = new QualityReadAlignmentsFilter();
    filter.setParameter("threshold", "50");
    rafb = new ReadAlignmentsFilterBuffer(filter);
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }

  /**
   * Test method for {@link fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilterBuffer#addAlignment(net.sf.samtools.SAMRecord)}
   * and {@link fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilterBuffer#getFilteredAlignments(boolean)}.
   */
  @Test
  public void testAddAlignmentAndGetFilteredAlignments() {
    
    // single-end mode
    
    // first case
    assertTrue(rafb.addAlignment(samRecordSE1));
    assertFalse(rafb.addAlignment(samRecordSE3));
    recordsVerif.add(samRecordSE1);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
    assertTrue(rafb.addAlignment(samRecordSE3));
    assertTrue(rafb.addAlignment(samRecordSE4));
    assertTrue(rafb.addAlignment(samRecordSE5));
    assertFalse(rafb.addAlignment(samRecordSE2));
    recordsVerif.clear();
    recordsVerif.add(samRecordSE3);
    recordsVerif.add(samRecordSE4);
    recordsVerif.add(samRecordSE5);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
    assertTrue(rafb.addAlignment(samRecordSE2));
//    rafb.checkBuffer();
    recordsVerif.clear();
    recordsVerif.add(samRecordSE2);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
    
    recordsVerif.clear();
    
    // second case
    assertTrue(rafb.addAlignment(samRecordSE3));
    assertTrue(rafb.addAlignment(samRecordSE4));
    assertFalse(rafb.addAlignment(samRecordSE1));
    recordsVerif.add(samRecordSE3);
    recordsVerif.add(samRecordSE4);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
    assertTrue(rafb.addAlignment(samRecordSE1));
    assertFalse(rafb.addAlignment(samRecordSE5));
    recordsVerif.clear();
    recordsVerif.add(samRecordSE1);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
    assertTrue(rafb.addAlignment(samRecordSE5));
    assertTrue(rafb.addAlignment(samRecordSE3));
//    rafb.checkBuffer();
    recordsVerif.clear();
    recordsVerif.add(samRecordSE5);
    recordsVerif.add(samRecordSE3);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
    
    recordsVerif.clear();
    
    // third case
    assertTrue(rafb.addAlignment(samRecordSE1));
    assertFalse(rafb.addAlignment(samRecordSE2));
    recordsVerif.add(samRecordSE1);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
    assertTrue(rafb.addAlignment(samRecordSE2));
//    rafb.checkBuffer();
    recordsVerif.clear();
    recordsVerif.add(samRecordSE2);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
    
    recordsVerif.clear();
    
    // fourth case
    assertTrue(rafb.addAlignment(samRecordSE3));
    assertTrue(rafb.addAlignment(samRecordSE4));
    assertFalse(rafb.addAlignment(samRecordSE6));
    recordsVerif.add(samRecordSE3);
    recordsVerif.add(samRecordSE4);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
    assertTrue(rafb.addAlignment(samRecordSE6));
    assertTrue(rafb.addAlignment(samRecordSE7));
//    rafb.checkBuffer();
    recordsVerif.clear();
    recordsVerif.add(samRecordSE6);
    recordsVerif.add(samRecordSE7);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
    
    recordsVerif.clear();
    
    // paired-end mode
    
    // first case
    assertTrue(rafb.addAlignment(samRecordPE1));
    assertTrue(rafb.addAlignment(samRecordPE2));
    assertFalse(rafb.addAlignment(samRecordPE3));
    recordsVerif.add(samRecordPE1);
    recordsVerif.add(samRecordPE2);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
    assertTrue(rafb.addAlignment(samRecordPE3));
    assertTrue(rafb.addAlignment(samRecordPE4));
    assertTrue(rafb.addAlignment(samRecordPE5));
    assertTrue(rafb.addAlignment(samRecordPE6));
    assertFalse(rafb.addAlignment(samRecordPE1));
    recordsVerif.clear();
    recordsVerif.add(samRecordPE3);
    recordsVerif.add(samRecordPE4);
    recordsVerif.add(samRecordPE5);
    recordsVerif.add(samRecordPE6);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
    assertTrue(rafb.addAlignment(samRecordPE1));
    assertTrue(rafb.addAlignment(samRecordPE2));
//    rafb.checkBuffer();
    recordsVerif.clear();
    recordsVerif.add(samRecordPE1);
    recordsVerif.add(samRecordPE2);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
    
    recordsVerif.clear();
    
    // second case
    assertTrue(rafb.addAlignment(samRecordPE3));
    assertTrue(rafb.addAlignment(samRecordPE4));
    assertTrue(rafb.addAlignment(samRecordPE5));
    assertTrue(rafb.addAlignment(samRecordPE6));
    assertFalse(rafb.addAlignment(samRecordPE1));
    recordsVerif.add(samRecordPE3);
    recordsVerif.add(samRecordPE4);
    recordsVerif.add(samRecordPE5);
    recordsVerif.add(samRecordPE6);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
    assertTrue(rafb.addAlignment(samRecordPE1));
    assertTrue(rafb.addAlignment(samRecordPE2));
    assertFalse(rafb.addAlignment(samRecordPE3));
    recordsVerif.clear();
    recordsVerif.add(samRecordPE1);
    recordsVerif.add(samRecordPE2);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
    assertTrue(rafb.addAlignment(samRecordPE3));
    assertTrue(rafb.addAlignment(samRecordPE4));
    assertTrue(rafb.addAlignment(samRecordPE5));
    assertTrue(rafb.addAlignment(samRecordPE6));
//    rafb.checkBuffer();
    recordsVerif.clear();
    recordsVerif.add(samRecordPE3);
    recordsVerif.add(samRecordPE4);
    recordsVerif.add(samRecordPE5);
    recordsVerif.add(samRecordPE6);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
    
    recordsVerif.clear();
    
    // third case
    assertTrue(rafb.addAlignment(samRecordPE1));
    assertTrue(rafb.addAlignment(samRecordPE2));
    assertFalse(rafb.addAlignment(samRecordPE3));
    recordsVerif.add(samRecordPE1);
    recordsVerif.add(samRecordPE2);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
    assertTrue(rafb.addAlignment(samRecordPE3));
    assertTrue(rafb.addAlignment(samRecordPE4));
//    rafb.checkBuffer();
    recordsVerif.clear();
    recordsVerif.add(samRecordPE3);
    recordsVerif.add(samRecordPE4);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
    
    recordsVerif.clear();
    
    // fourth case
    assertTrue(rafb.addAlignment(samRecordPE3));
    assertTrue(rafb.addAlignment(samRecordPE4));
    assertTrue(rafb.addAlignment(samRecordPE5));
    assertTrue(rafb.addAlignment(samRecordPE6));
    assertFalse(rafb.addAlignment(samRecordPE7));
    recordsVerif.add(samRecordPE3);
    recordsVerif.add(samRecordPE4);
    recordsVerif.add(samRecordPE5);
    recordsVerif.add(samRecordPE6);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
    assertTrue(rafb.addAlignment(samRecordPE7));
    assertTrue(rafb.addAlignment(samRecordPE8));
    assertTrue(rafb.addAlignment(samRecordPE9));
    assertTrue(rafb.addAlignment(samRecordPE10));
//    rafb.checkBuffer();
    recordsVerif.clear();
    recordsVerif.add(samRecordPE7);
    recordsVerif.add(samRecordPE8);
    recordsVerif.add(samRecordPE9);
    recordsVerif.add(samRecordPE10);
    assertEquals(recordsVerif, rafb.getFilteredAlignments());
  }

  /**
   * Test method for {@link fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilterBuffer#ReadAlignmentsFilterBuffer(fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilter)}.
   */
  @Test
  public void testReadAlignmentsFilterBufferReadAlignmentsFilter() {
//    fail("Not yet implemented");
  }

  /**
   * Test method for {@link fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilterBuffer#ReadAlignmentsFilterBuffer(fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilter, boolean)}.
   */
  @Test
  public void testReadAlignmentsFilterBufferReadAlignmentsFilterBoolean() {
//    fail("Not yet implemented");
  }

}
