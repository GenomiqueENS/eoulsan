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
import static fr.ens.biologie.genomique.kenetre.util.StringUtils.toCompactTime;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

import com.google.common.base.MoreObjects;

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;

/**
 * This class contains arguments for the Executor.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ExecutorArguments {

  private String localWorkingPathname;
  private String hadoopWorkingPathname;
  private String designPathname;
  private String workflowPathname;
  private String temporaryPathname;
  private String jobDescription = "";
  private String jobEnvironment = "";
  private String outputPathname;
  private String jobPathname;
  private String taskPathname;
  private String dataPathname;
  private final String jobId;
  private final String jobUUID = UUID.randomUUID().toString();
  private final long creationTime;

  //
  // Getters
  //

  /**
   * Get the local working path.
   * @return Returns the local working path
   */
  public final String getLocalWorkingPathname() {
    return this.localWorkingPathname;
  }

  /**
   * Get the Hadoop working path.
   * @return Returns the local working path
   */
  public final String getHadoopWorkingPathname() {
    return this.hadoopWorkingPathname;
  }

  /**
   * Get the job path.
   * @return Returns the job Path
   */
  public final String getJobPathname() {
    return this.jobPathname;
  }

  /**
   * Get the task path.
   * @return Returns the task Path
   */
  public final String getTaskPathname() {
    return this.taskPathname;
  }

  /**
   * Get the output path.
   * @return Returns the output Path
   */
  public final String getOutputPathname() {
    return this.outputPathname;
  }

  /**
   * Get the design file path.
   * @return the design file path
   */
  public final String getDesignPathname() {
    return this.designPathname;
  }

  /**
   * Get the workflow file path.
   * @return the workflow file path
   */
  public final String getWorkflowPathname() {
    return this.workflowPathname;
  }

  /**
   * Get the temporary directory path.
   * @return the temporary directory file path
   */
  public final String getTemporaryPathname() {
    return this.temporaryPathname;
  }

  /**
   * Get the data directory path.
   * @return the temporary directory file path
   */
  public final String getDataPathname() {
    return this.dataPathname;
  }

  /**
   * Get the job description.
   * @return the job description
   */
  public final String getJobDescription() {
    return this.jobDescription == null ? "" : this.jobDescription.trim();
  }

  /**
   * Get the job environment.
   * @return the job environment
   */
  public final String getJobEnvironment() {
    return this.jobEnvironment == null ? "" : this.jobEnvironment.trim();
  }

  /**
   * Get the job id.
   * @return the job id
   */
  public final String getJobId() {
    return this.jobId;
  }

  public final String getJobUUID() {
    return this.jobUUID;
  }

  /**
   * Get the creation time of the job.
   * @return the creation time of the context in milliseconds since epoch
   *         (1.1.1970)
   */
  public final long getCreationTime() {
    return this.creationTime;
  }

  //
  // Setters
  //

  /**
   * Set the local working path.
   * @param localWorkingPath The local working path to set
   */
  public final void setLocalWorkingPathname(final String localWorkingPath) {

    if (localWorkingPath == null) {
      return;
    }

    this.localWorkingPathname = localWorkingPath.trim();
  }

  /**
   * Set the local working path.
   * @param hadoopWorkingPath The local working path to set
   */
  public final void setHadoopWorkingPathname(final String hadoopWorkingPath) {

    if (hadoopWorkingPath == null) {
      return;
    }

    this.hadoopWorkingPathname = hadoopWorkingPath.trim();
  }

  /**
   * Set the job path
   * @param jobPath The log path to set
   */
  public final void setJobPathname(final String jobPath) {

    if (jobPath == null) {
      return;
    }

    this.jobPathname = jobPath.trim();
  }

  /**
   * Set the task path
   * @param taskPath The task path to set
   */
  public final void setTaskPathname(final String taskPath) {

    if (taskPath == null) {
      return;
    }

    this.taskPathname = taskPath.trim();
  }

  /**
   * Set the output path
   * @param outputPath The output path to set
   */
  public final void setOutputPathname(final String outputPath) {

    if (outputPath == null) {
      return;
    }

    this.outputPathname = outputPath.trim();
  }

  /**
   * Set the design file path.
   * @param designPathname The design path to set
   */
  public final void setDesignPathname(final String designPathname) {

    if (designPathname == null) {
      return;
    }

    this.designPathname = designPathname.trim();
  }

  /**
   * Set the workflow file path.
   * @param workflowPathname The workflow file path to set
   */
  public final void setWorkflowPathname(final String workflowPathname) {

    if (workflowPathname == null) {
      return;
    }

    this.workflowPathname = workflowPathname.trim();
  }

  /**
   * Set the temporary directory path.
   * @param temporaryPathname The temporary directory path to set
   */
  public final void setTemporaryPathname(final String temporaryPathname) {

    if (temporaryPathname == null) {
      return;
    }

    this.temporaryPathname = temporaryPathname;
  }

  /**
   * Set the data directory path.
   * @param dataPathname The data directory path to set
   */
  public final void setDataPathname(final String dataPathname) {

    if (dataPathname == null) {
      return;
    }

    this.dataPathname = dataPathname;
  }

  /**
   * Set job description.
   * @param jobDescription job description
   */
  public final void setJobDescription(final String jobDescription) {

    if (jobDescription == null) {
      return;
    }

    this.jobDescription = jobDescription.trim();
  }

  /**
   * Set job environment.
   * @param jobEnvironment job environment
   */
  public final void setJobEnvironment(final String jobEnvironment) {

    if (jobEnvironment == null) {
      return;
    }

    this.jobEnvironment = jobEnvironment.trim();
  }

  //
  // Other methods
  //

  /**
   * Open the Workflow file.
   * @return an InputStream with the content of the workflow file
   * @throws IOException if an error occurs while opening the workflow file
   */
  public InputStream openParamFile() throws IOException {

    return new DataFile(getWorkflowPathname()).open();
  }

  /**
   * Open the design file.
   * @return an InputStream with the content of the design file
   * @throws IOException if an error occurs while opening the design file
   */
  public InputStream openDesignFile() throws IOException {

    return new DataFile(getDesignPathname()).open();
  }

  /**
   * Create the log path.
   * @param logFilename log file name
   * @return a String with an URI for the log
   */
  public URI logPath(final String logFilename) {

    final File logDir = new File(URI.create(getJobPathname()).getPath());

    return new File(logDir, logFilename).getAbsoluteFile().toURI();
  }

  @Override
  public String toString() {

    return MoreObjects.toStringHelper(this)
        .add("localWorkingPathname", getLocalWorkingPathname())
        .add("hadoopWorkingPathname", getHadoopWorkingPathname())
        .add("designPathname", getDesignPathname())
        .add("workflowPathname", getWorkflowPathname())
        .add("jobDescription", getJobDescription())
        .add("jobEnvironment", getJobEnvironment())
        .add("outputPathname", getOutputPathname())
        .add("logPathname", getJobPathname()).add("jobId", getJobId())
        .add("jobUUID", getJobUUID()).add("creationTime", getCreationTime())
        .toString();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public ExecutorArguments() {

    this(System.currentTimeMillis());
  }

  /**
   * Public constructor.
   * @param millisSinceEpoch creation time of the job
   */
  public ExecutorArguments(final long millisSinceEpoch) {

    this.creationTime = millisSinceEpoch;
    this.jobId =
        Globals.APP_NAME_LOWER_CASE + "-" + toCompactTime(millisSinceEpoch);
  }

  /**
   * Public constructor.
   * @param workflowFile workflow file
   * @param designFile design file
   */
  public ExecutorArguments(final File workflowFile, final File designFile) {

    this();

    requireNonNull(workflowFile, "The workflow file is null");
    requireNonNull(designFile, "The design file is null");
    checkArgument(workflowFile.exists(), "The workflow file does not exists");
    checkArgument(designFile.exists(), "The design file does not exists");

    final File outputDir = new File(designFile.getAbsoluteFile().getParent());

    final File jobDir = new File(outputDir, getJobId());
    final File workingDir = new File(jobDir, "working");
    final File taskDir = new File(jobDir, "tasks");
    final File dataDir =
        new File(outputDir, Globals.APP_NAME_LOWER_CASE + "-data");
    final File tmpDir = new File(jobDir, "tmp");

    // Set the local working path
    setLocalWorkingPathname(workingDir.getAbsolutePath());

    // Set the design path
    setDesignPathname(designFile.getAbsolutePath());

    // Set the parameter path
    setWorkflowPathname(workflowFile.getAbsolutePath());

    // Set the output path
    setOutputPathname(outputDir.getAbsolutePath());

    // Set the job path
    setJobPathname(jobDir.getAbsolutePath());

    // Set the tasks path
    setTaskPathname(taskDir.getAbsolutePath());

    // Set the data path
    setDataPathname(dataDir.getAbsolutePath());

    // Set the temporary directory
    setTemporaryPathname(tmpDir.getAbsolutePath());
  }

}
