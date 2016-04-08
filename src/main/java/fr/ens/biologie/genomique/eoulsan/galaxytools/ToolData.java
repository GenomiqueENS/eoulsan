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

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.extractCommand;
import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.extractDescription;
import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.extractDockerImage;
import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.extractInterpreter;
import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.extractToolID;
import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.extractToolName;
import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.extractToolVersion;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.nullToEmpty;

import org.w3c.dom.Document;

import com.google.common.base.Objects;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;

/**
 * The class define a tool data which contains all data extracted from XML file.
 * @author Sandrine Perrin
 * @since 2.1
 */
public class ToolData {

  private static final String DEFAULT_VERSION = "unknown";

  /** The tool id. */
  private final String toolID;

  /** The tool name. */
  private final String toolName;

  /** The tool version. */
  private final String toolVersion;

  /** The description. */
  private final String description;

  /** The interpreter. */
  private final String interpreter;

  /** The command script. */
  private final String commandScript;

  /** The Docker image. */
  private final String dockerImage;

  //
  // Getters
  //

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
  public String getInterpreter() {
    return this.interpreter;
  }

  /**
   * Get the command script.
   * @return the command script
   */
  public String getCommandScript() {
    return this.commandScript;
  }

  /**
   * Get Docker image.
   * @return the docker image
   */
  public String getDockerImage() {
    return this.dockerImage;
  }

  //
  // Object method
  //

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("toolID", this.toolID)
        .add("toolName", this.toolName).add("toolVersion", this.toolVersion)
        .add("description", this.description)
        .add("interpreter", this.interpreter)
        .add("dockerImage", this.dockerImage)
        .add("commandScript", this.commandScript).toString();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param document the DOM document to parse
   * @throws EoulsanException if an error occurs while parsing the document
   */
  ToolData(final Document document) throws EoulsanException {

    checkNotNull(document, "doc argument cannot be null");

    // Set tool name
    this.toolID = extractToolID(document);
    this.toolName = extractToolName(document);
    this.description = extractDescription(document);
    this.interpreter = extractInterpreter(document);
    this.dockerImage = emptyToNull(extractDockerImage(document));
    this.commandScript = emptyToNull(extractCommand(document));

    final String toolVersion = nullToEmpty(extractToolVersion(document));
    this.toolVersion = "".equals(toolVersion) ? DEFAULT_VERSION : toolVersion;

    if (this.toolName == null) {
      throw new EoulsanException("GalaxyTool name can not be null");
    }

    if (this.commandScript == null) {
      throw new EoulsanException("No command found in Galaxy tool");
    }

    // TODO check the tool id string

  }

}
