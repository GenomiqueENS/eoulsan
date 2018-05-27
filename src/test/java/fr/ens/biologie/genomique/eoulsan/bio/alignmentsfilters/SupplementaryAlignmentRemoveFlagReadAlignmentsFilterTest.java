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
 * SupplementatyAlignmentRemoveFlagReadAlignmentsFilter.
 * @author Laurent Jourdren
 * @since 2.1
 */
public class SupplementaryAlignmentRemoveFlagReadAlignmentsFilterTest {

  private String recordSE1, recordSE2, recordSE3;
  private SAMRecord samRecordSE1, samRecordSE2, samRecordSE3;

  private List<SAMRecord> records;
  private ReadAlignmentsFilter filter;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {

    // recordSE1 mapped
    this.recordSE1 =
        "HWI-EAS285_0001_'':1:1:1260:18686#0/1\t16\tchr4\t129576419\t72\t76M\t*\t0\t0"
            + "\tGACGGATCCGAGANANTGANNTGANAAGAGGNNNNNNNNNNNNNNAATTTGAGGACCNAAGGGATGCAGATGATGC"
            + "\tGEGEGE:@=BB><#7#AA:##@CC#CA9A8;##############=GGGGGDCDCDD#DDCGFGDGGGGBGGGGGG"
            + "\tXA:i:1\tMD:Z:13T1T3C0T3A2C3T0T0T0T0G0G0T0T0T0T0G0T0G0G12C18\tNM:i:21";
    // recordSE2 unmapped
    this.recordSE2 =
        "HWI-EAS285_0001_'':1:1:1259:6203##0/1\t2048\tchr4\t129576419\t72\t76M\t*\t0\t0"
            + "\tGACGGATCCGAGANANTGANNTGANAAGAGGNNNNNNNNNNNNNNAATTTGAGGACCNAAGGGATGCAGATGATGC"
            + "\tGEGEGE:@=BB><#7#AA:##@CC#CA9A8;##############=GGGGGDCDCDD#DDCGFGDGGGGBGGGGGG"
            + "\tXA:i:1\tMD:Z:13T1T3C0T3A2C3T0T0T0T0G0G0T0T0T0T0G0T0G0G12C18\tNM:i:21";
    // recordSE3 unmapped
    this.recordSE3 =
        "HWI-EAS285_0001_'':1:1:1259:6203##0/1\t2064\tchr4\t129576419\t72\t76M\t*\t0\t0"
            + "\tGACGGATCCGAGANANTGANNTGANAAGAGGNNNNNNNNNNNNNNAATTTGAGGACCNAAGGGATGCAGATGATGC"
            + "\tGEGEGE:@=BB><#7#AA:##@CC#CA9A8;##############=GGGGGDCDCDD#DDCGFGDGGGGBGGGGGG"
            + "\tXA:i:1\tMD:Z:13T1T3C0T3A2C3T0T0T0T0G0G0T0T0T0T0G0T0G0G12C18\tNM:i:21";

    final GenomeDescription desc = new GenomeDescription();
    desc.addSequence("chr1", 197195432);
    desc.addSequence("chr4", 155630120);
    desc.addSequence("chr9", 124076172);
    desc.addSequence("chr11", 121843856);
    desc.addSequence("chr15", 103494974);

    SAMLineParser parser = new SAMLineParser(SAMUtils.newSAMFileHeader(desc));

    this.samRecordSE1 = parser.parseLine(this.recordSE1);
    this.samRecordSE2 = parser.parseLine(this.recordSE2);
    this.samRecordSE3 = parser.parseLine(this.recordSE3);

    this.records = new ArrayList<>();

    this.filter = new SupplementaryAlignmentRemoveFlagReadAlignmentsFilter();
  }

  /**
   * Test method for {fr.ens.biologie.genomique.eoulsan.bio.alignmentsfilters.
   * RemoveUnmappedReadAlignmentsFilter#getName()}.
   */
  @Test
  public void testGetName() {
    assertEquals("removesupplementary", this.filter.getName());
  }

  /**
   * Test method for {fr.ens.biologie.genomique.eoulsan.bio.alignmentsfilters.
   * RemoveUnmappedReadAlignmentsFilter#filterReadAlignments(java.util.List,
   * boolean)}.
   */
  @Test
  public void testFilterReadAlignments() {

    List<SAMRecord> recordsVerif = new ArrayList<>();

    // single-end mode
    this.records.add(this.samRecordSE1);
    this.records.add(this.samRecordSE2);
    this.records.add(this.samRecordSE3);
    recordsVerif.add(this.samRecordSE1);
    this.filter.filterReadAlignments(this.records);
    assertEquals(this.records, recordsVerif);

    this.records.clear();
    recordsVerif.clear();
  }
}
