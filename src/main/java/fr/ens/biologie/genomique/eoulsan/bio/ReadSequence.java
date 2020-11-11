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

import static fr.ens.biologie.genomique.eoulsan.util.StringUtils.trim;
import static fr.ens.biologie.genomique.eoulsan.util.Utils.equal;

import fr.ens.biologie.genomique.eoulsan.util.Utils;

/**
 * This class define a read sequence.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class ReadSequence extends Sequence {

  private FastqFormat fastqFormat = FastqFormat.FASTQ_SANGER;
  private String quality;

  //
  // Getters
  //

  /**
   * Get the fastq format value.
   * @return the fastq format
   */
  public FastqFormat getFastqFormat() {

    return this.fastqFormat;
  }

  /**
   * Get the quality of the sequence.
   * @return a string with the quality
   */
  public String getQuality() {

    return this.quality;
  }

  //
  // Setters
  //

  /**
   * Set the fastq format value.
   * @param fastqFormat the fastq format to set
   */
  public void setFastqFormat(final FastqFormat fastqFormat) {

    if (fastqFormat == null) {
      throw new NullPointerException("The FastqFormat is null");
    }

    this.fastqFormat = fastqFormat;
  }

  /**
   * Set the quality.
   * @param quality Sequence to set
   */
  public void setQuality(final String quality) {
    this.quality = trim(quality);
  }

  /**
   * Set the ReadSequence with the values of another ReadSequence.
   * @param rs ReadSequence to use to set the values of this ReadSequence
   */
  public void set(final ReadSequence rs) {

    if (rs == null) {
      return;
    }

    this.setName(rs.getName());
    this.setSequence(rs.getSequence());
    this.setQuality(rs.getQuality());
    this.setFastqFormat(rs.getFastqFormat());
  }

  //
  // Quality methods
  //

  public int[] qualityScores() {

    if (this.quality == null) {
      return null;
    }

    final char[] qualities = this.quality.toCharArray();
    final int len = qualities.length;
    final FastqFormat format = this.fastqFormat;
    final int[] result = new int[len];

    for (int i = 0; i < len; i++) {
      result[i] = format.getScore(qualities[i]);
    }

    return result;
  }

  public double[] errorProbabilities() {

    if (this.quality == null) {
      return null;
    }

    final char[] qualities = this.quality.toCharArray();
    final int len = qualities.length;
    final FastqFormat format = this.fastqFormat;
    final double[] result = new double[len];

    for (int i = 0; i < len; i++) {
      result[i] = format.getProbability(qualities[i]);
    }

    return result;
  }

  //
  // Sequence methods
  //

  /**
   * Create a sub-sequence from the current sequence. Note that index start at
   * 0.
   * @param beginIndex begin index of the sub-sequence
   * @param endIndex end index of the sub-sequence
   * @return a new sequence object with a sub-sequence of the current object
   */
  @Override
  public ReadSequence subSequence(final int beginIndex, final int endIndex) {

    if (this.sequence == null
        || this.quality == null
        || this.sequence.length() != this.quality.length()) {
      return null;
    }

    if (beginIndex < 0) {
      throw new StringIndexOutOfBoundsException(beginIndex);
    }

    if (endIndex > length()) {
      throw new StringIndexOutOfBoundsException(endIndex);
    }

    if (beginIndex > endIndex) {
      throw new StringIndexOutOfBoundsException(endIndex - beginIndex);
    }

    final ReadSequence result =
        new ReadSequence(this.name == null ? null : this.name + "[part]",
            this.sequence.substring(beginIndex, endIndex),
            this.quality.substring(beginIndex, endIndex));

    result.fastqFormat = this.fastqFormat;

    return result;
  }

  /**
   * Contact two ReadSequences.
   * @param sequence sequence to contact
   * @return a new sequence object with the sequence of the current object and
   *         the sequence of the input sequence
   */
  public ReadSequence concat(final ReadSequence sequence) {

    final ReadSequence result = new ReadSequence();
    result.setName(this.name + "[merged]");
    result.fastqFormat = this.fastqFormat;
    result.alphabet = this.alphabet;

    if (sequence == null) {
      result.sequence = this.sequence;
      result.quality = this.quality;
      return result;
    }

    if (this.sequence == null) {
      result.sequence = sequence.sequence;
    } else if (sequence.sequence == null) {
      result.sequence = this.sequence;
    } else {
      result.sequence = this.sequence + sequence.sequence;
    }

    if (this.quality == null) {
      result.quality = sequence.quality;
    } else if (sequence.quality == null) {
      result.quality = this.quality;
    } else {
      result.quality = this.quality + sequence.quality;
    }

    return result;
  }

  /**
   * Set the sequence as the reverse.
   */
  public void reverse() {

    this.sequence = reverse(this.sequence);
    this.quality = reverse(this.quality);
  }

  /**
   * Set the sequence as the reverse complement.
   */
  public void reverseComplement() {

    this.sequence = reverseComplement(this.sequence, this.alphabet);
    this.quality = reverse(this.quality);
  }

  //
  // Output methods
  //

  /**
   * Return the sequence in FastQ format.
   * @return a String with the sequence in FastQ format
   */
  public String toFastQ() {

    return toFastQ(this.name, this.sequence, this.quality, false);
  }

  /**
   * Return the sequence in FastQ format.
   * @param repeatId repeat the id on the 3rd line of the fastq entry
   * @return a String with the sequence in FastQ format
   */
  public String toFastQ(final boolean repeatId) {

    return toFastQ(this.name, this.sequence, this.quality, repeatId);
  }

  /**
   * Return the sequence in FastQ format.
   * @param name Name of the read
   * @param sequence Sequence of the read
   * @param quality Quality of the read
   * @return a String with the sequence in FastQ format
   */
  public static final String toFastQ(final String name, final String sequence,
      final String quality) {

    return toFastQ(name, sequence, quality, false);
  }

  /**
   * Return the sequence in FastQ format.
   * @param name Name of the read
   * @param sequence Sequence of the read
   * @param quality Quality of the read
   * @param repeatId repeat the id on the 3rd line of the fastq entry
   * @return a String with the sequence in FastQ format
   */
  public static final String toFastQ(final String name, final String sequence,
      final String quality, final boolean repeatId) {

    if (name == null || sequence == null || quality == null) {
      return null;
    }

    return '@'
        + name + '\n' + sequence + '\n' + '+' + (repeatId ? name : "") + '\n'
        + quality;
  }

  /**
   * Return the sequence in TFQ format.
   * @return a String with the sequence in FastQ format
   */
  public String toTFQ() {

    return toTFQ(true);
  }

  /**
   * Return the sequence in TFQ format.
   * @param withId true if id must be added to the result
   * @return a String with the sequence in FastQ format
   */
  public String toTFQ(final boolean withId) {

    return toTFQ(withId, this.name, this.sequence, this.quality);
  }

  /**
   * Return the sequence in TFQ format.
   * @param name Name of the read
   * @param sequence Sequence of the read
   * @param quality Quality of the read
   * @return a String with the sequence in FastQ format
   */
  public static final String toTFQ(final String name, final String sequence,
      final String quality) {

    return toTFQ(true, name, sequence, quality);
  }

  /**
   * Return the sequence in TFQ format.
   * @param withId true if id must be added to the result
   * @param name Name of the read
   * @param sequence Sequence of the read
   * @param quality Quality of the read
   * @return a String with the sequence in FastQ format
   */
  public static final String toTFQ(final boolean withId, final String name,
      final String sequence, final String quality) {

    if (name == null || sequence == null || quality == null) {
      return null;
    }

    if (withId) {
      return name + '\t' + sequence + '\t' + quality;
    }

    return '\t' + sequence + '\t' + quality;
  }

  /**
   * Return the key for the read (the name).
   * @return a string with the name of the read as the key
   */
  public String toOutKey() {

    return this.name;
  }

  /**
   * Return the value for the read (the sequence + the quality).
   * @return a string with the sequence and the quality of the read as the value
   */
  public String toOutValue() {

    return this.sequence + "\t" + this.quality;
  }

  //
  // Parsing methods
  //

  /**
   * Parse a FastQ sequence.
   * @param fastQ FastQ sequence to parse
   */
  public void parseFastQ(final String fastQ) {

    if (fastQ == null) {
      return;
    }

    final int indexCR1 = fastQ.indexOf('\n');
    final int indexCR2 = fastQ.indexOf('\n', indexCR1 + 1);
    final int indexCR3 = fastQ.indexOf('\n', indexCR2 + 1);
    final int indexCR4 = fastQ.indexOf('\n', indexCR3 + 1);

    this.name = fastQ.substring(1, indexCR1);
    this.sequence = fastQ.substring(indexCR1 + 1, indexCR2);

    if (indexCR4 == -1) {
      this.quality = fastQ.substring(indexCR3 + 1);
    } else {
      this.quality = fastQ.substring(indexCR3 + 1, indexCR4);
    }
  }

  /**
   * Parse a read.
   * @param s String to parse
   */
  public void parse(final String s) {

    if (s == null) {
      return;
    }

    final int indexTab1 = s.indexOf('\t');
    final int indexTab2 = s.indexOf('\t', indexTab1 + 1);

    this.name = s.substring(0, indexTab1);
    this.sequence = s.substring(indexTab1 + 1, indexTab2);
    this.quality = s.substring(indexTab2 + 1);
  }

  /**
   * Parse a read in key/value format.
   * @param key key to parse
   * @param value value to parse
   */
  public void parseKeyValue(final String key, final String value) {

    if (key == null || value == null) {
      return;
    }

    this.name = key;

    final int indexTab = value.indexOf('\t');
    this.sequence = value.substring(0, indexTab);
    this.quality = value.substring(indexTab + 1);
  }

  //
  // Validation methods
  //

  protected boolean validateQuality() {

    final String q = this.quality;

    if (q == null) {
      return false;
    }

    final int len = q.length();

    if (len == 0 || len != length()) {
      return false;
    }

    for (int i = 0; i < len; i++) {
      if (!this.fastqFormat.isCharValid(q.charAt(i))) {
        return false;
      }
    }

    return true;
  }

  /**
   * Check if the read is valid.
   * @return true if the read is validated
   */
  @Override
  public boolean validate() {

    return validateName() && validateSequence() && validateQuality();
  }

  //
  // Object methods
  //

  @Override
  public int hashCode() {

    return Utils.hashCode(this.name, this.description, this.alphabet,
        this.sequence, this.quality, this.fastqFormat);
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof ReadSequence) || !super.equals(o)) {
      return false;
    }

    final ReadSequence that = (ReadSequence) o;

    return this.fastqFormat.equals(that.fastqFormat)
        && equal(this.quality, that.quality);
  }

  @Override
  public String toString() {

    return this.getClass().getSimpleName()
        + "{name=" + this.name + ", description=" + this.description
        + ", alphabet=" + this.alphabet + ", sequence=" + this.sequence
        + ", fastqFormat=" + this.fastqFormat + ", quality=" + this.quality
        + "}";
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public ReadSequence() {
    super();
    this.alphabet = Alphabets.READ_DNA_ALPHABET;
  }

  /**
   * Public constructor.
   * @param name Name of the read
   * @param sequence Sequence of the read
   * @param quality Quality of the read
   */
  public ReadSequence(final String name, final String sequence,
      final String quality) {

    this(name, sequence, quality, FastqFormat.FASTQ_SANGER);
  }

  /**
   * Public constructor.
   * @param name Name of the read
   * @param sequence Sequence of the read
   * @param quality Quality of the read
   */
  public ReadSequence(final String name, final String sequence,
      final String quality, final FastqFormat fastqFormat) {

    super(name, sequence);
    this.quality = quality;
    this.fastqFormat = fastqFormat;
    this.alphabet = Alphabets.READ_DNA_ALPHABET;
  }

  /**
   * Public constructor.
   * @param id identifier
   * @param name Name of the read
   * @param sequence Sequence of the read
   * @param quality Quality of the read
   */
  @Deprecated
  public ReadSequence(final int id, final String name, final String sequence,
      final String quality) {

    this(name, sequence, quality);
  }

  /**
   * Public constructor.
   * @param id identifier
   * @param name Name of the read
   * @param sequence Sequence of the read
   * @param quality Quality of the read
   */
  @Deprecated
  public ReadSequence(final int id, final String name, final String sequence,
      final String quality, final FastqFormat fastqFormat) {

    this(name, sequence, quality, fastqFormat);
  }

}
