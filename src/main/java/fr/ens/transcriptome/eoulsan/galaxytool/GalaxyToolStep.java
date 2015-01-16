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
package fr.ens.transcriptome.eoulsan.galaxytool;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.galaxytool.element.ToolElement;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.util.Version;

// TODO: Auto-generated Javadoc
/**
 * The Class GalaxyToolStep.
 * @author Sandrine Perrin
 * @since 2.1
 */
public class GalaxyToolStep extends AbstractStep {

  /** The Constant NAME. */
  private static final String NAME = "galaxytool";

  /** The tool interpreter. */
  private final ToolInterpreter toolInterpreter;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Version getVersion() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    return null;
  }

  /**
   * Execute.
   * @param inputData the input data
   * @param outputData the output data
   * @param status the status
   * @return the int
   */
  public int execute(final Map<DataFormat, DataFile> inputData,
      final Map<DataFormat, DataFile> outputData, final StepStatus status) {

    try {

      final String commandTool =
          this.toolInterpreter.execute(inputData, outputData);

      // TODO
      // System.out.println("GalaxyToolStep: final command line " +
      // commandTool);

      // Execute command
      final Process p = Runtime.getRuntime().exec(commandTool, null);

      // Wait the end of the process
      final int exitValue = p.waitFor();

      // Execution script fail, create an exception
      if (exitValue != 0) {
        // TODO cat in StepResult
        System.out.println("FAIL process. exit value " + exitValue);
      }

      return exitValue;

    } catch (final InterruptedException e) {
      e.printStackTrace();

    } catch (final EoulsanException e) {
      e.printStackTrace();

    } catch (final IOException e) {
      e.printStackTrace();
    }

    return 0;

  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) {

    // Configure tool interpreter
    try {

      this.toolInterpreter.configure(stepParameters);

    } catch (final EoulsanException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();
    boolean isEmpty = true;

    for (final Map.Entry<DataFormat, ToolElement> entry : this.toolInterpreter
        .getInDataFormatExpected().entrySet()) {
      isEmpty = false;
      builder.addPort(entry.getValue().getName(), entry.getKey(), true);
    }

    if (isEmpty) {
      return InputPortsBuilder.noInputPort();
    }

    return builder.create();
  }

  @Override
  public OutputPorts getOutputPorts() {

    final OutputPortsBuilder builder = new OutputPortsBuilder();
    boolean isEmpty = true;

    for (final Map.Entry<DataFormat, ToolElement> entry : this.toolInterpreter
        .getInDataFormatExpected().entrySet()) {
      isEmpty = false;
      builder.addPort(entry.getValue().getName(), entry.getKey());
    }

    if (isEmpty) {
      return OutputPortsBuilder.noOutputPort();
    }

    return builder.create();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param toolXMLis the input stream on tool xml file
   * @throws EoulsanException the Eoulsan exception
   */
  public GalaxyToolStep(final InputStream toolXMLis) throws EoulsanException {

    this.toolInterpreter = new ToolInterpreter(toolXMLis);
  }

}
