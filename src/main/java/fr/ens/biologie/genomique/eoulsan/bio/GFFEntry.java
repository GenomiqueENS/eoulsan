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

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.util.GuavaCompatibility;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;

/**
 * This class defines a GFF Entry.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class GFFEntry {

  private static final Pattern SEMI_COMA_SPLIT_PATTERN = Pattern.compile(";");
  private static final Pattern COMA_SPLIT_PATTERN = Pattern.compile(",");

  // private final Map<String, List<String>> metaData = new LinkedHashMap<>();
  private final EntryMetadata metadata;
  private int id;
  private String seqId;
  private String source;
  private String type;
  private int start;
  private int end;
  private double score;
  private char strand;
  private int phase;
  private final Map<String, String> attributes = new LinkedHashMap<>();

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
   * Get the id.
   * @return the id
   */
  public final int getId() {
    return this.id;
  }

  /**
   * Get the seqId.
   * @return the seqId
   */
  public final String getSeqId() {
    return this.seqId;
  }

  /**
   * Get the source.
   * @return The source
   */
  public final String getSource() {
    return this.source;
  }

  /**
   * Get the type.
   * @return the type
   */
  public final String getType() {
    return this.type;
  }

  /**
   * Get the start position.
   * @return the start position
   */
  public final int getStart() {
    return this.start;
  }

  /**
   * Get the end position.
   * @return the end position
   */
  public final int getEnd() {
    return this.end;
  }

  /**
   * Get the length of the feature.
   * @return the end position
   */
  public final int getLength() {
    return this.end - this.start + 1;
  }

  /**
   * Get the score.
   * @return the score
   */
  public final double getScore() {
    return this.score;
  }

  /**
   * Get the strand.
   * @return the strand
   */
  public final char getStrand() {
    return this.strand;
  }

  /**
   * Get the phase.
   * @return the phase
   */
  public final int getPhase() {
    return this.phase;
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
   * Get attributes names.
   * @return the attributes names
   */
  public final Set<String> getAttributesNames() {

    return Collections.unmodifiableSet(this.attributes.keySet());
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
   * test if an attribute exists.
   * @param attributeName name of the attribute
   * @return true if the attribute exits
   */
  public final boolean isAttribute(final String attributeName) {

    return this.attributes.containsKey(attributeName);
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
   * Get attribute value.
   * @param attributeName name of the attribute
   * @return the value of the attribute or null if the attribute name does not
   *         exists
   */
  public final String getAttributeValue(final String attributeName) {

    return this.attributes.get(attributeName);
  }

  //
  // Setters
  //

  /**
   * Set the id.
   * @param id the id of the entry
   */
  public final void setId(final int id) {

    this.id = id;
  }

  /**
   * Set the seqId.
   * @param seqId the sequence id of the entry
   */
  public final void setSeqId(final String seqId) {

    if (seqId == null || ".".equals(seqId)) {
      this.seqId = "";
    } else {
      this.seqId = seqId.trim();
    }
  }

  /**
   * Set the source of the entry.
   * @param source the source of the entry
   */
  public final void setSource(final String source) {

    if (source == null || ".".equals(source)) {
      this.source = "";
    } else {
      this.source = source.trim();
    }
  }

  /**
   * Set the type of the entry.
   * @param type the type of the entry
   */
  public final void setType(final String type) {

    if (type == null || ".".equals(type)) {
      this.type = "";
    } else {
      this.type = type.trim();
    }
  }

  /**
   * Set the start position of the entry.
   * @param start the start position
   */
  public final void setStart(final int start) {

    if (start < 1) {
      this.start = -1;
    } else {
      this.start = start;
    }
  }

  /**
   * Set the end of the position.
   * @param end the end position
   */
  public final void setEnd(final int end) {

    if (end < 1) {
      this.end = -1;
    } else {
      this.end = end;
    }
  }

  /**
   * Set the score of the position.
   * @param score the score of the position
   */
  public final void setScore(final double score) {
    this.score = score;
  }

  /**
   * Set the strand of the position.
   * @param strand the strand of the position
   */
  public final void setStrand(final char strand) {

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
   * Set the phase of the entry.
   * @param phase the phase
   */
  public final void setPhase(final int phase) {

    if (phase < 0 || phase > 2) {
      this.phase = -1;
    } else {
      this.phase = phase;
    }
  }

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
  @Deprecated
  public final boolean addMetaDataEntries(
      final Map<String, List<String>> entries) {

    return this.metadata.add(entries);
  }

  /**
   * Get attribute value.
   * @param attributeName name of the attribute
   * @param value The value
   * @return true if the value is correctly added to the attributes of the entry
   *         exists
   */
  public final boolean setAttributeValue(final String attributeName,
      final String value) {

    if (attributeName == null || value == null) {
      return false;
    }

    this.attributes.put(attributeName, value);

    return true;
  }

  /**
   * Remove a metadata entry.
   * @param key key of the metadata entry to remove
   * @return true if the entry is removed
   */
  @Deprecated
  public final boolean removeMetaDataEntry(final String key) {

    return this.metadata.remove(key);
  }

  /**
   * Remove an attribute.
   * @param attributeName attribute to remove
   * @return true if the attribute is removed
   */
  public final boolean removeAttribute(final String attributeName) {

    return this.attributes.remove(attributeName) != null;
  }

  //
  // Other methods
  //

  /**
   * Clear the entry.
   */
  public final void clear() {

    this.seqId = "";
    this.source = "";
    this.type = "";
    this.start = -1;
    this.end = -1;
    this.score = Double.NaN;
    this.strand = '.';
    this.phase = -1;
    this.attributes.clear();
  }

  /**
   * Clear metadata of the entry.
   */
  @Deprecated
  public final void clearMetaData() {

    this.metadata.clear();
  }

  //
  // Test valid entries
  //

  /**
   * Test if the entry is valid.
   * @return true if the entry is valid
   */
  public final boolean isValidEntry() {

    // The only data that cannot be valid are the positions of the feature
    return isValidStartAndEnd();
  }

  /**
   * Test if the start and end position values are valid.
   * @return true if the positions are valid
   */
  public final boolean isValidStartAndEnd() {

    if (this.start < 1) {
      return false;
    }

    if (this.end < this.start) {
      return false;
    }

    return true;
  }

  /**
   * Test if the strand is valid.
   * @return true if the strand is valid
   */
  public final boolean isValidStrand() {

    // Always true with the current implementation of the class
    return true;
  }

  /**
   * Test if the phase is valid.
   * @return true if the phase is valid
   */
  public final boolean isValidPhase() {

    if ("CDS".equals(this.type)) {

      if (this.phase == -1) {
        return false;
      }

    } else if (this.phase != -1) {
      return false;
    }

    return true;
  }

  //
  // Parsing / Write methods
  //

  private static int parseInt(final String s, final int defaultValue) {

    try {
      return Integer.parseInt(s.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static double parseDouble(final String s, final double defaultValue) {

    try {
      return Double.parseDouble(s.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Parse the first fields of a GFF/GTF string.
   * @param s the string to parse
   * @return the last non parsed field
   * @throws BadBioEntryException if an error occurs while parsing the string
   */
  private String parseCommon(final String s) throws BadBioEntryException {

    if (s == null) {
      throw new NullPointerException("String to parse is null");
    }

    List<String> fields =
        GuavaCompatibility.splitToList(Splitter.on('\t').trimResults(), s);

    if (fields.size() != 9) {

      throw new BadBioEntryException("Error in GFF parsing line ("
          + s.split("\t").length + " fields, 9 attempted)", s);
    }

    setSeqId(fields.get(0));
    setSource(fields.get(1));
    setType(fields.get(2));

    setStart(parseInt(fields.get(3), Integer.MIN_VALUE));
    setEnd(parseInt(fields.get(4), Integer.MIN_VALUE));
    setScore(parseDouble(fields.get(5), Double.NaN));

    setStrand(fields.get(6).length() == 0 ? '.' : fields.get(6).charAt(0));
    setPhase(parseInt(fields.get(7), -1));

    return fields.get(8);
  }

  /**
   * Parse the attribute field in GFF3 format.
   * @param attributesField the attribute field
   */
  private void parseGFF3Attributes(final String attributesField) {

    this.attributes.clear();

    if ("".equals(attributesField) || ".".equals(attributesField)) {
      return;
    }

    final String s = attributesField.trim();

    final String[] fields = SEMI_COMA_SPLIT_PATTERN.split(s);
    for (String f : fields) {

      final int indexEquals = f.indexOf('=');
      if (indexEquals == -1) {
        continue;
      }

      final String key = f.substring(0, indexEquals).trim();
      final String value = f.substring(indexEquals + 1).trim();

      setAttributeValue(key, value);
    }
  }

  /**
   * Parse the attribute field in GTF format.
   * @param attributesField the attribute field
   */
  private void parseGTFAttributes(final String attributesField) {

    this.attributes.clear();

    if ("".equals(attributesField) || ".".equals(attributesField)) {
      return;
    }

    final String s = attributesField.trim();

    final String[] fields = SEMI_COMA_SPLIT_PATTERN.split(s);
    for (String f : fields) {

      f = f.trim();

      if (f.isEmpty()) {
        continue;
      }

      final int indexEquals = f.indexOf(' ');
      if (indexEquals == -1) {
        continue;
      }

      final String key = f.substring(0, indexEquals).trim();
      final String value = StringUtils
          .unDoubleQuotes(f.substring(indexEquals + 1).trim()).trim();

      if (getAttributesNames().contains(key)) {
        setAttributeValue(key, getAttributeValue(key) + ',' + value);
      } else {
        setAttributeValue(key, value);
      }
    }
  }

  /**
   * Parse a GFF entry. This method is deprecated, use <tt>parseGFF3()</tt>
   * instead.
   * @param s String to parse
   * @deprecated
   */
  @Deprecated
  public void parse(final String s) throws BadBioEntryException {

    parseGFF3(s);
  }

  /**
   * Parse a GFF3 entry.
   * @param s String to parse
   */
  public void parseGFF3(final String s) throws BadBioEntryException {

    final String attributeField = parseCommon(s);
    parseGFF3Attributes(attributeField);
  }

  /**
   * Parse a GTF entry.
   * @param s String to parse
   */
  public void parseGTF(final String s) throws BadBioEntryException {

    final String attributeField = parseCommon(s);
    parseGTFAttributes(attributeField);
  }

  /**
   * Convert the attributes to a GFF3 string.
   * @return a the attribute in the GFF3 format
   */
  private String attributesToGFF3String() {

    if (this.attributes.size() == 0) {
      return ".";
    }

    final StringBuilder sb = new StringBuilder();

    boolean first = true;

    for (Map.Entry<String, String> e : this.attributes.entrySet()) {

      if (first) {
        first = false;
      } else {
        sb.append(';');
      }

      sb.append(StringUtils.protectGFF(e.getKey()));
      sb.append('=');
      sb.append(StringUtils.protectGFF(e.getValue()).replace("\\,", ","));
    }

    return sb.toString();
  }

  /**
   * Convert the attributes to a GTF string.
   * @return a the attribute in the GTF format
   */
  private String attributesToGTFString() {

    if (this.attributes.size() == 0) {
      return ".";
    }

    final StringBuilder sb = new StringBuilder();

    boolean first = true;

    for (Map.Entry<String, String> e : this.attributes.entrySet()) {

      final String key = e.getKey();

      for (String value : COMA_SPLIT_PATTERN.split(e.getValue())) {

        if (first) {
          first = false;
        } else {
          sb.append(' ');
        }

        sb.append(key);
        sb.append(" \"");

        sb.append(value);
        sb.append('\"');
        sb.append(';');
      }
    }

    return sb.toString();
  }

  /**
   * Override toString().
   * @return the GFF entry in GFF3 format
   */
  public String toGFF3() {

    final String seqId = getSeqId();
    final String source = getSource();
    final String type = getType();

    return ("".equals(seqId) ? "." : StringUtils.protectGFF(seqId))
        + '\t' + ("".equals(source) ? "." : StringUtils.protectGFF(source))
        + '\t' + ("".equals(type) ? "." : StringUtils.protectGFF(type)) + '\t'
        + (getStart() == -1 ? "." : getStart()) + '\t'
        + (getEnd() == -1 ? "." : getEnd()) + '\t'
        + (Double.isNaN(getScore()) ? "." : getScore()) + '\t' + getStrand()
        + '\t' + (getPhase() == -1 ? "." : getPhase()) + '\t'
        + attributesToGFF3String();
  }

  /**
   * Override toString().
   * @return the GFF entry in GTF format
   */
  public String toGTF() {

    final String seqId = getSeqId();
    final String source = getSource();
    final String type = getType();

    return ("".equals(seqId) ? "." : StringUtils.protectGFF(seqId))
        + '\t' + ("".equals(source) ? "." : StringUtils.protectGFF(source))
        + '\t' + ("".equals(type) ? "." : StringUtils.protectGFF(type)) + '\t'
        + (getStart() == -1 ? "." : getStart()) + '\t'
        + (getEnd() == -1 ? "." : getEnd()) + '\t'
        + (Double.isNaN(getScore()) ? "." : getScore()) + '\t' + getStrand()
        + '\t' + (getPhase() == -1 ? "." : getPhase()) + '\t'
        + attributesToGTFString();
  }

  //
  // Object methods
  //

  /**
   * Override toString().
   * @return the GFF entry in GFF3 format
   */
  @Override
  public String toString() {

    return toGFF3();
  }

  @Override
  public int hashCode() {

    return Objects.hash(this.metadata, this.id, this.seqId, this.source,
        this.type, this.start, this.end, this.score, this.strand, this.phase,
        this.attributes);
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof GFFEntry)) {
      return false;
    }

    final GFFEntry that = (GFFEntry) o;

    return Objects.equals(this.metadata, that.metadata)
        && this.id == that.id && Objects.equals(this.seqId, that.seqId)
        && Objects.equals(this.source, that.source)
        && Objects.equals(this.type, that.type) && this.start == that.start
        && this.end == that.end && Objects.equals(this.score, that.score)
        && this.strand == that.strand && this.phase == that.phase
        && Objects.equals(this.attributes, that.attributes);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public GFFEntry() {

    this(new EntryMetadata());
  }

  /**
   * Public constructor.
   */
  public GFFEntry(EntryMetadata metadata) {

    requireNonNull(metadata, " metadata argument cannot  be null");

    this.metadata = metadata;
    clear();
  }

}
