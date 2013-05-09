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

import java.io.IOException;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.bio.Sequence;
import fr.ens.transcriptome.eoulsan.bio.io.FastaWriter;
import fr.ens.transcriptome.eoulsan.bio.io.GFFFastaReader;
import fr.ens.transcriptome.eoulsan.bio.io.SequenceReader;
import fr.ens.transcriptome.eoulsan.bio.io.SequenceWriter;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.StepResult;

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
  public String getLogName() {

    return null;
  }

  @Override
  public DataFormat[] getInputFormats() {

    return new DataFormat[] {DataFormats.ANNOTATION_GFF};
  }

  @Override
  public DataFormat[] getOutputFormats() {

    return new DataFormat[] {DataFormats.GENOME_FASTA};
  }

  @Override
  public StepResult execute(final Design design, final Context context) {

    final long startTime = System.currentTimeMillis();

    try {

      if (design.getSampleCount() == 0)
        throw new EoulsanException("No sample found in design file.");

      final Sample s1 = design.getSamples().get(0);

      // Get the annotation DataFile
      final DataFile annotationDataFile =
          context.getInputDataFile(getInputFormats()[0], s1);

      // Get the output DataFile
      final DataFile genomeDataFile =
          context.getOutputDataFile(getOutputFormats()[0], s1);

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

    } catch (EoulsanException e) {

      return new StepResult(context, e);
    } catch (IOException e) {

      return new StepResult(context, e);
    }

    return new StepResult(context, startTime, "Genome fasta creation");
  }

}
