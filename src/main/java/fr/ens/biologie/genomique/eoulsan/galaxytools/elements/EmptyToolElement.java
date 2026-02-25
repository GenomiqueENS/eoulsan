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

/**
 * This class define an empty tool element.
 *
 * @author Sandrine Perrin
 * @since 2.0
 */
public class EmptyToolElement implements ToolElement {

  private final String shortName;
  private final String name;

  @Override
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
  public String getValue() {
    return "No Value";
  }

  @Override
  public void setValue(final String value) {}

  //
  // Constructors
  //

  /** Public constructor. */
  public EmptyToolElement() {
    this("noName");
  }

  /**
   * Public constructor.
   *
   * @param toolElementName name of the tool element
   */
  public EmptyToolElement(final String toolElementName) {
    this(toolElementName, null);
  }

  /**
   * Public constructor.
   *
   * @param toolElementName name of the tool element
   * @param nameSpace name space
   */
  public EmptyToolElement(final String toolElementName, final String nameSpace) {
    this.shortName = toolElementName;

    // Add name space for full name, if exists
    this.name = (nameSpace == null ? "" : nameSpace + ".") + toolElementName;
  }
}
