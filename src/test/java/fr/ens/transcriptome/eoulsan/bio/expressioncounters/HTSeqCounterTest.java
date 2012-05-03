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

package fr.ens.transcriptome.eoulsan.bio.expressioncounters;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import net.sf.samtools.SAMParser;
import net.sf.samtools.SAMRecord;

import org.junit.Before;
import org.junit.Test;

import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;

/**
 * @author Claire Wallon
 */
public class HTSeqCounterTest {

  private File dir = new File("/home/wallon/Bureau/GSNAP");
  private File samFileSE = new File(dir, "TEST/junitSE.sam");
  private File samFilePE = new File(dir, "TEST/junitPE.sam");
  private File samHeaderSE = new File(dir, "TEST/headerSE.sam");
  private File samHeaderPE = new File(dir, "TEST/headerPE.sam");
  private File gffFileSE = new File(dir, "annotation.gff");
  private File gffFilePE = new File(dir, "PE/mouse.gff");
  // private File genomeDescSE = new File(dir, "genome_desc_1.txt");
  // private File genomeDescPE = new File(dir, "/PE/genome_desc_1.txt");
  private Writer writer;
  private String recordSE1, recordSE2, recordSE3, recordSE4, recordSE5;
  private String recordPE1, recordPE2, recordPE3;
  private String recordPE4, recordPE5, recordPE6;
  private SAMRecord samSE1, samSE2, samSE3, samSE4, samSE5;
  private SAMRecord samPE1, samPE2, samPE3;
  private SAMRecord samPE4, samPE5, samPE6;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    writer = new FileWriter(samFileSE);

    recordSE1 =
        "HWI-EAS285_0001_'':1:1:1259:6203#0/1   4   *   0   0   *   *   0   0   CGGCCGGACCGACCCCGTNGGGGTCCGACAANNNNNNNNNNNNNNCACANGNACGNNGCANANCCAACCCGAGCGT    GGGFGBGGGEGGGGGDD?#A=>B3BA=BD###############################################";
    recordSE2 =
        "HWI-EAS285_0001_'':1:1:1260:13682#0/1  0   chr16   23360177    38  76M *   0   0   ATTTGCGACAGGTAGTTTNAAATCTGTGACTNNNNNNNNNNNNNNAGTGNCNTTCNNCGTNGNCACTGACGTCACT    GGGGFGGGGFCECEEBCB#ACCCCCGGFGGA##############AA=A#A#A?A##A=?#9#8?CCB>CGEGGGA    MD:Z:76 NH:i:1  HI:i:1  NM:i:0  SM:i:38 XQ:i:40 X2:i:0";
    recordSE3 =
        "HWI-EAS285_0001_'':1:1:1261:14574#0/1  16  chr2    154692627   38  15S61M  *   0   0   GAGCCTTGCAGTAAGCAGCTNAACAGGAGCATTNNNNNNNNNNNNAAGGGCATCACTNTCTCAGGCCTCAAGCCAG    GEGDDGGGFGGDDCDDCDDD#DCDDDCCCCC=2############BGGGGGCCBAAD#DDDEGGGEGGGGGGFGGG    MD:Z:61 NH:i:1  HI:i:1  NM:i:0  SM:i:38 XQ:i:40 X2:i:0";
    // recordSE4 and recordSE5 are a two alignments of the same read
    recordSE4 =
        "HWI-EAS285_0001_'':1:1:1261:16324#0/1  0   chr9    34996845    32  43M33S  *   0   0   TCTTGGGCTTCTTCAACANAGCCTGCCTGAANNNNNNNNNNNNCTTGTCTCGAGGNTTCTTGGACGGAGTGGAGGA    GGGGGFGGGGGGFGGDDD#DDDDCDEGGFB@############=CCC>?CEECEE#BC@BCAC?@EE:D?CCBBB6    MD:Z:43 NH:i:2  HI:i:1  NM:i:0  SM:i:32 XQ:i:40 X2:i:33";
    recordSE5 =
        "HWI-EAS285_0001_'':1:1:1261:16324#0/1  256 chr9    34998597    0   30S46M  *   0   0   TCTTGGGCTTCTTCAACANAGCCTGCCTGAANNNNNNNNNNNNCTTGTCTCGAGGNTTCTTGGACGGAGTGGAGGA    GGGGGFGGGGGGFGGDDD#DDDDCDEGGFB@############=CCC>?CEECEE#BC@BCAC?@EE:D?CCBBB6    MD:Z:46 NH:i:2  HI:i:2  NM:i:0  SM:i:0  XQ:i:33 X2:i:33";

    // final GenomeDescription descSE = new GenomeDescription();
    // descSE.load(genomeDescSE);
    // descSE.addSequence("chr11", 121843856);
    // descSE.addSequence("chr16", 98319150);
    // descSE.addSequence("chr18", 90772031);

    // SAMParser parser = new SAMParser();
    // parser.setGenomeDescription(descSE);
    //
    // samSE1 = parser.parseLine(recordSE1);
    // samSE2 = parser.parseLine(recordSE2);
    // samSE3 = parser.parseLine(recordSE3);
    // samSE4 = parser.parseLine(recordSE4);
    // samSE5 = parser.parseLine(recordSE5);
  }

  /**
   * Test method for
   * {fr.ens.transcriptome.eoulsan.bio.expressioncounters.HTSeqCounter
   * #internalCount(java.io.File, fr.ens.transcriptome.eoulsan.data.DataFile)}.
   */
  @Test
  public void testInternalCount() {
    // fail("Not yet implemented");
  }

  /**
   * Test method for
   * {fr.ens.transcriptome.eoulsan.bio.expressioncounters.HTSeqCounter
   * #getCounterName()}.
   */
  @Test
  public void testGetCounterName() {
    // fail("Not yet implemented");
  }

  /**
   * Test method for
   * {fr.ens.transcriptome.eoulsan.bio.expressioncounters.HTSeqCounter
   * #getCounterVersion()}.
   */
  @Test
  public void testGetCounterVersion() {
    // fail("Not yet implemented");
  }

  /*
   * HWI-1KL110:37:C0BE6ACXX:7:1101:16369:2179 73 chr11 102702988 31 4S95M2S * 0
   * 0
   * TCCANNNNNNNNNNNNNNCAGGTGNNNNNNNNNNNTNCATTCTCTATGTCCTCAGNAAGGCNCTTCTCAAGGGGNTNNNNNNNNNNNNNNNNCTTGTTCTT
   * <<
   * <@##############32@@=?###########0#07=???????????????#-;=??#,9?==?????####
   * ######################### MD:Z:95 NH:i:1 HI:i:1 NM:i:0 SM:i:31 XQ:i:40
   * X2:i:0 HWI-1KL110:37:C0BE6ACXX:7:1101:16369:2179 133 * 0 0 * chr11
   * 102702988 0
   * NNGTTGTGNNNNNNTNNNNNNNNNNNNNNGAAACGTCNNNNNNNANATGCTNTGCNNNGNNNNNNNNNNNNAAGAACAAGATCACCATGATTGCCGAGCCC
   * ##44BDDF######2##############10?FFHGG#######-#-5BED#-5;###,############,,8@
   * BDDDDDDDDDDDDDDDEDDDDDDBDD HWI-1KL110:37:C0BE6ACXX:7:1101:4280:2413 81 chr1
   * 175831751 3 13S88M = 175829588 0
   * TTTTCACTTTTTTCTTGTGTAATGTAAACACTCCATAGAACACTGTTCCAGAAGTACCCTTTTGAATATCACAGATCTTAGGACTGGCATTTGCATTTCTG
   * DDEDCCDDEEFFFFFHHHHHJJJIHDIGGHJIIJJJJJJIIJIJJIJHJJJJJIHDJJJJJJJJJJJJJJHJJJJJJJJJJJJJJJJJHHHHHFFFFFCCC
   * MD:Z:88 NH:i:2 HI:i:1 NM:i:0 SM:i:3 XQ:i:40 X2:i:40
   * HWI-1KL110:37:C0BE6ACXX:7:1101:4280:2413 337 chr1 175947365 3 13S88M =
   * 175829588 0
   * TTTTCACTTTTTTCTTGTGTAATGTAAACACTCCATAGAACACTGTTCCAGAAGTACCCTTTTGAATATCACAGATCTTAGGACTGGCATTTGCATTTCTG
   * DDEDCCDDEEFFFFFHHHHHJJJIHDIGGHJIIJJJJJJIIJIJJIJHJJJJJIHDJJJJJJJJJJJJJJHJJJJJJJJJJJJJJJJJHHHHHFFFFFCCC
   * MD:Z:88 NH:i:2 HI:i:2 NM:i:0 SM:i:3 XQ:i:40 X2:i:40
   * HWI-1KL110:37:C0BE6ACXX:7:1101:4280:2413 161 chr1 175829588 40 78M23S =
   * 175831751 0
   * GTCCACTCCCCACAACTTCTATGCTTCCTGAACCATCTTTTATTTCATAGCTTGTGTTCTGTGTTTTCACTTTTTTCTTGTGTAATGTAAACACTCCATAG
   * CCCFFFFFHHHHHJJJJJJJJJJJJJJJJJJJJJIJJJJJJIJJJJJJJJGHJJJJJIJJJJJJJJJJIJJJJJJFFFFFFEEEEFEFEDDDDDDDDDDDC
   * MD:Z:78 NH:i:1 HI:i:1 NM:i:0 SM:i:40 XQ:i:40 X2:i:0
   * HWI-1KL110:37:C0BE6ACXX:7:1101:16619:2182 83 chr9 107518023 40 101M =
   * 107517997 -127
   * GAACACAAANTNNNNNNNNNNNNNGTNGGTGGCGCACGTGAAGTGCGTGTAGATCTCCTTGGTGTCTNNNNNNNNNTTTAGATNNNNNNANNNNNNNTGGA
   * #
   * ################################??????????????????????????????=<00#########
   * @@?@@23######3#######@<<< MD:Z:101 NH:i:1 HI:i:1 NM:i:0 SM:i:40 XQ:i:40
   * X2:i:0 HWI-1KL110:37:C0BE6ACXX:7:1101:16619:2182 163 chr9 107517997 40 101M
   * = 107518023 127
   * NNGATGATGNNNNNAGTNANNNNNNNNNNCACAAACTNNNNGNNCTTGGTGTCGGNGNCGCNNNNNNNGTGCGTGTAGATCTCCTTGGTGTCTTTGCGCTT
   * ##1=BDFFH#####33A#3##########11?FHIII####0##--BFHFCHHIH#-#,5=#######,,8?
   * BDDDDDEEDEDDDDDD@BDDDDDDDDDDD MD:Z:101 NH:i:1 HI:i:1 NM:i:0 SM:i:40 XQ:i:40
   * X2:i:0
   */
}
