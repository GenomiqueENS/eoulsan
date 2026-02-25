package fr.ens.biologie.genomique.eoulsan.design;

import java.io.Serializable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

public class UnmodifiableExperimentMetadata implements ExperimentMetadata, Serializable {

  private static final long serialVersionUID = -426306838693570333L;

  private final ExperimentMetadata md;

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
  public boolean isSkip() {
    return this.md.isSkip();
  }

  @Override
  public String getReference() {
    return this.md.getReference();
  }

  @Override
  public String getModel() {
    return this.md.getModel();
  }

  @Override
  public boolean isContrast() {
    return this.md.isContrast();
  }

  @Override
  public boolean isBuildContrast() {
    return this.md.isBuildContrast();
  }

  @Override
  public String getDesignFile() {
    return this.md.getDesignFile();
  }

  @Override
  public String getComparisons() {
    return this.md.getComparisons();
  }

  @Override
  public String getContrastFile() {
    return this.md.getContrastFile();
  }

  @Override
  public void setSkip(boolean newSkip) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setReference(String newReference) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setModel(String newModel) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setContrast(boolean newContrast) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setBuildContrast(boolean newBuildContrast) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setDesignFile(String newDesignFile) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setComparisons(String newComparisons) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setContrastFile(String newContrastFile) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsSkip() {
    return this.md.containsSkip();
  }

  @Override
  public boolean containsReference() {
    return this.md.containsReference();
  }

  @Override
  public boolean containsModel() {
    return this.md.containsModel();
  }

  @Override
  public boolean containsContrast() {
    return this.md.containsContrast();
  }

  @Override
  public boolean containsBuildContrast() {
    return this.md.containsBuildContrast();
  }

  @Override
  public boolean containsDesignFile() {
    return this.md.containsDesignFile();
  }

  @Override
  public boolean containsComparisons() {
    return this.md.containsComparisons();
  }

  @Override
  public boolean containsContrastFile() {
    return this.md.containsContrastFile();
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

  UnmodifiableExperimentMetadata(final ExperimentMetadata md) {
    this.md = md;
  }
}
