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
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
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
public final class SimpleContext implements Context {

  /** Logger. */
  protected static final Logger logger = Logger.getLogger(Globals.APP_NAME);

  private String basePathname;
  private String logPathname;
  private String outputPathname;
  private String jobId;
  private final String host;
  private String designPathname;
  private String paramPathname;
  private String jarPathname;
  private final String jobUUID = UUID.randomUUID().toString();
  private String jobDescription = "";
  private String jobEnvironment = "";
  private String commandName = "";
  private String commandDescription = "";
  private String commandAuthor = "";
  private WorkflowDescription workflow;
  private Step step;
  private Set<DataFormat> stepInputDataFormats;
  private Set<DataFormat> stepOutputDataFormats;

  private long contextCreationTime;

  private final Map<DataFormat, String> dataFormatsFields = Maps.newHashMap();

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

    logger.info("Base path: " + basePath);
    this.basePathname = basePath;
  }

  /**
   * Set the log path
   * @param logPath The log path to set
   */
  public void setLogPathname(final String logPath) {

    logger.info("Log path: " + logPath);
    this.logPathname = logPath;
  }

  /**
   * Set the output path
   * @param outputPath The output path to set
   */
  public void setOutputPathname(final String outputPath) {

    logger.info("Output path: " + outputPath);
    this.outputPathname = outputPath;
  }

  /**
   * Set the design path
   * @param designPathname The design path to set
   */
  public void setDesignPathname(final String designPathname) {

    logger.info("Design path: " + designPathname);
    this.designPathname = designPathname;
  }

  /**
   * Set the parameter path
   * @param paramPathname The parameter path to set
   */
  public void setParameterPathname(final String paramPathname) {

    logger.info("Parameter path: " + paramPathname);
    this.paramPathname = paramPathname;
  }

  /**
   * Set the jar path
   * @param jarPathname The jar path to set
   */
  public void setJarPathname(final String jarPathname) {

    logger.info("Jar path: " + jarPathname);
    this.jarPathname = jarPathname;
  }

  /**
   * Set command name
   * @param commandName the command name
   */
  void setCommandName(final String commandName) {

    logger.info("Command name: " + commandName);
    this.commandName = commandName;
  }

  /**
   * Set command description
   * @param commandDescription the command name
   */
  void setCommandDescription(final String commandDescription) {

    logger.info("Command description: " + commandDescription);
    this.commandDescription = commandDescription;
  }

  /**
   * Set command author
   * @param commandAuthor the command name
   */
  void setCommandAuthor(final String commandAuthor) {

    logger.info("Command author: " + commandAuthor);
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
   * Set job description.
   * @param jobDescription job description
   */
  void setJobDescription(final String jobDescription) {

    logger.info("Job description: " + jobDescription);
    this.jobDescription = jobDescription;
  }

  /**
   * Set job environment.
   * @param jobEnvironment job environment
   */
  void setJobEnvironment(final String jobEnvironment) {

    logger.info("Job environmnent: " + jobEnvironment);
    this.jobEnvironment = jobEnvironment;
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

      DataFormat dt = registry.getDataFormatForDesignField(fieldname);
      if (dt != null)
        this.dataFormatsFields.put(dt, fieldname);
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

    logger.info("EXECINFO Design path: " + this.getDesignPathname());
    logger.info("EXECINFO Parameter path: " + this.getParameterPathname());

    logger.info("EXECINFO Author: " + this.getCommandAuthor());
    logger.info("EXECINFO Description: " + this.getCommandDescription());
    logger.info("EXECINFO Command name: " + this.getCommandName());

    logger.info("EXECINFO Job Id: " + this.getJobId());
    logger.info("EXECINFO Job UUID: " + this.getJobUUID());
    logger.info("EXECINFO Job Description: " + this.getJobDescription());
    logger.info("EXECINFO Job Environment: " + this.getJobEnvironment());

    logger.info("EXECINFO Base path: " + this.getBasePathname());
    logger.info("EXECINFO Output path: " + this.getOutputPathname());
    logger.info("EXECINFO Log path: " + this.getLogPathname());
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

    return logger;
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
    final String fieldName = this.dataFormatsFields.get(df);

    if (fieldName != null) {

      return new DataFile(sample.getMetadata().getField(fieldName));
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

    final String fieldName = this.dataFormatsFields.get(df);

    if (fieldName != null) {

      final List<String> fieldValues =
          sample.getMetadata().getFieldAsList(fieldName);

      if (fieldValues != null && fieldValues.size() > fileIndex) {

        return new DataFile(fieldValues.get(fileIndex));
      }
    }

    return null;
  }

  @Override
  public int getDataFileCount(final DataFormat df, final Sample sample) {

    if (df == null || sample == null)
      return -1;

    if (df.getMaxFilesCount() < 2)
      throw new EoulsanRuntimeException(
          "Only multifiles DataFormat are handled by this method.");

    final String fieldName = this.dataFormatsFields.get(df);
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
  // Constructor
  //

  /**
   * Public constructor.
   */
  public SimpleContext() {

    this(System.currentTimeMillis());
  }

  /**
   * Public constructor.
   * @param millisSinceEpoch milliseconds since epoch (1.1.1970)
   */
  public SimpleContext(final long millisSinceEpoch) {
    createExecutionName(millisSinceEpoch);
    this.host = SystemUtils.getHostName();
  }

}
