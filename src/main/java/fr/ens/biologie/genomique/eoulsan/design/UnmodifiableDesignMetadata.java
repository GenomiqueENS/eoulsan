package fr.ens.biologie.genomique.eoulsan.design;

import java.io.Serializable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

class UnmodifiableDesignMetadata implements DesignMetadata, Serializable {

  private static final long serialVersionUID = -6546047931121812677L;

  private final DesignMetadata md;

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

  //
  //
  //

  @Override
  public String getGenomeFile() {
    return this.md.getGenomeFile();
  }

  @Override
  public String getGffFile() {
    return this.md.getGffFile();
  }

  @Override
  public String getGtfFile() {
    return this.md.getGtfFile();
  }

  @Override
  public String getAdditionalAnnotationFile() {
    return this.md.getAdditionalAnnotationFile();
  }

  @Override
  public void setGenomeFile(String newGenomeFile) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setGffFile(String newGffFile) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setGtfFile(String newGtfFile) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setAdditionalAnnotationFile(String newAdditionalAnotationFile) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsGenomeFile() {
    return this.md.containsGenomeFile();
  }

  @Override
  public boolean containsGffFile() {
    return this.md.containsGffFile();
  }

  @Override
  public boolean containsGtfFile() {
    return this.md.containsGtfFile();
  }

  @Override
  public boolean containsAdditionalAnnotationFile() {
    return this.md.containsAdditionalAnnotationFile();
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

  UnmodifiableDesignMetadata(final DesignMetadata md) {
    this.md = md;
  }
}
