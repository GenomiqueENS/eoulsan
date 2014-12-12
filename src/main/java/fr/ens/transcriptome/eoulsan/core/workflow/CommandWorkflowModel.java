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

package fr.ens.transcriptome.eoulsan.core.workflow;

import static fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowParser.AUTHOR_TAG_NAME;
import static fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowParser.DESCRIPTION_TAG_NAME;
import static fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowParser.DISCARDOUTPUT_ATTR_NAME_STEP_TAG;
import static fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowParser.FROMPORT_TAG_NAME;
import static fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowParser.FROMSTEP_TAG_NAME;
import static fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowParser.ID_ATTR_NAME_STEP_TAG;
import static fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowParser.INPUTS_TAG_NAMES;
import static fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowParser.INPUT_TAG_NAME;
import static fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowParser.NAME_TAG_NAME;
import static fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowParser.PARAMETERNAME_TAG_NAME;
import static fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowParser.PARAMETERS_TAG_NAME;
import static fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowParser.PARAMETERVALUE_TAG_NAME;
import static fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowParser.PARAMETER_TAG_NAME;
import static fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowParser.PORT_TAG_NAME;
import static fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowParser.ROOT_TAG_NAME;
import static fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowParser.SKIP_ATTR_NAME_STEP_TAG;
import static fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowParser.STEP_TAG_NAME;
import static fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowParser.WORKFLOWNAME_TAG_NAME;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflowParser.StepOutputPort;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType;

/**
 * This class define the workflow model object of Eoulsan.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class CommandWorkflowModel implements Serializable {

  /** Serialization version UID. */
  private static final long serialVersionUID = -5182569666862886788L;

  private String name = "";
  private String description = "";
  private String author = "";

  private final List<String> stepIdList = new ArrayList<>();
  private final Map<String, String> stepIdNames = new HashMap<>();
  private final Map<String, Map<String, StepPort>> stepInputs = new HashMap<>();
  private final Map<String, Set<Parameter>> stepParameters = new HashMap<>();
  private final Map<String, Boolean> stepSkiped = new HashMap<>();
  private final Map<String, Boolean> stepDiscardOutput = new HashMap<>();
  private final Set<Parameter> globalParameters = new HashSet<>();

  static final class StepPort implements Serializable {

    private static final long serialVersionUID = -1282360626885971051L;

    final String stepId;
    final String portName;

    private StepPort(final String stepId, final String portName) {

      this.stepId = stepId;
      this.portName = portName;
    }

  }

  //
  // Getters
  //

  /**
   * Get the name.
   * @return Returns the name
   */
  public String getName() {
    return this.name;
  }

  /**
   * Get description.
   * @return Returns the description
   */
  public String getDescription() {
    return this.description;
  }

  /**
   * Get Author.
   * @return Returns the author
   */
  public String getAuthor() {
    return this.author;
  }

  //
  // Setters
  //

  /**
   * Set the name
   * @param name The name to set
   */
  void setName(final String name) {

    if (name != null) {
      this.name = name;
    }
  }

  /**
   * Set the description
   * @param description The description to set
   */
  void setDescription(final String description) {

    if (description != null) {
      this.description = description;
    }
  }

  /**
   * Set the author.
   * @param author The author to set
   */
  void setAuthor(final String author) {

    if (author != null) {
      this.author = author;
    }
  }

  /**
   * Set globals parameters.
   * @param parameters parameters to set
   */
  void setGlobalParameters(final Set<Parameter> parameters) {

    this.globalParameters.addAll(parameters);
  }

  /**
   * Get the globals parameters.
   * @return a set of globals parameters
   */
  public Set<Parameter> getGlobalParameters() {

    return this.globalParameters;
  }

  /**
   * Add a step to the analysis
   * @param stepId id of the step
   * @param stepName name of the step to add
   * @param inputs where find step inputs
   * @param parameters parameters of the step
   * @param skipStep true if the step must be skip
   * @param discardOutput true if the output of the step can be removed
   * @throws EoulsanException if an error occurs while adding the step
   */
  void addStep(final String stepId, final String stepName,
      final Map<String, StepOutputPort> inputs,
      final Set<Parameter> parameters, final boolean skipStep,
      final boolean discardOutput) throws EoulsanException {

    if (stepName == null) {
      throw new EoulsanException("The name of the step is null.");
    }

    final String stepNameLower = stepName.toLowerCase().trim();

    if ("".equals(stepNameLower)) {
      throw new EoulsanException("The name of the step is empty.");
    }

    final String stepIdLower;
    if (stepId == null || "".equals(stepId.trim())) {
      stepIdLower = stepNameLower;
    } else {
      stepIdLower = stepId.toLowerCase().trim();
    }

    if ("".equals(stepIdLower)) {
      throw new EoulsanException("The id of the step is empty.");
    }

    if (!FileNaming.isStepIdValid(stepIdLower)) {
      throw new EoulsanException(
          "The id of the step is not valid (only ascii letters and digits are allowed): "
              + stepIdLower);
    }

    if (this.stepParameters.containsKey(stepIdLower)
        || StepType.getAllDefaultStepId().contains(stepIdLower)) {
      throw new EoulsanException("The step id already exists: " + stepIdLower);
    }

    if (parameters == null) {
      throw new EoulsanException("The parameters are null.");
    }

    if (inputs == null) {
      throw new EoulsanException("The inputs are null.");
    }

    // Check input data formats
    Map<String, StepPort> inputsMap = new HashMap<>();
    for (Map.Entry<String, StepOutputPort> e : inputs.entrySet()) {

      String toPortName = e.getKey();
      String fromStep = e.getValue().stepId;
      String fromPortName = e.getValue().outputPortName;

      if (toPortName == null) {
        throw new EoulsanException(
            "The input port name is null for input for step \"" + stepId);
      }
      if (fromStep == null) {
        throw new EoulsanException("The step name that generate \""
            + toPortName + "\" for step \"" + stepId + "\" is null");
      }
      if (fromPortName == null) {
        throw new EoulsanException("The output port name is null for input "
            + toPortName + " for step \"" + stepId);
      }

      toPortName = toPortName.trim().toLowerCase();
      fromStep = fromStep.trim().toLowerCase();
      fromPortName = fromPortName.trim().toLowerCase();

      if (!StepType.DESIGN_STEP.getDefaultStepId().equals(fromStep)
          && !this.stepIdNames.containsKey(fromStep)) {
        throw new EoulsanException("The step that generate \""
            + toPortName + "\" for step \"" + stepId
            + "\" has not been yet declared");
      }

      inputsMap.put(toPortName, new StepPort(fromStep, fromPortName));
    }

    this.stepIdList.add(stepIdLower);
    this.stepIdNames.put(stepIdLower, stepNameLower);
    this.stepInputs.put(stepIdLower, inputsMap);
    this.stepParameters.put(stepNameLower, parameters);
    this.stepSkiped.put(stepIdLower, skipStep);
    this.stepDiscardOutput.put(stepIdLower, discardOutput);
  }

  /**
   * Get the list of step ids.
   * @return a list of step ids
   */
  public List<String> getStepIds() {

    return this.stepIdList;
  }

  /**
   * Get the name of the step.
   * @param stepId step id
   * @return the name of the step
   */
  public String getStepName(final String stepId) {

    return this.stepIdNames.get(stepId);
  }

  /**
   * Get the inputs of a step
   * @param stepId the id of the step
   * @return a Map of with the inputs of the step
   */
  public Map<String, StepPort> getStepInputs(final String stepId) {

    Map<String, StepPort> result = this.stepInputs.get(stepId);

    if (result == null) {
      result = Collections.emptyMap();
    }

    return result;
  }

  /**
   * Get the parameters of a step
   * @param stepId the id of the step
   * @return a set of the parameters of the step
   */
  public Set<Parameter> getStepParameters(final String stepId) {

    Set<Parameter> result = this.stepParameters.get(stepId);

    if (result == null) {
      result = Collections.emptySet();
    }

    return result;
  }

  /**
   * Test if the step is skipped.
   * @param stepId step id
   * @return true if the step is skipped
   */
  public boolean isStepSkipped(final String stepId) {

    return this.stepSkiped.get(stepId);
  }

  /**
   * Test if the output of the step can be removed.
   * @param stepId step id
   * @return true if the output of the step can be removed
   */
  public boolean isStepDiscardOutput(final String stepId) {

    return this.stepDiscardOutput.get(stepId);
  }

  /**
   * Add a global parameter.
   * @param key key of the parameter
   * @param value value of the parameter
   */
  private void addGlobalParameter(final String key, final String value) {

    if (key == null || value == null) {
      return;
    }

    final String keyTrimmed = key.trim();
    final String valueTrimmed = value.trim();

    if ("".equals(keyTrimmed)) {
      return;
    }

    final Parameter p = new Parameter(keyTrimmed, valueTrimmed);
    this.globalParameters.add(p);
  }

  /**
   * Convert the object in XML.
   * @return the object as String in XML format
   */
  public String toXML() throws EoulsanException {

    try {
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

      // root elements
      Document doc = docBuilder.newDocument();
      Element rootElement = doc.createElement(ROOT_TAG_NAME);
      doc.appendChild(rootElement);

      // Header
      addElement(doc, rootElement,
          CommandWorkflowParser.FORMATVERSION_TAG_NAME,
          CommandWorkflowParser.FORMAT_VERSION);
      addElement(doc, rootElement, WORKFLOWNAME_TAG_NAME, getName());
      addElement(doc, rootElement, DESCRIPTION_TAG_NAME, getDescription());
      addElement(doc, rootElement, AUTHOR_TAG_NAME, getAuthor());

      // Step elements
      Element stepsElement =
          doc.createElement(CommandWorkflowParser.STEPS_TAG_NAME);
      rootElement.appendChild(stepsElement);

      for (String stepId : this.stepIdList) {
        addStepElement(doc, stepsElement, stepId);
      }

      // Global parameters
      addParametersElement(doc, rootElement,
          CommandWorkflowParser.GLOBALS_TAG_NAME, this.globalParameters);

      // write the content into xml file
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(
          "{http://xml.apache.org/xslt}indent-amount", "2");
      DOMSource source = new DOMSource(doc);
      StringWriter writer = new StringWriter();
      StreamResult result = new StreamResult(writer);

      transformer.transform(source, result);

      return writer.getBuffer().toString();
    } catch (ParserConfigurationException | TransformerException e) {
      throw new EoulsanRuntimeException(e.getMessage());
    }
  }

  /**
   * Add an Element to the XML document when creating XML output of the object.
   * @param document XML document
   * @param root root element
   * @param tagName tag name
   * @param tagValue tag value
   */
  private void addElement(final Document document, final Element root,
      final String tagName, final String tagValue) {

    if (document == null
        || root == null || tagName == null || "".equals(tagName.trim())
        || tagValue == null || "".equals(tagValue.trim())) {
      return;
    }

    Element e = document.createElement(tagName);
    e.appendChild(document.createTextNode(tagValue));
    root.appendChild(e);
  }

  /**
   * Add a parameters element to the XML document when creating XML output of
   * the object.
   * @param document XML document
   * @param root root element
   * @param elementName name of the parameters element
   * @param parameters the parameters to set
   */
  private void addParametersElement(final Document document,
      final Element root, final String elementName,
      final Set<Parameter> parameters) {

    if (document == null
        || root == null || parameters == null || elementName == null
        || "".equals(elementName.trim())) {
      return;
    }

    Element parametersElement = document.createElement(elementName);
    root.appendChild(parametersElement);
    for (Parameter p : parameters) {

      Element parameterElement = document.createElement(PARAMETER_TAG_NAME);
      parametersElement.appendChild(parameterElement);

      addElement(document, parameterElement, PARAMETERNAME_TAG_NAME,
          p.getName());
      addElement(document, parameterElement, PARAMETERVALUE_TAG_NAME,
          p.getValue());
    }

  }

  /**
   * Add a step element to the XML document when creating XML output of the
   * object.
   * @param document XML document
   * @param root root element
   * @param stepId step id
   */
  private void addStepElement(final Document document, final Element root,
      final String stepId) {

    if (document == null
        || root == null || stepId == null || "".equals(stepId.trim())) {
      return;
    }

    Element stepElement = document.createElement(STEP_TAG_NAME);
    root.appendChild(stepElement);

    // Set id attribute
    Attr idAttr = document.createAttribute(ID_ATTR_NAME_STEP_TAG);
    idAttr.setValue(stepId);
    stepElement.setAttributeNode(idAttr);

    // set discardOutput attribute
    Attr discardAttr =
        document.createAttribute(DISCARDOUTPUT_ATTR_NAME_STEP_TAG);
    discardAttr.setValue("" + this.stepDiscardOutput.get(stepId));
    stepElement.setAttributeNode(discardAttr);

    // Set skip attribute
    Attr skipAttr = document.createAttribute(SKIP_ATTR_NAME_STEP_TAG);
    skipAttr.setValue("" + this.stepSkiped.get(stepId));
    stepElement.setAttributeNode(skipAttr);

    // Set step name
    addElement(document, stepElement, NAME_TAG_NAME,
        this.stepIdNames.get(stepId));

    // Set step inputs
    Element inputsElement = document.createElement(INPUTS_TAG_NAMES);
    stepElement.appendChild(inputsElement);
    for (Map.Entry<String, StepPort> e : this.stepInputs.get(stepId).entrySet()) {

      Element inputElement = document.createElement(INPUT_TAG_NAME);
      inputsElement.appendChild(inputElement);

      addElement(document, inputElement, PORT_TAG_NAME, e.getKey());
      addElement(document, inputElement, FROMSTEP_TAG_NAME, e.getValue().stepId);
      addElement(document, inputElement, FROMPORT_TAG_NAME,
          e.getValue().portName);
    }

    // Set parameters
    addParametersElement(document, stepElement, PARAMETERS_TAG_NAME,
        this.stepParameters.get(stepId));
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public CommandWorkflowModel() {

    this(true);
  }

  /**
   * Public constructor.
   * @param addSettingsValues if all the settings must be added to global
   *          properties
   */
  public CommandWorkflowModel(final boolean addSettingsValues) {

    if (addSettingsValues) {

      final Settings settings = EoulsanRuntime.getRuntime().getSettings();

      for (String settingName : settings.getSettingsNames()) {
        addGlobalParameter(settingName, settings.getSetting(settingName));
      }
    }
  }

}
