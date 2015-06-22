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

import java.io.File;

import fr.ens.transcriptome.eoulsan.EoulsanException;

/**
 * The class define a tool data which contains all data extracted from XML file.
 * @author Sandrine Perrin
 * @since 2.1
 */
public class ToolData {

  private static final String GENERIC_NAME = "toolGalaxy";
  private static final String DEFAULT_VERSION = "unknown";

  /** The tool id. */
  private String toolID;

  /** The tool name. */
  private String toolName;

  /** The tool version. */
  private String toolVersion;

  /** The description. */
  private String description;

  /** The interpreter. */
  private String interpreter;

  private String cmdTagContent;

  /** The tool executable. */
  private File toolExecutable;

  public boolean isIntepreterSetting() {

    return getInterpreter() != null && !getInterpreter().isEmpty();
  }

  //
  // Getters and Setters
  //

  @Override
  public String toString() {
    return "ToolData [toolID="
        + toolID + ", toolName=" + toolName + ", toolVersion=" + toolVersion
        + ", description=" + description + ", interpreter=" + interpreter
        + ", cmdTagContent=" + cmdTagContent + "]";
  }

  public String getToolID() {
    return toolID;
  }

  public void setToolID(final String toolID) {

    this.toolID = toolID;
  }

  public String getToolName() {

    return toolName;
  }

  public void setToolName(final String toolName) {

    checkNotNull(toolName, "GalaxyTool name can not be null.");

    this.toolName = toolName;
  }

  public String getToolVersion() {
    return toolVersion;
  }

  public void setToolVersion(final String toolVersion) {

    if (toolVersion == null || toolVersion.isEmpty()) {
      this.toolVersion = DEFAULT_VERSION;
    } else {
      this.toolVersion = toolVersion;
    }
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getInterpreter() {
    return interpreter;
  }

  public void setInterpreter(final String interpreter) {
    this.interpreter = interpreter;
  }

  public String getCmdTagContent() {

    return cmdTagContent;
  }

  public String getToolExecutable() {
    return toolExecutable.getAbsolutePath();
  }

  public void setCmdTagContent(final String cmdTagContent)
      throws EoulsanException {

    checkNotNull(cmdTagContent, "GalaxyTool command tag can not be null.");

    if (cmdTagContent.isEmpty()) {
      throw new EoulsanException("Parsing tool XML file: no command found.");
    }

    this.cmdTagContent = cmdTagContent;
  }

  //
  // Constructor
  //

  ToolData() {

    this(null);
  }

  ToolData(final File toolExecutable) {
    this.toolExecutable = toolExecutable;
  }

}
