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

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder.singleOutputPort;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.io.CharStreams;
import com.google.common.io.Files;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
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
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
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

  private static final String STDERR = "stderr";

  private static final String STDOUT = "stdout";

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
  public StepResult execute(final StepContext context, final StepStatus status) {

    final Map<DataFormat, DataFile> inDataFiles = new HashMap<>();
    final Map<DataFormat, DataFile> outDataFiles = new HashMap<>();

    Data inData = null;

    // TODO check in data and out data corresponding to tool.xml

    // Create all inData set in toolshed
    for (DataFormat df : this.toolInterpreter.getInDataFormatExpected()
        .keySet()) {

      inData = context.getInputData(df);
      inDataFiles.put(df, inData.getDataFile());
    }

    // Create all inData set in toolshed
    for (DataFormat df : this.toolInterpreter.getOutDataFormatExpected()
        .keySet()) {

      outDataFiles.put(df, context.getOutputData(df, inData).getDataFile());
    }

    // TODO
    // System.out.println("DEBUG Before execute tool\n\tin data file :"
    // + Joiner.on(",").join(inDataFiles.values()) + "\n\tout data file :"
    // + Joiner.on(",").join(outDataFiles.values()));

    try {

      final String commandTool =
          this.toolInterpreter.execute(inDataFiles, outDataFiles);

      // TODO
      // System.out.println("GalaxyToolStep: final command line " +
      // commandTool);

      // Define stdout and stderr file
      // Execute command
      final Process p = Runtime.getRuntime().exec(commandTool, null);

      // Save stdout
      new CopyProcessOutput(p.getInputStream(), STDOUT).start();

      // Save stderr
      new CopyProcessOutput(p.getErrorStream(), STDERR).start();

      // Wait the end of the process
      final int exitValue = p.waitFor();

      // Set the description of the context
      status.setDescription("Launch tool galaxy");
      status.setMessage("Command line generate by python interpreter: "
          + commandTool + ")");

      // Execution script fail, create an exception
      if (exitValue != 0) {
        // TODO cat in StepResult
        System.out.println("FAIL process. Exit value " + exitValue);

        return status.createStepResult(null,
            "Fail execution tool galaxy with command "
                + commandTool + ". Exit value: " + exitValue);
      }

    } catch (final InterruptedException e) {
      e.printStackTrace();
      return status.createStepResult(e,
          "Error execution interrupted: " + e.getMessage());

    } catch (final EoulsanException e) {
      e.printStackTrace();
      return status.createStepResult(e, "Error while execution command tool: "
          + e.getMessage());

    } catch (final IOException e) {
      e.printStackTrace();
      return status.createStepResult(e, "Error while reading toolshed file: "
          + e.getMessage());
    }

    return status.createStepResult();

  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) {

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

    // Configure tool interpreter
    try {

      this.toolInterpreter =
          new ToolInterpreter(toolName,
              FileUtils.createInputStream(toolXmlPath), toolExecutablePath);

      this.toolInterpreter.configure(toolParameters);

      // // TODO
      // System.out.println("----- DEBUG end configure, cmd line "
      // + this.toolInterpreter.getCommandLine()
      // + "\n\t----------- name thread " + Thread.currentThread().getName());

    } catch (final EoulsanException | IOException e) {
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

  /**
   * This internal class allow to save Process outputs.
   * @author Laurent Jourdren
   */
  private static final class CopyProcessOutput extends Thread {

    /** The default size of the buffer. */
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    private final InputStream in;
    private final String desc;

    @Override
    public void run() {

      try {

        final Path std =
            new File(EoulsanRuntime.getSettings().getTempDirectory(),
                "STDERR_OUT").toPath();

        java.nio.file.Files.copy(this.in, std,
            StandardCopyOption.REPLACE_EXISTING);

        // // String txt = IOUtils.toString(this.in, Globals.DEFAULT_CHARSET);
        String txt =
            CharStreams.toString(new InputStreamReader(this.in,
                Globals.DEFAULT_CHARSET));

        getLogger().warning(txt);

            } catch (final IOException e) {
        getLogger().warning(
            "Error while copying " + this.desc + ": " + e.getMessage());
      }

    }

    CopyProcessOutput(final InputStream in, final String desc) {

      checkNotNull(in, "in argument cannot be null");
      checkNotNull(desc, "desc argument cannot be null");

      this.in = in;
      this.desc = desc;
    }

  }
}
