package fr.ens.biologie.genomique.eoulsan.bio;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.util.GuavaCompatibility;

/**
 * This class define a BedEntry. <b>Warning<b>: the coordinates stored in the
 * class are 1-based to be coherent with the other classes of the bio packages.
 * However the toBEDXX() methods generate output in 1-based coordinates.
 * @author Laurent Jourdren
 * @since 2.2
 */
public class BEDEntry {

  private final EntryMetadata metadata;
  private String chromosomeName;
  private int start;
  private int end;
  private String name;
  private String score;
  private char strand;
  private int thickStart;
  private int thickEnd;
  private String rgbItem;
  private final List<GenomicInterval> blocks = new ArrayList<>();

  //
  // Getters
  //

  /**
   * Get the metadata.
   * @return the metadata of the entry
   */
  public final EntryMetadata getMetadata() {

    return this.metadata;
  }

  /**
   * Get metadata keys names.
   * @return the metadata keys names
   */
  @Deprecated
  public final Set<String> getMetadataKeyNames() {

    return this.metadata.keySet();
  }

  /**
   * test if a metadata key exists.
   * @param key key name of the metadata
   * @return true if the entry in the meta data exists
   */
  @Deprecated
  public final boolean isMetaDataEntry(final String key) {

    return this.metadata.containsKey(key);
  }

  /**
   * Get the metadata values for a key.
   * @param key name of the metadata entry
   * @return the values of the attribute or null if the metadata name does not
   *         exists
   */
  @Deprecated
  public final List<String> getMetadataEntryValues(final String key) {

    return this.metadata.get(key);
  }

  /**
   * Get chromosome name.
   * @return the chromosome name
   */
  public String getChromosomeName() {
    return this.chromosomeName;
  }

  /**
   * Get the starting position of the feature in the chromosome (zero based).
   * @return the starts of the feature
   */
  public int getStart() {
    return this.start;
  }

  /**
   * Get the ending position of the feature in the chromosome (one based).
   * @return the ends of the feature
   */
  public int getEnd() {
    return this.end;
  }

  /**
   * Get the length of the entry.
   * @return the length of feature
   */
  public int getLength() {
    return this.end - this.start + 1;
  }

  /**
   * Get the name of the BED feature.
   * @return the name of the BED feature
   */
  public String getName() {
    return this.name;
  }

  /**
   * Get the score of the feature.
   * @return the score of the feature
   */
  public String getScore() {
    return this.score;
  }

  /**
   * Get the strand of the feature
   * @return the strand of the feature
   */
  public char getStrand() {
    return this.strand;
  }

  /**
   * Get the starting position at which the feature is drawn thickly.
   * @return the starting position at which the feature is drawn thickly
   */
  public int getThickStart() {
    return this.thickStart;
  }

  /**
   * Get the ending position at which the feature is drawn thickly.
   * @return the ending position at which the feature is drawn thickly
   */
  public int getThickEnd() {
    return this.thickEnd;
  }

  /**
   * Get the thick length.
   * @return the thick length
   */
  public int getThickLength() {
    return this.thickStart == -1 || this.getThickEnd() == -1
        ? 0 : this.thickEnd - this.thickStart + 1;
  }

  /**
   * Get the RGB value of the item.
   * @return the RGB value of the item
   */
  public String getRgbItem() {

    return this.rgbItem;
  }

  /**
   * Get the block count.
   * @return the block count
   */
  public int getBlockCount() {
    return this.blocks.size();
  }

  /**
   * Get the block sizes.
   * @return the block sizes
   */
  public List<Integer> getBlockSizes() {

    final List<Integer> result = new ArrayList<>(this.blocks.size());

    for (GenomicInterval b : this.blocks) {
      result.add(b.getLength());
    }

    return result;
  }

  /**
   * Get the block starts.
   * @return the block starts
   */
  public List<Integer> getBlockStarts() {

    final List<Integer> result = new ArrayList<>(this.blocks.size());

    for (GenomicInterval b : this.blocks) {
      result.add(b.getStart());
    }

    return result;
  }

  /**
   * Get the block starts.
   * @return the block starts
   */
  public List<Integer> getBlockEnds() {

    final List<Integer> result = new ArrayList<>(this.blocks.size());

    for (GenomicInterval b : this.blocks) {
      result.add(b.getEnd());
    }

    return result;
  }

  /**
   * Get the block.
   * @return the block starts
   */
  public List<GenomicInterval> getBlocks() {

    return Collections.unmodifiableList(this.blocks);
  }

  //
  // Setters
  //

  /**
   * Add metadata entry value.
   * @param key name of key of the metadata entry
   * @param value The value
   * @return true if the value is correctly added to the metadata
   */
  @Deprecated
  public final boolean addMetaDataEntry(final String key, final String value) {

    return this.metadata.add(key, value);
  }

  /**
   * Add metadata entries values. Stop at first entry that fail to be added.
   * @param entries the entries to add
   * @return true if all the entries are correctly added to the metadata
   */
  public final boolean addMetaDataEntries(
      final Map<String, List<String>> entries) {

    return this.metadata.add(entries);
  }

  /**
   * Remove a metadata entry.
   * @param key key of the metadata entry to remove
   * @return true if the entry is removed
   */
  public final boolean removeMetaDataEntry(final String key) {

    return this.metadata.remove(key);
  }

  /**
   * Set chromosome name.
   * @param chromosomeName the chromosome name
   */
  public void setChromosomeName(final String chromosomeName) {

    if (chromosomeName == null) {
      throw new NullPointerException("chromosomeName argument cannot be null");
    }

    this.chromosomeName = chromosomeName;
  }

  /**
   * Set the starting position of the feature in the chromosome (zero based).
   * @param start the start of the feature
   */
  public void setStart(final int start) {

    if (start < 1) {
      throw new IllegalArgumentException(
          "chromosomeStart argument cannot be lower than zero: " + start);
    }

    this.start = start;
  }

  /**
   * Set the ending position of the feature in the chromosome (one based).
   * @param end the end position on the chromosome
   */
  public void setEnd(final int end) {

    if (end < 1) {
      throw new IllegalArgumentException(
          "chromosomeEnd argument cannot be lower than zero: " + end);
    }

    this.end = end;
  }

  /**
   * Set the name of the BED feature.
   * @param name the name of the BED feature
   */
  public void setName(final String name) {

    Objects.requireNonNull(name, "name argument cannot be null");

    this.name = name.trim();
  }

  /**
   * Set the score of the feature.
   * @param score the score of the feature
   */
  public void setScore(final String score) {

    this.score = score;
  }

  /**
   * Set the score of the feature.
   * @param score the score of the feature
   */
  public void setScore(final int score) {

    if (score < 0 || score > 1000) {
      throw new IllegalArgumentException(
          "score must be in the range 0 - 1000: " + score);
    }

    this.score = Integer.toString(score);
  }

  /**
   * Set the score of the feature.
   * @param score the score of the feature
   */
  public void setScore(final double score) {

    this.score = Double.toString(score);
  }

  /**
   * Set the strand of the feature.
   * @param strand the strand of the feature
   */
  public void setStrand(final char strand) {

    switch (strand) {
    case '-':
    case '+':
    case 0:
      this.strand = strand;
      break;

    default:
      throw new IllegalArgumentException("Invalid strand value: " + strand);
    }
  }

  /**
   * Set the starting position at which the feature is drawn thickly.
   * @param thickStart the starting position at which the feature is drawn
   *          thickly
   */
  public void setThickStart(final int thickStart) {

    if (thickStart < 1) {
      throw new IllegalArgumentException(
          "thickStart argument cannot be lower than zero: " + thickStart);
    }

    this.thickStart = thickStart;
  }

  /**
   * Set the ending position at which the feature is drawn thickly.
   * @param thickEnd the ending position at which the feature is drawn thickly
   */
  public void setThickEnd(final int thickEnd) {

    if (thickEnd < 1) {
      throw new IllegalArgumentException(
          "thickEnd argument cannot be lower than zero: " + thickEnd);
    }

    this.thickEnd = thickEnd;
  }

  /**
   * Set the RGB value of the item.
   * @param rgbItem the RGB value of the item
   */
  public void setRgbItem(String rgbItem) {

    this.rgbItem = rgbItem;
  }

  /**
   * Set the RGB value of the item.
   * @param r the red value (0-255)
   * @param g the green value (0-255)
   * @param b the blue value (0-255)
   */
  public void setRgbItem(int r, int g, int b) {

    if (r < 0 || r > 255) {
      throw new IllegalArgumentException(
          "red value must be in [0-255] interval: " + r);
    }

    if (g < 0 || g > 255) {
      throw new IllegalArgumentException(
          "green value must be in [0-255] interval: " + g);
    }

    if (b < 0 || b > 255) {
      throw new IllegalArgumentException(
          "blue value must be in [0-255] interval: " + b);
    }

    this.rgbItem = "" + r + ',' + g + ',' + b;
  }

  /**
   * Add a block to the list of block.
   * @param startBlock start position of the block
   * @param endBlock end position of the block
   */
  public boolean addBlock(final int startBlock, final int endBlock) {

    return this.blocks.add(new GenomicInterval(this.chromosomeName, startBlock,
        endBlock, this.strand == 0 ? '.' : this.strand));
  }

  /**
   * Remove a block to the list of block.
   * @param startBlock start position of the block
   * @param endBlock end position of the block
   */
  public boolean removeBlock(final int startBlock, final int endBlock) {

    GenomicInterval block = new GenomicInterval(this.chromosomeName, startBlock,
        endBlock, this.strand == 0 ? '.' : this.strand);

    return this.blocks.remove(block);
  }

  //
  // toString methods
  //

  /**
   * Convert the object to a BED 3 columns entry.
   * @return a BED entry
   */
  public String toBED3() {

    return this.chromosomeName
        + '\t' + (this.start == -1 ? 0 : this.start - 1) + '\t'
        + (this.end == -1 ? 0 : this.end + 1);
  }

  /**
   * Convert the object to a BED 4 columns entry.
   * @return a BED entry
   */
  public String toBED4() {

    return this.chromosomeName
        + '\t' + (this.start == -1 ? 0 : this.start - 1) + '\t'
        + (this.end == -1 ? 0 : this.end + 1) + '\t' + this.name;
  }

  /**
   * Convert the object to a BED 5 columns entry.
   * @return a BED entry
   */
  public String toBED5() {

    return this.chromosomeName
        + '\t' + (this.start == -1 ? 0 : this.start - 1) + '\t'
        + (this.end == -1 ? 0 : this.end + 1) + '\t' + this.name + '\t'
        + ("".equals(this.score) ? this.score : '0');
  }

  /**
   * Convert the object to a BED 6 columns entry.
   * @return a BED entry
   */
  public String toBED6() {

    return this.chromosomeName
        + '\t' + (this.start == -1 ? 0 : this.start - 1) + '\t'
        + (this.end == -1 ? 0 : this.end + 1) + '\t' + this.name + '\t'
        + ("".equals(this.score) ? this.score : '0') + '\t'
        + (this.strand != 0 ? this.strand : "");
  }

  /**
   * Convert the object to a BED 12 columns entry.
   * @return a BED entry
   */
  public String toBED12() {

    StringBuilder sb = new StringBuilder();

    sb.append(this.chromosomeName);
    sb.append('\t');
    sb.append(this.start == -1 ? 0 : this.start - 1);
    sb.append('\t');
    sb.append(this.end == -1 ? 0 : this.end + 1);
    sb.append('\t');
    sb.append(this.name);
    sb.append('\t');
    sb.append("".equals(this.score) ? this.score : '0');
    sb.append('\t');
    sb.append(this.strand != 0 ? this.strand : "");
    sb.append('\t');
    sb.append(this.getThickStart() == -1 ? "0" : this.thickStart - 1);
    sb.append('\t');
    sb.append(this.getThickEnd() == -1 ? "0" : this.thickEnd + 1);
    sb.append('\t');
    sb.append(this.rgbItem);
    sb.append('\t');
    sb.append(getBlockCount());
    sb.append('\t');

    for (int i : getBlockStarts()) {
      sb.append(i - 1);
      sb.append(',');
    }
    sb.append('\t');

    for (int i : getBlockSizes()) {
      sb.append(i - 1);
      sb.append(',');
    }

    return sb.toString();
  }

  //
  // Object methods
  //

  @Override
  public String toString() {
    return toBED12();
  }

  @Override
  public int hashCode() {

    return Objects.hash(this.metadata, this.chromosomeName, this.start,
        this.end, this.name, this.score, this.strand, this.thickStart,
        this.thickEnd, this.rgbItem, this.blocks);
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof BEDEntry)) {
      return false;
    }

    final BEDEntry that = (BEDEntry) o;

    return Objects.equals(this.metadata, that.metadata)
        && Objects.equals(this.chromosomeName, that.chromosomeName)
        && this.start == that.start && this.end == that.end
        && Objects.equals(this.name, that.name)
        && Objects.equals(this.score, that.score) && this.strand == that.strand
        && this.thickStart == that.thickStart && this.thickEnd == that.thickEnd
        && Objects.equals(this.rgbItem, that.rgbItem)
        && Objects.equals(this.blocks, that.blocks);
  }

  //
  // Parse methods
  //

  /**
   * Parse an entry.
   * @param s the entry to parse
   * @throws BadBioEntryException if the entry is malformed
   */
  public void parse(final String s) throws BadBioEntryException {

    if (s == null) {
      throw new NullPointerException("s argument cannot be null");
    }

    // Count the number of fields
    int count = s.length() - s.replace("\t", "").length() + 1;

    parse(s, count);
  }

  /**
   * Parse an entry.
   * @param s the entry to parse
   * @param requiredFieldCount the required field count
   * @throws BadBioEntryException if the entry is malformed
   */
  public void parse(final String s, final int requiredFieldCount)
      throws BadBioEntryException {

    if (s == null) {
      throw new NullPointerException("s argument cannot be null");
    }

    if (requiredFieldCount < 3
        || requiredFieldCount > 12
        || (requiredFieldCount > 6 && requiredFieldCount < 12)) {
      throw new IllegalArgumentException(
          "Invalid required field count: " + requiredFieldCount);
    }

    clear();

    final Splitter splitter = Splitter.on('\t').trimResults();

    List<String> fields = GuavaCompatibility.splitToList(splitter, s);

    this.chromosomeName = fields.get(0);
    if (this.chromosomeName.isEmpty()) {
      throw new BadBioEntryException("chromosome name is empty", s);
    }

    this.start = parseCoordinate(fields.get(1), 1, Integer.MIN_VALUE);
    this.end = parseCoordinate(fields.get(2), -1, Integer.MAX_VALUE);

    if (requiredFieldCount == 3) {
      return;
    }

    this.name = fields.get(3);

    if (requiredFieldCount == 4) {
      return;
    }

    this.score = fields.get(4);

    if (requiredFieldCount == 5) {
      return;
    }

    switch (fields.get(5)) {

    case "+":
    case "-":
      this.strand = fields.get(5).charAt(0);
      break;

    default:
      this.strand = 0;
    }

    if (requiredFieldCount == 6) {
      return;
    }

    // Parse RGB
    setRgbItem(fields.get(8));

    this.thickStart = parseCoordinate(fields.get(6), 1, Integer.MIN_VALUE);
    this.thickEnd = parseCoordinate(fields.get(7), -1, Integer.MAX_VALUE);

    int blockCount = parseInt(fields.get(9), -1);

    if (blockCount == -1) {
      throw new BadBioEntryException("Invalid block count: " + fields.get(9),
          s);
    }

    List<Integer> starts = parseIntList(fields.get(10));
    List<Integer> sizes = parseIntList(fields.get(11));

    if (starts.size() != blockCount) {
      throw new BadBioEntryException("Invalid block starts: "
          + blockCount + "\t" + starts.size() + "\t" + fields.get(10), s);
    }
    if (sizes.size() != blockCount) {
      throw new BadBioEntryException("Invalid block sizes: " + fields.get(11),
          s);
    }

    for (int i = 0; i < blockCount; i++) {
      addBlock(starts.get(i) + 1, starts.get(i) + 1 + sizes.get(i));
    }

  }

  /**
   * Parse an integer in a String.
   * @param s String to parse
   * @param defaultValue the default value if the string is null
   * @return the parsed integer
   */
  private static int parseInt(final String s, final int defaultValue) {

    try {
      return Integer.parseInt(s.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Parse coordinates in a String.
   * @param s the string to parse
   * @param diff the difference between position in string in internal storage
   * @param defaultValue the default value
   * @return the parsed integer
   */
  private static int parseCoordinate(final String s, final int diff,
      final int defaultValue) {

    try {
      return Integer.parseInt(s.trim()) + diff;
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Parse integers in a string.
   * @param s String to parse
   * @return a list with the parsed integers
   */
  private List<Integer> parseIntList(final String s) {

    final Splitter splitter = Splitter.on(',').trimResults().omitEmptyStrings();

    List<Integer> result = new ArrayList<>();
    for (String v : splitter.split(s)) {

      try {
        result.add(Integer.parseInt(v));
      } catch (NumberFormatException e) {
      }
    }

    return result;
  }

  //
  // Other methods
  //

  /**
   * Clear the entry.
   */
  public void clear() {

    this.chromosomeName = "";
    this.start = -1;
    this.end = -1;
    this.name = "";
    this.score = "";
    this.strand = 0;
    this.thickStart = -1;
    this.thickEnd = -1;
    this.rgbItem = "0";
    this.blocks.clear();

  }

  /**
   * Clear the metadata of the entry.
   */
  public final void clearMetaData() {

    this.metadata.clear();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public BEDEntry() {

    this(new EntryMetadata());
  }

  /**
   * Public constructor.
   */
  public BEDEntry(EntryMetadata metadata) {

    requireNonNull(metadata, " metadata argument cannot  be null");

    this.metadata = metadata;
    clear();
  }

}