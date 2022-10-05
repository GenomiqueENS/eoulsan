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
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.kenetre.bio.GenomeDescription;
import fr.ens.biologie.genomique.kenetre.bio.readmapper.MapperInstance;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;
import fr.ens.biologie.genomique.kenetre.storage.AbstractFileGenomeIndexStorage;
import fr.ens.biologie.genomique.kenetre.storage.DataPath;
import fr.ens.biologie.genomique.kenetre.storage.GenomeIndexStorage;

/**
 * This class define a storage for genome indexes using DataFile API.
 * @since 2.6
 * @author Laurent Jourdren
 */
public class DataFileGenomeIndexStorage extends AbstractFileGenomeIndexStorage {

  @Override
  protected DataPath newDataPath(String source) {

    return new DataFileDataPath(source);
  }

  @Override
  protected DataPath newDataPath(DataPath parent, String filename) {

    return new DataFileDataPath(parent, filename);
  }

  /**
   * Get the DataFile that corresponds to a mapper and a genome
   * @param mapperInstance mapper
   * @param genome genome description object for the genome
   * @param additionalDescription description of the additional parameters
   * @return a file that contains the path to the index or null if the index has
   *         not yet been computed
   */
  public DataFile getDataFile(MapperInstance mapperInstance,
      GenomeDescription genome, Map<String, String> additionalDescription) {

    DataPath result =
        getDataPath(mapperInstance, genome, additionalDescription);

    return ((DataFileDataPath) result).getDataFile();
  }

  /**
   * Put the index archive in the storage.
   * @param mapperInstance mapper
   * @param genome genome description object
   * @param additionalDescription description of the additional parameters
   * @param indexArchive the file that contains the index
   */
  public void put(final MapperInstance mapper, final GenomeDescription genome,
      final Map<String, String> additionalDescription,
      final DataFile indexArchive) {

    requireNonNull(indexArchive, "IndexArchive is null");

    put(mapper, genome, additionalDescription,
        new DataFileDataPath(indexArchive));
  }

  //
  // Static methods
  //

  /**
   * Create a GenomeIndexStorage
   * @param source the path of the index storage
   * @param logger the logger
   * @return a GenomeIndexStorage object if the path contains an index storage
   *         or null if no index storage is found
   */
  public static GenomeIndexStorage getInstance(final String dir,
      GenericLogger logger) {

    requireNonNull(dir);

    try {
      return new DataFileGenomeIndexStorage(new DataFileDataPath(dir), logger);
    } catch (IOException | NullPointerException e) {
      return null;
    }
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param dir directory of the storage.
   * @param logger logger to use
   * @throws IOException if an error occurs while initializing the object
   */
  protected DataFileGenomeIndexStorage(final DataPath dir,
      final GenericLogger logger) throws IOException {

    super(dir, logger);
  }

}
