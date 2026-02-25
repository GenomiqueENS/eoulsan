package fr.ens.biologie.genomique.eoulsan.modules.generators;

import static fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder.singleOutputPort;

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.kenetre.util.Version;
import java.io.IOException;

/**
 * This class implements a dummy generator module that create an empty file.
 *
 * @since 2.0
 * @author Laurent Jourdren
 */
public class DummyGeneratorModule extends AbstractModule {

  public static final String MODULE_NAME = "dummygenerator";

  @Override
  public String getName() {

    return MODULE_NAME;
  }

  @Override
  public String getDescription() {

    return "Generate dummy data";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public OutputPorts getOutputPorts() {
    return singleOutputPort(DataFormats.DUMMY_TXT);
  }

  @Override
  public TaskResult execute(final TaskContext context, final TaskStatus status) {

    // Get input and output data
    final Data outData = context.getOutputData(DataFormats.DUMMY_TXT, "dummy");

    try {
      // Create empty file
      outData.getDataFile().create().close();
    } catch (IOException e) {
      return status.createTaskResult(e);
    }

    return status.createTaskResult();
  }
}
