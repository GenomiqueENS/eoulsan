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

import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.AbstractEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.core.workflow.Workflow;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Sample;

/**
 * This class define a context when process a sample in a step.
 * @since 1.3
 * @author Laurent Jourdren
 */
public class SampleStepContext {

  private final StepContext context;
  private final Sample sample;

  /**
   * Get the sample.
   * @return the sample
   */
  public Sample getSample() {

    return this.sample;
  }

  /**
   * Get the command name.
   * @return the command name
   */
  public String getCommandName() {

    return this.context.getCommandName();
  }

  /**
   * Get the UUID of the job.
   * @return the job UUID
   */
  public String getJobUUID() {

    return this.context.getJobUUID();
  }

  /**
   * Get the job description.
   * @return the job description
   */
  public String getJobDescription() {

    return this.context.getJobDescription();
  }

  /**
   * Get the job environment.
   * @return the job environment
   */
  public String getJobEnvironment() {

    return this.context.getJobEnvironment();
  }

  /**
   * Get command description.
   * @return the command description
   */
  public String getCommandDescription() {

    return this.context.getCommandDescription();
  }

  /**
   * Get the command author.
   * @return the command author
   */
  public String getCommandAuthor() {

    return this.context.getCommandAuthor();
  }

  /**
   * Get the local working path.
   * @return Returns the local working Path
   */
  public String getLocalWorkingPathname() {

    return this.context.getLocalWorkingPathname();
  }

  /**
   * Get the Hadoop working path.
   * @return Returns the Hadoop working Path
   */
  public String getHadoopWorkingPathname() {

    return this.context.getHadoopWorkingPathname();
  }

  /**
   * Get the log path.
   * @return Returns the log Path
   */
  public String getLogPathname() {

    return this.context.getLogPathname();
  }

  /**
   * Get the output path.
   * @return Returns the output Path
   */
  public String getOutputPathname() {

    return this.context.getOutputPathname();
  }

  /**
   * Get the step working path.
   * @return Returns the step working path
   */
  public String getStepWorkingPathname() {

    return this.context.getStepWorkingPathname();
  }

  /**
   * Get the job id.
   * @return the job id
   */
  public String getJobId() {

    return this.context.getJobId();
  }

  /**
   * Get the host of the job.
   * @return a string with the host of the job
   */
  public String getJobHost() {

    return this.context.getJobHost();
  }

  /**
   * Get the creation time of the context.
   * @return the creation time of the context in milliseconds since epoch
   *         (1.1.1970)
   */
  public long getContextCreationTime() {

    return this.context.getContextCreationTime();
  }

  /**
   * Get the design file path.
   * @return the design file path
   */
  public String getDesignPathname() {

    return this.context.getDesignPathname();
  }

  /**
   * Get the workflow file path.
   * @return the workflow file path
   */
  public String getWorkflowPathname() {

    return this.context.getWorkflowPathname();
  }

  /**
   * Get the application jar path.
   * @return Returns the jar path
   */
  public String getJarPathname() {

    return this.context.getJarPathname();
  }

  /**
   * Get EoulsanRuntime.
   * @return the EoulsanRuntime
   */
  public AbstractEoulsanRuntime getRuntime() {

    return this.context.getRuntime();
  }

  /**
   * Get Eoulsan settings.
   * @return the Settings
   */
  public Settings getSettings() {

    return this.context.getSettings();
  }

  /**
   * Get the logger.
   * @return the logger
   */
  public Logger getLogger() {

    return this.context.getLogger();
  }

  /**
   * Get the workflow description
   * @return the workflow description
   */
  public Workflow getWorkflow() {

    return this.context.getWorkflow();
  }

  /**
   * Get the current step.
   * @return the current Step or null if no Step is currently running.
   */
  public WorkflowStep getCurrentStep() {

    return this.context.getCurrentStep();
  }

  /**
   * Get the pathname for an input DataType and a Sample.
   * @param format the DataFormat of the source
   * @return a String with the pathname
   */
  public String getInputDataFilename(final DataFormat format) {

    return this.context.getInputDataFilename(format, this.sample);
  }

  /**
   * Get the pathname for an input DataType and a Sample. This method works only
   * for a multifile DataFormat.
   * @param format the DataFormat of the source
   * @param fileIndex file index for multifile data
   * @return a String with the pathname
   * @throws EoulsanRuntimeException if the DataFormat is not multifile
   */
  public String getInputDataFilename(final DataFormat format,
      final int fileIndex) {

    return this.context.getInputDataFilename(format, this.sample, fileIndex);
  }

  /**
   * Get the DataFile for an input DataType and a Sample.
   * @param format the DataFormat of the source
   * @return a new DataFile object
   */
  public DataFile getInputDataFile(final DataFormat format) {

    return this.context.getInputDataFile(format, this.sample);
  }

  /**
   * Get the DataFile for an input DataType and a Sample. This method works only
   * for a multifile DataFormat.
   * @param format the DataFormat of the source
   * @param fileIndex file index for multifile data
   * @return a new DataFile object
   * @throws EoulsanRuntimeException if the DataFormat is not multifile
   */
  public DataFile getInputDataFile(final DataFormat format, final int fileIndex) {

    return this.context.getInputDataFile(format, this.sample, fileIndex);
  }

  /**
   * Get the pathname for an output DataType and a Sample.
   * @param format the DataFormat of the source
   * @return a String with the pathname
   */
  public String getOutputDataFilename(final DataFormat format) {

    return this.context.getOutputDataFilename(format, this.sample);
  }

  /**
   * Get the pathname for an output DataType and a Sample. This method works
   * only for a multifile DataFormat.
   * @param format the DataFormat of the source
   * @param fileIndex file index for multifile data
   * @return a String with the pathname
   * @throws EoulsanRuntimeException if the DataFormat is not multifile
   */
  public String getOutputDataFilename(final DataFormat format,
      final int fileIndex) {

    return this.context.getOutputDataFilename(format, this.sample, fileIndex);
  }

  /**
   * Get the DataFile for a DataType and a Sample.
   * @param format the DataFormat of the source
   * @return a new DataFile object
   */
  public DataFile getOutputDataFile(final DataFormat format) {

    return this.context.getOutputDataFile(format, this.sample);
  }

  /**
   * Get the DataFile for an output DataType and a Sample. This method works
   * only for a multifile DataFormat.
   * @param format the DataFormat of the source
   * @param sample the sample for the source
   * @param fileIndex file index for multifile data
   * @return a new DataFile object
   * @throws EoulsanRuntimeException if the DataFormat is not multifile
   */
  public DataFile getOutputDataFile(final DataFormat format, final int fileIndex) {

    return this.context.getOutputDataFile(format, this.sample, fileIndex);
  }

  /**
   * Count the number for DataFile available for a multifile DataFormat and a
   * Sample. This method works only for a multifile DataFormat.
   * @param format the DataFormat of the source
   * @return the number of multifile for the DataFormat and the sample
   * @throws EoulsanRuntimeException if the DataFormat is not multifile
   */
  public int getInputDataFileCount(final DataFormat format) {

    return this.context.getInputDataFileCount(format, this.sample);
  }

  /**
   * Count the number for DataFile available for a multifile DataFormat and a
   * Sample. This method works only for a multifile DataFormat.
   * @param format the DataFormat of the source
   * @return the number of multifile for the DataFormat and the sample
   * @throws EoulsanRuntimeException if the DataFormat is not multifile
   */
  public int getOutputDataFileCount(final DataFormat format) {

    return this.context.getOutputDataFileCount(format, this.sample);
  }

  /**
   * Get the pathname for an input DataType and a Sample.
   * @param portName the name of the port
   * @return a String with the pathname
   */
  public String getInputDataFilename(final String portName) {

    return this.context.getInputDataFilename(portName, this.sample);
  }

  /**
   * Get the pathname for an input DataType and a Sample. This method works only
   * for a multifile DataFormat.
   * @param portName the name of the port
   * @param fileIndex file index for multifile data
   * @return a String with the pathname
   * @throws EoulsanRuntimeException if the DataFormat is not multifile
   */
  public String getInputDataFilename(final String portName, final int fileIndex) {

    return this.context.getInputDataFilename(portName, this.sample, fileIndex);
  }

  /**
   * Get the DataFile for an input DataType and a Sample.
   * @param portName the name of the port
   * @return a new DataFile object
   */
  public DataFile getInputDataFile(final String portName) {

    return this.context.getInputDataFile(portName, this.sample);
  }

  /**
   * Get the DataFile for an input DataType and a Sample. This method works only
   * for a multifile DataFormat.
   * @param portName the name of the port
   * @param fileIndex file index for multifile data
   * @return a new DataFile object
   * @throws EoulsanRuntimeException if the DataFormat is not multifile
   */
  public DataFile getInputDataFile(final String portName, final int fileIndex) {

    return this.context.getInputDataFile(portName, this.sample, fileIndex);
  }

  /**
   * Get the pathname for an output DataType and a Sample.
   * @param portName the name of the port
   * @return a String with the pathname
   */
  public String getOutputDataFilename(final String portName) {

    return this.context.getOutputDataFilename(portName, this.sample);
  }

  /**
   * Get the pathname for an output DataType and a Sample. This method works
   * only for a multifile DataFormat.
   * @param portName the name of the port
   * @param fileIndex file index for multifile data
   * @return a String with the pathname
   * @throws EoulsanRuntimeException if the DataFormat is not multifile
   */
  public String getOutputDataFilename(final String portName, final int fileIndex) {

    return this.context.getOutputDataFilename(portName, this.sample, fileIndex);
  }

  /**
   * Get the DataFile for a DataType and a Sample.
   * @param portName the name of the port
   * @return a new DataFile object
   */
  public DataFile getOutputDataFile(final String portName) {

    return this.context.getOutputDataFile(portName, this.sample);
  }

  /**
   * Get the DataFile for an output DataType and a Sample. This method works
   * only for a multifile DataFormat.
   * @param portName the name of the port
   * @param sample the sample for the source
   * @param fileIndex file index for multifile data
   * @return a new DataFile object
   * @throws EoulsanRuntimeException if the DataFormat is not multifile
   */
  public DataFile getOutputDataFile(final String portName, final int fileIndex) {

    return this.context.getOutputDataFile(portName, this.sample, fileIndex);
  }

  /**
   * Count the number for DataFile available for a multifile DataFormat and a
   * Sample. This method works only for a multifile DataFormat.
   * @param portName the name of the port
   * @return the number of multifile for the DataFormat and the sample
   * @throws EoulsanRuntimeException if the DataFormat is not multifile
   */
  public int getInputDataFileCount(final String portName) {

    return this.context.getInputDataFileCount(portName, this.sample);
  }

  /**
   * Count the number for DataFile available for a multifile DataFormat and a
   * Sample. This method works only for a multifile DataFormat.
   * @param portName the name of the port
   * @return the number of multifile for the DataFormat and the sample
   * @throws EoulsanRuntimeException if the DataFormat is not multifile
   */
  public int getOutputDataFileCount(final String portName) {

    return this.context.getOutputDataFileCount(portName, this.sample);
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param context Step context
   * @param sample sample
   */
  SampleStepContext(final StepContext context, final Sample sample) {

    if (context == null)
      throw new NullPointerException("context is null");

    if (sample == null)
      throw new NullPointerException("sample is null");

    this.context = context;
    this.sample = sample;
  }

}
