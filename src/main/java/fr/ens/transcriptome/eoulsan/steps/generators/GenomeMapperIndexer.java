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

package fr.ens.transcriptome.eoulsan.steps.generators;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.GenomeIndexStorage;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SimpleGenomeIndexStorage;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.protocols.DataProtocolService;
import fr.ens.transcriptome.eoulsan.data.protocols.FileDataProtocol;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define a genome mapper indexer.
 * @author Laurent Jourdren
 */
public final class GenomeMapperIndexer {

  private final SequenceReadsMapper mapper;
  private final GenomeIndexStorage storage;

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

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

    final FileDataProtocol defaultProtocol =
        DataProtocolService.getInstance().getDefaultProtocol();

    final File outputFile;

    if (mapperIndex.isLocalFile()) {

      outputFile = defaultProtocol.getFile(mapperIndex);
    } else {
      outputFile =
          EoulsanRuntime.getRuntime().createTempFile(
              mapper.getMapperName() + "-index-archive-", ".zip");
    }

    if (genome.isLocalFile()) {

      this.mapper.makeArchiveIndex(defaultProtocol.getFile(genome), outputFile);
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
      FileUtils.createSymbolicLink(new File(precomputedIndex.getSource()),
          new File(output.getSource()));
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
