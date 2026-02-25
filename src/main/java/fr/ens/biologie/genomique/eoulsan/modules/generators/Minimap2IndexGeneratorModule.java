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

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getGenericLogger;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.GENOME_FASTA;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.Generator;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.MapperIndexDataFormat;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.kenetre.bio.readmapper.Mapper;
import fr.ens.biologie.genomique.kenetre.bio.readmapper.MapperBuilder;
import fr.ens.biologie.genomique.kenetre.bio.readmapper.Minimap2MapperProvider;
import fr.ens.biologie.genomique.kenetre.util.Version;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * This class define a module that generate a Minimap2 mapper index.
 *
 * @since 2.1
 * @author Laurent Jourdren
 */
@LocalOnly
@Generator
public class Minimap2IndexGeneratorModule extends AbstractModule {

  public static final String MODULE_NAME = "minimap2indexgenerator";

  private final Mapper mapper =
      new MapperBuilder(Minimap2MapperProvider.MAPPER_NAME).withLogger(getGenericLogger()).build();

  private String indexerArguments = "";

  @Override
  public String getName() {

    return MODULE_NAME;
  }

  @Override
  public String getDescription() {

    return "Generate Mapper index";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    return new InputPortsBuilder()
        .addPort("genome", GENOME_FASTA)
        .addPort("genomedescription", GENOME_DESC_TXT)
        .create();
  }

  @Override
  public OutputPorts getOutputPorts() {
    return OutputPortsBuilder.singleOutputPort(new MapperIndexDataFormat(this.mapper));
  }

  @Override
  public void configure(final StepConfigurationContext context, final Set<Parameter> stepParameters)
      throws EoulsanException {

    if (stepParameters == null) {
      throw new EoulsanException("No parameters set in " + getName() + " generator");
    }

    for (Parameter p : stepParameters) {

      switch (p.getName()) {
        case "indexer.arguments":
          this.indexerArguments = p.getStringValue();
          break;

        default:
          throw new EoulsanException(
              "Unknown parameter for " + getName() + " step: " + p.getName());
      }
    }
  }

  @Override
  public TaskResult execute(final TaskContext context, final TaskStatus status) {

    try {

      status.setProgressMessage(this.mapper.getName() + " index creation");

      // Create the index
      GenomeMapperIndexGeneratorModule.execute(
          this.mapper,
          context,
          this.indexerArguments,
          Collections.singletonMap("indexer.arguments", this.indexerArguments));

    } catch (IOException | EoulsanException e) {

      return status.createTaskResult(e);
    }

    return status.createTaskResult();
  }
}
