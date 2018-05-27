package fr.ens.biologie.genomique.eoulsan.design;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UnmodifiableExperiment implements Experiment, Serializable {

  private static final long serialVersionUID = 2564230655661740901L;

  private final Experiment experiment;

  @Override
  public Design getDesign() {
    return this.experiment.getDesign();
  }

  @Override
  public String getId() {
    return this.experiment.getId();
  }

  @Override
  public String getName() {
    return this.experiment.getName();
  }

  @Override
  public int getNumber() {
    return this.experiment.getNumber();
  }

  @Override
  public ExperimentMetadata getMetadata() {
    return new UnmodifiableExperimentMetadata(this.experiment.getMetadata());
  }

  @Override
  public List<Sample> getSamples() {

    List<Sample> samples = this.experiment.getSamples();

    List<Sample> result = new ArrayList<>(samples.size());

    for (Sample sample : samples) {
      result.add(new UnmodifiableSample(sample));
    }

    return Collections.unmodifiableList(result);
  }

  @Override
  public List<ExperimentSample> getExperimentSamples() {

    List<ExperimentSample> samples = this.experiment.getExperimentSamples();

    List<ExperimentSample> result = new ArrayList<>(samples.size());

    for (ExperimentSample sample : samples) {
      result.add(new UnmodifiableExperimentSample(sample));
    }

    return Collections.unmodifiableList(result);
  }

  @Override
  public ExperimentSample getExperimentSample(Sample sample) {

    ExperimentSample es = this.experiment.getExperimentSample(sample);

    return es == null ? null : new UnmodifiableExperimentSample(es);
  }

  @Override
  public void setName(String newExperimentName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ExperimentSample addSample(Sample sample) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeSample(Sample sample) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsSample(Sample sample) {
    return this.experiment.containsSample(sample);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.experiment);
  }

  @Override
  public boolean equals(Object obj) {
    return this.experiment.equals(obj);
  }

  //
  // Constructor
  //

  public UnmodifiableExperiment(final Experiment experiment) {

    this.experiment = experiment;
  }

}
