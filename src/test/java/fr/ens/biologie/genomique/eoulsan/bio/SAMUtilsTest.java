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

package fr.ens.biologie.genomique.eoulsan.bio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;

public class SAMUtilsTest {

  @Test
  public void readSAMHeaderTest() {

    final InputStream is =
        SAMUtilsTest.class.getResourceAsStream("/mapper_results_SE.sam");

    String s = "@HD\tVN:1.5\n"
        + "@SQ\tSN:chr1\tLN:197195432\n" + "@SQ\tSN:chr2\tLN:181748087\n"
        + "@SQ\tSN:chr3\tLN:159599783\n" + "@SQ\tSN:chr4\tLN:155630120\n"
        + "@SQ\tSN:chr5\tLN:152537259\n" + "@SQ\tSN:chr6\tLN:149517037\n"
        + "@SQ\tSN:chr7\tLN:152524553\n" + "@SQ\tSN:chr8\tLN:131738871\n"
        + "@SQ\tSN:chr9\tLN:124076172\n" + "@SQ\tSN:chr10\tLN:129993255\n"
        + "@SQ\tSN:chr11\tLN:121843856\n" + "@SQ\tSN:chr12\tLN:121257530\n"
        + "@SQ\tSN:chr13\tLN:120284312\n" + "@SQ\tSN:chr14\tLN:125194864\n"
        + "@SQ\tSN:chr15\tLN:103494974\n" + "@SQ\tSN:chr16\tLN:98319150\n"
        + "@SQ\tSN:chr17\tLN:95272651\n" + "@SQ\tSN:chr18\tLN:90772031\n"
        + "@SQ\tSN:chr19\tLN:61342430\n" + "@SQ\tSN:chrX\tLN:166650296\n"
        + "@SQ\tSN:chrY\tLN:15902555\n" + "@SQ\tSN:chrMT\tLN:16299\n";

    assertEquals(s, SAMUtils.readSAMHeader(is));
  }

  @Test
  public void createGenomeDescriptionFromSAM() {

    final InputStream is =
        SAMUtilsTest.class.getResourceAsStream("/mapper_results_SE.sam");

    final GenomeDescription desc = SAMUtils.createGenomeDescriptionFromSAM(is);

    assertEquals(22, desc.getSequenceCount());
    assertFalse(desc.containsSequence("chr102"));

    assertTrue(desc.containsSequence("chr1"));
    assertNotSame(197195431, desc.getSequenceLength("chr1"));
    assertEquals(197195432, desc.getSequenceLength("chr1"));
    assertNotSame(197195433, desc.getSequenceLength("chr1"));

    assertEquals(197195432, desc.getSequenceLength("chr1"));
    assertEquals(181748087, desc.getSequenceLength("chr2"));
    assertEquals(159599783, desc.getSequenceLength("chr3"));
    assertEquals(155630120, desc.getSequenceLength("chr4"));
    assertEquals(152537259, desc.getSequenceLength("chr5"));
    assertEquals(149517037, desc.getSequenceLength("chr6"));
    assertEquals(152524553, desc.getSequenceLength("chr7"));
    assertEquals(131738871, desc.getSequenceLength("chr8"));
    assertEquals(124076172, desc.getSequenceLength("chr9"));
    assertEquals(129993255, desc.getSequenceLength("chr10"));
    assertEquals(121843856, desc.getSequenceLength("chr11"));
    assertEquals(121257530, desc.getSequenceLength("chr12"));
    assertEquals(120284312, desc.getSequenceLength("chr13"));
    assertEquals(125194864, desc.getSequenceLength("chr14"));
    assertEquals(103494974, desc.getSequenceLength("chr15"));
    assertEquals(98319150, desc.getSequenceLength("chr16"));
    assertEquals(95272651, desc.getSequenceLength("chr17"));
    assertEquals(90772031, desc.getSequenceLength("chr18"));
    assertEquals(61342430, desc.getSequenceLength("chr19"));
    assertEquals(166650296, desc.getSequenceLength("chrX"));
    assertEquals(15902555, desc.getSequenceLength("chrY"));
    assertEquals(16299, desc.getSequenceLength("chrMT"));
  }

  @Test
  public void newSAMFileHeader() {

    final GenomeDescription desc = new GenomeDescription();

    desc.addSequence("chr1", 197195432);
    desc.addSequence("chr2", 181748087);
    desc.addSequence("chr3", 159599783);

    final SAMFileHeader header = SAMUtils.newSAMFileHeader(desc);

    assertEquals(3, header.getSequenceDictionary().size());

    assertNotSame(197195431, header.getSequence("chr1").getSequenceLength());
    assertEquals(197195432, header.getSequence("chr1").getSequenceLength());
    assertNotSame(197195433, header.getSequence("chr1").getSequenceLength());

    assertEquals(181748087, header.getSequence("chr2").getSequenceLength());
    assertEquals(159599783, header.getSequence("chr3").getSequenceLength());
  }

  @Test
  public void newSAMSequenceDictionaryTest() {

    final GenomeDescription desc = new GenomeDescription();

    desc.addSequence("chr1", 197195432);
    desc.addSequence("chr2", 181748087);
    desc.addSequence("chr3", 159599783);

    final SAMSequenceDictionary dict = SAMUtils.newSAMSequenceDictionary(desc);

    assertEquals(3, dict.size());

    assertNotSame(197195431, dict.getSequence("chr1").getSequenceLength());
    assertEquals(197195432, dict.getSequence("chr1").getSequenceLength());
    assertNotSame(197195433, dict.getSequence("chr1").getSequenceLength());

    assertEquals(181748087, dict.getSequence("chr2").getSequenceLength());
    assertEquals(159599783, dict.getSequence("chr3").getSequenceLength());
  }

  @Test
  public void createGenomeDescriptionFromSAMTest() {

    final SAMFileHeader header = new SAMFileHeader();
    GenomeDescription desc = SAMUtils.createGenomeDescriptionFromSAM(header);

    assertEquals(0, desc.getSequenceCount());

    final List<SAMSequenceRecord> sequences = new ArrayList<>();

    sequences.add(new SAMSequenceRecord("chr1", 197195432));
    sequences.add(new SAMSequenceRecord("chr2", 181748087));
    sequences.add(new SAMSequenceRecord("chr3", 159599783));

    header.setSequenceDictionary(new SAMSequenceDictionary(sequences));

    desc = SAMUtils.createGenomeDescriptionFromSAM(header);

    assertEquals(3, desc.getSequenceCount());
    assertEquals(197195432, desc.getSequenceLength("chr1"));
    assertEquals(181748087, desc.getSequenceLength("chr2"));
    assertEquals(159599783, desc.getSequenceLength("chr3"));
  }

}
