package fr.ens.biologie.genomique.eoulsan.design;

import java.io.Serializable;
import java.util.Objects;

class UnmodifiableSample implements Sample, Serializable {

  private static final long serialVersionUID = 6336117473427204972L;

  private final Sample sample;

  @Override
  public Design getDesign() {
    return new UnmodifiableDesign(this.sample.getDesign());
  }

  @Override
  public String getId() {
    return this.sample.getId();
  }

  @Override
  public int getNumber() {
    return this.sample.getNumber();
  }

  @Override
  public String getName() {
    return this.sample.getName();
  }

  @Override
  public SampleMetadata getMetadata() {
    return new UnmodifiableSampleMetadata(this.sample.getMetadata());
  }

  @Override
  public void setName(String newSampleName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.sample);
  }

  @Override
  public boolean equals(Object obj) {
    return this.sample.equals(obj);
  }

  //
  // Constructor
  //

  UnmodifiableSample(final Sample sample) {

    this.sample = sample;
  }
}
