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

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFiles;
import fr.ens.transcriptome.eoulsan.data.storages.GenomeIndexStorage;
import fr.ens.transcriptome.eoulsan.data.storages.SimpleGenomeIndexStorage;

/**
 * This class define a genome mapper indexer.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class GenomeMapperIndexer {

  private final SequenceReadsMapper mapper;
  private final GenomeIndexStorage storage;
  private final LinkedHashMap<String, String> additionalDescription;

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

    getLogger().info("Mapper name: " + this.mapper.getMapperName());
    getLogger().info("Mapper version: " + this.mapper.getMapperVersion());
    getLogger().info("Mapper flavor: " + this.mapper.getMapperFlavor());
    getLogger().info("Indexer arguments: " + this.mapper.getIndexerArguments());

    if (this.storage == null) {
      precomputedIndexDataFile = null;
    } else {
      precomputedIndexDataFile =
          this.storage.get(this.mapper, genomeDescription,
              this.additionalDescription);
    }

    // If no index storage or if the index does not already exists compute it
    if (precomputedIndexDataFile == null) {

      getLogger().info("Mapper index not found, must compute it");

      // Compute mapper index
      computeIndex(genomeDataFile, mapperIndexDataFile);

      // Save mapper index in storage
      if (this.storage != null) {
        this.storage.put(this.mapper, genomeDescription,
            this.additionalDescription, mapperIndexDataFile);
      }
    } else {

      getLogger().info(
          "Mapper index found, no need to recompute it (mapper index file: "
              + precomputedIndexDataFile + ")");

      // Else download it
      DataFiles.symlinkOrCopy(precomputedIndexDataFile, mapperIndexDataFile);
    }

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
              this.mapper.getMapperName() + "-index-archive-", ".zip");
    }

    if (genome.toFile() != null) {
      this.mapper.makeArchiveIndex(genome.toFile(), outputFile);
    } else {
      this.mapper.makeArchiveIndex(genome.open(), outputFile);
    }

    getLogger().info("mapperIndexDataFile: " + mapperIndex);

    if (!mapperIndex.isLocalFile()) {

      new DataFile(outputFile.getAbsolutePath()).copyTo(mapperIndex);

      if (!outputFile.delete()) {
        getLogger().severe(
            "Unable to delete temporary "
                + this.mapper.getMapperName() + " archive index.");
      }

    }
  }

  /**
   * Check if a genome storage has been defined.
   * @return a GenomeIndexStorage object if genome storage has been defined or
   *         null if not
   */
  private GenomeIndexStorage checkForGenomeIndexStore() {

    final String genomeIndexStoragePath =
        EoulsanRuntime.getSettings().getGenomeMapperIndexStoragePath();

    if (genomeIndexStoragePath == null) {
      return null;
    }

    return SimpleGenomeIndexStorage.getInstance(new DataFile(
        genomeIndexStoragePath));
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param mapper Mapper to use for the index generator
   * @param additionnalArguments additional indexer arguments
   * @param additionalDescription additional indexer arguments description
   */
  public GenomeMapperIndexer(final SequenceReadsMapper mapper,
      final String additionnalArguments,
      final Map<String, String> additionalDescription) {

    checkNotNull(mapper, "Mapper is null");
    checkNotNull(additionalDescription, "additionalDescription is null");

    // Set the mapper
    this.mapper = mapper;

    // Get genome Index storage path
    this.storage = checkForGenomeIndexStore();

    // Set indexer additional arguments of the indexer
    this.mapper.setIndexerArguments(additionnalArguments);

    // Get the additional description
    this.additionalDescription = new LinkedHashMap<>(additionalDescription);
  }

}
