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

import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.ANNOTATION_GFF;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.GENOME_FASTA;

import java.io.IOException;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.bio.Sequence;
import fr.ens.transcriptome.eoulsan.bio.io.FastaWriter;
import fr.ens.transcriptome.eoulsan.bio.io.GFFFastaReader;
import fr.ens.transcriptome.eoulsan.bio.io.SequenceReader;
import fr.ens.transcriptome.eoulsan.bio.io.SequenceWriter;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.Data;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * This generator allow to generate a genome fasta file from the fasta section
 * of a GFF file.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class GFFFastaGeneratorStep extends AbstractStep {

  /** Logger */
  private static final Logger LOGGER = EoulsanLogger.getLogger();

  @Override
  public String getName() {

    return "_gfffastagenerator";
  }

  @Override
  public String getDescription() {

    return "Generate Genome Fasta file from a Fasta section of GFF file";
  }

  @Override
  public InputPorts getInputPorts() {
    return singleInputPort(ANNOTATION_GFF);
  }

  @Override
  public OutputPorts getOutputPorts() {
    return singleOutputPort(GENOME_FASTA);
  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    try {

      // Get input and output data
      final Data inData = context.getInputData(ANNOTATION_GFF);
      final Data outData = context.getOutputData(GENOME_FASTA, inData);

      // Get the annotation DataFile
      final DataFile annotationDataFile = inData.getDataFile();

      // Get the output DataFile
      final DataFile genomeDataFile = outData.getDataFile();

      LOGGER.info("Input annotation file: " + annotationDataFile);
      LOGGER.info("Output genome file: " + genomeDataFile);

      final SequenceReader reader =
          new GFFFastaReader(annotationDataFile.open());
      final SequenceWriter writer = new FastaWriter(genomeDataFile.create());

      for (final Sequence sequence : reader)
        writer.write(sequence);
      reader.throwException();

      reader.close();
      writer.close();

    } catch (IOException e) {

      return status.createStepResult(e);
    }

    status.setStepMessage("Genome fasta creation");
    return status.createStepResult();
  }

}
