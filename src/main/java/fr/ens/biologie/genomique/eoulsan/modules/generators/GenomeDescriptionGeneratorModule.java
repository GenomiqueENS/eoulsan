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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.modules.generators;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.GENOME_FASTA;

import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.storages.DataFileGenomeDescStorage;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.kenetre.bio.BadBioEntryException;
import fr.ens.biologie.genomique.kenetre.bio.GenomeDescription;
import fr.ens.biologie.genomique.kenetre.util.Version;
import java.io.IOException;

/**
 * This class implements a genome description generator module.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public class GenomeDescriptionGeneratorModule extends AbstractModule {

  public static final String MODULE_NAME = "genomedescgenerator";

  @Override
  public String getName() {

    return MODULE_NAME;
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
  public TaskResult execute(final TaskContext context, final TaskStatus status) {

    // Get input and output data
    final Data inData = context.getInputData(GENOME_FASTA);
    final Data outData = context.getOutputData(GENOME_DESC_TXT, inData);

    try {

      // Get the genome DataFile
      final DataFile genomeDataFile = inData.getDataFile();

      // Get the output DataFile
      final DataFile genomeDescriptionDataFile = outData.getDataFile();

      getLogger().fine("Input genome file: " + genomeDataFile);
      getLogger().fine("Output genome description file: " + genomeDescriptionDataFile);

      // Create genome description DataFile
      final GenomeDescription desc =
          new GenomeDescriptionCreator().createGenomeDescription(genomeDataFile);

      // Save the genome description in the analysis folder
      desc.save(genomeDescriptionDataFile.create());

      getLogger().fine("Genome description object: " + desc.toString());

    } catch (BadBioEntryException | IOException e) {

      return status.createTaskResult(e);
    }

    status.setProgressMessage("Genome description creation");
    return status.createTaskResult();
  }

  /**
   * Check if a genome description storage has been defined.
   *
   * @return a GenomeDescStorage object if genome storage has been defined or null if not
   */
  static DataFileGenomeDescStorage checkForGenomeDescStore() {

    final String genomeDescStoragePath = EoulsanRuntime.getSettings().getGenomeDescStoragePath();

    if (genomeDescStoragePath == null) {
      return null;
    }

    return (DataFileGenomeDescStorage)
        DataFileGenomeDescStorage.getInstance(
            new DataFile(genomeDescStoragePath), EoulsanLogger.getGenericLogger());
  }
}
