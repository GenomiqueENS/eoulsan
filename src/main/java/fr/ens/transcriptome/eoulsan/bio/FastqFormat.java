package fr.ens.transcriptome.eoulsan.bio;

import java.util.Set;

public enum FastqFormat {

  FASTQ_SANGER("fastq-sanger", null, 0, 93, 33, false), FASTQ_SOLEXA(
      "fastq-solexa", null, -5, 62, 64, true), FASTQ_ILLUMINA("fastq-illumina",
      null, 0, 62, 64, false);

  private final String name;
  private final Set<String> alias;

  private final int qualityMin;
  private final int qualityMax;
  private final int asciiOffset;
  private final boolean solexaQualityScore;

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
   * Get the minimal value of the quality score.
   * @return the minimal value of the quality score
   */
  public int getQualityMin() {
    return this.qualityMin;
  }

  /**
   * Get the maximal value of the quality score.
   * @return the maximal value of the quality score
   */
  public int getQualityMax() {
    return this.qualityMax;
  }

  /**
   * Get the ASCII offset.
   * @return the ASCII offset
   */
  public int getAsciiOffset() {
    return this.asciiOffset;
  }

  //
  // Other methods
  //

  /**
   * Get the minimal ASCII character used to represent the quality.
   * @return an ASCII character
   */
  public char getCharMin() {

    return (char) (this.asciiOffset + this.qualityMin);
  }

  /**
   * Get the maximal ASCII character used to represent the quality.
   * @return an ASCII character
   */
  public char getCharMax() {

    return (char) (this.asciiOffset + this.qualityMax);
  }

  /**
   * Convert a character to a quality number.
   * @param character character to convert
   * @return a quality score
   */
  public int getQuality(final char character) {

    return character - this.asciiOffset;
  }

  // /**
  // * Convert a quality character from a format to another.
  // * @param character character to convert
  // * @param format output format
  // * @return the converted character
  // */
  // public char convertTo(final char character, final FastqFormat format) {
  //
  // return (char) (format.asciiOffset + convertQualityTo(getQuality(character),
  // format));
  // }

  // /**
  // * Convert quality from a format to another.
  // * @param quality quality to transform
  // * @param format output format
  // * @return a converted quality
  // */
  // public int convertQualityTo(final int quality, final FastqFormat format) {
  //
  // if (this.solexaQualityScore != format.solexaQualityScore) {
  //
  // double dq = (double) quality;
  // System.out.println("dq=" + dq);
  //
  // double pow = Math.pow(10, dq / 10.0);
  // System.out.println("pow=" + pow);
  //
  // double log = Math.log10(pow + (this.solexaQualityScore ? 1 : -1));
  // System.out.println("log=" + log);
  //
  // double result = 10.0 * log;
  // System.out.println("result=" + result);
  //
  // int r = (int) result;
  // System.out.println("return=" + r);
  //
  // return r;
  //
  // // return (int) (10.0 * Math.log10(Math.pow(10, ((double) quality)
  // // / 10.0) + (this.solexaQualityScore ? 1 : -1)));
  // }
  //
  // return quality;
  // }

  /**
   * Get a format from its name or its alias.
   * @param name name of the format to get
   * @return the format or null if no format was found
   */
  public static FastqFormat getFormatFromName(final String name) {

    if (name == null)
      return null;

    for (FastqFormat format : FastqFormat.values()) {

      if (format.getName().equals(name))
        return format;

      if (format.alias != null && format.alias.contains(name))
        return format;

    }

    return null;
  }

  // public static void identifyFormat(Set<FastqFormat> formats,
  // final String qualityString) {
  //
  // if (formats == null || qualityString == null)
  // return;
  //
  // for (FastqFormat format : new HashSet<FastqFormat>(formats)) {
  //
  // for (int i = 0; i < qualityString.length(); i++) {
  // final char c = qualityString.charAt(i);
  // if (c < format.getCharMin() || c > format.getCharMax()) {
  // System.out.println("c=" + c + " (" + ((int) c) + ") " + format);
  // System.out.println(format.getCharMin()
  // + " (" + ((int) format.getCharMin()) + ") " + format.getCharMax()
  // + " (" + ((int) format.getCharMax()) + ") ");
  // formats.remove(format);
  // break;
  // }
  // }
  // }
  // }

  // public static final void main(final String[] args) throws IOException,
  // BadBioEntryException {
  //
  // File f = new File("/home/jourdren/tests/s1.fq");
  //
  // FastQReader reader = new FastQReader(f);
  //
  // Set<FastqFormat> formats =
  // new HashSet<FastqFormat>(Arrays.asList(FastqFormat.values()));
  //
  // int count = 0;
  // while (reader.readEntry()) {
  //
  // count++;
  // FastqFormat.identifyFormat(formats, reader.getQuality());
  //
  // if (formats.size() == 1) {
  // //System.out.println("Found: " + formats + " in " + count + " reads");
  // //break;
  // }
  // }
  // System.out.println("Found: " + formats + " in " + count + " reads");
  // reader.close();
  // }

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
   * @param alias alias of the format
   * @param qualityMin quality minimal value
   * @param qualityMax quality maximal value
   * @param asciiOffset ASCII offset
   * @param solexaQualityScore Solexa quality score
   */
  FastqFormat(final String name, final Set<String> alias, final int qualityMin,
      final int qualityMax, final int asciiOffset,
      final boolean solexaQualityScore) {

    this.name = name;
    this.alias = alias;
    this.qualityMin = qualityMin;
    this.qualityMax = qualityMax;
    this.asciiOffset = asciiOffset;
    this.solexaQualityScore = solexaQualityScore;
  }

}
