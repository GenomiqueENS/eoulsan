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
package fr.ens.biologie.genomique.eoulsan.galaxytools;

import static fr.ens.biologie.genomique.eoulsan.galaxytools.elements.ToolElementFactory.newToolElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.galaxytools.elements.ConditionalToolElement;
import fr.ens.biologie.genomique.eoulsan.galaxytools.elements.DataToolElement;
import fr.ens.biologie.genomique.eoulsan.galaxytools.elements.ToolElement;
import fr.ens.biologie.genomique.eoulsan.util.XMLUtils;
import com.google.common.base.Splitter;

/**
 * This class define static utils methods to extract data in Galaxy tool XML
 * file.
 * @author Sandrine Perrin
 * @since 2.0
 */
public final class GalaxyToolXMLParserUtils {

  /** The Constant ID_TAG. */
  private static final String ID_TAG = "id";

  /** The Constant NAME_TAG. */
  private static final String NAME_TAG = "name";

  /** The Constant VERSION_TAG. */
  private static final String VERSION_TAG = "version";

  /** The Constant TOOL_TAG. */
  private static final String TOOL_TAG = "tool";

  /** The Constant DESCRIPTION_TAG. */
  private static final String DESCRIPTION_TAG = "description";

  /** The Constant INTERPRETER_TAG. */
  private static final String INTERPRETER_TAG = "interpreter";

  /** The Constant DOCKER_IMAGE_TAG. */
  private static final String DOCKER_IMAGE_TAG = "dockerimage";

  /** The Constant COMMAND_TAG. */
  private static final String COMMAND_TAG = "command";

  /** The Constant PARAM_TAG. */
  private static final String PARAM_TAG = "param";

  /** The Constant INPUTS_TAG. */
  private static final String INPUTS_TAG = "inputs";

  /** The Constant DATA_TAG. */
  private static final String DATA_TAG = "data";

  /** The Constant OUTPUTS_TAG. */
  private static final String OUTPUTS_TAG = "outputs";

  /** The Constant CONDITIONAL. */
  private static final String CONDITIONAL = "conditional";

  /**
   * Extract parameter elements.
   * @param toolInfo the ToolInfo object
   * @param parent the parent
   * @param elementName the element name
   * @return a map with the Galaxy tool elements
   * @throws EoulsanException the Eoulsan exception
   */
  public static Map<String, ToolElement> extractParamElement(
      final ToolInfo toolInfo, final Element parent, final String elementName)
      throws EoulsanException {
    final Map<String, Parameter> stepParameters = Collections.emptyMap();
    return extractParamElement(toolInfo, parent, elementName, stepParameters);
  }

  /**
   * Extract parameter elements.
   * @param toolInfo the ToolInfo object
   * @param parent the parent
   * @param elementName the element name
   * @param stepParameters step parameters
   * @return a map with the Galaxy tool elements
   * @throws EoulsanException the Eoulsan exception
   */
  public static Map<String, ToolElement> extractParamElement(
      final ToolInfo toolInfo, final Element parent, final String elementName,
      final Map<String, Parameter> stepParameters) throws EoulsanException {

    final Map<String, ToolElement> results = new HashMap<>();

    // Extract all param tag
    final List<Element> simpleParams =
        extractChildElementsByTagName(parent, elementName);

    for (final Element param : simpleParams) {
      final ToolElement toolElement = newToolElement(toolInfo, param);

      setElementValue(toolElement, stepParameters);

      results.put(toolElement.getName(), toolElement);
    }

    return results;
  }

  /**
   * Set the tool element value.
   * @param toolElement tool element
   * @param stepParameters step parameters
   * @throws EoulsanException if an error occurs while setting the value
   */
  public static void setElementValue(final ToolElement toolElement,
      final Map<String, Parameter> stepParameters) throws EoulsanException {

    if (!(toolElement instanceof DataToolElement)) {

      // Use namespace
      Parameter p = stepParameters.get(toolElement.getName());

      if (p == null) {
        // Without namespace
        p = stepParameters.get(toolElement.getShortName());
      }

      if (p != null) {
        toolElement.setValue(p.getStringValue());
      }
    }
  }

  /**
   * Extract conditional param element.
   * @param parent the parent
   * @return the map
   * @throws EoulsanException the Eoulsan exception
   */
  public static Map<String, ToolElement> extractConditionalParamElement(
      final ToolInfo toolInfo, final Element parent) throws EoulsanException {
    final Map<String, Parameter> stepParameters = Collections.emptyMap();
    return extractConditionalParamElement(toolInfo, parent, stepParameters);
  }

  /**
   * Extract conditional param element.
   * @param parent the parent
   * @param stepParameters the step parameters
   * @return the map
   * @throws EoulsanException the Eoulsan exception
   */
  public static Map<String, ToolElement> extractConditionalParamElement(
      final ToolInfo toolInfo, final Element parent,
      final Map<String, Parameter> stepParameters) throws EoulsanException {

    final Map<String, ToolElement> results = new HashMap<>();

    // Extract conditional element, can be empty
    final List<Element> condParams = GalaxyToolXMLParserUtils
        .extractChildElementsByTagName(parent, CONDITIONAL);

    for (final Element param : condParams) {
      final ConditionalToolElement tce =
          new ConditionalToolElement(toolInfo, param);

      final ToolElement parameterSelect = tce.getToolElementSelected();
      results.put(parameterSelect.getName(), parameterSelect);

      // Set parameter
      tce.setValues(stepParameters);

      results.putAll(tce.getToolElementsResult());
    }

    return results;
  }

  /**
   * Extract elements by tag name.
   * @param doc the doc
   * @param tagName the tag name
   * @return the list
   * @throws EoulsanException the Eoulsan exception
   */
  public static List<Element> extractElementsByTagName(final ToolInfo toolInfo,
      final Document doc, final String tagName) throws EoulsanException {
    return extractElementsByTagName(toolInfo, doc, tagName, -1);
  }

  /**
   * Extract elements by tag name.
   * @param toolInfo ToolInfo object
   * @param doc the doc
   * @param tagName the tag name
   * @param expectedCount the expected count
   * @return the list
   * @throws EoulsanException the Eoulsan exception
   */
  public static List<Element> extractElementsByTagName(final ToolInfo toolInfo,
      final Document doc, final String tagName, final int expectedCount)
      throws EoulsanException {

    final List<Element> result = XMLUtils.getElementsByTagName(doc, tagName);

    // Expected count available
    if (expectedCount > 0) {
      if (result.isEmpty()) {
        throw new EoulsanException(
            "Parsing tool XML file: no " + tagName + " tag found.");
      }

      if (result.size() != expectedCount) {
        throw newEoulsanException(toolInfo,
            "invalid entry count found in \""
                + tagName + "\" tag (expected " + expectedCount + " found "
                + result.size() + ")");
      }
    }

    if (result == null || result.isEmpty()) {
      return Collections.emptyList();
    }

    return Collections.unmodifiableList(result);
  }

  /**
   * Create an EoulsanException.
   * @param toolInfo toolInfo object
   * @param message error message
   * @throws EoulsanException in any case
   */
  public static EoulsanException newEoulsanException(final ToolInfo toolInfo,
      final String message) throws EoulsanException {

    return new EoulsanException("Error while parsing \""
        + toolInfo.getToolSource() + "\" Galaxy tool file: " + message);
  }

  /**
   * Create an EoulsanException.
   * @param toolInfo toolInfo object
   * @param parameterName parameter name
   * @param message error message
   * @throws EoulsanException in any case
   */
  public static EoulsanException newEoulsanException(final ToolInfo toolInfo,
      final String parameterName, final String message)
      throws EoulsanException {

    return new EoulsanException("Error while parsing \""
        + parameterName + "\" parameter of the \"" + toolInfo.getToolSource()
        + "\" Galaxy tool file: " + message);
  }

  /**
   * Extract elements by tag name.
   * @param parent the parent
   * @param tagName the tag name
   * @return the list
   * @throws EoulsanException the Eoulsan exception
   */
  public static List<Element> extractElementsByTagName(final Element parent,
      final String tagName) throws EoulsanException {
    return extractElementsByTagName(parent, tagName, -1);
  }

  /**
   * Extract elements by tag name.
   * @param parent the parent
   * @param tagName the tag name
   * @param expectedCount the expected count
   * @return the list
   * @throws EoulsanException the Eoulsan exception
   */
  public static List<Element> extractElementsByTagName(final Element parent,
      final String tagName, final int expectedCount) throws EoulsanException {

    final List<Element> result =
        GalaxyToolXMLParserUtils.extractChildElementsByTagName(parent, tagName);

    // Expected count available
    if (expectedCount > 0) {
      if (result.isEmpty()) {
        throw new EoulsanException(
            "Parsing tool XML file: no " + tagName + " tag found.");
      }

      if (result.size() != expectedCount) {
        throw new EoulsanException("Parsing tool XML file: tag "
            + tagName + " invalid entry count found (expected " + expectedCount
            + " founded " + result.size() + ".");
      }
    }

    if (result == null || result.isEmpty()) {
      return Collections.emptyList();
    }

    return Collections.unmodifiableList(result);
  }

  /**
   * Extract child elements by tag name.
   * @param parentElement the parent element
   * @param elementName the element name
   * @return the list
   */
  public static List<Element> extractChildElementsByTagName(
      final Element parentElement, final String elementName) {

    if (elementName == null || parentElement == null) {
      return null;
    }

    final NodeList nStepsList = parentElement.getChildNodes();
    if (nStepsList == null) {
      return null;
    }

    final List<Element> result = new ArrayList<>();

    for (int i = 0; i < nStepsList.getLength(); i++) {

      final Node node = nStepsList.item(i);

      // Check element found
      if (node == null)
        continue;

      if (node.getNodeType() == Node.ELEMENT_NODE) {
        final Element e = (Element) node;

        if (e.getTagName().equals(elementName)) {
          result.add(e);
        }
      }
    }

    if (result == null || result.isEmpty()) {
      return Collections.emptyList();
    }

    return Collections.unmodifiableList(result);
  }

  /**
   * Extract all output parameters define in document.
   * @param toolInfo the tool information
   * @param doc document represented tool xml
   * @param stepParameters parameters for analysis
   * @return all output parameters
   * @throws EoulsanException if none output parameter found
   */
  public static Map<String, ToolElement> extractOutputs(final ToolInfo toolInfo,
      final Document doc, final Map<String, Parameter> stepParameters)
      throws EoulsanException {

    final Map<String, ToolElement> results = new HashMap<>();

    final Element outputElement =
        extractElementsByTagName(toolInfo, doc, OUTPUTS_TAG, 1).get(0);

    results.putAll(
        extractParamElement(toolInfo, outputElement, DATA_TAG, stepParameters));

    results.putAll(extractConditionalParamElement(toolInfo, outputElement,
        stepParameters));

    return results;
  }

  /**
   * Extract all input parameters define in document.
   * @param toolInfo the tool information
   * @param doc document represented tool xml
   * @param stepParameters parameters for analysis
   * @return all input parameters
   * @throws EoulsanException if none input parameter found
   */
  public static Map<String, ToolElement> extractInputs(final ToolInfo toolInfo,
      final Document doc, final Map<String, Parameter> stepParameters)
      throws EoulsanException {

    final Map<String, ToolElement> results = new HashMap<>();

    final Element inputElement =
        extractElementsByTagName(toolInfo, doc, INPUTS_TAG, 1).get(0);

    results.putAll(
        extractParamElement(toolInfo, inputElement, PARAM_TAG, stepParameters));

    results.putAll(
        extractConditionalParamElement(toolInfo, inputElement, stepParameters));

    // Extract input
    return results;
  }

  /**
   * Extract command tag in string.
   * @param doc document represented tool xml
   * @return command string
   */
  public static String extractCheetahScript(final Document doc) {
    return extractValueFromElement(doc, COMMAND_TAG, 0, null);
  }

  /**
   * Extract interpreter attribute in string.
   * @param doc document represented tool xml
   * @return interpreter names
   */
  public static List<String> extractInterpreters(final Document doc) {

    List<String> result = new ArrayList<>();

    for (String s : Splitter.on(',').trimResults()
        .split(extractValueFromElement(doc, COMMAND_TAG, 0, INTERPRETER_TAG))) {
      result.add(s);
    }

    return result;
  }

  /**
   * Extract docker image attribute in string.
   * @param doc document represented tool xml
   * @return the docker image name
   */
  public static String extractDockerImage(final Document doc) {
    return extractValueFromElement(doc, COMMAND_TAG, 0, DOCKER_IMAGE_TAG);
  }

  /**
   * Extract description tag in string.
   * @param doc document represented tool xml
   * @return description
   */
  public static String extractDescription(final Document doc) {
    return extractValueFromElement(doc, DESCRIPTION_TAG, 0, null);
  }

  /**
   * Extract tool version attribute in string.
   * @param doc document represented tool xml
   * @return tool version string
   */
  public static String extractToolVersion(final Document doc) {

    return extractValueFromElement(doc, TOOL_TAG, 0, VERSION_TAG);
  }

  /**
   * Extract tool name tag in string.
   * @param doc document represented tool xml
   * @return tool name string
   */
  public static String extractToolName(final Document doc) {

    return extractValueFromElement(doc, TOOL_TAG, 0, NAME_TAG);
  }

  /**
   * Extract tool id tag in string.
   * @param doc document represented tool xml
   * @return tool id string
   */
  public static String extractToolID(final Document doc) {

    return extractValueFromElement(doc, TOOL_TAG, 0, ID_TAG);
  }

  /**
   * Extract text from DOM from tag name, at the index place. If attribute name
   * defined return value attribute, otherwise return content text of element.
   * @param doc document represented tool xml
   * @param elementName element name to extract
   * @param index index of element name to extract
   * @param attributeName attribute name from element name to extract
   * @return value corresponding to element name content or attribute of element
   *         name. Return null, if none corresponding element or attribute
   *         found.
   */
  public static String extractValueFromElement(final Document doc,
      final String elementName, final int index, final String attributeName) {

    // List element
    final List<Element> e = XMLUtils.getElementsByTagName(doc, elementName);

    if (e.isEmpty()) {
      return null;
    }

    // Check size
    if (index >= e.size()) {
      return null;
    }

    // Return content text
    if (attributeName == null) {
      return e.get(index).getTextContent();
    }

    // Return value of attribute
    return e.get(index).getAttribute(attributeName);
  }

}
