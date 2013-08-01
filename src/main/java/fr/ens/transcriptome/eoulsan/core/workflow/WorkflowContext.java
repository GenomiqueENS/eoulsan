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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.AbstractEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.ExecutorArguments;
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
  protected static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private String basePathname;
  private String logPathname;
  private String outputPathname;
  private final String jobId;
  private final String host;
  private String designPathname;
  private String workflowPathname;
  private String jarPathname;
  private final String jobUUID;
  private final String jobDescription;
  private final String jobEnvironment;
  private String commandName = "";
  private String commandDescription = "";
  private String commandAuthor = "";
  private Workflow workflow;
  private AbstractWorkflowStep step;

  private final long contextCreationTime;

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
  public String getWorkflowPathname() {
    return this.workflowPathname;
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
   * Set the design file path
   * @param designPathname The design file path to set
   */
  public void setDesignPathname(final String designPathname) {

    this.designPathname = designPathname;
  }

  /**
   * Set the workflow file path
   * @param workflowPathname The workflow file path to set
   */
  public void setWorkflowPathname(final String workflowPathname) {

    this.workflowPathname = workflowPathname;
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
   * Set the workflow of the execution
   * @param workflow the workflow to set
   */
  void setWorkflow(final Workflow workflow) {

    this.workflow = workflow;
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
   * Constructor.
   * @param arguments arguments object
   */
  WorkflowContext(final ExecutorArguments arguments) {

    checkNotNull(arguments.getBasePathname(), "arguments cannot be null");

    this.jobId = arguments.getJobId();
    this.jobUUID = arguments.getJobUUID();
    this.contextCreationTime = arguments.getCreationTime();
    this.host = SystemUtils.getHostName();

    checkNotNull(arguments.getBasePathname(), "base path cannot be null");
    checkNotNull(arguments.getWorkflowPathname(),
        "parameter path cannot be null");
    checkNotNull(arguments.getDesignPathname(), "design cannot be null");
    checkNotNull(arguments.getOutputPathname(), "output path cannot be null");
    checkNotNull(arguments.getLogPathname(), "log path cannot be null");
    checkNotNull(arguments.getJobDescription(),
        "job description cannot be null");
    checkNotNull(arguments.getJobEnvironment(),
        "job environment cannot be null");

    this.basePathname = arguments.getBasePathname();
    this.workflowPathname = arguments.getWorkflowPathname();
    this.designPathname = arguments.getDesignPathname();
    this.outputPathname = arguments.getOutputPathname();
    this.logPathname = arguments.getLogPathname();
    this.jobDescription = arguments.getJobDescription();
    this.jobEnvironment = arguments.getJobEnvironment();
  }

}
