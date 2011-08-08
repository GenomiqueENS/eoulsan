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
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.AbstractEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.Step;

/**
 * This interface define a context.
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
   * Add executor information to log.
   */
  void logInfo();

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
  WorkflowDescription getWorkflow();

  /**
   * Get the current step.
   * @return the current Step or null if no Step is currently running.
   */
  Step getCurrentStep();

  /**
   * Get the pathname for a DataType and a Sample.
   * @param df the DataFormat of the source
   * @param sample the sample for the source
   * @return a String with the pathname
   */
  String getDataFilename(DataFormat df, Sample sample);

  /**
   * Get the DataFile for a DataType and a Sample.
   * @param df the DataFormat of the source
   * @param sample the sample for the source
   * @return a String with the pathname
   */
  DataFile getDataFile(DataFormat df, Sample sample);

  /**
   * Create an InputStream to load data.
   * @param ds the DataFormat of the data to load
   * @param sample the sample
   * @return an InputStream corresponding to DataType and Sample
   */
  InputStream getInputStream(DataFormat df, Sample sample) throws IOException;

  /**
   * Create a raw InputStream (without decompression of input data) to load
   * data.
   * @param dt the DataFormat of the data to load
   * @param sample the sample
   * @return an InputStream corresponding to DataType and Sample
   */
  InputStream getRawInputStream(DataFormat df, Sample sample)
      throws IOException;

  /**
   * Create an OutputStream to load data.
   * @param dt the DataFormat of the data to write
   * @param sample the sample
   * @return an InputStream corresponding to DataType and Sample
   */
  OutputStream getOutputStream(DataFormat df, Sample sample) throws IOException;
}
