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

import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.extractChildElementsByTagName;
import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.newEoulsanException;
import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.setElementValue;
import static fr.ens.biologie.genomique.kenetre.util.XMLUtils.getElementsByTagName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntimeException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.galaxytools.ToolInfo;

/**
 * This class define a conditional tool element.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class ConditionalToolElement implements ToolElement {

  /** The Constant TYPE. */
  public final static String TYPE = "boolean";

  /** The name space. */
  private final String nameSpace;

  // Parameter represent choice in option list
  /** The tool parameter select. */
  private final ToolElement toolElementSelected;

  // Variable name in command tag and tool parameter related
  /** The actions related options. */
  private final Multimap<String, ToolElement> actionsRelatedOptions;

  /** The tool parameters selected. */
  private final Map<String, ToolElement> toolElementResult;

  /** The value. */
  private String value;

  /** The is settings. */
  private boolean set;

  //
  // Getters
  //

  //
  // Getter
  //

  @Override
  public String getName() {
    return this.nameSpace;
  }

  @Override
  public String getShortName() {
    return this.nameSpace;
  }

  @Override
  public String getValidatedName() {
    throw new EoulsanRuntimeException(
        "Name tool conditional can not be change.");
  }

  /**
   * Gets the tool parameter selected.
   * @return the tool parameter selected
   */
  public ToolElement getToolElementSelected() {
    return this.toolElementSelected;
  }

  /**
   * Gets the tool parameters result.
   * @return the tool parameters result
   */
  public Map<String, ToolElement> getToolElementsResult() {

    if (this.toolElementResult.isEmpty()) {
      return Collections.emptyMap();
    }

    return this.toolElementResult;
  }

  /**
   * Test if the value has been set (if not the default value)
   * @return true if the value has been set
   */
  public boolean isSet() {
    return this.set;
  }

  @Override
  public String getValue() {
    return this.value;
  }

  //
  // Setters
  //

  @Override
  public void setValue(final String value) {
  }

  /**
   * Set the values.
   * @param stepParameters step parameters
   * @throws EoulsanException if an error occurs while setting the values
   */
  public void setValues(final Map<String, Parameter> stepParameters)
      throws EoulsanException {

    setElementValue(this.toolElementSelected, stepParameters);

    // Parameter corresponding to choice
    final Collection<ToolElement> toolParameters =
        this.actionsRelatedOptions.get(this.toolElementSelected.getValue());

    // Check value parameter corresponding to a key
    for (final ToolElement toolParameter : toolParameters) {

      setElementValue(toolParameter, stepParameters);

      // Save map result
      this.toolElementResult.put(toolParameter.getName(), toolParameter);
    }

    // Save setting parameter
    this.set = true;
  }

  //
  // Private methods
  //

  /**
   * Parses the actions related options.
   * @param toolInfo the ToolInfo object
   * @param element the element
   * @return a multimap
   * @throws EoulsanException if an error occurs while creating a tool element
   */
  private Multimap<String, ToolElement> parseActionsRelatedOptions(
      final ToolInfo toolInfo, final Element element) throws EoulsanException {

    // Associate value options with param define in when tag, can be empty
    final Multimap<String, ToolElement> result = ArrayListMultimap.create();
    final List<Element> whenElement = getElementsByTagName(element, "when");

    for (final Element e : whenElement) {
      final String whenName = e.getAttribute("value");

      final List<Element> paramElement = new ArrayList<>();
      paramElement.addAll(getElementsByTagName(e, "param"));
      paramElement.addAll(getElementsByTagName(e, "data"));

      // Can be empty, nothing to do
      if (paramElement == null || paramElement.isEmpty()) {
        result.put(whenName, new EmptyToolElement());
        continue;
      }

      // Save param element
      for (final Element elem : paramElement) {
        // Initialize tool parameter related to the choice
        final ToolElement toolParameter =
            ToolElementFactory.newToolElement(toolInfo, elem, this.nameSpace);

        if (toolParameter != null) {
          // Add tool parameter in result
          result.put(whenName, toolParameter);
        }
      }
    }

    return result;
  }

  @Override
  public String toString() {
    return "ToolConditionalElement [name="
        + this.nameSpace + ", toolParameterSelect=" + this.toolElementSelected
        + ", options=" + this.actionsRelatedOptions + ", parameterEoulsan="
        + this.getValue() + "]";
  }

  //
  // Constructor
  //

  /**
   * Instantiates a new tool conditional element.
   * @param toolInfo the ToolInfo object
   * @param element the element
   * @throws EoulsanException the eoulsan exception
   */
  public ConditionalToolElement(final ToolInfo toolInfo, final Element element)
      throws EoulsanException {

    this.nameSpace = element.getAttribute("name");

    final List<Element> param = extractChildElementsByTagName(element, "param");

    if (param.isEmpty() || param.size() != 1) {
      throw newEoulsanException(toolInfo, getName(),
          "no valid parameter element found. "
              + "Only 1 element must be found in conditional element (found:  "
              + param.size() + ")");
    }

    // Check element select exist
    if (!param.get(0).getAttribute("type").equals("select")) {
      throw newEoulsanException(toolInfo, getName(),
          "no parameter with \"select\" type found in conditional element");
    }

    // Init parameter select
    this.toolElementSelected =
        new SelectParameterToolElement(toolInfo, param.get(0), this.nameSpace);

    // Init default value
    if (this.toolElementSelected instanceof AbstractParameterToolElement
        && ((AbstractParameterToolElement) this.toolElementSelected).isSet()) {
      this.value = this.toolElementSelected.getValue();
    }

    // Extract all case available
    this.actionsRelatedOptions =
        this.parseActionsRelatedOptions(toolInfo, element);
    this.toolElementResult = new HashMap<>();
  }

}
