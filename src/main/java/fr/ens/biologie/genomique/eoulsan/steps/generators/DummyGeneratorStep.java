package fr.ens.biologie.genomique.eoulsan.steps.generators;

import static fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder.singleOutputPort;

import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.StepContext;
import fr.ens.biologie.genomique.eoulsan.core.StepResult;
import fr.ens.biologie.genomique.eoulsan.core.StepStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.steps.AbstractStep;
import fr.ens.biologie.genomique.eoulsan.util.Version;

/**
 * This class implements a dummy generator step that create an empty file.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class DummyGeneratorStep extends AbstractStep {

  public static final String STEP_NAME = "dummygenerator";

  @Override
  public String getName() {

    return STEP_NAME;
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
  public StepResult execute(final StepContext context,
      final StepStatus status) {

    // Get input and output data
    final Data outData = context.getOutputData(DataFormats.DUMMY_TXT, "dummy");

    try {
      // Create empty file
      outData.getDataFile().create().close();
    } catch (IOException e) {
      return status.createStepResult(e);
    }

    return status.createStepResult();
  }

}
