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
package fr.ens.transcriptome.eoulsan.steps.galaxytool;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder.singleOutputPort;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.galaxytool.elements.ToolElement;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * The Class GalaxyToolStep.
 * @author Sandrine Perrin
 * @since 2.1
 */
@LocalOnly
public class GalaxyToolStep extends AbstractStep {

  /** The Constant NAME. */
  public static final String STEP_NAME = "galaxytool";

  private static final String TOOL_NAME = "toolname";
  private static final String TOOL_XML_PATH = "toolxmlpath";
  private static final String TOOL_EXECUTABLE_PATH = "toolexecutablepath";

  private boolean isConfigured = false;
  private boolean isExecuted = false;

  /** The tool interpreter. */
  private ToolInterpreter toolInterpreter;

  @Override
  public String getName() {
    return STEP_NAME;
  }

  @Override
  public Version getVersion() {
    return Globals.APP_VERSION;
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
        .getOutDataFormatExpected().entrySet()) {
      // isEmpty = false;
      // builder.addPort(entry.getValue().getName(), entry.getKey());

      return singleOutputPort(entry.getKey());
    }

    // if (isEmpty) {
    return OutputPortsBuilder.noOutputPort();
    // }

    // return builder.create();
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) {

    if (isExecuted)
      throw new EoulsanRuntimeException(
          "GalaxyToolStep, this instance has been already executed");

    final Set<Parameter> toolParameters = new HashSet<>();

    String toolName = "";
    File toolXmlPath = null;
    File toolExecutablePath = null;

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      // Name tool
      case TOOL_NAME:
        toolName = p.getStringValue();
        break;

      // Path to a toolXML
      case TOOL_XML_PATH:
        String v = p.getStringValue();

        if (v == null || v.isEmpty())
          new EoulsanRuntimeException(
              "Configure step ToolGalaxy, xml path is null or empty.");

        toolXmlPath = new File(v);

        // TODO change code, use command guava/utils eoulsan
        if (!(toolXmlPath.exists() || toolXmlPath.canRead())) {
          new EoulsanRuntimeException("Configure step ToolGalaxy, xml path "
              + toolXmlPath.getAbsolutePath() + " can not readable.");
        }

        break;

      // Path to a executable tool
      case TOOL_EXECUTABLE_PATH:
        String exe = p.getStringValue();

        if (exe == null || exe.isEmpty())
          new EoulsanRuntimeException(
              "Configure step ToolGalaxy, tool executable path is null or empty.");

        toolExecutablePath = new File(exe);

        if (!(toolExecutablePath.exists() || toolExecutablePath.canRead())) {
          new EoulsanRuntimeException("Configure step ToolGalaxy, tool "
              + toolName + " executable path "
              + toolExecutablePath.getAbsolutePath() + " can not readable.");
        }

        break;

      default:
        toolParameters.add(p);
      }
    }

    // Init tool interpreter
    initToolInterpreter(toolParameters, toolName, toolXmlPath,
        toolExecutablePath);

    isConfigured = true;
  }

  private void initToolInterpreter(final Set<Parameter> toolParameters,
      final String toolName, final File toolXmlPath,
      final File toolExecutablePath) {

    checkNotNull(toolName, "None tool name define for Galaxy Tool step.");

    try {
      // Configure tool interpreter
      this.toolInterpreter =
          new ToolInterpreter(toolName,
              FileUtils.createInputStream(toolXmlPath), toolExecutablePath);

      this.toolInterpreter.configure(toolParameters);

    } catch (final EoulsanException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    if (!isConfigured)
      throw new EoulsanRuntimeException(
          "GalaxyToolStep, this instance has been configured");

    // TODO check in data and out data corresponding to tool.xml
    // Check DataFormat expected corresponding from stepContext

    checkArgument(
        this.toolInterpreter.checkDataFormat(context),
        "GalaxyTool step, dataFormat inval between extract from analysis and setting in xml file.");

    int exitValue = -1;
    GalaxyToolResult result = null;

    try {
      result = this.toolInterpreter.execute(context);
      exitValue = result.getExitValue();

    } catch (EoulsanException e) {
      return status.createStepResult(e,
          "Error execution tool interpreter from building tool command line : "
              + e.getMessage());
    }

    // Set the description of the context
    status.setDescription(this.toolInterpreter.getDescription());

    status.setMessage("Command line generate by python interpreter: "
        + result.getCommandLine() + ".");

    // Execution script fail, create an exception
    if (exitValue != 0) {

      return status.createStepResult(null,
          "Fail execution tool galaxy with command "
              + result.getCommandLine() + ". Exit value: " + exitValue);
    }

    if (result.asThrowedException()) {
      final Throwable e = result.getException();

      return status.createStepResult(e,
          "Error execution interrupted: " + e.getMessage());
    }

    isExecuted = true;

    return status.createStepResult();

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

    this.toolInterpreter = new ToolInterpreter("Unknown", toolXMLis, null);
  }

  /**
   * Constructor.
   * @throws EoulsanException the Eoulsan exception
   */
  public GalaxyToolStep() throws EoulsanException {

    this.toolInterpreter = null;
  }

}
