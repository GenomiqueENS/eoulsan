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

package fr.ens.transcriptome.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;

/**
 * This class define a data element.
 * @since 2.0
 * @author Laurent Jourdren
 */
class DataElement extends AbstractData implements Serializable {

  private static final long serialVersionUID = -8982205120365590676L;

  private final Map<String, String> metadata = Maps.newHashMap();
  protected final List<DataFile> files;

  // Field required for multi-files Data creation
  private final WorkflowOutputPort port;

  private boolean canRename = true;

  @Override
  public void setName(final String name) {

    if (!this.canRename)
      throw new EoulsanRuntimeException(
          "Data cannot be renamed once it has been used");

    super.setName(name);

    // Update datafiles
    updateDataFiles();
  }

  @Override
  void setPart(int part) {

    if (!this.canRename)
      throw new EoulsanRuntimeException(
          "Data cannot be renamed once it has been used");

    super.setPart(part);

    // Update dataFiles
    updateDataFiles();
  }

  @Override
  public boolean isList() {
    return false;
  }

  @Override
  public List<Data> getListElements() {
    return Collections.singletonList((Data) this);
  }

  @Override
  public Map<String, String> getMetadata() {
    return this.metadata;
  }

  /**
   * Set a metadata.
   * @param key key of the metadata
   * @param value value of the metadata
   */
  protected void setMetadata(final String key, final String value) {

    if (key == null || value == null)
      return;

    this.metadata.put(key, value);
  }

  @Override
  public Data addDataToList(final String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Data addDataToList(final String name, final int part) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getDataFilename() {
    return getDataFile().getName();
  }

  @Override
  public String getDataFilename(final int fileIndex) {

    if (getFormat().getMaxFilesCount() < 2)
      throw new EoulsanRuntimeException(
          "Only multifiles DataFormat are handled by this method.");

    return this.files.get(fileIndex).getName();
  }

  @Override
  public DataFile getDataFile() {

    this.canRename = false;
    return this.files.get(0);
  }

  void setDataFile(final DataFile dataFile) {

    Preconditions.checkNotNull(dataFile, "DataFile to set cannot be null");

    if (this.files.size() == 0)
      throw new IllegalStateException(
          "Cannot set a DataFile if not already exists");

    this.files.set(0, dataFile);
  }

  void setDataFile(final int fileIndex, final DataFile dataFile) {

    Preconditions.checkArgument(fileIndex >= 0,
        "fileIndex argument must be >=0");
    Preconditions.checkNotNull(dataFile, "DataFile to set cannot be null");

    if (fileIndex >= this.files.size())
      throw new IllegalStateException(
          "Cannot set a DataFile if not already exists");

    this.files.set(fileIndex, dataFile);
  }

  List<DataFile> getDataFiles() {

    return Collections.unmodifiableList(Lists.newArrayList(this.files));
  }

  @Override
  public DataFile getDataFile(int fileIndex) {

    if (getFormat().getMaxFilesCount() < 2)
      throw new EoulsanRuntimeException(
          "Only multi-files DataFormat are handled by this method.");

    if (fileIndex < 0)
      throw new EoulsanRuntimeException(
          "File index parameter cannot be lower than 0");

    if (fileIndex > this.files.size())
      throw new EoulsanRuntimeException("Cannot create file index "
          + fileIndex + " as file index " + this.files.size()
          + " is not created");

    if (fileIndex >= getFormat().getMaxFilesCount())
      throw new EoulsanRuntimeException("The format "
          + getFormat().getName() + " does not support more than "
          + getFormat().getMaxFilesCount() + " multi-files");

    // Create DataFile is required
    if (fileIndex == this.files.size()) {
      this.files.add(createDataFile(fileIndex));
    }

    return this.files.get(fileIndex);
  }

  @Override
  public int getDataFileCount() {
    return this.files.size();
  }

  @Override
  public int getDataFileCount(boolean existingFiles) {
    return this.files.size();
  }

  private DataFile createDataFile(final int fileIndex) {

    return FileNaming.file(port, this, fileIndex);
  }

  private void updateDataFiles() {

    // If DataFile object(s) has not been set in the constructor
    if (this.port != null) {

      // Update the DataFile filename
      if (this.port.getFormat().getMaxFilesCount() > 1) {

        // Multi-file formats
        for (int i = 0; i < this.files.size(); i++) {
          this.files.set(i, createDataFile(i));
        }
      } else {
        // Mono-file formats
        this.files.set(0, createDataFile(-1));
      }
    }
  }

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("name", getName())
        .add("format", getFormat().getName()).add("metadata", getMetadata())
        .add("list", isList()).add("content", this.files).toString();
  }

  //
  // Constructor
  //

  DataElement(final DataFormat format, final List<DataFile> files) {

    super(format);

    checkNotNull(format, "format argument cannot be null");
    checkNotNull(files, "files argument cannot be null");

    for (DataFile f : files)
      if (f == null)
        throw new IllegalArgumentException(
            "The files list argument cannot contains null elements");

    this.files = Lists.newArrayList(files);

    this.port = null;
  }

  DataElement(final DataFormat format, final DataFile file) {
    this(format, Collections.singletonList(file));
  }

  DataElement(final WorkflowOutputPort port) {

    super(port.getFormat());

    checkNotNull(port, "port argument cannot be null");

    this.port = port;

    if (getFormat().getMaxFilesCount() == 1) {
      this.files = Lists.newArrayList(createDataFile(-1));
    } else {
      this.files = Lists.newArrayList(createDataFile(0));
    }
  }

}
