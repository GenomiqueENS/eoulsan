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

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.GenomeDescription;
import fr.ens.biologie.genomique.eoulsan.bio.SAMUtils;
import htsjdk.samtools.SAMLineParser;
import htsjdk.samtools.SAMRecord;

/**
 * This class is a JUnit test class to test the class
 * DistanceFromReferenceReadAlignmentsFilter.java.
 * @author Claire Wallon
 */
public class DistanceFromReferenceReadAlignmentsFilterTest {

  private String recordSE1, recordSE2, recordSE3;
  private String recordPE1, recordPE2, recordPE3, recordPE4, recordPE5,
      recordPE6;
  private SAMRecord samRecordSE1, samRecordSE2, samRecordSE3;
  private SAMRecord samRecordPE1, samRecordPE2, samRecordPE3, samRecordPE4,
      samRecordPE5, samRecordPE6;
  private List<SAMRecord> records;
  private DistanceFromReferenceReadAlignmentsFilter filter;

  @Before
  public void setUp() throws Exception {

    // recordSE1, recordSE2 and recordSE3 have the same read name (three matches
    // for the same read)
    // recordSE1 : clipping ko, NM ok
    this.recordSE1 =
        "HWI-EAS285_0001_'':1:1:1261:14574#0/1\t16\tchr1\t173235257\t38\t15S61M\t*\t0\t0"
            + "\tGAGCCTTGCAGTAAGCAGCTNAACAGGAGCATTNNNNNNNNNNNNAAGGGCATCACTNTCTCAGGCCTCAAGCCAG"
            + "\tGEGDDGGGFGGDDCDDCDDD#DCDDDCCCCC=2############BGGGGGCCBAAD#DDDEGGGEGGGGGGFGGG"
            + "\tMD:Z:61\tNH:i:1\tHI:i:1\tNM:i:0\tSM:i:38\tXQ:i:40\tX2:i:0";
    // recordSE2 : clipping ok, NM ok
    this.recordSE2 =
        "HWI-EAS285_0001_'':1:1:1261:12613#0/1\t0\tchr5\t142537259\t35\t76M\t*\t0\t0"
            + "\tGAAGCCAGCAATTTGTCANGTGTGATTCATGNNNNNNNNNNNNCCTGGTGTTTTANAAATGCATTGTCTTAAGTAC"
            + "\tD=?DDDCBD=?AAAB;BB#@6;06,>@@>@A############;96777;>A@A:#=8;9>B<>=C@CBBB?D?@="
            + "\tMD:Z:76\tNH:i:1\tHI:i:1\tNM:i:0\tSM:i:35\tXQ:i:40\tX2:i:0";
    // recordSE3 : clipping ok, NM ko
    this.recordSE3 =
        "HWI-EAS285_0001_'':1:1:1421:9400#0/1\t16\tchr9\t104076172\t38\t35M4D41M\t*\t0\t0"
            + "\tCAGCTGAACTCTTCAGCAACTAGTGTTAAATTCCTAAAAAAAAAGATGAAAAGGAAGACCTGAGGTCCACTTTGCC"
            + "\tGEGEEGGEGGGGGDGGFGEFGGGGGGGGGGGFGGGGGGGGGGGGGGGGGGGGGGGGGGEGGGGGGGFGGGGGGGGG"
            + "\tMD:Z:35^AAAA41\tNH:i:1\tHI:i:1\tNM:i:4\tSM:i:38\tXQ:i:40\tX2:i:0";

    // recordPE1, recordPE2, recordPE3, recordPE4, recordPE5 and recordPE6 have
    // the same read name (three matches for the same read)
    // recordPE1 and recordPE2 paired
    // recordPE1 : clipping ok, NM ok
    this.recordPE1 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:1617:2229\t99\tchr11\t119192614\t40\t101M\t=\t119192649\t136"
            + "\tCTTTTTGCCCTCCTGTGGATTCTCCCATCAGCCATTTGGTCTTACTCTTAAGGCCAGTTGAAGATGGTCCCTTACGGTTTCCCAAGTTAGGTTAGTGATGT"
            + "\tCCCFFFFFHHHHHJJHJIJJJIJJJJJJJJJIJJJJJJIIHIJJJJIJJJJJJJJJJIJJJJJIJJJHIJJJJHHEHDFFFEEEEDDDEDDCCDDCDEEDC"
            + "\tMD:Z:5C95\tNH:i:1\tHI:i:1\tNM:i:1\tSM:i:40\tXQ:i:40\tX2:i:0";
    // recordPE2 : clipping ko, NM ok
    this.recordPE2 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:1617:2229\t147\tchr11\t119192649\t40\t100M1S\t=\t119192614\t-136"
            + "\tTTGGTCTTACTCTTAAGGCCAGTTGAAGATGGTCCCTTACGGTTTCCCAAGTTAGGTTAGTGATGTGAGATGCCCTGTCCCTACCTCCTTCCCGAGCCCCG"
            + "\tDDDDDCDCADDDDDDDDDDDDDDEEDDDDDDDDDDDDDBDEEFFFFHHHHEHIEJJJIGJIIGGIIJIJJIIJJJJIHJHGD?JIHJJHFHHGFFFFFCCC"
            + "\tMD:Z:87T12\tNH:i:1\tHI:i:1\tNM:i:1\tSM:i:40\tXQ:i:40\tX2:i:0";
    // recordPE3 and recordPE4 paired
    // recordPE3 : clipping ok, NM ko
    this.recordPE3 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:8942:2345\t83\tchr5\t103806613\t40\t46M2D55M\t=\t103806546\t-170"
            + "\tGGCTGGTGGGAGTTGAAGCTCACTGGGTCCTGTGAAGCCATAGAGGCTCATGGGGGAAGGGAAGGAAGGGTCTCAGCGACTTCCTGCATATTACAAGGTTG"
            + "\tDDDDDDDDDDDDDDDDDDDDDDDDC?DFFFFHHHHHJJJJJJJJJJJJJJJJJJJJJJJJJJJJJIIJJJJJJJJJJJJJJJJJJJJJHHHHHFFFFFCCC"
            + "\tMD:Z:23A22^TA0G54\tNH:i:1\tHI:i:1\tNM:i:4\tSM:i:40\tXQ:i:40\tX2:i:0";
    // recordPE4 : clipping ok, NM ok
    this.recordPE4 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:8942:2345\t163\tchr5\t103806546\t40\t101M\t=\t103806613\t170"
            + "\tCTGAGATATTAGTGACAGGTTTTGGAGAAGAAGGGTCCTTGCAGCCAAGGGTATCATTTCAAACCAAGGCTGGTGGGAGTTGAAGCTCACTGGGTCCTGTG"
            + "\tB<?AADBDDFHBCG>EEFC3AEFFF>@9AEEFIJJIJJJJJJIJJJJIJJGFHHIIIJJJIJJJJIIJHHHHFDFFDDDBDDDDDDDDDDDCDDDDDDDCD"
            + "\tMD:Z:90A10\tNH:i:1\tHI:i:1\tNM:i:1\tSM:i:40\tXQ:i:40\tX2:i:0";
    // recordPE5 and recordPE6 paired
    // recordPE5 : clipping ok, NM ok
    this.recordPE5 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:1426:2207\t99\tchr1\t35400491\t40\t101M\t=\t35400811\t421"
            + "\tCCGGNNNNNNGGGAGCGGGAAACACAGAAAGCCAAGGGCCAAGAGCAGTGGTTCCGAGTGAGCCTGAGGAACCTGCTCGGCTACTACAACCAGAGCGCGGG"
            + "\t<<<@######34@=@?@@?????????????????????????????????????===<?????=<<<<=<<<<<;<=<:<<<<======<<<<<=:::::"
            + "\tMD:Z:101\tNH:i:1\tHI:i:1\tNM:i:0\tSM:i:40\tXQ:i:40\tX2:i:0";
    // recordPE6 : clipping ok, NM ok
    this.recordPE6 =
        "HWI-1KL110:37:C0BE6ACXX:7:1101:1426:2207\t147\tchr1\t35400811\t40\t101M\t=\t35400491\t-421"
            + "\tGCTGTGACTTGGGGTCGGACTGGCGCCTCCTCCGCGGGTACCTGCAGTTCGCCTATGAAGGCCGCGATTACATCGCCCTGAACGAAGACCTGAAAACGTGG"
            + "\tDDDDEDCDDDDDDDDDDDDDDDDDDDDDDDBDDDDDDDDBDDDDDDDDFHEHHJIJJJJJJJJJJJJJIHJJIIIIHIJIJIJJJJIHHHHHHFFFFFCCC"
            + "\tMD:Z:101\tNH:i:1\tHI:i:1\tNM:i:0\tSM:i:40\tXQ:i:40\tX2:i:0";

    final GenomeDescription desc = new GenomeDescription();
    desc.addSequence("chr1", 197195432);
    desc.addSequence("chr5", 152537259);
    desc.addSequence("chr9", 124076172);
    desc.addSequence("chr11", 121843856);

    SAMLineParser parser = new SAMLineParser(SAMUtils.newSAMFileHeader(desc));

    this.samRecordSE1 = parser.parseLine(this.recordSE1);
    this.samRecordSE2 = parser.parseLine(this.recordSE2);
    this.samRecordSE3 = parser.parseLine(this.recordSE3);

    this.samRecordPE1 = parser.parseLine(this.recordPE1);
    this.samRecordPE2 = parser.parseLine(this.recordPE2);
    this.samRecordPE3 = parser.parseLine(this.recordPE3);
    this.samRecordPE4 = parser.parseLine(this.recordPE4);
    this.samRecordPE5 = parser.parseLine(this.recordPE5);
    this.samRecordPE6 = parser.parseLine(this.recordPE6);

    this.records = new ArrayList<>();

    this.filter = new DistanceFromReferenceReadAlignmentsFilter();
    this.filter.setParameter("threshold", "3");
  }

  @Test
  public void testSetParameter() {
    try {
      this.filter.setParameter("threshold", "40");
      assertTrue(true);
    } catch (EoulsanException e) {
      assertTrue(false);
    }

    try {
      this.filter.setParameter("threshold", "-2");
      assertTrue(false);
    } catch (EoulsanException e) {
      assertTrue(true);
    }

    try {
      this.filter.setParameter("ko", "2");
      assertTrue(false);
    } catch (EoulsanException e) {
      assertTrue(true);
    }
  }

  @Test
  public void testInit() {
    try {
      this.filter.init();
      assertTrue(true);
    } catch (Exception e) {
      assertTrue(false);
    }
  }

  @Test
  public void testGetName() {
    assertEquals("distancefromreference", this.filter.getName());
    assertFalse("ko".equals(this.filter.getName()));
  }

  @Test
  public void testGetDescription() {
    assertEquals(
        "After this filter, only the alignments which the distance from the "
            + "reference is lower than the given distance are kept.",
        this.filter.getDescription());
    assertFalse("ko".equals(this.filter.getName()));
  }

  @Test
  public void testFilterReadAlignments() {

    // single-end mode
    this.records.add(this.samRecordSE1);
    assertEquals(1, this.records.size());
    this.filter.filterReadAlignments(this.records);
    assertEquals(0, this.records.size());

    this.records.add(this.samRecordSE3);
    assertEquals(1, this.records.size());
    this.filter.filterReadAlignments(this.records);
    assertEquals(0, this.records.size());

    this.records.add(this.samRecordSE2);
    assertEquals(1, this.records.size());
    this.filter.filterReadAlignments(this.records);
    assertEquals(1, this.records.size());

    this.records.add(this.samRecordSE1);
    this.records.add(this.samRecordSE3);
    assertEquals(3, this.records.size());
    this.filter.filterReadAlignments(this.records);
    assertEquals(1, this.records.size());

    this.records.clear();

    // paired-end mode
    this.records.add(this.samRecordPE1);
    this.records.add(this.samRecordPE2);
    assertEquals(2, this.records.size());
    this.filter.filterReadAlignments(this.records);
    assertEquals(0, this.records.size());

    this.records.add(this.samRecordPE3);
    this.records.add(this.samRecordPE4);
    assertEquals(2, this.records.size());
    this.filter.filterReadAlignments(this.records);
    assertEquals(0, this.records.size());

    this.records.add(this.samRecordPE5);
    this.records.add(this.samRecordPE6);
    assertEquals(2, this.records.size());
    this.filter.filterReadAlignments(this.records);
    assertEquals(2, this.records.size());

    this.records.add(this.samRecordPE3);
    this.records.add(this.samRecordPE4);
    this.records.add(this.samRecordPE1);
    this.records.add(this.samRecordPE2);
    assertEquals(6, this.records.size());
    this.filter.filterReadAlignments(this.records);
    assertEquals(2, this.records.size());

  }

}
