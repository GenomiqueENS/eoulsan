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

import static fr.ens.transcriptome.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.GENOME_FASTA;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.GenomeIndexStorage;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapperService;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SimpleGenomeIndexStorage;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.protocols.DataProtocolService;
import fr.ens.transcriptome.eoulsan.data.protocols.FileDataProtocol;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

public class ReadsIndexGeneratorStep extends AbstractStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private final SequenceReadsMapper mapper;

  @Override
  public String getName() {

    return "_genericindexgenerator";
  }

  @Override
  public String getDescription() {

    return "Generate Mapper index";
  }

  @Override
  public DataFormat[] getInputFormats() {

    return new DataFormat[] {GENOME_FASTA, GENOME_DESC_TXT};
  }

  @Override
  public DataFormat[] getOutputFormats() {

    return new DataFormat[] {this.mapper.getArchiveFormat()};
  }

  @Override
  public StepResult execute(final Design design, final Context context) {

    final long startTime = System.currentTimeMillis();

    try {

      if (design.getSampleCount() == 0)
        throw new EoulsanException("No sample found in design file.");

      final Sample s1 = design.getSamples().get(0);
      if (!s1.getMetadata().isGenomeField())
        throw new EoulsanException("No genome found in design file.");

      final String genomeSource = s1.getMetadata().getGenome();
      if (genomeSource == null)
        throw new EoulsanException("Genome source is null.");

      // Get the genome DataFile
      final DataFile genomeDataFile = new DataFile(genomeSource);

      // Get the genome description DataFile
      final DataFile descDataFile = context.getDataFile(GENOME_DESC_TXT, s1);
      final GenomeDescription desc =
          GenomeDescription.load(descDataFile.open());

      // Get the output DataFile
      final DataFile mapperIndexDataFile =
          context.getDataFile(this.mapper.getArchiveFormat(), s1);

      // Set mapper temporary directory
      mapper.setTempDirectory(context.getSettings().getTempDirectoryFile());

      // Get genome Index storage path
      final String genomeIndexStoragePath =
          context.getSettings().getGenomeIndexStoragePath();

      final GenomeIndexStorage storage;
      if (genomeIndexStoragePath == null)
        storage = null;
      else
        storage =
            SimpleGenomeIndexStorage.getInstance(new DataFile(
                genomeIndexStoragePath));

      final DataFile precomputedIndexDataFile;

      if (storage == null)
        precomputedIndexDataFile = null;
      else
        precomputedIndexDataFile = storage.get(this.mapper, desc);

      if (precomputedIndexDataFile == null) {
        LOGGER.info("Genome index not found, must compute it.");
        computeIndex(context, mapperIndexDataFile, genomeDataFile);
        if (storage != null)
          storage.put(this.mapper, desc, mapperIndexDataFile);
      } else
        downloadPrecomputedIndex(precomputedIndexDataFile, mapperIndexDataFile);

    } catch (EoulsanException e) {

      return new StepResult(context, e);
    } catch (IOException e) {

      return new StepResult(context, e);
    }

    return new StepResult(context, startTime, this.mapper.getMapperName()
        + " index creation");
  }

  private void computeIndex(final Context context, final DataFile mapperIndex,
      final DataFile genome) throws IOException {

    final FileDataProtocol defaultProtocol =
        DataProtocolService.getInstance().getDefaultProtocol();

    final File outputFile;

    if (mapperIndex.isLocalFile()) {

      outputFile = defaultProtocol.getFile(mapperIndex);
    } else {
      outputFile =
          context.getRuntime().createTempFile(
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

  private void downloadPrecomputedIndex(final DataFile precomputedIndex,
      final DataFile output) throws IOException {

    if (precomputedIndex.isLocalFile() && output.isLocalFile()) {
      FileUtils.createSymbolicLink(new File(precomputedIndex.getSource()),
          new File(output.getSource()));
    } else
      FileUtils.copy(precomputedIndex.rawOpen(), output.create());
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param mapperName name of the mapper
   */
  public ReadsIndexGeneratorStep(final String mapperName) {

    Preconditions.checkNotNull(mapperName, "Mapper name is null");

    this.mapper =
        SequenceReadsMapperService.getInstance().getMapper(mapperName);

    Preconditions.checkNotNull(this.mapper, "Mapper name not found: "
        + mapperName);
  }

}
