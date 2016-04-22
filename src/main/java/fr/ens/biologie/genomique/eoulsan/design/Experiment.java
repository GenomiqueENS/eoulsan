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
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.*;

import com.google.common.base.Objects;

import fr.ens.biologie.genomique.eoulsan.core.FileNaming;

/**
 * This class defines an experiment.
 * @author Xavier Bauquet
 * @since 2.0
 */

public class Experiment implements Serializable {

  /** Serialization version UID. */
  private static final long serialVersionUID = -2644491674956956116L;

  private static int instanceCount;

  private final Design design;
  private final String experimentId;
  private final int experimentNumber = ++instanceCount;
  private String experimentName = "Experiment" + experimentNumber;
  private final ExperimentMetadata metadata = new ExperimentMetadata();
  private final List<ExperimentSample> samples = new ArrayList<>();
  private final Set<String> sampleNames = new HashSet<>();

  //
  // Getters
  //

  /**
   * Get the design related to the experiment.
   * @return the Design object related to the experiment
   */
  public Design getDesign() {

    return this.design;
  }

  /**
   * get the experiment id.
   * @return the experiment id
   */
  public String getId() {

    return experimentId;
  }

  /**
   * Get the experiment name.
   * @return the experiment name
   */
  public String getName() {

    return experimentName;
  }

  /**
   * Get the experiment number.
   * @return the experiment number
   */
  public int getNumber() {

    return experimentNumber;
  }

  /**
   * Get the experiment metadata.
   * @return the experiment metadata
   */
  public ExperimentMetadata getMetadata() {

    return metadata;
  }

  /**
   * Get the samples of the experiment.
   * @return a list of ExperimentSample object
   */
  public List<Sample> getSamples() {

    final List<Sample> result = new ArrayList<>();

    for (ExperimentSample es : getExperimentSamples()) {
      result.add(es.getSample());
    }

    return Collections.unmodifiableList(result);
  }

  /**
   * Get experiment samples list.
   * @return a list of ExperimentSample object
   */
  public List<ExperimentSample> getExperimentSamples() {

    return Collections.unmodifiableList(samples);
  }

  /**
   * Get the experiment sample related to the sample.
   * @param sample the sample
   * @return an experiment sample object if exists or null
   */
  public ExperimentSample getExperimentSample(final Sample sample) {

    for (ExperimentSample eSample : this.samples) {

      if (eSample.getSample() == sample) {
        return eSample;
      }
    }

    return null;
  }

  //
  // Setter
  //

  /**
   * Set the name of the experiment.
   * @param newExperimentName the new experiment name
   */
  public void setName(String newExperimentName) {

    checkNotNull(newExperimentName,
        "newExperimentName argument cannot be null");

    final String name = newExperimentName.trim();

    // Do nothing if the new name is the old name
    if (name.equals(this.experimentName)) {
      return;
    }

    checkArgument(!this.design.containsExperimentName(name),
        "The sample name already exists in the design: " + name);

    this.experimentName = name;
  }

  //
  // Add
  //

  /**
   * Add a sample.
   * @param sample the sample to add
   * @return an experiment sample object
   */
  public ExperimentSample addSample(final Sample sample) {

    checkNotNull(sample, "sample argument cannot be null");
    checkArgument(!this.sampleNames.contains(sample.getId()),
        "The sample already exists in the experiment: " + sample.getId());
    checkArgument(sample.getDesign() == this.design,
        "The sample to add to the experiment is not a sample of the design: "
            + sample.getId());

    final ExperimentSample newExperimentSample = new ExperimentSample(sample);

    this.samples.add(newExperimentSample);
    this.sampleNames.add(sample.getId());

    return newExperimentSample;
  }

  //
  // Remove
  //

  /**
   * Remove the sample.
   * @param sample the sample to remove
   */
  public void removeSample(final Sample sample) {

    checkNotNull(sample, "sample argument cannot be null");
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

  /**
   * Test if the experiment contains a sample.
   * @param sample the sample to test
   * @return true if the sample is the experiment
   */
  public boolean containsSample(final Sample sample) {

    checkNotNull(sample, "sample argument cannot be null");

    return this.sampleNames.contains(sample.getId());
  }

  //
  // Objects methods
  //

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("experimentId", this.experimentId)
        .add("experimentNumber", this.experimentNumber)
        .add("experimentName", this.experimentName)
        .add("experimentMetadata", this.metadata)
        .add("experimentSamples", this.samples).toString();
  }

  @Override
  public int hashCode() {

    return Objects.hashCode(this.experimentId, this.experimentNumber,
        this.experimentName, this.metadata, this.samples);
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof Experiment)) {
      return false;
    }

    final Experiment that = (Experiment) o;

    return Objects.equal(this.experimentId, that.experimentId)
        && Objects.equal(this.experimentName, that.experimentName)
        && Objects.equal(this.metadata, that.metadata)
        && Objects.equal(this.samples, that.samples);
  }

  //
  // Constructor
  //
  /**
   * @param design the design object
   * @param experimentId the experiment id
   */
  Experiment(Design design, String experimentId) {

    checkNotNull(design, "design argument cannot be null");
    checkNotNull(experimentId, "sampleId argument cannot be null");
    checkArgument(FileNaming.isDataNameValid(experimentId),
        "The id of an experiment can only contains letters and digit: "
            + experimentId);

    this.design = design;
    this.experimentId = experimentId.trim();
  }

}
