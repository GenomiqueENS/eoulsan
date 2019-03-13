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

import static fr.ens.biologie.genomique.eoulsan.util.Utils.newArrayList;
import static fr.ens.biologie.genomique.eoulsan.util.Utils.newHashSet;
import static java.lang.Math.log10;
import static java.lang.Math.max;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.util.Collections.unmodifiableSet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.bio.io.FastqReader;

/**
 * This enum define the existing fastq formats. It provide many function to
 * transform a format to another.
 * @since 1.1
 * @author Laurent Jourdren
 */
public enum FastqFormat {

  FASTQ_SANGER("fastq-sanger",
      new String[] {"sanger", "fastq-illumina-1.8", "illumina-1.8", "1.8"},
      "1.8", 0, 93, 40, 33, true),

  FASTQ_SOLEXA("fastq-solexa",
      new String[] {"solexa", "fastq-solexa-1.0", "solexa-1.0", "1.0"}, "1.0",
      -5, 62, 40, 64, false),

  FASTQ_ILLUMINA("fastq-illumina-1.3",
      new String[] {"fastq-illumina", "illumina", "illumina-1.3", "1.3"}, "1.3",
      0, 62, 40, 64, true),

  FASTQ_ILLUMINA_1_5("fastq-illumina-1.5", new String[] {"illumina-1.5", "1.5"},
      "1.5", 2, 62, 40, 64, true);

  private final String name;
  private final Set<String> alias;
  private final String illuminaVersion;

  private final int scoreMin;
  private final int scoreMax;
  private final int scoreMaxExpected;
  private final int asciiOffset;
  private final boolean phredScore;

  //
  // Getters
  //

  /**
   * Get the name of the fastq format.
   * @return the name of the format
   */
  public String getName() {
    return this.name;
  }

  /**
   * Get the alias for the name of this format.
   * @return a set of string with the alias names
   */
  public Set<String> getAlias() {

    if (this.alias == null) {
      return Collections.emptySet();
    }

    return unmodifiableSet(this.alias);
  }

  /**
   * Get the first version of the solexa/illumina tools that can have generated
   * a file with this format.
   * @return a string with the format version
   */
  public String getIlluminaVersion() {

    return this.illuminaVersion;
  }

  /**
   * Get the minimal value of the quality score.
   * @return the minimal value of the quality score
   */
  public int getScoreMin() {
    return this.scoreMin;
  }

  /**
   * Get the maximal value of the quality score.
   * @return the maximal value of the quality score
   */
  public int getScoreMax() {
    return this.scoreMax;
  }

  /**
   * Get the ASCII offset.
   * @return the ASCII offset
   */
  public int getAsciiOffset() {
    return this.asciiOffset;
  }

  /**
   * Get the maximal expected value of the quality score.
   * @return the maximal expected value of the quality score
   */
  public int getScoreMaxExpected() {
    return this.scoreMaxExpected;
  }

  /**
   * Test if the format use Phred quality score.
   * @return true if the format Phred quality score
   */
  public boolean isPhredScore() {
    return this.phredScore;
  }

  //
  // Other methods
  //

  /**
   * Get the minimal ASCII character used to represent the quality score.
   * @return an ASCII character
   */
  public char getCharMin() {

    return (char) (this.asciiOffset + this.scoreMin);
  }

  /**
   * Get the maximal ASCII character used to represent the quality score.
   * @return an ASCII character
   */
  public char getCharMax() {

    return (char) (this.asciiOffset + this.scoreMax);
  }

  /**
   * Get the maximal ASCII character used to represent the quality score.
   * @return an ASCII character
   */
  public char getCharMaxExpected() {

    return (char) (this.asciiOffset + this.scoreMaxExpected);
  }

  /**
   * Test if a character is valid to represent the quality.
   * @param c the character to test
   * @return true if the character if valid
   */
  public boolean isCharValid(final char c) {

    return c >= getCharMin() && c <= getCharMax();
  }

  /**
   * Test if all the character of a string are valid to represent the quality.
   * @param s the string to test
   * @return -1 if all the characters of the string are valid of the value of
   *         the first invalid character
   */
  public int findInvalidChar(final String s) {

    if (s == null) {
      throw new NullPointerException();
    }

    final int len = s.length();

    for (int i = 0; i < len; i++) {

      final char c = s.charAt(i);
      if (!isCharValid(c)) {
        return c;
      }
    }

    return -1;
  }

  /**
   * Convert a character to a quality number.
   * @param character character to convert
   * @return a quality score
   */
  public int getScore(final char character) {

    return character - this.asciiOffset;
  }

  /**
   * Convert a character to an error probability.
   * @param character character to convert
   * @return a quality score
   */
  public double getProbability(final char character) {

    return convertScoreToProbability(character - this.asciiOffset);
  }

  /**
   * Convert a quality score to a probability.
   * @param score the quality score
   * @return the probability that correspond to the quality score
   */
  public double convertScoreToProbability(final int score) {

    if (this.phredScore) {
      return pow(10.0, (double) score / -10.0);
    }

    return 1.0 / ((1.0 / pow(10.0, -score / 10.0)) + 1.0);
  }

  /**
   * Convert a probability to a quality score.
   * @param p probability to convert
   * @return a quality score
   */
  public double convertProbabilityToScore(final double p) {

    if (this.phredScore) {
      return -10.0 * log10(p);
    }

    return -10.0 * log10(p / (1 - p));
  }

  /**
   * Convert Solexa quality score to Phred quality score. The formula is taken
   * from BioPython.
   * @param solexaScore the input quality score
   * @return the quality converted to Phred quality score
   */
  public static double convertSolexaScoreToPhredScore(final int solexaScore) {

    if (solexaScore < -5) {
      throw new IllegalArgumentException(
          "Invalid Solexa quality: " + solexaScore);
    }

    return 10.0 * log10(pow(10, solexaScore / 10.0) + 1);
  }

  /**
   * Convert Phred quality score to Solexa quality score. The formula is taken
   * from BioPython.
   * @param phredScore the input quality score
   * @return the quality converted to Solexa
   */
  public static double convertPhredSCoreToSolexaScore(final int phredScore) {

    if (phredScore == 0) {
      return -5.0;
    }

    if (phredScore > 0) {
      return max(-5.0, 10.0 * log10(pow(10, phredScore / 10.0) - 1));
    }

    throw new IllegalArgumentException("Invalid PHRED quality: " + phredScore);
  }

  /**
   * Convert a quality score string from a format to another.
   * @param quality quality string to convert
   * @param format output format
   * @return the converted quality string
   */
  public String convertTo(final String quality, final FastqFormat format) {

    if (quality == null) {
      return null;
    }

    final StringBuilder sb = new StringBuilder(quality.length());

    for (char c : quality.toCharArray()) {
      sb.append(convertTo(c, format));
    }

    return sb.toString();
  }

  /**
   * Convert a quality score character from a format to another.
   * @param character character to convert
   * @param format output format
   * @return the converted character
   */
  public char convertTo(final char character, final FastqFormat format) {

    return (char) (format.asciiOffset
        + convertScoreTo(getScore(character), format));
  }

  /**
   * Convert quality from a format to another.
   * @param score quality score to transform
   * @param format output format
   * @return a converted quality score
   */
  public int convertScoreTo(final int score, final FastqFormat format) {

    if (this.isPhredScore() != format.isPhredScore()) {

      if (isPhredScore()) {
        return (int) round(convertPhredSCoreToSolexaScore(score));
      }

      return (int) round(convertSolexaScoreToPhredScore(score));
    }

    return score;
  }

  /**
   * Get a format from its name or its alias.
   * @param name name of the format to get
   * @return the format or null if no format was found
   */
  public static FastqFormat getFormatFromName(final String name) {

    if (name == null) {
      return null;
    }

    final String lowerName = name.toLowerCase().trim();

    for (FastqFormat format : FastqFormat.values()) {

      if (format.getName().toLowerCase().equals(lowerName)) {
        return format;
      }

      if (format.alias != null && format.alias.contains(lowerName)) {
        return format;
      }

    }

    return null;
  }

  /**
   * Identify the fastq format used in a Fastq file.
   * @param is input stream
   * @return The FastqFormat found or null if no format was found
   * @throws IOException if an error occurs while reading the fastq stream
   * @throws BadBioEntryException if bad fastq entry is found
   */
  public static FastqFormat identifyFormat(final InputStream is)
      throws IOException, BadBioEntryException {

    return identifyFormat(is, -1);
  }

  /**
   * Identify the fastq format used in a Fastq file.
   * @param is input stream
   * @param maxEntriesToRead maximal entries of the file to read. If this value
   *          is lower than 1 all the entries of the stream are read
   * @return The FastqFormat found or null if no format was found
   * @throws IOException if an error occurs while reading the fastq stream
   * @throws BadBioEntryException if bad fastq entry is found
   */
  public static FastqFormat identifyFormat(final InputStream is,
      final int maxEntriesToRead) throws IOException, BadBioEntryException {

    if (is == null) {
      throw new NullPointerException("The input format is null");
    }

    final FastqReader reader = new FastqReader(is);
    final Set<FastqFormat> formats =
        newHashSet(Arrays.asList(FastqFormat.values()));

    int count = 0;

    final int[] range = new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE};

    for (final ReadSequence read : reader) {

      if (maxEntriesToRead > 0 && count > maxEntriesToRead) {
        break;
      }

      removeBadFormats(formats, read.getQuality(), range);
      count++;
    }
    reader.throwException();

    reader.close();

    return identifyFormatByHeuristic(formats, range[0], range[1]);
  }

  /**
   * Identify the fastq format used in a quality string file.
   * @param qualityString a string with quality data
   * @return The FastqFormat found or null if no format was found
   */
  public static FastqFormat identifyFormat(final String qualityString) {

    if (qualityString == null) {
      return null;
    }

    final Set<FastqFormat> formats =
        newHashSet(Arrays.asList(FastqFormat.values()));

    final int[] range = new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE};

    removeBadFormats(formats, qualityString, range);
    return identifyFormatByHeuristic(formats, range[0], range[1]);
  }

  private static void removeBadFormats(final Set<FastqFormat> formats,
      final String qualityString, final int[] range) {

    Set<FastqFormat> toRemove = null;

    for (FastqFormat format : formats) {

      for (int i = 0; i < qualityString.length(); i++) {
        final int c = qualityString.codePointAt(i);

        // Check if the character is the lowest value
        if (c < range[0]) {
          range[0] = c;
        }

        // Check if the character is highest value
        if (c > range[1]) {
          range[1] = c;
        }

        if (c < format.getCharMin() || c > format.getCharMax()) {

          if (toRemove == null) {
            toRemove = new HashSet<>();
          }
          toRemove.add(format);
        }
      }
    }

    if (toRemove != null) {
      formats.removeAll(toRemove);
    }
  }

  private static FastqFormat identifyFormatByHeuristic(
      final Set<FastqFormat> formats, final int lowerChar,
      final int higherChar) {

    if (formats == null) {
      return null;
    }

    if (formats.isEmpty()) {
      return null;
    }

    // Sort formats with increasing minimal char
    final List<FastqFormat> sortedFormats = newArrayList(formats);
    Collections.sort(sortedFormats, new Comparator<FastqFormat>() {
      @Override
      public int compare(final FastqFormat o1, final FastqFormat o2) {

        return Integer.compare(o1.getCharMin(), o2.getCharMin());
      }
    });

    FastqFormat last = null;

    // The format is the
    for (FastqFormat f : sortedFormats) {

      if (last != null
          && last.getCharMin() <= lowerChar && lowerChar < f.getCharMin()) {
        return f;
      }

      last = f;
    }

    // Check if the higher value is valid for the selected format
    if (higherChar <= last.getCharMax()) {
      return last;
    }

    return null;
  }

  @Override
  public String toString() {

    return getName();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param name format name
   * @param alias for the format name
   * @param illuminaVersion illumina version of the format
   * @param scoreMin quality score minimal value
   * @param scoreMax quality score maximal value
   * @param scoreMaxExpected quality score maximal expected value
   * @param asciiOffset ASCII offset
   * @param phredQualityScore Phred quality score
   */
  FastqFormat(final String name, final String[] alias,
      final String illuminaVersion, final int scoreMin, final int scoreMax,
      final int scoreMaxExpected, final int asciiOffset,
      final boolean phredQualityScore) {

    this.name = name;
    this.alias = alias == null ? null : newHashSet(Arrays.asList(alias));
    this.illuminaVersion = illuminaVersion;
    this.scoreMin = scoreMin;
    this.scoreMax = scoreMax;
    this.scoreMaxExpected = scoreMaxExpected;
    this.asciiOffset = asciiOffset;
    this.phredScore = phredQualityScore;
  }

}
