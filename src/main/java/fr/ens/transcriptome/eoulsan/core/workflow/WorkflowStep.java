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
 * of the Institut de Biologie de l'√âcole Normale Sup√©rieure and
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

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.ContextUtils;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepService;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFileMetadata;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.data.DataType;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.steps.StepResult;

public class WorkflowStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private final String id;
  private final boolean skip;
  private final StepType type;
  private final Step step;
  private final Set<Parameter> stepParameters;

  private final Design design;
  private final Context context;
  private final Set<DataFormat> outputFormats = Sets.newHashSet();
  private final Set<DataFormat> inputFormats = Sets.newHashSet();
  private final Map<DataFormat, InputDataFileLocation> inputFormatLocations =
      Maps.newHashMap();
  private boolean configurationDone;
  private long duration = -1;

  public static enum StepType {
    STANDARD_STEP, DESIGN_STEP, FIRST_STEP, TERMINAL_STEP, GENERATOR_STEP
  };

  /**
   * This class allow to retrieve the DataFile that correspond to a input
   * DataFormat of the Workflow step.
   * @author Laurent Jourdren
   */
  public static final class InputDataFileLocation {

    private final DataFormat format;
    private final WorkflowStep step;

    /**
     * Get the DataFile.
     * @param sample sample
     * @return the DataFile for the sample
     */
    public DataFile getDataFile(final Sample sample) {

      return getDataFile(sample, -1);
    }

    /**
     * Get the DataFile.
     * @param sample sample
     * @param fileIndex file index for multifile data
     * @return the DataFile for the sample
     */
    public DataFile getDataFile(final Sample sample, final int fileIndex) {

      Preconditions.checkNotNull(sample, "Sample cannot be null");

      return this.step.getOutputDataFile(this.format, sample, fileIndex);
    }

    @Override
    public String toString() {

      return Objects.toStringHelper(this).add("format", format)
          .add("Step", this.step.id).toString();
    }

    /**
     * Constructor.
     * @param step Workflow step
     * @param format format
     */
    public InputDataFileLocation(DataFormat format, final WorkflowStep step) {

      Preconditions.checkNotNull(step, "Format cannot be null");
      Preconditions.checkNotNull(step, "Step cannot be null");

      this.format = format;
      this.step = step;

    }

  }

  public void show() {

    System.out.println("Step: " + getId() + "(" + getType() + ")");
    for (Map.Entry<DataFormat, InputDataFileLocation> e : this.inputFormatLocations
        .entrySet())
      System.out.println("\t"
          + e.getKey().getFormatName() + "\t" + e.getValue().step.getId());

  }

  //
  // Getters
  //

  /**
   * Get step id.
   * @return the step id
   */
  public String getId() {

    return id;
  }

  /**
   * Test if the step must be skipped
   * @return true if the step must be skipped
   */
  public boolean isSkip() {

    return skip;
  }

  /**
   * Get the type of the step.
   * @return the type of the step;
   */
  public StepType getType() {

    return type;
  }

  /**
   * Get the step.
   * @return the step object
   */
  public Step getStep() {

    return step;
  }

  /**
   * Get the duration of the execution of the step.
   * @return the duration of the step in milliseconds
   */
  public long getDuration() {

    return this.duration;
  }

  /**
   * Get the input DataFormats.
   * @return a unmodifiable set with DataFormats
   */
  Set<DataFormat> getInputDataFormats() {

    return Collections.unmodifiableSet(this.inputFormats);
  }

  /**
   * Get the output DataFormats.
   * @return a unmodifiable set with DataFormats
   */
  Set<DataFormat> getOutputDataFormats() {

    return Collections.unmodifiableSet(this.outputFormats);
  }

  ListMultimap<DataType, DataFormat> getInputDataFormatByDataTypes() {

    final ListMultimap<DataType, DataFormat> result =
        ArrayListMultimap.create();

    for (DataFormat df : this.inputFormats)
      result.put(df.getType(), df);

    return result;
  }

  //
  // Setters
  //

  void addInputFormatLocation(DataFormat format, final WorkflowStep step) {

    if (!this.inputFormats.contains(format))
      throw new EoulsanRuntimeException("Cannot add ");

    if (!this.inputFormatLocations.containsKey(format))
      this.inputFormatLocations.put(format, new InputDataFileLocation(format,
          step));
  }

  //
  // Input/output datafiles methods
  //

  public DataFile getOutputDataFile(final DataFormat format, final Sample sample) {

    return getOutputDataFile(format, sample, -1);
  }

  public DataFile getOutputDataFile(final DataFormat format,
      final Sample sample, final int fileIndex) {

    Preconditions.checkNotNull(format, "Format argument cannot be null");
    Preconditions.checkNotNull(sample, "Sample argument cannot be null");

    switch (this.type) {

    case STANDARD_STEP:

      if (!this.outputFormats.contains(format))
        throw new EoulsanRuntimeException("The "
            + format.getFormatName()
            + " format is not an output format of the step "
            + this.step.getName());

      // Return a file created by a step
      return newDataFile(this.context, this, format, sample, fileIndex);

    case DESIGN_STEP:

      final DataType type = format.getType();
      final DataFormatRegistry registry = DataFormatRegistry.getInstance();
      final String fieldName =
          registry.getDesignFieldnameForDataType(design, type);

      if (fieldName == null)
        throw new EoulsanRuntimeException("The "
            + format.getFormatName()
            + " format was not found in the design file for sample "
            + sample.getId() + " (" + sample.getName() + ")");

      final DataFile file =
          new DataFile(sample.getMetadata().getField(fieldName));

      if (!isDesignDataFileValidFormat(file, type, format))
        throw new EoulsanRuntimeException("The file "
            + file + " in design file is not a " + format.getFormatName()
            + format.getFormatName() + " format for " + sample.getId() + " ("
            + sample.getName() + ")");

      return file;

    default:
      return null;
    }

  }

  /**
   * Get a DataFile that correspond to a DataFormat and a Sample for this step.
   * @param format the input format
   * @param sample the sample
   * @return a DataFormat or null if the DataFormat is not available
   */
  public DataFile getInputDataFile(final DataFormat format, final Sample sample) {

    Preconditions.checkNotNull(format, "Format argument cannot be null");
    Preconditions.checkNotNull(sample, "Sample argument cannot be null");

    if (!this.inputFormatLocations.containsKey(format))
      throw new EoulsanRuntimeException("The "
          + format.getFormatName()
          + " format is not an output format of the step "
          + this.step.getName());

    return this.inputFormatLocations.get(format).getDataFile(sample);
  }

  //
  // Step lifetime methods
  //

  /**
   * Configure the step.
   * @throws EoulsanException if an error occurs while configuring a step
   */
  public void configure() throws EoulsanException {

    if (this.configurationDone
        || (this.type != StepType.STANDARD_STEP && this.type != StepType.GENERATOR_STEP))
      return;

    LOGGER.info("Configure "
        + this.id + " step with step parameters: " + this.stepParameters);

    if (this.type == StepType.STANDARD_STEP)
      this.step.configure(this.stepParameters);

    // Get output formats
    final DataFormat[] dfOut = this.step.getOutputFormats();
    if (dfOut != null)
      for (DataFormat df : dfOut)
        this.outputFormats.add(df);

    // Get input format
    final DataFormat[] dfIn = this.step.getInputFormats();
    if (dfIn != null)
      for (DataFormat df : dfIn)
        this.inputFormats.add(df);

    this.configurationDone = true;
  }

  /**
   * Execute the step.
   * @return a Step result object.
   */
  public StepResult execute() {

    if (this.type != StepType.STANDARD_STEP
        && this.type != StepType.GENERATOR_STEP)
      return null;

    Stopwatch stopwatch = new Stopwatch();
    stopwatch.start();

    final StepResult result = step.execute(design, context);

    this.duration = stopwatch.elapsedMillis();

    return result;
  }

  //
  // Static methods
  //

  /**
   * Get a Step object from its name.
   * @param stepName name of the step
   * @return a Step object
   * @throws EoulsanException if the step does not exits
   */
  private final static Step findStep(final String stepName)
      throws EoulsanException {

    if (stepName == null)
      throw new EoulsanException("Step name is null");

    final String lower = stepName.trim().toLowerCase();
    final boolean hadoopMode = EoulsanRuntime.getRuntime().isHadoopMode();

    final Step result = StepService.getInstance(hadoopMode).getStep(lower);

    if (result == null)
      throw new EoulsanException("Unknown step: " + lower);

    return result;
  }

  /**
   * Create a DataFile object that correspond to a standard Eoulsan input file
   * @param context context object
   * @param wStep step
   * @param format format
   * @param sample sample
   * @param fileIndex file index for multifile data
   * @return a new Datafile object
   */
  private static final DataFile newDataFile(final Context context,
      final WorkflowStep wStep, final DataFormat format, final Sample sample,
      final int fileIndex) {

    // TODO must use wStep.getId() to create file path
    return new DataFile(context.getBasePathname()
        + '/' + ContextUtils.getNewDataFilename(format, sample, fileIndex));
  }

  /**
   * Check if a DataFile from the design has a the good format.
   * @param file the DataFile to test
   * @param dt the DataType
   * @param df the DataFormat
   * @return true if a DataFile from the design has a the good format
   */
  private boolean isDesignDataFileValidFormat(final DataFile file,
      final DataType dt, final DataFormat df) {

    if (file == null || dt == null || df == null)
      return false;

    DataFileMetadata md = null;

    try {
      md = file.getMetaData();
    } catch (IOException e) {
      LOGGER.warning("Error while getting metadata for file "
          + file + ": " + e.getMessage());
      md = null;
    }

    if (md != null && df.equals(md.getDataFormat()))
      return true;

    final DataFormatRegistry dfr = DataFormatRegistry.getInstance();
    final DataFormat sourceDf =
        dfr.getDataFormatFromExtension(dt, file.getExtension());

    if (sourceDf != null && sourceDf.equals(df))
      return true;

    return false;
  }

  public void info() {

    System.out.println("Id: " + this.id);
    System.out.print("Type: " + this.type);

    if (this.type == StepType.STANDARD_STEP
        || this.type == StepType.GENERATOR_STEP) {
      System.out.println("Step name: " + this.step.getName());
      System.out.println("Inputs:");
      for (Map.Entry<DataFormat, InputDataFileLocation> e : this.inputFormatLocations
          .entrySet()) {
        System.out.println("\t" + e.getValue());
      }
      for (DataFormat e : this.outputFormats) {
        System.out.println("\t" + e);
      }
    }
    System.out.println();
  }

  //
  // Constructors
  //

  public WorkflowStep(final Design design, final Context context,
      final StepType type) {

    Preconditions.checkArgument(type != StepType.STANDARD_STEP,
        "This constructor cannot be used for standard steps");

    Preconditions.checkNotNull(design, "Design argument cannot be null");
    Preconditions.checkNotNull(context, "Context argument cannot be null");
    Preconditions.checkNotNull(type, "Type argument cannot be null");

    this.design = design;
    this.context = context;

    this.id = type.name();
    this.skip = false;
    this.type = type;
    this.step = null;
    this.stepParameters = null;
  }

  /**
   * Create a Generator Workflow step.
   * @param design design object
   * @param context context object
   * @param format DataFormat
   * @throws EoulsanException if an error occurs while configuring the generator
   */
  public WorkflowStep(final Design design, final Context context,
      final DataFormat format) throws EoulsanException {

    Preconditions.checkNotNull(design, "Design argument cannot be null");
    Preconditions.checkNotNull(context, "Context argument cannot be null");
    Preconditions.checkNotNull(format, "Format argument cannot be null");

    this.design = design;
    this.context = context;
    this.step = format.getGenerator();

    Preconditions.checkNotNull(this.step, "The generator step is null");

    this.id = this.step.getName();
    this.skip = false;
    this.type = StepType.GENERATOR_STEP;

    this.stepParameters = null;
  }

  public WorkflowStep(final Design design, final Context context,
      final String id, final String stepName,
      final Set<Parameter> stepParameters, final boolean skip)
      throws EoulsanException {

    Preconditions.checkNotNull(design, "Design argument cannot be null");
    Preconditions.checkNotNull(context, "Context argument cannot be null");
    Preconditions.checkNotNull(id, "Step id argument cannot be null");
    Preconditions.checkNotNull(stepName, "Step name argument cannot be null");
    Preconditions.checkNotNull(stepParameters,
        "Step arguments argument cannot be null");

    this.design = design;
    this.context = context;

    this.id = id;
    this.skip = skip;
    this.type = StepType.STANDARD_STEP;
    this.step = findStep(stepName);
    this.stepParameters = Sets.newHashSet(stepParameters);
  }

}
