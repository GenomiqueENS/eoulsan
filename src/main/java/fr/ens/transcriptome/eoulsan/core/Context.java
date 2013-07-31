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
 * This interface define a context.
 * @since 1.0
 * @author Laurent Jourdren
 */
public interface Context {

  /**
   * Get the command name.
   * @return the command name
   */
  String getCommandName();

  /**
   * Get the UUID of the job.
   * @return the job UUID
   */
  String getJobUUID();

  /**
   * Get the job description.
   * @return the job description
   */
  String getJobDescription();

  /**
   * Get the job environment.
   * @return the job environment
   */
  String getJobEnvironment();

  /**
   * Get command description.
   * @return the command description
   */
  String getCommandDescription();

  /**
   * Get the command author.
   * @return the command author
   */
  String getCommandAuthor();

  /**
   * Get the base path.
   * @return Returns the basePath
   */
  String getBasePathname();

  /**
   * Get the log path.
   * @return Returns the log Path
   */
  String getLogPathname();

  /**
   * Get the output path.
   * @return Returns the output Path
   */
  String getOutputPathname();

  /**
   * Get the job id.
   * @return the job id
   */
  String getJobId();

  /**
   * Get the host of the job.
   * @return a string with the host of the job
   */
  String getJobHost();

  /**
   * Get the creation time of the context.
   * @return the creation time of the context in milliseconds since epoch
   *         (1.1.1970)
   */
  long getContextCreationTime();

  /**
   * Get the design path.
   * @return the design path
   */
  String getDesignPathname();

  /**
   * Get the parameter path.
   * @return the parameter path
   */
  String getParameterPathname();

  /**
   * Get the application jar path.
   * @return Returns the jar path
   */
  String getJarPathname();

  /**
   * Get EoulsanRuntime.
   * @return the EoulsanRuntime
   */
  AbstractEoulsanRuntime getRuntime();

  /**
   * Get Eoulsan settings.
   * @return the Settings
   */
  Settings getSettings();

  /**
   * Get the logger.
   * @return the logger
   */
  Logger getLogger();

  /**
   * Get the workflow description
   * @return the workflow description
   */
  Workflow getWorkflow();

  /**
   * Get the current step.
   * @return the current Step or null if no Step is currently running.
   */
  WorkflowStep getCurrentStep();

  /**
   * Get the pathname for an input DataType and a Sample.
   * @param format the DataFormat of the source
   * @param sample the sample for the source
   * @return a String with the pathname
   */
  String getInputDataFilename(DataFormat format, Sample sample);

  /**
   * Get the pathname for an input DataType and a Sample. This method works only
   * for a multifile DataFormat.
   * @param format the DataFormat of the source
   * @param sample the sample for the source
   * @param fileIndex file index for multifile data
   * @return a String with the pathname
   * @throws EoulsanRuntimeException if the DataFormat is not multifile
   */
  String getInputDataFilename(DataFormat format, Sample sample, int fileIndex);

  /**
   * Get the DataFile for an input DataType and a Sample.
   * @param format the DataFormat of the source
   * @param sample the sample for the source
   * @return a new DataFile object
   */
  DataFile getInputDataFile(DataFormat format, Sample sample);

  /**
   * Get the DataFile for an input DataType and a Sample. This method works only
   * for a multifile DataFormat.
   * @param format the DataFormat of the source
   * @param sample the sample for the source
   * @param fileIndex file index for multifile data
   * @return a new DataFile object
   * @throws EoulsanRuntimeException if the DataFormat is not multifile
   */
  DataFile getInputDataFile(DataFormat format, Sample sample, int fileIndex);

  /**
   * Get the pathname for an output DataType and a Sample.
   * @param format the DataFormat of the source
   * @param sample the sample for the source
   * @return a String with the pathname
   */
  String getOutputDataFilename(DataFormat format, Sample sample);

  /**
   * Get the pathname for an output DataType and a Sample. This method works
   * only for a multifile DataFormat.
   * @param format the DataFormat of the source
   * @param sample the sample for the source
   * @param fileIndex file index for multifile data
   * @return a String with the pathname
   * @throws EoulsanRuntimeException if the DataFormat is not multifile
   */
  String getOutputDataFilename(DataFormat format, Sample sample, int fileIndex);

  /**
   * Get the DataFile for a DataType and a Sample.
   * @param format the DataFormat of the source
   * @param sample the sample for the source
   * @return a new DataFile object
   */
  DataFile getOutputDataFile(DataFormat format, Sample sample);

  /**
   * Get the DataFile for an output DataType and a Sample. This method works
   * only for a multifile DataFormat.
   * @param format the DataFormat of the source
   * @param sample the sample for the source
   * @param fileIndex file index for multifile data
   * @return a new DataFile object
   * @throws EoulsanRuntimeException if the DataFormat is not multifile
   */
  DataFile getOutputDataFile(DataFormat format, Sample sample, int fileIndex);

  /**
   * Count the number for DataFile available for a multifile DataFormat and a
   * Sample. This method works only for a multifile DataFormat.
   * @param format the DataFormat of the source
   * @param sample the sample for the source
   * @return the number of multifile for the DataFormat and the sample
   * @throws EoulsanRuntimeException if the DataFormat is not multifile
   */
  int getInputDataFileCount(final DataFormat format, final Sample sample);

  /**
   * Count the number for DataFile available for a multifile DataFormat and a
   * Sample. This method works only for a multifile DataFormat.
   * @param format the DataFormat of the source
   * @param sample the sample for the source
   * @return the number of multifile for the DataFormat and the sample
   * @throws EoulsanRuntimeException if the DataFormat is not multifile
   */
  int getOutputDataFileCount(final DataFormat format, final Sample sample);

}
