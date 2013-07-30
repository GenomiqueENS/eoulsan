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

package fr.ens.transcriptome.eoulsan.steps.generators;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.storages.GenomeIndexStorage;
import fr.ens.transcriptome.eoulsan.data.storages.SimpleGenomeIndexStorage;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define a genome mapper indexer.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class GenomeMapperIndexer {

  private final SequenceReadsMapper mapper;
  private final GenomeIndexStorage storage;

  /** Logger */
  private static final Logger LOGGER = EoulsanLogger.getLogger();

  /**
   * Create an archived genome index.
   * @param genomeDataFile genome to index
   * @param genomeDescription description of the genome
   * @param mapperIndexDataFile output genome index archive
   * @throws IOException if an error occurs while creating the genome
   */
  public void createIndex(final DataFile genomeDataFile,
      final GenomeDescription genomeDescription,
      final DataFile mapperIndexDataFile) throws IOException {

    final DataFile precomputedIndexDataFile;

    if (storage == null)
      precomputedIndexDataFile = null;
    else
      precomputedIndexDataFile = storage.get(this.mapper, genomeDescription);

    // If no index storage or if the index does not already exists compute it
    if (precomputedIndexDataFile == null) {
      LOGGER.info("Genome index not found, must compute it.");
      computeIndex(genomeDataFile, mapperIndexDataFile);
      if (storage != null)
        storage.put(this.mapper, genomeDescription, mapperIndexDataFile);
    } else
      // Else download it
      downloadPrecomputedIndex(precomputedIndexDataFile, mapperIndexDataFile);

  }

  /**
   * This this method that really launch index computation.
   * @param genome the path to the genome
   * @param mapperIndex the path to the output archive index
   * @throws IOException if an error occurs while computing index
   */
  private void computeIndex(final DataFile genome, final DataFile mapperIndex)
      throws IOException {

    File outputFile = mapperIndex.toFile();
    if (outputFile == null) {
      outputFile =
          EoulsanRuntime.getRuntime().createTempFile(
              mapper.getMapperName() + "-index-archive-", ".zip");
    }

    if (genome.toFile() != null) {
      this.mapper.makeArchiveIndex(genome.toFile(), outputFile);
    } else {
      this.mapper.makeArchiveIndex(genome.open(), outputFile);
    }

    LOGGER.info("mapperIndexDataFile: " + mapperIndex);

    if (!mapperIndex.isLocalFile()) {

      new DataFile(outputFile.getAbsolutePath()).copyTo(mapperIndex);

      if (!outputFile.delete()) {
        LOGGER.severe("Unbable to delete temporary "
            + this.mapper.getMapperName() + " archive index.");
      }

    }
  }

  /**
   * Download the index from the genome index storage.
   * @param precomputedIndex Path to the precomputed index in the storage
   * @param output output path to the index
   * @throws IOException if an error occurs while copying the index
   */
  private void downloadPrecomputedIndex(final DataFile precomputedIndex,
      final DataFile output) throws IOException {

    if (precomputedIndex.isLocalFile() && output.isLocalFile()) {
      FileUtils.createSymbolicLink(precomputedIndex.toFile(), output.toFile());
    } else
      FileUtils.copy(precomputedIndex.rawOpen(), output.create());
  }

  /**
   * Check if a genome storage has been defined.
   * @return a GenomeIndexStorage object if genome storage has been defined or
   *         null if not
   */
  private GenomeIndexStorage checkForGenomeIndexStore() {

    final String genomeIndexStoragePath =
        EoulsanRuntime.getSettings().getGenomeMapperIndexStoragePath();

    if (genomeIndexStoragePath == null)
      return null;

    return SimpleGenomeIndexStorage.getInstance(new DataFile(
        genomeIndexStoragePath));
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param mapper Mapper to use for the index generator
   */
  public GenomeMapperIndexer(final SequenceReadsMapper mapper) {

    Preconditions.checkNotNull(mapper, "Mapper is null");

    // Set the mapper
    this.mapper = mapper;

    // Get genome Index storage path
    this.storage = checkForGenomeIndexStore();
  }

}
