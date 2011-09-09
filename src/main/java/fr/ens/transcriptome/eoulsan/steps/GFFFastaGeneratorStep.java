package fr.ens.transcriptome.eoulsan.steps;

import java.io.IOException;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
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

/**
 * This generator allow to generate a genome fasta file from the fasta section
 * of a GFF file.
 * @author Laurent Jourdren
 */
public class GFFFastaGeneratorStep extends AbstractStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

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
      if (!s1.getMetadata().isAnnotationField())
        throw new EoulsanException("No annotation found in design file.");

      final String annotationSource = s1.getMetadata().getAnnotation();
      if (annotationSource == null)
        throw new EoulsanException("Annotation source is null.");

      // Get the annotation DataFile
      final DataFile annotationDataFile = new DataFile(annotationSource);

      // Get the output DataFile
      final DataFile genomeDataFile =
          context.getDataFile(getOutputFormats()[0], s1);

      LOGGER.info("Input annotation file: " + annotationDataFile);
      LOGGER.info("Output genome file: " + genomeDataFile);

      final SequenceReader reader =
          new GFFFastaReader(annotationDataFile.open());
      final SequenceWriter writer = new FastaWriter(genomeDataFile.create());

      // Copy the sequences
      for (Sequence sequence : reader)
        writer.write(sequence);

      // throw IOException of reader if needed
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
