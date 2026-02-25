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

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataMetadata;
import java.io.Serializable;
import java.util.List;

/**
 * This class define an unmodifiable data
 *
 * @since 2.0
 * @author Laurent Jourdren
 */
public class UnmodifiableData implements Data, Serializable {

  private static final long serialVersionUID = -7754468478398255288L;

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
  public int getPart() {

    return this.data.getPart();
  }

  @Override
  public DataMetadata getMetadata() {
    return new UnmodifiableDataMetadata(this.data.getMetadata());
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
  public int size() {
    return getListElements().size();
  }

  @Override
  public boolean isEmpty() {
    return getListElements().isEmpty();
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
  public DataFile getDataFile(final int fileIndex) {
    return this.data.getDataFile(fileIndex);
  }

  @Override
  public int getDataFileCount() {
    return this.data.getDataFileCount();
  }

  @Override
  public int getDataFileCount(final boolean existingFiles) {
    return this.data.getDataFileCount(existingFiles);
  }

  /**
   * Get the AbstractData object wrapped by this object.
   *
   * @return the AbstractData object wrapped by this object
   */
  AbstractData getData() {

    Data data = this;

    do {
      data = ((UnmodifiableData) data).data;
    } while (data instanceof UnmodifiableData);

    return (AbstractData) data;
  }

  @Override
  public String toString() {

    return MoreObjects.toStringHelper(this)
        .add("name", getName())
        .add("format", getFormat().getName())
        .add("metadata", getMetadata())
        .add("list", isList())
        .add("elements", getListElements())
        .toString();
  }

  //
  // Constructor
  //

  UnmodifiableData(final Data data) {

    requireNonNull(data, "data argument cannot be null");

    this.data = data;
  }
}
