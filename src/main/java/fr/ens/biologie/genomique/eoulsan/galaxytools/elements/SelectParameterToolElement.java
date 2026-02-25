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

import com.google.common.base.Joiner;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils;
import fr.ens.biologie.genomique.eoulsan.galaxytools.ToolInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.w3c.dom.Element;

/**
 * This class define a select tool element parameter.
 *
 * @author Sandrine Perrin
 * @since 2.0
 */
public class SelectParameterToolElement extends AbstractParameterToolElement {

  /** The Constant TYPE. */
  public static final String TYPE = "select";

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

  private boolean set;

  private final ToolInfo toolInfo;

  //
  // Getters
  //

  @Override
  public boolean isParameterValueValid() {
    // Check value contains in options values
    return this.optionsValue.contains(this.value);
  }

  @Override
  public String getValue() {
    return this.value;
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

    this.value = value;
    this.set = true;

    if (!this.isParameterValueValid()) {
      throw newEoulsanException(
          this.toolInfo,
          getName(),
          "The \""
              + this.value
              + "\" value is invalid. Available values are: "
              + Joiner.on(",").join(this.optionsValue));
    }
  }

  //
  // Private methods
  //

  /**
   * Extract all options.
   *
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
        this.set = true;
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

  //
  // Object methods
  //

  @Override
  public String toString() {
    return "ToolParameterSelect [" + super.toString() + "]";
  }

  //
  // Constructors
  //

  /**
   * Instantiates a new select tool element parameter.
   *
   * @param toolInfo the ToolInfo object
   * @param param the param
   * @throws EoulsanException the eoulsan exception
   */
  public SelectParameterToolElement(final ToolInfo toolInfo, final Element param)
      throws EoulsanException {
    this(toolInfo, param, null);
  }

  /**
   * Instantiates a new select tool element parameter.
   *
   * @param toolInfo the ToolInfo object
   * @param param the param
   * @param nameSpace the name space
   * @throws EoulsanException the eoulsan exception
   */
  public SelectParameterToolElement(
      final ToolInfo toolInfo, final Element param, final String nameSpace)
      throws EoulsanException {

    super(param, nameSpace);
    this.toolInfo = toolInfo;

    this.optionsElement = GalaxyToolXMLParserUtils.extractChildElementsByTagName(param, "option");
    this.optionsValue = this.extractAllOptions();
  }
}
