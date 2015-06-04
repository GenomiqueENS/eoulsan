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

package fr.ens.transcriptome.eoulsan.design2;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.python.google.common.base.Objects;

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
  private final List<ExperimentSample> samples =
      new ArrayList<ExperimentSample>();

  //
  // Getters
  //

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
   * Get experiment samples list.
   * @return a list of ExperimentSample object
   */
  public List<ExperimentSample> getSamples() {

    return Collections.unmodifiableList(samples);
  }

  //
  // Setter
  //

  /**
   * Set the name of the experiment.
   * @param newExperimentName the new experiment name
   * @return the experiment name
   */
  public void setName(String newExperimentName) {

    checkNotNull(newExperimentName, "newExperimentName argument cannot be null");

    final String name = newExperimentName.trim();

    checkArgument(!this.design.containsExperimentName(name),
        "The sample name already exists in the design: " + name);

    this.experimentName = name;
  }

  //
  // Add
  //

  /**
   * Add a sample.
   * @param sampleId the sample id
   * @return the sample object
   */
  public ExperimentSample addSample(final Sample sample) {

    checkNotNull(sample, "sample argument cannot be null");
    checkArgument(!this.samples.contains(sample),
        "The sample already exists in the experiment: " + sample.getId());
    checkArgument(sample.getDesign() == this.design,
        "The sample to add to the experiment is not a sample of the design: "
            + sample.getId());

    final ExperimentSample newExperimentSample = new ExperimentSample(sample);

    this.samples.add(newExperimentSample);

    return newExperimentSample;
  }

  //
  // Remove
  //

  /**
   * Remove the sample.
   * @param sampleId the sample id
   */
  public void removeSample(final Sample sample) {

    checkNotNull(sample, "sample argument cannot be null");
    checkArgument(this.samples.contains(sample),
        "The sample does not exists in the experiment: " + sample.getId());
    checkArgument(sample.getDesign() == this.design,
        "The sample to remove to the experiment is not a sample of the design: "
            + sample.getId());

    this.samples.remove(sample);
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
        && Objects.equal(this.experimentNumber, that.experimentNumber)
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

    this.design = design;
    this.experimentId = experimentId.trim();
  }

}
