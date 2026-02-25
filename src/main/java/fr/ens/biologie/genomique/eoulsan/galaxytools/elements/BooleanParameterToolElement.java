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

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.galaxytools.ToolInfo;
import org.w3c.dom.Element;

/**
 * This class define a boolean tool element parameter.
 *
 * @author Sandrine Perrin
 * @since 2.0
 */
public class BooleanParameterToolElement extends AbstractParameterToolElement {

  /** The Constant TYPE. */
  public static final String TYPE = "boolean";

  /** The Constant ATT_CHECKED_KEY. */
  private static final String ATT_CHECKED_KEY = "checked";

  /** The Constant ATT_TRUEVALUE_KEY. */
  private static final String ATT_TRUEVALUE_KEY = "truevalue";

  /** The Constant ATT_FALSEVALUE_KEY. */
  private static final String ATT_FALSEVALUE_KEY = "falsevalue";

  /** The checked_lowered. */
  private final boolean checked;

  /** The true value. */
  private final String trueValue;

  /** The false value. */
  private final String falseValue;

  /** The value. */
  private boolean value;

  private boolean set;

  //
  // Getters
  //

  @Override
  public boolean isParameterValueValid() {
    return true;
  }

  @Override
  public String getValue() {
    return this.value ? this.trueValue : this.falseValue;
  }

  @Override
  public boolean isSet() {
    return this.set;
  }

  //
  // Setters
  //

  @Override
  public void setValue(final String value) throws EoulsanException {

    if (value == null) {
      this.value = false;
      return;
    }

    switch (value.trim().toLowerCase(Globals.DEFAULT_LOCALE)) {
      case "yes":
      case "on":
      case "true":
        this.value = true;
        break;

      default:
        this.value = false;
        break;
    }

    this.set = true;
  }

  //
  // Object methods
  //

  @Override
  public String toString() {
    return "ToolParameterBoolean [checked="
        + this.checked
        + ", trueValue="
        + this.trueValue
        + ", falseValue="
        + this.falseValue
        + ", value="
        + this.value
        + "]";
  }

  //
  // Constructors
  //

  /**
   * Instantiates a new boolean tool parameter.
   *
   * @param toolInfo the ToolInfo object
   * @param param the parameter
   */
  public BooleanParameterToolElement(final ToolInfo toolInfo, final Element param) {
    this(toolInfo, param, null);
  }

  /**
   * Instantiates a new boolean tool parameter.
   *
   * @param toolInfo the ToolInfo object
   * @param param the parameter
   * @param nameSpace the name space
   */
  public BooleanParameterToolElement(
      final ToolInfo toolInfo, final Element param, final String nameSpace) {
    super(param, nameSpace);

    this.checked = Boolean.parseBoolean(param.getAttribute(ATT_CHECKED_KEY));

    String trueValue = param.getAttribute(ATT_TRUEVALUE_KEY);
    this.trueValue = trueValue == null ? "" : trueValue;

    String falseValue = param.getAttribute(ATT_FALSEVALUE_KEY);
    this.falseValue = falseValue == null ? "" : falseValue;

    // Set default value
    this.value = this.checked;
  }
}
