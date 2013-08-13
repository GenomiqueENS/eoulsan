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

import static com.google.common.collect.Sets.newHashSet;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.GENOME_FASTA;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.data.storages.GenomeDescStorage;
import fr.ens.transcriptome.eoulsan.data.storages.SimpleGenomeDescStorage;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;

/**
 * This class implements a genome description generator step.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class GenomeDescriptionGeneratorStep extends AbstractStep {

  /** Logger */
  private static final Logger LOGGER = EoulsanLogger.getLogger();

  @Override
  public String getName() {

    return "_genomedescgenerator";
  }

  @Override
  public String getDescription() {

    return "Generate genome description";
  }

  @Override
  public Set<DataFormat> getInputFormats() {
    return newHashSet(GENOME_FASTA);
  }

  @Override
  public Set<DataFormat> getOutputFormats() {
    return newHashSet(GENOME_DESC_TXT);
  }

  @Override
  public StepResult execute(final Design design, final StepContext context,
      final StepStatus status) {

    try {

      if (design.getSampleCount() == 0)
        throw new EoulsanException("No sample found in design file.");

      // Get the first sample
      final Sample s1 = design.getSamples().get(0);

      // Get the genome DataFile
      final DataFile genomeDataFile =
          context.getInputDataFile(DataFormats.GENOME_FASTA, s1);

      // Get the output DataFile
      final DataFile genomeDescriptionDataFile =
          context.getOutputDataFile(GENOME_DESC_TXT, s1);

      LOGGER.fine("Input genome file: " + genomeDataFile);
      LOGGER.fine("Output genome description file: "
          + genomeDescriptionDataFile);

      // Create genome description DataFile
      final GenomeDescription desc =
          new GenomeDescriptionCreator()
              .createGenomeDescription(genomeDataFile);

      // Save the genome description in the analysis folder
      desc.save(genomeDescriptionDataFile.create());

      LOGGER.fine("Genome description object: " + desc.toString());

    } catch (BadBioEntryException e) {

      return status.createStepResult(e);
    } catch (EoulsanException e) {

      return status.createStepResult(e);
    } catch (IOException e) {

      return status.createStepResult(e);
    }

    status.setStepMessage("Genome description creation");
    return status.createStepResult();
  }

  /**
   * Check if a genome description storage has been defined.
   * @return a GenomedescStorage object if genome storage has been defined or
   *         null if not
   */
  static GenomeDescStorage checkForGenomeDescStore() {

    final String genomeDescStoragePath =
        EoulsanRuntime.getSettings().getGenomeDescStoragePath();

    if (genomeDescStoragePath == null)
      return null;

    return SimpleGenomeDescStorage.getInstance(new DataFile(
        genomeDescStoragePath));
  }

}
