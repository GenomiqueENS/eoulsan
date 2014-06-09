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

import java.io.IOException;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapperService;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;

/**
 * This class define a step that generate a genome mapper index.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class GenomeMapperIndexGeneratorStep extends AbstractStep {

  private SequenceReadsMapper mapper;

  @Override
  public String getName() {

    return "_genericindexgenerator";
  }

  @Override
  public String getDescription() {

    return "Generate Mapper index";
  }

  @Override
  public InputPorts getInputPorts() {
    return new InputPortsBuilder().addPort("genome", GENOME_FASTA)
        .addPort("genome_description", GENOME_DESC_TXT).create();
  }

  @Override
  public OutputPorts getOutputPorts() {
    return OutputPortsBuilder.singleOutputPort(this.mapper.getArchiveFormat());
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    if (stepParameters == null)
      throw new EoulsanException("No parameters set in "
          + getName() + " generator");

    for (Parameter p : stepParameters) {

      if ("mappername".equals(p.getName().toLowerCase())) {
        final String mapperName = p.getStringValue();

        this.mapper =
            SequenceReadsMapperService.getInstance().newService(mapperName);

        if (this.mapper == null)
          throw new EoulsanException(
              "Mapper with the following name not found: " + mapperName);

      } else
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());

    }

  }

  @Override
  public StepResult execute(final Design design, final StepContext context,
      final StepStatus status) {

    try {

      if (design.getSampleCount() == 0)
        throw new EoulsanException("No sample found in design file.");

      final Sample s1 = design.getSamples().get(0);

      // Get the genome DataFile
      final DataFile genomeDataFile =
          context.getInputDataFile(GENOME_FASTA, s1);

      // Get the genome description DataFile
      final DataFile descDataFile =
          context.getInputDataFile(GENOME_DESC_TXT, s1);
      final GenomeDescription desc =
          GenomeDescription.load(descDataFile.open());

      // Get the output DataFile
      final DataFile mapperIndexDataFile =
          context.getOutputDataFile(this.mapper.getArchiveFormat(), s1);

      // Set mapper temporary directory
      mapper.setTempDirectory(context.getSettings().getTempDirectoryFile());

      // Create indexer
      final GenomeMapperIndexer indexer = new GenomeMapperIndexer(this.mapper);

      // Create index
      indexer.createIndex(genomeDataFile, desc, mapperIndexDataFile);

    } catch (EoulsanException e) {

      return status.createStepResult(e);
    } catch (IOException e) {

      return status.createStepResult(e);
    }

    status.setStepMessage(this.mapper.getMapperName() + " index creation");
    return status.createStepResult();
  }

}
