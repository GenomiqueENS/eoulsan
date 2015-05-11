package fr.ens.transcriptome.eoulsan.steps.galaxytool;

import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.steps.galaxytool.elements.ToolElement;

public interface ToolInterpreter {

  /**
   * Parse tool file to extract useful data to run tool.
   * @param setStepParameters the set step parameters
   * @throws EoulsanException if an data missing
   */
  void configure(final Set<Parameter> setStepParameters)
      throws EoulsanException;

  /**
   * Convert command tag from tool file in string, variable are replace by
   * value.
   * @param context Step context
   * @return the string
   * @throws EoulsanException the Eoulsan exception
   */
  ToolExecutorResult execute(final StepContext context) throws EoulsanException;

  /**
   * Gets the in data format expected associated with variable found in command
   * line.
   * @return the in data format expected
   */
  Map<DataFormat, ToolElement> getInDataFormatExpected();

  /**
   * Gets the out data format expected associated with variable found in command
   * line.
   * @return the out data format expected
   */
  Map<DataFormat, ToolElement> getOutDataFormatExpected();

  ToolData getToolData();

}
