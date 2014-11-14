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

package fr.ens.transcriptome.eoulsan.toolgalaxy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

/**
 * This class create an interpreter to tool xml file from Galaxy
 * @author Sandrine Perrin
 */
public class ToolInterpreter {

  private final InputStream toolXMLis;

  // Throw an exception if tag exist in tool file
  private final static Set<String> TAG_FORBIDDEN = Sets.newHashSet("repeat");

  // Set DOM related to the tool XML file
  private final Document doc;

  /** Data from tool XML */
  private Set<ToolElement> inputs;
  private Set<ToolElement> outputs;

  private String toolName;
  private String toolVersion;
  private String description;
  private String interpreter;
  private String command;

  // Set input port is setting
  private boolean isSettingsInputPort = false;
  // Set output port is setting
  private boolean isSettingsOutputPort = false;

  /**
   * Parse tool xml to extract useful data to run tool
   * @param parametersEoulsan parameters from Eoulsan
   * @throws EoulsanException if an data missing
   */
  // TODO param remove by Set<Parameter>
  public void configure(final Map<String, String> parametersEoulsan)
      throws EoulsanException {

    // Set tool name
    this.toolName = extractToolName(this.doc);
    this.toolVersion = extractToolVersion(this.doc);
    this.description = extractDescription(this.doc);

    this.inputs = extractInputs(this.doc, parametersEoulsan);
    this.outputs = extractOutputs(this.doc);

    //
    this.interpreter = extractInterpreter(this.doc);
    this.command = extractCommand(this.doc);

    if (this.command.isEmpty())
      throw new EoulsanException("Parsing tool XML file: no command found.");

    // Associate parameter input tool with parameters Eoulsan
    for (ToolElement ptg : inputs) {
      final String name = ptg.getName();

      if (parametersEoulsan.containsKey(name))
        // TODO Check validity
        ptg.setParameterEoulsan(parametersEoulsan.get(name));
    }

  }

  /**
   * Set input ports with name and value.
   * @param inputsPort
   */
  public void setPortInput(final Map<String, String> inputsPort) {
    this.isSettingsInputPort = true;
    setParameterToolWithPort(this.inputs, inputsPort);
  }

  /**
   * Set output ports with name and value.
   * @param outputsPort
   */
  public void setPortOutput(final Map<String, String> outputsPort) {
    this.isSettingsOutputPort = true;
    setParameterToolWithPort(this.outputs, outputsPort);
  }

  /**
   * Convert command tag from tool xml in string, variable are replace by value.
   * @return
   * @throws EoulsanException
   */
  public String createCommandLine() throws EoulsanException {

    if (!isSettingsPorts())
      throw new EoulsanException("Interpreter tool galaxy: no setting ports.");

    // List variable name define
    Map<String, String> parameters = extractParameters();

    // TODO
    // System.out.println("map parameters \n"
    // + Joiner.on("\n").withKeyValueSeparator("\t").join(parameters));

    String old_command = this.command;
    String new_command = "";
    // Replace variable by value
    for (Map.Entry<String, String> e : parameters.entrySet()) {
      final String var = "$" + e.getKey();
      final String value = e.getValue();

      new_command = old_command.replace(var, value);
      old_command = new_command;
    }

    // Remove new line
    new_command =
        old_command.replaceAll("[\\n\\t\\r]+", " ").replaceAll("\\s+", " ");

    return new_command;
  }

  //
  // Private methods
  //

  /**
   * Check if all ports are setting.
   * @return
   */
  private boolean isSettingsPorts() {
    return this.isSettingsInputPort && this.isSettingsOutputPort;
  }

  /**
   * Associate port value on parameter tools corresponding.
   * @param paramTool set of parameter for tool galaxy
   * @param ports map on ports
   */
  private void setParameterToolWithPort(final Set<ToolElement> paramTool,
      final Map<String, String> ports) {

    for (ToolElement ptg : paramTool) {

      for (Map.Entry<String, String> e : ports.entrySet()) {

        // Compare name between parameterToolGalaxy and name port
        if (ptg.getName().equals(e.getKey()))
          // Set value
          ptg.setParameterEoulsan(e.getValue());
      }
    }
  }

  /**
   * Extract all setting parameters tools with values.
   * @return setting parameters tools with values
   * @throws EoulsanException no parameter setting found
   */
  private Map<String, String> extractParameters() throws EoulsanException {

    final int variablesCount = this.inputs.size() + this.outputs.size();
    final Map<String, String> results =
        Maps.newHashMapWithExpectedSize(variablesCount);

    // Parse input
    for (ToolElement ptg : inputs) {
      if (ptg.isSetting())
        results.put(ptg.getName(), ptg.getParameterEoulsan());
    }

    // Parse output
    for (ToolElement ptg : outputs) {
      if (ptg.isSetting())
        // Add in map
        results.put(ptg.getName(), ptg.getParameterEoulsan());
    }

    if (results.isEmpty())
      throw new EoulsanException("No parameter settings.");

    return results;
  }

  /**
   * Create DOM instance from tool xml file.
   * @return DOM instance
   * @throws EoulsanException if an error occurs during creation instance
   */
  private Document buildDOM() throws EoulsanException {

    try {
      // Read the XML file
      final DocumentBuilder dBuilder =
          DocumentBuilderFactory.newInstance().newDocumentBuilder();
      final Document doc = dBuilder.parse(this.toolXMLis);
      doc.getDocumentElement().normalize();
      return doc;

    } catch (IOException e) {
      throw new EoulsanException(e.getMessage());
    } catch (SAXException e) {
      throw new EoulsanException(e.getMessage());
    } catch (ParserConfigurationException e) {
      throw new EoulsanException(e.getMessage());
    }
    // TODO
    // close is

  }

  private void checkDomValidity() throws EoulsanException {

    for (String tag : TAG_FORBIDDEN) {

      // Check tag exists in tool file
      if (!XMLUtils.getElementsByTagName(this.doc, tag).isEmpty())
        // Throw exception
        throw new EoulsanException("Parsing tool xml: unsupported tag " + tag);
    }
  }

  //
  // Static methods parsing xml
  //

  static Set<ToolElement> extractParamElement(final Element parent)
      throws EoulsanException {

    Set<ToolElement> results = Sets.newHashSet();

    // Extract all param tag
    List<Element> simpleParams = extractElementsByTagName(parent, "param");

    for (Element param : simpleParams) {
      final ToolParameter ptg = new ToolParameter(param);
      results.add(ptg);
    }

    return results;
  }

  static Set<ToolElement> extractConditionalParamElement(final Element parent,
      final Map<String, String> parametersEoulsan) throws EoulsanException {

    Set<ToolElement> results = Sets.newHashSet();

    // Extract conditional element, can be empty
    List<Element> condParams = extractElementsByTagName(parent, "conditional");

    for (Element param : condParams) {
      final ToolConditionalElement tce = new ToolConditionalElement(param);
      results.add(tce.getToolParameterSelect());

      // Set parameter
      tce.setParameterEoulsan(parametersEoulsan);

      if (tce.isSetting()) {
        results.addAll(tce.getToolParametersResult());
      }
    }

    return results;
  }

  /**
   * @param doc
   * @param tagName
   * @param expectedCount
   * @return
   * @throws EoulsanException
   */
  static List<Element> extractElementsByTagName(final Document doc,
      final String tagName) throws EoulsanException {
    return extractElementsByTagName(doc, tagName, -1);
  }

  /**
   * @param doc
   * @param tagName
   * @param expectedCount
   * @return
   * @throws EoulsanException
   */
  static List<Element> extractElementsByTagName(final Document doc,
      final String tagName, final int expectedCount) throws EoulsanException {

    final List<Element> result = XMLUtils.getElementsByTagName(doc, tagName);

    // Expected count available
    if (expectedCount > 0) {
      if (result.isEmpty())
        throw new EoulsanException("Parsing tool XML file: no "
            + tagName + " tag found.");

      if (result.size() != expectedCount)
        throw new EoulsanException("Parsing tool XML file: tag "
            + tagName + " invalid entry coutn found (expected " + expectedCount
            + " founded " + result.size() + ".");

    }

    return result;

  }

  /**
   * @param parent
   * @param tagName
   * @param expectedCount
   * @return
   * @throws EoulsanException
   */
  static List<Element> extractElementsByTagName(final Element parent,
      final String tagName) throws EoulsanException {
    return extractElementsByTagName(parent, tagName, -1);
  }

  /**
   * @param parent
   * @param tagName
   * @param expectedCount
   * @return
   * @throws EoulsanException
   */
  static List<Element> extractElementsByTagName(final Element parent,
      final String tagName, final int expectedCount) throws EoulsanException {

    final List<Element> result = extractChildElementsByTagName(parent, tagName);

    // Expected count available
    if (expectedCount > 0) {
      if (result.isEmpty())
        throw new EoulsanException("Parsing tool XML file: no "
            + tagName + " tag found.");

      if (result.size() != expectedCount)
        throw new EoulsanException("Parsing tool XML file: tag "
            + tagName + " invalid entry coutn found (expected " + expectedCount
            + " founded " + result.size() + ".");

    }

    return result;

  }

  static List<Element> extractChildElementsByTagName(
      final Element parentElement, final String elementName) {

    if (elementName == null || parentElement == null)
      return null;

    final NodeList nStepsList = parentElement.getChildNodes();
    if (nStepsList == null)
      return null;

    final List<Element> result = Lists.newArrayList();

    for (int i = 0; i < nStepsList.getLength(); i++) {

      final Node node = nStepsList.item(i);

      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element e = (Element) node;

        if (e.getTagName().equals(elementName))
          result.add(e);
      }
    }

    return result;
  }

  /**
   * Extract all output parameters define in document.
   * @param doc document represented tool xml
   * @return all output parameters
   * @throws EoulsanException if none output parameter found
   */
  static Set<ToolElement> extractOutputs(final Document doc)
      throws EoulsanException {

    final Set<ToolElement> results = Sets.newHashSet();

    Element outputsElement = extractElementsByTagName(doc, "outputs", 1).get(0);

    // Extract all param tag
    List<Element> simpleParams =
        XMLUtils.getElementsByTagName(outputsElement, "data");

    for (Element param : simpleParams) {
      final ToolElement ptg = new ToolParameter(param);
      results.add(ptg);
    }

    // Extract all param tag
    List<Element> condParams =
        XMLUtils.getElementsByTagName(outputsElement, "conditional");

    for (Element param : condParams) {
      final ToolElement ptg = new ToolConditionalElement(param);
      results.add(ptg);
    }

    return results;
  }

  /**
   * Extract all input parameters define in document.
   * @param doc document represented tool xml
   * @param parametersEoulsan parameters from Eoulsan
   * @return all input parameters
   * @throws EoulsanException if none input parameter found
   */
  static Set<ToolElement> extractInputs(final Document doc,
      final Map<String, String> parametersEoulsan) throws EoulsanException {

    final Set<ToolElement> results = Sets.newHashSet();

    final Element inputElement =
        extractElementsByTagName(doc, "inputs", 1).get(0);

    results.addAll(extractParamElement(inputElement));
    results.addAll(extractConditionalParamElement(inputElement,
        parametersEoulsan));

    return results;
  }

  /**
   * Extract command tag in string.
   * @param doc document represented tool xml
   * @return command string
   */
  static String extractCommand(final Document doc) {
    return extractValueFromElement(doc, "command", 0, null);
  }

  /**
   * Extract interpreter attribute in string.
   * @param doc document represented tool xml
   * @return interpreter name
   */
  static String extractInterpreter(final Document doc) {
    return extractValueFromElement(doc, "command", 0, "interpreter");
  }

  /**
   * Extract description tag in string.
   * @param doc document represented tool xml
   * @return description
   */
  static String extractDescription(final Document doc) {
    return extractValueFromElement(doc, "description", 0, null);
  }

  /**
   * Extract tool version attribute in string.
   * @param doc document represented tool xml
   * @return tool version string
   */
  static String extractToolVersion(final Document doc) {

    return extractValueFromElement(doc, "tool", 0, "version");
  }

  /**
   * Extract tool name tag in string.
   * @param doc document represented tool xml
   * @return tool name string
   */
  static String extractToolName(final Document doc) {

    return extractValueFromElement(doc, "tool", 0, "name");
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
  static String extractValueFromElement(final Document doc,
      final String elementName, final int index, final String attributeName) {

    // List element
    List<Element> e = XMLUtils.getElementsByTagName(doc, elementName);

    if (e.isEmpty())
      return null;

    // Check size
    if (index >= e.size())
      return null;

    // Return content text
    if (attributeName == null)
      return e.get(index).getTextContent();

    // Return value of attribute
    return e.get(index).getAttribute(attributeName);
  }

  //
  // Getter
  //

  /** Get tool name */
  public String getToolName() {
    return toolName;
  }

  /** Get tool version */
  public String getToolVersion() {
    return toolVersion;
  }

  /** Get tool description */
  public String getDescription() {
    return description;
  }

  /** Get interpreter name */
  public String getInterpreter() {
    return interpreter;
  }

  @Override
  public String toString() {
    return "InterpreterToolGalaxy \n[inputs="
        + Joiner.on("\n").join(inputs) + ", \noutputs="
        + Joiner.on("\n").join(outputs) + ", \ntoolName=" + toolName
        + ", toolVersion=" + toolVersion + ", description=" + description
        + ", interpreter=" + interpreter + ", command=\n" + command
        + ", \nisSettingsInputPort=" + isSettingsInputPort
        + ", isSettingsOutputPort=" + isSettingsOutputPort + "]";
  }

  //
  // Constructor
  //

  public ToolInterpreter(final InputStream is) throws EoulsanException {

    this.toolXMLis = is;
    this.doc = buildDOM();

    checkDomValidity();
  }

}
