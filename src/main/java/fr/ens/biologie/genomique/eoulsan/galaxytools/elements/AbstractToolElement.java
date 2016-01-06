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

package fr.ens.biologie.genomique.eoulsan.galaxytools.elements;

import java.util.Map;

import org.w3c.dom.Element;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.workflow.FileNaming;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;

// TODO: Auto-generated Javadoc
/**
 * The Class AbstractToolElement.
 * @author Sandrine Perrin
 * @since 2.1
 */
public abstract class AbstractToolElement implements ToolElement {

  /** SPLITTER. */
  final static Splitter COMMA =
      Splitter.on(',').trimResults().omitEmptyStrings();

  /** Data from attribute param tag. */
  private final String shortName;

  /** The name space. */
  private final String nameSpace;

  /** The name. */
  private final String name;

  /** The type. */
  private final String type;

  /** The is optional. */
  private Boolean isOptional = null;

  /** The label. */
  private String label = "";

  /** The help. */
  private String help = "";

  /** The is setting. */
  protected boolean isSetting = false;

  // private String parameterEoulsan;

  // public boolean isParameterEoulsanValid(final String paramEoulsan) {
  // return true;
  // }

  /**
   * Checks if is value parameter valid.
   * @return true, if is value parameter valid
   */
  abstract boolean isValueParameterValid();

  @Override
  public void setValues(final Map<String, Parameter> stepParameters)
      throws EoulsanException {

    // Extract parameter
    Parameter parameter = extractParameterByName(stepParameters);

    setValue(parameter);
  }

  @Override
  public boolean isFile() {
    return false;
  }

  @Override
  public DataFormat getDataFormat() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Parameter extractParameterByName(
      final Map<String, Parameter> stepParameters) {

    // Use namespace
    Parameter p = stepParameters.get(getName());

    if (p == null) {
      // Without namespace
      p = stepParameters.get(getShortName());
    }

    // Return parameter founded or null
    return p;
  }

  //
  // Getter and setter
  //

  @Override
  public boolean isSetting() {
    return this.isSetting;
  }

  @Override
  abstract public String getValue();

  @Override
  public void setValue(final Parameter stepParameter) throws EoulsanException {
    // TODO
    if (stepParameter == null && !isSetting())
      throw new EoulsanException(
          "GalaxyTool parameter missing to set " + getName());

  }

  /**
   * Checks if is optional.
   * @return the boolean
   */
  public Boolean isOptional() {
    return this.isOptional;
  }

  /**
   * Gets the label.
   * @return the label
   */
  public String getLabel() {
    return this.label;
  }

  /**
   * Gets the help.
   * @return the help
   */
  public String getHelp() {
    return this.help;
  }

  /**
   * Gets the short name.
   * @return the short name
   */
  public String getShortName() {
    return this.shortName;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String getValidatedName() {
    return FileNaming.toValidName(this.name);
  }

  /**
   * Gets the type.
   * @return the type
   */
  public String getType() {
    return this.type;
  }

  @Override
  public String toString() {
    return "ParameterToolGalaxy [name="
        + this.shortName + ", type=" + this.type + ", isOptional="
        + this.isOptional + ", label=" + this.label + ", help=" + this.help
        + ", parameterEoulsan=" + getValue() + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.help == null) ? 0 : this.help.hashCode());
    result = prime * result
        + ((this.isOptional == null) ? 0 : this.isOptional.hashCode());
    result =
        prime * result + ((this.label == null) ? 0 : this.label.hashCode());
    result = prime * result
        + ((this.shortName == null) ? 0 : this.shortName.hashCode());
    result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final AbstractToolElement other = (AbstractToolElement) obj;
    if (this.help == null) {
      if (other.help != null) {
        return false;
      }
    } else if (!this.help.equals(other.help)) {
      return false;
    }
    if (this.isOptional == null) {
      if (other.isOptional != null) {
        return false;
      }
    } else if (!this.isOptional.equals(other.isOptional)) {
      return false;
    }
    if (this.label == null) {
      if (other.label != null) {
        return false;
      }
    } else if (!this.label.equals(other.label)) {
      return false;
    }
    if (this.shortName == null) {
      if (other.shortName != null) {
        return false;
      }
    } else if (!this.shortName.equals(other.shortName)) {
      return false;
    }
    if (this.type == null) {
      if (other.type != null) {
        return false;
      }
    } else if (!this.type.equals(other.type)) {
      return false;
    }
    return true;
  }

  //
  /**
   * Gets the instance tool element.
   * @param param the param
   * @return the instance tool element
   * @throws EoulsanException the eoulsan exception
   */
  public static ToolElement getInstanceToolElement(final Element param)
      throws EoulsanException {
    return getInstanceToolElement(param, null);
  }

  /**
   * Gets the instance tool element.
   * @param param the param
   * @param nameSpace the name space
   * @return the instance tool element
   * @throws EoulsanException the eoulsan exception
   */
  public static ToolElement getInstanceToolElement(final Element param,
      final String nameSpace) throws EoulsanException {

    if (param == null) {
      throw new EoulsanException(
          "Parsing xml: no element param found to instantiate a tool element.");
    }

    // Instantiate a tool parameter according to attribute type value
    final String type =
        param.getAttribute("type").toLowerCase(Globals.DEFAULT_LOCALE);

    final String paramName = param.getTagName();
    if (paramName.equals(ToolOutputsData.TAG_NAME)) {
      return new ToolOutputsData(param, nameSpace);
    }

    ToolElement toolElement = null;

    switch (type) {

    case ToolElementBoolean.TYPE:
      toolElement = new ToolElementBoolean(param, nameSpace);
      break;
    case ToolElementInteger.TYPE:
      toolElement = new ToolElementInteger(param, nameSpace);
      break;
    case ToolElementFloat.TYPE:
      toolElement = new ToolElementFloat(param, nameSpace);
      break;
    case ToolElementSelect.TYPE:
      toolElement = new ToolElementSelect(param, nameSpace);
      break;

    default:
      toolElement = new ToolElementData(param, nameSpace);
      break;
    }

    return toolElement;
  }

  //
  // Constructor
  //
  /**
   * Instantiates a new abstract tool element.
   * @param param the param
   */
  public AbstractToolElement(final Element param) {
    this(param, null);
  }

  /**
   * Instantiates a new abstract tool element.
   * @param param the param
   * @param nameSpace the name space
   */
  public AbstractToolElement(final Element param, final String nameSpace) {

    this.shortName = param.getAttribute("name");
    this.nameSpace = nameSpace;

    // If exists add prefix from parent element
    if (nameSpace == null || nameSpace.isEmpty()) {
      this.name = this.shortName;
    } else {
      this.name = nameSpace + SEP + this.shortName;

    }

    this.type = param.getAttribute("type");

    final String optional = param.getAttribute("optional");
    this.isOptional = optional.isEmpty() ? null : Boolean.getBoolean(optional);

    this.label = param.getAttribute("label");
    this.help = param.getAttribute("help");
  }

}
