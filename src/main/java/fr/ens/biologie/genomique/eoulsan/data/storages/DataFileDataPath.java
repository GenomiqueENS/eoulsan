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

package fr.ens.biologie.genomique.eoulsan.data.storages;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFileMetadata;
import fr.ens.biologie.genomique.eoulsan.data.DataFiles;
import fr.ens.biologie.genomique.kenetre.io.FileUtils;
import fr.ens.biologie.genomique.kenetre.storage.DataPath;

/**
 * This class define a bridge between Kenetre DataPath objects and Eoulsan
 * DataFile objects.
 * @since 2.6
 * @author Laurent Jourdren
 */
public class DataFileDataPath
    implements DataPath, Comparable<DataFileDataPath> {

  private final DataFile file;

  public DataFile getDataFile() {
    return this.file;
  }

  @Override
  public DataPath canonicalize() throws IOException {

    if (!this.file.isLocalFile()) {
      return this;
    }

    return new DataFileDataPath(new DataFile(file.toFile().getCanonicalFile()));
  }

  @Override
  public OutputStream create() throws IOException {

    return this.file.create();
  }

  @Override
  public boolean exists() {

    return this.file.exists();
  }

  @Override
  public long getContentLength() {

    try {
      final DataFileMetadata md = this.file.getMetaData();
      return md.getContentLength();
    } catch (IOException e) {
      return 0;
    }
  }

  @Override
  public long getLastModified() {
    try {
      final DataFileMetadata md = this.file.getMetaData();
      return md.getLastModified();
    } catch (IOException e) {
      return 0;
    }
  }

  @Override
  public String getName() {

    return this.file.getName();
  }

  @Override
  public String getSource() {

    return this.file.getSource();
  }

  @Override
  public List<DataPath> list() throws IOException {

    if (!this.file.getProtocol().canList()) {
      return Collections.emptyList();
    }

    List<DataPath> result = new ArrayList<>();

    for (DataFile f : this.file.list()) {
      result.add(new DataFileDataPath(f));
    }

    return Collections.unmodifiableList(result);
  }

  @Override
  public InputStream open() throws IOException {

    return this.file.open();
  }

  @Override
  public OutputStream rawCreate() throws IOException {

    return this.file.rawCreate();
  }

  @Override
  public InputStream rawOpen() throws IOException {

    return this.file.rawOpen();
  }

  @Override
  public File toFile() {

    return this.file.toFile();
  }

  @Override
  public void copy(DataPath output) throws IOException {

    requireNonNull(output);

    FileUtils.copy(open(), output.create());
  }

  @Override
  public void symlinkOrCopy(DataPath link) throws IOException {

    requireNonNull(link);

    if (!(link instanceof DataFileDataPath)) {
      copy(link);
    }

    DataFile output = ((DataFileDataPath) link).file;

    DataFiles.symlinkOrCopy(this.file, output);
  }

  //
  // Object methods
  //

  @Override
  public int compareTo(DataFileDataPath o) {

    if (o == null) {
      return -1;
    }

    return this.file.compareTo(o.file);
  }

  @Override
  public int hashCode() {

    return this.file.hashCode();
  }

  @Override
  public boolean equals(Object obj) {

    return this.file.equals(obj);
  }

  @Override
  public String toString() {

    return this.file.toString();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param source source of the DataFile
   */
  public DataFileDataPath(String source) {

    requireNonNull(source);

    this.file = new DataFile(source);
  }

  /**
   * Constructor.
   * @param file DataFile object to wrap
   */
  public DataFileDataPath(DataFile file) {

    requireNonNull(file);

    this.file = file;
  }

  /**
   * Constructor.
   * @param parent parent DataFile
   * @param filename filename
   */
  public DataFileDataPath(DataPath parent, String filename) {

    requireNonNull(parent);
    requireNonNull(filename);

    if (!(parent instanceof DataFileDataPath)) {
      throw new IllegalArgumentException(
          "parent is not a DataFileDataPath object");
    }

    DataFileDataPath p = (DataFileDataPath) parent;

    this.file = new DataFile(p.file, filename);
  }

}
