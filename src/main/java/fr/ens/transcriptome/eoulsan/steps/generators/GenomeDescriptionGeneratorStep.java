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

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.GENOME_FASTA;

import java.io.IOException;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.storages.GenomeDescStorage;
import fr.ens.transcriptome.eoulsan.data.storages.SimpleGenomeDescStorage;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class implements a genome description generator step.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class GenomeDescriptionGeneratorStep extends AbstractStep {

  public static final String STEP_NAME = "genomedescgenerator";

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public String getDescription() {

    return "Generate genome description";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {
    return singleInputPort(GENOME_FASTA);
  }

  @Override
  public OutputPorts getOutputPorts() {
    return singleOutputPort(GENOME_DESC_TXT);
  }

  @Override
  public StepResult execute(final StepContext context,
      final StepStatus status) {

    // Get input and output data
    final Data inData = context.getInputData(GENOME_FASTA);
    final Data outData = context.getOutputData(GENOME_DESC_TXT, inData);

    try {

      // Get the genome DataFile
      final DataFile genomeDataFile = inData.getDataFile();

      // Get the output DataFile
      final DataFile genomeDescriptionDataFile = outData.getDataFile();

      getLogger().fine("Input genome file: " + genomeDataFile);
      getLogger()
          .fine("Output genome description file: " + genomeDescriptionDataFile);

      // Create genome description DataFile
      final GenomeDescription desc = new GenomeDescriptionCreator()
          .createGenomeDescription(genomeDataFile);

      // Save the genome description in the analysis folder
      desc.save(genomeDescriptionDataFile.create());

      getLogger().fine("Genome description object: " + desc.toString());

    } catch (BadBioEntryException e) {

      return status.createStepResult(e);
    } catch (IOException e) {

      return status.createStepResult(e);
    }

    status.setMessage("Genome description creation");
    return status.createStepResult();
  }

  /**
   * Check if a genome description storage has been defined.
   * @return a GenomeDescStorage object if genome storage has been defined or
   *         null if not
   */
  static GenomeDescStorage checkForGenomeDescStore() {

    final String genomeDescStoragePath =
        EoulsanRuntime.getSettings().getGenomeDescStoragePath();

    if (genomeDescStoragePath == null) {
      return null;
    }

    return SimpleGenomeDescStorage
        .getInstance(new DataFile(genomeDescStoragePath));
  }

}
