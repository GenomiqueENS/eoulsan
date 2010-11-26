package fr.ens.transcriptome.eoulsan.core;

import java.util.List;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.Step;

public interface WorkflowDescription {

  /**
   * Get the list of Steps of the workflow.
   * @return a list with the steps to execute
   */
  List<Step> getSteps();

  /**
   * Get the DataFormat of the inputs of the workflow for a sample.
   * @param sample a sample of the design
   * @return a set with all input DataFormat of the workflow
   */
  Set<DataFormat> getGlobalInputDataFormat(Sample sample);

  /**
   * Get the DataFormat of the outputs of the workflow for a sample.
   * @param sample a sample of the design
   * @return a set with all output DataFormat of the workflow
   */
  Set<DataFormat> getGlobalOutputDataFormat(Sample sample);

}
