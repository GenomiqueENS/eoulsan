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

package fr.ens.biologie.genomique.eoulsan.galaxytools.elements;

import org.w3c.dom.Element;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.core.Naming;
import com.google.common.base.Objects;

/**
 * This class define an abstract tool element.
 * @author Sandrine Perrin
 * @since 2.0
 */
public abstract class AbstractToolElement implements ToolElement {

  /** SPLITTER. */
  protected final static Splitter COMMA =
      Splitter.on(',').trimResults().omitEmptyStrings();

  /** Data from attribute param tag. */
  private final String shortName;

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

  //
  // Getter and setter
  //

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
    return Naming.toValidName(this.name);
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

    return Objects.hashCode(this.help, this.isOptional, this.label,
        this.shortName, this.type);
  }

  @Override
  public boolean equals(final Object obj) {

    if (this == obj) {
      return true;
    }

    if (obj == null || !(obj instanceof AbstractToolElement)) {
      return false;
    }

    final AbstractToolElement that = (AbstractToolElement) obj;

    if (!Objects.equal(this.help, that.help)) {
      return false;
    }

    if (!Objects.equal(this.isOptional, that.isOptional)) {
      return false;
    }

    if (!Objects.equal(this.label, that.label)) {
      return false;
    }

    if (!Objects.equal(this.shortName, that.shortName)) {
      return false;
    }

    if (!Objects.equal(this.type, that.type)) {
      return false;
    }
    return true;
  }

  //
  // Constructor
  //
  /**
   * Instantiates a new abstract tool element.
   * @param param the parameter
   */
  public AbstractToolElement(final Element param) {
    this(param, null);
  }

  /**
   * Instantiates a new abstract tool element.
   * @param param the parameter
   * @param nameSpace the name space
   */
  public AbstractToolElement(final Element param, final String nameSpace) {

    this.shortName = param.getAttribute("name");

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
