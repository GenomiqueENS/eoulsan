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

import static fr.ens.transcriptome.eoulsan.galaxytool.io.GalaxyToolXMLParser.extractCommand;
import static fr.ens.transcriptome.eoulsan.galaxytool.io.GalaxyToolXMLParser.extractDescription;
import static fr.ens.transcriptome.eoulsan.galaxytool.io.GalaxyToolXMLParser.extractInputs;
import static fr.ens.transcriptome.eoulsan.galaxytool.io.GalaxyToolXMLParser.extractInterpreter;
import static fr.ens.transcriptome.eoulsan.galaxytool.io.GalaxyToolXMLParser.extractOutputs;
import static fr.ens.transcriptome.eoulsan.galaxytool.io.GalaxyToolXMLParser.extractToolID;
import static fr.ens.transcriptome.eoulsan.galaxytool.io.GalaxyToolXMLParser.extractToolName;
import static fr.ens.transcriptome.eoulsan.galaxytool.io.GalaxyToolXMLParser.extractToolVersion;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.galaxytool.element.ToolElement;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

/**
 * This class create an interpreter to tool xml file from Galaxy.
 * @author Sandrine Perrin
 * @since 2.1
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

  /** Data from tool XML. */
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
   * Parse tool xml to extract useful data to run tool.
   * @param setStepParameters the set step parameters
   * @throws EoulsanException if an data missing
   */
  public void configure(final Set<Parameter> setStepParameters)
      throws EoulsanException {

    this.initStepParameters(setStepParameters);

    // Set tool name
    this.toolID = extractToolID(this.doc);
    this.toolName = extractToolName(this.doc);
    this.toolVersion = extractToolVersion(this.doc);
    this.description = extractDescription(this.doc);

    this.inputs = extractInputs(this.doc, this.stepParameters);
    this.outputs = extractOutputs(this.doc);

    this.inDataFormatExpected = this.extractDataFormat(this.inputs);
    this.outDataFormatExpected = this.extractDataFormat(this.outputs);

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
   * @param inputData the input data
   * @param outputData the output data
   * @return the string
   * @throws EoulsanException the eoulsan exception
   */
  public String execute(final Map<DataFormat, DataFile> inputData,
      final Map<DataFormat, DataFile> outputData) throws EoulsanException {

    // Copy step tool element
    final Map<String, String> variablesCommand = this.extractVariables();

    // Add port with file path
    this.setPortInput(variablesCommand, inputData);

    // Add port with file path
    this.setPortOutput(variablesCommand, outputData);

    String newCommand =
        this.pythonInterperter.executeScript(this.command, variablesCommand);

    // Add interpreter if exists
    if (!(getInterpreter() == null || getInterpreter().isEmpty())) {
      newCommand = this.getInterpreter() + " " + newCommand;
    }

    return newCommand;
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
    final Map<String, String> variables = this.extractVariables();

    final String newCommand =
        this.pythonInterperter.executeScript(this.command, variables);

    return newCommand;
  }

  /**
   * Set input ports with name and value.
   * @param inputsPort the inputs port
   * @throws EoulsanException the eoulsan exception
   */
  public void setPortInput(final Map<String, String> inputsPort)
      throws EoulsanException {

    this.setToolElementWithPort(this.inputs, inputsPort);
  }

  /**
   * Set output ports with name and value.
   * @param outputsPort the outputs port
   * @throws EoulsanException the eoulsan exception
   */
  public void setPortOutput(final Map<String, String> outputsPort)
      throws EoulsanException {

    this.setToolElementWithPort(this.outputs, outputsPort);
  }

  /**
   * Associate port value on parameter tools corresponding.
   * @param paramTool set of parameter for tool galaxy
   * @param ports map on ports
   * @throws EoulsanException the eoulsan exception
   */
  private void setToolElementWithPort(final Map<String, ToolElement> paramTool,
      final Map<String, String> ports) throws EoulsanException {

    for (final Map.Entry<String, String> e : ports.entrySet()) {

      final ToolElement parameter = paramTool.get(e.getKey());

      if (parameter == null) {
        throw new EoulsanException(
            "Parsing tool xml: no parameter found related port: "
                + e.getKey() + ", " + e.getValue());
      }

      // Set value
      parameter.setValue(new Parameter(e.getKey(), e.getValue()));
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
   * Convert set parameters in map with name parameter related parameter.
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
   * @param variablesCommand the variables command
   * @param inData the in data
   * @throws EoulsanException the Eoulsan exception
   */
  private void setPortInput(final Map<String, String> variablesCommand,
      final Map<DataFormat, DataFile> inData) throws EoulsanException {

    // Parse format expected
    for (final Map.Entry<DataFormat, ToolElement> entry : this.inDataFormatExpected
        .entrySet()) {

      // TODO add case pair-end

      // Get the source
      final File inFile = inData.get(entry.getKey()).toFile();

      final String inVariableName = entry.getValue().getName();

      // Initialize variable with path
      variablesCommand.put(inVariableName, inFile.getAbsolutePath());
    }
  }

  /**
   * Set output ports with name and value.
   * @param variablesCommand the variables command
   * @param outData the out data
   * @throws EoulsanException the Eoulsan exception
   */
  private void setPortOutput(final Map<String, String> variablesCommand,
      final Map<DataFormat, DataFile> outData) throws EoulsanException {

    // Parse format expected
    for (final Map.Entry<DataFormat, ToolElement> entry : this.outDataFormatExpected
        .entrySet()) {

      // Get the source
      final File outFile = outData.get(entry.getKey()).toFile();

      final String inVariableName = entry.getValue().getName();

      // Initialize variable with path
      variablesCommand.put(inVariableName, outFile.getAbsolutePath());
    }

  }

  /**
   * Extract all setting variables tools with values.
   * @return setting parameters tools with values
   * @throws EoulsanException no parameter setting found
   */
  private Map<String, String> extractVariables() throws EoulsanException {

    final Map<String, String> results = extractVariablesFromXML();

    // Compare with variable from command tag
    // Add variable not found in xml tag, corresponding to dataset value from
    // external file
    final Map<String, String> missingVariables =
        this.comparisonVariablesFromXMLToCommand(results);

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
  private Map<String, String> extractVariablesFromXML() throws EoulsanException {

    final int variablesCount = this.inputs.size() + this.outputs.size();
    final Map<String, String> results = new HashMap<>(variablesCount);

    // // TODO
    // System.out.println("inputs param " + Joiner.on("\n").join(inputs));
    // // TODO
    // System.out.println("outputs param " + Joiner.on("\n").join(outputs));

    // Extract from inputs variable command
    for (final ToolElement ptg : this.inputs.values()) {
      results.put(ptg.getName(), ptg.getValue());
    }

    // Extract from outputs variable command
    for (final ToolElement ptg : this.outputs.values()) {
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
  private Map<String, String> comparisonVariablesFromXMLToCommand(
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
  // Getter
  //

  /**
   * Gets the tool id.
   * @return the tool id
   */
  public Object getToolID() {
    return this.toolID;
  }

  /**
   * Get tool name.
   * @return the tool name
   */
  public String getToolName() {
    return this.toolName;
  }

  /**
   * Get tool version.
   * @return the tool version
   */
  public String getToolVersion() {
    return this.toolVersion;
  }

  /**
   * Get tool description.
   * @return the description
   */
  public String getDescription() {
    return this.description;
  }

  /**
   * Get interpreter name.
   * @return the interpreter
   */
  public String getInterpreter() {
    return this.interpreter;
  }

  /**
   * Gets the in data format expected associated with variable found in command
   * line.
   * @return the in data format expected
   */
  public Map<DataFormat, ToolElement> getInDataFormatExpected() {
    return this.inDataFormatExpected;
  }

  /**
   * Gets the out data format expected associated with variable found in command
   * line.
   * @return the out data format expected
   */
  public Map<DataFormat, ToolElement> getOutDataFormatExpected() {
    return this.outDataFormatExpected;
  }

  @Override
  public String toString() {
    return "InterpreterToolGalaxy \n[inputs="
        + Joiner.on("\n").withKeyValueSeparator("=").join(this.inputs)
        + ", \noutputs="
        + Joiner.on("\n").withKeyValueSeparator("=").join(this.outputs)
        + ", \ntoolName=" + this.toolName + ", toolVersion=" + this.toolVersion
        + ", description=" + this.description + ", interpreter="
        + this.interpreter + ", command=\n" + this.command + "]";
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param is the is
   * @throws EoulsanException the Eoulsan exception
   */
  public ToolInterpreter(final InputStream is) throws EoulsanException {

    this.toolXMLis = is;
    this.doc = this.buildDOM();
    this.stepParameters = new HashMap<>();

    this.pythonInterperter = new ToolPythonInterpreter();

    this.checkDomValidity();
  }

}
