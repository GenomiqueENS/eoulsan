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
package fr.ens.biologie.genomique.eoulsan.galaxytools;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.nullToEmpty;
import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.extractCheetahScript;
import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.extractDescription;
import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.extractDockerImage;
import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.extractInterpreters;
import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.extractToolID;
import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.extractToolName;
import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.extractToolVersion;

import java.util.List;

import org.w3c.dom.Document;

import com.google.common.base.Objects;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.galaxytools.executorinterpreters.DockerExecutorInterpreter;

/**
 * The class define a tool data which contains all data extracted from XML file.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class ToolInfo {

  private static final String DEFAULT_VERSION = "unknown";

  /** The tool id. */
  private final String toolID;

  /** The tool name. */
  private final String toolName;

  /** The tool source. */
  private final String toolSource;

  /** The tool version. */
  private final String toolVersion;

  /** The description. */
  private final String description;

  /** The interpreters. */
  private final List<String> interpreters;

  /** The command script. */
  private final String cheetahScript;

  /** The Docker image. */
  private final String dockerImage;

  //
  // Getters
  //

  /**
   * Get the tool source.
   * @return the tool source
   */
  public String getToolSource() {
    return this.toolSource;
  }

  /**
   * Get the tool Id.
   * @return the tool id
   */
  public String getToolID() {
    return this.toolID;
  }

  /**
   * Get the tool name.
   * @return the tool name
   */
  public String getToolName() {
    return this.toolName;
  }

  /**
   * Get the tool version.
   * @return the tool version
   */
  public String getToolVersion() {
    return this.toolVersion;
  }

  /**
   * Get the tool description.
   * @return the tool description
   */
  public String getDescription() {
    return this.description;
  }

  /**
   * Get the interpreter.
   * @return the interpreter
   */
  public String getInterpreter(final boolean dockerEnabled) {
    return selectInterpreter(this.interpreters, dockerEnabled);
  }

  /**
   * Get the Cheetah script.
   * @return the Cheetah script
   */
  public String getCheetahScript() {
    return this.cheetahScript;
  }

  /**
   * Get Docker image.
   * @return the docker image
   */
  public String getDockerImage() {
    return this.dockerImage;
  }

  /**
   * Select the interpreter.
   * @param interpreters interpreters to checks
   * @return the interpreter to use
   * @throws EoulsanException if a bad couple of interpreter has been chosen
   */
  private static String selectInterpreter(List<String> interpreters,
      boolean dockerEnabled) {

    if (interpreters == null || interpreters.isEmpty()) {
      return "";
    }

    if (interpreters.size() == 1) {
      return interpreters.get(0);
    }

    if (DockerExecutorInterpreter.INTERPRETER_NAME.equals(interpreters.get(0))
        && !dockerEnabled) {
      return interpreters.get(1);
    }
    return interpreters.get(0);
  }

  //
  // Object method
  //

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("toolID", this.toolID)
        .add("toolName", this.toolName).add("toolVersion", this.toolVersion)
        .add("description", this.description)
        .add("interpreters", this.interpreters)
        .add("dockerImage", this.dockerImage)
        .add("commandScript", this.cheetahScript).toString();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param document the DOM document to parse
   * @param toolSource the source of the tool
   * @throws EoulsanException if an error occurs while parsing the document
   */
  ToolInfo(final Document document, final String toolSource)
      throws EoulsanException {

    java.util.Objects.requireNonNull(document, "doc argument cannot be null");

    // Set tool name
    this.toolID = extractToolID(document);
    this.toolName = extractToolName(document);
    this.description = extractDescription(document);
    this.interpreters = extractInterpreters(document);
    this.dockerImage = emptyToNull(extractDockerImage(document));
    this.cheetahScript = emptyToNull(extractCheetahScript(document));

    final String toolVersion = nullToEmpty(extractToolVersion(document));
    this.toolVersion = "".equals(toolVersion) ? DEFAULT_VERSION : toolVersion;
    this.toolSource = toolSource == null ? "unknown source" : toolSource;

    if (this.toolName == null) {
      throw new EoulsanException("GalaxyTool name can not be null");
    }

    if (this.cheetahScript == null) {
      throw new EoulsanException("No command found in Galaxy tool");
    }

    // TODO check the tool id string
  }

}
