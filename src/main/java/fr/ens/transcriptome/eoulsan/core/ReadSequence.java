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

package fr.ens.transcriptome.eoulsan.core;

import fr.ens.transcriptome.eoulsan.EoulsanException;

/**
 * This class define a read sequence.
 * @author Laurent Jourdren
 */
public class ReadSequence extends Sequence {

  private String quality;

  //
  // Getters
  //

  /**
   * Get the quality of the sequence.
   * @return a string with the quality
   */
  public final String getQuality() {

    return this.quality;
  }

  //
  // Setters
  // 

  /**
   * Set the quality.
   * @param quality Sequence to set
   */
  public final void setQuality(final String quality) {
    this.quality = quality;
  }

  //
  // Other methods
  // 

  /**
   * Test if a ReadSequence is valid. Only check if all fields are not null.
   * @return true if the ReadSequence is valid
   */
  public boolean isFastQValid() {

    return this.name != null && this.sequence != null && this.quality != null;
  }

  /**
   * Compute mean quality score of the read. Illumina version.
   * @return the mean quality score of the read.
   */
  public double meanQuality() {

    if (this.quality == null)
      return Double.NaN;

    int score = 0;
    final int len = quality.length();
    for (int i = 0; i < len; i++)
      score += quality.charAt(i) - 64;

    return score / len;
  }

  /**
   * Return the sequence in FastQ format
   * @return a String with the sequence in FastQ format
   */
  public String toFastQ() {

    if (this.name == null || this.sequence == null || this.quality == null)
      return null;

    return '@'
        + this.name + '\n' + this.sequence + '\n' + '+' + this.name + '\n'
        + this.quality + '\n';
  }

  /**
   * Return the sequence in TFQ format
   * @return a String with the sequence in FastQ format
   */
  public String toTFQ() {

    return toTFQ(true);
  }

  /**
   * Return the sequence in TFQ format
   * @return a String with the sequence in FastQ format
   */
  public String toTFQ(final boolean withId) {

    if (this.name == null || this.sequence == null || this.quality == null)
      return null;

    if (withId)
      return this.name + '\t' + this.sequence + '\t' + this.quality + '\n';

    return '\t' + this.sequence + '\t' + this.quality + '\n';
  }

  /**
   * Return the key for the read (the name).
   * @return a string with the name of the read as the key
   */
  public String toOutKey() {

    return this.name;
  }

  /**
   * Return the value for the read (the sequence + the quality)
   * @return a string with the sequence and the quality of the read as the value
   */
  public String toOutValue() {

    return this.sequence + "\t" + this.quality;
  }

  /**
   * Parse a FastQ sequence.
   * @param fastQ FastQ sequence to parse
   */
  public void parseFastQ(final String fastQ) {

    if (fastQ == null)
      return;

    final int indexCR1 = fastQ.indexOf('\n');
    final int indexCR2 = fastQ.indexOf('\n', indexCR1 + 1);
    final int indexCR3 = fastQ.indexOf('\n', indexCR2 + 1);

    this.name = fastQ.substring(0, indexCR1);
    this.sequence = fastQ.substring(indexCR1 + 1, indexCR2);
    this.quality = fastQ.substring(indexCR3 + 1);
  }

  /**
   * Parse a read
   * @param s String to parse
   */
  public void parse(final String s) {

    if (s == null)
      return;

    final int indexTab1 = s.indexOf('\t');
    final int indexTab2 = s.indexOf('\t', indexTab1 + 1);

    this.name = s.substring(0, indexTab1);
    this.sequence = s.substring(indexTab1 + 1, indexTab2);
    this.quality = s.substring(indexTab2 + 1);
  }

  /**
   * Parse a read in key/value format
   * @param key key to parse
   * @param value value to parse
   */
  public void parseKeyValue(final String key, final String value) {

    if (key == null || value == null)
      return;

    this.name = key;

    final int indexTab = value.indexOf('\t');
    this.sequence = value.substring(0, indexTab);
    this.quality = value.substring(indexTab + 1);
  }

  public void checkWithException() throws EoulsanException {

    if (this.name == null)
      throw new NullPointerException("The name of the sequence is null");
    if (this.sequence == null)
      throw new NullPointerException("The sequence is null");
    if (this.quality == null)
      throw new NullPointerException(
          "The quality string of the sequence is null");

    if (this.sequence.length() == 0)
      throw new NullPointerException("The sequence length equals 0");

    if (this.quality.length() == 0)
      throw new NullPointerException(
          "The length of quality string of the sequence equals 0");

    if (this.sequence.length() != this.quality.length())
      throw new EoulsanException(
          "The length of sequence and quality string are not equals");

    if (!checkCharString(this.quality, (char) 33, (char) 126))
      throw new EoulsanException(
          "The length of sequence and quality string are not equals");

    if (!checkBases(this.sequence))
      throw new EoulsanException("Invalid bases in sequence");

  }

  public boolean check() {

    return this.name != null
        && this.sequence != null && this.quality != null
        && this.sequence.length() > 0 && this.quality.length() > 0
        && this.sequence.length() == this.quality.length()
        && checkCharString(this.quality, (char) 33, (char) 126)
        && checkBases(this.sequence);
  }

  private boolean checkCharString(final String s, final char intervalLow,
      final char intervalHigh) {

    final int len = s.length();

    for (int i = 0; i < len; i++) {

      final char c = s.charAt(i);
      if (c < intervalLow || c > intervalHigh)
        return false;
    }

    return true;
  }

  private boolean checkBases(final String s) {

    final int len = s.length();

    for (int i = 0; i < len; i++) {

      switch (s.charAt(i)) {

      case 'A':
      case 'T':
      case 'G':
      case 'C':
      case 'N':
        break;
      default:
        return false;
      }

    }
    return true;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public ReadSequence() {
  }

  /**
   * Public constructor.
   * @param id identifier
   * @param name Name of the read
   * @param sequence Sequence of the read
   * @param quality Quality of the read
   */
  public ReadSequence(final int id, final String name, final String sequence,
      final String quality) {

    super(id, name, sequence);
    this.quality = quality;
  }

}
