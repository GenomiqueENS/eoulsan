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

import org.w3c.dom.Element;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Parameter;

// TODO: Auto-generated Javadoc
/**
 * The Class ToolParameterFloat.
 * @author Sandrine Perrin
 * @since 2.4
 */
public class ToolParameterFloat extends AbstractToolElement {

  /** The Constant TYPE. */
  public final static String TYPE = "float";

  /** The Constant ATT_DEFAULT_KEY. */
  private final static String ATT_DEFAULT_KEY = "value";
  
  /** The Constant ATT_MIN_KEY. */
  private final static String ATT_MIN_KEY = "min";
  
  /** The Constant ATT_MAX_KEY. */
  private final static String ATT_MAX_KEY = "max";

  /** The min. */
  private final double min;
  
  /** The max. */
  private final double max;

  /** The value. */
  private double value;

  @Override
  public void setParameterEoulsan() {
  }

  @Override
  public boolean isValueParameterValid() {
    return this.value >= this.min && this.value <= this.max;
  }

  @Override
  public void setParameterEoulsan(final Parameter stepParameter)
      throws EoulsanException {

    this.value = stepParameter.getDoubleValue();
    this.isSetting = true;

    if (!isValueParameterValid()) {
      throw new EoulsanException("ToolGalaxy step: parameter "
          + getName() + " value setting for step: " + this.value
          + ". Invalid to interval [" + this.min + "," + this.max + "]");
    }
  }

  @Override
  public String getValue() {
    return "" + this.value;
  }

  @Override
  public String toString() {
    return "ToolParameterFloat [min="
        + this.min + ", max=" + this.max + ", value=" + this.value + "]";
  }

  //
  // Constructor
  //
  /**
   * Instantiates a new tool parameter float.
   * @param param the param
   * @throws EoulsanException the eoulsan exception
   */
  public ToolParameterFloat(final Element param) throws EoulsanException {
    this(param, null);
  }

  /**
   * Instantiates a new tool parameter float.
   * @param param the param
   * @param nameSpace the name space
   * @throws EoulsanException the eoulsan exception
   */
  public ToolParameterFloat(final Element param, final String nameSpace)
      throws EoulsanException {
    super(param, nameSpace);

    try {
      final double defaultValue =
          Double.parseDouble(param.getAttribute(ATT_DEFAULT_KEY));

      this.value = defaultValue;

    } catch (final NumberFormatException e) {
      throw new EoulsanException("No found default value for parameter "
          + getName());
    }

    try {
      String value = param.getAttribute(ATT_MIN_KEY);
      this.min = value.isEmpty() ? Double.MIN_VALUE : Double.parseDouble(value);

      value = param.getAttribute(ATT_MAX_KEY);
      this.max = value.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(value);

    } catch (final NumberFormatException e) {
      throw new EoulsanException("Fail extract value " + e.getMessage());
    }
  }

}
