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

import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFileMetadata;
import fr.ens.biologie.genomique.kenetre.io.FileUtils;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;
import fr.ens.biologie.genomique.kenetre.storage.AbstractFileGenomeDescStorage;
import fr.ens.biologie.genomique.kenetre.storage.DataPath;
import fr.ens.biologie.genomique.kenetre.storage.GenomeDescStorage;

/**
 * This class define a storage for genome description files using DataFile API.
 * @since 2.6
 * @author Laurent Jourdren
 */
public class DataFileGenomeDescStorage extends AbstractFileGenomeDescStorage {

  @Override
  protected DataPath newDataPath(String source) {

    return new DataFileDataPath(source);
  }

  @Override
  protected DataPath newDataPath(DataPath parent, String filename) {

    return new DataFileDataPath(parent, filename);
  }

  @Override
  protected String computeMD5Sum(DataPath genomeFile) throws IOException {

    DataFile df = ((DataFileDataPath) genomeFile).getDataFile();
    DataFileMetadata md = null;

    try {
      md = df.getMetaData();
    } catch (IOException e) {
    }

    if (md != null
        && genomeFile.equals(this.lastGenomeFile)
        && this.lastGenomeFileModified == md.getLastModified()
        && this.lastMD5Computed != null) {
      return this.lastMD5Computed;
    }

    final String md5Sum = FileUtils.computeMD5Sum(genomeFile.rawOpen());

    if (md != null && md5Sum != null) {
      this.lastGenomeFile = genomeFile;
      this.lastGenomeFileModified = md.getLastModified();
      this.lastMD5Computed = md5Sum;
    }

    return md5Sum;
  }

  //
  // Static methods
  //

  /**
   * Create a GenomeDescStorage
   * @param dir the path of the genome descriptions storage
   * @param logger to use
   * @return a GenomeDescStorage object if the path contains an index storage or
   *         null if no index storage is found
   */
  public static GenomeDescStorage getInstance(final DataFile dir,
      final GenericLogger logger) {

    requireNonNull(dir);

    try {
      return new DataFileGenomeDescStorage(new DataFileDataPath(dir), logger);
    } catch (IOException | NullPointerException e) {
      return null;
    }
  }

  //
  // Constructor
  //

  /**
   * Protected constructor.
   * @param dir directory of the storage.
   * @param logger logger to use
   * @throws IOException if an error occurs while initializing the object
   */
  protected DataFileGenomeDescStorage(final DataPath dir,
      final GenericLogger logger) throws IOException {

    super(dir, logger);
  }

}
