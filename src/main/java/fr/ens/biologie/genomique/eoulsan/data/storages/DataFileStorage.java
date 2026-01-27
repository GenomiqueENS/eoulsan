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

import java.io.File;
import java.io.IOException;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.kenetre.storage.AbstractFileStorage;
import fr.ens.biologie.genomique.kenetre.storage.DataPath;

/**
 * This class define a storage using DataFile API.
 * @since 2.6
 * @author Laurent Jourdren
 */
public class DataFileStorage extends AbstractFileStorage {

  @Override
  protected DataPath newDataPath(String source) {

    return new DataFileDataPath(source);
  }

  @Override
  protected DataPath newDataPath(DataPath parent, String filename) {

    return new DataFileDataPath(parent, filename);
  }

  /**
   * Get a Datafile related to a short name
   * @param shortName the short name of a file
   * @return a DataFile
   * @throws IOException if the file cannot be found
   */
  public DataFile getDataFile(String shortName) throws IOException {

    DataPath result = getDataPath(shortName);

    return result != null ? ((DataFileDataPath) result).getDataFile() : null;
  }

  /**
   * Get a file related to a short name
   * @param shortName the short name of a file
   * @return a DataFile
   * @throws IOException if the file cannot be found
   */
  @Override
  public File getFile(String shortName) throws IOException {

    DataFile result = getDataFile(shortName);

    if (result.isLocalFile()) {
      return result.toFile();
    }

    throw new UnsupportedOperationException();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param rootPath root of the storage
   * @param extensions extension of the files
   */
  public DataFileStorage(String rootPath, List<String> extensions) {
    super(rootPath, extensions);
  }

}
