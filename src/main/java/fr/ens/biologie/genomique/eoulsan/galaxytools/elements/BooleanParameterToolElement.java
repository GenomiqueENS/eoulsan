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

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;

/**
 * The Class ToolParameterBoolean.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class BooleanParameterToolElement extends AbstractToolElement {

  /** The Constant TYPE. */
  public final static String TYPE = "boolean";

  /** The Constant ATT_CHECKED_KEY. */
  private final static String ATT_CHECKED_KEY = "checked";

  /** The Constant ATT_TRUEVALUE_KEY. */
  private final static String ATT_TRUEVALUE_KEY = "truevalue";

  /** The Constant ATT_FALSEVALUE_KEY. */
  private final static String ATT_FALSEVALUE_KEY = "falsevalue";

  /** The checked_lowered. */
  private final String checked;

  /** The true value. */
  private final String trueValue;

  /** The false value. */
  private final String falseValue;

  /** The value. */
  private String value;

  @Override
  public boolean isValueParameterValid() {
    return true;
  }

  @Override
  public String getValue() {
    return this.value;
  }

  @Override
  public void setDefaultValue() throws EoulsanException {

    setValue(this.checked == null ? null : this.checked.toLowerCase());
  }

  @Override
  public void setValue(final Parameter stepParameter) throws EoulsanException {

    super.setValue(stepParameter);

    setValue(stepParameter.getLowerStringValue());
  }

  private void setValue(final String value) throws EoulsanException {

    switch (value) {

    case "yes":
    case "on":
    case "true":
      this.value = this.trueValue;
      break;

    default:
      this.value = this.falseValue;
      break;
    }

    this.set = true;
  }

  @Override
  public String toString() {
    return "ToolParameterBoolean [checked="
        + this.checked + ", trueValue=" + this.trueValue + ", falseValue="
        + this.falseValue + ", value=" + this.value + "]";
  }

  //
  // Constructors
  //

  /**
   * Instantiates a new tool parameter boolean.
   * @param param the param
   */
  public BooleanParameterToolElement(final Element param) {
    this(param, null);
  }

  /**
   * Instantiates a new tool parameter boolean.
   * @param param the param
   * @param nameSpace the name space
   */
  public BooleanParameterToolElement(final Element param, final String nameSpace) {
    super(param, nameSpace);

    this.checked = param.getAttribute(ATT_CHECKED_KEY);

    this.trueValue = param.getAttribute(ATT_TRUEVALUE_KEY);

    this.falseValue = param.getAttribute(ATT_FALSEVALUE_KEY);
  }

}
