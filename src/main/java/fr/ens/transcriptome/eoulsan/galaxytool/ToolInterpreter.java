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

package fr.ens.transcriptome.eoulsan.galaxytool;

import static fr.ens.transcriptome.eoulsan.galaxytool.element.AbstractToolElement.getInstanceToolElement;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
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
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.galaxytool.element.ToolConditionalElement;
import fr.ens.transcriptome.eoulsan.galaxytool.element.ToolElement;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

/**
 * This class create an interpreter to tool xml file from Galaxy
 * @author Sandrine Perrin
 */
public class ToolInterpreter {

  /** The tool xm lis. */
  private final InputStream toolXMLis;

  // Throw an exception if tag exist in tool file
  /** The Constant TAG_FORBIDDEN. */
  private final static Set<String> TAG_FORBIDDEN = Sets.newHashSet("repeat");

  /** The Constant DEFAULT_VALUE_NULL. */
  private static final String DEFAULT_VALUE_NULL = "no_authorized";

  // Set DOM related to the tool XML file
  /** The doc. */
  private final Document doc;

  /** The python interperter. */
  private final ToolPythonInterpreter pythonInterperter;

  /** Data from tool XML */
  private Map<String, ToolElement> inputs;

  /** The outputs. */
  private Map<String, ToolElement> outputs;

  /** The tool id. */
  private String toolID;

  /** The tool name. */
  private String toolName;

  /** The tool version. */
  private String toolVersion;

  /** The description. */
  private String description;

  /** The interpreter. */
  private String interpreter;

  /** The command. */
  private String command;

  /** The variable names from command tag. */
  private Set<String> variableNamesFromCommandTag;

  /** The step parameters. */
  private final Map<String, Parameter> stepParameters;

  /** The in data format expected. */
  private Map<DataFormat, ToolElement> inDataFormatExpected;

  /** The out data format expected. */
  private Map<DataFormat, ToolElement> outDataFormatExpected;

  /**
   * Parse tool xml to extract useful data to run tool
   * @param stepParameters parameters for analysis
   * @throws EoulsanException if an data missing
   */
  public void configure(final Set<Parameter> setStepParameters)
      throws EoulsanException {

    initStepParameters(setStepParameters);

    // Set tool name
    this.toolID = extractToolID(this.doc);
    this.toolName = extractToolName(this.doc);
    this.toolVersion = extractToolVersion(this.doc);
    this.description = extractDescription(this.doc);

    this.inputs = extractInputs(this.doc, this.stepParameters);
    this.outputs = extractOutputs(this.doc);

    this.inDataFormatExpected = extractDataFormat(this.inputs);
    this.outDataFormatExpected = extractDataFormat(this.outputs);

    //
    this.interpreter = extractInterpreter(this.doc);
    final String cmdTagContent = extractCommand(this.doc);

    if (cmdTagContent.isEmpty()) {
      throw new EoulsanException("Parsing tool XML file: no command found.");
    }

    this.command = this.pythonInterperter.parseCommandString(cmdTagContent);
    this.variableNamesFromCommandTag =
        this.pythonInterperter.getVariableNames();
  }

  /**
   * Convert command tag from tool xml in string, variable are replace by value.
   * @return
   * @throws EoulsanException
   */
  public String execute(final Map<String, DataFile> inData,
      final Map<String, DataFile> outData) throws EoulsanException {

    // Copy step tool element
    final Map<String, String> parametersCommand = extractParameters();

    // Add port with file path
    setPortInput(parametersCommand, inData);

    // Add port with file path
    setPortOutput(parametersCommand, outData);

    final String new_command =
        this.pythonInterperter.executeScript(this.command, parametersCommand);

    return new_command;
  }

  //
  // Test methods for Junit
  //

  /**
   * Creates the command line.
   * @return the string
   * @throws EoulsanException the eoulsan exception
   */
  public String createCommandLine() throws EoulsanException {

    // List variable name define
    final Map<String, String> parameters = extractParameters();

    final String new_command =
        this.pythonInterperter.executeScript(this.command, parameters);

    return new_command;
  }

  /**
   * Set input ports with name and value.
   * @param inputsPort
   * @throws EoulsanException
   */
  public void setPortInput(final Map<String, String> inputsPort)
      throws EoulsanException {

    setParameterToolWithPort(this.inputs, inputsPort);
  }

  /**
   * Set output ports with name and value.
   * @param outputsPort
   * @throws EoulsanException
   */
  public void setPortOutput(final Map<String, String> outputsPort)
      throws EoulsanException {

    setParameterToolWithPort(this.outputs, outputsPort);
  }

  /**
   * Associate port value on parameter tools corresponding.
   * @param paramTool set of parameter for tool galaxy
   * @param ports map on ports
   * @throws EoulsanException
   */
  private void setParameterToolWithPort(
      final Map<String, ToolElement> paramTool, final Map<String, String> ports)
      throws EoulsanException {

    for (final Map.Entry<String, String> e : ports.entrySet()) {

      final ToolElement parameter = paramTool.get(e.getKey());

      if (parameter == null) {
        throw new EoulsanException(
            "Parsing tool xml: no parameter found related port: "
                + e.getKey() + ", " + e.getValue());
      }

      // Set value
      parameter.setParameterEoulsan(new Parameter(e.getKey(), e.getValue()));
    }
  }

  //
  // Private methods
  //

  /**
   * Extract data format.
   * @param parameters the parameters
   * @return the map
   */
  private Map<DataFormat, ToolElement> extractDataFormat(
      final Map<String, ToolElement> parameters) {

    final Map<DataFormat, ToolElement> results = new HashMap<>();

    // Parse parameters
    for (final Map.Entry<String, ToolElement> entry : parameters.entrySet()) {
      final ToolElement parameter = entry.getValue();

      if (parameter.isFile()) {

        // Extract data format
        results.put(parameter.getDataFormat(), parameter);
      }
    }

    return Collections.unmodifiableMap(results);
  }

  /**
   * Convert set parameters in map with name parameter related parameter
   * @param setStepParameters the set step parameters
   */
  private void initStepParameters(final Set<Parameter> setStepParameters) {

    // Convert Set in Map
    for (final Parameter p : setStepParameters) {
      this.stepParameters.put(p.getName(), p);
    }
  }

  /**
   * Set input ports with name and value.
   * @param inputsPort
   * @param inData
   * @throws EoulsanException
   */
  private void setPortInput(final Map<String, String> parametersCommand,
      final Map<String, DataFile> inData) throws EoulsanException {

    setParameterToolWithPort(parametersCommand, inData,
        this.inDataFormatExpected);
  }

  /**
   * Set output ports with name and value.
   * @param outputsPort
   * @param outData
   * @throws EoulsanException
   */
  private void setPortOutput(final Map<String, String> parametersCommand,
      final Map<String, DataFile> outData) throws EoulsanException {

    setParameterToolWithPort(parametersCommand, outData,
        this.outDataFormatExpected);
  }

  /**
   * Associate port value on parameter tools corresponding.
   * @param paramTool set of parameter for tool galaxy
   * @param ports map on ports
   * @param outDataFormatExpected2
   */
  private void setParameterToolWithPort(
      final Map<String, String> parametersCommand,
      final Map<String, DataFile> ports,
      final Map<DataFormat, ToolElement> dataFormatExpected)
      throws EoulsanException {

    // Parse ports
    for (final Map.Entry<String, DataFile> entry : ports.entrySet()) {
      // Extract data format related
      final DataFile port = entry.getValue();
      final DataFormat dataFormatPort = port.getDataFormat();

      if (dataFormatPort == null) {
        throw new EoulsanException(
            "Parsing tool xml: data format not found for port: "
                + entry.getKey() + "(" + port.getName() + ")");
      }

      // Check exist in expected list
      final ToolElement parameter = dataFormatExpected.get(dataFormatPort);

      if (parameter == null) {
        throw new EoulsanException(
            "Parsing tool xml: data format invalid: found "
                + dataFormatPort.getName() + " is expected "
                + Joiner.on(",").join(dataFormatExpected.keySet()));
      }

      // Add value in parameters command
      parametersCommand.put(parameter.getName(), entry.getKey());
    }

  }

  /**
   * Extract all setting parameters tools with values.
   * @return setting parameters tools with values
   * @throws EoulsanException no parameter setting found
   */
  private Map<String, String> extractParameters() throws EoulsanException {

    final Map<String, String> results = extractParametersFromXML();

    // Compare with variable from command tag
    // Add variable not found in xml tag, corresponding to dataset value from
    // external file
    final Map<String, String> missingVariables =
        comparisonParametersXMLVariablesCommand(results);

    // TODO
    System.out.println("variable init with stepParameters: \n"
        + Joiner.on("\n").withKeyValueSeparator("=").join(results));
    System.out.println("variable init with stepParameters: \n"
        + Joiner.on("\n").withKeyValueSeparator("=").join(missingVariables));

    if (!missingVariables.isEmpty()) {
      results.putAll(missingVariables);
    }

    return results;
  }

  /**
   * Extract parameters from xml.
   * @return the map
   * @throws EoulsanException the eoulsan exception
   */
  private Map<String, String> extractParametersFromXML()
      throws EoulsanException {

    final int variablesCount = this.inputs.size() + this.outputs.size();
    final Map<String, String> results = new HashMap<>(variablesCount);

    // // TODO
    // System.out.println("inputs param " + Joiner.on("\n").join(inputs));
    // // TODO
    // System.out.println("outputs param " + Joiner.on("\n").join(outputs));

    // Parse input
    for (final ToolElement ptg : this.inputs.values()) {
      // TODO
      // if (ptg.isSetting())
      // System.out.println("extract name="
      // + ptg.getName() + "\tval=" + ptg.getValue());
      results.put(ptg.getName(), ptg.getValue());
    }

    // Parse output
    for (final ToolElement ptg : this.outputs.values()) {
      // TODO
      // Add in map
      // System.out.println("extract name="
      // + ptg.getName() + "\tval=" + ptg.getValue());
      results.put(ptg.getName(), ptg.getValue());
    }

    if (results.isEmpty()) {
      throw new EoulsanException("No parameter settings.");
    }

    return results;
  }

  /**
   * Comparison parameters xml variables command.
   * @param parametersXML the parameters xml
   * @return the map
   * @throws EoulsanException the eoulsan exception
   */
  private Map<String, String> comparisonParametersXMLVariablesCommand(
      final Map<String, String> parametersXML) throws EoulsanException {

    final Map<String, String> results = new HashMap<>();

    // Parsing variable name found in command tag
    for (final String variableName : this.variableNamesFromCommandTag) {
      // Check exist
      if (parametersXML.get(variableName) == null) {
        results.put(variableName, DEFAULT_VALUE_NULL);
      }
    }
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

    } catch (final IOException e) {
      throw new EoulsanException(e.getMessage());
    } catch (final SAXException e) {
      throw new EoulsanException(e.getMessage());
    } catch (final ParserConfigurationException e) {
      throw new EoulsanException(e.getMessage());
    }
    // TODO
    // close is

  }

  /**
   * Check DOM validity.
   * @throws EoulsanException the Eoulsan exception
   */
  private void checkDomValidity() throws EoulsanException {

    for (final String tag : TAG_FORBIDDEN) {

      // Check tag exists in tool file
      if (!XMLUtils.getElementsByTagName(this.doc, tag).isEmpty()) {
        // Throw exception
        throw new EoulsanException("Parsing tool xml: unsupported tag " + tag);
      }
    }
  }

  //
  // Static methods parsing xml
  //

  /**
   * Extract param element.
   * @param parent the parent
   * @param elementName the element name
   * @return the map
   * @throws EoulsanException the Eoulsan exception
   */
  static Map<String, ToolElement> extractParamElement(final Element parent,
      final String elementName) throws EoulsanException {

    final Map<String, ToolElement> results = new HashMap<>();

    // Extract all param tag
    final List<Element> simpleParams =
        extractChildElementsByTagName(parent, elementName);

    for (final Element param : simpleParams) {
      final ToolElement ptg = getInstanceToolElement(param);

      results.put(ptg.getName(), ptg);
    }

    return results;
  }

  /**
   * Extract conditional param element.
   * @param parent the parent
   * @return the map
   * @throws EoulsanException the Eoulsan exception
   */
  static Map<String, ToolElement> extractConditionalParamElement(
      final Element parent) throws EoulsanException {
    final Map<String, Parameter> stepParameters = Collections.emptyMap();
    return extractConditionalParamElement(parent, stepParameters);
  }

  /**
   * Extract conditional param element.
   * @param parent the parent
   * @param stepParameters the step parameters
   * @return the map
   * @throws EoulsanException the Eoulsan exception
   */
  static Map<String, ToolElement> extractConditionalParamElement(
      final Element parent, final Map<String, Parameter> stepParameters)
      throws EoulsanException {

    final Map<String, ToolElement> results = new HashMap<>();

    // Extract conditional element, can be empty
    final List<Element> condParams =
        extractChildElementsByTagName(parent, "conditional");

    for (final Element param : condParams) {
      final ToolConditionalElement tce = new ToolConditionalElement(param);

      final ToolElement parameterSelect = tce.getToolParameterSelect();
      results.put(parameterSelect.getName(), parameterSelect);

      // Set parameter
      tce.setParameterEoulsan(stepParameters);

      results.putAll(tce.getToolParametersResult());

      // TODO
      // System.out.println("cond " + tce);
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
  static List<Element> extractElementsByTagName(final Document doc,
      final String tagName) throws EoulsanException {
    return extractElementsByTagName(doc, tagName, -1);
  }

  /**
   * Extract elements by tag name.
   * @param doc the doc
   * @param tagName the tag name
   * @param expectedCount the expected count
   * @return the list
   * @throws EoulsanException the Eoulsan exception
   */
  static List<Element> extractElementsByTagName(final Document doc,
      final String tagName, final int expectedCount) throws EoulsanException {

    final List<Element> result = XMLUtils.getElementsByTagName(doc, tagName);

    // Expected count available
    if (expectedCount > 0) {
      if (result.isEmpty()) {
        throw new EoulsanException("Parsing tool XML file: no "
            + tagName + " tag found.");
      }

      if (result.size() != expectedCount) {
        throw new EoulsanException("Parsing tool XML file: tag "
            + tagName + " invalid entry coutn found (expected " + expectedCount
            + " founded " + result.size() + ".");
      }
    }
    return result;
  }

  /**
   * Extract elements by tag name.
   * @param parent the parent
   * @param tagName the tag name
   * @return the list
   * @throws EoulsanException the Eoulsan exception
   */
  static List<Element> extractElementsByTagName(final Element parent,
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
  static List<Element> extractElementsByTagName(final Element parent,
      final String tagName, final int expectedCount) throws EoulsanException {

    final List<Element> result = extractChildElementsByTagName(parent, tagName);

    // Expected count available
    if (expectedCount > 0) {
      if (result.isEmpty()) {
        throw new EoulsanException("Parsing tool XML file: no "
            + tagName + " tag found.");
      }

      if (result.size() != expectedCount) {
        throw new EoulsanException("Parsing tool XML file: tag "
            + tagName + " invalid entry coutn found (expected " + expectedCount
            + " founded " + result.size() + ".");
      }
    }
    return result;
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

    final List<Element> result = Lists.newArrayList();

    for (int i = 0; i < nStepsList.getLength(); i++) {

      final Node node = nStepsList.item(i);

      if (node.getNodeType() == Node.ELEMENT_NODE) {
        final Element e = (Element) node;

        if (e.getTagName().equals(elementName)) {
          result.add(e);
        }
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
  static Map<String, ToolElement> extractOutputs(final Document doc)
      throws EoulsanException {

    final Map<String, ToolElement> results = new HashMap<>();

    final Element outputElement =
        extractElementsByTagName(doc, "outputs", 1).get(0);

    results.putAll(extractParamElement(outputElement, "data"));

    results.putAll(extractConditionalParamElement(outputElement));

    return results;
  }

  /**
   * Extract all input parameters define in document.
   * @param doc document represented tool xml
   * @param stepParameters parameters for analysis
   * @return all input parameters
   * @throws EoulsanException if none input parameter found
   */
  static Map<String, ToolElement> extractInputs(final Document doc,
      final Map<String, Parameter> stepParameters) throws EoulsanException {

    final Map<String, ToolElement> results = new HashMap<>();

    final Element inputElement =
        extractElementsByTagName(doc, "inputs", 1).get(0);

    results.putAll(extractParamElement(inputElement, "param"));

    results
        .putAll(extractConditionalParamElement(inputElement, stepParameters));

    // Extract input
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
   * Extract tool id tag in string.
   * @param doc document represented tool xml
   * @return tool id string
   */
  static String extractToolID(final Document doc) {

    return extractValueFromElement(doc, "tool", 0, "id");
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

  //
  // Getter
  //

  /**
   * Gets the tool id.
   * @return the tool id
   */
  public Object getToolID() {
    return this.toolID;
  }

  /** Get tool name */
  public String getToolName() {
    return this.toolName;
  }

  /** Get tool version */
  public String getToolVersion() {
    return this.toolVersion;
  }

  /** Get tool description */
  public String getDescription() {
    return this.description;
  }

  /** Get interpreter name */
  public String getInterpreter() {
    return this.interpreter;
  }

  @Override
  public String toString() {
    return "InterpreterToolGalaxy \n[inputs="
        + Joiner.on("\n").withKeyValueSeparator("=").join(this.inputs)
        + ", \noutputs="
        + Joiner.on("\n").withKeyValueSeparator("=").join(this.outputs)
        + ", \ntoolName=" + this.toolName + ", toolVersion=" + this.toolVersion
        + ", description=" + this.description + ", interpreter=" + this.interpreter
        + ", command=\n" + this.command + "]";
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   * @param is the is
   * @throws EoulsanException the Eoulsan exception
   */
  public ToolInterpreter(final InputStream is) throws EoulsanException {

    this.toolXMLis = is;
    this.doc = buildDOM();
    this.stepParameters = new HashMap<>();

    this.pythonInterperter = new ToolPythonInterpreter();

    checkDomValidity();
  }

}
