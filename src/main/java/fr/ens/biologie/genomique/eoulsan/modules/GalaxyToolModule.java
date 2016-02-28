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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */
package fr.ens.biologie.genomique.eoulsan.modules;

import static fr.ens.biologie.genomique.eoulsan.requirements.DockerRequirement.newDockerRequirement;

import java.io.InputStream;
import java.util.Collections;
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
import fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolInterpreter;
import fr.ens.biologie.genomique.eoulsan.galaxytools.ToolData;
import fr.ens.biologie.genomique.eoulsan.galaxytools.ToolExecutorResult;
import fr.ens.biologie.genomique.eoulsan.galaxytools.elements.ToolElement;
import fr.ens.biologie.genomique.eoulsan.galaxytools.executorinterpreters.DockerExecutorInterpreter;
import fr.ens.biologie.genomique.eoulsan.requirements.Requirement;
import fr.ens.biologie.genomique.eoulsan.util.Version;

/**
 * The Class GalaxyToolStep.
 * @author Sandrine Perrin
 * @since 2.0
 */
@LocalOnly
public class GalaxyToolModule extends AbstractModule {

  /** Tool data. */
  private ToolData toolData;

  /** The tool interpreter. */
  private final GalaxyToolInterpreter toolInterpreter;

  /** The source of the Galaxy tool. */
  private final String source;

  /** The requirement of the tool. */
  private Requirement requirement;

  //
  // Steps methods
  //

  @Override
  public String getName() {
    return this.toolData.getToolName();
  }

  @Override
  public Version getVersion() {
    return new Version(this.toolData.getToolVersion());
  }

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();

    for (final ToolElement element : this.toolInterpreter
        .getInputDataElements()) {

      builder.addPort(element.getValidatedName(), element.getDataFormat(),
          true);
    }

    return builder.create();
  }

  @Override
  public OutputPorts getOutputPorts() {

    final OutputPortsBuilder builder = new OutputPortsBuilder();

    for (final ToolElement element : this.toolInterpreter
        .getOutputDataElements()) {

      builder.addPort(element.getValidatedName(), element.getDataFormat());
    }

    return builder.create();
  }

  @Override
  public Set<Requirement> getRequirements() {

    if (this.requirement == null) {
      return Collections.emptySet();
    }

    return Collections.singleton(this.requirement);
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    // Configure tool interpreter
    this.toolInterpreter.configure(stepParameters);

    // If the interpreter of the tool is Docker, add the Docker image to the
    // list of the Docker image to fetch
    final ToolData toolData = this.toolInterpreter.getToolData();
    if (DockerExecutorInterpreter.INTERPRETER_NAME
        .equals(toolData.getInterpreter())) {

      this.requirement = newDockerRequirement(toolData.getDockerImage());
    }
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    // TODO check in data and out data corresponding to tool.xml
    // Check DataFormat expected corresponding from stepContext

    final ToolExecutorResult result;

    try {

      result = this.toolInterpreter.execute(context);
    } catch (EoulsanException e) {
      return status.createTaskResult(e,
          "Error execution tool interpreter from building tool command line : "
              + e.getMessage());
    }

    // Set the description of the context
    status.setDescription(this.toolInterpreter.getDescription());

    status.setProgressMessage("Command line generate by python interpreter: "
        + result.getCommandLineAsString() + ".");

    // Execution script fail, create an exception
    if (!result.isException()) {
      final Throwable e = result.getException();

      return status.createTaskResult(e,
          "Error execution interrupted: " + e.getMessage());
    }

    if (result.getExitValue() != 0) {

      return status.createTaskResult(null,
          "Fail execution tool galaxy with command "
              + result.getCommandLine() + ". Exit value: "
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
   * @param toolXMLis the input stream on tool xml file
   * @param source source of the Galaxy tool
   * @throws EoulsanException the Eoulsan exception
   */
  public GalaxyToolModule(final InputStream toolXMLis, final String source)
      throws EoulsanException {

    if (toolXMLis == null) {
      throw new NullPointerException("toolXMLis argument cannot be null");
    }

    this.toolInterpreter = new GalaxyToolInterpreter(toolXMLis);
    this.source = source == null ? "Undefined source" : source.trim();

    // Extract tool data
    this.toolData = this.toolInterpreter.getToolData();
  }

}
