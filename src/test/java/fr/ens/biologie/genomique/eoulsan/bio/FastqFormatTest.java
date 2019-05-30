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

import static fr.ens.biologie.genomique.eoulsan.bio.FastqFormat.FASTQ_ILLUMINA;
import static fr.ens.biologie.genomique.eoulsan.bio.FastqFormat.FASTQ_ILLUMINA_1_5;
import static fr.ens.biologie.genomique.eoulsan.bio.FastqFormat.FASTQ_SANGER;
import static fr.ens.biologie.genomique.eoulsan.bio.FastqFormat.FASTQ_SOLEXA;
import static fr.ens.biologie.genomique.eoulsan.bio.FastqFormat.convertPhredSCoreToSolexaScore;
import static fr.ens.biologie.genomique.eoulsan.bio.FastqFormat.convertSolexaScoreToPhredScore;
import static fr.ens.biologie.genomique.eoulsan.bio.FastqFormat.getFormatFromName;
import static fr.ens.biologie.genomique.eoulsan.bio.FastqFormat.identifyFormat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

public class FastqFormatTest {

  @Test
  public void testGetName() {

    assertEquals("fastq-sanger", FASTQ_SANGER.getName());
    assertEquals("fastq-solexa", FASTQ_SOLEXA.getName());
    assertEquals("fastq-illumina-1.3", FASTQ_ILLUMINA.getName());
    assertEquals("fastq-illumina-1.5", FASTQ_ILLUMINA_1_5.getName());
  }

  @Test
  public void testGetFormatFromName() {

    assertNull(getFormatFromName(null));
    assertNull(getFormatFromName("toto"));

    assertEquals(FASTQ_SANGER, getFormatFromName("fastq-sanger"));
    assertEquals(FASTQ_SANGER, getFormatFromName("sanger"));
    assertEquals(FASTQ_SANGER, getFormatFromName("Illumina-1.8"));
    assertEquals(FASTQ_SANGER, getFormatFromName("1.8"));

    assertEquals(FASTQ_SOLEXA, getFormatFromName("fastq-solexa"));
    assertEquals(FASTQ_SOLEXA, getFormatFromName("solexa"));
    assertEquals(FASTQ_SOLEXA, getFormatFromName("1.0"));

    assertEquals(FASTQ_ILLUMINA, getFormatFromName("fastq-illumina-1.3"));
    assertEquals(FASTQ_ILLUMINA, getFormatFromName("illumina-1.3"));
    assertEquals(FASTQ_ILLUMINA, getFormatFromName("fastq-illumina"));
    assertEquals(FASTQ_ILLUMINA, getFormatFromName("illumina"));
    assertEquals(FASTQ_ILLUMINA, getFormatFromName("1.3"));

    assertEquals(FASTQ_ILLUMINA_1_5, getFormatFromName("fastq-illumina-1.5"));
    assertEquals(FASTQ_ILLUMINA_1_5, getFormatFromName("illumina-1.5"));
    assertEquals(FASTQ_ILLUMINA_1_5, getFormatFromName("1.5"));

  }

  @Test
  public void testGetIlluminaVersion() {

    assertEquals("1.0", FASTQ_SOLEXA.getIlluminaVersion());
    assertEquals("1.3", FASTQ_ILLUMINA.getIlluminaVersion());
    assertEquals("1.5", FASTQ_ILLUMINA_1_5.getIlluminaVersion());
    assertEquals("1.8", FASTQ_SANGER.getIlluminaVersion());
  }

  @Test
  public void testGetScoreMin() {

    assertEquals(0, FASTQ_SANGER.getScoreMin());
    assertEquals(-5, FASTQ_SOLEXA.getScoreMin());
    assertEquals(0, FASTQ_ILLUMINA.getScoreMin());
    assertEquals(2, FASTQ_ILLUMINA_1_5.getScoreMin());
  }

  @Test
  public void testGetScoreMax() {

    assertEquals(93, FASTQ_SANGER.getScoreMax());
    assertEquals(62, FASTQ_SOLEXA.getScoreMax());
    assertEquals(62, FASTQ_ILLUMINA.getScoreMax());
    assertEquals(62, FASTQ_ILLUMINA_1_5.getScoreMax());
  }

  @Test
  public void testGetCharMin() {

    assertEquals('!', FASTQ_SANGER.getCharMin());
    assertEquals(';', FASTQ_SOLEXA.getCharMin());
    assertEquals('@', FASTQ_ILLUMINA.getCharMin());
    assertEquals('B', FASTQ_ILLUMINA_1_5.getCharMin());
  }

  @Test
  public void testGetCharMax() {

    assertEquals('~', FASTQ_SANGER.getCharMax());
    assertEquals('~', FASTQ_SOLEXA.getCharMax());
    assertEquals('~', FASTQ_ILLUMINA.getCharMax());
    assertEquals('~', FASTQ_ILLUMINA_1_5.getCharMax());
  }

  @Test
  public void testGetCharMaxExpected() {

    assertEquals('I', FASTQ_SANGER.getCharMaxExpected());
    assertEquals('h', FASTQ_SOLEXA.getCharMaxExpected());
    assertEquals('h', FASTQ_ILLUMINA.getCharMaxExpected());
    assertEquals('h', FASTQ_ILLUMINA_1_5.getCharMaxExpected());
  }

  @Test
  public void testGetOffset() {

    assertEquals(33, FASTQ_SANGER.getAsciiOffset());
    assertEquals(64, FASTQ_ILLUMINA.getAsciiOffset());
    assertEquals(64, FASTQ_ILLUMINA_1_5.getAsciiOffset());
  }

  @Test
  public void testGetScore() {
    assertEquals(0, FASTQ_SANGER.getScore('!'));
    assertEquals(0, FASTQ_ILLUMINA.getScore('@'));
    assertEquals(0, FASTQ_ILLUMINA_1_5.getScore('@'));
  }

  @Test
  public void testConvertScoreToProbability() {

    // assertEquals(1, FASTQ_SANGER.convertQualityToProbability(0), 0.01);
    assertEquals(0.1, FASTQ_SANGER.convertScoreToProbability(10), 0.01);
    assertEquals(0.01, FASTQ_SANGER.convertScoreToProbability(20), 0.01);
    assertEquals(0.001, FASTQ_SANGER.convertScoreToProbability(30), 0.01);
    assertEquals(0.0001, FASTQ_SANGER.convertScoreToProbability(40), 0.01);
    assertEquals(0.00001, FASTQ_SANGER.convertScoreToProbability(50), 0.01);
    assertEquals(0.000001, FASTQ_SANGER.convertScoreToProbability(60), 0.01);
    assertEquals(0.0000001, FASTQ_SANGER.convertScoreToProbability(70), 0.01);
    assertEquals(0.00000001, FASTQ_SANGER.convertScoreToProbability(80), 0.01);
    assertEquals(0.000000001, FASTQ_SANGER.convertScoreToProbability(90), 0.01);

    for (int i = 0; i <= 90; i += 10) {
      assertEquals(i, convertToProbaToScore(FASTQ_SANGER, i), 0.01);
    }

  }

  @Test
  public void testConvertProbabilitytoScore() {

    assertEquals(Double.POSITIVE_INFINITY,
        FASTQ_SANGER.convertProbabilityToScore(0.0), 0.01);
    assertEquals(10, FASTQ_SANGER.convertProbabilityToScore(0.1), 0.01);
    assertEquals(20, FASTQ_SANGER.convertProbabilityToScore(0.01), 0.01);
    assertEquals(30, FASTQ_SANGER.convertProbabilityToScore(0.001), 0.01);
    assertEquals(40, FASTQ_SANGER.convertProbabilityToScore(0.0001), 0.01);
    assertEquals(50, FASTQ_SANGER.convertProbabilityToScore(0.00001), 0.01);
    assertEquals(60, FASTQ_SANGER.convertProbabilityToScore(0.000001), 0.01);
    assertEquals(70, FASTQ_SANGER.convertProbabilityToScore(0.0000001), 0.01);
    assertEquals(80, FASTQ_SANGER.convertProbabilityToScore(0.00000001), 0.01);
    assertEquals(90, FASTQ_SANGER.convertProbabilityToScore(0.000000001), 0.01);

    for (int i = 0; i <= 90; i += 10) {
      assertEquals(i, convertToProbaToScore(FASTQ_SANGER, i), 0.01);
    }

    for (int i = 0; i <= 90; i += 10) {
      assertEquals(i, convertToProbaToScore(FASTQ_ILLUMINA, i), 0.01);
    }

  }

  private double convertToProbaToScore(final FastqFormat f, final int q) {

    final double proba = f.convertScoreToProbability(q);

    return f.convertProbabilityToScore(proba);
  }

  private double phredScoreToSolexaScore(final double s) {

    return FASTQ_SOLEXA.convertProbabilityToScore(
        FASTQ_SANGER.convertScoreToProbability((int) Math.round(s)));
  }

  private double solexaScoreToPhredScore(final double s) {

    return FASTQ_SANGER.convertProbabilityToScore(
        FASTQ_SOLEXA.convertScoreToProbability((int) Math.round(s)));
  }

  @Test
  public void testConvertPhredScoreToSolexaScore() {

    try {
      FastqFormat.convertPhredSCoreToSolexaScore(-1);
      fail();
    } catch (IllegalArgumentException e) {
      assertTrue(true);
    }

    assertEquals(80.00, FastqFormat.convertPhredSCoreToSolexaScore(80), 0.01);
    assertEquals(50.00, FastqFormat.convertPhredSCoreToSolexaScore(50), 0.01);
    assertEquals(19.96, FastqFormat.convertPhredSCoreToSolexaScore(20), 0.01);
    assertEquals(9.54, FastqFormat.convertPhredSCoreToSolexaScore(10), 0.01);
    assertEquals(3.35, FastqFormat.convertPhredSCoreToSolexaScore(5), 0.01);
    assertEquals(1.80, FastqFormat.convertPhredSCoreToSolexaScore(4), 0.01);
    assertEquals(-0.02, FastqFormat.convertPhredSCoreToSolexaScore(3), 0.01);
    assertEquals(-2.33, FastqFormat.convertPhredSCoreToSolexaScore(2), 0.01);
    assertEquals(-5.00, FastqFormat.convertPhredSCoreToSolexaScore(1), 0.0);
    assertEquals(-5.00, FastqFormat.convertPhredSCoreToSolexaScore(0), 0.0);

    assertEquals(phredScoreToSolexaScore(80),
        convertPhredSCoreToSolexaScore(80), 0.9);
    assertEquals(phredScoreToSolexaScore(50),
        convertPhredSCoreToSolexaScore(50), 0.9);
    assertEquals(phredScoreToSolexaScore(20),
        convertPhredSCoreToSolexaScore(20), 0.9);
    assertEquals(phredScoreToSolexaScore(10),
        convertPhredSCoreToSolexaScore(10), 0.9);
    assertEquals(phredScoreToSolexaScore(5), convertPhredSCoreToSolexaScore(5),
        0.9);
    assertEquals(phredScoreToSolexaScore(4), convertPhredSCoreToSolexaScore(4),
        0.9);
    assertEquals(phredScoreToSolexaScore(3), convertPhredSCoreToSolexaScore(3),
        0.9);
    assertEquals(phredScoreToSolexaScore(2), convertPhredSCoreToSolexaScore(2),
        0.9);
    assertEquals(phredScoreToSolexaScore(1), convertPhredSCoreToSolexaScore(1),
        0.9);
    // assertEquals(phredToSolexa(0), convertPhredQualityToSolexa(0), 0.9);
  }

  @Test
  public void testConvertSolexaScoreToPhredScore() {

    try {
      FastqFormat.convertSolexaScoreToPhredScore(-6);
      fail();
    } catch (IllegalArgumentException e) {
      assertFalse(false);
    }

    assertEquals(80.00, FastqFormat.convertSolexaScoreToPhredScore(80), 0.01);
    assertEquals(20.04, FastqFormat.convertSolexaScoreToPhredScore(20), 0.01);
    assertEquals(10.41, FastqFormat.convertSolexaScoreToPhredScore(10), 0.01);
    assertEquals(3.01, FastqFormat.convertSolexaScoreToPhredScore(0), 0.01);
    assertEquals(1.19, FastqFormat.convertSolexaScoreToPhredScore(-5), 0.01);

    assertEquals(solexaScoreToPhredScore(80),
        convertSolexaScoreToPhredScore(80), 0.9);
    assertEquals(solexaScoreToPhredScore(20),
        convertSolexaScoreToPhredScore(20), 0.9);
    assertEquals(solexaScoreToPhredScore(10),
        convertSolexaScoreToPhredScore(10), 0.9);
    assertEquals(solexaScoreToPhredScore(0), convertSolexaScoreToPhredScore(0),
        0.9);
    assertEquals(solexaScoreToPhredScore(-5),
        convertSolexaScoreToPhredScore(-5), 0.9);
  }

  @Test
  public void testConvertScoreTo() {

    assertEquals(0, FASTQ_SANGER.convertScoreTo(0, FASTQ_SANGER));
    assertEquals(1, FASTQ_SANGER.convertScoreTo(1, FASTQ_SANGER));
    assertEquals(2, FASTQ_SANGER.convertScoreTo(2, FASTQ_SANGER));
    assertEquals(3, FASTQ_SANGER.convertScoreTo(3, FASTQ_SANGER));
    assertEquals(4, FASTQ_SANGER.convertScoreTo(4, FASTQ_SANGER));
    assertEquals(5, FASTQ_SANGER.convertScoreTo(5, FASTQ_SANGER));
    assertEquals(6, FASTQ_SANGER.convertScoreTo(6, FASTQ_SANGER));
    assertEquals(7, FASTQ_SANGER.convertScoreTo(7, FASTQ_SANGER));
    assertEquals(8, FASTQ_SANGER.convertScoreTo(8, FASTQ_SANGER));
    assertEquals(9, FASTQ_SANGER.convertScoreTo(9, FASTQ_SANGER));
    assertEquals(10, FASTQ_SANGER.convertScoreTo(10, FASTQ_SANGER));

    assertEquals(-5, FASTQ_SOLEXA.convertScoreTo(-5, FASTQ_SOLEXA));
    assertEquals(-2, FASTQ_SOLEXA.convertScoreTo(-2, FASTQ_SOLEXA));
    assertEquals(0, FASTQ_SOLEXA.convertScoreTo(0, FASTQ_SOLEXA));
    assertEquals(2, FASTQ_SOLEXA.convertScoreTo(2, FASTQ_SOLEXA));
    assertEquals(3, FASTQ_SOLEXA.convertScoreTo(3, FASTQ_SOLEXA));
    assertEquals(5, FASTQ_SOLEXA.convertScoreTo(5, FASTQ_SOLEXA));
    assertEquals(6, FASTQ_SOLEXA.convertScoreTo(6, FASTQ_SOLEXA));
    assertEquals(7, FASTQ_SOLEXA.convertScoreTo(7, FASTQ_SOLEXA));
    assertEquals(8, FASTQ_SOLEXA.convertScoreTo(8, FASTQ_SOLEXA));
    assertEquals(10, FASTQ_SOLEXA.convertScoreTo(10, FASTQ_SOLEXA));

    assertEquals(-5, FASTQ_SANGER.convertScoreTo(0, FASTQ_SOLEXA));
    assertEquals(-5, FASTQ_SANGER.convertScoreTo(1, FASTQ_SOLEXA));
    assertEquals(-2, FASTQ_SANGER.convertScoreTo(2, FASTQ_SOLEXA));
    assertEquals(0, FASTQ_SANGER.convertScoreTo(3, FASTQ_SOLEXA));
    assertEquals(2, FASTQ_SANGER.convertScoreTo(4, FASTQ_SOLEXA));
    assertEquals(3, FASTQ_SANGER.convertScoreTo(5, FASTQ_SOLEXA));
    assertEquals(5, FASTQ_SANGER.convertScoreTo(6, FASTQ_SOLEXA));
    assertEquals(6, FASTQ_SANGER.convertScoreTo(7, FASTQ_SOLEXA));
    assertEquals(7, FASTQ_SANGER.convertScoreTo(8, FASTQ_SOLEXA));
    assertEquals(8, FASTQ_SANGER.convertScoreTo(9, FASTQ_SOLEXA));
    assertEquals(10, FASTQ_SANGER.convertScoreTo(10, FASTQ_SOLEXA));

    for (int i = 11; i <= 40; i++) {
      assertEquals(i, FASTQ_SANGER.convertScoreTo(i, FASTQ_SOLEXA));
    }

    assertEquals(1, FASTQ_SOLEXA.convertScoreTo(-5, FASTQ_SANGER));
    assertEquals(1, FASTQ_SOLEXA.convertScoreTo(-4, FASTQ_SANGER));
    assertEquals(2, FASTQ_SOLEXA.convertScoreTo(-3, FASTQ_SANGER));
    assertEquals(2, FASTQ_SOLEXA.convertScoreTo(-2, FASTQ_SANGER));
    assertEquals(3, FASTQ_SOLEXA.convertScoreTo(-1, FASTQ_SANGER));
    assertEquals(3, FASTQ_SOLEXA.convertScoreTo(0, FASTQ_SANGER));
    assertEquals(4, FASTQ_SOLEXA.convertScoreTo(1, FASTQ_SANGER));
    assertEquals(4, FASTQ_SOLEXA.convertScoreTo(2, FASTQ_SANGER));
    assertEquals(5, FASTQ_SOLEXA.convertScoreTo(3, FASTQ_SANGER));
    assertEquals(5, FASTQ_SOLEXA.convertScoreTo(4, FASTQ_SANGER));
    assertEquals(6, FASTQ_SOLEXA.convertScoreTo(5, FASTQ_SANGER));
    assertEquals(7, FASTQ_SOLEXA.convertScoreTo(6, FASTQ_SANGER));
    assertEquals(8, FASTQ_SOLEXA.convertScoreTo(7, FASTQ_SANGER));
    assertEquals(9, FASTQ_SOLEXA.convertScoreTo(8, FASTQ_SANGER));
    assertEquals(10, FASTQ_SOLEXA.convertScoreTo(9, FASTQ_SANGER));
    assertEquals(10, FASTQ_SOLEXA.convertScoreTo(10, FASTQ_SANGER));

    for (int i = 11; i <= 40; i++) {
      assertEquals(i, FASTQ_SOLEXA.convertScoreTo(i, FASTQ_SANGER));
    }
  }

  private String rangeCharacters(final int min, final int max) {

    final StringBuilder sb = new StringBuilder();

    for (int i = min; i <= max; i++) {
      sb.append((char) i);
    }

    return sb.toString();
  }

  @Test
  public void testIdentifyFormat() {

    assertNull(identifyFormat((String) null));

    assertEquals(FASTQ_SANGER, identifyFormat(rangeCharacters(33, 73)));
    assertEquals(FASTQ_SOLEXA, identifyFormat(rangeCharacters(59, 104)));
    assertEquals(FASTQ_ILLUMINA, identifyFormat(rangeCharacters(64, 104)));
    assertEquals(FASTQ_ILLUMINA_1_5, identifyFormat(rangeCharacters(66, 104)));

    assertEquals(FASTQ_SANGER, identifyFormat(rangeCharacters(33, 126)));
    assertEquals(FASTQ_SOLEXA, identifyFormat(rangeCharacters(59, 126)));
    assertEquals(FASTQ_ILLUMINA, identifyFormat(rangeCharacters(64, 126)));
    assertEquals(FASTQ_ILLUMINA_1_5, identifyFormat(rangeCharacters(66, 126)));

    for (FastqFormat f : FastqFormat.values()) {
      assertEquals(f,
          identifyFormat(rangeCharacters(f.getCharMin(), f.getCharMax())));
    }

    for (FastqFormat f : FastqFormat.values()) {
      assertEquals(f,
          identifyFormat(rangeCharacters(f.getCharMin() + 1, f.getCharMax())));
    }

    assertNull(identifyFormat(rangeCharacters(32, 104)));
    assertNull(identifyFormat(rangeCharacters(33, 127)));
  }

  @Test
  public void testGetAlias() {

    assertEquals(4, FASTQ_SANGER.getAlias().size());
    assertTrue(FASTQ_SANGER.getAlias().contains("sanger"));
    assertTrue(FASTQ_SANGER.getAlias().contains("fastq-illumina-1.8"));
    assertTrue(FASTQ_SANGER.getAlias().contains("illumina-1.8"));
    assertTrue(FASTQ_SANGER.getAlias().contains("1.8"));

    assertEquals(4, FASTQ_SOLEXA.getAlias().size());
    assertTrue(FASTQ_SOLEXA.getAlias().contains("solexa"));
    assertTrue(FASTQ_SOLEXA.getAlias().contains("fastq-solexa-1.0"));
    assertTrue(FASTQ_SOLEXA.getAlias().contains("solexa-1.0"));
    assertTrue(FASTQ_SOLEXA.getAlias().contains("1.0"));

    assertEquals(4, FASTQ_ILLUMINA.getAlias().size());
    assertTrue(FASTQ_ILLUMINA.getAlias().contains("fastq-illumina"));
    assertTrue(FASTQ_ILLUMINA.getAlias().contains("illumina"));
    assertTrue(FASTQ_ILLUMINA.getAlias().contains("illumina-1.3"));
    assertTrue(FASTQ_ILLUMINA.getAlias().contains("1.3"));

    assertEquals(2, FASTQ_ILLUMINA_1_5.getAlias().size());
    assertTrue(FASTQ_ILLUMINA_1_5.getAlias().contains("illumina-1.5"));
    assertTrue(FASTQ_ILLUMINA_1_5.getAlias().contains("1.5"));
  }

  @Test
  public void testGetMaxScoreExpected() {

    assertEquals(40, FASTQ_SANGER.getScoreMaxExpected());
    assertEquals(40, FASTQ_SOLEXA.getScoreMaxExpected());
    assertEquals(40, FASTQ_ILLUMINA.getScoreMaxExpected());
    assertEquals(40, FASTQ_ILLUMINA_1_5.getScoreMaxExpected());
  }

  @Test
  public void testIsCharValid() {

    assertTrue(FastqFormat.FASTQ_SANGER.isCharValid('!'));
    assertTrue(FastqFormat.FASTQ_SANGER.isCharValid('~'));
    assertFalse(FastqFormat.FASTQ_SANGER.isCharValid(' '));
    assertFalse(FastqFormat.FASTQ_SANGER.isCharValid((char) 127));
  }

  @Test
  public void testIsStringValid() {

    try {
      FastqFormat.FASTQ_SANGER.findInvalidChar(null);
      fail();
    } catch (NullPointerException e) {
      assertTrue(true);
    }

    assertEquals(-1,
        FastqFormat.FASTQ_SANGER
            .findInvalidChar("!\"#$%&'()*+,-./0123456789:;<=>?"
                + "@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`"
                + "abcdefghijklmnopqrstuvwxyz{|}~"));

    assertEquals(' ', FastqFormat.FASTQ_SANGER
        .findInvalidChar("!\"#$%&'()*+,-./012345 6789:;<=>?"));

    assertEquals(-1, FastqFormat.FASTQ_SOLEXA.findInvalidChar(
        ";<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"));
    assertEquals(' ', FastqFormat.FASTQ_SOLEXA.findInvalidChar(
        ";<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdef ghijklmnopqrstuvwxyz{|}~"));

    assertEquals(-1, FastqFormat.FASTQ_ILLUMINA.findInvalidChar(
        "@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"));
    assertEquals(' ', FastqFormat.FASTQ_ILLUMINA.findInvalidChar(
        "@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~ "));

    assertEquals(-1, FastqFormat.FASTQ_ILLUMINA_1_5.findInvalidChar(
        "BCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"));
    assertEquals(' ', FastqFormat.FASTQ_ILLUMINA_1_5.findInvalidChar(
        "BCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~ "));
  }

  @Test
  public void testConvertTo() {

    assertEquals(';',
        FastqFormat.FASTQ_SANGER.convertTo('!', FastqFormat.FASTQ_SOLEXA));

    assertNull(
        FastqFormat.FASTQ_SANGER.convertTo(null, FastqFormat.FASTQ_SOLEXA));

    assertEquals(";",
        FastqFormat.FASTQ_SANGER.convertTo("!", FastqFormat.FASTQ_SOLEXA));

    assertEquals(";;;;",
        FastqFormat.FASTQ_SANGER.convertTo("!!!!", FastqFormat.FASTQ_SOLEXA));

  }

  @Test
  public void testIdentifyFormatInputStream()
      throws IOException, BadBioEntryException {

    try {
      FastqFormat.identifyFormat((InputStream) null);
      fail();
    } catch (NullPointerException e) {
      assertTrue(true);
    }

    InputStream is = this.getClass().getResourceAsStream("/illumina_1_8.fastq");
    assertEquals(FastqFormat.FASTQ_SANGER, FastqFormat.identifyFormat(is));

    is = this.getClass().getResourceAsStream("/illumina_1_8.fastq");
    assertEquals(FastqFormat.FASTQ_SANGER, FastqFormat.identifyFormat(is, 500));

  }

}
