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

import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.newEoulsanException;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.galaxytools.ToolInfo;
import org.w3c.dom.Element;

/**
 * This class define a float tool element parameter.
 *
 * @author Sandrine Perrin
 * @since 2.0
 */
public class FloatParameterToolElement extends TextParameterToolElement {

  /** The Constant TYPE. */
  public static final String TYPE = "float";

  /** The Constant ATT_MIN_KEY. */
  private static final String ATT_MIN_KEY = "min";

  /** The Constant ATT_MAX_KEY. */
  private static final String ATT_MAX_KEY = "max";

  /** The min. */
  private final double min;

  /** The max. */
  private final double max;

  private final ToolInfo toolInfo;

  //
  // Getters
  //

  @Override
  public boolean isParameterValueValid() {

    double v;

    try {
      v = Double.parseDouble(getValue());
    } catch (NumberFormatException e) {
      return false;
    }
    return v >= this.min && v <= this.max;
  }

  //
  // Setters
  //

  @Override
  public void setValue(final String value) throws EoulsanException {

    super.setValue(value);

    if (!this.isParameterValueValid()) {
      throw newEoulsanException(
          this.toolInfo,
          getName(),
          "invalid value for parameter: "
              + getValue()
              + " (the value must be in interval ["
              + this.min
              + " - "
              + this.max
              + "])");
    }
  }

  //
  // Object methods
  //

  @Override
  public String toString() {
    return "ToolParameterFloat [min="
        + this.min
        + ", max="
        + this.max
        + ", value="
        + getValue()
        + "]";
  }

  //
  // Constructors
  //

  /**
   * Instantiates a new float tool parameter.
   *
   * @param toolInfo the ToolInfo object
   * @param param the param
   * @throws EoulsanException the eoulsan exception
   */
  public FloatParameterToolElement(final ToolInfo toolInfo, final Element param)
      throws EoulsanException {
    this(toolInfo, param, null);
  }

  /**
   * Instantiates a new float tool parameter.
   *
   * @param toolInfo the ToolInfo object
   * @param param the param
   * @param nameSpace the name space
   * @throws EoulsanException the eoulsan exception
   */
  public FloatParameterToolElement(
      final ToolInfo toolInfo, final Element param, final String nameSpace)
      throws EoulsanException {

    super(toolInfo, param, nameSpace);
    this.toolInfo = toolInfo;

    // Get minimal value
    String value = param.getAttribute(ATT_MIN_KEY);
    try {
      this.min = value.isEmpty() ? Double.MIN_VALUE : Double.parseDouble(value);
    } catch (final NumberFormatException e) {
      throw newEoulsanException(toolInfo, getName(), "Failed to extract min value: " + value);
    }

    // Get maximal
    value = param.getAttribute(ATT_MAX_KEY);
    try {
      this.max = value.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(value);
    } catch (NumberFormatException e) {
      throw newEoulsanException(toolInfo, getName(), "Failed to extract max value: " + value);
    }
  }
}
