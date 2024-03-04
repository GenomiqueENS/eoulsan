package fr.ens.biologie.genomique.eoulsan.core.workflow;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static java.util.Objects.requireNonNull;

public class KConfiguration implements Serializable {

  private final Map<String, String> conf = new LinkedHashMap<>();
  private boolean sealed;

  //
  // Getters
  //

  /**
   * Test if configuration contains a key.
   *
   * @param key the key to test
   * @return true if the configuration contains the key
   */
  public boolean containsKey(String key) {

    requireNonNull(key);

    return this.conf.containsKey(key.trim());
  }

  /**
   * Get the value for a configuration key.
   *
   * @param key the key. The key must exists
   * @return a String with the value
   */
  public String get(String key) {

    requireNonNull(key);

    if (!containsKey(key)) {
      throw new NoSuchElementException(key);
    }

    return this.conf.get(key.trim());
  }

  /**
   * Get a boolean configuration key.
   *
   * @param key the key The key must exists
   * @return a boolean value
   */
  public boolean getBoolean(String key) {

    return Boolean.parseBoolean(get(key));
  }

  /**
   * Get an int configuration key.
   *
   * @param key the key The key must exists
   * @return a integer value
   */
  public int getInt(String key) {

    return Integer.parseInt(get(key));
  }

  /**
   * Get a long configuration key.
   *
   * @param key the key The key must exists
   * @return a long value
   */
  public long getLong(String key) {

    return Long.parseLong(get(key));
  }

  /**
   * Get a double configuration key.
   *
   * @param key the key The key must exists
   * @return a double value
   */
  public double getDouble(String key) {

    return Double.parseDouble(get(key));
  }

  /**
   * Get Path configuration key.
   *
   * @param key the key The key must exists
   * @return a Path value
   */
  public Path getPath(String key) {

    return Paths.get(get(key));
  }

  /**
   * Get the value for a configuration key.
   *
   * @param key          the key
   * @param defaultValue value if the key does not exists
   * @return a String with the value
   */
  public String get(String key, String defaultValue) {

    requireNonNull(defaultValue);

    if (!containsKey(key)) {
      return defaultValue;
    }

    return this.conf.get(key.trim());
  }

  /**
   * Get a boolean value for a configuration key.
   *
   * @param key          the key
   * @param defaultValue value if the key does not exists
   * @return a String with the value
   */
  public boolean getBoolean(String key, boolean defaultValue) {

    return Boolean.parseBoolean(get(key, Boolean.toString(defaultValue)));
  }

  /**
   * Get an integer value for a configuration key.
   *
   * @param key          the key
   * @param defaultValue value if the key does not exists
   * @return a integer
   */
  public int getInt(String key, int defaultValue) {

    return Integer.parseInt(get(key, Integer.toString(defaultValue)));
  }

  /**
   * Get a long value for a configuration key.
   *
   * @param key          the key
   * @param defaultValue value if the key does not exists
   * @return a long
   */
  public long getLong(String key, long defaultValue) {

    return Long.parseLong(get(key, Long.toString(defaultValue)));
  }

  /**
   * Get a double value for a configuration key.
   *
   * @param key          the key
   * @param defaultValue value if the key does not exists
   * @return a double
   */
  public double getDouble(String key, double defaultValue) {

    return Double.parseDouble(get(key, Double.toString(defaultValue)));
  }

  /**
   * Get a Path value for a configuration key.
   *
   * @param key          the key
   * @param defaultValue value if the key does not exists
   * @return a Path
   */
  public Path getPath(String key, Path defaultValue) {

    requireNonNull(defaultValue);

    if (!containsKey(key)) {
      return defaultValue;
    }

    return Paths.get(get(key));
  }

  //
  // Setters
  //

  /**
   * Set a value.
   *
   * @param key   the key
   * @param value the value
   */
  public void set(String key, String value) {

    requireNonNull(key);
    if (this.sealed) {
      throw new IllegalStateException("Configuration is sealed");
    }

    this.conf.put(key.trim(), value);
  }

  /**
   * Set a value.
   *
   * @param key   the key
   * @param value the value
   */
  public void set(String key, boolean value) {

    requireNonNull(key);

    set(key.trim(), Boolean.toString(value));
  }

  /**
   * Set a value.
   *
   * @param key   the key
   * @param value the value
   */
  public void set(String key, Number value) {

    set(key, value.toString());
  }

  //
  // Other
  //

  /**
   * Convert the configuration as a Map.
   *
   * @return a copy of the configuration object in a map
   */
  public Map<String, String> toMap() {

    return new LinkedHashMap<String, String>(this.conf);
  }

  /**
   * Return the size of the configuration.
   *
   * @return the number of elements in the configuration
   */
  public int size() {
    return this.conf.size();
  }

  /**
   * Test if the configuration is empty.
   *
   * @return true if the configuration is empty
   */
  public boolean isEmpty() {
    return this.conf.isEmpty();
  }

  @Override public String toString() {
    return this.getClass().getSimpleName() + " [conf=" + conf + "]";
  }

  public void seal() {
    this.sealed = true;
  }

  //
  // Constructors
  //

  /**
   * Default constructor.
   */
  public KConfiguration() {
  }

}
