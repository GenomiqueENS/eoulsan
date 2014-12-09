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
package fr.ens.transcriptome.eoulsan.galaxytool.element;

import java.util.List;

import org.python.google.common.collect.Lists;
import org.w3c.dom.Element;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.Parameter;

// TODO: Auto-generated Javadoc
/**
 * The Class ToolParameterBoolean.
 * @author Sandrine Perrin
 * @since 2.4
 */
public class ToolElementBoolean extends AbstractToolElement {

  /** The Constant TYPE. */
  public final static String TYPE = "boolean";

  /** The Constant ATT_CHECKED_KEY. */
  private final static String ATT_CHECKED_KEY = "checked";

  /** The Constant ATT_TRUEVALUE_KEY. */
  private final static String ATT_TRUEVALUE_KEY = "truevalue";

  /** The Constant ATT_FALSEVALUE_KEY. */
  private final static String ATT_FALSEVALUE_KEY = "falsevalue";

  /** The Constant CHECKED_VALUES. */
  private final static List<String> CHECKED_VALUES = Lists.newArrayList("yes",
      "on", "true");

  /** The checked_lowered. */
  private final String checked_lowered;

  /** The true value. */
  private final String trueValue;

  /** The false value. */
  private final String falseValue;

  /** The value. */
  private String value = "";

  @Override
  public boolean isValueParameterValid() {
    return true;
  }

  @Override
  public String getValue() {
    return this.value;
  }

  @Override
  public void setValue() {
    // Set value to the default value
    if (this.value.isEmpty()) {
      this.value = this.trueValue;
    }

    this.isSetting = true;
  }

  @Override
  public void setValue(final Parameter stepParameter) {

    final boolean valueParameter = stepParameter.getBooleanValue();
    this.value = valueParameter ? this.trueValue : this.falseValue;

    this.isSetting = true;

  }

  @Override
  public String toString() {
    return "ToolParameterBoolean [checked="
        + this.checked_lowered + ", trueValue=" + this.trueValue
        + ", falseValue=" + this.falseValue + ", value=" + this.value + "]";
  }

  //
  // Constructors
  //
  
  /**
   * Instantiates a new tool parameter boolean.
   * @param param the param
   */
  public ToolElementBoolean(final Element param) {
    this(param, null);
  }

  /**
   * Instantiates a new tool parameter boolean.
   * @param param the param
   * @param nameSpace the name space
   */
  public ToolElementBoolean(final Element param, final String nameSpace) {
    super(param, nameSpace);

    this.checked_lowered =
        param.getAttribute(ATT_CHECKED_KEY).toLowerCase(Globals.DEFAULT_LOCALE);

    this.trueValue = param.getAttribute(ATT_TRUEVALUE_KEY);

    this.falseValue = param.getAttribute(ATT_FALSEVALUE_KEY);

    // Set default if define
    if (CHECKED_VALUES.contains(this.checked_lowered)) {
      this.value = this.trueValue;
    }

  }
}
