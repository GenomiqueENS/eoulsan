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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.google.common.base.Objects;

import fr.ens.biologie.genomique.eoulsan.AbstractEoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntimeException;
import fr.ens.biologie.genomique.eoulsan.Settings;
import fr.ens.biologie.genomique.eoulsan.core.InputPort;
import fr.ens.biologie.genomique.eoulsan.core.OutputPort;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.Workflow;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.util.ClassLoaderObjectInputStream;

/**
 * This class define a task context.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TaskContextImpl implements TaskContext, Serializable {

  /** Serialization version UID. */
  private static final long serialVersionUID = 8288158811122533646L;

  private static AtomicInteger instanceCount = new AtomicInteger(0);

  private final int id;
  private final WorkflowContext workflowContext;
  private String contextName;
  private final AbstractStep step;

  private final Map<String, Data> inputData = new HashMap<>();
  private final Map<String, AbstractData> outputData = new HashMap<>();

  //
  // Getters
  //

  public int getId() {
    return this.id;
  }

  @Override
  public String getContextName() {

    return this.contextName;
  }

  /**
   * Get the local working directory.
   * @return Returns the local working directory
   */
  public DataFile getLocalWorkingPathname() {

    return this.workflowContext.getLocalWorkingDirectory();
  }

  /**
   * Get the Hadoop working directory.
   * @return Returns the Hadoop working directory
   */
  public DataFile getHadoopWorkingPathname() {

    return this.workflowContext.getHadoopWorkingDirectory();
  }

  /**
   * Get the job directory.
   * @return Returns the job directory
   */
  public DataFile getJobDirectory() {

    return this.workflowContext.getJobDirectory();
  }

  /**
   * Get the task output directory.
   * @return Returns the task output directory
   */
  public DataFile getTaskOutputDirectory() {

    return this.workflowContext.getTaskDirectory();
  }

  /**
   * Get the data repository directory.
   * @return Returns the data repository directory
   */
  public DataFile getDataRepositoryDirectory() {

    return this.workflowContext.getDataRepositoryDirectory();
  }

  @Override
  public DataFile getOutputDirectory() {
    return this.workflowContext.getOutputDirectory();
  }

  @Override
  public DataFile getStepOutputDirectory() {

    return this.step.getStepOutputDirectory();
  }

  @Override
  public String getJobId() {
    return this.workflowContext.getJobId();
  }

  @Override
  public String getJobHost() {
    return this.workflowContext.getJobHost();
  }

  @Override
  public long getContextCreationTime() {
    return this.workflowContext.getContextCreationTime();
  }

  @Override
  public DataFile getDesignFile() {
    return this.workflowContext.getDesignFile();
  }

  @Override
  public DataFile getWorkflowFile() {
    return this.workflowContext.getWorkflowFile();
  }

  /**
   * Get the application jar path.
   * @return Returns the jar path
   */
  public DataFile getJarPathname() {
    return this.workflowContext.getJarFile();
  }

  @Override
  public String getJobUUID() {
    return this.workflowContext.getJobUUID();
  }

  @Override
  public String getJobDescription() {
    return this.workflowContext.getJobDescription();
  }

  @Override
  public String getJobEnvironment() {
    return this.workflowContext.getJobEnvironment();
  }

  @Override
  public String getCommandName() {
    return this.workflowContext.getCommandName();
  }

  @Override
  public String getCommandDescription() {
    return this.workflowContext.getCommandDescription();
  }

  @Override
  public String getCommandAuthor() {
    return this.workflowContext.getCommandAuthor();
  }

  @Override
  public Workflow getWorkflow() {

    return this.workflowContext.getWorkflow();
  }

  @Override
  public Step getCurrentStep() {
    return this.step;
  }

  /**
   * Get the AbstractWorkflowStep object.
   * @return a AbstractWorkflowStep object
   */
  AbstractStep getStep() {

    return this.step;
  }

  //
  // Setters
  //

  @Override
  public void setContextName(final String contextName) {

    requireNonNull(contextName, "contextName argument cannot be null");

    // TODO Check if the context name is unique for the step

    this.contextName = contextName.trim();
  }

  //
  // Other methods
  //

  @Override
  public AbstractEoulsanRuntime getRuntime() {

    return this.workflowContext.getRuntime();
  }

  @Override
  public Settings getSettings() {

    return this.workflowContext.getSettings();
  }

  @Override
  public Logger getLogger() {

    return this.workflowContext.getLogger();
  }

  @Override
  public Data getInputData(final String portName) {

    requireNonNull(portName, "portName cannot be null");
    checkArgument(this.inputData.containsKey(portName),
        "unknown input port name: " + portName);

    return new UnmodifiableData(this.inputData.get(portName));
  }

  @Override
  public Data getInputData(final DataFormat format) {

    return getInputData(getInputPortNameForFormat(format));
  }

  @Override
  public Data getOutputData(final String portName, final String dataName) {
    return getOutputData(portName, dataName, -1);
  }

  @Override
  public Data getOutputData(final String portName, final String dataName,
      final int part) {

    requireNonNull(portName, "portName cannot be null");
    checkArgument(this.outputData.containsKey(portName),
        "unknown output port name: " + portName);

    final AbstractData data = this.outputData.get(portName);
    data.setName(dataName, true);
    data.setPart(part);

    return data;
  }

  @Override
  public Data getOutputData(final String portName, final Data origin) {

    requireNonNull(origin, "origin cannot be null");
    requireNonNull(portName, "portName cannot be null");
    checkArgument(this.outputData.containsKey(portName),
        "unknown output port name: " + portName);

    final AbstractData result = this.outputData.get(portName);

    AbstractData oriData = origin instanceof UnmodifiableData
        ? ((UnmodifiableData) origin).getData() : (AbstractData) origin;

    result.setName(oriData.getName(), oriData.isDefaultName());
    result.setPart(oriData.getPart());

    // Set the metadata of the new data from the origin data only if each data
    // are not a list
    if (!result.isList() && !origin.isList()) {
      result.getMetadata().set(origin.getMetadata());
    }

    return result;
  }

  @Override
  public Data getOutputData(final DataFormat format, final String dataName) {
    return getOutputData(format, dataName, -1);
  }

  @Override
  public Data getOutputData(final DataFormat format, final String dataName,
      final int part) {

    return getOutputData(getOutputPortNameForFormat(format), dataName);
  }

  @Override
  public Data getOutputData(final DataFormat format, final Data origin) {

    return getOutputData(getOutputPortNameForFormat(format), origin);
  }

  /**
   * Update serialized output data. This method is used when process serialized
   * task result.
   * @param data data to set
   */
  private void updateOutputData(final Map<String, AbstractData> data) {

    requireNonNull(data, "data argument cannot be null");
    checkArgument(data.size() == this.outputData.size(),
        "Unexpected number of output data ("
            + this.outputData.size() + " was expected): " + data.size());

    for (Map.Entry<String, AbstractData> e : data.entrySet()) {

      checkArgument(this.outputData.containsKey(e.getKey()),
          "Unknown port: " + e.getKey());

      // Update outputData
      this.outputData.put(e.getKey(), e.getValue());
    }
  }

  @Override
  public File getLocalTempDirectory() {

    return EoulsanRuntime.getRuntime().getTempDirectory();
  }

  /**
   * Create the prefix of a related task file.
   * @return a string with the prefix of the task file
   */
  public String getTaskFilePrefix() {

    return getStep().getId() + "_context#" + getId();
  }

  //
  // Package methods
  //

  /**
   * Get raw access to input data stored in the object.
   * @param port name of the input port
   * @return a Data object
   */
  public Data getInputData(final InputPort port) {

    requireNonNull(port, "port cannot be null");

    if (!this.inputData.containsKey(port.getName())) {
      throw new EoulsanRuntimeException(
          "Unknown port: " + port.getName() + " for step " + this.step.getId());
    }

    return this.inputData.get(port.getName());
  }

  /**
   * Get raw access to output data stored in the object.
   * @param port name of the output port
   * @return a Data object
   */
  Data getOutputData(final OutputPort port) {

    requireNonNull(port, "port cannot be null");

    if (!this.outputData.containsKey(port.getName())) {
      throw new EoulsanRuntimeException(
          "Unknown port: " + port.getName() + " for step " + this.step.getId());
    }

    return this.outputData.get(port.getName());
  }

  AbstractStep getWorkflowStep() {

    return this.step;
  }

  //
  // Private methods
  //

  /**
   * Get the input port for a given format.
   * @param format the format
   * @return the port that matches to the format
   */
  private String getInputPortNameForFormat(final DataFormat format) {

    requireNonNull(format, "The format is null");

    final List<StepInputPort> ports =
        this.step.getWorkflowInputPorts().getPortsWithDataFormat(format);

    switch (ports.size()) {

    case 0:
      throw new EoulsanRuntimeException("The step "
          + this.step.getId() + " do not provide an input port with format: "
          + format.getName());
    case 1:
      return ports.get(0).getName();
    default:
      throw new EoulsanRuntimeException("The step "
          + this.step.getId()
          + " provide more than one input port with format: "
          + format.getName());
    }
  }

  /**
   * Get the output port for a given format.
   * @param format the format
   * @return the port that matches to the format
   */
  private String getOutputPortNameForFormat(final DataFormat format) {

    requireNonNull(format, "The format is null");

    final List<StepOutputPort> ports =
        this.step.getWorkflowOutputPorts().getPortsWithDataFormat(format);

    switch (ports.size()) {

    case 0:
      throw new EoulsanRuntimeException("The step "
          + this.step.getId() + " do not provide an output port with format: "
          + format.getName());
    case 1:
      return ports.get(0).getName();
    default:
      throw new EoulsanRuntimeException("The step "
          + this.step.getId()
          + " provide more than one output port with format: "
          + format.getName());
    }
  }

  //
  // Other methods
  //

  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("id", this.id)
        .add("step", this.step.getId()).add("contextName", this.contextName)
        .toString();
  }

  //
  // Serialization methods
  //

  /**
   * Serialize the TaskContext object.
   * @param file output DataFile
   * @throws IOException if an error occurs while creating the file
   */
  public void serialize(final File file) throws IOException {

    requireNonNull(file, "file argument cannot be null");

    try (OutputStream out = new FileOutputStream(file)) {
      serialize(out);
    }
  }

  /**
   * Serialize the TaskContext object.
   * @param file output DataFile
   * @throws IOException if an error occurs while creating the file
   */
  public void serialize(final DataFile file) throws IOException {

    requireNonNull(file, "file argument cannot be null");

    try (OutputStream out = file.create()) {
      serialize(out);
    }
  }

  /**
   * Serialize the TaskContext object.
   * @param out output stream
   * @throws IOException if an error occurs while creating the file
   */
  public void serialize(final OutputStream out) throws IOException {

    requireNonNull(out, "out argument cannot be null");

    try (final ObjectOutputStream oos = new ObjectOutputStream(out)) {

      oos.writeObject(this);
      oos.writeObject(EoulsanRuntime.getSettings());
    }

  }

  /**
   * Deserialize the TaskContext object. Warning: this method update the values
   * of the settings of the Eoulsan runtime.
   * @param file input DataFile
   * @throws IOException if an error occurs while reading the file
   */
  public static TaskContextImpl deserialize(final File file)
      throws IOException {

    requireNonNull(file, "file argument cannot be null");

    try (InputStream in = new FileInputStream(file)) {
      return deserialize(in);
    }
  }

  /**
   * Deserialize the TaskContext object. Warning: this method update the values
   * of the settings of the Eoulsan runtime.
   * @param file input DataFile
   * @throws IOException if an error occurs while reading the file
   */
  public static TaskContextImpl deserialize(final DataFile file)
      throws IOException {

    requireNonNull(file, "file argument cannot be null");

    try (InputStream in = file.open()) {
      return deserialize(in);
    }
  }

  /**
   * Deserialize the TaskContext object. Warning: this method update the values
   * of the settings of the Eoulsan runtime.
   * @param in input stream
   * @throws IOException if an error occurs while reading the file
   */
  public static TaskContextImpl deserialize(final InputStream in)
      throws IOException {

    requireNonNull(in, "in argument cannot be null");

    try (final ObjectInputStream ois = new ClassLoaderObjectInputStream(in)) {

      // Read TaskContext object
      final TaskContextImpl result = (TaskContextImpl) ois.readObject();

      // Read Settings object
      final Settings settings = (Settings) ois.readObject();

      // Overwrite current Settings of Eoulsan runtime
      EoulsanRuntime.getSettings().setSettings(settings);

      return result;

    } catch (ClassNotFoundException e) {
      throw new EoulsanRuntimeException(e);
    }
  }

  /**
   * Serialize output data.
   * @param file output file
   * @throws IOException if an error occurs while creating the file
   */
  public void serializeOutputData(final File file) throws IOException {

    requireNonNull(file, "file argument cannot be null");

    serializeOutputData(new FileOutputStream(file));
  }

  /**
   * Serialize output data.
   * @param file output DataFile
   * @throws IOException if an error occurs while creating the file
   */
  public void serializeOutputData(final DataFile file) throws IOException {

    requireNonNull(file, "file argument cannot be null");

    serializeOutputData(file.create());
  }

  /**
   * Serialize output data.
   * @param out output stream
   * @throws IOException if an error occurs while creating the file
   */
  public void serializeOutputData(final OutputStream out) throws IOException {

    requireNonNull(out, "out argument cannot be null");

    final ObjectOutputStream oos = new ObjectOutputStream(out);

    oos.writeObject(this.outputData);
    oos.close();
  }

  /**
   * Deserialize output data.
   * @param file input datafile
   * @throws IOException if an error occurs while reading the file
   */
  public void deserializeOutputData(final DataFile file) throws IOException {

    requireNonNull(file, "file argument cannot be null");

    deserializeOutputData(file.open());
  }

  /**
   * Deserialize output data.
   * @param file input file
   * @throws IOException if an error occurs while reading the file
   */
  public void deserializeOutputData(final File file) throws IOException {

    requireNonNull(file, "file argument cannot be null");

    deserializeOutputData(new FileInputStream(file));
  }

  /**
   * Deserialize output data.
   * @param in input stream
   * @throws IOException if an error occurs while reading the file
   */
  public void deserializeOutputData(final InputStream in) throws IOException {

    requireNonNull(in, "in argument cannot be null");

    try {
      final ObjectInputStream ois = new ClassLoaderObjectInputStream(in);

      // Read TaskContext object
      @SuppressWarnings("unchecked")
      final Map<String, AbstractData> outputData =
          (Map<String, AbstractData>) ois.readObject();

      ois.close();

      // Update serialized data
      updateOutputData(outputData);

    } catch (ClassNotFoundException e) {
      throw new EoulsanRuntimeException(e);
    }
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param step step related to the context
   */
  TaskContextImpl(final WorkflowContext workflowContext,
      final AbstractStep step, final Map<InputPort, Data> inputData,
      final Map<OutputPort, AbstractData> outputData) {

    requireNonNull(workflowContext, "workflow context cannot be null");
    requireNonNull(step, "step cannot be null");

    this.id = instanceCount.incrementAndGet();
    this.contextName = "context" + this.id;

    this.workflowContext = workflowContext;
    this.step = step;

    // Copy input and output data
    for (Map.Entry<InputPort, Data> e : inputData.entrySet()) {
      this.inputData.put(e.getKey().getName(), e.getValue());
    }

    for (Map.Entry<OutputPort, AbstractData> e : outputData.entrySet()) {
      this.outputData.put(e.getKey().getName(), e.getValue());
    }
  }

}
