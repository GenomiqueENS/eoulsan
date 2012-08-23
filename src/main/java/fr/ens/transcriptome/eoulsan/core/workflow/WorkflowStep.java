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
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
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
  private boolean configurationDone;
  private long duration = -1;

  public enum StepType {
    STANDARD_STEP, DESIGN_STEP, FIRST_STEP, TERMINAL_STEP
  };

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

  //
  // Input/output datafiles methods
  //

  public DataFile getOutputDataFile(final DataFormat format, final Sample sample) {

    if (format == null || sample == null) {
      return null;
    }

    switch (this.type) {

    case STANDARD_STEP:

      if (!this.outputFormats.contains(format))
        throw new EoulsanRuntimeException("The "
            + format.getFormatName()
            + " format is not an input format of the step "
            + this.step.getName());

      // Return a file created by a step
      return newDataFile(this.context, this, format, sample);

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

  //
  // Step lifetime methods
  //

  /**
   * Configure the step.
   * @throws EoulsanException if an error occurs while configuring a step
   */
  public void configure() throws EoulsanException {

    if (!this.configurationDone || this.type != StepType.STANDARD_STEP)
      return;

    LOGGER.info("Configure "
        + this.id + " step with step parameters: " + this.stepParameters);

    this.step.configure(this.stepParameters);

    // Get output formats
    final DataFormat[] dfIn = this.step.getOutputFormats();
    if (dfIn != null)
      for (DataFormat df : dfIn)
        this.outputFormats.add(df);

  }

  /**
   * Execute the step.
   * @return a Step result object.
   */
  public StepResult execute() {

    if (this.type != StepType.STANDARD_STEP)
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
   * @return a new Datafile object
   */
  private static final DataFile newDataFile(final Context context,
      final WorkflowStep wStep, final DataFormat format, final Sample sample) {

    // TODO must use wStep.getId() to create file path
    return new DataFile(context.getBasePathname()
        + '/' + ContextUtils.getNewDataFilename(format, sample));
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

  //
  // Constructors
  //

  public WorkflowStep(final Design design, final Context context,
      final StepType type) {

    Preconditions.checkArgument(type == StepType.STANDARD_STEP,
        "This constructor cannot be used for standard steps");

    this.design = design;
    this.context = context;

    this.id = type.name();
    this.skip = false;
    this.type = type;
    this.step = null;
    this.stepParameters = null;
  }

  public WorkflowStep(final Design design, final Context context,
      final String id, final String stepName,
      final Set<Parameter> stepParameters, final boolean skip)
      throws EoulsanException {

    this.design = design;
    this.context = context;

    this.id = id;
    this.skip = skip;
    this.type = StepType.STANDARD_STEP;
    this.step = findStep(stepName);
    this.stepParameters = Sets.newHashSet(stepParameters);
  }

}
