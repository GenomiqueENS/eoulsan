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
  public String getLogName() {

    return null;
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

      // Get the first sample
      final Sample s1 = design.getSamples().get(0);

      // Get the genome DataFile
      final DataFile genomeDataFile =
          context.getDataFile(DataFormats.GENOME_FASTA, s1);

      // Get the output DataFile
      final DataFile genomeDescriptionDataFile =
          context.getDataFile(getOutputFormats()[0], s1);

      LOGGER.fine("Input genome file: " + genomeDataFile);
      LOGGER.fine("Output genome description file: "
          + genomeDescriptionDataFile);

      final GenomeDescription desc =
          GenomeDescription.createGenomeDescFromFasta(genomeDataFile.open(),
              genomeDataFile.getName());

      LOGGER.fine("Genome description object: " + desc.toString());

      desc.save(genomeDescriptionDataFile.create());

    } catch (BadBioEntryException e) {

      return new StepResult(context, e);
    } catch (EoulsanException e) {

      return new StepResult(context, e);
    } catch (IOException e) {

      return new StepResult(context, e);
    }

    return new StepResult(context, startTime, "Genome description creation");
  }

}
