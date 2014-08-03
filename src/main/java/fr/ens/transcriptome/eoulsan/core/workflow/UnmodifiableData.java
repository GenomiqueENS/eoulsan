package fr.ens.transcriptome.eoulsan.core.workflow;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import fr.ens.transcriptome.eoulsan.core.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;

/**
 * This class define an unmodifiable data
 * @since 1.3
 * @author Laurent Jourdren
 */
public class UnmodifiableData implements Data {

  private final Data data;

  @Override
  public String getName() {
    return this.data.getName();
  }

  @Override
  public DataFormat getFormat() {
    return this.data.getFormat();
  }

  @Override
  public Map<String, String> getMetadata() {
    return Collections.unmodifiableMap(this.data.getMetadata());
  }

  @Override
  public boolean isList() {
    return this.data.isList();
  }

  @Override
  public List<Data> getListElements() {
    return this.data.getListElements();
  }

  @Override
  public Data addDataToList(final String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getDataFilename() {
    return this.data.getDataFilename();
  }

  @Override
  public String getDataFilename(final int fileIndex) {
    return this.data.getDataFilename(fileIndex);
  }

  @Override
  public DataFile getDataFile() {
    return this.data.getDataFile();
  }

  @Override
  public DataFile getDataFile(int fileIndex) {
    return this.data.getDataFile(fileIndex);
  }

  @Override
  public int getDataFileCount() {
    return this.data.getDataFileCount();
  }

  @Override
  public int getDataFileCount(boolean existingFiles) {
    return this.data.getDataFileCount(existingFiles);
  }

  /**
   * Get the AbstractData object wrapped by this object.
   * @return the AbstractData object wrapped by this object
   */
  AbstractData getData() {

    Data data = this;

    do {
      data = ((UnmodifiableData) data).data;
    } while (data instanceof UnmodifiableData);

    return (AbstractData) this.data;
  }

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("name", getName())
        .add("format", getFormat().getName()).add("metadata", getMetadata())
        .add("list", isList()).add("elements", getListElements()).toString();
  }

  //
  // Constructor
  //

  UnmodifiableData(final Data data) {

    Preconditions.checkNotNull(data, "data argument cannot be null");

    this.data = data;
  }

}
