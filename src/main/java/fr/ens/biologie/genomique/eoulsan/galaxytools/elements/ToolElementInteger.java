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

import org.w3c.dom.Element;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;

// TODO: Auto-generated Javadoc
/**
 * The Class ToolParameterInteger.
 * @author Sandrine Perrin
 * @since 2.1
 */
public class ToolElementInteger extends AbstractToolElement {

  /** The Constant TYPE. */
  public final static String TYPE = "integer";

  /** The Constant ATT_DEFAULT_KEY. */
  private final static String ATT_DEFAULT_KEY = "value";

  /** The Constant ATT_MIN_KEY. */
  private final static String ATT_MIN_KEY = "min";

  /** The Constant ATT_MAX_KEY. */
  private final static String ATT_MAX_KEY = "max";

  /** The min. */
  private final int min;

  /** The max. */
  private final int max;

  /** The value. */
  private int value;

  @Override
  public boolean isValueParameterValid() {
    return this.value >= this.min && this.value <= this.max;
  }

  @Override
  public void setValue() {
  }

  @Override
  public void setValue(final Parameter stepParameter) throws EoulsanException {
    super.setValue(stepParameter);

    this.setValue(stepParameter.getStringValue());

  }

  @Override
  public void setValue(final String value) throws EoulsanException {
    this.value = Integer.parseInt(value);

    this.isSetting = true;

    if (!this.isValueParameterValid()) {
      throw new EoulsanException("ToolGalaxy step: parameter "
          + this.getName() + " value setting for step: " + this.value
          + ". Invalid to interval [" + this.min + "," + this.max + "]");
    }
  }

  @Override
  public String getValue() {
    return "" + this.value;
  }

  @Override
  public String toString() {
    return "ToolParameterInteger [min="
        + this.min + ", max=" + this.max + ", value=" + this.value + "]";
  }

  //
  // Constructors
  //

  /**
   * Instantiates a new tool parameter integer.
   * @param param the param
   * @throws EoulsanException the eoulsan exception
   */
  public ToolElementInteger(final Element param) throws EoulsanException {
    this(param, null);
  }

  /**
   * Instantiates a new tool parameter integer.
   * @param param the param
   * @param nameSpace the name space
   * @throws EoulsanException the eoulsan exception
   */
  public ToolElementInteger(final Element param, final String nameSpace)
      throws EoulsanException {
    super(param, nameSpace);

    try {
      final int defaultValue =
          Integer.parseInt(param.getAttribute(ATT_DEFAULT_KEY));

      // Set value
      this.value = defaultValue;

    } catch (final NumberFormatException e) {
      throw new EoulsanException(
          "No found default value for parameter " + this.getName(), e);
    }

    try {
      String value = param.getAttribute(ATT_MIN_KEY);
      this.min = value.isEmpty() ? Integer.MIN_VALUE : Integer.parseInt(value);

      value = param.getAttribute(ATT_MAX_KEY);
      this.max = value.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(value);

    } catch (final NumberFormatException e) {
      throw new EoulsanException("Fail extract value " + e.getMessage());
    }
  }

}
