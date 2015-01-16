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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.python.google.common.base.Joiner;
import org.w3c.dom.Element;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.util.galaxytool.GalaxyToolXMLParser;

// TODO: Auto-generated Javadoc
/**
 * The Class ToolParameterSelect.
 * @author Sandrine Perrin
 * @since 2.1
 */
public class ToolElementSelect extends AbstractToolElement {

  /** The Constant TYPE. */
  public final static String TYPE = "select";

  /** The Constant ATT_SELECTED_KEY. */
  private static final String ATT_SELECTED_KEY = "selected";

  /** The Constant ATT_VALUE_KEY. */
  private static final String ATT_VALUE_KEY = "value";

  /** The options value. */
  private final List<String> optionsValue;

  /** The options element. */
  private final List<Element> optionsElement;

  /** The value. */
  private String value = "";

  @Override
  boolean isValueParameterValid() {
    // Check value contains in options values
    return this.optionsValue.contains(this.value);
  }

  @Override
  public void setValue() {
  }

  @Override
  public void setValue(final Parameter stepParameter) throws EoulsanException {

    this.setValue(stepParameter.getStringValue());
  }

  @Override
  public void setValue(final String value) throws EoulsanException {
    this.isSetting = true;

    this.value = value;

    if (!this.isValueParameterValid()) {
      throw new EoulsanException("ToolGalaxy step: parameter "
          + this.getName() + " value setting : " + this.value
          + " is invalid. \n\tAvailable values: "
          + Joiner.on(",").join(this.optionsValue));
    }
  }

  /**
   * Extract all options.
   * @return the list
   * @throws EoulsanException the eoulsan exception
   */
  private List<String> extractAllOptions() throws EoulsanException {

    final List<String> options = new ArrayList<>();

    for (final Element e : this.optionsElement) {
      options.add(e.getAttribute(ATT_VALUE_KEY));

      // Check default settings
      final String attributeSelected = e.getAttribute(ATT_SELECTED_KEY);
      if (!attributeSelected.isEmpty()) {
        this.value = e.getAttribute(ATT_VALUE_KEY);
        this.isSetting = true;
      }
    }

    if (options.isEmpty()) {
      // throw new EoulsanException(
      // "Parsing tool xml: no option found in conditional element: "
      // + getName());
      return Collections.emptyList();
    }

    return Collections.unmodifiableList(options);
  }

  @Override
  public String toString() {
    return "ToolParameterSelect [" + super.toString() + "]";
  }

  @Override
  public String getValue() {
    return this.value;
  }

  //
  // Constructors
  //

  /**
   * Instantiates a new tool parameter select.
   * @param param the param
   * @throws EoulsanException the eoulsan exception
   */
  public ToolElementSelect(final Element param) throws EoulsanException {
    this(param, null);
  }

  /**
   * Instantiates a new tool parameter select.
   * @param param the param
   * @param nameSpace the name space
   * @throws EoulsanException the eoulsan exception
   */
  public ToolElementSelect(final Element param, final String nameSpace)
      throws EoulsanException {
    super(param, nameSpace);

    this.optionsElement =
        GalaxyToolXMLParser.extractChildElementsByTagName(param, "option");
    this.optionsValue = this.extractAllOptions();

  }

}
