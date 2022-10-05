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
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.kenetre.bio.GenomeDescription;
import fr.ens.biologie.genomique.kenetre.bio.readmapper.MapperInstance;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;
import fr.ens.biologie.genomique.kenetre.storage.FileGenomeMapperIndexer;
import fr.ens.biologie.genomique.kenetre.storage.GenomeIndexStorage;

/**
 * This class define a genome mapper indexer.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class DataFileGenomeMapperIndexer extends FileGenomeMapperIndexer {

  /**
   * Create an archived genome index.
   * @param genomeDataFile genome to index
   * @param genomeDescription description of the genome
   * @param mapperIndexDataFile output genome index archive
   * @throws IOException if an error occurs while creating the genome
   */
  public void createIndex(final DataFile genomePath,
      final GenomeDescription genomeDescription, final DataFile mapperIndexPath)
      throws IOException {

    requireNonNull(genomePath);
    requireNonNull(genomePath);

    createIndex(new DataFileDataPath(genomePath), genomeDescription,
        new DataFileDataPath(mapperIndexPath));
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param mapperInstance Mapper to use for the index generator
   * @param additionalArguments additional indexer arguments
   * @param additionalDescription additional indexer arguments description
   * @param storage the genome index storage
   * @param temporaryDirectory temporary directory for the indexer
   * @param logger the logger
   */
  public DataFileGenomeMapperIndexer(MapperInstance mapperInstance,
      String additionalArguments, Map<String, String> additionalDescription,
      int threads, GenomeIndexStorage storage, File temporaryDirectory,
      GenericLogger logger) {

    super(mapperInstance, additionalArguments, additionalDescription, threads,
        storage, temporaryDirectory, logger);
  }

}
