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

package fr.ens.transcriptome.eoulsan.toolgalaxy.parameter;

import java.util.List;

import org.w3c.dom.Element;

import com.google.common.base.Splitter;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;

public abstract class AbstractToolParameter implements ToolElement {

  /** SPLITTER */
  final static Splitter COMMA = Splitter.on(',').trimResults()
      .omitEmptyStrings();

  /** Data from attribute param tag */
  private final String shortName;
  private final String name;

  private final String type;
  private final List<String> formats;

  private Boolean isOptional = null;
  private String label = "";
  private String help = "";

  protected boolean isSetting = false;

  // private String parameterEoulsan;

  // public boolean isParameterEoulsanValid(final String paramEoulsan) {
  // return true;
  // }

  public Object castFormat() {
    return null;
  }

  abstract boolean isValueParameterValid();

  //
  // Getter and setter
  //

  @Override
  public boolean isSetting() {
    return isSetting;
  }

  @Override
  abstract public String getValue();

  abstract public boolean setParameterEoulsan(final String paramValue);

  public Boolean isOptional() {
    return isOptional;
  }

  public String getLabel() {
    return label;
  }

  public String getHelp() {
    return help;
  }

  public String getShortName() {
    return shortName;
  }

  public String getName() {
    return this.name;
  }

  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    return "ParameterToolGalaxy [name="
        + shortName + ", type=" + type + ", isOptional=" + isOptional
        + ", label=" + label + ", help=" + help + ", parameterEoulsan="
        + getValue() + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((help == null) ? 0 : help.hashCode());
    result =
        prime * result + ((isOptional == null) ? 0 : isOptional.hashCode());
    result = prime * result + ((label == null) ? 0 : label.hashCode());
    result = prime * result + ((shortName == null) ? 0 : shortName.hashCode());
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
    AbstractToolParameter other = (AbstractToolParameter) obj;
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
    if (shortName == null) {
      if (other.shortName != null)
        return false;
    } else if (!shortName.equals(other.shortName))
      return false;
    if (type == null) {
      if (other.type != null)
        return false;
    } else if (!type.equals(other.type))
      return false;
    return true;
  }

  //
  public static ToolElement getInstanceToolParameter(final Element param)
      throws EoulsanException {
    return getInstanceToolParameter(param, null);
  }

  public static ToolElement getInstanceToolParameter(final Element param,
      final String prefixName) throws EoulsanException {

    if (param == null)
      throw new EoulsanException(
          "Parsing xml: no element param found to intantiate a tool element.");

    // Intantiate a tool parameter according to attribute type value
    final String type =
        param.getAttribute("type").toLowerCase(Globals.DEFAULT_LOCALE);

    ToolElement toolElement = null;

    switch (type) {

    case ToolParameterBoolean.TYPE:
      toolElement = new ToolParameterBoolean(param, prefixName);
      break;
    case ToolParameterInteger.TYPE:
      toolElement = new ToolParameterInteger(param, prefixName);
      break;
    case ToolParameterFloat.TYPE:
      toolElement = new ToolParameterFloat(param, prefixName);
      break;
    case ToolParameterSelect.TYPE:
      toolElement = new ToolParameterSelect(param, prefixName);
      break;

    default:
      toolElement = new ToolParameter(param, prefixName);
      break;

    }

    return toolElement;
  }

  //
  // Constructor
  //
  public AbstractToolParameter(final Element param) {
    this(param, null);
  }

  public AbstractToolParameter(final Element param, final String prefixName) {

    this.shortName = param.getAttribute("name");

    // If exists add prefix from parent element
    if (prefixName == null || prefixName.isEmpty()) {
      this.name = this.shortName;
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
