package fr.ens.biologie.genomique.eoulsan.bio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This class define a metadata for bio entries objects like GFFEntry or
 * BedEntry.
 * @author Laurent Jourdren
 * @since 2.3
 */
public final class EntryMetadata {

  private final Map<String, List<String>> map = new LinkedHashMap<>();

  /**
   * Add metadata entry value.
   * @param key name of key of the metadata entry
   * @param value The value
   * @return true if the value is correctly added to the metadata
   */
  public boolean add(final String key, final String value) {

    if (key == null || value == null) {
      return false;
    }

    final List<String> list;

    if (!this.map.containsKey(key)) {
      list = new ArrayList<>();
      this.map.put(key, list);
    } else {
      list = this.map.get(key);
    }

    list.add(value);

    return true;
  }

  /**
   * Add metadata entries values. Stop at first entry that fail to be added.
   * @param metadata the metadata entries to add
   * @return true if all the entries are correctly added to the metadata
   */
  public boolean add(EntryMetadata metadata) {

    if (metadata == null) {
      return false;
    }

    return add(metadata.entries());
  }

  /**
   * Add metadata entries values. Stop at first entry that fail to be added.
   * @param entries the entries to add
   * @return true if all the entries are correctly added to the metadata
   */
  public boolean add(final Map<String, List<String>> entries) {

    if (entries == null) {
      return false;
    }

    for (Map.Entry<String, List<String>> e : entries.entrySet()) {

      if (e.getValue() == null) {
        return false;
      }

      for (String v : e.getValue()) {

        if (!add(e.getKey(), v)) {
          return false;
        }

      }
    }

    return true;
  }

  /**
   * test if a metadata key exists.
   * @param key key name of the metadata
   * @return true if the entry in the meta data exists
   */
  public boolean containsKey(final String key) {

    return this.map.containsKey(key);
  }

  /**
   * Get the metadata values for a key.
   * @param key name of the metadata entry
   * @return the values of the attribute or null if the metadata name does not
   *         exists
   */
  public List<String> get(final String key) {

    final List<String> list = this.map.get(key);

    if (list == null) {
      return Collections.emptyList();
    }

    return Collections.unmodifiableList(list);

  }

  /**
   * Get metadata keys names.
   * @return the metadata keys names
   */
  public Set<String> keySet() {

    return Collections.unmodifiableSet(this.map.keySet());
  }

  /**
   * Get all the metadata entries.
   * @return a map with all the metadata entries
   */
  public Map<String, List<String>> entries() {

    return Collections.unmodifiableMap(this.map);
  }

  /**
   * Remove a metadata entry.
   * @param key key of the metadata entry to remove
   * @return true if the entry is removed
   */
  public final boolean remove(final String key) {

    return this.map.remove(key) != null;
  }

  /**
   * Clear the metadata of the entry.
   */
  public void clear() {

    this.map.clear();
  }

  //
  // Object methods
  //

  @Override
  public int hashCode() {

    return this.map.hashCode();
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof EntryMetadata)) {
      return false;
    }

    final EntryMetadata that = (EntryMetadata) o;

    return this.map.equals(that.map);
  }

  @Override
  public String toString() {

    return this.map.toString();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public EntryMetadata() {
  }

  /**
   * Public constructor.
   * @param metadata metadata entries to add to the new object
   */
  public EntryMetadata(final EntryMetadata metadata) {

    Objects.requireNonNull(metadata, "metadata argument cannot be null");

    add(metadata);
  }

}