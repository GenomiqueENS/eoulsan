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

package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.eoulsan.data.DataMetadata;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class define a simple class for metadata of data objects.
 *
 * @since 2.0
 * @author Laurent Jourdren
 */
class SimpleDataMetadata extends AbstractDataMetadata {

  private static final long serialVersionUID = -2905676240064559127L;

  private final Design design;
  private final Map<String, String> map = new HashMap<>();

  private static final String STRING_TYPE = "value";
  private static final String SAMPLE_NUMBER_TYPE = "design_sample_id";
  private static final String SAMPLE_NAME_TYPE = "design_sample_name";
  private static final String DESIGN_METADATA_TYPE = "design_metadata";
  private static final String SAMPLE_METADATA_TYPE = "design_sample_metadata";
  private static final String EXPERIMENT_METADATA_TYPE = "design_experiment_sample_metadata";
  private static final char SEPARATOR = ':';

  /**
   * Get the raw value of a metadata entry.
   *
   * @param key the key
   * @return the raw entry if exists or null
   */
  String getRaw(final String key) {

    checkKey(key);

    return this.map.get(key);
  }

  @Override
  public String get(final String key) {

    final String value = getRaw(key);

    if (value == null) {
      return null;
    }

    final String type = value.substring(0, value.indexOf(SEPARATOR));

    switch (type) {
      case STRING_TYPE:
        return value.substring(type.length() + 1);

      case SAMPLE_NAME_TYPE:
        final String sampleId1 = value.substring(type.length() + 1);

        try {
          return this.design.getSample(sampleId1).getName();
        } catch (IllegalArgumentException e) {
          return null;
        }

      case SAMPLE_METADATA_TYPE:
        final int endSampleId = value.indexOf(SEPARATOR, type.length() + 1);

        final String sampleId2 = value.substring(type.length() + 1, endSampleId);
        final String sampleMDKey = value.substring(endSampleId + 1);

        try {
          return this.design.getSample(sampleId2).getMetadata().get(sampleMDKey);
        } catch (IllegalArgumentException e) {
          return null;
        }

      case DESIGN_METADATA_TYPE:
        final String designMDKey = value.substring(type.length() + 1);

        try {
          return this.design.getMetadata().get(designMDKey);
        } catch (IllegalArgumentException e) {
          return null;
        }

      default:
        throw new IllegalStateException("Unknown metadata type: " + type);
    }
  }

  /**
   * Set the raw entry of a metadata.
   *
   * @param key the key
   * @param value the raw value
   */
  void setRaw(final String key, final String value) {

    checkKey(key);
    requireNonNull(value, "value argument cannot be null");

    this.map.put(key, value);
  }

  @Override
  public void set(final String key, final String value) {

    requireNonNull(value, "value argument cannot be null");

    setRaw(key, STRING_TYPE + SEPARATOR + value);
  }

  void setSampleName(final Sample sample) {

    requireNonNull(sample, "sample argument cannot be null");

    final String value = SAMPLE_NAME_TYPE + SEPARATOR + sample.getId();

    setRaw(SAMPLE_NAME_KEY, value);
  }

  void setSampleNumber(final Sample sample) {

    requireNonNull(sample, "sample argument cannot be null");

    final String value = SAMPLE_NUMBER_TYPE + SEPARATOR + sample.getId();

    setRaw(SAMPLE_NUMBER_KEY, value);
  }

  void setSampleMetadata(final Sample sample, final String key) {

    requireNonNull(sample, "sample argument cannot be null");
    requireNonNull(key, "key argument cannot be null");
    checkArgument(sample.getMetadata().contains(key));

    final String value = SAMPLE_METADATA_TYPE + SEPARATOR + sample.getId() + SEPARATOR + key;

    setRaw(key, value);
  }

  void setDesignMetadata(final Design design, final String key) {

    requireNonNull(design, "sample argument cannot be null");
    requireNonNull(key, "key argument cannot be null");
    checkArgument(design.getMetadata().contains(key));

    final String value = DESIGN_METADATA_TYPE + SEPARATOR + key;

    setRaw(key, value);
  }

  void setExperimentMetadata(final Experiment experiment, final String key) {

    requireNonNull(experiment, "sample argument cannot be null");
    requireNonNull(key, "key argument cannot be null");
    checkArgument(experiment.getMetadata().contains(key));

    final String value = EXPERIMENT_METADATA_TYPE + SEPARATOR + key;

    setRaw(key, value);
  }

  @Override
  public boolean containsKey(final String key) {

    checkKey(key);

    return this.map.containsKey(key);
  }

  @Override
  public boolean removeKey(final String key) {

    checkKey(key);

    this.map.remove(key);

    return true;
  }

  @Override
  public void set(final DataMetadata metadata) {

    requireNonNull(metadata, "metadata argument cannot be null");

    final SimpleDataMetadata md = WorkflowDataUtils.getSimpleMetadata(metadata);

    if (md != null) {

      // If metadata object is a SimpleDataMetaData do raw copy
      for (Map.Entry<String, String> e : md.map.entrySet()) {
        setRaw(e.getKey(), e.getValue());
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

    requireNonNull(key, "key argument cannot be null");

    for (int i = 0; i < key.length(); i++) {

      if (key.charAt(i) < ' ') {
        throw new IllegalArgumentException(
            "Invalid metadata key character found: " + key.charAt(i));
      }
    }
  }

  //
  // Constructor
  //

  /** Constructor. */
  SimpleDataMetadata(final Design design) {

    this.design = design;
  }

  /** Copy constructor. */
  SimpleDataMetadata(final SimpleDataMetadata metadata) {

    requireNonNull(metadata, "data argument cannot be null");

    this.design = metadata.design;
    this.map.putAll(metadata.map);
  }
}
