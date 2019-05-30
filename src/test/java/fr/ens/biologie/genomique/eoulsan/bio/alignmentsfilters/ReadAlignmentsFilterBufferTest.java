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

package fr.ens.biologie.genomique.eoulsan.bio.alignmentsfilters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.bio.GenomeDescription;
import fr.ens.biologie.genomique.eoulsan.bio.SAMUtils;
import htsjdk.samtools.SAMLineParser;
import htsjdk.samtools.SAMRecord;

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

  @Before
  public void setUp() throws Exception {

    // all records (single-end and paired-end modes) have a good mapping
    // quality score (above the threshold)

    this.recordSE1 =
        "HWI-EAS285_0001_'':1:1:1260:18686#0/1\t16\tchr4\t129576419\t72\t76M\t*\t0\t0"
            + "\tGACGGATCCGAGANANTGANNTGANAAGAGGNNNNNNNNNNNNNNAATTTGAGGACCNAAGGGATGCAGATGATGC"
            + "\tGEGEGE:@=BB><#7#AA:##@CC#CA9A8;##############=GGGGGDCDCDD#DDCGFGDGGGGBGGGGGG"
            + "\tXA:i:1\tMD:Z:13T1T3C0T3A2C3T0T0T0T0G0G0T0T0T0T0G0T0G0G12C18\tNM:i:21";
    this.recordSE2 =
        "HWI-EAS285_0001_'':1:1:1260:13682#0/1\t0\tchr16\t23360177\t55\t76M\t*\t0\t0\t"
            + "ATTTGCGACAGGTAGTTTNAAATCTGTGACTNNNNNNNNNNNNNNAGTGNCNTTCNNCGTNGNCACTGACGTCACT"
            + "\tGGGGFGGGGFCECEEBCB#ACCCCCGGFGGA##############AA=A#A#A?A##A=?#9#8?CCB>CGEGGGA\tXA:i:1\tMD:Z:18A12G0A0G0T0G0C0T0A0T0A0G0G0A0A4T1T3T0A3G1G13\tNM:i:21";
    // recordSE3, recordSE4 and recordSE5 are various matches of the same read
    this.recordSE3 =
        "HWI-EAS285_0001_'':1:1:1259:2442#0/1\t16\tchr9\t59513044\t50\t76M\t*\t0\t0"
            + "\tGGGACTGCCTTCANNNAGANNCAGNANCTCCNNNNNNNNNNNNNNGACACCTTCCTGNAACACATGTGCCGCCTGG"
            + "\t#############################################ABE@?E@>CC@C#BBDEDGGGGFFEGEGGGG"
            + "\tXA:i:1\tMD:Z:13T0T0C3C0C3C1G4A0T0G0C0A0G0C0C0A0T0G0G0C0T12G18\tNM:i:22";
    this.recordSE4 =
        "HWI-EAS285_0001_'':1:1:1259:2442#0/1\t0\tchr5\t22231887\t55\t76M\t*\t0\t0"
            + "\tCCAGGCGGCACATGTGTTNCAGGAAGGTGTCNNNNNNNNNNNNNNGGAGNTNCTGNNTCTNNNTGAAGGCAGTCCC"
            + "\tGGGGEGEFFGGGGDEDBB#C@CC>@E?@EBA#############################################"
            + "\tXA:i:1\tMD:Z:18C12A0G0C0C0A0T0G0G0C0T0G0C0A0T4C1G3G0G3G0A0A13"
            + "\tNM:i:22";
    this.recordSE5 =
        "HWI-EAS285_0001_'':1:1:1259:2442#0/1\t0\tchr13\t14002582\t72\t76M\t*\t0\t0"
            + "\tCCAGGCGGCACATGTGTTNCAGGAAGGTGTCNNNNNNNNNNNNNNGGAGNTNCTGNNTCTNNNTGAAGGCAGTCCC"
            + "\tGGGGEGEFFGGGGDEDBB#C@CC>@E?@EBA#############################################"
            + "\tXA:i:2\tMD:Z:18C3A8A0G0C0C0A0T0T0G0C0T0C0C0A0T4C1G2A0G0G3G0A0A5A6A0\tNM:i:26";
    // recordSE6 and recordSE7 are various matches of the same read
    this.recordSE6 =
        "HWI-EAS285_0001_'':1:1:1259:1873#0/1\t16\tchr2\t28011331\t255\t76M\t*\t0\t0"
            + "\tGTCTGGCTCCGACNCNCAGGNACCNCNGCCCNNNNNNNNNNNNNNAAGAGCCAGTTCNGGGGTCCCTGGGCCACAC"
            + "\t##############################################EEE:E=<?5=?#BAAF=AFFEFFFDE?EEE"
            + "\tXA:i:1\tMD:Z:0C2A3A0G0T3T1T4C3A1A1A2G0C0T0G0G0G0T0A0C0T0G0A0A0G12T18\tNM:i:26";
    this.recordSE7 =
        "HWI-EAS285_0001_'':1:1:1259:1873#0/1\t16\tchr12\t56412446\t255\t76M\t*\t0\t0"
            + "\tGTCTGGCTCCGACNCNCAGGNACCNCNGCCCNNNNNNNNNNNNNNAAGAGCCAGTTCNGGGGTCCCTGGGCCACAC"
            + "\t##############################################EEE:E=<?5=?#BAAF=AFFEFFFDE?EEE"
            + "\tXA:i:1\tMD:Z:13T1T4T3A1A1A2A0C0T0G0G0G0T0A0C0C0A0C0A0G12T18\tNM:i:21";

    // recordPE1 and recordPE2 paired
    this.recordPE1 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:2462:2222\t99\tchr13\t34124505\t75\t101M\t=\t34124588\t184"
            + "\tCTGANAGAGAAAGTTTACCAAATGCTTCAGAAGTGGCTGATGCGGGAAGGCACCAAAGGGGCCACAGTGGGAAAGTTGGCCCAGGCACTTCACCAATGTTG"
            + "\tCCCF#2ADHHGHHIIJJJJJJJJJJJJJJIJJIGIIJJJGGIDHIJGIIIIIJJIJH?CHFFDDDEDCDDDBDDBDCDDDDDD?BDDDDDDDDDDDDDDED"
            + "\tXA:i:1\tMD:Z:4A96\tNM:i:1";
    this.recordPE2 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:2462:2222\t147\tchr13\t34124588\t145\t101M\t=\t34124505\t-184"
            + "\tGGCACTTCACCAATGTTGCAGGATAGACCTGCTGAACCACTTGATTCGTGCCAGCCAGAGCTAAGCCTGGGCAGGCTCTGGCAGTGGGAAGCAAACTATTT"
            + "\tDDDCDDDDDDDDDDDDDDDEEDEEDEBFFFFHGHHHHJIJJJJJJJJJJIGJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJHHHHHFFFFFCCC"
            + "\tXA:i:0\tMD:Z:101\tNM:i:0";
    // recordPE3, recordPE4, recordPE5 and recordPE6 are various matches of
    // the same read
    // recordPE3 and recordPE4 paired
    this.recordPE3 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:1668:2230\t99\tchr12\t76732159\t128\t101M\t=\t76732246\t188"
            + "\tCTTTCCATTCAGCTCACTGATGACCTTGTTGAGCCGATCATCGACCGCTTCGATGCCCACGCTGTCTAGTATTTTCTTGATGTCTTTGGCGCTAGGAGAGG"
            + "\tCCCFFFFFHHHHHJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJ@GIIIJJJJJJJHHHHHFFDDDDDEDDDFFFEDDDDDDDEDEDDDDDDDDDDDDDBDD\tXA:i:0\tMD:Z:43T20C36\tNM:i:2";
    this.recordPE4 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:1668:2230\t147\tchr12\t76732246\t60\t101M\t=\t76732159\t-188"
            + "\tGGCGCTAGGAGAGGAGTTGCCCCCGAGGGCGGCCAGCAGGTAAGAGGCGACGTAGCGCATGTCGGCTGCGGGGGACAGACCTCACGCGTGCGACCTCGGCG"
            + "\tBDBDDDCDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDBDDDDBDDDDDDDFFFHHJJJJJJJJJHFJJIHGJJJJHHHHHFFFFFCCC\tXA:i:1\tMD:Z:100A0\tNM:i:1";
    // recordPE5 and recordPE6 paired
    this.recordPE5 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:1668:2230\t99\tchr4\t129192614\t74\t101M\t=\t129192649\t136"
            + "\tCTTTTTGCCCTCCTGTGGATTCTCCCATCAGCCATTTGGTCTTACTCTTAAGGCCAGTTGAAGATGGTCCCTTACGGTTTCCCAAGTTAGGTTAGTGATGT"
            + "\tCCCFFFFFHHHHHJJHJIJJJIJJJJJJJJJIJJJJJJIIHIJJJJIJJJJJJJJJJIJJJJJIJJJHIJJJJHHEHDFFFEEEEDDDEDDCCDDCDEEDC\tXA:i:1\tMD:Z:5C95\tNM:i:1";
    this.recordPE6 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:1668:2230\t147\tchr4\t129192649\t230\t101M\t=\t129192614\t-136"
            + "\tTTGGTCTTACTCTTAAGGCCAGTTGAAGATGGTCCCTTACGGTTTCCCAAGTTAGGTTAGTGATGTGAGATGCCCTGTCCCTACCTCCTTCCCGAGCCCCG"
            + "\tDDDDDCDCADDDDDDDDDDDDDDEEDDDDDDDDDDDDDBDEEFFFFHHHHEHIEJJJIGJIIGGIIJIJJIIJJJJIHJHGD?JIHJJHFHHGFFFFFCCC\tXA:i:2\tMD:Z:87T12A0\tNM:i:2";

    // recordPE7 and recordPE8 paired
    this.recordPE7 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:2584:2222\t99\tchr1\t173235257\t255\t101M\t=\t173235280\t124"
            + "\tCTTGTATCGCTCCTCAAACTTGACCTTGGCCTCCCGCCTGGCCTTGCGCTTCAAAGCTGGGTCCCTGAACACATCCTTGTTGACAACAGTCTTGTCCAAGG"
            + "\tCCCFFFFFHHHHHJJJJJJJJJJJJJJJJJJJJJJJJJHIIJIJJIIIIJJJJJIIJHHHHFFFFFDEEEEDDDDDDDDDDDDDDDDDDDDDEDDDDDDDD"
            + "\tXA:i:0\tMD:Z:101\tNM:i:0";
    this.recordPE8 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:2584:2222\t147\tchr1\t173235280\t255\t101M\t=\t173235257\t-124"
            + "\tCCTTGGCCTCCCGCCTGGCCTTGCGCTTCAAAGCTGGGTCCCTGAACACATCCTTGTTGACAACAGTCTTGTCCAAGGGGATATCCACAGAGTACCTTGTG"
            + "\tDDDDDDDDDDDDDDDDDDDDDDDFFHHHHHHJJJJJJJJJHDJJJJJIJIHIJJJJJIIIJJJIJJIJJJJJHJJJJJJJJJJJJJJJHHHHHDFFFFCCC"
            + "\tXA:i:0\tMD:Z:101\tNM:i:0";
    this.recordPE9 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:2584:2222\t99\tchr11\t93898574\t255\t101M\t=\t93898597\t124"
            + "\tCTTGTATCGCTCCTCAAACTTGACCTTGGCCTCCCGCCTGGCCTTGCGCTTCAAAGCTGGGTCCCTGAACACATCCTTGTTGACAACAGTCTTGTCCAAGG"
            + "\tCCCFFFFFHHHHHJJJJJJJJJJJJJJJJJJJJJJJJJHIIJIJJIIIIJJJJJIIJHHHHFFFFFDEEEEDDDDDDDDDDDDDDDDDDDDDEDDDDDDDD"
            + "\tXA:i:0\tMD:Z:101\tNM:i:0";
    this.recordPE10 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:2584:2222\t147\tchr11\t93898597\t255\t101M\t=\t93898574\t-124"
            + "\tCCTTGGCCTCCCGCCTGGCCTTGCGCTTCAAAGCTGGGTCCCTGAACACATCCTTGTTGACAACAGTCTTGTCCAAGGGGATATCCACAGAGTACCTTGTG"
            + "\tDDDDDDDDDDDDDDDDDDDDDDDFFHHHHHHJJJJJJJJJHDJJJJJIJIHIJJJJJIIIJJJIJJIJJJJJHJJJJJJJJJJJJJJJHHHHHDFFFFCCC"
            + "\tXA:i:0\tMD:Z:101\tNM:i:0";

    final GenomeDescription desc = new GenomeDescription();
    desc.addSequence("chr1", 197195432);
    desc.addSequence("chr2", 181748087);
    desc.addSequence("chr4", 155630120);
    desc.addSequence("chr5", 152537259);
    desc.addSequence("chr9", 124076172);
    desc.addSequence("chr11", 121843856);
    desc.addSequence("chr12", 121257530);
    desc.addSequence("chr13", 120284312);
    // desc.addSequence("chr15", 103494974);
    desc.addSequence("chr16", 98319150);

    SAMLineParser parser = new SAMLineParser(SAMUtils.newSAMFileHeader(desc));

    this.samRecordSE1 = parser.parseLine(this.recordSE1);
    this.samRecordSE2 = parser.parseLine(this.recordSE2);
    this.samRecordSE3 = parser.parseLine(this.recordSE3);
    this.samRecordSE4 = parser.parseLine(this.recordSE4);
    this.samRecordSE5 = parser.parseLine(this.recordSE5);
    this.samRecordSE6 = parser.parseLine(this.recordSE6);
    this.samRecordSE7 = parser.parseLine(this.recordSE7);

    this.samRecordPE1 = parser.parseLine(this.recordPE1);
    this.samRecordPE2 = parser.parseLine(this.recordPE2);
    this.samRecordPE3 = parser.parseLine(this.recordPE3);
    this.samRecordPE4 = parser.parseLine(this.recordPE4);
    this.samRecordPE5 = parser.parseLine(this.recordPE5);
    this.samRecordPE6 = parser.parseLine(this.recordPE6);
    this.samRecordPE7 = parser.parseLine(this.recordPE7);
    this.samRecordPE8 = parser.parseLine(this.recordPE8);
    this.samRecordPE9 = parser.parseLine(this.recordPE9);
    this.samRecordPE10 = parser.parseLine(this.recordPE10);

    this.recordsVerif = new ArrayList<>();

    this.filter = new QualityReadAlignmentsFilter();
    this.filter.setParameter("threshold", "50");
    this.rafb = new ReadAlignmentsFilterBuffer(this.filter);
  }

  /**
   * Test method for {fr.ens.biologie.genomique.eoulsan.bio.alignmentsfilters.
   * ReadAlignmentsFilterBuffer#addAlignment(net.sf.samtools.SAMRecord)} and
   * {fr.ens.biologie.genomique.eoulsan.bio.alignmentsfilters.
   * ReadAlignmentsFilterBuffer #getFilteredAlignments(boolean)}.
   */
  @Test
  public void testAddAlignmentAndGetFilteredAlignments() {

    // single-end mode

    // first case
    assertTrue(this.rafb.addAlignment(this.samRecordSE1));
    assertFalse(this.rafb.addAlignment(this.samRecordSE3));
    this.recordsVerif.add(this.samRecordSE1);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());
    assertTrue(this.rafb.addAlignment(this.samRecordSE3));
    assertTrue(this.rafb.addAlignment(this.samRecordSE4));
    assertTrue(this.rafb.addAlignment(this.samRecordSE5));
    assertFalse(this.rafb.addAlignment(this.samRecordSE2));
    this.recordsVerif.clear();
    this.recordsVerif.add(this.samRecordSE3);
    this.recordsVerif.add(this.samRecordSE4);
    this.recordsVerif.add(this.samRecordSE5);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());
    assertTrue(this.rafb.addAlignment(this.samRecordSE2));
    // rafb.checkBuffer();
    this.recordsVerif.clear();
    this.recordsVerif.add(this.samRecordSE2);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());

    this.recordsVerif.clear();

    // second case
    assertTrue(this.rafb.addAlignment(this.samRecordSE3));
    assertTrue(this.rafb.addAlignment(this.samRecordSE4));
    assertFalse(this.rafb.addAlignment(this.samRecordSE1));
    this.recordsVerif.add(this.samRecordSE3);
    this.recordsVerif.add(this.samRecordSE4);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());
    assertTrue(this.rafb.addAlignment(this.samRecordSE1));
    assertFalse(this.rafb.addAlignment(this.samRecordSE5));
    this.recordsVerif.clear();
    this.recordsVerif.add(this.samRecordSE1);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());
    assertTrue(this.rafb.addAlignment(this.samRecordSE5));
    assertTrue(this.rafb.addAlignment(this.samRecordSE3));
    // rafb.checkBuffer();
    this.recordsVerif.clear();
    this.recordsVerif.add(this.samRecordSE5);
    this.recordsVerif.add(this.samRecordSE3);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());

    this.recordsVerif.clear();

    // third case
    assertTrue(this.rafb.addAlignment(this.samRecordSE1));
    assertFalse(this.rafb.addAlignment(this.samRecordSE2));
    this.recordsVerif.add(this.samRecordSE1);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());
    assertTrue(this.rafb.addAlignment(this.samRecordSE2));
    // rafb.checkBuffer();
    this.recordsVerif.clear();
    this.recordsVerif.add(this.samRecordSE2);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());

    this.recordsVerif.clear();

    // fourth case
    assertTrue(this.rafb.addAlignment(this.samRecordSE3));
    assertTrue(this.rafb.addAlignment(this.samRecordSE4));
    assertFalse(this.rafb.addAlignment(this.samRecordSE6));
    this.recordsVerif.add(this.samRecordSE3);
    this.recordsVerif.add(this.samRecordSE4);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());
    assertTrue(this.rafb.addAlignment(this.samRecordSE6));
    assertTrue(this.rafb.addAlignment(this.samRecordSE7));
    // rafb.checkBuffer();
    this.recordsVerif.clear();
    this.recordsVerif.add(this.samRecordSE6);
    this.recordsVerif.add(this.samRecordSE7);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());

    this.recordsVerif.clear();

    // paired-end mode

    // first case
    assertTrue(this.rafb.addAlignment(this.samRecordPE1));
    assertTrue(this.rafb.addAlignment(this.samRecordPE2));
    assertFalse(this.rafb.addAlignment(this.samRecordPE3));
    this.recordsVerif.add(this.samRecordPE1);
    this.recordsVerif.add(this.samRecordPE2);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());
    assertTrue(this.rafb.addAlignment(this.samRecordPE3));
    assertTrue(this.rafb.addAlignment(this.samRecordPE4));
    assertTrue(this.rafb.addAlignment(this.samRecordPE5));
    assertTrue(this.rafb.addAlignment(this.samRecordPE6));
    assertFalse(this.rafb.addAlignment(this.samRecordPE1));
    this.recordsVerif.clear();
    this.recordsVerif.add(this.samRecordPE3);
    this.recordsVerif.add(this.samRecordPE4);
    this.recordsVerif.add(this.samRecordPE5);
    this.recordsVerif.add(this.samRecordPE6);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());
    assertTrue(this.rafb.addAlignment(this.samRecordPE1));
    assertTrue(this.rafb.addAlignment(this.samRecordPE2));
    // rafb.checkBuffer();
    this.recordsVerif.clear();
    this.recordsVerif.add(this.samRecordPE1);
    this.recordsVerif.add(this.samRecordPE2);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());

    this.recordsVerif.clear();

    // second case
    assertTrue(this.rafb.addAlignment(this.samRecordPE3));
    assertTrue(this.rafb.addAlignment(this.samRecordPE4));
    assertTrue(this.rafb.addAlignment(this.samRecordPE5));
    assertTrue(this.rafb.addAlignment(this.samRecordPE6));
    assertFalse(this.rafb.addAlignment(this.samRecordPE1));
    this.recordsVerif.add(this.samRecordPE3);
    this.recordsVerif.add(this.samRecordPE4);
    this.recordsVerif.add(this.samRecordPE5);
    this.recordsVerif.add(this.samRecordPE6);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());
    assertTrue(this.rafb.addAlignment(this.samRecordPE1));
    assertTrue(this.rafb.addAlignment(this.samRecordPE2));
    assertFalse(this.rafb.addAlignment(this.samRecordPE3));
    this.recordsVerif.clear();
    this.recordsVerif.add(this.samRecordPE1);
    this.recordsVerif.add(this.samRecordPE2);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());
    assertTrue(this.rafb.addAlignment(this.samRecordPE3));
    assertTrue(this.rafb.addAlignment(this.samRecordPE4));
    assertTrue(this.rafb.addAlignment(this.samRecordPE5));
    assertTrue(this.rafb.addAlignment(this.samRecordPE6));
    // rafb.checkBuffer();
    this.recordsVerif.clear();
    this.recordsVerif.add(this.samRecordPE3);
    this.recordsVerif.add(this.samRecordPE4);
    this.recordsVerif.add(this.samRecordPE5);
    this.recordsVerif.add(this.samRecordPE6);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());

    this.recordsVerif.clear();

    // third case
    assertTrue(this.rafb.addAlignment(this.samRecordPE1));
    assertTrue(this.rafb.addAlignment(this.samRecordPE2));
    assertFalse(this.rafb.addAlignment(this.samRecordPE3));
    this.recordsVerif.add(this.samRecordPE1);
    this.recordsVerif.add(this.samRecordPE2);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());
    assertTrue(this.rafb.addAlignment(this.samRecordPE3));
    assertTrue(this.rafb.addAlignment(this.samRecordPE4));
    // rafb.checkBuffer();
    this.recordsVerif.clear();
    this.recordsVerif.add(this.samRecordPE3);
    this.recordsVerif.add(this.samRecordPE4);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());

    this.recordsVerif.clear();

    // fourth case
    assertTrue(this.rafb.addAlignment(this.samRecordPE3));
    assertTrue(this.rafb.addAlignment(this.samRecordPE4));
    assertTrue(this.rafb.addAlignment(this.samRecordPE5));
    assertTrue(this.rafb.addAlignment(this.samRecordPE6));
    assertFalse(this.rafb.addAlignment(this.samRecordPE7));
    this.recordsVerif.add(this.samRecordPE3);
    this.recordsVerif.add(this.samRecordPE4);
    this.recordsVerif.add(this.samRecordPE5);
    this.recordsVerif.add(this.samRecordPE6);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());
    assertTrue(this.rafb.addAlignment(this.samRecordPE7));
    assertTrue(this.rafb.addAlignment(this.samRecordPE8));
    assertTrue(this.rafb.addAlignment(this.samRecordPE9));
    assertTrue(this.rafb.addAlignment(this.samRecordPE10));
    // rafb.checkBuffer();
    this.recordsVerif.clear();
    this.recordsVerif.add(this.samRecordPE7);
    this.recordsVerif.add(this.samRecordPE8);
    this.recordsVerif.add(this.samRecordPE9);
    this.recordsVerif.add(this.samRecordPE10);
    assertEquals(this.recordsVerif, this.rafb.getFilteredAlignments());
  }

  /**
   * Test method for {fr.ens.biologie.genomique.eoulsan.bio.alignmentsfilters.
   * ReadAlignmentsFilterBuffer
   * #ReadAlignmentsFilterBuffer(fr.ens.biologie.genomique.eoulsan
   * .bio.alignmentsfilters.ReadAlignmentsFilter)}.
   */
  @Test
  public void testReadAlignmentsFilterBufferReadAlignmentsFilter() {
    // fail("Not yet implemented");
  }

  /**
   * Test method for {fr.ens.biologie.genomique.eoulsan.bio.alignmentsfilters.
   * ReadAlignmentsFilterBuffer
   * #ReadAlignmentsFilterBuffer(fr.ens.biologie.genomique.eoulsan
   * .bio.alignmentsfilters.ReadAlignmentsFilter, boolean)}.
   */
  @Test
  public void testReadAlignmentsFilterBufferReadAlignmentsFilterBoolean() {
    // fail("Not yet implemented");
  }

}
