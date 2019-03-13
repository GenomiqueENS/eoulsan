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

package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.Globals.APP_BUILD_DATE;
import static fr.ens.biologie.genomique.eoulsan.Globals.APP_BUILD_NUMBER;
import static fr.ens.biologie.genomique.eoulsan.Globals.APP_NAME_LOWER_CASE;
import static fr.ens.biologie.genomique.eoulsan.Globals.APP_VERSION_STRING;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.ProcessUtils;

/**
 * This class allow parse the workflow file.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class CommandWorkflowParser {

  /** Version constant name. */
  public static final String VERSION_CONSTANT_NAME =
      APP_NAME_LOWER_CASE + ".version";
  /** Build number constant name. */
  public static final String BUILD_NUMBER_CONSTANT_NAME =
      APP_NAME_LOWER_CASE + ".build.number";
  /** Build date constant name. */
  public static final String BUILD_DATE_CONSTANT_NAME =
      APP_NAME_LOWER_CASE + ".build.date";
  /** Available processor constant name. */
  public static final String AVAILABLE_PROCESSORS_CONSTANT_NAME =
      APP_NAME_LOWER_CASE + "available.processors";
  /** Design file path constant name. */
  public static final String DESIGN_FILE_PATH_CONSTANT_NAME =
      "design.file.path";
  /** Parameters file path constant name. */
  public static final String WORKFLOW_FILE_PATH_CONSTANT_NAME =
      "workflow.file.path";
  /** Output path constant name. */
  public static final String OUTPUT_PATH_CONSTANT_NAME = "output.path";
  /** Job id constant name. */
  public static final String JOB_ID_CONSTANT_NAME = "job.id";
  /** Job UUID constant name. */
  public static final String JOB_UUID_CONSTANT_NAME = "job.uuid";
  /** Logs path constant name. */
  public static final String JOB_PATH_CONSTANT_NAME = "job.path";

  /** Version of the format of the workflow file. */
  static final String FORMAT_VERSION = "1.0";

  // Tag and attribute names
  static final String WORKFLOWNAME_TAG_NAME = "name";
  static final String PARAMETERNAME_TAG_NAME = "name";
  static final String PARAMETERVALUE_TAG_NAME = "value";
  static final String PARAMETER_TAG_NAME = "parameter";
  static final String FROMPORT_TAG_NAME = "fromport";
  static final String FROMSTEP_TAG_NAME = "fromstep";
  static final String PORT_TAG_NAME = "port";
  static final String INPUT_TAG_NAME = "input";
  static final String INPUTS_TAG_NAME = "inputs";
  static final String GLOBALS_TAG_NAME = "globals";
  static final String PARAMETERS_TAG_NAME = "parameters";
  static final String STEPNAME_TAG_NAME = "stepname";
  static final String MODULE_TAG_NAME = "module";
  static final String DISCARDOUTPUT_ATTR_NAME_STEP_TAG = "discardoutput";
  static final String SKIP_ATTR_NAME_STEP_TAG = "skip";
  static final String REQUIRED_MEM_ATTR_NAME_STEP_TAG = "requiredmemory";
  static final String REQUIRED_CPU_ATTR_NAME_STEP_TAG = "requiredprocs";
  static final String DATAPRODUCT_ATTR_NAME_STEP_TAG = "dataproduct";
  static final String ID_ATTR_NAME_STEP_TAG = "id";
  static final String VERSION_TAG = "version";
  static final String STEP_TAG_NAME = "step";
  static final String STEPS_TAG_NAME = "steps";
  static final String CONSTANTS_TAG_NAME = "constants";
  static final String AUTHOR_TAG_NAME = "author";
  static final String DESCRIPTION_TAG_NAME = "description";
  static final String NAME_TAG_NAME = "name";
  static final String FORMATVERSION_TAG_NAME = "formatversion";
  static final String ROOT_TAG_NAME = "analysis";

  private final InputStream is;

  private final Map<String, String> constants = initConstants();

  public static final class StepOutputPort {

    public final String stepId;
    public final String outputPortName;

    public StepOutputPort(final String stepId, final String outputPortName) {

      this.stepId = stepId == null ? null : stepId.trim();
      this.outputPortName =
          outputPortName == null ? null : outputPortName.trim();
    }
  }

  /**
   * Parse the workflow file.
   * @throws EoulsanException if an error occurs while parsing file
   */
  public CommandWorkflowModel parse() throws EoulsanException {

    getLogger().info("Start parsing the workflow workflow file");

    final CommandWorkflowModel result = new CommandWorkflowModel();
    final Document doc;

    //
    // Init parser
    //

    try {

      final DocumentBuilderFactory dbFactory =
          DocumentBuilderFactory.newInstance();
      final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      doc = dBuilder.parse(this.is);
      doc.getDocumentElement().normalize();

    } catch (ParserConfigurationException | SAXException e) {
      throw new EoulsanException(
          "Error while parsing workflow file: " + e.getMessage(), e);
    } catch (IOException e) {
      throw new EoulsanException(
          "Error while reading workflow file. " + e.getMessage(), e);
    }

    final NodeList nAnalysisList = doc.getElementsByTagName(ROOT_TAG_NAME);

    for (int i = 0; i < nAnalysisList.getLength(); i++) {

      Node nNode = nAnalysisList.item(i);
      if (nNode.getNodeType() == Node.ELEMENT_NODE) {

        Element eElement = (Element) nNode;

        // Check allowed child tags of the root tag
        checkAllowedChildTags(eElement, FORMATVERSION_TAG_NAME,
            WORKFLOWNAME_TAG_NAME, DESCRIPTION_TAG_NAME, AUTHOR_TAG_NAME,
            CONSTANTS_TAG_NAME, STEPS_TAG_NAME, GLOBALS_TAG_NAME);

        //
        // Parse description elements
        //

        final String formatVersion =
            getTagValue(FORMATVERSION_TAG_NAME, eElement);
        if (formatVersion == null || !FORMAT_VERSION.equals(formatVersion)) {
          throw new EoulsanException(
              "Invalid version of the format of the workflow file.");
        }

        final String name = getTagValue(WORKFLOWNAME_TAG_NAME, eElement);
        result.setName(name);

        final String description = getTagValue(DESCRIPTION_TAG_NAME, eElement);
        result.setDescription(description);

        final String author = getTagValue(AUTHOR_TAG_NAME, eElement);
        result.setAuthor(author);

        //
        // Parse constants
        //

        addConstants(
            parseParameters(eElement, CONSTANTS_TAG_NAME, null, false));

        //
        // Parse steps
        //

        final NodeList nStepsList =
            eElement.getElementsByTagName(STEPS_TAG_NAME);

        for (int j = 0; j < nStepsList.getLength(); j++) {

          final Node nodeSteps = nStepsList.item(j);
          if (nodeSteps.getNodeType() == Node.ELEMENT_NODE) {

            final Element stepsElement = (Element) nodeSteps;

            // Check allowed child tag for the "steps" tag
            checkAllowedChildTags(stepsElement, STEP_TAG_NAME);

            final NodeList nStepList =
                stepsElement.getElementsByTagName(STEP_TAG_NAME);

            for (int k = 0; k < nStepList.getLength(); k++) {

              final Node nStepNode = nStepList.item(k);
              if (nStepNode.getNodeType() == Node.ELEMENT_NODE) {

                final Element eStepElement = (Element) nStepNode;

                // Check allowed attributes for the "step" tag
                checkAllowedAttributes(eStepElement, ID_ATTR_NAME_STEP_TAG,
                    SKIP_ATTR_NAME_STEP_TAG, REQUIRED_MEM_ATTR_NAME_STEP_TAG,
                    REQUIRED_CPU_ATTR_NAME_STEP_TAG,
                    REQUIRED_CPU_ATTR_NAME_STEP_TAG,
                    DISCARDOUTPUT_ATTR_NAME_STEP_TAG,
                    DATAPRODUCT_ATTR_NAME_STEP_TAG);

                // Check allowed child tag of the the "step" tag
                checkAllowedChildTags(eStepElement, MODULE_TAG_NAME,
                    STEPNAME_TAG_NAME, NAME_TAG_NAME, VERSION_TAG,
                    INPUTS_TAG_NAME, PARAMETERS_TAG_NAME);

                final String stepId =
                    getAttribute(eStepElement, ID_ATTR_NAME_STEP_TAG).trim()
                        .toLowerCase();

                final boolean skip = Boolean.parseBoolean(
                    getAttribute(eStepElement, SKIP_ATTR_NAME_STEP_TAG).trim()
                        .toLowerCase());

                final int requiredMemory =
                    parseMemory(getAttribute(eStepElement,
                        REQUIRED_MEM_ATTR_NAME_STEP_TAG));

                final int requiredProcs =
                    parseProcessors(getAttribute(eStepElement,
                        REQUIRED_CPU_ATTR_NAME_STEP_TAG));

                final Step.DiscardOutput discardOutput =
                    Step.DiscardOutput.parse(getAttribute(eStepElement,
                        DISCARDOUTPUT_ATTR_NAME_STEP_TAG));

                final String dataProduct =
                    getAttribute(eStepElement, DATAPRODUCT_ATTR_NAME_STEP_TAG)
                        .trim();

                // Parse the module tags
                String module = getTagValue(MODULE_TAG_NAME, eStepElement);
                if (module == null) {
                  module = getTagValue(STEPNAME_TAG_NAME, eStepElement);
                }
                if (module == null) {
                  module = getTagValue(NAME_TAG_NAME, eStepElement);
                }
                if (module == null) {
                  throw new EoulsanException(
                      "Module name not found in workflow file.");
                }

                final String version = getTagValue(VERSION_TAG, eStepElement);

                final Map<String, StepOutputPort> inputs = parseInputs(
                    eStepElement, "".equals(stepId) ? module : stepId);

                final Set<Parameter> parameters = parseParameters(eStepElement,
                    PARAMETERS_TAG_NAME, module, true);

                getLogger().info("In workflow file found "
                    + module + " step (parameters: " + parameters + ").");
                result.addStep(stepId, module, version, inputs, parameters,
                    skip, discardOutput, requiredMemory, requiredProcs,
                    dataProduct);
              }
            }
          }
        }

        //
        // Parse globals parameters
        //

        result.setGlobalParameters(
            parseParameters(eElement, GLOBALS_TAG_NAME, null, true));

      }
    }

    getLogger().info("End of parsing of workflow file");
    getLogger().info(
        "Found " + result.getStepIds().size() + " step(s) in workflow file");

    return result;
  }

  /**
   * Parse inputs sections
   * @param root root element to parse
   * @param stepId step id for the exception message
   * @throws EoulsanException if the tags of the parameter are not found
   */
  private Map<String, StepOutputPort> parseInputs(final Element root,
      final String stepId) throws EoulsanException {

    final Map<String, StepOutputPort> result = new HashMap<>();

    final NodeList nList = root.getElementsByTagName(INPUTS_TAG_NAME);

    for (int i = 0; i < nList.getLength(); i++) {

      final Node node = nList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {

        Element element = (Element) node;

        // Check allowed child tag for the "inputs" tag
        checkAllowedChildTags(element, INPUT_TAG_NAME);

        final NodeList nParameterList =
            element.getElementsByTagName(INPUT_TAG_NAME);

        for (int j = 0; j < nParameterList.getLength(); j++) {

          final Node nParameterNode = nParameterList.item(j);

          if (nParameterNode.getNodeType() == Node.ELEMENT_NODE) {

            Element inputElement = (Element) nParameterNode;

            // Check allowed child tag for the "input" tag
            checkAllowedChildTags(inputElement, PORT_TAG_NAME,
                FROMSTEP_TAG_NAME, FROMPORT_TAG_NAME);

            // Get and check the toInput attribute
            final String portName = getTagValue(PORT_TAG_NAME, inputElement);
            if (portName == null) {
              throw new EoulsanException(
                  "the \"toInput\" attribute not exists in an input section of step \""
                      + stepId + "\" in workflow file.");
            }
            if (portName.isEmpty()) {
              throw new EoulsanException(
                  "the \"toInput\" attribute is empty in an input section of step \""
                      + stepId + "\" in workflow file.");
            }
            if (result.containsKey(portName)) {
              throw new EoulsanException("an input for "
                  + portName
                  + " port has been already defined in the inputs section of step \""
                  + stepId + "\" in workflow file.");
            }

            final StepOutputPort input =
                new StepOutputPort(getTagValue(FROMSTEP_TAG_NAME, inputElement),
                    getTagValue(FROMPORT_TAG_NAME, inputElement));

            // Check step ID
            if (input.stepId == null) {
              throw new EoulsanException(
                  "the \"fromStep\" attribute has not been defined for "
                      + portName + " input in section of step \"" + stepId
                      + "\" in workflow file.");
            }
            if (input.stepId.isEmpty()) {
              throw new EoulsanException(
                  "the \"fromStep\" attribute is empty for "
                      + portName + " input in section of step \"" + stepId
                      + "\" in workflow file.");
            }

            // Check port ID
            if (input.outputPortName == null) {
              throw new EoulsanException(
                  "the \"fromPort\" attribute has not been defined for "
                      + portName + " input in section of step \"" + stepId
                      + "\" in workflow file.");
            }
            if (input.outputPortName.isEmpty()) {
              throw new EoulsanException(
                  "the \"fromPort\" attribute is empty for "
                      + portName + " input in section of step \"" + stepId
                      + "\" in workflow file.");
            }

            result.put(portName, input);
          }
        }
      }
    }

    return result;
  }

  /**
   * Parse parameter sections
   * @param root root element to parse
   * @param elementName name of the element
   * @param evaluateValues evaluate parameters values
   * @return a set of Parameter object
   * @throws EoulsanException if the tags of the parameter are not found
   */
  private Set<Parameter> parseParameters(final Element root,
      final String elementName, final String stepName,
      final boolean evaluateValues) throws EoulsanException {

    final Set<Parameter> result = new LinkedHashSet<>();
    final Set<String> parameterNames = new HashSet<>();

    final NodeList nList = root.getElementsByTagName(elementName);

    for (int i = 0; i < nList.getLength(); i++) {

      final Node node = nList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {

        Element element = (Element) node;

        // Check allowed tags for the "parameter" tag
        checkAllowedChildTags(element, PARAMETER_TAG_NAME);

        final NodeList nParameterList =
            element.getElementsByTagName(PARAMETER_TAG_NAME);

        for (int j = 0; j < nParameterList.getLength(); j++) {

          final Node nParameterNode = nParameterList.item(j);

          if (nParameterNode.getNodeType() == Node.ELEMENT_NODE) {

            Element eParameterElement = (Element) nParameterNode;

            checkAllowedChildTags(eParameterElement, PARAMETERNAME_TAG_NAME,
                PARAMETERVALUE_TAG_NAME);

            final String paramName =
                getTagValue(PARAMETERNAME_TAG_NAME, eParameterElement);
            final String paramValue =
                getTagValue(PARAMETERVALUE_TAG_NAME, eParameterElement);

            if (paramName == null) {
              throw new EoulsanException(
                  "<name> Tag not found in parameter section of "
                      + (stepName == null
                          ? "global parameters" : stepName + " step")
                      + " in workflow file.");
            }
            if (paramValue == null) {
              throw new EoulsanException(
                  "<value> Tag not found in parameter section of "
                      + (stepName == null
                          ? "global parameters" : stepName + " step")
                      + " in workflow file.");
            }

            if (parameterNames.contains(paramName)) {
              throw new EoulsanException("The parameter \""
                  + paramName + "\" has been already defined for "
                  + (stepName == null
                      ? "global parameters" : stepName + " step")
                  + " in workflow file.");
            }
            parameterNames.add(paramName);

            result.add(new Parameter(paramName, evaluateValues
                ? evaluateExpressions(paramValue, true) : paramValue));
          }
        }

      }
    }

    return result;
  }

  /**
   * Get the value of a tag
   * @param tag name of the tag
   * @param element root element
   * @return the value of the tag
   */
  private static String getTagValue(final String tag, final Element element) {

    final NodeList nl = element.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {

      final Node n = nl.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE && tag.equals(n.getNodeName())) {
        return n.getTextContent();
      }
    }

    return null;
  }

  /**
   * Check if the attribute of a tag is in a list of allowed attribute names.
   * @param element the tag element
   * @param attributeNames the allowed attribute names
   * @throws EoulsanException if an attribute of the tag in not in the allowed
   *           attribute list
   */
  private static void checkAllowedAttributes(final Element element,
      String... attributeNames) throws EoulsanException {

    final List<String> attributeList = Arrays.asList(attributeNames);

    final NamedNodeMap nnm = element.getAttributes();

    for (int i = 0; i < nnm.getLength(); i++) {

      final Node n = nnm.item(i);

      if (n.getNodeType() == Node.ATTRIBUTE_NODE) {

        final String attributeName = n.getNodeName();

        if (!attributeList.contains(attributeName)) {
          throw new EoulsanException("the \""
              + element.getNodeName() + "\" tag contains an unknown attribute: "
              + attributeName + ".");
        }
      }
    }
  }

  /**
   * Check if the child tags of a tag is in a list of allowed tag names.
   * @param element the tag element
   * @param tagNames the allowed tag names
   * @throws EoulsanException if a child tag of the tag in not in the allowed
   *           tag list
   */
  private static void checkAllowedChildTags(final Element element,
      String... tagNames) throws EoulsanException {

    final List<String> tagList = Arrays.asList(tagNames);

    final NodeList nl = element.getChildNodes();

    for (int i = 0; i < nl.getLength(); i++) {

      final Node n = nl.item(i);

      if (n.getNodeType() == Node.ELEMENT_NODE) {

        final String childTagName = n.getNodeName();

        if (!tagList.contains(childTagName)) {
          throw new EoulsanException("the \""
              + element.getNodeName() + "\" tag contains an unknown tag: "
              + childTagName + ".");
        }
      }
    }
  }

  //
  // Constants handling
  //

  /**
   * Set the constants values
   * @param parameters a set with the parameters
   * @throws EoulsanException if an error occurs while evaluating the parameters
   */
  private void addConstants(final Set<Parameter> parameters)
      throws EoulsanException {

    if (parameters == null) {
      return;
    }

    for (Parameter p : parameters) {
      if (!"".equals(p.getName())) {
        addConstant(p.getName(), p.getValue(), true);
      }
    }
  }

  /**
   * Add job arguments information to constants.
   * @param arguments job arguments
   * @throws EoulsanException if an error occurs while evaluating the constant
   */
  public void addConstants(final ExecutorArguments arguments)
      throws EoulsanException {

    if (arguments == null) {
      return;
    }

    addConstant(DESIGN_FILE_PATH_CONSTANT_NAME, arguments.getDesignPathname());
    addConstant(WORKFLOW_FILE_PATH_CONSTANT_NAME,
        arguments.getWorkflowPathname());
    addConstant(OUTPUT_PATH_CONSTANT_NAME, arguments.getOutputPathname());
    addConstant(JOB_ID_CONSTANT_NAME, arguments.getJobId());
    addConstant(JOB_UUID_CONSTANT_NAME, arguments.getJobUUID());
    addConstant(JOB_PATH_CONSTANT_NAME, arguments.getJobPathname());
  }

  /**
   * Add a constant.
   * @param constantName constant Name
   * @param constantValue constant value
   * @throws EoulsanException if an error occurs while evaluating the constant
   */
  public void addConstant(final String constantName, final String constantValue)
      throws EoulsanException {

    addConstant(constantName, constantValue, false);
  }

  /**
   * Add a constant.
   * @param constantName constant Name
   * @param constantValue constant value
   * @param evaluateValue allow evaluate the value of the constant and start an
   *          external process to do this
   * @throws EoulsanException if an error occurs while evaluating the constant
   */
  public void addConstant(final String constantName, final String constantValue,
      final boolean evaluateValue) throws EoulsanException {

    if (constantName == null || constantValue == null) {
      return;
    }

    if (evaluateValue) {
      this.constants.put(constantName.trim(),
          evaluateExpressions(constantValue, true));
    } else {
      this.constants.put(constantName.trim(), constantValue);
    }
  }

  /**
   * Initialize the constants values.
   * @return a map with the constants
   */
  private static Map<String, String> initConstants() {

    final Map<String, String> constants = new HashMap<>();

    constants.put(VERSION_CONSTANT_NAME, APP_VERSION_STRING);
    constants.put(BUILD_NUMBER_CONSTANT_NAME, APP_BUILD_NUMBER);
    constants.put(BUILD_DATE_CONSTANT_NAME, APP_BUILD_DATE);

    constants.put(AVAILABLE_PROCESSORS_CONSTANT_NAME,
        "" + Runtime.getRuntime().availableProcessors());

    // Add java properties
    for (Map.Entry<Object, Object> e : System.getProperties().entrySet()) {
      constants.put((String) e.getKey(), (String) e.getValue());
    }

    // Add environment properties
    for (Map.Entry<String, String> e : System.getenv().entrySet()) {
      constants.put(e.getKey(), e.getValue());
    }

    return constants;
  }

  /**
   * Evaluate expression in a string.
   * @param s string in witch expression must be replaced
   * @param allowExec allow execution of code
   * @return a string with expression evaluated
   * @throws EoulsanException if an error occurs while parsing the string or
   *           executing an expression
   */
  private String evaluateExpressions(final String s, final boolean allowExec)
      throws EoulsanException {

    if (s == null) {
      return null;
    }

    final StringBuilder result = new StringBuilder();

    final int len = s.length();

    for (int i = 0; i < len; i++) {

      final int c0 = s.codePointAt(i);

      // Variable substitution
      if (c0 == '$' && i + 1 < len) {

        final int c1 = s.codePointAt(i + 1);
        if (c1 == '{') {

          final String expr = subStr(s, i + 2, '}');

          final String trimmedExpr = expr.trim();
          if (this.constants.containsKey(trimmedExpr)) {
            result.append(this.constants.get(trimmedExpr));
          }

          i += expr.length() + 2;
          continue;
        }
      }

      // Command substitution
      if (c0 == '`' && allowExec) {
        final String expr = subStr(s, i + 1, '`');
        try {
          final String r =
              ProcessUtils.execToString(evaluateExpressions(expr, false));

          // remove last '\n' in the result
          if (r.charAt(r.length() - 1) == '\n') {
            result.append(r.substring(0, r.length() - 1));
          } else {
            result.append(r);
          }

        } catch (IOException e) {
          throw new EoulsanException(
              "Error while evaluating expression \"" + expr + "\"", e);
        }
        i += expr.length() + 1;
        continue;
      }

      result.appendCodePoint(c0);
    }

    return result.toString();
  }

  private String subStr(final String s, final int beginIndex,
      final int charPoint) throws EoulsanException {

    final int endIndex = s.indexOf(charPoint, beginIndex);

    if (endIndex == -1) {
      throw new EoulsanException(
          "Unexpected end of expression in \"" + s + "\"");
    }

    return s.substring(beginIndex, endIndex);
  }

  //
  // Parsing methods
  //

  /**
   * Get the value of an XML tag attribute and evaluate it if necessary.
   * @param eStepElement the XML tag element
   * @param name the name of the attribute
   * @return the value of the attribute
   * @throws EoulsanException if an error occurs while getting the value of the
   *           attribute
   */
  private String getAttribute(Element eStepElement, final String name)
      throws EoulsanException {

    return evaluateExpressions(eStepElement.getAttribute(name), true);
  }

  /**
   * Parse the required processors attribute.
   * @param s the string to parse
   * @return the processor count required in MB
   * @throws EoulsanException if the string if not valid
   */
  private static int parseMemory(final String s) throws EoulsanException {

    if (s == null) {
      return -1;
    }

    final String value = s.replace(" ", "").trim().toLowerCase();

    if (s.isEmpty()) {
      return -1;
    }

    final Pattern pattern = Pattern.compile("^(\\d+)([a-z]*)$");

    final Matcher matcher = pattern.matcher(value);

    if (!matcher.matches()) {
      throw new EoulsanException("Invalid memory value in "
          + REQUIRED_MEM_ATTR_NAME_STEP_TAG + " attribute of tag "
          + STEP_TAG_NAME + ": " + s);
    }

    int result;

    try {
      result = Integer.parseInt(matcher.group(1));
    } catch (NumberFormatException e) {
      throw new EoulsanException("Invalid memory value in "
          + REQUIRED_MEM_ATTR_NAME_STEP_TAG + " attribute of tag "
          + STEP_TAG_NAME + ": " + s);
    }

    switch (matcher.group(2)) {

    case "mib":
    case "mb":
    case "m":
    case "":
      break;

    case "gib":
    case "gb":
    case "g":
      result *= 1024;
      break;

    default:
      throw new EoulsanException("Invalid memory value in "
          + REQUIRED_MEM_ATTR_NAME_STEP_TAG + " attribute of tag "
          + STEP_TAG_NAME + ": " + s);
    }

    return result;
  }

  /**
   * Parse the required processors attribute.
   * @param s the string to parse
   * @return the processor count required
   * @throws EoulsanException if the string if not valid
   */
  private static int parseProcessors(final String s) throws EoulsanException {

    if (s == null) {
      return -1;
    }

    final String value = s.trim();

    if (value.isEmpty()) {
      return -1;
    }

    try {
      return Integer.parseInt(s.trim());
    } catch (NumberFormatException e) {
      throw new EoulsanException("Invalid processor count in "
          + REQUIRED_CPU_ATTR_NAME_STEP_TAG + " attribute of tag "
          + STEP_TAG_NAME + ": " + s);
    }
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param file the workflow file
   * @throws FileNotFoundException if the file is not found
   */
  public CommandWorkflowParser(final File file) throws FileNotFoundException {

    this(FileUtils.createInputStream(file));
  }

  /**
   * Public constructor.
   * @param file the workflow file
   * @throws IOException if an error occurs while opening the file
   */
  public CommandWorkflowParser(final DataFile file) throws IOException {

    this(file.open());
  }

  /**
   * Public constructor.
   * @param is Input stream
   */
  public CommandWorkflowParser(final InputStream is) {

    this.is = is;
  }

}
