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

package fr.ens.transcriptome.eoulsan;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fr.ens.transcriptome.eoulsan.bio.AlignResult;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;

public class AlignResultTest {

  final AlignResult ar = new AlignResult();

  // @Before
  // public void setUp() throws Exception {
  //
  // ar
  // .parseResultLine("HWI-EAS285:7:1:24:1771#0/1\tCATTAACCAATTCTGTAACCAACGAATCTACTCGCG\t"
  // + "^^aa]^B[^b`abbaaaabbbabbbabbbababbba\t"
  // + "1\ta\t36\t-\tCa21chr2\t" + "2172942\t0\t36M\t36");
  //
  // }

  @Test
  public void testGetSequenceId() throws BadBioEntryException {

    ar
        .parseResultLine("HWI-EAS285:7:1:24:1771#0/1\tCATTAACCAATTCTGTAACCAACGAATCTACTCGCG\t"
            + "^^aa]^B[^b`abbaaaabbbabbbabbbababbba\t"
            + "1\ta\t36\t-\tCa21chr2\t" + "2172942\t0\t36M\t36");
    assertEquals("HWI-EAS285:7:1:24:1771#0/1", ar.getSequenceId());
  }

  @Test
  public void testGetSequence() throws BadBioEntryException {

    ar
        .parseResultLine("HWI-EAS285:7:1:24:1771#0/1\tCATTAACCAATTCTGTAACCAACGAATCTACTCGCG\t"
            + "^^aa]^B[^b`abbaaaabbbabbbabbbababbba\t"
            + "1\ta\t36\t-\tCa21chr2\t" + "2172942\t0\t36M\t36");
    assertEquals("CATTAACCAATTCTGTAACCAACGAATCTACTCGCG", ar.getSequence());
  }

  @Test
  public void testGetQuality() throws BadBioEntryException {

    ar
        .parseResultLine("HWI-EAS285:7:1:24:1771#0/1\tCATTAACCAATTCTGTAACCAACGAATCTACTCGCG\t"
            + "^^aa]^B[^b`abbaaaabbbabbbabbbababbba\t"
            + "1\ta\t36\t-\tCa21chr2\t" + "2172942\t0\t36M\t36");
    assertEquals("^^aa]^B[^b`abbaaaabbbabbbabbbababbba", ar.getQuality());
  }

  @Test
  public void testGetNumberOfHits() throws BadBioEntryException {

    ar
        .parseResultLine("HWI-EAS285:7:1:24:1771#0/1\tCATTAACCAATTCTGTAACCAACGAATCTACTCGCG\t"
            + "^^aa]^B[^b`abbaaaabbbabbbabbbababbba\t"
            + "1\ta\t36\t-\tCa21chr2\t" + "2172942\t0\t36M\t36");
    assertEquals(1, ar.getNumberOfHits());

    ar.parseResultLine("HWI-EAS285:7:1:33:1613#0/1\t"
        + "GAAAAAGAAACTTGACGAATTGAAAGTCAAAGAAGA\t"
        + "abbbba[abbbbb_bb_bbb`Zab`W`aba`Q``^a\t" + "8\ta\t36\t+\t"
        + "Ca21chr3\t14007\t0\t" + "36M\t36");
    assertEquals(8, ar.getNumberOfHits());

  }

  @Test
  public void testGetReadLength() throws BadBioEntryException {

    ar
        .parseResultLine("HWI-EAS285:7:1:24:1771#0/1\tCATTAACCAATTCTGTAACCAACGAATCTACTCGCG\t"
            + "^^aa]^B[^b`abbaaaabbbabbbabbbababbba\t"
            + "1\ta\t36\t-\tCa21chr2\t" + "2172942\t0\t36M\t36");
    assertEquals(36, ar.getReadLength());
  }

  @Test
  public void testGetPairendFlag() throws BadBioEntryException {

    ar
        .parseResultLine("HWI-EAS285:7:1:24:1771#0/1\tCATTAACCAATTCTGTAACCAACGAATCTACTCGCG\t"
            + "^^aa]^B[^b`abbaaaabbbabbbabbbababbba\t"
            + "1\ta\t36\t-\tCa21chr2\t" + "2172942\t0\t36M\t36");
    assertEquals('a', ar.getPairendFlag());

  }

  @Test
  public void testIsDirectStrand() throws BadBioEntryException {

    ar
        .parseResultLine("HWI-EAS285:7:1:24:1771#0/1\tCATTAACCAATTCTGTAACCAACGAATCTACTCGCG\t"
            + "^^aa]^B[^b`abbaaaabbbabbbabbbababbba\t"
            + "1\ta\t36\t-\tCa21chr2\t" + "2172942\t0\t36M\t36");
    assertEquals(false, ar.isDirectStrand());

    ar.parseResultLine("HWI-EAS285:7:1:33:1613#0/1\t"
        + "GAAAAAGAAACTTGACGAATTGAAAGTCAAAGAAGA\t"
        + "abbbba[abbbbb_bb_bbb`Zab`W`aba`Q``^a\t" + "8\ta\t36\t+\t"
        + "Ca21chr3\t14007\t0\t" + "36M\t36");
    assertEquals(true, ar.isDirectStrand());
  }

  @Test
  public void testGetChromosome() throws BadBioEntryException {

    ar
        .parseResultLine("HWI-EAS285:7:1:24:1771#0/1\tCATTAACCAATTCTGTAACCAACGAATCTACTCGCG\t"
            + "^^aa]^B[^b`abbaaaabbbabbbabbbababbba\t"
            + "1\ta\t36\t-\tCa21chr2\t" + "2172942\t0\t36M\t36");
    assertEquals("Ca21chr2", ar.getChromosome());
  }

  @Test
  public void testGetLocation() throws BadBioEntryException {

    ar
        .parseResultLine("HWI-EAS285:7:1:24:1771#0/1\tCATTAACCAATTCTGTAACCAACGAATCTACTCGCG\t"
            + "^^aa]^B[^b`abbaaaabbbabbbabbbababbba\t"
            + "1\ta\t36\t-\tCa21chr2\t" + "2172942\t0\t36M\t36");
    assertEquals(2172942, ar.getLocation());
  }

  @Test
  public void testGetHitType() throws BadBioEntryException {

    ar
        .parseResultLine("HWI-EAS285:7:1:24:1771#0/1\tCATTAACCAATTCTGTAACCAACGAATCTACTCGCG\t"
            + "^^aa]^B[^b`abbaaaabbbabbbabbbababbba\t"
            + "1\ta\t36\t-\tCa21chr2\t" + "2172942\t0\t36M\t36");
    assertEquals(0, ar.getHitType());

    ar.parseResultLine("HWI-EAS285:7:1:35:910#0/1\t"
        + "TAATTTTAGCCATTGGGTTTTATCTAGCTAAACGAA\t"
        + "BBBBBB[b`__aaaaaaaabbaa^`baZ^bba_baa\t"
        + "1\ta\t36\t-\tCa21chr2\t476727\t2\tC->1A2\tA->5T2\t36M\t1C3A30");
    assertEquals(2, ar.getHitType());
  }

}
