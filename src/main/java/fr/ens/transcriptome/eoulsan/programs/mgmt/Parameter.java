/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.programs.mgmt;

import fr.ens.transcriptome.eoulsan.EoulsanException;

/**
 * This class define a parameter.
 * @author Laurent Jourdren
 */
public class Parameter {

  private String name;
  private String value;

  /**
   * Get the name of the parameter
   * @return Returns the name
   */
  public String getName() {
    return this.name;
  }

  /**
   * Get the value of the parameter
   * @return Returns the value
   */
  public String getValue() {
    return this.value;
  }

  /**
   * Get the value of the parameter as a String value
   * @return the value as a String
   */
  public String getStringValue() {

    return this.value;
  }

  /**
   * Get the value of the parameter as a integer value
   * @return the value as an integer
   */
  public int getIntValue() throws EoulsanException {

    try {

      return Integer.parseInt(this.value);
    } catch (NumberFormatException e) {

      throw new EoulsanException(
          "Invalid parameter, an integer parameter is need for "
              + this.name + " parameter: " + this.value);
    }

  }

  /**
   * Get the value of the parameter as a double value
   * @return the value as an integer
   */
  public double getDoubleValue() throws EoulsanException {

    try {

      return Double.parseDouble(this.value);
    } catch (NumberFormatException e) {

      throw new EoulsanException(
          "Invalid parameter, an integer parameter is need for "
              + this.name + " parameter: " + this.value);
    }

  }

  /**
   * Get the value of the parameter as a boolean value
   * @return the value as a boolean
   */
  public boolean getBooleanValue() {

    return Boolean.parseBoolean(this.value);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param name Name of the parameter
   * @param value value of the parameter
   */
  public Parameter(final String name, final String value) {

    if (name == null)
      throw new NullPointerException("Parameter name can't be null");

    if (value == null)
      throw new NullPointerException("Parameter value can't be null");

    if ("".equals(name))
      throw new IllegalArgumentException("Parameter name can't be empty");

    this.name = name;
    this.value = value;
  }

}
