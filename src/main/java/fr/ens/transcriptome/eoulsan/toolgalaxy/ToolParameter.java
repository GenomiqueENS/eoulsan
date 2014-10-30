package fr.ens.transcriptome.eoulsan.toolgalaxy;

import java.util.List;

import org.w3c.dom.Element;

import com.google.common.base.Splitter;

public class ToolParameter implements ToolElement {

  /** SPLITTER */
  final static Splitter COMMA = Splitter.on(',').trimResults()
      .omitEmptyStrings();

  /** Data from attribute param tag */
  private final String name;

  private final String type;
  private final List<String> formats;

  private Boolean isOptional = null;
  private String label = "";
  private String help = "";

  private String parameterEoulsan;

  public boolean isParameterEoulsanValid(final String paramEoulsan) {
    return true;
  }

  public String getValue() {
    // TODO Auto-generated method stub
    return null;
  }

  public Object castFormat() {
    return null;
  }

  //
  //
  //

  // TODO override equals and hashcode

  //
  // Getter and setter
  //

  @Override
  public boolean isSetting() {
    return !(this.parameterEoulsan == null || this.parameterEoulsan.length() == 0);
  }

  @Override
  public String getParameterEoulsan() {
    return this.parameterEoulsan;
  }

  @Override
  public void setParameterEoulsan(final String paramValue) {
    // TODO function type
    this.parameterEoulsan = paramValue;
  }

  public Boolean isOptional() {
    return isOptional;
  }

  public String getLabel() {
    return label;
  }

  public String getHelp() {
    return help;
  }

  @Override
  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    return "ParameterToolGalaxy [name="
        + name + ", type=" + type + ", isOptional=" + isOptional + ", label="
        + label + ", help=" + help + ", parameterEoulsan=" + parameterEoulsan
        + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((help == null) ? 0 : help.hashCode());
    result =
        prime * result + ((isOptional == null) ? 0 : isOptional.hashCode());
    result = prime * result + ((label == null) ? 0 : label.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ToolParameter other = (ToolParameter) obj;
    if (help == null) {
      if (other.help != null)
        return false;
    } else if (!help.equals(other.help))
      return false;
    if (isOptional == null) {
      if (other.isOptional != null)
        return false;
    } else if (!isOptional.equals(other.isOptional))
      return false;
    if (label == null) {
      if (other.label != null)
        return false;
    } else if (!label.equals(other.label))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (type == null) {
      if (other.type != null)
        return false;
    } else if (!type.equals(other.type))
      return false;
    return true;
  }

  //
  // Constructor
  //
  public ToolParameter(final Element param) {
    this(param, null);
  }

  public ToolParameter(final Element param, final String prefixName) {

    if (prefixName == null || prefixName.isEmpty()) {
      this.name = param.getAttribute("name");
    } else {
      this.name = prefixName + SEP + param.getAttribute("name");

    }

    this.type = param.getAttribute("type");

    this.formats = COMMA.splitToList(param.getAttribute("format"));

    String optional = param.getAttribute("optional");
    this.isOptional = optional.isEmpty() ? null : Boolean.getBoolean(optional);

    this.label = param.getAttribute("label");
    this.help = param.getAttribute("help");
  }

}
