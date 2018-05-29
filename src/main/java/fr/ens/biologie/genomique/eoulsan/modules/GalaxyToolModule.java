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
package fr.ens.biologie.genomique.eoulsan.modules;

import static fr.ens.biologie.genomique.eoulsan.requirements.DockerRequirement.newDockerRequirement;

import java.io.InputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
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
import fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolInterpreter;
import fr.ens.biologie.genomique.eoulsan.galaxytools.ToolExecutorResult;
import fr.ens.biologie.genomique.eoulsan.galaxytools.ToolInfo;
import fr.ens.biologie.genomique.eoulsan.galaxytools.elements.DataToolElement;
import fr.ens.biologie.genomique.eoulsan.galaxytools.executorinterpreters.DockerExecutorInterpreter;
import fr.ens.biologie.genomique.eoulsan.io.CompressionType;
import fr.ens.biologie.genomique.eoulsan.requirements.Requirement;

/**
 * The Class GalaxyToolStep.
 * @author Sandrine Perrin
 * @since 2.0
 */
@LocalOnly
public class GalaxyToolModule extends AbstractModule {

  /** The tool interpreter. */
  private final GalaxyToolInterpreter toolInterpreter;

  /** The source of the Galaxy tool. */
  private final String source;

  /** The requirements of the tool. */
  private Set<Requirement> requirements = new HashSet<>();

  //
  // Module methods
  //

  @Override
  public String getName() {
    return this.toolInterpreter.getToolInfo().getToolName();
  }

  @Override
  public Version getVersion() {
    return new Version(this.toolInterpreter.getToolInfo().getToolVersion());
  }

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();

    for (final DataToolElement element : this.toolInterpreter
        .getInputDataElements()) {

      builder.addPort(element.getValidatedName(), element.getDataFormat(),
          EnumSet.of(CompressionType.NONE), true);
    }

    return builder.create();
  }

  @Override
  public OutputPorts getOutputPorts() {

    final OutputPortsBuilder builder = new OutputPortsBuilder();

    for (final DataToolElement element : this.toolInterpreter
        .getOutputDataElements()) {

      builder.addPort(element.getValidatedName(), element.getDataFormat());
    }

    return builder.create();
  }

  @Override
  public Set<Requirement> getRequirements() {

    return Collections.unmodifiableSet(this.requirements);
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    // Configure tool interpreter
    this.toolInterpreter.configure(stepParameters);

    // Check if Docker is enabled
    boolean dockerEnabled = context.getSettings().isDockerConnectionDefined();

    // If the interpreter of the tool is Docker, add the Docker image to the
    // list of the Docker image to fetch
    final ToolInfo toolData = this.toolInterpreter.getToolInfo();
    if (DockerExecutorInterpreter.INTERPRETER_NAME
        .equals(toolData.getInterpreter(dockerEnabled))) {

      this.requirements.add(newDockerRequirement(toolData.getDockerImage()));
    }
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    // TODO check in data and out data corresponding to tool.xml
    // Check DataFormat expected corresponding from taskContext

    // Check if Docker is enabled
    boolean dockerEnabled = context.getSettings().isDockerConnectionDefined();

    // Set the description of the context
    final ToolInfo toolInfo = this.toolInterpreter.getToolInfo();
    context.getLogger()
        .info("Launch tool galaxy "
            + toolInfo.getToolName() + ", version " + toolInfo.getToolVersion()
            + " with interpreter " + toolInfo.getInterpreter(dockerEnabled));

    final ToolExecutorResult result;

    try {
      result = this.toolInterpreter.execute(context);
    } catch (EoulsanException e) {
      return status.createTaskResult(e,
          "Error execution tool interpreter from building tool command line : "
              + e.getMessage());
    }

    // Execution script fail, create an exception
    if (!result.isException()) {
      final Throwable e = result.getException();

      return status.createTaskResult(e,
          "Error, execution interrupted: " + e.getMessage());
    }

    if (result.getExitValue() != 0) {

      return status.createTaskResult(null,
          "Fail execution of Galaxy tool with command: "
              + result.getCommandLine() + " Exit value: "
              + result.getExitValue());
    }

    return status.createTaskResult();
  }

  //
  // Other methods
  //

  /**
   * Get the source the Galaxy tool.
   * @return the source of the Galaxy tool
   */
  public String getSource() {

    return this.source;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param toolXMLis the input stream on tool xml file
   * @throws EoulsanException the Eoulsan exception
   */
  public GalaxyToolModule(final InputStream toolXMLis) throws EoulsanException {

    this(toolXMLis, null);
  }

  /**
   * Constructor.
   * @param in the input stream for XML tool file
   * @param source source of the Galaxy tool
   * @throws EoulsanException the Eoulsan exception
   */
  public GalaxyToolModule(final InputStream in, final String source)
      throws EoulsanException {

    if (in == null) {
      throw new NullPointerException("in argument cannot be null");
    }

    this.source = source == null ? "Undefined source" : source.trim();
    this.toolInterpreter = new GalaxyToolInterpreter(in, this.source);
  }

}
