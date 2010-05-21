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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define a GFF Entry.
 * @author Laurent Jourdren
 */
public class GFFEntry {

  private Map<String, String> metaData = new LinkedHashMap<String, String>();
  private int id;
  private String seqId;
  private String source;
  private String type;
  private int start;
  private int end;
  private double score;
  private char strand;
  private int phase;
  private Map<String, String> attributes;
  private String[] parsedFields;
  private static final Pattern SEMI_COMA_SPLIT_PATTERN = Pattern.compile(";");

  //
  // Getters
  //

  /**
   * Get the id
   * @return the id
   */
  public int getId() {
    return id;
  }

  /**
   * Get the seqId
   * @return the seqId
   */
  public String getSeqId() {
    return seqId;
  }

  /**
   * Get the source.
   * @return The source
   */
  public String getSource() {
    return source;
  }

  /**
   * Get the type.
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Get the start position.
   * @return the start position
   */
  public int getStart() {
    return start;
  }

  /**
   * Get the end position.
   * @return the end position
   */
  public int getEnd() {
    return end;
  }

  /**
   * Get the score
   * @return the score
   */
  public double getScore() {
    return score;
  }

  /**
   * Get the strand
   * @return the strand
   */
  public char getStrand() {
    return strand;
  }

  /**
   * Get the phase
   * @return the phase
   */
  public int getPhase() {
    return phase;
  }

  /**
   * Get metadata keys names
   * @return the metadata keys names
   */
  public Set<String> getMetadataKeyNames() {

    if (this.metaData == null)
      return Collections.emptySet();

    return Collections.unmodifiableSet(this.metaData.keySet());
  }

  /**
   * Get attributes names
   * @return the attributes names
   */
  public Set<String> getAttributesNames() {

    if (this.attributes == null)
      return Collections.emptySet();

    return Collections.unmodifiableSet(this.attributes.keySet());
  }

  /**
   * test if a metadata key exists.
   * @param key key name of the metadata
   * @return true if the entry in the meta data exists
   */
  public boolean isMetaDataEntry(final String key) {

    if (key == null)
      return false;

    return this.metaData.containsKey(key);
  }

  /**
   * test if an attribute exists.
   * @param attributeName name of the attribute
   * @return true if the attribute exits
   */
  public boolean isAttribute(final String attributeName) {

    if (attributeName == null || this.attributes == null)
      return false;

    return this.attributes.containsKey(attributeName);
  }

  /**
   * Get a metadata entry value
   * @param key name of the metadata entry
   * @return the value of the attribute or null if the attribute name does not
   *         exists
   */
  public String getMetadataEntryValue(final String key) {

    if (key == null)
      return null;

    return this.metaData.get(key);
  }

  /**
   * Get attribute value
   * @param attributeName name of the attribute
   * @return the value of the attribute or null if the attribute name does not
   *         exists
   */
  public String getAttributeValue(final String attributeName) {

    if (attributeName == null || this.attributes == null)
      return null;

    return this.attributes.get(attributeName);
  }

  //
  // Setters
  //

  /**
   * Set the id
   * @param id the id of the entry
   */
  public void setId(final int id) {

    this.id = id;
  }

  /**
   * Set the seqId
   * @param seqId the sequence id of the entry
   */
  public void setSeqId(final String seqId) {

    if (seqId == null || ".".equals(seqId))
      this.seqId = "";
    else
      this.seqId = seqId;
  }

  /**
   * Set the source of the entry
   * @param source the source of the entry
   */
  public void setSource(final String source) {

    if (source == null || ".".equals(source))
      this.source = "";
    else
      this.source = source;
  }

  /**
   * Set the type of the entry
   * @param type the type of the entry
   */
  public void setType(final String type) {

    if (type == null || ".".equals(type))
      this.type = "";
    else
      this.type = type;
  }

  /**
   * Set the start position of the entry
   * @param start the start position
   */
  public void setStart(final int start) {
    this.start = start;
  }

  /**
   * Set the end of the position
   * @param end the end position
   */
  public void setEnd(final int end) {
    this.end = end;
  }

  /**
   * Set the score of the position
   * @param score the score of the position
   */
  public void setScore(final double score) {
    this.score = score;
  }

  /**
   * Set the strand of the position
   * @param strand the strand of the position
   */
  public void setStrand(final char strand) {

    switch (strand) {

    case '.':
    case '+':
    case '-':
    case '?':
      this.strand = strand;
      break;
    default:
      this.strand = '.';
    }

  }

  /**
   * Set the phase of the entry
   * @param phase the phase
   */
  public void setPhase(final int phase) {

    if (phase < 0 || phase > 2)
      this.phase = -1;
    else
      this.phase = phase;
  }

  /**
   * Get metadata entry value
   * @param key name of key of the metadata entry
   * @param value The value
   * @return true if the value is correctly added to the metadata
   */
  public boolean setMetaDataEntry(final String key, final String value) {

    if (key == null || value == null)
      return false;

    this.metaData.put(key, value);

    return true;
  }

  /**
   * Get attribute value
   * @param attributeName name of the attribute
   * @param value The value
   * @return true if the value is correctly added to the attributes of the entry
   *         exists
   */
  public boolean setAttributeValue(final String attributeName,
      final String value) {

    if (attributeName == null || value == null)
      return false;

    if (this.attributes == null)
      this.attributes = new LinkedHashMap<String, String>();

    this.attributes.put(attributeName, value);

    return true;
  }

  /**
   * Remove a metadata entry
   * @param key key of the metadata entry to remove
   * @return true if the entry is removed
   */
  public boolean removeMetaDataEntry(final String key) {

    if (this.metaData.containsKey(key))
      return false;

    return this.metaData.remove(key) != null;
  }

  /**
   * Remove an attribute
   * @param attributeName attribute to remove
   * @return true if the attribute is removed
   */
  public boolean removeAttribute(final String attributeName) {

    if (this.attributes == null || this.attributes.containsKey(attributeName))
      return false;

    return this.attributes.remove(attributeName) != null;
  }

  //
  // Other methods
  //

  /**
   * Clear the entry
   */
  public void clear() {

    this.seqId = this.source = this.type = "";
    this.start = Integer.MIN_VALUE;
    this.end = Integer.MAX_VALUE;
    this.score = Double.NaN;
    this.strand = '.';
    this.phase = -1;
    if (this.attributes != null)
      this.attributes.clear();
  }

  public void clearMetaData() {

    this.metaData.clear();
  }

  //
  // Test valid entries
  //

  /**
   * Test if the entry is valid.
   * @return true if the entry is valid
   */
  public boolean isValidEntry() {

    return seqId == null
        && source != null && type != null && isValidStartAndEnd()
        && isValidStrand();
  }

  /**
   * Test if the start and end position values are valids.
   * @return true if the positions are valids
   */
  public boolean isValidStartAndEnd() {

    if (this.start == Integer.MIN_VALUE || this.end == Integer.MAX_VALUE)
      return false;

    if (this.start < 1)
      return false;

    if (this.end < this.start)
      return false;

    return true;
  }

  /**
   * Test if the strand is valid.
   * @return true if the strand is valid
   */
  public boolean isValidStrand() {

    switch (this.strand) {

    case '+':
    case '-':
    case '.':
    case '?':
      return true;
    default:
      return false;
    }
  }

  /**
   * Test if the phase is valid.
   * @return true if the phase is valid
   */
  public boolean isValidPhase() {

    if ("CDS".equals(this.type)) {

      if (this.phase == -1)
        return false;

    } else if (this.phase != -1)
      return false;

    return true;
  }

  //
  // Parsing / Write methods
  //

  private static final int parseInt(final String s, final int defaultValue) {

    if (s == null)
      return defaultValue;

    try {
      return Integer.parseInt(s.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static final double parseDouble(final String s,
      final double defaultValue) {

    if (s == null)
      return defaultValue;

    try {
      return Double.parseDouble(s.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private final void parseAttributes(final String attributesField) {

    if (this.attributes != null)
      this.attributes.clear();

    if (attributesField == null)
      return;

    final String s = attributesField.trim();
    if (attributesField.equals("") || attributesField.equals("."))
      return;

    final String[] fields = SEMI_COMA_SPLIT_PATTERN.split(s);
    for (String f : fields) {

      final int indexEquals = f.indexOf('=');
      if (indexEquals == -1)
        continue;
      final String key = f.substring(0, indexEquals).trim();
      final String value = f.substring(indexEquals + 1).trim();

      setAttributeValue(key, value);
    }

  }

  /**
   * Parse a GFF entry
   * @param s String to parse
   */
  public void parse(final String s) {

    if (s == null)
      throw new NullPointerException("String to parse is null");

    if (this.parsedFields == null)
      this.parsedFields = new String[9];
    else
      Arrays.fill(this.parsedFields, null);

    final String[] fields = this.parsedFields;

    StringUtils.fastSplit(s, fields);

    setSeqId(fields[0]);
    setSource(fields[1]);
    setType(fields[2]);

    setStart(parseInt(fields[3], Integer.MIN_VALUE));
    setEnd(parseInt(fields[4], Integer.MIN_VALUE));
    setScore(parseDouble(fields[5], Double.NaN));

    setStrand(fields[6] == null || fields[6].length() == 0 ? '.' : fields[6]
        .charAt(0));
    setPhase(parseInt(fields[7], -1));
    parseAttributes(fields[8]);
  }

  private String attributesToString() {

    if (this.attributes == null || this.attributes.size() == 0)
      return ".";

    final StringBuilder sb = new StringBuilder();

    boolean first = true;

    for (Map.Entry<String, String> e : this.attributes.entrySet()) {

      if (first)
        first = false;
      else
        sb.append(';');

      sb.append(StringUtils.protectGFF(e.getKey()));
      sb.append('=');
      sb.append(StringUtils.protectGFF(e.getValue()).replace("\\,", ","));
    }

    return sb.toString();
  }

  /**
   * Overide toString().
   * @return the GFF entry in GFF3 format
   */
  public String toString() {

    final String seqId = getSeqId();
    final String source = getSource();
    final String type = getType();

    return ("".equals(seqId) ? "." : StringUtils.protectGFF(seqId))
        + '\t' + ("".equals(source) ? "." : StringUtils.protectGFF(source))
        + '\t' + ("".equals(type) ? "." : StringUtils.protectGFF(type)) + '\t'
        + (getStart() == Integer.MIN_VALUE ? "." : getStart()) + '\t'
        + (getEnd() == Integer.MAX_VALUE ? "." : getEnd()) + '\t'
        + (Double.isNaN(getScore()) ? "." : getScore()) + '\t' + getStrand()
        + '\t' + (getPhase() == -1 ? "." : getPhase()) + '\t'
        + attributesToString();
  }

}
