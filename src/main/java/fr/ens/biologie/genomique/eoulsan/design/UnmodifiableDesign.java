package fr.ens.biologie.genomique.eoulsan.design;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

class UnmodifiableDesign implements Design, Serializable {

  private static final long serialVersionUID = -5904542219204905245L;

  private final Design design;

  @Override
  public void setName(String newDesignName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Sample getSample(String sampleId) {

    Sample sample = this.design.getSample(sampleId);
    return sample == null ? null : new UnmodifiableSample(sample);
  }

  @Override
  public List<Sample> getSamples() {

    List<Sample> samples = this.design.getSamples();
    List<Sample> result = new ArrayList<>(samples.size());

    for (Sample sample : samples) {
      result.add(new UnmodifiableSample(sample));
    }

    return Collections.unmodifiableList(result);
  }

  @Override
  public Experiment getExperiment(String experimentId) {

    Experiment experiment = this.design.getExperiment(experimentId);
    return experiment == null ? null : new UnmodifiableExperiment(experiment);
  }

  @Override
  public List<Experiment> getExperiments() {

    List<Experiment> experiments = this.design.getExperiments();
    List<Experiment> result = new ArrayList<>(experiments.size());

    for (Experiment experiment : experiments) {
      result.add(new UnmodifiableExperiment(experiment));
    }

    return Collections.unmodifiableList(result);
  }

  @Override
  public DesignMetadata getMetadata() {
    return new UnmodifiableDesignMetadata(this.design.getMetadata());
  }

  @Override
  public int getNumber() {
    return this.design.getNumber();
  }

  @Override
  public String getName() {
    return this.design.getName();
  }

  @Override
  public void removeSample(String sampleId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeExperiment(String experimentId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsSample(String sampleId) {
    return this.design.containsSample(sampleId);
  }

  @Override
  public boolean containsExperiment(String experimentId) {
    return this.design.containsExperiment(experimentId);
  }

  @Override
  public boolean containsSampleName(String sampleName) {
    return this.design.containsSampleName(sampleName);
  }

  @Override
  public boolean containsExperimentName(String experimentName) {
    return this.design.containsExperimentName(experimentName);
  }

  @Override
  public SampleImpl addSample(String sampleId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Experiment addExperiment(String experimentId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Experiment> getExperimentsUsingASample(Sample sampleId) {

    List<Experiment> experiments = this.design.getExperimentsUsingASample(sampleId);
    List<Experiment> result = new ArrayList<>(experiments.size());

    for (Experiment experiment : experiments) {
      result.add(new UnmodifiableExperiment(experiment));
    }

    return Collections.unmodifiableList(result);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.design);
  }

  @Override
  public boolean equals(Object obj) {
    return this.design.equals(obj);
  }

  //
  // Constructor
  //

  UnmodifiableDesign(final Design design) {

    this.design = design;
  }
}
