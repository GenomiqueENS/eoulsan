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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMLineParser;
import net.sf.samtools.SAMRecord;

import org.junit.Before;
import org.junit.Test;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.SAMUtils;

/**
 * This class is a JUnit test class to test the class
 * QualityReadAlignmentsFilter.java.
 * @author Claire Wallon
 */
public class QualityReadAlignmentsFilterTest {

  private String recordSE1, recordSE2;
  private String recordPE1, recordPE2, recordPE3, recordPE4;
  private String recordPE5, recordPE6, recordPE7, recordPE8;
  private SAMRecord samRecordSE1, samRecordSE2;
  private SAMRecord samRecordPE1, samRecordPE2, samRecordPE3, samRecordPE4;
  private SAMRecord samRecordPE5, samRecordPE6, samRecordPE7, samRecordPE8;
  private List<SAMRecord> records;
  private QualityReadAlignmentsFilter filter;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {

    // recordSE1 quality score: 0
    recordSE1 =
        "HWI-EAS285_0001_'':1:1:1259:6203#0/1\t4\t*\t0\t0\t*\t*\t0\t0\t"
            + "CGGCCGGACCGACCCCGTNGGGGTCCGACAANNNNNNNNNNNNNNCACANGNACGNNGCANANCCAACCCGAGCGT"
            + "\tGGGFGBGGGEGGGGGDD?#A=>B3BA=BD###############################################\tXM:i:0";
    // recordSE2 quality score: 55
    recordSE2 =
        "HWI-EAS285_0001_'':1:1:1260:13682#0/1\t0\tchr16\t"
            + "23360177\t55\t76M\t*\t0\t0\t"
            + "ATTTGCGACAGGTAGTTTNAAATCTGTGACTNNNNNNNNNNNNNNAGTGNCNTTCNNCGTNGNCACTGACGTCACT"
            + "\tGGGGFGGGGFCECEEBCB#ACCCCCGGFGGA##############AA=A#A#A?A##A=?#9#8?CCB>CGEGGGA\tXA:i:1\tMD:Z:18A12G0A0G0T0G0C0T0A0T0A0G0G0A0A4T1T3T0A3G1G13\tNM:i:21";

    // recordPE1 and recordPE2 paired
    // recordPE1 quality score: 0
    recordPE1 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:1379:2189 1:Y:0:GCCAAT\t77\t*\t0\t0\t*\t*\t0\t0"
            + "\tGTTTNNNNNNNTNNNNNNTCAAACACNNNNNNNNTATAAATTTCAAAAAATGTTGCATATACTTGAAGTTCTAGNTTNNNNNNNNNNNNNTNCTTCAGCAT"
            + "\t<<<@#######4######32@@@?@@########00<=????????????????????@@@@@??????????############################\tXM:i:0";
    // recordPE2 quality score: 0
    recordPE2 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:1379:2189 2:Y:0:GCCAAT\t141\t*\t0\t0\t*\t*\t0\t0"
            + "\tNNGGAAAGANNNNNGANNGNNNNNNNNNNATTTATGCANNAGNNGCTCGCGATGTNTNTTTNNNNNNNAAGAAGCCAAACTATAAGAAACTAGAACTTCAA"
            + "\t##1=DDFFH#####33##3##########12BGHIJJJ##07##.7BGEGEEFFD#,#,;?#######,,8?BDDDDDDDDDDDDDDDDDDDDDDDDDDDD\tXM:i:0";

    // recordPE3 and recordPE4 paired
    // recordPE3 quality score: 12
    recordPE3 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:1668:2230\t99\tchr12\t76732159\t12\t101M\t=\t76732246\t188"
            + "\tCTTTCCATTCAGCTCACTGATGACCTTGTTGAGCCGATCATCGACCGCTTCGATGCCCACGCTGTCTAGTATTTTCTTGATGTCTTTGGCGCTAGGAGAGG"
            + "\tCCCFFFFFHHHHHJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJ@GIIIJJJJJJJHHHHHFFDDDDDEDDDFFFEDDDDDDDEDEDDDDDDDDDDDDDBDD\tXA:i:0\tMD:Z:43T20C36\tNM:i:2";
    // recordPE4 quality score: 60
    recordPE4 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:1668:2230\t147\tchr12\t76732246\t60\t101M\t=\t76732159\t-188"
            + "\tGGCGCTAGGAGAGGAGTTGCCCCCGAGGGCGGCCAGCAGGTAAGAGGCGACGTAGCGCATGTCGGCTGCGGGGGACAGACCTCACGCGTGCGACCTCGGCG"
            + "\tBDBDDDCDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDBDDDDBDDDDDDDFFFHHJJJJJJJJJHFJJIHGJJJJHHHHHFFFFFCCC\tXA:i:1\tMD:Z:100A0\tNM:i:1";

    // recordPE5 and recordPE6 paired
    // recordPE5 quality score: 74
    recordPE5 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:1617:2229\t99\tchr4\t129192614\t74\t101M\t=\t129192649\t136"
            + "\tCTTTTTGCCCTCCTGTGGATTCTCCCATCAGCCATTTGGTCTTACTCTTAAGGCCAGTTGAAGATGGTCCCTTACGGTTTCCCAAGTTAGGTTAGTGATGT"
            + "\tCCCFFFFFHHHHHJJHJIJJJIJJJJJJJJJIJJJJJJIIHIJJJJIJJJJJJJJJJIJJJJJIJJJHIJJJJHHEHDFFFEEEEDDDEDDCCDDCDEEDC\tXA:i:1\tMD:Z:5C95\tNM:i:1";
    // recordPE6 quality score: 43
    recordPE6 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:1617:2229\t147\tchr4\t129192649\t43\t101M\t=\t129192614\t-136"
            + "\tTTGGTCTTACTCTTAAGGCCAGTTGAAGATGGTCCCTTACGGTTTCCCAAGTTAGGTTAGTGATGTGAGATGCCCTGTCCCTACCTCCTTCCCGAGCCCCG"
            + "\tDDDDDCDCADDDDDDDDDDDDDDEEDDDDDDDDDDDDDBDEEFFFFHHHHEHIEJJJIGJIIGGIIJIJJIIJJJJIHJHGD?JIHJJHFHHGFFFFFCCC\tXA:i:2\tMD:Z:87T12A0\tNM:i:2";

    // recordPE7 and recordPE8 paired
    // recordPE7 quality score: 89
    recordPE7 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:1802:2241\t99\tchr15\t74592687\t89\t101M\t=\t74592729\t143"
            + "\tCCAGCCTTAGCGCCTGGTGCCTCCATCATGGCTAAAGCATGGGCCGTGGGGACACTGACGACTAGAAGGGAAGGGGCAAGGGAAATTCCAAGCAGTACATT"
            + "\tCCCFFFFFHHHHHJJJJFHHJJJJJJJJJJJJJJJJHIJJJJIJJJFHJJJHHHFFFFFDDDDDDDDDDDDDDDB>BDDDDDDDDDDDDEDDDDDCCCDED\tXA:i:0\tMD:Z:101\tNM:i:0";
    // recordPE8 quality score: 212
    recordPE8 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:1802:2241\t147\tchr15\t74592729\t212\t101M\t=\t74592687\t-143"
            + "\tGCCGTGGGGACACTGACGACTAGAAGGGAAGGGGCAAGGGAAATTCCAAGCAGTACATTACAGGCAGAGAGCCATAACAGTGAGCAGGCTGAGGCTCGTTG"
            + "\tDDDDDDDDEDDDDDDDDEEEEFFFFFHHHHHJJJJJJJJJJJJJJJJJJHHJIIFIGIIHIJJJJJIHIJJJJJJJIJJJJJJJJJJJHHHHHFFFFFCCC\tXA:i:0\tMD:Z:101\tNM:i:0";

    final GenomeDescription desc = new GenomeDescription();
    desc.addSequence("chr4", 155630120);
    desc.addSequence("chr12", 121257530);
    desc.addSequence("chr15", 103494974);
    desc.addSequence("chr16", 98319150);

    SAMLineParser parser = new SAMLineParser(SAMUtils.newSAMFileHeader(desc));

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

    records = new ArrayList<>();

    filter = new QualityReadAlignmentsFilter();
    filter.setParameter("threshold", "50");
  }

  /**
   * Test method for {fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.
   * QualityReadAlignmentsFilter#setParameter(java.lang.String,
   * java.lang.String)}.
   */
  @Test
  public void testSetParameter() {
    try {
      filter.setParameter("threshold", "100");
      assertTrue(true);
    } catch (EoulsanException e) {
      assertTrue(false);
    }

    try {
      filter.setParameter("threshold", "-2");
      assertTrue(false);
    } catch (EoulsanException e) {
      assertTrue(true);
    }

    try {
      filter.setParameter("ko", "2");
      assertTrue(false);
    } catch (EoulsanException e) {
      assertTrue(true);
    }
  }

  /**
   * Test method for {fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.
   * QualityReadAlignmentsFilter#init()}.
   */
  @Test
  public void testInit() {
    try {
      filter.init();
      assertTrue(true);
    } catch (Exception e) {
      assertTrue(false);
    }
  }

  /**
   * Test method for {fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.
   * QualityReadAlignmentsFilter#getName()}.
   */
  @Test
  public void testGetName() {
    assertEquals("mappingquality", filter.getName());
    assertFalse("ko".equals(filter.getName()));
  }

  /**
   * Test method for {fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.
   * QualityReadAlignmentsFilter#getDescription()}.
   */
  @Test
  public void testGetDescription() {
    assertEquals(
        "With this filter, the alignments are filtered by their quality score.",
        filter.getDescription());
    assertFalse("ko".equals(filter.getName()));
  }

  /**
   * Test method for {fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.
   * QualityReadAlignmentsFilter#filterReadAlignments(java.util.List, boolean)}.
   */
  @Test
  public void testFilterReadAlignments() {

    // single-end mode
    records.add(samRecordSE1);
    assertEquals(1, records.size());
    filter.filterReadAlignments(records);
    assertEquals(0, records.size());

    records.add(samRecordSE2);
    assertEquals(1, records.size());
    filter.filterReadAlignments(records);
    assertEquals(1, records.size());

    records.add(samRecordSE1);
    records.add(samRecordSE2);
    assertEquals(3, records.size());
    filter.filterReadAlignments(records);
    assertEquals(2, records.size());

    records.clear();

    // paired-end mode
    records.add(samRecordPE1);
    records.add(samRecordPE2);
    assertEquals(2, records.size());
    filter.filterReadAlignments(records);
    assertEquals(0, records.size());

    records.add(samRecordPE3);
    records.add(samRecordPE4);
    assertEquals(2, records.size());
    filter.filterReadAlignments(records);
    assertEquals(0, records.size());

    records.add(samRecordPE5);
    records.add(samRecordPE6);
    assertEquals(2, records.size());
    filter.filterReadAlignments(records);
    assertEquals(0, records.size());

    records.add(samRecordPE7);
    records.add(samRecordPE8);
    assertEquals(2, records.size());
    filter.filterReadAlignments(records);
    assertEquals(2, records.size());
  }

  /**
   * Test method for {fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.
   * QualityReadAlignmentsFilter#QualityReadAlignmentsFilter()}.
   */
  @Test
  public void testQualityReadAlignmentsFilter() {
    QualityReadAlignmentsFilter filterTest = new QualityReadAlignmentsFilter();
    assertNotNull(filterTest);
  }

}
