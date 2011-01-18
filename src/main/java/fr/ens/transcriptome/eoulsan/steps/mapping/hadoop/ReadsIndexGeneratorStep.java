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

package fr.ens.transcriptome.eoulsan.steps.mapping.hadoop;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapperService;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
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

    return new DataFormat[] {DataFormats.GENOME_FASTA};
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

      // Get the output DataFile
      final DataFile mapperIndexDataFile =
          context.getDataFile(this.mapper.getArchiveFormat(), s1);

      final FileDataProtocol defaultProtocol =
          DataProtocolService.getInstance().getDefaultProtocol();

      final File outputFile;

      if (mapperIndexDataFile.isDefaultProtocol()) {

        outputFile = defaultProtocol.getFile(mapperIndexDataFile);
      } else {
        outputFile =
            FileUtils.createTempFile(
                mapper.getMapperName() + "-index-archive-", ".zip");
      }

      if (genomeDataFile.isDefaultProtocol()) {

        this.mapper.makeArchiveIndex(defaultProtocol.getFile(genomeDataFile),
            outputFile);
      } else {
        this.mapper.makeArchiveIndex(genomeDataFile.open(), outputFile);
      }

      LOGGER.info("mapperIndexDataFile: " + mapperIndexDataFile);
      LOGGER.info("mapperIndexDataFile.isDefaultProtocol(): "
          + mapperIndexDataFile.isDefaultProtocol());

      if (!mapperIndexDataFile.isDefaultProtocol()) {

        new DataFile(outputFile.getAbsolutePath()).copyTo(mapperIndexDataFile);

        if (!outputFile.delete()) {
          context.getLogger().severe(
              "Unbable to delete temporary "
                  + this.mapper.getMapperName() + " archive index.");
        }

      }

    } catch (EoulsanException e) {

      return new StepResult(this, e);
    } catch (IOException e) {

      return new StepResult(this, e);
    }

    return new StepResult(this, startTime, this.mapper.getMapperName()
        + " index creation");
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
