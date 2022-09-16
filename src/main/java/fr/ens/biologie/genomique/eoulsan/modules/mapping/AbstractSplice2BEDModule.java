package fr.ens.biologie.genomique.eoulsan.modules.mapping;

import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULT_BED;

import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.kenetre.util.Version;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;

/**
 * This class define a module for converting SAM files into BED.
 * @since 2.3
 * @author Laurent Jourdren
 */
public abstract class AbstractSplice2BEDModule extends AbstractModule {

  private static final String MODULE_NAME = "splice2bed";

  protected static final String COUNTER_GROUP = "splice2bed";

  //
  // Module methods
  //

  @Override
  public String getName() {

    return MODULE_NAME;
  }

  @Override
  public String getDescription() {

    return "This module convert SAM files into BED files.";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    return singleInputPort(MAPPER_RESULTS_SAM);
  }

  @Override
  public OutputPorts getOutputPorts() {
    return singleOutputPort(MAPPER_RESULT_BED);
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      default:

        Modules.unknownParameter(context, p);
        break;
      }
    }

  }

}
