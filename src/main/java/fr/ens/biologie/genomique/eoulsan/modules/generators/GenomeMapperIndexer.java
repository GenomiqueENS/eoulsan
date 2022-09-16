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

package fr.ens.biologie.genomique.eoulsan.modules.generators;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFiles;
import fr.ens.biologie.genomique.eoulsan.data.storages.GenomeIndexStorage;
import fr.ens.biologie.genomique.eoulsan.data.storages.SimpleGenomeIndexStorage;
import fr.ens.biologie.genomique.kenetre.bio.GenomeDescription;
import fr.ens.biologie.genomique.kenetre.bio.readmapper.MapperInstance;

/**
 * This class define a genome mapper indexer.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class GenomeMapperIndexer {

  private final MapperInstance mapperInstance;
  private final GenomeIndexStorage storage;
  private final String indexerArguments;
  private final int threads;
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

    getLogger().info("Mapper name: " + this.mapperInstance.getName());
    getLogger().info("Mapper version: " + this.mapperInstance.getVersion());
    getLogger().info("Mapper flavor: " + this.mapperInstance.getFlavor());
    getLogger().info("Indexer arguments: " + this.indexerArguments);

    if (this.storage == null) {
      precomputedIndexDataFile = null;
    } else {
      precomputedIndexDataFile = this.storage.get(this.mapperInstance,
          genomeDescription, this.additionalDescription);
    }

    // If no index storage or if the index does not already exists compute it
    if (precomputedIndexDataFile == null) {

      getLogger().info("Mapper index not found, must compute it");

      // Compute mapper index
      computeIndex(genomeDataFile, mapperIndexDataFile);

      // Save mapper index in storage
      if (this.storage != null) {
        this.storage.put(this.mapperInstance, genomeDescription,
            this.additionalDescription, mapperIndexDataFile);
      }
    } else {

      getLogger().info(
          "Mapper index found, no need to recompute it (mapper index file: "
              + precomputedIndexDataFile + ")");

      getLogger()
          .info("Copy or create a symbolic link for the mapper index file "
              + "(Created file or symbolic link: " + mapperIndexDataFile + ")");

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
      outputFile = EoulsanRuntime.getRuntime().createTempFile(
          this.mapperInstance.getName() + "-index-archive-", ".zip");
    }

    if (genome.toFile() != null) {
      this.mapperInstance.makeArchiveIndex(genome.toFile(), outputFile,
          this.indexerArguments, this.threads);
    } else {
      this.mapperInstance.makeArchiveIndex(genome.open(), outputFile,
          this.indexerArguments, this.threads);
    }

    getLogger().info("mapperIndexDataFile: " + mapperIndex);

    if (!mapperIndex.isLocalFile()) {

      new DataFile(outputFile.getAbsolutePath()).copyTo(mapperIndex);

      if (!outputFile.delete()) {
        getLogger().severe("Unable to delete temporary "
            + this.mapperInstance.getName() + " archive index.");
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

    return SimpleGenomeIndexStorage
        .getInstance(new DataFile(genomeIndexStoragePath));
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param mapperInstance Mapper to use for the index generator
   * @param additionalArguments additional indexer arguments
   * @param additionalDescription additional indexer arguments description
   */
  public GenomeMapperIndexer(final MapperInstance mapperInstance,
      final String additionalArguments,
      final Map<String, String> additionalDescription, final int threads) {

    requireNonNull(mapperInstance, "Mapper is null");
    requireNonNull(additionalDescription, "additionalDescription is null");

    // Set the mapper
    this.mapperInstance = mapperInstance;

    // Get genome Index storage path
    this.storage = checkForGenomeIndexStore();

    // Set indexer additional arguments of the indexer
    this.indexerArguments =
        additionalArguments == null || additionalArguments.trim().isEmpty()
            ? "" : additionalArguments;

    // Set the threads number
    this.threads = threads;

    // Get the additional description
    this.additionalDescription = new LinkedHashMap<>(additionalDescription);
  }

}
