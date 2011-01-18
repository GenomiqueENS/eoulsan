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

package fr.ens.transcriptome.eoulsan.steps;

import java.io.IOException;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;

/**
 * This class implements a genome description generator step.
 * @author Laurent Jourdren
 */
public class GenomeDescriptionGeneratorStep extends AbstractStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  @Override
  public String getName() {

    return "_genomedescgenerator";
  }

  @Override
  public String getDescription() {

    return "Generate genome description";
  }

  @Override
  public DataFormat[] getInputFormats() {

    return new DataFormat[] {DataFormats.GENOME_FASTA};
  }

  @Override
  public DataFormat[] getOutputFormats() {

    return new DataFormat[] {DataFormats.GENOME_DESC_TXT};
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
      final DataFile genomeDescriptionDataFile =
          context.getDataFile(getOutputFormats()[0], s1);

      LOGGER.info("Input genome file: " + genomeDataFile);
      LOGGER.info("Output genome description file: "
          + genomeDescriptionDataFile);

      final GenomeDescription desc =
          GenomeDescription.createGenomeDescFromFasta(genomeDataFile.open());

      LOGGER.info("Genome description object: " + desc.toString());

      desc.save(genomeDescriptionDataFile.create());

    } catch (BadBioEntryException e) {

      return new StepResult(this, e);
    } catch (EoulsanException e) {

      return new StepResult(this, e);
    } catch (IOException e) {

      return new StepResult(this, e);
    }

    return new StepResult(this, startTime, "Genome description creation");
  }

}
