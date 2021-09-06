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

package fr.ens.biologie.genomique.eoulsan.design;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.MoreObjects;

import fr.ens.biologie.genomique.eoulsan.util.StringUtils;

/**
 * This abstract class defines methods for metadata.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractMetadata implements Metadata, Serializable {

  /** Serialization version UID. */
  private static final long serialVersionUID = 5756414666624839231L;

  private final Map<String, String> metadata = new LinkedHashMap<>();

  //
  // Methods
  //

  @Override
  public String get(final String key) {

    requireNonNull(key, "key argument cannot be null");

    return this.metadata.get(key.trim());
  }

  @Override
  public String getTrimmed(final String key) {

    final String value = get(key);

    return value != null ? value.trim() : null;
  }

  @Override
  public void set(final String key, final String value) {

    requireNonNull(key, "key argument cannot be null");
    requireNonNull(value, "value argument cannot be null");

    this.metadata.put(key, value);
  }

  @Override
  public void set(final String key, final List<String> value) {

    requireNonNull(key, "key argument cannot be null");
    requireNonNull(value, "value argument cannot be null");

    switch (value.size()) {

    case 0:
      set(key, "");
      break;

    case 1:
      set(key, value.get(0));
      break;

    default:
      set(key, StringUtils.serializeStringArray(value));
    }
  }

  @Override
  public int size() {

    return this.metadata.size();
  }

  @Override
  public boolean isEmpty() {

    return this.metadata.isEmpty();
  }

  @Override
  public boolean contains(final String key) {

    requireNonNull(key, "key argument cannot be null");

    return this.metadata.containsKey(key.trim());
  }

  @Override
  public List<String> getAsList(final String key) {

    requireNonNull(key, "key argument cannot be null");

    return StringUtils.deserializeStringArray(get(key.trim()));
  }

  @Override
  public boolean getAsBoolean(final String key) {

    requireNonNull(key, "key argument cannot be null");

    String value = get(key.trim());
    if (value == null) {
      return false;
    }
    return Boolean.parseBoolean(value.toLowerCase());
  }

  @Override
  public Set<String> keySet() {

    return Collections.unmodifiableSet(this.metadata.keySet());
  }

  @Override
  public Set<Map.Entry<String, String>> entrySet() {

    return Collections.unmodifiableSet(this.metadata.entrySet());
  }

  @Override
  public void remove(final String key) {

    requireNonNull(key, "key argument cannot be null");

    this.metadata.remove(key.trim());
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return MoreObjects.toStringHelper(this)
        .add("metadata", this.metadata).toString();
  }

  @Override
  public int hashCode() {

    return Objects.hash(this.metadata);
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof AbstractMetadata)) {
      return false;
    }

    final AbstractMetadata that = (AbstractMetadata) o;

    return Objects.equals(this.metadata, that.metadata);
  }

}
