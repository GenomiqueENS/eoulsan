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

package fr.ens.transcriptome.eoulsan.bio;

import static fr.ens.transcriptome.eoulsan.util.StringUtils.trim;
import static fr.ens.transcriptome.eoulsan.util.Utils.equal;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.util.StatUtils;
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class define a read sequence.
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
  public final FastqFormat getFastqFormat() {

    return this.fastqFormat;
  }

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
   * Set the fastq format value.
   * @param fastqFormat the fastq format to set
   */
  public final void setFastqFormat(final FastqFormat fastqFormat) {

    if (fastqFormat == null)
      throw new NullPointerException("The FastqFormat is null");

    this.fastqFormat = fastqFormat;
  }

  /**
   * Set the quality.
   * @param quality Sequence to set
   */
  public final void setQuality(final String quality) {
    this.quality = trim(quality);
  }

  /**
   * Set the ReadSequence with the values of another ReadSequence.
   * @param rs ReadSequence to use to set the values of this ReadSequence
   */
  public final void set(final ReadSequence rs) {

    if (rs == null) {
      return;
    }

    this.setId(rs.getId());
    this.setName(rs.getName());
    this.setSequence(rs.getSequence());
    this.setQuality(rs.getQuality());
    this.setFastqFormat(rs.getFastqFormat());
  }

  //
  // Quality methods
  //

  public int[] qualityScores() {

    if (this.quality == null)
      return null;

    final char[] qualities = this.quality.toCharArray();
    final int len = qualities.length;
    final FastqFormat format = this.fastqFormat;
    final int[] result = new int[len];

    for (int i = 0; i < len; i++)
      result[i] = format.getScore(qualities[i]);

    return result;
  }

  public double[] errorProbabilities() {

    if (this.quality == null)
      return null;

    final char[] qualities = this.quality.toCharArray();
    final int len = qualities.length;
    final FastqFormat format = this.fastqFormat;
    final double[] result = new double[len];

    for (int i = 0; i < len; i++)
      result[i] = format.getProbability(qualities[i]);

    return result;
  }

  //
  // Sequence methods
  //

  /**
   * Create a subsequence from the current sequence. Note that index start at 0.
   * @param beginIndex begin index of the subsequence
   * @param endIndex end index of the subsequence
   * @return a new sequence object with a subsequence of the current object
   */
  public ReadSequence subSequence(final int beginIndex, final int endIndex) {

    if (this.sequence == null
        || this.quality == null
        || this.sequence.length() != this.quality.length())
      return null;

    if (beginIndex < 0)
      throw new StringIndexOutOfBoundsException(beginIndex);

    if (endIndex > length())
      throw new StringIndexOutOfBoundsException(endIndex);

    if (beginIndex > endIndex)
      throw new StringIndexOutOfBoundsException(endIndex - beginIndex);

    return new ReadSequence(-1,
        this.name == null ? null : this.name + "[part]",
        this.sequence.substring(beginIndex, endIndex), this.quality.substring(
            beginIndex, endIndex));
  }

  /**
   * Contact two ReadSequences.
   * @param sequence sequence to contact
   * @return a new sequence object with the sequence of the current object and
   *         the sequence of the input sequence
   */
  public ReadSequence concat(final ReadSequence sequence) {

    final ReadSequence result = new ReadSequence();
    result.setName(this.name + " [merged]");

    if (sequence == null || this.sequence == null)
      return result;

    result.setSequence(getSequence() + sequence.getSequence());
    result.setQuality(getQuality() + sequence.getQuality());
    return result;
  }

  //
  // Output methods
  //

  /**
   * Return the sequence in FastQ format.
   * @param repeatId repeat the id on the 3rd line of the fastq entry
   * @return a String with the sequence in FastQ format
   */
  public final String toFastQ() {

    return toFastQ(this.name, this.sequence, this.quality, false);
  }

  /**
   * Return the sequence in FastQ format.
   * @param repeatId repeat the id on the 3rd line of the fastq entry
   * @return a String with the sequence in FastQ format
   */
  public final String toFastQ(final boolean repeatId) {

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
        + quality + '\n';
  }

  /**
   * Return the sequence in TFQ format.
   * @return a String with the sequence in FastQ format
   */
  public final String toTFQ() {

    return toTFQ(true);
  }

  /**
   * Return the sequence in TFQ format.
   * @param withId true if id must be added to the result
   * @return a String with the sequence in FastQ format
   */
  public final String toTFQ(final boolean withId) {

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
      return name + '\t' + sequence + '\t' + quality + '\n';
    }

    return '\t' + sequence + '\t' + quality + '\n';
  }

  /**
   * Return the key for the read (the name).
   * @return a string with the name of the read as the key
   */
  public final String toOutKey() {

    return this.name;
  }

  /**
   * Return the value for the read (the sequence + the quality).
   * @return a string with the sequence and the quality of the read as the value
   */
  public final String toOutValue() {

    return this.sequence + "\t" + this.quality;
  }

  //
  // Parsing methods
  //

  /**
   * Parse a FastQ sequence.
   * @param fastQ FastQ sequence to parse
   */
  public final void parseFastQ(final String fastQ) {

    if (fastQ == null) {
      return;
    }

    final int indexCR1 = fastQ.indexOf('\n');
    final int indexCR2 = fastQ.indexOf('\n', indexCR1 + 1);
    final int indexCR3 = fastQ.indexOf('\n', indexCR2 + 1);

    this.name = fastQ.substring(1, indexCR1);
    this.sequence = fastQ.substring(indexCR1 + 1, indexCR2);
    this.quality = fastQ.substring(indexCR3 + 1);
  }

  /**
   * Parse a read.
   * @param s String to parse
   */
  public final void parse(final String s) {

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
  public final void parseKeyValue(final String key, final String value) {

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
    final int len = q.length();

    if (q == null || len == 0 || len != length())
      return false;

    for (int i = 0; i < len; i++)

      if (!this.fastqFormat.isCharValid(q.charAt(i)))
        return false;

    return true;
  }

  /**
   * Check if the read is valid.
   * @return true if the read is validated
   */
  public boolean validate() {

    return validateName() && validateSequence() && validateQuality();
  }

  //
  // Object methods
  //

  @Override
  public int hashCode() {

    return Utils.hashCode(this.id, this.name, this.description, this.alphabet,
        this.sequence, this.quality, this.fastqFormat);
  }

  @Override
  public boolean equals(final Object o) {

    if (!super.equals(o))
      return false;

    if (!(o instanceof ReadSequence))
      return false;

    final ReadSequence that = (ReadSequence) o;

    return this.fastqFormat.equals(that.fastqFormat)
        && equal(this.quality, that.quality);
  }

  @Override
  public String toString() {

    return this.getName()
        + "{id=" + this.id + ", name=" + this.name + ", description="
        + this.description + ", alphabet=" + this.alphabet + ", sequence="
        + this.sequence + ", fastqFormat=" + this.fastqFormat + ", quality="
        + quality + "}";
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
   * @param id identifier
   * @param name Name of the read
   * @param sequence Sequence of the read
   * @param quality Quality of the read
   */
  public ReadSequence(final int id, final String name, final String sequence,
      final String quality) {

    this(id, name, sequence, quality, EoulsanRuntime.getSettings()
        .getDefaultFastqFormat());
    this.alphabet = Alphabets.READ_DNA_ALPHABET;
  }

  /**
   * Public constructor.
   * @param id identifier
   * @param name Name of the read
   * @param sequence Sequence of the read
   * @param quality Quality of the read
   */
  public ReadSequence(final int id, final String name, final String sequence,
      final String quality, final FastqFormat fastqFormat) {

    super(id, name, sequence);
    this.quality = quality;
    this.fastqFormat = fastqFormat;
    this.alphabet = Alphabets.READ_DNA_ALPHABET;
  }

}
