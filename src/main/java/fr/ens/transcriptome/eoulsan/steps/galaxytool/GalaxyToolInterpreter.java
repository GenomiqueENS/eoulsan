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
import static org.python.google.common.base.Preconditions.checkNotNull;
import static org.python.google.common.base.Preconditions.checkState;

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
public class GalaxyToolInterpreter implements ToolInterpreter {

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

  /** The step parameters. */
  private final Map<String, Parameter> stepParameters;

  /** The in data format expected. */
  private Map<DataFormat, ToolElement> inFileExpected;

  /** The out data format expected. */
  private Map<DataFormat, ToolElement> outFileExpected;

  /** The tool. */
  private final ToolData tool;

  private boolean isConfigured = false;
  private boolean isExecuted = false;

  @Override
  public void configure(final Set<Parameter> setStepParameters)
      throws EoulsanException {

    checkState(!isConfigured,
        "GalaxyToolStep, this instance has been already configured");

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

    isConfigured = true;
  }

  @Override
  public ToolExecutorResult execute(final StepContext context)
      throws EoulsanException {

    checkState(!isExecuted,
        "GalaxyToolStep, this instance has been already executed");

    context.getLogger().info("Parsing xml file successfully.");
    context.getLogger().info("Tool description " + this.tool);

    final int variablesCount = this.inputs.size() + this.outputs.size();
    final Map<String, String> variables = new HashMap<>(variablesCount);

    Data inData = null;
    int inDataFileFoundCount = 0;

    // Extract from inputs variable command
    for (final ToolElement ptg : this.inputs.values()) {

      if (ptg.isFile()) {

        // Extract value from context from DataFormat
        inData = context.getInputData(ptg.getDataFormat());

        if (inData != null) {

          boolean multiInData = inData.getDataFileCount() > 1;

          final DataFile dataFile =
              (multiInData
                  ? inData.getDataFile(inDataFileFoundCount++) : inData
                      .getDataFile());

          variables.put(ptg.getName(), dataFile.toFile().getAbsolutePath());
        }

      } else {
        // Variables setting with parameters file
        variables.put(ptg.getName(), ptg.getValue());
      }
    }

    // Extract from outputs variable command
    for (final ToolElement ptg : this.outputs.values()) {

      if (ptg.isFile()) {

        // Extract value from context from DataFormat
        final Data outData = context.getOutputData(ptg.getDataFormat(), inData);

        if (outData != null) {
          variables.put(ptg.getName(), outData.getDataFile().toFile()
              .getAbsolutePath());
        }
      } else {
        // Variables setting with parameters file
        variables.put(ptg.getName(), ptg.getValue());
      }
    }

    if (variables.isEmpty()) {
      throw new EoulsanException("No parameter settings.");
    }

    context.getLogger().info(
        "Tool variable settings  "
            + Joiner.on("\t").withKeyValueSeparator("=").join(variables));

    final ToolPythonInterpreter pythonInterpreter =
        new ToolPythonInterpreter(context, this.tool,
            Collections.unmodifiableMap(variables));

    final ToolExecutorResult result = pythonInterpreter.executeScript();

    isExecuted = true;

    // TODO
    return result;
  }

  /**
   * Check data format.
   * @param context the context
   * @return true, if successful
   */
  public boolean checkDataFormat(final StepContext context) {

    // Check inData
    for (final DataFormat inFormat : this.inFileExpected.keySet()) {

      final Data inData = context.getInputData(inFormat);

      // Case not found
      if (inData == null || inData.size() == 0)
        return false;

      for (final DataFormat outFormat : this.outFileExpected.keySet()) {

        // Check outData related
        final Data outData = context.getOutputData(outFormat, inData);
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

  //
  // Private methods
  //

  /**
   * Extract input or output tool elements corresponding to file.
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

    } catch (final IOException | SAXException | ParserConfigurationException e) {
      throw new EoulsanException(e);
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
  // Getters
  //

  /**
   * Gets the inputs.
   * @return the inputs
   */
  public Map<String, ToolElement> getInputs() {
    return this.inputs;
  }

  /**
   * Gets the outputs.
   * @return the outputs
   */
  public Map<String, ToolElement> getOutputs() {
    return this.outputs;
  }

  @Override
  public Map<DataFormat, ToolElement> getInDataFormatExpected() {
    return this.inFileExpected;
  }

  @Override
  public Map<DataFormat, ToolElement> getOutDataFormatExpected() {
    return this.outFileExpected;
  }

  @Override
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
   * @param is the input stream
   * @throws EoulsanException the Eoulsan exception
   */
  public GalaxyToolInterpreter(final InputStream is) throws EoulsanException {

    checkNotNull(is, "input stream on XML file");

    this.toolXMLis = is;
    this.doc = this.buildDOM();
    this.stepParameters = new HashMap<>();

    this.tool = new ToolData();

    this.checkDomValidity();
  }
}
