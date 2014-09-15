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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.amazonaws.services.importexport.model.InvalidParameterException;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.data.DataMetadata;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;

/**
 * This class define a simple class for metadata of data objects.
 * @since 2.0
 * @author Laurent Jourdren
 */
class SimpleDataMetaData extends AbstractDataMetaData implements Serializable {

  private static final long serialVersionUID = -2905676240064559127L;

  private final Design design;
  private final Map<String, String> map = Maps.newHashMap();

  private static final String STRING_TYPE = "string";
  private static final String SAMPLE_NAME_TYPE = "samplename";
  private static final String SAMPLE_METADATA_TYPE = "samplemetadata";
  private static final char SEPARATOR = ':';

  @Override
  public String get(final String key) {

    checkKey(key);

    final String value = this.map.get(key);

    if (value == null) {
      return null;
    }

    final String type = value.substring(0, value.indexOf(SEPARATOR));

    switch (type) {

    case STRING_TYPE:
      return value.substring(type.length() + 1);

    case SAMPLE_NAME_TYPE:

      int sampleId1 = Integer.parseInt(value.substring(type.length() + 1));
      Sample sample1 = null;
      for (Sample s : this.design.getSamples()) {
        if (s.getId() == sampleId1) {
          sample1 = s;
        }
      }

      return sample1 == null ? null : sample1.getName();

    case SAMPLE_METADATA_TYPE:

      int endSampleId = value.indexOf(SEPARATOR, type.length() + 1);

      int sampleId2 =
          Integer.parseInt(value.substring(type.length() + 1, endSampleId));

      String field = value.substring(endSampleId + 1);

      Sample sample2 = null;
      for (Sample s : this.design.getSamples()) {
        if (s.getId() == sampleId2) {
          sample2 = s;
        }
      }

      return sample2.getMetadata().getField(field);

    default:
      throw new IllegalStateException("Unknown metadata type: " + type);
    }

  }

  @Override
  public void set(final String key, final String value) {

    checkKey(key);
    checkNotNull(value, "value argument cannot be null");

    this.map.put(key, STRING_TYPE + SEPARATOR + value);
  }

  void setSampleName(final Sample sample) {

    checkNotNull(sample, "sample argument cannot be null");

    final String value = SAMPLE_NAME_TYPE + SEPARATOR + sample.getId();

    this.map.put(SAMPLE_NAME_KEY, value);
  }

  void setSampleField(final Sample sample, final String sampleField) {

    checkKey(sampleField);
    checkNotNull(sample, "sample argument cannot be null");
    checkNotNull(sample, "sampleField argument cannot be null");
    checkArgument(sample.getMetadata().isField(sampleField));

    final String value =
        SAMPLE_METADATA_TYPE
            + SEPARATOR + sample.getId() + SEPARATOR + sampleField;

    this.map.put(sampleField, value);
  }

  @Override
  public boolean containsKey(final String key) {

    checkKey(key);

    return this.map.containsKey(key);
  }

  @Override
  public boolean removeKey(final String key) {

    checkKey(key);

    return this.removeKey(key);
  }

  @Override
  public void set(final DataMetadata metadata) {

    checkNotNull(metadata, "metadata argument cannot be null");

    final SimpleDataMetaData md = DataUtils.getSimpleMetadata(metadata);

    if (md != null) {

      // If metadata object is a SimpleDataMetaData do raw copy
      for (Map.Entry<String, String> e : md.map.entrySet()) {
        this.map.put(e.getKey(), e.getValue());
      }

    } else {

      // If not, do a standard copy
      for (String key : metadata.keySet()) {
        set(key, metadata.get(key));
      }
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

  private void checkKey(final String key) {

    checkNotNull(key, "key argument cannot be null");

    for (int i = 0; i < key.length(); i++) {

      if (key.charAt(i) < ' ') {
        throw new InvalidParameterException(
            "Invalid metadata key character found: " + key.charAt(i));
      }
    }

  }

  //
  // Constructor
  //

  /**
   * Constructor.
   */
  SimpleDataMetaData(final Design design) {

    this.design = design;
  }

}
