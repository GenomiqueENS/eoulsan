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

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.logging.Logger;

import fr.ens.biologie.genomique.eoulsan.AbstractEoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Settings;
import fr.ens.biologie.genomique.eoulsan.core.Workflow;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;
import fr.ens.biologie.genomique.kenetre.util.SystemUtils;

/**
 * This class define the context implementation.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class WorkflowContext implements Serializable {

  /** Serialization version UID. */
  private static final long serialVersionUID = -260344001954382358L;

  private final String jobId;
  private final String host;
  private DataFile designFile;
  private DataFile workflowFile;
  private DataFile jarFile;
  private final String jobUUID;
  private final String jobDescription;
  private final String jobEnvironment;
  private String commandName = "";
  private String commandDescription = "";
  private String commandAuthor = "";
  private final AbstractWorkflow workflow;

  private final long contextCreationTime;

  //
  // Getters
  //

  /**
   * Get the local working directory.
   * @return Returns the local working path
   */
  public DataFile getLocalWorkingDirectory() {

    return this.workflow.getLocalWorkingDirectory();
  }

  /**
   * Get the local working directory.
   * @return Returns the local working directory
   */
  public DataFile getHadoopWorkingDirectory() {

    return this.workflow.getHadoopWorkingDirectory();
  }

  /**
   * Get the output directory.
   * @return Returns the output directory
   */
  public DataFile getOutputDirectory() {

    return this.workflow.getOutputDirectory();
  }

  /**
   * Get the job path.
   * @return Returns the log Path
   */
  public DataFile getJobDirectory() {

    return this.workflow.getJobDirectory();
  }

  /**
   * Get the task directory.
   * @return Returns the task directory
   */
  public DataFile getTaskDirectory() {

    return this.workflow.getTaskDirectory();
  }

  /**
   * Get the data repository directory.
   * @return Returns the data repository directory
   */
  public DataFile getDataRepositoryDirectory() {

    return this.workflow.getDataRepositoryDirectory();
  }

  /**
   * Get the job id.
   * @return the job id
   */
  public String getJobId() {
    return this.jobId;
  }

  /**
   * Get the host of the job.
   * @return a string with the host of the job
   */
  public String getJobHost() {
    return this.host;
  }

  /**
   * Get the creation time of the context.
   * @return the creation time of the context in milliseconds since epoch
   *         (1.1.1970)
   */
  public long getContextCreationTime() {
    return this.contextCreationTime;
  }

  /**
   * Get the design file.
   * @return the design file
   */
  public DataFile getDesignFile() {
    return this.designFile;
  }

  /**
   * Get the workflow file.
   * @return the workflow file
   */
  public DataFile getWorkflowFile() {
    return this.workflowFile;
  }

  /**
   * Get the application jar file.
   * @return Returns the jar file
   */
  public DataFile getJarFile() {
    return this.jarFile;
  }

  /**
   * Get the UUID of the job.
   * @return the job UUID
   */
  public String getJobUUID() {
    return this.jobUUID;
  }

  /**
   * Get the job description.
   * @return the job description
   */
  public String getJobDescription() {
    return this.jobDescription;
  }

  /**
   * Get the job environment.
   * @return the job environment
   */
  public String getJobEnvironment() {
    return this.jobEnvironment;
  }

  /**
   * Get the command name.
   * @return the command name
   */
  public String getCommandName() {
    return this.commandName;
  }

  /**
   * Get command description.
   * @return the command description
   */
  public String getCommandDescription() {
    return this.commandDescription;
  }

  /**
   * Get the command author.
   * @return the command author
   */
  public String getCommandAuthor() {
    return this.commandAuthor;
  }

  /**
   * Get the workflow description.
   * @return the workflow description
   */
  public Workflow getWorkflow() {

    return this.workflow;
  }

  //
  // Setters
  //

  /**
   * Set the design file.
   * @param designFile The design file to set
   */
  public void setDesignFile(final DataFile designFile) {

    this.designFile = designFile;
  }

  /**
   * Set the workflow file.
   * @param workflowFile The workflow file to set
   */
  public void setWorkflowFile(final DataFile workflowFile) {

    this.workflowFile = workflowFile;
  }

  /**
   * Set the jar file.
   * @param jarFile The jar file to set
   */
  public void setJarFile(final DataFile jarFile) {

    this.jarFile = jarFile;
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

  //
  // Other methods
  //

  /**
   * Get EoulsanRuntime.
   * @return the EoulsanRuntime
   */
  public AbstractEoulsanRuntime getRuntime() {

    return EoulsanRuntime.getRuntime();
  }

  /**
   * Get Eoulsan settings.
   * @return the Settings
   */
  public Settings getSettings() {

    return getRuntime().getSettings();
  }

  /**
   * Get the logger.
   * @return the logger
   */
  public Logger getLogger() {

    return EoulsanLogger.getLogger();
  }

  /**
   * Get the generic logger.
   * @return the generic logger
   */
  public GenericLogger getGenericLogger() {

    return EoulsanLogger.getGenericLogger();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param arguments arguments object
   */
  WorkflowContext(final ExecutorArguments arguments,
      final AbstractWorkflow workflow) {

    requireNonNull(arguments.getLocalWorkingPathname(),
        "arguments cannot be null");
    requireNonNull(workflow, "workflow cannot be null");

    this.workflow = workflow;
    this.jobId = arguments.getJobId();
    this.jobUUID = arguments.getJobUUID();
    this.contextCreationTime = arguments.getCreationTime();
    this.host = SystemUtils.getHostName();

    requireNonNull(arguments.getLocalWorkingPathname(),
        "base path cannot be null");
    requireNonNull(arguments.getWorkflowPathname(),
        "parameter path cannot be null");
    requireNonNull(arguments.getDesignPathname(), "design cannot be null");
    requireNonNull(arguments.getOutputPathname(), "output path cannot be null");
    requireNonNull(arguments.getJobPathname(), "log path cannot be null");
    requireNonNull(arguments.getJobDescription(),
        "job description cannot be null");
    requireNonNull(arguments.getJobEnvironment(),
        "job environment cannot be null");

    this.workflowFile = new DataFile(arguments.getWorkflowPathname());
    this.designFile = new DataFile(arguments.getDesignPathname());
    this.jobDescription = arguments.getJobDescription();
    this.jobEnvironment = arguments.getJobEnvironment();
  }

}
