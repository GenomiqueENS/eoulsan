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

package fr.ens.transcriptome.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.data.DataMetadata;

/**
 * This class define a simple class for metadata of data objects.
 * @since 2.0
 * @author Laurent Jourdren
 */
class SimpleDataMetaData extends AbstractDataMetaData implements Serializable {

  private static final long serialVersionUID = -2905676240064559127L;

  private final Map<String, String> map = Maps.newHashMap();

  @Override
  public String get(final String key) {

    checkNotNull(key, "key argument cannot be null");

    return this.map.get(key);
  }

  @Override
  public void set(final String key, final String value) {

    checkNotNull(key, "key argument cannot be null");
    checkNotNull(value, "value argument cannot be null");

    this.map.put(key, value);
  }

  @Override
  public boolean containsKey(final String key) {

    checkNotNull(key, "key argument cannot be null");

    return this.map.containsKey(key);
  }

  @Override
  public boolean removeKey(final String key) {

    checkNotNull(key, "key argument cannot be null");

    return this.removeKey(key);
  }

  @Override
  public void set(final DataMetadata metadata) {

    checkNotNull(metadata, "metadata argument cannot be null");

    for (String key : metadata.keySet()) {
      set(key, metadata.get(key));
    }
  }

  @Override
  public void clear() {

    this.map.clear();
  }

  @Override
  public Set<String> keySet() {

    return this.map.keySet();
  }

  @Override
  public String toString() {

    return this.map.toString();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   */
  SimpleDataMetaData() {
  }

}
