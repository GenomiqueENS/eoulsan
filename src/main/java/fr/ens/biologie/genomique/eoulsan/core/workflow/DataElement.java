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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntimeException;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataMetadata;
import fr.ens.biologie.genomique.eoulsan.design.Design;

/**
 * This class define a data element.
 * @since 2.0
 * @author Laurent Jourdren
 */
class DataElement extends AbstractData implements Serializable {

  private static final long serialVersionUID = -8982205120365590676L;

  private final DataMetadata metadata;
  protected final List<DataFile> files;

  // Field required for multi-files Data creation
  private final StepOutputPort port;

  private boolean canRename = true;

  @Override
  public void setName(final String name) {

    if (!this.canRename) {
      throw new EoulsanRuntimeException(
          "Data cannot be renamed once it has been used");
    }

    super.setName(name);

    // Update datafiles
    updateDataFiles();
  }

  @Override
  void setPart(final int part) {

    if (!this.canRename) {
      throw new EoulsanRuntimeException(
          "Data cannot be renamed once it has been used");
    }

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
  public DataMetadata getMetadata() {
    return this.metadata;
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

    if (getFormat().getMaxFilesCount() < 2) {
      throw new EoulsanRuntimeException(
          "Only multi-files DataFormat are handled by this method.");
    }

    return this.files.get(fileIndex).getName();
  }

  @Override
  public DataFile getDataFile() {

    if (getFormat().getMaxFilesCount() > 1) {
      throw new EoulsanRuntimeException(
          "Multi-files DataFormat cannot be handled by this method, "
              + "use getDataFile(int) instead.");
    }

    this.canRename = false;
    return this.files.get(0);
  }

  /**
   * Set the first data file.
   * @param dataFile data file to set
   */
  void setDataFile(final DataFile dataFile) {

    requireNonNull(dataFile, "DataFile to set cannot be null");

    if (this.files.size() == 0) {
      throw new IllegalStateException(
          "Cannot set a DataFile if not already exists");
    }

    this.files.set(0, dataFile);
  }

  /**
   * Set a DataFile.
   * @param fileIndex index of the data file
   * @param dataFile file to set
   */
  void setDataFile(final int fileIndex, final DataFile dataFile) {

    checkArgument(fileIndex >= 0, "fileIndex argument must be >=0");
    requireNonNull(dataFile, "DataFile to set cannot be null");

    if (fileIndex >= this.files.size()) {
      throw new IllegalStateException(
          "Cannot set a DataFile if not already exists");
    }

    this.files.set(fileIndex, dataFile);
  }

  /**
   * Set the DataFiles.
   * @param dataFiles files to set
   */
  void setDataFiles(final List<DataFile> dataFiles) {

    requireNonNull(dataFiles, "dataFiles to set cannot be null");

    for (DataFile file : dataFiles) {

      checkArgument(file != null, "dataFiles cannot contains null value");
      checkArgument(Collections.frequency(dataFiles, file) == 1,
          "dataFiles cannot contains the same file: " + file);
    }

    this.files.clear();
    this.files.addAll(dataFiles);
  }

  /**
   * Get the DataFiles of the data.
   * @return a list with the DataFiles
   */
  List<DataFile> getDataFiles() {

    return Collections.unmodifiableList(Lists.newArrayList(this.files));
  }

  @Override
  public DataFile getDataFile(final int fileIndex) {

    if (getFormat().getMaxFilesCount() < 2) {
      throw new EoulsanRuntimeException(
          "Only multi-files DataFormat are handled by this method, "
              + "use getDataFile() instead.");
    }

    if (fileIndex < 0) {
      throw new EoulsanRuntimeException(
          "File index parameter cannot be lower than 0");
    }

    if (fileIndex > this.files.size()) {
      throw new EoulsanRuntimeException("Cannot create file index "
          + fileIndex + " as file index " + this.files.size()
          + " is not created");
    }

    if (fileIndex >= getFormat().getMaxFilesCount()) {
      throw new EoulsanRuntimeException("The format "
          + getFormat().getName() + " does not support more than "
          + getFormat().getMaxFilesCount() + " multi-files");
    }

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
  public int getDataFileCount(final boolean existingFiles) {
    return this.files.size();
  }

  private DataFile createDataFile(final int fileIndex) {

    return WorkflowFileNaming.file(this.port, this, fileIndex);
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

    return MoreObjects.toStringHelper(this).add("name", getName())
        .add("format", getFormat().getName()).add("metadata", getMetadata())
        .add("list", isList()).add("content", this.files).toString();
  }

  //
  // Constructor
  //

  DataElement(final DataFormat format, final List<DataFile> files,
      final Design design) {

    super(format);

    this.metadata = new SimpleDataMetadata(design);

    requireNonNull(format, "format argument cannot be null");
    requireNonNull(files, "files argument cannot be null");
    requireNonNull(design, "design argument cannot be null");

    for (DataFile f : files) {
      if (f == null) {
        throw new IllegalArgumentException(
            "The files list argument cannot contains null elements");
      }
    }

    this.files = Lists.newArrayList(files);

    this.port = null;
  }

  DataElement(final DataFormat format, final DataFile file,
      final Design design) {
    this(format, Collections.singletonList(file), design);
  }

  DataElement(final StepOutputPort port, final Design design) {

    super(port.getFormat());

    requireNonNull(port, "port argument cannot be null");
    requireNonNull(design, "design argument cannot be null");

    this.metadata = new SimpleDataMetadata(design);

    this.port = port;

    if (getFormat().getMaxFilesCount() == 1) {
      this.files = Lists.newArrayList(createDataFile(-1));
    } else {
      this.files = Lists.newArrayList(createDataFile(0));
    }
  }

  /**
   * Copy constructor.
   * @param data data to copy
   */
  DataElement(final DataElement data) {

    super(data);

    this.metadata = new SimpleDataMetadata((SimpleDataMetadata) data.metadata);
    this.files = new ArrayList<>(data.files);
    this.port = data.port;
    this.canRename = true;
  }

}
