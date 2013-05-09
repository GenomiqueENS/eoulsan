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

package fr.ens.transcriptome.eoulsan.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.AbstractEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFileMetadata;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.data.DataType;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.util.SystemUtils;
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class define an simple ExecutorInfo.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class SimpleContext implements Context {

  /** Logger. */
  private static final Logger LOGGER = EoulsanLogger.getLogger();

  private String basePathname;
  private String logPathname;
  private String outputPathname;
  private String jobId;
  private final String host;
  private String designPathname;
  private String paramPathname;
  private String jarPathname;
  private final String jobUUID = UUID.randomUUID().toString();
  private final String jobDescription;
  private final String jobEnvironment;
  private String commandName = "";
  private String commandDescription = "";
  private String commandAuthor = "";
  private WorkflowDescription workflow;
  private Step step;
  private Set<DataFormat> stepInputDataFormats;
  private Set<DataFormat> stepOutputDataFormats;

  private long contextCreationTime;

  private final Map<DataType, String> dataTypesFields = Maps.newHashMap();

  //
  // Getters
  //

  @Override
  public String getBasePathname() {
    return this.basePathname;
  }

  @Override
  public String getLogPathname() {
    return this.logPathname;
  }

  @Override
  public String getOutputPathname() {
    return this.outputPathname;
  }

  @Override
  public String getJobId() {
    return this.jobId;
  }

  @Override
  public String getJobHost() {
    return this.host;
  }

  @Override
  public long getContextCreationTime() {
    return this.contextCreationTime;
  }

  @Override
  public String getDesignPathname() {
    return this.designPathname;
  }

  @Override
  public String getParameterPathname() {
    return this.paramPathname;
  }

  @Override
  public String getJarPathname() {
    return this.jarPathname;
  }

  @Override
  public String getJobUUID() {
    return this.jobUUID;
  }

  @Override
  public String getJobDescription() {
    return this.jobDescription;
  }

  @Override
  public String getJobEnvironment() {
    return this.jobEnvironment;
  }

  @Override
  public String getCommandName() {
    return this.commandName;
  }

  @Override
  public String getCommandDescription() {
    return this.commandDescription;
  }

  @Override
  public String getCommandAuthor() {
    return this.commandAuthor;
  }

  @Override
  public WorkflowDescription getWorkflow() {

    return this.workflow;
  }

  @Override
  public Step getCurrentStep() {
    return this.step;
  }

  //
  // Setters
  //

  /**
   * Set the base path
   * @param basePath The basePath to set
   */
  public void setBasePathname(final String basePath) {

    this.basePathname = basePath;
  }

  /**
   * Set the log path
   * @param logPath The log path to set
   */
  public void setLogPathname(final String logPath) {

    this.logPathname = logPath;
  }

  /**
   * Set the output path
   * @param outputPath The output path to set
   */
  public void setOutputPathname(final String outputPath) {

    this.outputPathname = outputPath;
  }

  /**
   * Set the design path
   * @param designPathname The design path to set
   */
  public void setDesignPathname(final String designPathname) {

    this.designPathname = designPathname;
  }

  /**
   * Set the parameter path
   * @param paramPathname The parameter path to set
   */
  public void setParameterPathname(final String paramPathname) {

    this.paramPathname = paramPathname;
  }

  /**
   * Set the jar path
   * @param jarPathname The jar path to set
   */
  public void setJarPathname(final String jarPathname) {

    this.jarPathname = jarPathname;
  }

  /**
   * Set command name
   * @param commandName the command name
   */
  void setCommandName(final String commandName) {

    this.commandName = commandName;
  }

  /**
   * Set command description
   * @param commandDescription the command name
   */
  void setCommandDescription(final String commandDescription) {

    this.commandDescription = commandDescription;
  }

  /**
   * Set command author
   * @param commandAuthor the command name
   */
  void setCommandAuthor(final String commandAuthor) {

    this.commandAuthor = commandAuthor;
  }

  /**
   * Add information from command object.
   * @param command the command object
   */
  void addCommandInfo(final Command command) {

    if (command == null)
      throw new NullPointerException("The command is null");

    setCommandName(command.getName());
    setCommandDescription(command.getDescription());
    setCommandAuthor(command.getAuthor());
  }

  /**
   * Set the workflow of the execution
   * @param workflow the workflow to set
   */
  void setWorkflow(final WorkflowDescription workflow) {

    this.workflow = workflow;
  }

  /**
   * Set the current step running.
   * @param step step to set
   */
  void setStep(final Step step) {

    this.step = step;

    if (step == null) {

      this.stepInputDataFormats = null;
      this.stepOutputDataFormats = null;
    } else {

      final DataFormat[] in = step.getInputFormats();
      final DataFormat[] out = step.getOutputFormats();

      this.stepInputDataFormats =
          in == null ? null : Utils.newHashSet(Arrays.asList(in));
      this.stepOutputDataFormats =
          out == null ? null : Utils.newHashSet(Arrays.asList(out));
    }
  }

  /**
   * Set the design.
   * @param design design to set
   */
  public void setDesign(final Design design) {

    if (design == null)
      return;

    final List<String> fieldnames = design.getMetadataFieldsNames();
    DataFormatRegistry registry = DataFormatRegistry.getInstance();

    for (String fieldname : fieldnames) {

      DataType dt = registry.getDataTypeForDesignField(fieldname);
      if (dt != null)
        this.dataTypesFields.put(dt, fieldname);
    }

  }

  //
  // Other methods
  //

  private void createExecutionName(final long millisSinceEpoch) {

    final Calendar cal = Calendar.getInstance(Locale.ENGLISH);
    cal.setTime(new Date(millisSinceEpoch));

    final String creationDate =
        String.format("%04d%02d%02d-%02d%02d%02d", cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
            cal.get(Calendar.SECOND));

    this.contextCreationTime = millisSinceEpoch;
    this.jobId = Globals.APP_NAME_LOWER_CASE + "-" + creationDate;
  }

  @Override
  public void logInfo() {

    LOGGER.info("Design file path: " + this.getDesignPathname());
    LOGGER.info("Workflow parameter file path: " + this.getParameterPathname());

    LOGGER.info("Workflow Author: " + this.getCommandAuthor());
    LOGGER.info("Workflow Description: " + this.getCommandDescription());
    LOGGER.info("Job Command name: " + this.getCommandName());

    LOGGER.info("Job Id: " + this.getJobId());
    LOGGER.info("Job UUID: " + this.getJobUUID());
    LOGGER.info("Job Description: " + this.getJobDescription());
    LOGGER.info("Job Environment: " + this.getJobEnvironment());

    LOGGER.info("Job Base path: " + this.getBasePathname());
    LOGGER.info("Job Output path: " + this.getOutputPathname());
    LOGGER.info("Job Log path: " + this.getLogPathname());
  }

  @Override
  public AbstractEoulsanRuntime getRuntime() {

    return EoulsanRuntime.getRuntime();
  }

  @Override
  public Settings getSettings() {

    return getRuntime().getSettings();
  }

  @Override
  public Logger getLogger() {

    return LOGGER;
  }

  private void checkInputDataFormat(final DataFormat df) {

    if (this.step == null)
      return;

    if (df == null)
      throw new EoulsanRuntimeException("The format is null");

    if (this.stepInputDataFormats != null
        && !this.stepInputDataFormats.contains(df))
      throw new EoulsanRuntimeException("The "
          + df.getFormatName() + " format is not an input format of the step "
          + this.step.getName());
  }

  private void checkOutputDataFormat(final DataFormat df) {

    if (this.step == null)
      return;

    if (df == null)
      throw new EoulsanRuntimeException("The format is null");

    if (this.stepOutputDataFormats != null
        && !this.stepOutputDataFormats.contains(df))
      throw new EoulsanRuntimeException("The "
          + df.getFormatName() + " format is not an output format of the step "
          + this.step.getName());
  }

  @Override
  public String getInputDataFilename(final DataFormat df, final Sample sample) {

    checkInputDataFormat(df);
    return getOtherDataFilename(df, sample);
  }

  @Override
  public String getInputDataFilename(final DataFormat df, final Sample sample,
      final int fileIndex) {

    checkInputDataFormat(df);
    return getOtherDataFilename(df, sample, fileIndex);
  }

  @Override
  public DataFile getInputDataFile(final DataFormat df, final Sample sample) {

    checkInputDataFormat(df);
    return getOtherDataFile(df, sample);
  }

  @Override
  public DataFile getInputDataFile(final DataFormat df, final Sample sample,
      final int fileIndex) {

    checkInputDataFormat(df);
    return getOtherDataFile(df, sample, fileIndex);
  }

  @Override
  public String getOutputDataFilename(final DataFormat df, final Sample sample) {

    checkOutputDataFormat(df);
    return getOtherDataFilename(df, sample);
  }

  @Override
  public String getOutputDataFilename(final DataFormat df, final Sample sample,
      final int fileIndex) {

    checkOutputDataFormat(df);
    return getOtherDataFilename(df, sample, fileIndex);
  }

  @Override
  public DataFile getOutputDataFile(final DataFormat df, final Sample sample) {

    checkOutputDataFormat(df);
    return getOtherDataFile(df, sample);
  }

  @Override
  public DataFile getOutputDataFile(final DataFormat df, final Sample sample,
      final int fileIndex) {

    checkOutputDataFormat(df);
    return getOtherDataFile(df, sample, fileIndex);
  }

  @Override
  public String getOtherDataFilename(final DataFormat df, final Sample sample) {

    final DataFile file = getOtherDataFile(df, sample);

    return file == null ? null : file.getSource();
  }

  @Override
  public String getOtherDataFilename(final DataFormat df, final Sample sample,
      final int fileIndex) {

    final DataFile file = getOtherDataFile(df, sample, fileIndex);

    return file == null ? null : file.getSource();
  }

  @Override
  public DataFile getOtherDataFile(final DataFormat df, final Sample sample) {

    if (df == null || sample == null)
      return null;

    if (df.getMaxFilesCount() != 1)
      throw new EoulsanRuntimeException(
          "Multifiles DataFormat are not handled by this method.");

    // Test if the file is defined in the design file
    final DataFile fileFromDesign = getFileFromDesign(sample, df);
    if (fileFromDesign != null)
      return fileFromDesign;

    // Else the file is in base path
    return new DataFile(this.getBasePathname()
        + '/' + ContextUtils.getNewDataFilename(df, sample));
  }

  @Override
  public DataFile getOtherDataFile(final DataFormat df, final Sample sample,
      final int fileIndex) {

    if (df == null || sample == null || fileIndex < 0)
      return null;

    if (df.getMaxFilesCount() < 2)
      throw new EoulsanRuntimeException(
          "Only multifiles DataFormat are handled by this method.");

    if (fileIndex > df.getMaxFilesCount())
      throw new EoulsanRuntimeException(
          "The file index is greater than the maximal number of file for this format.");

    // Test if the file is defined in the design file
    final DataFile fileFromDesign = getFileFromDesign(sample, df, fileIndex);
    if (fileFromDesign != null)
      return fileFromDesign;

    // Else the file is in base path
    return new DataFile(this.getBasePathname()
        + '/' + ContextUtils.getNewDataFilename(df, sample, fileIndex));
  }

  /**
   * Get a DataFile that has been defined in the design file
   * @param df the DataFormat of the source
   * @param sample the sample for the source
   * @return a DataFile object if the file has been defined in the design file
   */
  private DataFile getFileFromDesign(final Sample sample, final DataFormat df) {

    // First try search the file in the design file
    final DataType dt = df.getType();
    final String fieldName = this.dataTypesFields.get(dt);

    if (fieldName != null) {

      final DataFile file =
          new DataFile(sample.getMetadata().getField(fieldName));

      return isDesignDataFileValidFormat(file, dt, df) ? file : null;
    }

    return null;
  }

  /**
   * Get a DataFile that has been defined in the design file
   * @param df the DataFormat of the source
   * @param sample the sample for the source
   * @param fileIndex file index for multifile data
   * @return a DataFile object if the file has been defined in the design file
   */
  private DataFile getFileFromDesign(final Sample sample, final DataFormat df,
      final int fileIndex) {

    final DataType dt = df.getType();
    final String fieldName = this.dataTypesFields.get(dt);

    if (fieldName != null) {

      final List<String> fieldValues =
          sample.getMetadata().getFieldAsList(fieldName);

      if (fieldValues != null && fieldValues.size() > fileIndex) {

        final DataFile file = new DataFile(fieldValues.get(fileIndex));

        return isDesignDataFileValidFormat(file, dt, df) ? file : null;
      }
    }

    return null;
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

  @Override
  public int getDataFileCount(final DataFormat df, final Sample sample) {

    if (df == null || sample == null)
      return -1;

    if (df.getMaxFilesCount() < 2)
      throw new EoulsanRuntimeException(
          "Only multifiles DataFormat are handled by this method.");

    final DataType dt = df.getType();
    final String fieldName = this.dataTypesFields.get(dt);
    if (fieldName != null) {

      return sample.getMetadata().getFieldAsList(fieldName).size();
    }

    // Check existing files to get the file count

    int count = 0;
    boolean found = false;

    do {

      final DataFile file =
          new DataFile(this.getBasePathname()
              + '/' + ContextUtils.getNewDataFilename(df, sample, count));

      found = file.exists();
      if (found)
        count++;
    } while (found);

    return count;
  }

  @Override
  public DataFile getExistingInputDataFile(final DataFormat[] formats,
      final Sample sample) {

    if (formats == null)
      return null;

    for (DataFormat df : formats) {

      if (df == null)
        continue;

      final DataFile file;

      // TODO Very ugly, to change with paired-end support in MapReduce
      if (df.getMaxFilesCount() == 1)
        file = getOtherDataFile(df, sample);
      else
        file = getOtherDataFile(df, sample, 0);

      if (file != null && file.exists())
        return file;

    }

    return null;
  }

  @Override
  public DataFile getExistingInputDataFile(final DataFormat[] formats,
      final Sample sample, final int fileIndex) {

    if (formats == null)
      return null;

    for (DataFormat df : formats) {

      if (df == null)
        continue;

      final DataFile file;

      // TODO Very ugly, to change with paired-end support in MapReduce
      if (df.getMaxFilesCount() == 1)
        file = getOtherDataFile(df, sample, 0);
      else
        file = getOtherDataFile(df, sample, fileIndex);

      if (file != null && file.exists())
        return file;

    }

    return null;
  }

  @Override
  public InputStream getInputStream(final DataFormat df, final Sample sample)
      throws IOException {

    final DataFile file = getOtherDataFile(df, sample);

    if (file == null)
      return null;

    return file.open();
  }

  @Override
  public InputStream getInputStream(final DataFormat df, final Sample sample,
      final int fileIndex) throws IOException {

    final DataFile file = getOtherDataFile(df, sample, fileIndex);

    if (file == null)
      return null;

    return file.open();
  }

  @Override
  public InputStream getRawInputStream(final DataFormat df, final Sample sample)
      throws IOException {

    final DataFile file = getOtherDataFile(df, sample);

    return file == null ? null : file.rawOpen();
  }

  @Override
  public InputStream getRawInputStream(final DataFormat df,
      final Sample sample, final int fileIndex) throws IOException {

    final DataFile file = getOtherDataFile(df, sample, fileIndex);

    return file == null ? null : file.rawOpen();
  }

  @Override
  public OutputStream getOutputStream(final DataFormat df, final Sample sample)
      throws IOException {

    final DataFile file = getOtherDataFile(df, sample);

    return file == null ? null : file.create();
  }

  @Override
  public OutputStream getOutputStream(final DataFormat df, final Sample sample,
      final int fileIndex) throws IOException {

    final DataFile file = getOtherDataFile(df, sample, fileIndex);

    return file == null ? null : file.create();
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param millisSinceEpoch milliseconds since epoch (1.1.1970)
   */
  public SimpleContext(final long millisSinceEpoch,
      final String jobDescription, final String jobEnvironment) {

    createExecutionName(millisSinceEpoch);
    this.host = SystemUtils.getHostName();
    this.jobDescription = jobDescription == null ? "" : jobDescription;
    this.jobEnvironment = jobEnvironment == null ? "" : jobEnvironment;
  }

  /**
   * Constructor for a standard local job.
   * @param designFile design file
   * @param paramFile parameter file
   * @param jobDescription job description
   */
  public SimpleContext(final File designFile, final File paramFile,
      final String jobDescription) {

    this(designFile, paramFile, jobDescription, null);
  }

  /**
   * Constructor for a standard local job.
   * @param designFile design file
   * @param paramFile parameter file
   * @param jobDescription job description
   * @param jobEnvironment job environment
   */
  public SimpleContext(final File designFile, final File paramFile,
      final String jobDescription, final String jobEnvironment) {

    this(System.currentTimeMillis(), jobDescription, jobEnvironment);

    // Set the base path
    setBasePathname(designFile.getAbsoluteFile().getParentFile()
        .getAbsolutePath());

    // Set the design path
    setDesignPathname(designFile.getAbsolutePath());

    // Set the parameter path
    setParameterPathname(paramFile.getAbsolutePath());

    final File logDir =
        new File(designFile.getAbsoluteFile().getParent().toString()
            + "/" + getJobId());

    final File outputDir =
        new File(designFile.getAbsoluteFile().getParent().toString()
            + "/" + getJobId());

    // Set the output path
    setOutputPathname(outputDir.getAbsolutePath());

    // Set the log path
    setLogPathname(logDir.getAbsolutePath());
  }

}
