package fr.ens.biologie.genomique.eoulsan.design;

import fr.ens.biologie.genomique.kenetre.bio.FastqFormat;
import java.io.Serializable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

class UnmodifiableSampleMetadata implements SampleMetadata, Serializable {

  private static final long serialVersionUID = -2476125486194895667L;

  private final SampleMetadata md;

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
  public List<String> getReads() {
    return this.md.getReads();
  }

  @Override
  public String getDescription() {
    return this.md.getDescription();
  }

  @Override
  public String getOperator() {
    return this.md.getOperator();
  }

  @Override
  public String getComment() {
    return this.md.getComment();
  }

  @Override
  public String getDate() {
    return this.md.getDate();
  }

  @Override
  public String getSerialNumber() {
    return this.md.getSerialNumber();
  }

  @Override
  public String getUUID() {
    return this.md.getUUID();
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
  public FastqFormat getFastqFormat() {
    return this.md.getFastqFormat();
  }

  @Override
  public String getCondition() {
    return this.md.getCondition();
  }

  @Override
  public void setReads(List<String> newReads) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setDescription(String newDescription) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setOperator(String newOperator) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setComment(String newComment) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setDate(String newDate) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setSerialNumber(String newSerialNumber) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setUUID(String newUUID) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setRepTechGroup(String newReptechGroup) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setReference(String newReference) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setFastqFormat(FastqFormat newfastqFormat) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setCondition(String newCondition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsReads() {
    return this.md.containsReads();
  }

  @Override
  public boolean containsDescription() {
    return this.md.containsDescription();
  }

  @Override
  public boolean containsOperator() {
    return this.md.containsOperator();
  }

  @Override
  public boolean containsComment() {
    return this.md.containsComment();
  }

  @Override
  public boolean containsDate() {
    return this.md.containsDate();
  }

  @Override
  public boolean containsSerialNumber() {
    return this.md.containsSerialNumber();
  }

  @Override
  public boolean containsUUID() {
    return this.md.containsUUID();
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
  public boolean containsFastqFormat() {
    return this.md.containsFastqFormat();
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

  UnmodifiableSampleMetadata(final SampleMetadata md) {
    this.md = md;
  }
}
