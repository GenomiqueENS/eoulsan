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
import static fr.ens.transcriptome.eoulsan.util.galaxytool.GalaxyToolXMLParser.extractCommand;
import static fr.ens.transcriptome.eoulsan.util.galaxytool.GalaxyToolXMLParser.extractDescription;
import static fr.ens.transcriptome.eoulsan.util.galaxytool.GalaxyToolXMLParser.extractInterpreter;
import static fr.ens.transcriptome.eoulsan.util.galaxytool.GalaxyToolXMLParser.extractToolID;
import static fr.ens.transcriptome.eoulsan.util.galaxytool.GalaxyToolXMLParser.extractToolName;
import static fr.ens.transcriptome.eoulsan.util.galaxytool.GalaxyToolXMLParser.extractToolVersion;
import static org.python.google.common.base.Strings.emptyToNull;
import static org.python.google.common.base.Strings.nullToEmpty;

import org.w3c.dom.Document;

import com.google.common.base.Objects;

import fr.ens.transcriptome.eoulsan.EoulsanException;

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

  private final String cmdTagContent;

  //
  // Getters and Setters
  //

  /**
   * Get the tool Id.
   * @return the tool id
   */
  public String getToolID() {
    return toolID;
  }

  /**
   * Get the tool name.
   * @return the tool name
   */
  public String getToolName() {
    return toolName;
  }

  /**
   * Get the tool version.
   * @return the tool version
   */
  public String getToolVersion() {
    return toolVersion;
  }

  /**
   * Get the tool description.
   * @return the tool description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get the interpreter.
   * @return the interpreter
   */
  public String getInterpreter() {
    return interpreter;
  }

  /**
   * Get the content of the tag command.
   * @return the content of the tag command
   */
  public String getCmdTagContent() {
    return cmdTagContent;
  }

  //
  // Other methods
  //

  public boolean isInterpreterSetting() {

    return getInterpreter() != null && !getInterpreter().isEmpty();
  }

  //
  // Object method
  //

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("toolID", toolID)
        .add("toolName", toolName).add("toolVersion", toolVersion)
        .add("description", description).add("interpreter", interpreter)
        .add("cmdTagContent", cmdTagContent).toString();
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
    this.cmdTagContent = emptyToNull(extractCommand(document));

    final String toolVersion = nullToEmpty(extractToolVersion(document));
    this.toolVersion = "".equals(toolVersion) ? DEFAULT_VERSION : toolVersion;

    if (this.toolName == null) {
      throw new EoulsanException("GalaxyTool name can not be null");
    }

    if (this.cmdTagContent == null) {
      throw new EoulsanException("No command found in Galaxy tool");
    }

    // TODO check the tool id string

  }

}
