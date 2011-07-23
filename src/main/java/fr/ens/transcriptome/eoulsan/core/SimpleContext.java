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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import fr.ens.transcriptome.eoulsan.data.DataType;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.Step;

/**
 * This class define an simple ExecutorInfo.
 * @author Laurent Jourdren
 */
public final class SimpleContext implements Context {

  /** Logger. */
  protected static final Logger logger = Logger.getLogger(Globals.APP_NAME);

  private String basePathname;
  private String logPathname;
  private String outputPathname;
  private String jobId;
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

  private final Map<DataType, String> dataTypesFields = Maps.newHashMap();
  private String objectCreationDate;

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
   * @param logPath The log Path to set
   */
  public void setLogPathname(final String logPath) {

    logger.info("Log path: " + logPath);
    this.logPathname = logPath;
  }

  /**
   * Set the output path
   * @param logPathname The output Path to set
   */
  public void setOutputPathname(final String outputPath) {

    logger.info("Output path: " + outputPath);
    this.outputPathname = outputPath;
  }

  /**
   * Set the design path
   * @param designPathname The design Path to set
   */
  public void setDesignPathname(final String designPathname) {

    logger.info("Design path: " + designPathname);
    this.designPathname = designPathname;
  }

  /**
   * Set the parameter path
   * @param paramPathname The parameter Path to set
   */
  public void setParameterPathname(final String paramPathname) {

    logger.info("Parameter path: " + paramPathname);
    this.paramPathname = paramPathname;
  }

  /**
   * Set the jar path
   * @param jarPathname The jar Path to set
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

  private void createExecutionName() {

    final Calendar cal = Calendar.getInstance(Locale.ENGLISH);
    cal.setTime(new Date(System.currentTimeMillis()));

    this.objectCreationDate =
        String.format("%04d%02d%02d-%02d%02d%02d", cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
            cal.get(Calendar.SECOND));

    this.jobId = Globals.APP_NAME_LOWER_CASE + "-" + this.objectCreationDate;
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

  @Override
  public String getDataFilename(final DataFormat df, final Sample sample) {

    final DataFile file = getDataFile(df, sample);

    return file == null ? null : file.getSource();
  }

  @Override
  public DataFile getDataFile(final DataFormat df, final Sample sample) {

    if (df == null || sample == null)
      return null;

    if (df.isMultiFiles())
      throw new EoulsanRuntimeException(
          "Multifiles DataFormat are not handled by getDataFile()");

    // First try search the file in the design file

    final DataType dt = df.getType();
    final String fieldName = this.dataTypesFields.get(dt);
    if (fieldName != null) {
      final DataFormatRegistry dfr = DataFormatRegistry.getInstance();
      final DataFile file = new DataFile(sample.getMetadata().getField(fieldName));
      final DataFormat sourceDf =
          dfr.getDataFormatFromExtension(dt, file.getExtension());

      if (sourceDf != null && sourceDf.equals(df))
        return file;
    }

    // Else the file is in base path

    return new DataFile(this.getBasePathname()
        + "/" + df.getType().getPrefix()
        + (df.getType().isOneFilePerAnalysis() ? "1" : sample.getId())
        + df.getDefaultExtention());
  }

  @Override
  public DataFile getExistingDataFile(final DataFormat[] formats,
      final Sample sample) {

    if (formats == null)
      return null;

    for (DataFormat df : formats) {

      if (df == null)
        continue;

      final DataFile file = getDataFile(df, sample);

      if (file != null && file.exists())
        return file;

    }

    return null;
  }

  @Override
  public InputStream getInputStream(final DataFormat df, final Sample sample)
      throws IOException {

    final DataFile file = getDataFile(df, sample);

    if (file == null)
      return null;

    return file.open();
  }

  @Override
  public InputStream getRawInputStream(final DataFormat df, final Sample sample)
      throws IOException {

    final DataFile file = getDataFile(df, sample);

    return file == null ? null : file.rawOpen();
  }

  @Override
  public OutputStream getOutputStream(final DataFormat df, final Sample sample)
      throws IOException {

    final DataFile file = getDataFile(df, sample);

    return file == null ? null : file.create();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public SimpleContext() {

    createExecutionName();
  }

}
