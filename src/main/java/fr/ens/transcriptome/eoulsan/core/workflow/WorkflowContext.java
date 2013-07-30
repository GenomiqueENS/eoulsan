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

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.AbstractEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.util.SystemUtils;

/**
 * This class define the context implementation.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class WorkflowContext implements Context {

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
  private Workflow workflow;
  private AbstractWorkflowStep step;

  private long contextCreationTime;

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
  public Workflow getWorkflow() {

    return this.workflow;
  }

  @Override
  public WorkflowStep getCurrentStep() {
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
   * Set the workflow of the execution
   * @param workflow the workflow to set
   */
  void setWorkflow(final Workflow workflow) {

    this.workflow = workflow;
  }

  /**
   * Set job description.
   * @param jobDescription job description
   */
  public void setJobDescription(final String jobDescription) {

    logger.info("Job description: " + jobDescription);
    this.jobDescription = jobDescription;
  }

  /**
   * Set job environment.
   * @param jobEnvironment job environment
   */
  public void setJobEnvironment(final String jobEnvironment) {

    logger.info("Job environmnent: " + jobEnvironment);
    this.jobEnvironment = jobEnvironment;
  }

  /**
   * Set the current step running.
   * @param step step to set
   */
  public void setStep(final AbstractWorkflowStep step) {

    this.step = step;
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

  @Override
  public String getInputDataFilename(final DataFormat format,
      final Sample sample) {

    return this.step.getInputDataFile(format, sample).getSource();
  }

  @Override
  public String getInputDataFilename(final DataFormat format,
      final Sample sample, final int fileIndex) {

    return this.step.getInputDataFile(format, sample, fileIndex).getSource();
  }

  @Override
  public DataFile getInputDataFile(final DataFormat format, final Sample sample) {

    return this.step.getInputDataFile(format, sample);
  }

  @Override
  public DataFile getInputDataFile(final DataFormat format,
      final Sample sample, final int fileIndex) {

    return this.step.getInputDataFile(format, sample, fileIndex);
  }

  @Override
  public int getInputDataFileCount(final DataFormat format, final Sample sample) {

    return this.step.getInputDataFileCount(format, sample, true);
  }

  @Override
  public String getOutputDataFilename(final DataFormat format,
      final Sample sample) {

    return this.step.getOutputDataFile(format, sample).getSource();
  }

  @Override
  public String getOutputDataFilename(final DataFormat format,
      final Sample sample, final int fileIndex) {

    return this.step.getOutputDataFile(format, sample, fileIndex).getSource();
  }

  @Override
  public DataFile getOutputDataFile(final DataFormat format, final Sample sample) {

    return this.step.getOutputDataFile(format, sample);
  }

  @Override
  public DataFile getOutputDataFile(final DataFormat format,
      final Sample sample, final int fileIndex) {

    return this.step.getOutputDataFile(format, sample, fileIndex);
  }

  @Override
  public int getOutputDataFileCount(final DataFormat format, final Sample sample) {

    return this.step.getOutputDataFileCount(format, sample, true);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param millisSinceEpoch milliseconds since epoch (1.1.1970)
   */
  public WorkflowContext(final long millisSinceEpoch,
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
  public WorkflowContext(final File designFile, final File paramFile,
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
  public WorkflowContext(final File designFile, final File paramFile,
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
