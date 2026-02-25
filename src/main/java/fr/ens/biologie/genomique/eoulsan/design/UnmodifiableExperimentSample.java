package fr.ens.biologie.genomique.eoulsan.design;

import java.io.Serializable;
import java.util.Objects;

class UnmodifiableExperimentSample implements ExperimentSample, Serializable {

  private static final long serialVersionUID = -8223606436283749785L;

  private final ExperimentSample experimentSample;

  @Override
  public Sample getSample() {
    return new UnmodifiableSample(this.experimentSample.getSample());
  }

  @Override
  public ExperimentSampleMetadata getMetadata() {
    return new UnmodifiableExperimentSampleMetadata(this.experimentSample.getMetadata());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.experimentSample);
  }

  @Override
  public boolean equals(Object obj) {
    return this.experimentSample.equals(obj);
  }

  //
  // Constructor
  //

  UnmodifiableExperimentSample(final ExperimentSample experimentSample) {

    this.experimentSample = experimentSample;
  }
}
