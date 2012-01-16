/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.data.protocols;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFileMetadata;
import fr.ens.transcriptome.eoulsan.io.CompressionType;

/**
 * This abstract class define a storage protocol. It is useful to easily access
 * common resources like genomes or annotations.
 * @author Laurent Jourdren
 */
public abstract class StorageDataProtocol extends AbstractDataProtocol {

  protected abstract String getExtension();

  protected abstract String getBasePath();

  @Override
  public InputStream getData(final DataFile src) throws IOException {

    return internalDataFile(src).rawOpen();
  }

  @Override
  public OutputStream putData(final DataFile dest) throws IOException {

    throw new IOException("PutData() method is no supported by "
        + getName() + " protocol");
  }

  @Override
  public boolean exists(final DataFile src) {

    try {
      final DataFile f = internalDataFile(src);

      if (f == null)
        return false;

      return f.exists();
    } catch (IOException e) {

      return false;
    }
  }

  @Override
  public DataFileMetadata getMetadata(final DataFile src) throws IOException {

    return internalDataFile(src).getMetaData();
  }

  @Override
  public boolean isReadable() {

    return true;
  }

  @Override
  public boolean isWritable() {

    return false;
  }

  private DataFile internalDataFile(final DataFile src) throws IOException {

    final String basePath = getBasePath();

    if (basePath == null)
      throw new IOException(getName() + " storage is not configurated");

    final DataFile baseDir = new DataFile(basePath);

    if (!baseDir.exists())
      throw new IOException(getName()
          + " storage base path does not exists: " + baseDir);

    final String filename = src.getName().toLowerCase().trim() + getExtension();

    for (CompressionType c : CompressionType.values()) {

      final DataFile f = new DataFile(baseDir, filename + c.getExtension());
      if (f.exists())
        return f;
    }

    throw new IOException("No " + getName() + " found for: " + src.getName());
  }

}
