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

import static com.google.common.base.Preconditions.checkState;
import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.extractInputs;
import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.extractOutputs;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Naming;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.eoulsan.galaxytools.elements.DataToolElement;
import fr.ens.biologie.genomique.eoulsan.galaxytools.elements.ToolElement;
import fr.ens.biologie.genomique.kenetre.util.XMLUtils;

/**
 * This class create an interpreter to tool xml file from Galaxy.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class GalaxyToolInterpreter {

  /** The tool xm lis. */
  private final InputStream toolXMLis;

  /** The Constant TAG_FORBIDDEN. */
  private final static Set<String> TAG_FORBIDDEN = Sets.newHashSet("repeat");

  private static final String TMP_DIR_VARIABLE_NAME = "TMPDIR";
  private static final String THREADS_VARIABLE_NAME = "THREADS";

  // Set DOM related to the tool XML file
  /** The doc. */
  private final Document doc;

  /** Data from tool XML. */
  private Map<String, ToolElement> inputs;

  /** The outputs. */
  private Map<String, ToolElement> outputs;

  private ElementPorts inputPorts;
  private ElementPorts outputPorts;

  /** The tool information. */
  private final ToolInfo toolInfo;

  private boolean isConfigured = false;
  private boolean isExecuted = false;

  //
  // Inner classes
  //

  /**
   * This inner class define the link between an Element an Eoulsan step port.
   */
  private static final class ElementPort {

    private final DataToolElement element;

    private final String portName;

    private final int fileIndex;

    /**
     * Get the DataFile linked to the Element.
     * @param context Step context
     * @return a DataFile object
     */
    public DataFile getInputDataFile(final TaskContext context) {

      final Data data = context.getInputData(this.portName);

      return this.fileIndex == -1
          ? data.getDataFile() : data.getDataFile(this.fileIndex);
    }

    /**
     * Get the DataFile linked to the Element.
     * @param context Step context
     * @return a DataFile object
     */
    public DataFile getOutputDataFile(final TaskContext context,
        final Data inData) {

      final Data data = context.getOutputData(this.portName, inData);

      return this.fileIndex == -1
          ? data.getDataFile() : data.getDataFile(this.fileIndex);
    }

    @Override
    public String toString() {
      return "ElementPort{element="
          + element.getName() + ", portName=" + portName + ", fileIndex="
          + fileIndex + "}";
    }

    /**
     * Constructor.
     * @param element Tool element
     * @param portName Eoulsan port name
     * @param fileIndex file index
     */
    public ElementPort(final DataToolElement element, final String portName,
        final int fileIndex) {

      this.element = element;
      this.portName = portName;
      this.fileIndex = fileIndex;
    }
  }

  /**
   * This inner class define a collection of ElementPorts.
   */
  private static final class ElementPorts {

    private final Map<String, ElementPort> ports = new HashMap<>();

    /**
     * Get an ElementPort from its name.
     * @param elementName the name of the element port
     * @return an ElementPort or null if the element name does not exists
     */
    public ElementPort getPortElements(final String elementName) {

      return this.ports.get(elementName);
    }

    /**
     * Get the ToolElement objects that will be used to create the Eoulsan step
     * ports. Only one of the port of multi-files DataFormat are kept.
     * @return a set of ToolElement
     */
    public Set<DataToolElement> getStepElements() {

      final Set<DataToolElement> result = new HashSet<>();

      for (ElementPort e : ports.values()) {

        if (e.fileIndex < 1) {
          result.add(e.element);
        }
      }

      return Collections.unmodifiableSet(result);
    }

    /**
     * Sort ToolElements.
     * @param elements element to sort
     * @return a sorted list of ToolElement
     */
    private static List<ToolElement> sortedElements(
        final Collection<ToolElement> elements) {

      final List<ToolElement> elementsSorted = new ArrayList<>(elements);
      elementsSorted.sort(Comparator.comparing(ToolElement::getName));

      return Collections.unmodifiableList(elementsSorted);
    }

    @Override
    public String toString() {
      return this.ports.toString();
    }

    /**
     * Constructor.
     * @param elements the element
     */
    public ElementPorts(final Map<String, ToolElement> elements) {

      final Multiset<DataFormat> formatCount = HashMultiset.create();
      final Map<DataFormat, String> formatPortNames = new HashMap<>();

      for (ToolElement e : sortedElements(elements.values())) {

        // Discard parameters
        if (!(e instanceof DataToolElement)) {
          continue;
        }

        DataToolElement dataElement = (DataToolElement) e;

        final DataFormat format = dataElement.getDataFormat();

        if (format.getMaxFilesCount() == 1) {
          this.ports.put(e.getName(),
              new ElementPort(dataElement, e.getValidatedName(), -1));
        } else {

          // If the DataFormat of the element is multi-file, only keep one
          // element for Eoulsan step ports

          final String portName;

          if (formatPortNames.containsKey(format)) {

            portName = formatPortNames.get(format);
          } else {

            portName = e.getValidatedName();
            formatPortNames.put(format, portName);
          }

          this.ports.put(e.getName(), new ElementPort(dataElement, portName,
              formatCount.count(format)));
          formatCount.add(format);
        }
      }
    }
  }

  /**
   * Parse tool file to extract useful data to run tool.
   * @param parameters the set step parameters
   * @throws EoulsanException if an data missing
   */
  public void configure(final Set<Parameter> parameters)
      throws EoulsanException {

    checkState(!isConfigured,
        "GalaxyToolStep, this instance has been already configured");

    Map<String, Parameter> stepParameters = new HashMap<>();

    // Convert Set in Map
    for (final Parameter p : parameters) {
      stepParameters.put(p.getName(), p);
    }

    final Document localDoc = this.doc;

    // Extract variable settings
    this.inputs = extractInputs(this.toolInfo, localDoc, stepParameters);
    this.outputs = extractOutputs(this.toolInfo, localDoc, stepParameters);

    this.inputPorts = new ElementPorts(this.inputs);
    this.outputPorts = new ElementPorts(this.outputs);

    isConfigured = true;
  }

  /**
   * Convert command tag from tool file in string, variable are replace by
   * value.
   * @param context the context
   * @return the string
   * @throws EoulsanException the Eoulsan exception
   */
  public ToolExecutorResult execute(final TaskContext context)
      throws EoulsanException {

    checkState(!isExecuted, "this instance has been already executed");

    context.getLogger().info("Parsing xml file successfully.");
    context.getLogger().info("Tool description: " + this.toolInfo);

    final int variablesCount = this.inputs.size() + this.outputs.size();
    final Map<String, String> variables = new HashMap<>(variablesCount);

    // Set a TMPDIR variable that contain the path to the temporary directory
    variables.put(TMP_DIR_VARIABLE_NAME,
        context.getLocalTempDirectory().getAbsolutePath());

    // Set a THREADS variable that contain the required thread number
    final int threadNumber =
        context.getCurrentStep().getRequiredProcessors() > 0
            ? context.getCurrentStep().getRequiredProcessors() : 1;
    variables.put(THREADS_VARIABLE_NAME, "" + threadNumber);

    // Input Data
    final Set<Data> inputDataSet = new HashSet<>();

    // Input file to use for Docker
    final Set<File> inputFiles = new HashSet<>();

    // Extract from inputs variable command
    for (final ToolElement e : this.inputs.values()) {

      if (e instanceof DataToolElement) {

        final ElementPort inPort = this.inputPorts.getPortElements(e.getName());

        // Extract value from context from DataFormat
        final Data data = context.getInputData(inPort.portName);
        inputDataSet.add(data);

        final File inFile = inPort.getInputDataFile(context).toFile();
        inputFiles.add(inFile);
        variables.put(e.getName(), inFile.getAbsolutePath());
        variables.put(removeNamespace(e.getName()), inFile.getAbsolutePath());

      } else {
        // Variables setting with parameters file
        variables.put(e.getName(), e.getValue());
      }
    }

    // Extract from outputs variable command
    for (final ToolElement e : this.outputs.values()) {

      if (e instanceof DataToolElement) {

        final ElementPort outPort =
            this.outputPorts.getPortElements(e.getName());

        // Extract value from context from DataFormat
        final DataFile outFile = outPort.getOutputDataFile(context,
            selectSourceData(inputDataSet, context));
        variables.put(e.getName(), outFile.toFile().getAbsolutePath());
        variables.put(removeNamespace(e.getName()),
            outFile.toFile().getAbsolutePath());
      } else {
        // Variables setting with parameters file
        variables.put(e.getName(), e.getValue());
      }
    }

    if (variables.isEmpty()) {
      throw new EoulsanException("No variable set for Cheetah script.");
    }

    context.getLogger().info("Tool variables: "
        + Joiner.on("\t").withKeyValueSeparator("=").join(variables));

    // Create the Cheetah interpreter
    final CheetahInterpreter cheetahInterpreter =
        new CheetahInterpreter(this.toolInfo.getCheetahScript(), variables);

    // Get the command line to execute from Cheetah code execution
    final String commandLine = cheetahInterpreter.execute();
    context.getLogger().fine("Cheetah code execution output: " + this.toolInfo);

    try {
      // Create the executor and interpret the command tag
      final ToolExecutor executor =
          new ToolExecutor(context, this.toolInfo, commandLine, inputFiles);

      // Execute the command
      final ToolExecutorResult result = executor.execute();

      this.isExecuted = true;
      return result;
    } catch (IOException e) {
      this.isExecuted = true;
      throw new EoulsanException(e);
    }
  }

  //
  // Private methods
  //

  /**
   * Create DOM instance from tool xml file.
   * @return DOM instance
   * @throws EoulsanException if an error occurs during creation instance
   */
  private Document buildDOM() throws EoulsanException {

    try (InputStream in = this.toolXMLis) {
      // Read the XML file
      final DocumentBuilder dBuilder =
          DocumentBuilderFactory.newInstance().newDocumentBuilder();
      final Document doc = dBuilder.parse(in);
      doc.getDocumentElement().normalize();
      return doc;

    } catch (final IOException | SAXException
        | ParserConfigurationException e) {
      throw new EoulsanException(e);
    }
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

  /**
   * Select the input data to use as data source.
   * @param inputDataSet a set with all the input data
   * @param context the task context
   * @return the data source to use for the output data or null if there is no
   *         input data
   */
  private Data selectSourceData(final Set<Data> inputDataSet,
      final TaskContext context) {

    Data inData = null;

    for (Data data : inputDataSet) {

      if (inData == null) {
        // Take the first data in all case
        inData = data;
      } else if (!data.getFormat().isOneFilePerAnalysis()) {

        if (inData.getFormat().isOneFilePerAnalysis()) {
          // If there is more that one input data replace the current selected
          // Data by a data which format allow multiple data per analysis
          inData = data;
        } else if (isDataNameInDesign(data, context)) {
          // If a data with a format that allow multiple data per analysis is
          // selected, prefers data from design
          inData = data;
        }
      }
    }

    return inData;
  }

  /**
   * Test if a data name is a sample name.
   * @param data the data to test
   * @param context the step context
   * @return true the data name is a sample name
   */
  private boolean isDataNameInDesign(final Data data,
      final TaskContext context) {

    final String dataName = data.getName();

    for (Sample sample : context.getWorkflow().getDesign().getSamples()) {

      if (Naming.toValidName(sample.getId()).equals(dataName)) {
        return true;
      }
    }

    return false;
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

  /**
   * Gets the in data format expected associated with variable found in command
   * line.
   * @return the in data format expected
   */
  public Set<DataToolElement> getInputDataElements() {
    return this.inputPorts.getStepElements();
  }

  /**
   * Gets the out data format expected associated with variable found in command
   * line.
   * @return the out data format expected
   */
  public Set<DataToolElement> getOutputDataElements() {
    return this.outputPorts.getStepElements();
  }

  /**
   * Gets the tool information.
   * @return the tool information
   */
  public ToolInfo getToolInfo() {
    return this.toolInfo;
  }

  @Override
  public String toString() {
    return "InterpreterToolGalaxy \n[inputs="
        + Joiner.on("\n").withKeyValueSeparator("=").join(this.inputs)
        + ", \noutputs="
        + Joiner.on("\n").withKeyValueSeparator("=").join(this.outputs)
        + ", \ntool=" + this.toolInfo + "]";
  }

  /**
   * Remove the namespace from the name of a variable.
   * @param variableName variable name
   * @return the variable name without the namespace
   */
  public static String removeNamespace(final String variableName) {

    if (variableName == null) {
      return null;
    }

    final int dotIndex = variableName.lastIndexOf('.');

    if (dotIndex == -1) {
      return variableName;
    }

    return variableName.substring(dotIndex + 1);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param file the Galaxy tool file
   * @throws EoulsanException the Eoulsan exception
   * @throws IOException if the file cannot be load
   */
  public GalaxyToolInterpreter(final File file)
      throws EoulsanException, IOException {

    this(Files.newInputStream(file.toPath()), file.getName());
  }

  /**
   * Public constructor.
   * @param file the Galaxy tool file
   * @throws EoulsanException the Eoulsan exception
   * @throws IOException if the file cannot be load
   */
  public GalaxyToolInterpreter(final Path file)
      throws EoulsanException, IOException {

    this(Files.newInputStream(file), file.getFileName().toString());
  }

  /**
   * Public constructor.
   * @param in the input stream
   * @param toolSource tool source
   * @throws EoulsanException the Eoulsan exception
   */
  public GalaxyToolInterpreter(final InputStream in, final String toolSource)
      throws EoulsanException {

    requireNonNull(in, "in argument cannot be null");

    this.toolXMLis = in;
    this.doc = buildDOM();

    this.toolInfo = new ToolInfo(this.doc, toolSource);

    checkDomValidity();
  }

}
