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

package fr.ens.transcriptome.eoulsan.design;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.python.google.common.base.Objects;

import fr.ens.transcriptome.eoulsan.core.workflow.FileNaming;

/**
 * This class defines the design.
 * @author Xavier Bauquet
 * @since 2.0
 */

public class Design implements Serializable {

  /** Serialization version UID. */
  private static final long serialVersionUID = 7250832351983922161L;

  private static int instanceCount;

  private final int designNumber = ++instanceCount;
  private String designName = "Design" + designNumber;
  private final Map<String, Sample> samples = new LinkedHashMap<>();
  private final Map<String, Experiment> experiments = new LinkedHashMap<>();
  private final DesignMetadata designMetadata = new DesignMetadata();

  //
  // Getters
  //

  /**
   * Set the design name.
   * @param newDesignName the new design name
   */
  public void setName(String newDesignName) {

    checkNotNull(newDesignName, "newDesignName argument cannot be null");

    this.designName = newDesignName.trim();
  }

  /**
   * Get the name of a sample.
   * @param sampleId the sample id
   * @return a sample object
   */
  public Sample getSample(final String sampleId) {

    checkNotNull(sampleId, "sampleId argument cannot be null");

    final String id = sampleId.trim();

    checkArgument(this.samples.containsKey(id),
        "The sample does not exists in the design: " + id);

    return samples.get(id);
  }

  /**
   * Get the list of the samples.
   * @return the list of the samples
   */
  public List<Sample> getSamples() {

    return Collections.unmodifiableList(new ArrayList<>(this.samples.values()));
  }

  /**
   * Get the name of an experiment.
   * @param experimentId the experiment id
   * @return an experiment object
   */
  public Experiment getExperiment(String experimentId) {

    checkNotNull(experimentId, "experimentId argument cannot be null");

    final String id = experimentId.trim();

    checkArgument(this.experiments.containsKey(id),
        "The experiment does not exists in the design: " + id);

    return experiments.get(id);
  }

  /**
   * Get the list of the experiments.
   * @return the list of the experiments
   */
  public List<Experiment> getExperiments() {

    return Collections.unmodifiableList(new ArrayList<>(this.experiments
        .values()));
  }

  /**
   * Get the design Metadata.
   * @return a designMetadata object
   */
  public DesignMetadata getMetadata() {

    return this.designMetadata;
  }

  /**
   * Get design number.
   * @return the design number
   */
  public int getNumber() {

    return this.designNumber;
  }

  /**
   * Get design name.
   * @return the design name
   */
  public String getName() {

    return this.designName;
  }

  //
  // Remove
  //

  /**
   * Remove the sample.
   * @param sampleId the sample id
   */
  public void removeSample(String sampleId) {

    checkNotNull(sampleId, "sampleId argument cannot be null");
    checkArgument(this.samples.containsKey(sampleId),
        "The sample does not exists in the design: " + sampleId);

    this.samples.remove(sampleId.trim());
  }

  /**
   * Remove the experiment.
   * @param experimentId the experiment id
   */
  public void removeExperiment(String experimentId) {

    checkNotNull(experimentId, "experimentId argument cannot be null");
    checkArgument(this.experiments.containsKey(experimentId.trim()),
        "The experiment does not exists in the design: " + experimentId);

    this.experiments.remove(experimentId.trim());
  }

  //
  // Contains
  //

  /**
   * Test if the sample exists.
   * @param sampleId the sample id
   * @return true if the sample exists
   */
  public boolean containsSample(String sampleId) {

    return this.samples.containsKey(sampleId.trim());
  }

  /**
   * Test if the experiment exists.
   * @param experimentId the experiment id
   * @return true if the experiment exists
   */
  public boolean containsExperiment(String experimentId) {

    return this.experiments.containsKey(experimentId.trim());
  }

  public boolean containsSampleName(final String sampleName) {

    checkNotNull(sampleName, "sampleName argument cannot be null");

    final String name = sampleName.trim();

    for (Sample sample : this.samples.values()) {

      if (name.equals(sample.getName())) {
        return true;
      }
    }

    return false;
  }

  public boolean containsExperimentName(final String experimentName) {

    checkNotNull(experimentName, "sampleName argument cannot be null");

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

  /**
   * Add a sample.
   * @param sampleId the sample id
   * @return the sample object
   */
  public Sample addSample(String sampleId) {

    checkNotNull(sampleId, "sampleId argument cannot be null");

    final String id = sampleId.trim();

    checkArgument(!this.samples.containsKey(id),
        "The sample already exists in the design: " + id);
    checkArgument(FileNaming.isDataNameValid(sampleId),
        "The id of a sample can only contains letters and digit: " + sampleId);

    final Sample newSample = new Sample(this, id);

    this.samples.put(id, newSample);

    return newSample;
  }

  /**
   * Add an experiment.
   * @param experimentId the experiment id
   * @return the experiment object
   */
  public Experiment addExperiment(String experimentId) {

    checkNotNull(experimentId, "experimentId argument cannot be null");

    final String id = experimentId.trim();

    checkArgument(!this.experiments.containsKey(id),
        "The experiment already exists in the design: " + id);
    checkArgument(FileNaming.isDataNameValid(experimentId),
        "The id of an experiment can only contains letters and digit: "
            + experimentId);

    final Experiment newExperiment = new Experiment(this, id);

    this.experiments.put(id, newExperiment);

    return newExperiment;
  }

  //
  // Other methods
  //

  /**
   * Get all the experiments related to a sample.
   * @param sample the sample
   * @return a list with the experiments that use the sample
   */
  public List<Experiment> getExperimentsUsingASample(final Sample sample) {

    checkNotNull(sample, "sample argument cannot be null");

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

    return Objects.toStringHelper(this).add("designNumber", this.designNumber)
        .add("designName", this.designName).add("samples", this.samples)
        .add("experiments", this.experiments)
        .add("designMetadata", this.designMetadata).toString();
  }

  @Override
  public int hashCode() {

    return Objects.hashCode(this.designNumber, this.designName, this.samples,
        this.experiments, this.designMetadata);
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof Design)) {
      return false;
    }

    final Design that = (Design) o;

    return Objects.equal(this.designNumber, that.designNumber)
        && Objects.equal(this.designName, that.designName)
        && Objects.equal(this.samples, that.samples)
        && Objects.equal(this.experiments, that.experiments)
        && Objects.equal(this.designMetadata, that.designMetadata);
  }

  //
  // Constructor
  //

  /**
   * Default constructor
   */
  Design() {
  }
}
