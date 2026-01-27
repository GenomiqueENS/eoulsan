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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.MoreObjects;

import fr.ens.biologie.genomique.eoulsan.core.FileNaming;

/**
 * This class defines the default implementation of an experiment.
 * @author Xavier Bauquet
 * @since 2.0
 */
public class ExperimentImpl implements Serializable, Experiment {

  /** Serialization version UID. */
  private static final long serialVersionUID = -2644491674956956116L;

  private static AtomicInteger instanceCount = new AtomicInteger(0);

  private final Design design;
  private final String experimentId;
  private final int experimentNumber = instanceCount.incrementAndGet();
  private String experimentName = "Experiment" + experimentNumber;
  private final ExperimentMetadataImpl metadata = new ExperimentMetadataImpl();
  private final List<ExperimentSample> samples = new ArrayList<>();
  private final Set<String> sampleNames = new HashSet<>();

  //
  // Getters
  //

  @Override
  public Design getDesign() {

    return this.design;
  }

  @Override
  public String getId() {

    return experimentId;
  }

  @Override
  public String getName() {

    return experimentName;
  }

  @Override
  public int getNumber() {

    return experimentNumber;
  }

  @Override
  public ExperimentMetadataImpl getMetadata() {

    return metadata;
  }

  @Override
  public List<Sample> getSamples() {

    final List<Sample> result = new ArrayList<>();

    for (ExperimentSample es : getExperimentSamples()) {
      result.add(es.getSample());
    }

    return Collections.unmodifiableList(result);
  }

  @Override
  public List<ExperimentSample> getExperimentSamples() {

    return Collections.unmodifiableList(samples);
  }

  @Override
  public ExperimentSample getExperimentSample(final Sample sample) {

    if (sample == null) {
      return null;
    }

    for (ExperimentSample eSample : this.samples) {

      if (eSample.getSample().getId().equals(sample.getId())) {
        return eSample;
      }
    }

    return null;
  }

  //
  // Setter
  //

  @Override
  public void setName(String newExperimentName) {

    requireNonNull(newExperimentName,
        "newExperimentName argument cannot be null");

    final String name = newExperimentName.trim();

    // Do nothing if the new name is the old name
    if (name.equals(this.experimentName)) {
      return;
    }

    checkArgument(!this.design.containsExperimentName(name),
        "The experiment name already exists in the design: " + name);

    this.experimentName = name;
  }

  //
  // Add
  //

  @Override
  public ExperimentSample addSample(final Sample sample) {

    requireNonNull(sample, "sample argument cannot be null");
    checkArgument(!this.sampleNames.contains(sample.getId()),
        "The sample already exists in the experiment: " + sample.getId());
    checkArgument(sample.getDesign() == this.design,
        "The sample to add to the experiment is not a sample of the design: "
            + sample.getId());

    final ExperimentSampleImpl newExperimentSample =
        new ExperimentSampleImpl(sample);

    this.samples.add(newExperimentSample);
    this.sampleNames.add(sample.getId());

    return newExperimentSample;
  }

  //
  // Remove
  //

  @Override
  public void removeSample(final Sample sample) {

    requireNonNull(sample, "sample argument cannot be null");
    checkArgument(this.sampleNames.contains(sample.getId()),
        "The sample does not exists in the experiment: " + sample.getId());
    checkArgument(sample.getDesign() == this.design,
        "The sample to remove to the experiment is not a sample of the design: "
            + sample.getId());

    this.samples.remove(getExperimentSample(sample));
    this.sampleNames.remove(sample.getId());
  }

  //
  // Contains
  //

  @Override
  public boolean containsSample(final Sample sample) {

    requireNonNull(sample, "sample argument cannot be null");

    return this.sampleNames.contains(sample.getId());
  }

  //
  // Objects methods
  //

  @Override
  public String toString() {

    return MoreObjects.toStringHelper(this)
        .add("experimentId", this.experimentId)
        .add("experimentNumber", this.experimentNumber)
        .add("experimentName", this.experimentName)
        .add("experimentMetadata", this.metadata)
        .add("experimentSamples", this.samples).toString();
  }

  @Override
  public int hashCode() {

    return Objects.hash(this.experimentId, this.experimentName, this.metadata,
        this.samples);
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof ExperimentImpl)) {
      return false;
    }

    final ExperimentImpl that = (ExperimentImpl) o;

    return Objects.equals(this.experimentId, that.experimentId)
        && Objects.equals(this.experimentName, that.experimentName)
        && Objects.equals(this.metadata, that.metadata)
        && Objects.equals(this.samples, that.samples);
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param design the design object
   * @param experimentId the experiment id
   */
  ExperimentImpl(Design design, String experimentId) {

    requireNonNull(design, "design argument cannot be null");
    requireNonNull(experimentId, "sampleId argument cannot be null");
    checkArgument(FileNaming.isDataNameValid(experimentId),
        "The id of an experiment can only contains letters and digit: "
            + experimentId);

    this.design = design;
    this.experimentId = experimentId.trim();
  }

}
