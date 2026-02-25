package fr.ens.biologie.genomique.eoulsan.design;

import java.io.Serializable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

public class UnmodifiableExperimentSampleMetadata
    implements ExperimentSampleMetadata, Serializable {

  private static final long serialVersionUID = 2441285392336371023L;

  private final ExperimentSampleMetadata md;

  @Override
  public String get(String key) {
    return this.md.get(key);
  }

  @Override
  public String getTrimmed(String key) {
    return this.md.getTrimmed(key);
  }

  @Override
  public void set(String key, String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void set(String key, List<String> value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int size() {
    return this.md.size();
  }

  @Override
  public boolean isEmpty() {
    return this.md.isEmpty();
  }

  @Override
  public boolean contains(String key) {
    return this.md.contains(key);
  }

  @Override
  public List<String> getAsList(String key) {
    return this.md.getAsList(key);
  }

  @Override
  public boolean getAsBoolean(String key) {
    return this.md.getAsBoolean(key);
  }

  @Override
  public Set<String> keySet() {
    return this.md.keySet();
  }

  @Override
  public Set<Entry<String, String>> entrySet() {
    return this.md.entrySet();
  }

  @Override
  public void remove(String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getRepTechGroup() {
    return this.md.getRepTechGroup();
  }

  @Override
  public String getReference() {
    return this.md.getReference();
  }

  @Override
  public boolean isReference() {
    return this.md.isReference();
  }

  @Override
  public String getCondition() {
    return this.md.getCondition();
  }

  @Override
  public void setRepTechGroup(String newReptechGroup) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setReference(boolean newReference) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setReference(String newReference) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setCondition(String newCondition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsRepTechGroup() {
    return this.md.containsRepTechGroup();
  }

  @Override
  public boolean containsReference() {
    return this.md.containsReference();
  }

  @Override
  public boolean containsCondition() {
    return this.md.containsCondition();
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.md);
  }

  @Override
  public boolean equals(Object obj) {
    return this.md.equals(obj);
  }

  //
  // Constructor
  //

  public UnmodifiableExperimentSampleMetadata(final ExperimentSampleMetadata md) {

    this.md = md;
  }
}
