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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMParser;
import net.sf.samtools.SAMRecord;

import org.junit.Test;

import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;

public class RemoveMultiMatchesReadAlignmentsFilterTest {

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

    try {
      filter.filterReadAlignments(null);
      assertTrue(true);
    } catch (Exception e) {
      assertTrue(false);
    }

    records.add(r1);
    assertEquals(1, records.size());
    filter.filterReadAlignments(records);
    assertEquals(1, records.size());

    records.add(r2);
    records.add(r3);
    assertEquals(3, records.size());
    filter.filterReadAlignments(records);
    assertEquals(0, records.size());
  }

}
