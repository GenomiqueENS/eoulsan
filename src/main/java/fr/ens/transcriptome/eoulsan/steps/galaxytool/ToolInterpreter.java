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

package fr.ens.transcriptome.eoulsan.steps.galaxytool;

import static fr.ens.transcriptome.eoulsan.util.galaxytool.GalaxyToolXMLParser.extractCommand;
import static fr.ens.transcriptome.eoulsan.util.galaxytool.GalaxyToolXMLParser.extractDescription;
import static fr.ens.transcriptome.eoulsan.util.galaxytool.GalaxyToolXMLParser.extractInputs;
import static fr.ens.transcriptome.eoulsan.util.galaxytool.GalaxyToolXMLParser.extractInterpreter;
import static fr.ens.transcriptome.eoulsan.util.galaxytool.GalaxyToolXMLParser.extractOutputs;
import static fr.ens.transcriptome.eoulsan.util.galaxytool.GalaxyToolXMLParser.extractToolID;
import static fr.ens.transcriptome.eoulsan.util.galaxytool.GalaxyToolXMLParser.extractToolName;
import static fr.ens.transcriptome.eoulsan.util.galaxytool.GalaxyToolXMLParser.extractToolVersion;

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
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.steps.galaxytool.elements.ToolElement;
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

  // Set DOM related to the tool XML file
  /** The doc. */
  private final Document doc;

  /** Data from tool XML. */
  private Map<String, ToolElement> inputs;

  /** The outputs. */
  private Map<String, ToolElement> outputs;

  /** The command. */
  // private String command;

  /** The variable names from command tag. */
  // private Set<String> variableNamesFromCommandTag;

  /** The tool name from parameter Eoulsan file. */
  private final String toolNameFromParameter;

  /** The step parameters. */
  private final Map<String, Parameter> stepParameters;

  /** The in data format expected. */
  private Map<DataFormat, ToolElement> inFileExpected;

  /** The out data format expected. */
  private Map<DataFormat, ToolElement> outFileExpected;

  /** The tool. */
  private final ToolData tool;

  private StepContext context;

  // private String newCommand;

  /**
   * Parse tool xml to extract useful data to run tool.
   * @param setStepParameters the set step parameters
   * @throws EoulsanException if an data missing
   */
  public void configure(final Set<Parameter> setStepParameters)
      throws EoulsanException {

    this.initStepParameters(setStepParameters);

    final Document localDoc = this.doc;

    // Set tool name
    this.tool.setToolID(extractToolID(localDoc));
    this.tool.setToolName(extractToolName(localDoc));
    this.tool.setToolVersion(extractToolVersion(localDoc));
    this.tool.setDescription(extractDescription(localDoc));

    this.tool.setInterpreter(extractInterpreter(localDoc));
    this.tool.setCmdTagContent(extractCommand(localDoc));

    // Extract variable settings
    this.inputs = extractInputs(localDoc, this.stepParameters);
    this.outputs = extractOutputs(localDoc);

    this.inFileExpected = this.extractToolElementsIsFile(this.inputs);
    this.outFileExpected = this.extractToolElementsIsFile(this.outputs);

  }

  /**
   * Convert command tag from tool xml in string, variable are replace by value.
   * @param inputData the input data
   * @param outputData the output data
   * @return the string
   * @throws EoulsanException the Eoulsan exception
   * @throws IOException
   */
  public GalaxyToolResult execute(final StepContext context)
      throws EoulsanException {

    // TODO debug
    this.context = context;

    // Copy step tool element
    final Map<String, String> variablesCommand = this.extractVariablesFromXML();

    final Map<DataFormat, DataFile> inDataFiles = new HashMap<>();
    final Map<DataFormat, DataFile> outDataFiles = new HashMap<>();

    for (final Map.Entry<DataFormat, ToolElement> entry : this.inFileExpected
        .entrySet()) {

      final Data inData = context.getInputData(entry.getKey());
      final DataFile inFile = inData.getDataFile();

      for (final Map.Entry<DataFormat, ToolElement> e : this.outFileExpected
          .entrySet()) {

        final Data outData = context.getOutputData(entry.getKey(), inData);

        final DataFile outFile = outData.getDataFile();

        inDataFiles.put(entry.getKey(), inFile);
        outDataFiles.put(e.getKey(), outFile);
      }

    }

    // TODO
    System.out.println("ToolInterpreter, nb inData "
        + inDataFiles.size() + " out data " + outDataFiles
        + " nb variables found " + variablesCommand.size());

    // Add port with file path
    this.setPortInput(variablesCommand, inDataFiles);

    // Add port with file path
    this.setPortOutput(variablesCommand, outDataFiles);

    final ToolPythonInterpreter pythonInterperter =
        new ToolPythonInterpreter(context, this.tool, variablesCommand);

    final GalaxyToolResult result = pythonInterperter.executeScript();
    
    //TODO
    System.out.println("------------------ result in toolInter "+ result);
    return result;
  }

  /**
   * Check data format.
   * @param context the context
   * @return true, if successful
   */
  public boolean checkDataFormat(final StepContext context) {

    // Check inData
    for (final DataFormat indf : this.inFileExpected.keySet()) {

      final Data inData = context.getInputData(indf);

      // Case not found
      if (inData == null || inData.size() == 0)
        return false;

      for (final DataFormat outdf : this.outFileExpected.keySet()) {

        // Check outData related
        final Data outData = context.getOutputData(outdf, inData);
        if (outData == null || outData.size() == 0)
          return false;
      }
    }

    return true;
  }

  public String getDescription() {

    return "Launch tool galaxy "
        + this.tool.getToolName() + ", version " + this.tool.getToolVersion()
        + " with interpreter " + this.tool.getInterpreter();
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
   * Extract input or output tool elementd corresponding to file.
   * @param parameters all parameters extracted from tool xml file.
   * @return the map associated DataFormat and toolElement.
   */
  private Map<DataFormat, ToolElement> extractToolElementsIsFile(
      final Map<String, ToolElement> parameters) {

    final Map<DataFormat, ToolElement> results = new HashMap<>();

    // Parse parameters
    for (final Map.Entry<String, ToolElement> entry : parameters.entrySet()) {
      final ToolElement toolElement = entry.getValue();

      // Check tool element is a file
      if (toolElement.isFile()) {

        // Extract data format
        results.put(toolElement.getDataFormat(), toolElement);
      }
    }

    if (results.isEmpty())
      return Collections.emptyMap();

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
   * @param inputData the in data
   * @throws EoulsanException the Eoulsan exception
   * @throws IOException
   */
  private void setPortInput(final Map<String, String> variablesCommand,
      final Map<DataFormat, DataFile> inputData) throws EoulsanException {

    for (Map.Entry<DataFormat, DataFile> inData : inputData.entrySet()) {

      DataFormat inDataFormat = inData.getKey();

      final ToolElement inToolElement = this.inFileExpected.get(inDataFormat);

      // TODO
      // System.out.println("DEBUG in dataformat found in xml \t"
      // + Joiner.on(",").join(this.inDataFormatExpected.keySet()));
      // System.out.println("DEBUG Expected data format from context "
      // + inDataFormat);

      if (inToolElement == null) {
        throw new EoulsanException(
            "DEBUG Toolgalaxy, no toolelement found to set input port.");
      }

      // Initialize variable with path
      variablesCommand.put(inToolElement.getName(), inData.getValue().toFile()
          .getAbsolutePath());

    }

  }

  /**
   * Set output ports with name and value.
   * @param variablesCommand the variables command
   * @param outputData the out data
   * @throws EoulsanException the Eoulsan exception
   * @throws IOException
   */
  private void setPortOutput(final Map<String, String> variablesCommand,
      final Map<DataFormat, DataFile> outputData) throws EoulsanException {

    for (Map.Entry<DataFormat, DataFile> outData : outputData.entrySet()) {

      final DataFormat outDataFormat = outData.getKey();

      final ToolElement outToolElement =
          this.outFileExpected.get(outDataFormat);

      // Initialize variable with path
      variablesCommand.put(outToolElement.getName(), outData.getValue()
          .toFile().getAbsolutePath());

    }

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

    final Document localDoc = this.doc;

    for (final String tag : TAG_FORBIDDEN) {

      // Check tag exists in tool file
      if (!XMLUtils.getElementsByTagName(localDoc, tag).isEmpty()) {
        // Throw exception
        throw new EoulsanException("Parsing tool xml: unsupported tag " + tag);
      }
    }
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

    final ToolPythonInterpreter pythonInterperter =
        new ToolPythonInterpreter(this.context, this.tool,
            extractVariablesFromXML());

    final String newCommand = "";
    // pythonInterperter.createCommandLine();

    // TODO
    // System.out.println("DEBUG final command \t" + newCommand);

    return newCommand;
  }

  //
  // Getter
  //

  /**
   * Gets the in data format expected associated with variable found in command
   * line.
   * @return the in data format expected
   */
  public Map<DataFormat, ToolElement> getInDataFormatExpected() {
    return this.inFileExpected;
  }

  /**
   * Gets the out data format expected associated with variable found in command
   * line.
   * @return the out data format expected
   */
  public Map<DataFormat, ToolElement> getOutDataFormatExpected() {
    return this.outFileExpected;
  }

  public ToolData getToolData() {
    return this.tool;
  }

  @Override
  public String toString() {
    return "InterpreterToolGalaxy \n[inputs="
        + Joiner.on("\n").withKeyValueSeparator("=").join(this.inputs)
        + ", \noutputs="
        + Joiner.on("\n").withKeyValueSeparator("=").join(this.outputs)
        + ", \ntool=" + this.tool + "]";
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
    this("UNDEFINED", is, null);
  }

  public ToolInterpreter(final String toolName, final InputStream is,
      final File toolExecutablePath) throws EoulsanException {

    this.toolNameFromParameter = toolName;
    this.toolXMLis = is;
    this.doc = this.buildDOM();
    this.stepParameters = new HashMap<>();

    this.tool = new ToolData(toolExecutablePath);

    this.checkDomValidity();
  }

}
