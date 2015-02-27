package fr.ens.transcriptome.eoulsan.steps.galaxytool;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;

import fr.ens.transcriptome.eoulsan.EoulsanException;

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

  public File getToolExecutable() {
    return toolExecutable;
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
