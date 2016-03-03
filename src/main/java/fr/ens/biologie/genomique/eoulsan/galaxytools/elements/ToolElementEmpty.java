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

import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;

// TODO: Auto-generated Javadoc
/**
 * The Class ToolParameterEmpty.
 * @author Sandrine Perrin
 * @since 2.1
 */
public class ToolElementEmpty implements ToolElement {

  private final String shortName;
  private final String name;

  /**
   * Instantiates a new tool parameter empty.
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
    return getName();
  }

  @Override
  public boolean isSetting() {
    return false;
  }

  @Override
  public String getValue() {
    return "No Value";
  }

  @Override
  public void setValue() {
  }

  @Override
  public void setValue(final Parameter stepParameter) {
  }

  @Override
  public void setValues(final Map<String, Parameter> stepParameters) {

  }

  @Override
  public void setValue(final String value) throws EoulsanException {

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
      Map<String, Parameter> stepParameters) {

    final Parameter p = stepParameters.get(getName());

    if (p == null)
      return stepParameters.get(getShortName());

    return p;
  }

  //
  // Constructors
  //

  public ToolElementEmpty() {
    this("noName");
  }

  public ToolElementEmpty(final String nameToolElement) {
    this(nameToolElement, null);
  }

  public ToolElementEmpty(final String nameToolElement,
      final String nameSpace) {
    this.shortName = nameToolElement;

    // Add name space for full name, if exists
    this.name = (nameSpace == null ? "" : nameSpace + ".") + nameToolElement;

  }
}
