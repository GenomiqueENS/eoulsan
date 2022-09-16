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
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATION_GFF;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.GENOME_FASTA;

import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.kenetre.util.Version;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.kenetre.bio.Sequence;
import fr.ens.biologie.genomique.kenetre.bio.io.FastaWriter;
import fr.ens.biologie.genomique.kenetre.bio.io.GFFFastaReader;
import fr.ens.biologie.genomique.kenetre.bio.io.SequenceReader;
import fr.ens.biologie.genomique.kenetre.bio.io.SequenceWriter;

/**
 * This generator allow to generate a genome fasta file from the fasta section
 * of a GFF file.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class GFFFastaGeneratorModule extends AbstractModule {

  public static final String MODULE_NAME = "gfffastagenerator";

  @Override
  public String getName() {

    return "gfffastagenerator";
  }

  @Override
  public String getDescription() {

    return "Generate Genome Fasta file from a Fasta section of GFF file";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
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
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    try {

      // Get input and output data
      final Data inData = context.getInputData(ANNOTATION_GFF);
      final Data outData = context.getOutputData(GENOME_FASTA, inData);

      // Get the annotation DataFile
      final DataFile annotationDataFile = inData.getDataFile();

      // Get the output DataFile
      final DataFile genomeDataFile = outData.getDataFile();

      getLogger().info("Input annotation file: " + annotationDataFile);
      getLogger().info("Output genome file: " + genomeDataFile);

      final SequenceReader reader =
          new GFFFastaReader(annotationDataFile.open());
      final SequenceWriter writer = new FastaWriter(genomeDataFile.create());

      for (final Sequence sequence : reader) {
        writer.write(sequence);
      }
      reader.throwException();

      reader.close();
      writer.close();

    } catch (IOException e) {

      return status.createTaskResult(e);
    }

    status.setProgressMessage("Genome fasta creation");
    return status.createTaskResult();
  }

}
