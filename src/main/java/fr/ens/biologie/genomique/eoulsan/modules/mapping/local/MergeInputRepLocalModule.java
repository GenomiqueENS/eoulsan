package fr.ens.biologie.genomique.eoulsan.modules.mapping.local;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
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
import fr.ens.biologie.genomique.eoulsan.core.Version;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import picard.sam.MergeSamFiles;

/**
 * This class merges SAM files of input of the same experiment. It uses Picard's
 * MergeSamFiles to merge SAM files from inputs of the same experiment.
 * @author Celine Hernandez - CSB lab - ENS - Paris
 */
@LocalOnly
public class MergeInputRepLocalModule extends AbstractModule {

  private static final String STEP_NAME = "mergeinput";

  //
  // Overridden methods
  //

  /**
   * Name of the Step.
   */
  @Override
  public String getName() {
    return STEP_NAME;
  }

  /**
   * A short description of the tool and what is done in the step.
   */
  @Override
  public String getDescription() {
    return "This step merges Input files for each experiment. "
        + "It uses Picard's MergeSamFiles.";
  }

  /**
   * Version.
   */
  @Override
  public Version getVersion() {
    return Globals.APP_VERSION;
  }

  /**
   * Define input port.
   */
  @Override
  public InputPorts getInputPorts() {
    final InputPortsBuilder builder = new InputPortsBuilder();
    builder.addPort("input", true, MAPPER_RESULTS_SAM);
    return builder.create();
  }

  /**
   * Define output port.
   */
  @Override
  public OutputPorts getOutputPorts() {
    final OutputPortsBuilder builder = new OutputPortsBuilder();
    builder.addPort("output", true, MAPPER_RESULTS_SAM);
    return builder.create();
  }

  /**
   * Set the parameters of the step to configure the step.
   * @param stepParameters parameters of the step
   * @throws EoulsanException if a parameter is invalid
   */
  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      getLogger().info("MergeInputRep parameter: "
          + p.getName() + " : " + p.getStringValue());
      throw new EoulsanException(
          "Unknown parameter for " + getName() + " step: " + p.getName());
    }

  }

  /**
   * Merge input replicates.
   * @throws EoulsanException if temp file can't be created.
   */
  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    // Get input data (SAM format)
    final Data inData = context.getInputData(MAPPER_RESULTS_SAM);

    // Get file name created by Eoulsan
    final Data outputDataList =
        context.getOutputData(MAPPER_RESULTS_SAM, "mergedinput");

    Map<String, List<Data>> referenceSamples = new HashMap<>();
    for (Data anInputData : inData.getListElements()) {

      getLogger().finest("Input file. ref : "
          + anInputData.getMetadata().get("Reference") + "| exp : "
          + anInputData.getMetadata().get("Experiment") + "| rep : "
          + anInputData.getMetadata().get("RepTechGroup"));

      boolean isReference = anInputData.getMetadata().get("Reference")
          .toLowerCase().equals("true");

      // Only treat reference files
      if (isReference) {
        final String experimentName =
            anInputData.getMetadata().get("Experiment");

        if (referenceSamples.containsKey(experimentName)
            && referenceSamples.get(experimentName) != null) {
          referenceSamples.get(experimentName).add(anInputData);
        } else {
          List<Data> tmpList = new ArrayList<Data>();
          tmpList.add(anInputData);
          referenceSamples.put(experimentName, tmpList);
        }
      } else {

        // If it's not a reference file, create a symlink with the correct
        // output
        // name (to make it available to further steps)

        final Data outputData = outputDataList.addDataToList(anInputData
            .getMetadata().get("Name").replaceAll("[^a-zA-Z0-9]", ""));
        outputData.getMetadata().set(anInputData.getMetadata());

        try {
          anInputData.getDataFile().symlink(outputData.getDataFile());
        } catch (IOException ioe) {
          getLogger().severe("Could not create symlink from "
              + anInputData.getDataFile() + " to " + outputData.getDataFile());
          return status.createTaskResult();
        }
      }

    }

    // Loop through all references
    for (String experimentName : referenceSamples.keySet()) {

      List<Data> expData = referenceSamples.get(experimentName);

      // If we have only one Input, just make a symbolic link having the correct
      // output name for this step
      if (expData.size() == 1) {

        // Get the one input of this experiment
        final Data inputData = expData.get(0);

        // Get file name created by Eoulsan
        final Data outputData = outputDataList.addDataToList(
            inputData.getMetadata().get("Name").replaceAll("[^a-zA-Z0-9]", ""));
        outputData.getMetadata().set(inputData.getMetadata());

        try {
          inputData.getDataFile().symlink(outputData.getDataFile());
        } catch (IOException ioe) {
          getLogger().severe("Could not create symlink from "
              + inputData.getDataFile() + " to " + outputData.getDataFile());
          return status.createTaskResult();
        }
      }

      // Use Picard's MegerSamFiles to sort and merge (only if more than one
      // input exists)
      if (expData.size() > 2) {

        getLogger().info(
            "Running Picard's MergeSamFiles for experiment  " + experimentName);

        // Get file name created by Eoulsan
        final Data outputData = outputDataList.addDataToList(expData.get(0)
            .getMetadata().get("Name").replaceAll("[^a-zA-Z0-9]", ""));
        outputData.getMetadata().set(expData.get(0).getMetadata());

        String[] arguments = new String[expData.size() + 2];
        arguments[0] = "OUTPUT=" + outputData.getDataFile();
        arguments[1] = "QUIET=true";

        int startPos = 2;
        for (Data anInputData : expData) {
          arguments[startPos++] = "INPUT=" + anInputData.getDataFile();
        }

        // Start MergeSamFiles
        new MergeSamFiles().instanceMain(arguments);

      }

    }

    return status.createTaskResult();

  }

}
