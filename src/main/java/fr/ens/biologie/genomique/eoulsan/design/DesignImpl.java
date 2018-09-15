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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import fr.ens.biologie.genomique.eoulsan.core.FileNaming;

/**
 * This class defines the default implementation of the design.
 * @author Xavier Bauquet
 * @since 2.0
 */
class DesignImpl implements Serializable, Design {

  /** Serialization version UID. */
  private static final long serialVersionUID = 7250832351983922161L;

  private static AtomicInteger instanceCount = new AtomicInteger(0);

  private final int designNumber = instanceCount.incrementAndGet();
  private String designName = "Design" + designNumber;
  private final Map<String, Sample> samples = new LinkedHashMap<>();
  private final Map<String, Experiment> experiments = new LinkedHashMap<>();
  private final DesignMetadata designMetadata = new DesignMetadataImpl();

  //
  // Getters
  //

  @Override
  public void setName(String newDesignName) {

    requireNonNull(newDesignName, "newDesignName argument cannot be null");

    this.designName = newDesignName.trim();
  }

  @Override
  public Sample getSample(final String sampleId) {

    requireNonNull(sampleId, "sampleId argument cannot be null");

    final String id = sampleId.trim();

    checkArgument(this.samples.containsKey(id),
        "The sample does not exists in the design: " + id);

    return samples.get(id);
  }

  @Override
  public List<Sample> getSamples() {

    return Collections.unmodifiableList(new ArrayList<>(this.samples.values()));
  }

  @Override
  public Experiment getExperiment(String experimentId) {

    requireNonNull(experimentId, "experimentId argument cannot be null");

    final String id = experimentId.trim();

    checkArgument(this.experiments.containsKey(id),
        "The experiment does not exists in the design: " + id);

    return experiments.get(id);
  }

  @Override
  public List<Experiment> getExperiments() {

    return Collections
        .unmodifiableList(new ArrayList<>(this.experiments.values()));
  }

  @Override
  public DesignMetadata getMetadata() {

    return this.designMetadata;
  }

  @Override
  public int getNumber() {

    return this.designNumber;
  }

  @Override
  public String getName() {

    return this.designName;
  }

  //
  // Remove
  //

  @Override
  public void removeSample(String sampleId) {

    requireNonNull(sampleId, "sampleId argument cannot be null");
    checkArgument(this.samples.containsKey(sampleId),
        "The sample does not exists in the design: " + sampleId);

    this.samples.remove(sampleId.trim());
  }

  @Override
  public void removeExperiment(String experimentId) {

    requireNonNull(experimentId, "experimentId argument cannot be null");
    checkArgument(this.experiments.containsKey(experimentId.trim()),
        "The experiment does not exists in the design: " + experimentId);

    this.experiments.remove(experimentId.trim());
  }

  //
  // Contains
  //

  @Override
  public boolean containsSample(String sampleId) {

    return this.samples.containsKey(sampleId.trim());
  }

  @Override
  public boolean containsExperiment(String experimentId) {

    return this.experiments.containsKey(experimentId.trim());
  }

  @Override
  public boolean containsSampleName(final String sampleName) {

    requireNonNull(sampleName, "sampleName argument cannot be null");

    final String name = sampleName.trim();

    for (Sample sample : this.samples.values()) {

      if (name.equals(sample.getName())) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean containsExperimentName(final String experimentName) {

    requireNonNull(experimentName, "sampleName argument cannot be null");

    final String name = experimentName.trim();

    for (Experiment experiment : this.experiments.values()) {

      if (name.equals(experiment.getName())) {
        return true;
      }
    }

    return false;
  }

  //
  // Add
  //

  @Override
  public SampleImpl addSample(String sampleId) {

    requireNonNull(sampleId, "sampleId argument cannot be null");

    final String id = sampleId.trim();

    checkArgument(!this.samples.containsKey(id),
        "The sample already exists in the design: " + id);
    checkArgument(FileNaming.isDataNameValid(sampleId),
        "The id of a sample can only contains letters and digit: " + sampleId);

    final SampleImpl newSample = new SampleImpl(this, id);

    this.samples.put(id, newSample);

    return newSample;
  }

  @Override
  public Experiment addExperiment(String experimentId) {

    requireNonNull(experimentId, "experimentId argument cannot be null");

    final String id = experimentId.trim();

    checkArgument(!this.experiments.containsKey(id),
        "The experiment already exists in the design: " + id);
    checkArgument(FileNaming.isDataNameValid(experimentId),
        "The id of an experiment can only contains letters and digit: "
            + experimentId);

    final ExperimentImpl newExperiment = new ExperimentImpl(this, id);

    this.experiments.put(id, newExperiment);

    return newExperiment;
  }

  //
  // Other methods
  //

  @Override
  public List<Experiment> getExperimentsUsingASample(final Sample sample) {

    requireNonNull(sample, "sample argument cannot be null");

    final List<Experiment> result = new ArrayList<>();

    for (Experiment e : getExperiments()) {
      if (e.containsSample(sample)) {
        result.add(e);
      }
    }

    return Collections.unmodifiableList(result);
  }

  //
  // Objects methods
  //

  @Override
  public String toString() {

    return com.google.common.base.Objects.toStringHelper(this)
        .add("designNumber", this.designNumber)
        .add("designName", this.designName).add("samples", this.samples)
        .add("experiments", this.experiments)
        .add("designMetadata", this.designMetadata).toString();
  }

  @Override
  public int hashCode() {

    return Objects.hash(this.designNumber, this.designName, this.samples,
        this.experiments, this.designMetadata);
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof DesignImpl)) {
      return false;
    }

    final DesignImpl that = (DesignImpl) o;

    return Objects.equals(this.designName, that.designName)
        && Objects.equals(this.samples, that.samples)
        && Objects.equals(this.experiments, that.experiments)
        && Objects.equals(this.designMetadata, that.designMetadata);
  }

  //
  // Constructor
  //

  /**
   * Default constructor
   */
  DesignImpl() {
  }

}
